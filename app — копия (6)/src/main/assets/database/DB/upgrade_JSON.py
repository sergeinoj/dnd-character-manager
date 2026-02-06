import sqlite3
import json
import os
import logging
import sys
from typing import Dict, Any, List

# ==========================================
# CONFIGURATION
# ==========================================
DB_NAME = "dnd_clean.db"
SCHEMA_FILE = "dnd_schema.sql"
LOG_FILE = "migration.log"

FILE_TO_TABLE_MAP = {
    "5e-SRD-Alignments.json": "alignments",
    "5e-SRD-Backgrounds.json": "backgrounds",
    "5e-SRD-Classes.json": "classes",
    "5e-SRD-Damage-Types.json": "damage_types",
    "5e-SRD-Equipment-Categories.json": "equipment_categories",
    "5e-SRD-Equipment.json": "equipment",
    "5e-SRD-Features.json": "features",
    "5e-SRD-Languages.json": "languages",
    "5e-SRD-Levels.json": "progression",
    "5e-SRD-Magic-Items.json": "magic_items",
    "5e-SRD-Magic-Schools.json": "magic_schools",
    "5e-SRD-Proficiencies.json": "proficiencies",
    "5e-SRD-Races.json": "races",
    "5e-SRD-Skills.json": "skills",
    "5e-SRD-Spells.json": "spells",
    "5e-SRD-Subclasses.json": "subclasses",
    "5e-SRD-Subraces.json": "subraces",
    "5e-SRD-Traits.json": "features",
    "5e-SRD-Weapon-Properties.json": "weapon_properties",
}

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(sys.stdout),
        logging.FileHandler(LOG_FILE, mode='w', encoding='utf-8')
    ]
)
logger = logging.getLogger(__name__)

class DNDImporter:
    def __init__(self, db_path: str, schema_path: str):
        self.db_path = db_path
        self.schema_path = schema_path
        self.conn = None
        self.cursor = None

    def connect(self):
        try:
            if os.path.exists(self.db_path):
                os.remove(self.db_path)
                logger.info(f"Removed existing database: {self.db_path}")
            self.conn = sqlite3.connect(self.db_path)
            self.cursor = self.conn.cursor()
            self.cursor.execute("PRAGMA journal_mode = WAL;")
            self.cursor.execute("PRAGMA foreign_keys = ON;")
            logger.info(f"Connected to database: {self.db_path}")
        except sqlite3.Error as e:
            logger.critical(f"Connection failed: {e}")
            sys.exit(1)

    def init_schema(self):
        with open(self.schema_path, 'r', encoding='utf-8') as f:
            ddl = f.read()
        self.cursor.executescript(ddl)
        self.conn.commit()
        logger.info("Schema initialized.")

    def get_table_columns(self, table_name: str) -> List[str]:
        self.cursor.execute(f"PRAGMA table_info({table_name})")
        return [row[1] for row in self.cursor.fetchall()]

    def load_json(self, filename: str) -> List[Dict]:
        if not os.path.exists(filename): return []
        with open(filename, 'r', encoding='utf-8') as f:
            return json.load(f)

    def prepare_data(self, record: Dict, columns: List[str]) -> Dict:
        mapped_data = {}
        rename_map = {"desc": "description", "url": "reference_json", "index": "index_name"}
        for key, value in record.items():
            db_key = rename_map.get(key, key)
            if db_key not in columns and f"{db_key}_json" in columns: db_key = f"{db_key}_json"
            if db_key not in columns:
                if key == "class" and "class_index" in columns: db_key = "class_index"
                elif key == "subclass" and "subclass_index" in columns: db_key = "subclass_index"
                elif key == "race" and "race_index" in columns: db_key = "race_index"
                else: continue
            if value is None: mapped_data[db_key] = None
            elif isinstance(value, (dict, list)): mapped_data[db_key] = json.dumps(value, ensure_ascii=False)
            else: mapped_data[db_key] = value
        return mapped_data

    def insert_batch(self, table_name: str, records: List[Dict]):
        if not records: return
        columns = self.get_table_columns(table_name)
        prepared_records = [self.prepare_data(r, columns) for r in records]
        count = 0
        for row in prepared_records:
            keys = list(row.keys())
            placeholders = ", ".join(["?"] * len(keys))
            cols = ", ".join(keys)
            if table_name == "equipment_category_links":
                query = f"INSERT OR IGNORE INTO {table_name} ({cols}) VALUES ({placeholders})"
                self.cursor.execute(query, list(row.values()))
                count += 1
                continue
            conflict_target = "entity_index" if table_name == "progression" else "index_name"
            update_set = ", ".join([f"{k}=excluded.{k}" for k in keys if k != 'id'])
            query = f"INSERT INTO {table_name} ({cols}) VALUES ({placeholders}) ON CONFLICT({conflict_target}) DO UPDATE SET {update_set}"
            self.cursor.execute(query, list(row.values()))
            count += 1
        self.conn.commit()
        logger.info(f"Table '{table_name}': Upserted {count} records.")

    def process_equipment_split(self, filename: str):
        data = self.load_json(filename)
        eq_records, wp_records, ar_records = [], [], []
        for item in data:
            cat_idx = item.get("category_index", "adventuring-gear")
            record = item.copy()
            cost_obj = record.get("cost")
            if isinstance(cost_obj, dict): record["cost"] = f"{cost_obj.get('quantity', 0)} {cost_obj.get('unit', '')}"
            if cat_idx == "weapon":
                record["label"] = record.get("name")
                dt = record.get("damage_type")
                if isinstance(dt, dict): record["damage_type"] = dt.get("name") or dt.get("index")
                dmg = record.get("damage")
                if isinstance(dmg, dict):
                    if not record.get("damage_dice"): record["damage_dice"] = dmg.get("damage_dice")
                    if not record.get("damage_type"):
                         dt_in = dmg.get("damage_type")
                         record["damage_type"] = dt_in.get("name") if isinstance(dt_in, dict) else dt_in
                wp_records.append(record)
            elif cat_idx == "armor":
                ac_data = record.get("armor_class_json") or record.get("armor_class")
                if isinstance(ac_data, dict):
                    record["ac_base"] = ac_data.get("base", 10)
                    record["dex_bonus"] = 1 if ac_data.get("dex_bonus") else 0
                    record["max_bonus"] = ac_data.get("max_bonus")
                ar_records.append(record)
            else: eq_records.append(record)
        self.insert_batch("weapons", wp_records)
        self.insert_batch("armor", ar_records)
        self.insert_batch("equipment", eq_records)

    def process_equipment_categories(self, filename: str):
        """Handler for Categories. Fills categories AND links tables."""
        data = self.load_json(filename)
        cat_records, link_records = [], []
        for item in data:
            # --- ФИКС: Добавлена поддержка parent_index ---
            cat_records.append({
                "index_name": item.get("index_name"),
                "name": item.get("name"),
                "parent_index": item.get("parent_index")
            })
            cat_idx = item.get("index_name")
            items = item.get("items_json", [])
            for ref in items:
                item_idx = ref if isinstance(ref, str) else ref.get("index")
                if item_idx: link_records.append({"category_index": cat_idx, "item_index": item_idx})
        self.insert_batch("equipment_categories", cat_records)
        self.insert_batch("equipment_category_links", link_records)

    def run(self):
        self.connect()
        self.init_schema()
        for filename, table in FILE_TO_TABLE_MAP.items():
            if filename == "5e-SRD-Equipment.json": self.process_equipment_split(filename)
            elif filename == "5e-SRD-Equipment-Categories.json": self.process_equipment_categories(filename)
            else:
                logger.info(f"Processing {filename}...")
                data = self.load_json(filename)
                if data: self.insert_batch(table, data)
        self.cursor.execute("PRAGMA optimize;")
        self.conn.close()
        logger.info("Migration Complete.")

if __name__ == "__main__":
    importer = DNDImporter(DB_NAME, SCHEMA_FILE)
    importer.run()
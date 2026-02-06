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

# MAPPING: JSON File -> Target Table
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
    "5e-SRD-Traits.json": "features",  # Traits merge into features
    "5e-SRD-Weapon-Properties.json": "weapon_properties",
}

# ==========================================
# LOGGING SETUP
# ==========================================
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
        """Establish database connection."""
        try:
            # Remove old DB to ensure clean state on re-run
            if os.path.exists(self.db_path):
                os.remove(self.db_path)
                logger.info(f"Removed existing database: {self.db_path}")

            self.conn = sqlite3.connect(self.db_path)
            self.cursor = self.conn.cursor()
            self.cursor.execute("PRAGMA journal_mode = WAL;")
            self.cursor.execute("PRAGMA foreign_keys = ON;")
            logger.info(f"Connected to new database: {self.db_path}")
        except sqlite3.Error as e:
            logger.critical(f"Database connection failed: {e}")
            sys.exit(1)

    def init_schema(self):
        """Load and execute DDL from SQL file."""
        if not os.path.exists(self.schema_path):
            logger.critical(f"Schema file not found: {self.schema_path}")
            sys.exit(1)

        try:
            with open(self.schema_path, 'r', encoding='utf-8') as f:
                ddl = f.read()
            self.cursor.executescript(ddl)
            self.conn.commit()
            logger.info("Schema initialized successfully.")
        except sqlite3.Error as e:
            logger.critical(f"Schema execution failed: {e}")
            self.conn.close()
            sys.exit(1)

    def get_table_columns(self, table_name: str) -> List[str]:
        """Fetch column names for a specific table to ensure safe inserts."""
        try:
            self.cursor.execute(f"PRAGMA table_info({table_name})")
            return [row[1] for row in self.cursor.fetchall()]
        except sqlite3.Error as e:
            logger.error(f"Failed to get columns for {table_name}: {e}")
            return []

    def load_json(self, filename: str) -> List[Dict]:
        """Safe JSON loader."""
        if not os.path.exists(filename):
            logger.warning(f"File not found: {filename}. Skipping.")
            return []

        try:
            with open(filename, 'r', encoding='utf-8') as f:
                return json.load(f)
        except json.JSONDecodeError as e:
            logger.error(f"JSON Error in {filename}: {e}")
            return []

    def prepare_data(self, record: Dict, columns: List[str]) -> Dict:
        """
        Maps JSON fields to DB columns.
        Handles serialization of lists/dicts to JSON strings.
        Handles renaming conventions (desc -> description, index -> index_name).
        """
        mapped_data = {}

        # Standard renames derived from audit
        rename_map = {
            "desc": "description",
            "url": "reference_json",
            "index": "index_name"
        }

        for key, value in record.items():
            # 1. Apply Renaming
            db_key = rename_map.get(key, key)

            # 2. Check if specific suffix is needed (e.g. classes -> classes_json)
            if db_key not in columns and f"{db_key}_json" in columns:
                db_key = f"{db_key}_json"

            # 3. Final Verification: Is this key in the table?
            if db_key not in columns:
                # Special cases for schema inconsistencies
                if key == "class" and "class_index" in columns: db_key = "class_index"
                elif key == "subclass" and "subclass_index" in columns: db_key = "subclass_index"
                elif key == "race" and "race_index" in columns: db_key = "race_index"
                else:
                    continue

            # 4. Value Transformation
            if value is None:
                mapped_data[db_key] = None
            elif isinstance(value, (dict, list)):
                # Force serialization for complex types
                mapped_data[db_key] = json.dumps(value, ensure_ascii=False)
            elif db_key.endswith("_json") and isinstance(value, str):
                mapped_data[db_key] = value
            else:
                mapped_data[db_key] = value

        return mapped_data

    def insert_batch(self, table_name: str, records: List[Dict]):
        """
        Performs batch INSERT OR REPLACE.
        """
        if not records:
            return

        columns = self.get_table_columns(table_name)
        if not columns:
            logger.error(f"Skipping {table_name}: No columns found.")
            return

        prepared_records = []
        for r in records:
            prepared_records.append(self.prepare_data(r, columns))

        if not prepared_records:
            return

        count = 0
        try:
            for row in prepared_records:
                keys = list(row.keys())
                placeholders = ", ".join(["?"] * len(keys))
                cols = ", ".join(keys)

                conflict_target = "index_name"
                if table_name == "progression":
                    conflict_target = "entity_index"
                elif table_name == "equipment_category_links":
                    query = f"INSERT OR IGNORE INTO {table_name} ({cols}) VALUES ({placeholders})"
                    self.cursor.execute(query, list(row.values()))
                    count += 1
                    continue

                update_set = ", ".join([f"{k}=excluded.{k}" for k in keys if k != 'id'])

                query = f"""
                    INSERT INTO {table_name} ({cols})
                    VALUES ({placeholders})
                    ON CONFLICT({conflict_target}) DO UPDATE SET {update_set}
                """
                self.cursor.execute(query, list(row.values()))
                count += 1

            self.conn.commit()
            logger.info(f"Table '{table_name}': Upserted {count} records.")

        except sqlite3.Error as e:
            logger.error(f"Batch Error in {table_name}: {e}")
            self.conn.rollback()

    def process_equipment_split(self, filename: str):
        """
        Special handler for Equipment.json which feeds into:
        - equipment (general)
        - weapons (if category_index == 'weapon')
        - armor (if category_index == 'armor')
        """
        data = self.load_json(filename)
        if not data: return

        eq_records = []
        wp_records = []
        ar_records = []

        for item in data:
            cat_idx = item.get("category_index", "adventuring-gear")

            # Common fields map
            record = item.copy()

            # Cost flattening
            cost_obj = record.get("cost")
            if isinstance(cost_obj, dict):
                cost_str = f"{cost_obj.get('quantity', 0)} {cost_obj.get('unit', '')}"
                record["cost"] = cost_str

            if cat_idx == "weapon":
                record["label"] = record.get("name")

                dt = record.get("damage_type")
                if isinstance(dt, dict):
                    record["damage_type"] = dt.get("name") or dt.get("index")

                # Check for nested damage dict from JSON source like {damage_dice, damage_type}
                dmg = record.get("damage")
                if isinstance(dmg, dict):
                    if not record.get("damage_dice"): record["damage_dice"] = dmg.get("damage_dice")
                    if not record.get("damage_type"):
                         dt_in = dmg.get("damage_type")
                         if isinstance(dt_in, dict):
                             record["damage_type"] = dt_in.get("name")
                         else:
                             record["damage_type"] = dt_in

                wp_records.append(record)

            elif cat_idx == "armor":
                ac_data = record.get("armor_class_json") or record.get("armor_class")
                if isinstance(ac_data, dict):
                    record["ac_base"] = ac_data.get("base", 10)
                    record["dex_bonus"] = 1 if ac_data.get("dex_bonus") else 0
                    record["max_bonus"] = ac_data.get("max_bonus")

                ar_records.append(record)

            else:
                eq_records.append(record)

        logger.info(f"Splitting Equipment: {len(wp_records)} Weapons, {len(ar_records)} Armor, {len(eq_records)} Gear.")
        self.insert_batch("weapons", wp_records)
        self.insert_batch("armor", ar_records)
        self.insert_batch("equipment", eq_records)

    def process_equipment_categories(self, filename: str):
        """
        Handler for Equipment-Categories.json.
        Fills: equipment_categories AND equipment_category_links
        """
        data = self.load_json(filename)
        if not data: return

        cat_records = []
        link_records = []

        for item in data:
            cat_records.append({
                "index_name": item.get("index_name"),
                "name": item.get("name")
            })

            cat_idx = item.get("index_name")
            items = item.get("items_json", [])

            for ref in items:
                item_idx = None
                if isinstance(ref, str):
                    item_idx = ref
                elif isinstance(ref, dict):
                    item_idx = ref.get("index")

                if item_idx:
                    link_records.append({
                        "category_index": cat_idx,
                        "item_index": item_idx
                    })

        self.insert_batch("equipment_categories", cat_records)
        self.insert_batch("equipment_category_links", link_records)

    def run(self):
        """Main execution flow."""
        self.connect()
        self.init_schema()

        for filename, table in FILE_TO_TABLE_MAP.items():
            # Special Processors
            if filename == "5e-SRD-Equipment.json":
                self.process_equipment_split(filename)
                continue

            if filename == "5e-SRD-Equipment-Categories.json":
                self.process_equipment_categories(filename)
                continue

            # Standard Processors
            logger.info(f"Processing {filename} -> {table}...")
            data = self.load_json(filename)
            if data:
                self.insert_batch(table, data)

        # Cleanup
        self.cursor.execute("PRAGMA optimize;")
        self.conn.close()
        logger.info("Migration Complete.")

# ==========================================
# EXECUTION BLOCK
# ==========================================
if __name__ == "__main__":
    current_dir = os.path.dirname(os.path.abspath(__file__))
    os.chdir(current_dir)

    importer = DNDImporter(DB_NAME, SCHEMA_FILE)
    importer.run()
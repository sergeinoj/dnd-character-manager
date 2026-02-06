import sqlite3
import json
import os
import logging
import sys
from typing import Dict, Any, List, Optional, Set

# ==========================================
# CONFIGURATION
# ==========================================
DB_NAME = "dnd_clean.db"
LOG_FILE = "migration.log"
SCHEMA_FILE = "dnd_schema.sql"

# Файлы, которые обрабатываются специальными функциями, исключены из общего цикла
FILE_TO_TABLE_MAP = {
    "5e-SRD-Alignments.json": "alignments",
    "5e-SRD-Classes.json": "classes",
    "5e-SRD-Damage-Types.json": "damage_types",
    "5e-SRD-Languages.json": "languages",
    "5e-SRD-Levels.json": "progression",
    "5e-SRD-Magic-Schools.json": "magic_schools",
    "5e-SRD-Proficiencies.json": "proficiencies",
    "5e-SRD-Races.json": "races",
    "5e-SRD-Skills.json": "skills",
    "5e-SRD-Spells.json": "spells",
    "5e-SRD-Subclasses.json": "subclasses",
    "5e-SRD-Subraces.json": "subraces",
    "5e-SRD-Weapon-Properties.json": "weapon_properties"
}

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(sys.stdout),
        logging.FileHandler(LOG_FILE, mode='w', encoding='utf-8')
    ]
)
logger = logging.getLogger("SRDImporter")

class SRDImporter:
    def __init__(self, db_path: str, schema_path: str):
        self.db_path = db_path
        self.schema_path = schema_path
        self.conn = None
        self.cursor = None
        self.feature_cache = {}
        self.items_registry = set() # Глобальный реестр существующих предметов (обычных и магических)

    def connect(self):
        try:
            if os.path.exists(self.db_path):
                os.remove(self.db_path)
                logger.info(f"Cleaned previous DB: {self.db_path}")

            self.conn = sqlite3.connect(self.db_path)
            self.conn.row_factory = sqlite3.Row
            self.cursor = self.conn.cursor()
            self.cursor.execute("PRAGMA journal_mode = WAL;")
            self.cursor.execute("PRAGMA foreign_keys = ON;")
            logger.info("SYSTEM READY. Database connected.")
        except sqlite3.Error as e:
            logger.critical(f"FATAL: Database connection failed. {e}")
            sys.exit(1)

    def init_schema(self):
        if not os.path.exists(self.schema_path):
             logger.error(f"Schema file {self.schema_path} not found.")
             return
        try:
            with open(self.schema_path, 'r', encoding='utf-8') as f:
                ddl = f.read()
            self.cursor.executescript(ddl)
            self.conn.commit()
            logger.info("DDL Schema applied successfully.")
        except sqlite3.Error as e:
            logger.critical(f"FATAL: Schema application failed. {e}")
            sys.exit(1)

    def load_json(self, filename: str) -> List[Dict]:
        if not os.path.exists(filename):
            logger.warning(f"File not found: {filename}. Skipping.")
            return []
        try:
            with open(filename, 'r', encoding='utf-8') as f:
                return json.load(f)
        except json.JSONDecodeError as e:
            logger.error(f"JSON Error in {filename}: {e}")
            return []

    def _serialize(self, data: Any) -> Any:
        if data is None: return None
        if isinstance(data, (dict, list)):
            return json.dumps(data, ensure_ascii=False)
        return data

    def _flatten_list_of_strings(self, data: Any) -> Optional[str]:
        if not data: return None
        if isinstance(data, list):
            clean_list = [str(item) for item in data]
            return json.dumps(clean_list, ensure_ascii=False)
        return None

    def get_columns(self, table_name: str) -> List[str]:
        self.cursor.execute(f"PRAGMA table_info({table_name})")
        return [row['name'] for row in self.cursor.fetchall()]

    def upsert_record(self, table_name: str, data: Dict, conflict_key: str = "index_name"):
        columns = self.get_columns(table_name)
        valid_data = {}
        rename_map = {"desc": "description", "url": "reference_json", "index": "index_name"}

        for key, value in data.items():
            db_key = rename_map.get(key, key)
            if db_key not in columns and f"{db_key}_json" in columns:
                db_key = f"{db_key}_json"

            # Specific mappings
            if key == "class" and "class_index" in columns: db_key = "class_index"
            elif key == "subclass" and "subclass_index" in columns: db_key = "subclass_index"
            elif key == "race" and "race_index" in columns: db_key = "race_index"
            elif key == "starting_equipment_options" and "starting_equipment_options_json" in columns: db_key = "starting_equipment_options_json"
            elif key == "starting_proficiencies" and "starting_proficiencies_json" in columns: db_key = "starting_proficiencies_json"
            elif key == "proficiency_choices" and "proficiency_choices_json" in columns: db_key = "proficiency_choices_json"

            if db_key in columns:
                valid_data[db_key] = self._serialize(value)

        if not valid_data: return

        cols = ", ".join(valid_data.keys())
        placeholders = ", ".join(["?"] * len(valid_data))

        if table_name == "equipment_category_links":
             query = f"INSERT OR IGNORE INTO {table_name} ({cols}) VALUES ({placeholders})"
             vals = list(valid_data.values())
        else:
            actual_conflict_key = "entity_index" if table_name == "progression" else conflict_key
            update_set = ", ".join([f"{k}=excluded.{k}" for k in valid_data.keys() if k != 'id'])
            query = f"INSERT INTO {table_name} ({cols}) VALUES ({placeholders}) ON CONFLICT({actual_conflict_key}) DO UPDATE SET {update_set}"
            vals = list(valid_data.values())

        try:
            self.cursor.execute(query, vals)
        except sqlite3.Error as e:
            logger.error(f"Insert failed for {table_name}: {e}")

    # --- SPECIALIZED PROCESSORS ---

    def process_features_precache(self):
        data = self.load_json("5e-SRD-Features.json")
        traits = self.load_json("5e-SRD-Traits.json")
        all_features = data + traits
        self.feature_cache = {}
        self.conn.execute("BEGIN TRANSACTION")
        try:
            for item in all_features:
                self.upsert_record("features", item)
                idx = item.get("index") or item.get("index_name")
                if idx: self.feature_cache[idx] = item
            self.conn.commit()
            logger.info(f"Processed {len(all_features)} features.")
        except Exception as e:
            self.conn.rollback()
            logger.error(f"Feature processing failed: {e}")

    def process_backgrounds_enhanced(self):
        filename = "5e-SRD-Backgrounds.json"
        data = self.load_json(filename)
        logger.info(f"Processing Enhanced Backgrounds...")
        self.conn.execute("BEGIN TRANSACTION")
        try:
            for item in data:
                bg_idx = item.get("index") or item.get("index_name")
                related_features = []
                main_feature_index = "None"

                for f_idx in self.feature_cache.keys():
                    if f_idx.startswith(f"bgf-{bg_idx}"):
                        related_features.append(f_idx)
                        if "lore" in f_idx or "feature" in f_idx:
                            main_feature_index = f_idx

                if not item.get("starting_proficiencies"):
                    skill_feat = self.feature_cache.get(f"bgf-{bg_idx}-skills")
                    if skill_feat and "choices_json" in skill_feat:
                         item["starting_proficiencies_json"] = skill_feat["choices_json"]

                if not item.get("language_options"):
                    lang_feat = self.feature_cache.get(f"bgf-{bg_idx}-langs")
                    if lang_feat and "choices_json" in lang_feat:
                        item["language_options_json"] = lang_feat["choices_json"]

                gold_amount = 0
                equip_list = item.get("starting_equipment_json", [])
                if not equip_list: equip_list = item.get("starting_equipment", [])
                clean_equip = []
                for eq in equip_list:
                    eq_idx = eq.get("index") or eq.get("equipment", {}).get("index")
                    qty = eq.get("quantity", 1)
                    if eq_idx == "gold": gold_amount = qty
                    else: clean_equip.append({"index": eq_idx, "quantity": qty})

                record = {
                    "index_name": bg_idx,
                    "name": item.get("name"),
                    "starting_proficiencies_json": item.get("starting_proficiencies_json") or item.get("starting_proficiencies"),
                    "language_options_json": item.get("language_options_json") or item.get("language_options"),
                    "starting_equipment_json": clean_equip,
                    "starting_equipment_options_json": item.get("starting_equipment_options"),
                    "starting_gold": gold_amount,
                    "feature_index": main_feature_index,
                    "feature_name": item.get("feature_name"),
                    "feature_desc": item.get("feature_desc"),
                    "feature_indices_json": related_features,
                    "personality_traits_json": self._flatten_list_of_strings(item.get("personality_traits_json")),
                    "ideals_json": self._flatten_list_of_strings(item.get("ideals_json")),
                    "bonds_json": self._flatten_list_of_strings(item.get("bonds_json")),
                    "flaws_json": self._flatten_list_of_strings(item.get("flaws_json"))
                }
                self.upsert_record("backgrounds", record)
            self.conn.commit()
            logger.info("Backgrounds processed.")
        except Exception as e:
            self.conn.rollback()
            logger.error(f"Background processing failed: {e}")

    def process_equipment_split(self):
        """Loads mundane equipment and registers IDs."""
        filename = "5e-SRD-Equipment.json"
        data = self.load_json(filename)
        if not data: return
        self.conn.execute("BEGIN TRANSACTION")
        try:
            for item in data:
                cat_idx = ""
                cat_obj = item.get("category_index") or item.get("equipment_category")
                if isinstance(cat_obj, dict): cat_idx = cat_obj.get("index", "")
                elif isinstance(cat_obj, str): cat_idx = cat_obj
                item["category_index"] = cat_idx

                # Register ID
                idx = item.get("index_name") or item.get("index")
                if idx: self.items_registry.add(idx)

                if cat_idx == "weapon": self.upsert_record("weapons", item)
                elif cat_idx == "armor": self.upsert_record("armor", item)
                else: self.upsert_record("equipment", item)
            self.conn.commit()
            logger.info("Equipment split processed.")
        except Exception as e:
            self.conn.rollback()
            logger.error(f"Equipment processing failed: {e}")

    def process_magic_items(self):
        """Loads magic items and registers IDs."""
        filename = "5e-SRD-Magic-Items.json"
        data = self.load_json(filename)
        if not data: return
        self.conn.execute("BEGIN TRANSACTION")
        try:
            for item in data:
                # Register ID
                idx = item.get("index_name") or item.get("index")
                if idx: self.items_registry.add(idx)

                self.upsert_record("magic_items", item)
            self.conn.commit()
            logger.info("Magic Items processed.")
        except Exception as e:
            self.conn.rollback()
            logger.error(f"Magic Items processing failed: {e}")

    def process_equipment_categories(self):
        """
        Links items to categories.
        MUST BE RUN AFTER Equipment AND Magic Items are loaded.
        """
        filename = "5e-SRD-Equipment-Categories.json"
        data = self.load_json(filename)
        if not data: return

        self.conn.execute("BEGIN TRANSACTION")
        try:
            # Step 1: Insert Categories
            subcategory_items: Set[str] = set()
            for item in data:
                self.upsert_record("equipment_categories", item)
                # Collect subcategory items to handle hierarchy (Optional but good)
                if item.get("parent_index"):
                    items_list = item.get("items_json") or item.get("equipment")
                    if isinstance(items_list, list):
                        for ref in items_list:
                            if isinstance(ref, dict) and ref.get("index"):
                                subcategory_items.add(ref.get("index"))

            # Step 2: Create Links
            for item in data:
                cat_idx = item.get("index_name") or item.get("index")
                items_list = item.get("items_json") or item.get("equipment")

                if isinstance(items_list, list):
                    for ref in items_list:
                        if isinstance(ref, dict):
                            item_idx = ref.get("index")
                            if item_idx:
                                # Validation: Item must exist in registry (Equipment OR Magic Item)
                                if item_idx not in self.items_registry:
                                    logger.warning(f"Item '{item_idx}' linked in Category '{cat_idx}' but NOT FOUND in any item table!")

                                # Hierarchy Filter (Optional)
                                if not item.get("parent_index") and item_idx in subcategory_items:
                                    continue

                                try:
                                    self.cursor.execute(
                                        "INSERT OR IGNORE INTO equipment_category_links (category_index, item_index) VALUES (?, ?)",
                                        (cat_idx, item_idx)
                                    )
                                except sqlite3.Error: pass
            self.conn.commit()
            logger.info("Equipment Categories processed.")
        except Exception as e:
            self.conn.rollback()
            logger.error(f"Cat processing failed: {e}")

    def run(self):
        self.connect()
        self.init_schema()

        # 1. Load Features (for backgrounds)
        self.process_features_precache()

        # 2. Load Backgrounds
        self.process_backgrounds_enhanced()

        # 3. Load ALL Items (Populates Registry)
        self.process_equipment_split()
        self.process_magic_items() # <--- NEW: Must run before categories

        # 4. Load Categories & Links (Validates against Registry)
        self.process_equipment_categories()

        # 5. Load standard tables
        for filename, table in FILE_TO_TABLE_MAP.items():
            logger.info(f"Processing {filename} -> {table}")
            data = self.load_json(filename)
            if not data: continue

            self.conn.execute("BEGIN TRANSACTION")
            try:
                for item in data:
                    if table == "progression":
                        if "entity_index" not in item:
                             c_idx = item.get("class", {}).get("index") if isinstance(item.get("class"), dict) else item.get("class_index")
                             s_idx = item.get("subclass", {}).get("index") if isinstance(item.get("subclass"), dict) else item.get("subclass_index")
                             lvl = item.get("level")
                             key_prefix = s_idx if s_idx else c_idx
                             item["entity_index"] = f"{key_prefix}-{lvl}"
                        self.upsert_record(table, item, conflict_key="entity_index")
                    else:
                        self.upsert_record(table, item)
                self.conn.commit()
            except Exception as e:
                self.conn.rollback()
                logger.error(f"Failed processing {filename}: {e}")

        self.cursor.execute("PRAGMA optimize;")
        self.conn.close()
        logger.info("MIGRATION COMPLETE.")

if __name__ == "__main__":
    importer = SRDImporter(DB_NAME, SCHEMA_FILE)
    importer.run()
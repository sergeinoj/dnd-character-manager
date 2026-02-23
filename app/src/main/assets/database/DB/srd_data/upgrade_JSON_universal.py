# D&D 5.1 Universal ETL Script
# Handles minimal SRD data with missing/null fields

import sqlite3
import json
import os
import logging
import sys

DB_NAME = "dnd_clean.db"
SCHEMA_FILE = "dnd_schema.sql"
LOG_FILE = "migration.log"

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [%(levelname)s] %(message)s',
    handlers=[
        logging.FileHandler(LOG_FILE, mode='w', encoding='utf-8'),
        logging.StreamHandler(sys.stdout)
    ]
)
logger = logging.getLogger("ETL_UNIVERSAL")

SAFE_DEFAULTS = {
    "weight": 0.0, "cost_cp": 0, "ac_base": 0, "dex_bonus": 0,
    "max_bonus": 0, "str_minimum": 0, "stealth_disadvantage": 0,
    "requires_attunement": 0, "max_charges": 0, "max_charges_2": 0,
    "max_charges_3": 0, "bonus_ac": 0, "bonus_attack": 0, "bonus_damage": 0,
    "bonus_save_dc": 0, "level": 0, "die_count": 0, "die_size": 0,
    "scaling_bonus": 0, "movement_bonus": 0, "ability_score_bonuses": 0,
    "prof_bonus": 0, "concentration": 0, "ritual": 0, "change_rule": 0,
    "variant": 0, "caster_level_increment": 0.0, "is_pact_increment": 0,
    "speed": 30, "hit_die": 0, "caster_weight": 0.0, "sub_caster_weight": 0.0,
    "starting_gold": 0, "blinded_restrained": 1, "capacity_count": 1,
    "hit_die_count": 0, "hit_die_size": 0, "hit_die_bonus": 0, "proficiency_bonus": 0,
    "hit_points": 0, "challenge_rating": 0.0
}


class UniversalImporter:
    def __init__(self, db_path, schema_path):
        self.db_path = db_path
        self.schema_path = schema_path
        self.conn = None
        self.cursor = None
        self.columns_cache = {}
        self.lookup = {}

    def setup(self):
        if os.path.exists(self.db_path):
            os.remove(self.db_path)
            logger.info("Old database removed.")

        self.conn = sqlite3.connect(self.db_path)
        self.conn.execute("PRAGMA foreign_keys = ON;")
        self.conn.execute("PRAGMA journal_mode = WAL;")
        self.cursor = self.conn.cursor()

        if os.path.exists(self.schema_path):
            with open(self.schema_path, 'r', encoding='utf-8') as f:
                self.cursor.executescript(f.read())
            logger.info("DDL Schema applied successfully.")
        else:
            logger.critical(f"FATAL: Schema file not found: {self.schema_path}")
            sys.exit(1)

        self._build_lookup_cache()

    def _get_cols(self, table):
        if table not in self.columns_cache:
            try:
                self.cursor.execute(f"PRAGMA table_info({table})")
                self.columns_cache[table] = [row[1] for row in self.cursor.fetchall()]
            except:
                return []
        return self.columns_cache[table]

    def _serialize(self, data):
        if data is None:
            return None
        return json.dumps(data, ensure_ascii=False) if isinstance(data, (dict, list)) else data

    def _get_smart_cost_cp(self, item_data):
        if "cost_cp" in item_data and item_data["cost_cp"] is not None:
            return int(item_data["cost_cp"])
        cost_obj = item_data.get("cost") or item_data.get("cost_json")
        if not cost_obj or not isinstance(cost_obj, dict):
            return 0
        q = cost_obj.get("quantity", 0)
        u = str(cost_obj.get("unit", "cp")).lower()
        rates = {"cp": 1, "sp": 10, "ep": 50, "gp": 100, "pp": 1000}
        return int(q * rates.get(u, 1))

    def upsert(self, table, data, pk="index_name"):
        cols = self._get_cols(table)
        if not cols:
            return

        if "index" in data and "index_name" not in data:
            data["index_name"] = data["index"]

        if not data.get(pk):
            return

        clean_data = {}
        for col_name in cols:
            if col_name == 'id':
                continue

            json_key = col_name[:-5] if col_name.endswith('_json') else col_name
            val = data.get(col_name)
            if val is None:
                val = data.get(json_key)

            if col_name == 'description' and val is None:
                val = data.get('desc')

            if val is None and col_name in SAFE_DEFAULTS:
                val = SAFE_DEFAULTS[col_name]

            if val is not None:
                clean_data[col_name] = self._serialize(val)

        fields = ", ".join(clean_data.keys())
        placeholders = ", ".join(["?"] * len(clean_data))
        updates = ", ".join([f"{k}=excluded.{k}" for k in clean_data.keys() if k != pk])
        sql = f"INSERT INTO {table} ({fields}) VALUES ({placeholders}) ON CONFLICT({pk}) DO UPDATE SET {updates}"
        self.cursor.execute(sql, list(clean_data.values()))

    def insert_row(self, table, data):
        cols = self._get_cols(table)
        if not cols:
            return

        clean_data = {}
        for col_name in cols:
            if col_name == 'id':
                continue

            json_key = col_name[:-5] if col_name.endswith('_json') else col_name
            val = data.get(col_name)
            if val is None:
                val = data.get(json_key)
            if val is None and col_name in SAFE_DEFAULTS:
                val = SAFE_DEFAULTS[col_name]
            if val is None:
                continue

            clean_data[col_name] = self._serialize(val)

        if not clean_data:
            return

        fields = ", ".join(clean_data.keys())
        placeholders = ", ".join(["?"] * len(clean_data))
        sql = f"INSERT INTO {table} ({fields}) VALUES ({placeholders})"
        self.cursor.execute(sql, list(clean_data.values()))

    def _build_lookup_cache(self):
        files = ["5e-SRD-Skills.json", "5e-SRD-Languages.json", "5e-SRD-Proficiencies.json",
                 "5e-SRD-Damage-Types.json", "5e-SRD-Races.json", "5e-SRD-Subraces.json"]
        for f in files:
            self._load_lookup_file(f)

    def _load_lookup_file(self, name):
        if os.path.exists(name):
            path = name
        else:
            base_dir = os.path.dirname(os.path.abspath(__file__))
            path = os.path.join(base_dir, name)
        
        if not os.path.exists(path):
            return {}
        
        try:
            with open(path, 'r', encoding='utf-8') as f:
                data = json.load(f)
            if not isinstance(data, list):
                return {}
            for x in data:
                if isinstance(x, dict) and x.get("index"):
                    self.lookup[x["index"]] = x.get("name")
        except:
            pass
        return {}

    def process_generic(self, data, table):
        for entry in data:
            entry["cost_cp"] = self._get_smart_cost_cp(entry)
            
            if table == "skills" and "ability_score" in entry:
                if isinstance(entry["ability_score"], dict):
                    entry["ability_score_index"] = entry["ability_score"].get("index")
            
            self.upsert(table, entry)

    def process_features(self, data):
        for entry in data:
            fk_map = {"class": "class_index", "subclass": "subclass_index", 
                      "race": "race_index", "subrace": "subrace_index", 
                      "background": "background_index"}
            for j_k, d_c in fk_map.items():
                if j_k in entry and isinstance(entry[j_k], dict):
                    entry[d_c] = entry[j_k].get("index")

            idx = entry.get("index_name") or entry.get("index")
            if idx and idx.startswith("bgf-") and not entry.get("background_index"):
                parts = idx.split("-")
                if len(parts) > 1:
                    entry["background_index"] = parts[1]

            if "choices" in entry:
                entry["choices"] = self._normalize_choices(entry["choices"])
            self.upsert("features", entry)

    def _normalize_choices(self, choices):
        if isinstance(choices, dict):
            choices = [choices]
        if not isinstance(choices, list):
            return choices
        return choices

    def process_spells(self, data):
        for entry in data:
            entry["concentration"] = 1 if entry.get("concentration") else 0
            entry["ritual"] = 1 if entry.get("ritual") else 0
            if isinstance(entry.get("school"), dict):
                entry["school"] = entry["school"].get("index")
            entry["cost_cp"] = self._get_smart_cost_cp(entry)
            self.upsert("spells", entry)

    def process_equipment(self, data):
        for item in data:
            item["cost_cp"] = self._get_smart_cost_cp(item)
            cat_obj = item.get("equipment_category")
            cat_idx = str(cat_obj.get("index") if isinstance(cat_obj, dict) else item.get("category_index", "")).lower()
            item["category_index"] = cat_idx

            if "weapon" in cat_idx:
                dmg = item.get("damage") or item.get("damage_json") or {}
                if isinstance(dmg, dict):
                    item["damage_dice"] = item.get("damage_dice") or dmg.get("damage_dice")
                    rt = item.get("damage_type") or dmg.get("damage_type")
                    item["damage_type"] = rt.get("index") if isinstance(rt, dict) else rt
                self.upsert("weapons", item)
            elif "armor" in cat_idx or "shield" in cat_idx:
                ac = item.get("armor_class") or {}
                if isinstance(ac, dict):
                    item["ac_base"] = ac.get("base", 0)
                    item["dex_bonus"] = 1 if ac.get("dex_bonus") else 0
                    item["max_bonus"] = ac.get("max_bonus")
                self.upsert("armor", item)
            else:
                self.upsert("equipment", item)

    def process_progression(self, data):
        for entry in data:
            c = entry.get("class", {})
            s = entry.get("subclass", {})

            c_idx = entry.get("class_index") or (c.get("index") if isinstance(c, dict) else None)
            s_idx = entry.get("subclass_index") or (s.get("index") if isinstance(s, dict) else None)

            ent_idx = entry.get("entity_index")
            if not ent_idx:
                ent_idx = f"{s_idx or c_idx}-{entry.get('level', 1)}"

            entry["entity_index"] = ent_idx
            entry["class_index"] = c_idx
            entry["subclass_index"] = s_idx

            self.upsert("progression", entry, pk="entity_index")

    def process_categories(self, data):
        for cat in data:
            idx = cat.get("index_name") or cat.get("index")
            if not idx:
                continue
            cat["index_name"] = idx
            self.upsert("equipment_categories", cat)
            for i in (cat.get("items_json") or cat.get("equipment") or []):
                i_idx = i.get("index") if isinstance(i, dict) else i
                if i_idx:
                    self.cursor.execute(
                        "INSERT OR IGNORE INTO equipment_category_links (category_index, item_index) VALUES (?, ?)",
                        (idx, i_idx)
                    )

    def process_conditions(self, data):
        for entry in data:
            if "mechanics" in entry and isinstance(entry["mechanics"], dict):
                entry["mechanics_json"] = json.dumps(entry["mechanics"], ensure_ascii=False)
            else:
                entry["mechanics_json"] = "{}"

            if isinstance(entry.get("desc"), list):
                entry["description"] = "\n".join(entry["desc"])

            self.upsert("conditions", entry, pk="index_name")

    def process_subraces(self, data):
        for entry in data:
            race = entry.get("race", {})
            if isinstance(race, dict):
                entry["race_index"] = race.get("index")

            self.upsert("subraces", entry)

    def process_subclasses(self, data):
        for entry in data:
            cls = entry.get("class", {})
            if isinstance(cls, dict):
                entry["class_index"] = cls.get("index")

            self.upsert("subclasses", entry)

    def process_monsters(self, data):
        for entry in data:
            idx = entry.get("index_name") or entry.get("index")
            if not idx:
                continue
            entry["index_name"] = idx

            cr_raw = entry.get("challenge_rating") or entry.get("challenge")
            normalized = self._normalize_challenge_rating(cr_raw)
            if normalized is not None:
                entry["challenge_rating"] = normalized

            if "stats" not in entry:
                sm = {}
                for k, s in [("strength", "STR"), ("dexterity", "DEX"), ("constitution", "CON"),
                             ("intelligence", "INT"), ("wisdom", "WIS"), ("charisma", "CHA")]:
                    if k in entry:
                        sm[s.lower()] = entry[k]
                if sm:
                    entry["stats"] = json.dumps(sm)

            self.upsert("monsters", entry)

            for t in ["monster_actions", "monster_special_abilities", "monster_legendary_actions",
                      "monster_reactions", "monster_proficiencies", "monster_damage_mods",
                      "monster_action_effects", "monster_attack_patterns", "monster_containers"]:
                try:
                    self.cursor.execute(f"DELETE FROM {t} WHERE monster_index = ?", (idx,))
                except:
                    pass

            for action in entry.get("actions", []) or []:
                a_idx = action.get("index")
                if not a_idx:
                    url = action.get("url")
                    if url:
                        a_idx = url.split("/")[-1]
                    else:
                        a_idx = action.get("name", "unknown").lower().replace(" ", "-")

                act_row = {
                    "monster_index": idx,
                    "action_index": a_idx,
                    "name": action.get("name"),
                    "desc": action.get("desc"),
                    "attack_bonus": action.get("attack_bonus"),
                    "attack_json": action.get("attack"),
                    "damage_json": action.get("damage"),
                    "dc_json": action.get("dc"),
                    "usage_json": action.get("usage"),
                    "options_json": action.get("options"),
                    "type": action.get("type")
                }
                self.insert_row("monster_actions", act_row)

            for leg in entry.get("legendary_actions", []) or []:
                row = {
                    "monster_index": idx,
                    "name": leg.get("name"),
                    "desc": leg.get("desc"),
                    "attack_json": leg.get("attack"),
                    "damage_json": leg.get("damage"),
                    "dc_json": leg.get("dc"),
                    "cost": leg.get("cost")
                }
                self.insert_row("monster_legendary_actions", row)

            for ab in entry.get("special_abilities", []) or []:
                row = {
                    "monster_index": idx,
                    "name": ab.get("name"),
                    "desc": ab.get("desc"),
                    "dc_json": ab.get("dc"),
                    "spellcasting_json": ab.get("spellcasting")
                }
                self.insert_row("monster_special_abilities", row)

    def _normalize_challenge_rating(self, value):
        if value is None:
            return 0.0
        if isinstance(value, (int, float)):
            return float(value)
        if isinstance(value, dict):
            nested = value.get("challenge_rating") or value.get("value")
            return self._normalize_challenge_rating(nested)
        if isinstance(value, str):
            import re
            from fractions import Fraction
            cleaned = value.strip().replace("CR", "").strip().split(" ")[0]
            try:
                if "/" in cleaned:
                    return float(Fraction(cleaned))
                return float(cleaned)
            except:
                pass
            frac_match = re.match(r"(\d+)\s*/\s*(\d+)", cleaned)
            if frac_match:
                try:
                    return float(Fraction(int(frac_match.group(1)), int(frac_match.group(2))))
                except:
                    pass
        return 0.0

    def transactional_load(self, file_path, processor_func, table_name):
        if not os.path.exists(file_path):
            logger.warning(f"File not found: {file_path}. Skipping.")
            return
        with open(file_path, 'r', encoding='utf-8') as f:
            data = json.load(f)
        self.conn.execute("BEGIN TRANSACTION")
        try:
            processor_func(data)
            self.conn.commit()
            logger.info(f"Loaded {file_path} -> {table_name}")
        except Exception as e:
            self.conn.rollback()
            logger.error(f"Error loading {file_path}: {e}")


if __name__ == "__main__":
    etl = UniversalImporter(DB_NAME, SCHEMA_FILE)
    etl.setup()

    etl.transactional_load("5e-SRD-Damage-Types.json", lambda d: etl.process_generic(d, "damage_types"), "damage_types")
    etl.transactional_load("5e-SRD-Magic-Schools.json", lambda d: etl.process_generic(d, "magic_schools"), "magic_schools")
    etl.transactional_load("5e-SRD-Weapon-Properties.json", lambda d: etl.process_generic(d, "weapon_properties"), "weapon_properties")
    etl.transactional_load("5e-SRD-Alignments.json", lambda d: etl.process_generic(d, "alignments"), "alignments")
    etl.transactional_load("5e-SRD-Languages.json", lambda d: etl.process_generic(d, "languages"), "languages")
    etl.transactional_load("5e-SRD-Skills.json", lambda d: etl.process_generic(d, "skills"), "skills")

    etl.transactional_load("5e-SRD-Conditions.json", etl.process_conditions, "conditions")

    etl.transactional_load("5e-SRD-Races.json", lambda d: etl.process_generic(d, "races"), "races")
    etl.transactional_load("5e-SRD-Subraces.json", etl.process_subraces, "subraces")
    etl.transactional_load("5e-SRD-Classes.json", lambda d: etl.process_generic(d, "classes"), "classes")
    etl.transactional_load("5e-SRD-Subclasses.json", etl.process_subclasses, "subclasses")
    etl.transactional_load("5e-SRD-Levels.json", etl.process_progression, "progression")
    etl.transactional_load("5e-SRD-Features.json", etl.process_features, "features")
    etl.transactional_load("5e-SRD-Traits.json", etl.process_features, "features (traits)")

    etl.transactional_load("5e-SRD-Equipment-Categories.json", etl.process_categories, "equipment_categories")
    etl.transactional_load("5e-SRD-Equipment.json", etl.process_equipment, "equipment")
    etl.transactional_load("5e-SRD-Magic-Items.json", lambda d: etl.process_generic(d, "magic_items"), "magic_items")
    etl.transactional_load("5e-SRD-Spells.json", etl.process_spells, "spells")

    etl.transactional_load("5e-SRD-Proficiencies.json", lambda d: etl.process_generic(d, "proficiencies"), "proficiencies")
    etl.transactional_load("5e-SRD-Backgrounds.json", lambda d: etl.process_generic(d, "backgrounds"), "backgrounds")

    etl.transactional_load("5e-SRD-Monsters.json", etl.process_monsters, "monsters (SRD)")

    etl.cursor.execute("PRAGMA optimize;")
    etl.conn.close()
    logger.info("ETL Universal Execution Complete.")

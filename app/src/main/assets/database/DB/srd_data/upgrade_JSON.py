# Имя файла: upgrade_JSON.py
# --- НАЧАЛО ФАЙЛА ---
import sqlite3
import json
import os
import logging
import sys
import re
from fractions import Fraction

# ==========================================
# CONFIGURATION & LOGGING
# ==========================================
DB_NAME = "dnd_clean.db"
LOG_FILE = "migration.log"
SCHEMA_FILE = "dnd_schema.sql"

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [%(levelname)s] %(message)s',
    handlers=[
        logging.FileHandler(LOG_FILE, mode='w', encoding='utf-8'),
        logging.StreamHandler(sys.stdout)
    ]
)
logger = logging.getLogger("ETL_MASTER_V3.1")

# Защита от NULL (Schema Absolutism)
SAFE_DEFAULTS = {
    "weight": 0.0, "cost_cp": 0, "ac_base": 0, "dex_bonus": 0,
    "max_bonus": 0, "str_minimum": 0, "stealth_disadvantage": 0,
    "requires_attunement": 0, "max_charges": 0, "max_charges_2": 0,
    "max_charges_3": 0, "bonus_ac": 0, "bonus_attack": 0, "bonus_damage": 0,
    "bonus_save_dc": 0, "level": 0, "die_count": 0, "die_size": 0,
    "scaling_bonus": 0, "movement_bonus": 0, "ability_score_bonuses": 0,
    "prof_bonus": 0, "concentration": 0, "ritual": 0, "change_rule": 0,
    "variant": 0, "caster_level_increment": 0.0, "is_pact_increment": 0, "speed": 30,
    "hit_die": 0, "caster_weight": 0.0, "sub_caster_weight": 0.0, "starting_gold": 0,
    "blinded_restrained": 1, "capacity_count": 1
}

class SRDImporter:
    def __init__(self, db_path, schema_path):
        self.db_path = db_path
        self.schema_path = schema_path
        self.conn = None
        self.cursor = None
        self.columns_cache = {}
        self.lookup = {}
        self.stats_map = {
            "str": "Сила", "dex": "Ловкость", "con": "Телосложение",
            "int": "Интеллект", "wis": "Мудрость", "cha": "Харизма"
        }

    def setup(self):
        """Полная инициализация БД."""
        if os.path.exists(self.db_path):
            try:
                os.remove(self.db_path)
                logger.info("Old database removed.")
            except Exception as e:
                logger.warning(f"Could not remove old DB: {e}")

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
        if data is None: return None
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

    def _generate_slug(self, text):
        if not text: return "unknown"
        return text.lower().strip().replace(" ", "-").replace("/", "-").replace("'", "")

    def upsert(self, table, data, pk="index_name"):
        cols = self._get_cols(table)
        if not cols: return
        clean_data = {}

        if "index" in data and "index_name" not in data:
            data["index_name"] = data["index"]

        if not data.get(pk):
            return

        for col_name in cols:
            if col_name == 'id': continue

            json_key = col_name[:-5] if col_name.endswith('_json') else col_name
            val = data.get(col_name)
            if val is None: val = data.get(json_key)
            if col_name == 'description' and val is None: val = data.get('desc')

            if val is None and col_name in SAFE_DEFAULTS:
                val = SAFE_DEFAULTS[col_name]

            clean_data[col_name] = self._serialize(val)

        fields = ", ".join(clean_data.keys())
        placeholders = ", ".join(["?"] * len(clean_data))
        updates = ", ".join([f"{k}=excluded.{k}" for k in clean_data.keys() if k != pk])
        sql = f"INSERT INTO {table} ({fields}) VALUES ({placeholders}) ON CONFLICT({pk}) DO UPDATE SET {updates}"
        self.cursor.execute(sql, list(clean_data.values()))

    def insert_row(self, table, data):
        cols = self._get_cols(table)
        if not cols: return
        clean_data = {}

        for col_name in cols:
            if col_name == 'id': continue

            json_key = col_name[:-5] if col_name.endswith('_json') else col_name
            val = data.get(col_name)

            if val is None: val = data.get(json_key)
            if val is None and col_name in SAFE_DEFAULTS: val = SAFE_DEFAULTS[col_name]
            if val is None: continue

            clean_data[col_name] = self._serialize(val)

        if not clean_data: return

        fields = ", ".join(clean_data.keys())
        placeholders = ", ".join(["?"] * len(clean_data))
        sql = f"INSERT INTO {table} ({fields}) VALUES ({placeholders})"
        self.cursor.execute(sql, list(clean_data.values()))

    # ==========================================
    # LOOKUP HELPERS
    # ==========================================
    def _resolve_file(self, name):
        if os.path.exists(name): return name
        base_dir = os.path.dirname(os.path.abspath(__file__))
        candidate = os.path.join(base_dir, name)
        return candidate if os.path.exists(candidate) else name

    def _load_lookup_file(self, name):
        path = self._resolve_file(name)
        if not os.path.exists(path): return {}
        try:
            with open(path, 'r', encoding='utf-8') as f:
                data = json.load(f)
            if not isinstance(data, list): return {}
            return {x.get("index"): x.get("name") for x in data if isinstance(x, dict) and x.get("index")}
        except: return {}

    def _build_lookup_cache(self):
        files = ["5e-SRD-Skills.json", "5e-SRD-Languages.json", "5e-SRD-Proficiencies.json",
                 "5e-SRD-Damage-Types.json", "5e-SRD-Races.json", "5e-SRD-Subraces.json"]
        for f in files: self.lookup.update(self._load_lookup_file(f))

    def _lookup_label(self, idx):
        if not idx: return None
        key = str(idx).lower().strip()
        for pref in ("skill-", "tool-", "lang-", "language-"):
            if key.startswith(pref): key = key[len(pref):]; break
        return self.stats_map.get(key) or self.lookup.get(key) or self.lookup.get(idx)

    def _normalize_choices(self, choices):
        if isinstance(choices, dict): choices = [choices]
        if not isinstance(choices, list): return choices
        def walk(obj):
            if isinstance(obj, dict):
                if "from" in obj:
                    from_el = obj.get("from")
                    if isinstance(from_el, list):
                        obj["from"] = [self._normalize_choice_option(o) for o in from_el]
                    elif isinstance(from_el, dict) and "options" in from_el:
                         from_el["options"] = [self._normalize_choice_option(o) for o in from_el["options"]]
                for v in obj.values(): walk(v)
            elif isinstance(obj, list):
                for it in obj: walk(it)
        for ch in choices: walk(ch)
        return choices

    def _normalize_choice_option(self, opt):
        if not isinstance(opt, dict): return opt
        if opt.get("label") or opt.get("name"): return opt
        item = opt.get("item")
        if isinstance(item, dict):
            idx = item.get("index")
            if idx and not item.get("name"):
                label = self._lookup_label(idx)
                if label: item["name"] = label
            return opt
        idx = opt.get("index") or opt.get("value")
        if idx:
            label = self._lookup_label(idx)
            if label: opt["label"] = label
        return opt

    # ==========================================
    # PROCESSORS
    # ==========================================

    def process_generic(self, data, table):
        for entry in data:
            entry["cost_cp"] = self._get_smart_cost_cp(entry)
            self.upsert(table, entry)

    def process_features(self, data):
        for entry in data:
            fk_map = {"class": "class_index", "subclass": "subclass_index", "race": "race_index",
                      "subrace": "subrace_index", "background": "background_index"}
            for j_k, d_c in fk_map.items():
                if j_k in entry and isinstance(entry[j_k], dict): entry[d_c] = entry[j_k].get("index")

            idx = entry.get("index_name") or entry.get("index")
            if idx and idx.startswith("bgf-") and not entry.get("background_index"):
                parts = idx.split("-")
                if len(parts) > 1: entry["background_index"] = parts[1]

            if "choices" in entry: entry["choices"] = self._normalize_choices(entry["choices"])
            self.upsert("features", entry)

    def process_spells(self, data):
        for entry in data:
            entry["concentration"] = 1 if entry.get("concentration") else 0
            entry["ritual"] = 1 if entry.get("ritual") else 0
            if isinstance(entry.get("school"), dict): entry["school"] = entry["school"].get("index")
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
        """Fixed logic to handle flat JSON without nested 'class' objects."""
        for entry in data:
            c = entry.get("class", {})
            s = entry.get("subclass", {})

            # Priority: Direct Key -> Nested Object -> None
            c_idx = entry.get("class_index") or (c.get("index") if isinstance(c, dict) else None)
            s_idx = entry.get("subclass_index") or (s.get("index") if isinstance(s, dict) else None)

            # entity_index generation fallback
            ent_idx = entry.get("entity_index")
            if not ent_idx:
                ent_idx = f"{s_idx or c_idx}-{entry.get('level')}"

            entry["entity_index"] = ent_idx
            entry["class_index"] = c_idx
            entry["subclass_index"] = s_idx

            self.upsert("progression", entry, pk="entity_index")

    def process_categories(self, data):
        for cat in data:
            idx = cat.get("index_name") or cat.get("index")
            if not idx: continue
            cat["index_name"] = idx
            self.upsert("equipment_categories", cat)
            for i in (cat.get("items_json") or cat.get("equipment") or []):
                i_idx = i.get("index") if isinstance(i, dict) else i
                if i_idx: self.cursor.execute("INSERT OR IGNORE INTO equipment_category_links (category_index, item_index) VALUES (?, ?)", (idx, i_idx))

    def process_conditions(self, data):
        """Fixed loader: Iterates list and serializes mechanics."""
        for entry in data:
            # Map mechanics dict to JSON string for DB
            if "mechanics" in entry and isinstance(entry["mechanics"], dict):
                entry["mechanics_json"] = json.dumps(entry["mechanics"], ensure_ascii=False)

            # Map desc array to string
            if isinstance(entry.get("desc"), list):
                entry["description"] = "\n".join(entry["desc"])

            self.upsert("conditions", entry, pk="index_name")

    def process_monsters(self, data):
        for entry in data:
            idx = entry.get("index_name") or entry.get("index")
            if not idx: continue
            entry["index_name"] = idx

            cr_raw = entry.get("challenge_rating") or entry.get("challenge")
            normalized = self._normalize_challenge_rating(cr_raw)
            if normalized is not None:
                entry["challenge_rating"] = normalized

            if "stats" not in entry:
                sm = {}
                for k, s in [("strength", "STR"), ("dexterity", "DEX"), ("constitution", "CON"),
                             ("intelligence", "INT"), ("wisdom", "WIS"), ("charisma", "CHA")]:
                    if k in entry: sm[s.lower()] = entry[k]
                if sm: entry["stats"] = sm

            self.upsert("monsters", entry)

            for t in ["monster_actions", "monster_special_abilities", "monster_legendary_actions",
                      "monster_reactions", "monster_proficiencies", "monster_damage_mods",
                      "monster_action_effects", "monster_attack_patterns", "monster_containers"]:
                try:
                    self.cursor.execute(f"DELETE FROM {t} WHERE monster_index = ?", (idx,))
                except: pass

            for action in entry.get("actions", []) or []:
                a_idx = action.get("index")
                if not a_idx:
                     url = action.get("url")
                     if url: a_idx = url.split("/")[-1]
                     else: a_idx = self._generate_slug(action.get("name"))

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

                dc_obj = action.get("dc")

                for cond_id in action.get("added_conditions", []) or []:
                    eff_row = {
                        "monster_index": idx,
                        "action_index": a_idx,
                        "trigger_event": "ON_HIT",
                        "effect_type": "APPLY_CONDITION",
                        "payload_json": json.dumps({"condition": cond_id}, ensure_ascii=False)
                    }
                    if dc_obj:
                         eff_row["trigger_event"] = "ON_SAVE_FAIL"
                         eff_row["save_stat"] = dc_obj.get("type", dc_obj.get("dc_type", {}).get("name", "STR"))
                         eff_row["save_dc_override"] = dc_obj.get("value", dc_obj.get("dc_value"))

                    self.insert_row("monster_action_effects", eff_row)

                damages = action.get("damage")
                if isinstance(damages, list):
                    for dmg in damages:
                        if isinstance(dmg, dict) and "damage_dice" in dmg:
                             dtype = dmg.get("damage_type")
                             if isinstance(dtype, dict): dtype = dtype.get("index")

                             eff_row = {
                                "monster_index": idx,
                                "action_index": a_idx,
                                "trigger_event": "ON_HIT",
                                "effect_type": "DAMAGE",
                                "payload_json": json.dumps({
                                    "dice": dmg["damage_dice"],
                                    "type": dtype
                                }, ensure_ascii=False)
                             }
                             self.insert_row("monster_action_effects", eff_row)

                ma_logic = action.get("multiattack_logic")
                if ma_logic:
                    self.cursor.execute("""
                        INSERT INTO monster_attack_patterns (monster_index, pattern_slug, logic_operator, description)
                        VALUES (?, ?, 'AND', ?)
                    """, (idx, a_idx, action.get("desc")))
                    pattern_id = self.cursor.lastrowid

                    for entry in ma_logic:
                        self.cursor.execute("""
                            INSERT INTO monster_attack_pattern_entries (pattern_id, entry_type, entry_index, count)
                            VALUES (?, 'ACTION', ?, ?)
                        """, (pattern_id, entry.get("action_index"), entry.get("count", 1)))

                if a_idx == 'swallow' or action.get("index") == 'swallow':
                     cont_row = {
                         "monster_index": idx,
                         "action_index": a_idx,
                         "capacity_size_limit": "MEDIUM",
                         "capacity_count": 1,
                         "blinded_restrained": 1
                     }
                     if dc_obj: cont_row["escape_dc"] = dc_obj.get("value")
                     self.insert_row("monster_containers", cont_row)

            for leg in entry.get("legendary_actions", []) or []:
                row = { "monster_index": idx, "name": leg.get("name"), "desc": leg.get("desc"),
                        "attack_json": leg.get("attack"), "damage_json": leg.get("damage"),
                        "dc_json": leg.get("dc"), "cost": leg.get("cost") }
                self.insert_row("monster_legendary_actions", row)

            for ab in entry.get("special_abilities", []) or []:
                row = { "monster_index": idx, "name": ab.get("name"), "desc": ab.get("desc"),
                        "dc_json": ab.get("dc"), "spellcasting_json": ab.get("spellcasting") }
                self.insert_row("monster_special_abilities", row)

            for prof in entry.get("proficiencies", []) or []:
                p_obj = prof.get("proficiency")
                if isinstance(p_obj, dict):
                    self.insert_row("monster_proficiencies", {
                        "monster_index": idx, "proficiency_index": p_obj.get("index"), "value": prof.get("value")
                    })

            for key, kind in [("damage_immunities", "immunity"), ("damage_resistances", "resistance"),
                              ("damage_vulnerabilities", "vulnerability")]:
                for v in entry.get(key, []) or []:
                    self.insert_row("monster_damage_mods", {"monster_index": idx, "kind": kind, "value": v})

    def _normalize_challenge_rating(self, value):
        if value is None:
            return None
        if isinstance(value, (int, float)):
            return float(value)
        if isinstance(value, dict):
            nested = value.get("challenge_rating") or value.get("value")
            return self._normalize_challenge_rating(nested)
        if isinstance(value, str):
            cleaned = value.strip().replace("CR", "").strip()
            cleaned = cleaned.split(" ")[0]
            try:
                if "/" in cleaned:
                    return float(Fraction(cleaned))
                return float(cleaned)
            except ValueError:
                pass
            frac_match = re.match(r"(\d+)\s*/\s*(\d+)", cleaned)
            if frac_match:
                try:
                    return float(Fraction(int(frac_match.group(1)), int(frac_match.group(2))))
                except ZeroDivisionError:
                    pass
        return None


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
    etl = SRDImporter(DB_NAME, SCHEMA_FILE)
    etl.setup()

    # 1. Dictionaries
    etl.transactional_load("5e-SRD-Damage-Types.json", lambda d: etl.process_generic(d, "damage_types"), "damage_types")
    etl.transactional_load("5e-SRD-Magic-Schools.json", lambda d: etl.process_generic(d, "magic_schools"), "magic_schools")
    etl.transactional_load("5e-SRD-Weapon-Properties.json", lambda d: etl.process_generic(d, "weapon_properties"), "weapon_properties")
    etl.transactional_load("5e-SRD-Alignments.json", lambda d: etl.process_generic(d, "alignments"), "alignments")
    etl.transactional_load("5e-SRD-Languages.json", lambda d: etl.process_generic(d, "languages"), "languages")
    etl.transactional_load("5e-SRD-Skills.json", lambda d: etl.process_generic(d, "skills"), "skills")

    # 2. Conditions (FIXED)
    etl.transactional_load("5e-SRD-Conditions.json", etl.process_conditions, "conditions")

    # 3. Character Options
    etl.transactional_load("5e-SRD-Races.json", lambda d: etl.process_generic(d, "races"), "races")
    etl.transactional_load("5e-SRD-Subraces.json", lambda d: etl.process_generic(d, "subraces"), "subraces")
    etl.transactional_load("5e-SRD-Classes.json", lambda d: etl.process_generic(d, "classes"), "classes")
    etl.transactional_load("5e-SRD-Subclasses.json", lambda d: etl.process_generic(d, "subclasses"), "subclasses")
    # Levels (FIXED)
    etl.transactional_load("5e-SRD-Levels.json", etl.process_progression, "progression")
    etl.transactional_load("5e-SRD-Features.json", etl.process_features, "features")
    etl.transactional_load("5e-SRD-Traits.json", etl.process_features, "features (traits)")

    # 4. Items & Spells
    etl.transactional_load("5e-SRD-Equipment-Categories.json", etl.process_categories, "equipment_categories")
    etl.transactional_load("5e-SRD-Equipment.json", etl.process_equipment, "equipment")
    etl.transactional_load("5e-SRD-Magic-Items.json", lambda d: etl.process_generic(d, "magic_items"), "magic_items")
    etl.transactional_load("5e-SRD-Spells.json", etl.process_spells, "spells")

    # 5. Other
    etl.transactional_load("5e-SRD-Proficiencies.json", lambda d: etl.process_generic(d, "proficiencies"), "proficiencies")
    etl.transactional_load("5e-SRD-Backgrounds.json", lambda d: etl.process_generic(d, "backgrounds"), "backgrounds")

    # 6. Monsters
    if os.path.exists("wild_shape_forms_localized.json"):
        etl.transactional_load("wild_shape_forms_localized.json", etl.process_monsters, "monsters (wild shape)")
    else:
        etl.transactional_load("5e-SRD-Monsters.json", etl.process_monsters, "monsters (SRD)")

    etl.cursor.execute("PRAGMA optimize;")
    etl.conn.close()
    logger.info("ETL v3.1 Execution Complete.")
# --- КОНЕЦ ФАЙЛА ---
# Имя файла: upgrade_JSON.py

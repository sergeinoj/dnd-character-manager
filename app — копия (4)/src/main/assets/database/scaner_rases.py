import sqlite3
import json
import os
import sys

# ==========================================
# CONFIGURATION
# ==========================================
DB_NAME = "dnd_clean.db"
OUTPUT_FILE = "DND_Races_Structure_Map.txt"

class ArchitectureMapper:
    def __init__(self, db_path: str):
        if not os.path.exists(db_path):
            print(f"Error: Database {db_path} not found.")
            sys.exit(1)
        self.conn = sqlite3.connect(db_path)
        self.conn.row_factory = sqlite3.Row
        self.cursor = self.conn.cursor()
        self.output = []

    def log(self, text: str, indent: int = 0):
        """Append text to output buffer with indentation."""
        spacer = "    " * indent
        self.output.append(f"{spacer}{text}")

    def get_feature_details(self, feature_key: str) -> dict:
        """Fetch feature details from DB."""
        self.cursor.execute("SELECT * FROM features WHERE index_name = ?", (feature_key,))
        row = self.cursor.fetchone()
        if row:
            return dict(row)
        return {"name": "UNKNOWN FEATURE", "description": "Feature not found in database."}

    def format_json_field(self, json_str: str) -> str:
        """Pretty print short JSON summary."""
        if not json_str:
            return "None"
        try:
            data = json.loads(json_str)
            # Serialize compactly for report
            return json.dumps(data, ensure_ascii=False)
        except:
            return "Invalid JSON"

    def process_trait_list(self, traits_json: str, indent: int):
        """Parse traits list and expand each feature."""
        if not traits_json:
            self.log("[No Traits]", indent)
            return

        try:
            trait_keys = json.loads(traits_json)
        except:
            self.log("[Error Parsing Traits JSON]", indent)
            return

        for key in trait_keys:
            feat = self.get_feature_details(key)

            # Header line for the trait
            self.log(f"-> TRAIT: [{key}] {feat.get('name')}", indent)

            # Details
            if feat.get('change_rule') == 1:
                 self.log(f"   [MECHANIC CHANGE] This trait modifies base stats logic.", indent + 1)

            self.log(f"   Desc: {feat.get('description', '')[:100]}...", indent + 1)

            # Check for Choices (e.g. Skill Selection, Dragon Color)
            if feat.get('choices_json'):
                self.log(f"   >>> CHOICE LOGIC: {self.format_json_field(feat['choices_json'])}", indent + 1)

                # Special Case: Dragon Ancestor expansion
                if key == "dragon-ancestor":
                    self.expand_dragon_ancestor(indent + 2)

            # Check for Reference Data (e.g. Damage Types)
            if feat.get('reference_json'):
                self.log(f"   >>> METADATA: {self.format_json_field(feat['reference_json'])}", indent + 1)

            # Check for Spells
            if feat.get('spell_show_json'):
                self.log(f"   >>> SPELLS GRANTED: {self.format_json_field(feat['spell_show_json'])}", indent + 1)

    def expand_dragon_ancestor(self, indent: int):
        """Special helper to show children of dragon ancestor."""
        self.cursor.execute("SELECT * FROM features WHERE index_name LIKE 'dragon-ancestor-%'")
        rows = self.cursor.fetchall()
        self.log("   [Available Ancestry Options (Linked via choices)]:", indent)
        for row in rows:
            meta = self.format_json_field(row['reference_json'])
            self.log(f"   * {row['name']} -> {meta}", indent)

    def generate_map(self):
        self.log("==================================================================================")
        self.log("D&D 5.1 SRD: RACE & SUBRACE ARCHITECTURE MAP")
        self.log("Generated from SQLite Database")
        self.log("==================================================================================\n")

        # 1. Fetch all Races
        self.cursor.execute("SELECT * FROM races ORDER BY name")
        races = self.cursor.fetchall()

        for race in races:
            self.log(f"RACE: {race['name'].upper()} ({race['index_name']})")
            self.log(f"Stats: Speed {race['speed']}ft, Size {race['size']}")
            self.log(f"Ability Bonuses: {self.format_json_field(race['ability_bonuses_json'])}")

            if race['language_options_json']:
                 self.log(f"WARNING: Legacy language options detected: {race['language_options_json']}", 1)

            self.log("BASE RACIAL TRAITS:", 1)
            self.process_trait_list(race['traits_json'], 1)

            # 2. Fetch Subraces for this Race
            self.cursor.execute("SELECT * FROM subraces WHERE race_index = ?", (race['index_name'],))
            subraces = self.cursor.fetchall()

            if subraces:
                self.log("\n    SUBRACES:", 1)
                for sub in subraces:
                    self.log(f"    + SUBRACE: {sub['name']} ({sub['index_name']})", 1)
                    if sub['ability_bonuses_json']:
                        self.log(f"      Bonuses: {self.format_json_field(sub['ability_bonuses_json'])}", 2)

                    # Logic for Human Variant special case
                    if sub['index_name'] == 'human-variant':
                         # We know this table doesn't have ability_bonus_options_json column usually in SRD standard schema,
                         # but if we added it or hacked it into 'description' or another field, we'd show it here.
                         # Since our schema for subraces has 'language_options_json' but maybe not 'ability_options',
                         # let's check what's actually there.
                         pass

                    self.log("      SUBRACE TRAITS:", 2)
                    self.process_trait_list(sub['traits_json'], 2)
                    self.log("")
            else:
                self.log("\n    (No Subraces defined)\n", 1)

            self.log("-" * 80 + "\n")

    def save_to_file(self):
        with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
            f.write("\n".join(self.output))
        print(f"Architecture Map saved to: {os.path.abspath(OUTPUT_FILE)}")

# ==========================================
# EXECUTION
# ==========================================
if __name__ == "__main__":
    mapper = ArchitectureMapper(DB_NAME)
    mapper.generate_map()
    mapper.save_to_file()
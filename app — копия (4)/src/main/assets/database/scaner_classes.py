import sqlite3
import json
import os
import sys
from collections import defaultdict

# ==========================================
# CONFIGURATION
# ==========================================
DB_NAME = "dnd_clean.db"
OUTPUT_FILE = "DND_Classes_Structure_Map.txt"

class ClassArchitectureMapper:
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
        return {"name": f"UNKNOWN FEATURE ({feature_key})", "description": "Feature not found in database."}

    def format_json_field(self, json_str: str) -> str:
        """Pretty print short JSON summary."""
        if not json_str:
            return "None"
        try:
            data = json.loads(json_str)
            return json.dumps(data, ensure_ascii=False, separators=(',', ':'))
        except json.JSONDecodeError:
            return "Invalid JSON"

    def process_feature(self, feature_key: str, indent: int):
        """Logs the details of a single feature."""
        feat = self.get_feature_details(feature_key)
        self.log(f"-> FEATURE: [{feature_key}] {feat.get('name')}", indent)
        
        sub_indent = indent + 1
        if feat.get('ui_group'):
            self.log(f"[UI Group: {feat.get('ui_group')}]", sub_indent)
        
        if feat.get('description'):
            desc_snippet = feat['description'].replace('\n', ' ').strip()
            self.log(f"Desc: {desc_snippet[:120]}...", sub_indent)

        if feat.get('choices_json'):
            self.log(f"CHOICE LOGIC: {self.format_json_field(feat['choices_json'])}", sub_indent)

        if feat.get('spell_show_json'):
            self.log(f"SPELLS GRANTED: {self.format_json_field(feat['spell_show_json'])}", sub_indent)
            
        if feat.get('reference_json'):
            self.log(f"METADATA: {self.format_json_field(feat['reference_json'])}", sub_indent)
            
        if feat.get('change_rule') == 1:
            self.log(f"!! MECHANIC CHANGE DETECTED !!", sub_indent)

    def process_progression_level(self, level_data: dict, indent: int):
        """Processes and logs a single row from the progression table."""
        source = f"Subclass ({level_data['subclass_index']})" if level_data['subclass_index'] else "Base Class"
        self.log(f"Source: {source}", indent)

        sub_indent = indent + 1
        if level_data.get('ability_score_bonuses'):
            self.log(f"Ability Score Bonuses: {level_data['ability_score_bonuses']}", sub_indent)
        if level_data.get('class_specific_json'):
            self.log(f"Class Specific: {self.format_json_field(level_data['class_specific_json'])}", sub_indent)
        if level_data.get('subclass_specific_json'):
            self.log(f"Subclass Specific: {self.format_json_field(level_data['subclass_specific_json'])}", sub_indent)
        if level_data.get('spellcasting_json'):
            self.log(f"Spellcasting Changes: {self.format_json_field(level_data['spellcasting_json'])}", sub_indent)

        # Process features for this progression entry
        features_json = level_data.get('feature_indices_json')
        if features_json:
            try:
                feature_keys = json.loads(features_json)
                if feature_keys:
                    self.log("Features Gained:", sub_indent)
                    for key in feature_keys:
                        self.process_feature(key, sub_indent + 1)
            except json.JSONDecodeError:
                self.log("Error Parsing Features JSON", sub_indent)
        
        self.log("", 0) # Add a newline for readability

    def generate_map(self):
        self.log("==================================================================================")
        self.log("D&D 5.1 SRD: CLASS & PROGRESSION ARCHITECTURE MAP")
        self.log(f"Generated from SQLite Database: {DB_NAME}")
        self.log("==================================================================================\n")

        self.cursor.execute("SELECT * FROM classes ORDER BY name")
        classes = self.cursor.fetchall()

        for cls in classes:
            self.log(f"CLASS: {cls['name'].upper()} ({cls['index_name']})")
            self.log(f"Hit Die: d{cls['hit_die']}", 1)
            self.log(f"Saving Throws: {self.format_json_field(cls['saving_throws_json'])}", 1)
            self.log(f"Static Proficiencies: {self.format_json_field(cls['proficiencies_json'])}", 1)
            self.log(f"Proficiency Choices: {self.format_json_field(cls['proficiency_choices_json'])}", 1)
            self.log(f"Spellcasting: {self.format_json_field(cls['spellcasting_json'])}", 1)

            # Fetch Subclasses
            self.cursor.execute("SELECT * FROM subclasses WHERE class_index = ?", (cls['index_name'],))
            subclasses = self.cursor.fetchall()
            if subclasses:
                self.log("SUBCLASSES:", 1)
                for sub in subclasses:
                    self.log(f"+ {sub['name']} ({sub['index_name']})", 2)
                    if sub['spells_json']:
                        self.log(f"Subclass Spells: {self.format_json_field(sub['spells_json'])}", 3)
            self.log("-" * 20, 1)

            # Fetch and group all progression data for this class
            self.cursor.execute("SELECT * FROM progression WHERE class_index = ? ORDER BY level", (cls['index_name'],))
            progressions = self.cursor.fetchall()
            
            prog_by_level = defaultdict(list)
            for p_row in progressions:
                prog_by_level[p_row['level']].append(dict(p_row))

            self.log("CLASS PROGRESSION:", 1)
            for level in range(1, 21):
                if level in prog_by_level:
                    level_entries = prog_by_level[level]
                    prof_bonus = level_entries[0].get('prof_bonus', 'N/A')
                    self.log(f"--- LEVEL {level} (Prof Bonus: +{prof_bonus}) ---", 2)
                    for entry in level_entries:
                        self.process_progression_level(entry, 3)
                
            self.log("\n" + "=" * 80 + "\n")

    def save_to_file(self):
        with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
            f.write("\n".join(self.output))
        print(f"Class Architecture Map saved to: {os.path.abspath(OUTPUT_FILE)}")

# ==========================================
# EXECUTION
# ==========================================
if __name__ == "__main__":
    mapper = ClassArchitectureMapper(DB_NAME)
    mapper.generate_map()
    mapper.save_to_file()
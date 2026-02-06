import json
import os
import glob
import logging
import shutil
from typing import Any, Dict, List

# --- CONFIGURATION ---
SOURCE_DIR = "./JSON"   # Папка с исходными файлами
TARGET_DIR = "./clean_data" # Папка для чистых файлов
LOG_FILE = "migration.log"

# --- SCHEMA MAPPING RULES ---
# Ключ: часть имени файла (case-insensitive). Значение: словарь замен {старое: новое}
FIELD_RENAMES = {
    "Races":          {"desc": "description"},
    "Subraces":       {"desc": "description"},
    "Features":       {"desc": "description"},
    "Traits":         {"desc": "description"},
    "Spells":         {"desc": "description"},
    "Equipment":      {"desc": "description"},
    "Magic-Items":    {"desc": "description"},
    "Skills":         {"desc": "description"},
    "Languages":      {"desc": "description"},
    "Damage-Types":   {"desc": "description"},
    "Magic-Schools":  {"desc": "description"},
    "Weapon-Properties": {"desc": "description"},
    "Alignments":     {"description": "desc"}, # В базе 'desc'
    "Subclasses":     {"description": "desc"}, # В базе 'desc'
    "Classes":        {}, # У классов нет описания в схеме, но есть hit_die и прочее
}

# Поля, которые нужно склеить из списка строк в одну строку с \n
FIELDS_TO_FLATTEN = ["desc", "description"]

# Поля, которые должны быть ОБЪЕКТАМИ в JSON (а не строками)
JSON_STRUCTURE_FIELDS = [
    "ability_bonuses_json", "languages_json", "traits_json",
    "starting_proficiencies_json", "starting_proficiency_options_json",
    "language_options_json", "subraces_json", "choices_json",
    "spell_show_json", "prerequisites_json", "reference_json",
    "components_json", "classes_json", "subclasses_json",
    "damage_json", "dc_json", "area_of_effect_json", "heal_at_slot_level_json",
    "proficiency_choices_json", "proficiencies_json", "saving_throws_json",
    "starting_equipment_json", "starting_equipment_options_json",
    "spellcasting_json", "multi_classing_json", "feature_indices_json",
    "class_specific_json", "subclass_specific_json", "cost_json",
    "armor_class_json", "range_json", "properties_json", "contents_json",
    "variants_json", "typical_speakers_json", "items_json"
]

# --- LOGGING SETUP ---
logging.basicConfig(
    level=logging.INFO,
    format="%(levelname)s: %(message)s",
    handlers=[
        logging.FileHandler(LOG_FILE, mode='w', encoding='utf-8'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger("Migrator")

class UniversalMigrator:
    def __init__(self):
        self.files_processed = 0

    def recursive_unpack(self, value: Any, depth=0) -> Any:
        """Рекурсивно распаковывает JSON-строки в объекты."""
        if depth > 5: return value
        if value is None: return None

        if isinstance(value, dict):
            return {k: self.recursive_unpack(v, depth + 1) for k, v in value.items()}

        if isinstance(value, list):
            return [self.recursive_unpack(v, depth + 1) for v in value]

        if isinstance(value, str):
            clean_val = value.strip()
            if not clean_val or clean_val.lower() == 'null':
                return None
            # Эвристика: если похоже на JSON
            if (clean_val.startswith('{') and clean_val.endswith('}')) or \
               (clean_val.startswith('[') and clean_val.endswith(']')):
                try:
                    parsed = json.loads(clean_val)
                    return self.recursive_unpack(parsed, depth + 1)
                except (json.JSONDecodeError, TypeError):
                    pass
        return value

    def flatten_text(self, value: Any) -> Any:
        """Склеивает список строк в один текст."""
        if isinstance(value, list) and all(isinstance(x, str) for x in value):
            return "\n".join(value)
        return value

    def get_rename_map(self, filename: str) -> Dict[str, str]:
        """Возвращает карту замен для конкретного файла."""
        for key, mapping in FIELD_RENAMES.items():
            if key.lower() in filename.lower():
                return mapping
        return {}

    def process_file(self, filepath: str):
        filename = os.path.basename(filepath)
        logger.info(f"--- Обработка: {filename} ---")

        try:
            with open(filepath, 'r', encoding='utf-8') as f:
                raw_data = json.load(f)
        except Exception as e:
            logger.error(f"ОШИБКА чтения {filename}: {e}")
            return

        if not isinstance(raw_data, list):
            logger.warning(f"Пропуск {filename}: Корневой элемент не список.")
            return

        rename_map = self.get_rename_map(filename)
        cleaned_data = []

        for record in raw_data:
            if not isinstance(record, dict):
                continue

            new_record = {}

            # 1. Проход по полям записи
            for key, val in record.items():
                new_key = key
                new_val = val

                # A. Переименование (Schema Alignment)
                if key in rename_map:
                    new_key = rename_map[key]
                    # Если в записи уже есть целевой ключ (конфликт), решаем его
                    if new_key in record:
                         # Если новое значение пустое, а старое нет - берем старое
                         # Если оба есть - приоритет у переименования (обычно)
                         pass

                # B. Распаковка JSON (Fix Double Encoding)
                if new_key in JSON_STRUCTURE_FIELDS or new_key.endswith("_json"):
                    new_val = self.recursive_unpack(val)

                # C. Склейка текста (Flatten Description)
                if new_key in FIELDS_TO_FLATTEN:
                    new_val = self.flatten_text(new_val)

                new_record[new_key] = new_val

            cleaned_data.append(new_record)

        # Сохранение
        os.makedirs(TARGET_DIR, exist_ok=True)
        target_path = os.path.join(TARGET_DIR, filename)

        with open(target_path, 'w', encoding='utf-8') as f_out:
            json.dump(cleaned_data, f_out, ensure_ascii=False, indent=2)

        logger.info(f"Успешно сохранено в: {target_path}")
        self.files_processed += 1

    def run(self):
        if not os.path.exists(SOURCE_DIR):
            logger.error(f"Папка {SOURCE_DIR} не найдена! Создайте её и положите туда JSON файлы.")
            return

        files = glob.glob(os.path.join(SOURCE_DIR, "*.json"))
        if not files:
            logger.warning("JSON файлы не найдены.")
            return

        logger.info(f"Найдено файлов: {len(files)}")
        for f in files:
            self.process_file(f)

        logger.info("--- МИГРАЦИЯ ЗАВЕРШЕНА ---")

if __name__ == "__main__":
    migrator = UniversalMigrator()
    migrator.run()
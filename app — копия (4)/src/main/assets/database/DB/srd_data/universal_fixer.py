import json
import os
import glob
import logging
import shutil
from typing import Any

# --- CONFIGURATION ---
TARGET_EXTENSION = "FIXED_5e-SRD-Magic-Items.json" # Маска файлов
BACKUP_SUFFIX = ".bak"       # Расширение для бэкапов
LOG_FILENAME = "batch_fixer.log"

# Список полей, которые МЫ ОЖИДАЕМ видеть как объекты (List/Dict), а не строки.
# Если скрипт видит такое поле и оно строка -> он пытается его распаковать.
# Добавляй сюда любые новые поля с суффиксом _json или специфичные имена.
FORCE_UNPACK_FIELDS = [
    "components_json",
    "classes_json",
    "subclasses_json",
    "damage_json",
    "dc_json",
    "area_of_effect_json",
    "heal_at_slot_level_json",
    "ability_bonuses_json",
    "languages_json",
    "traits_json",
    "starting_proficiencies_json",
    "starting_proficiency_options_json",
    "language_options_json",
    "subraces_json",
    "choices_json",
    "prerequisites_json",
    "spellcasting_json",
    "multi_classing_json",
    "starting_equipment_json",
    "starting_equipment_options_json",
    "feature_indices_json",
    "class_specific_json",
    "subclass_specific_json",
    "reference_json",
    "spell_show_json",
    "variants_json",
    "cost_json",
    "armor_class_json",
    "range_json",
    "properties_json",
    "contents_json",
    "typical_speakers_json"
]

# --- LOGGING ---
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s",
    handlers=[
        logging.FileHandler(LOG_FILENAME, mode='w', encoding='utf-8'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger("BatchFixer")

class JsonBatchFixer:
    def __init__(self):
        self.processed_files = 0
        self.error_files = 0

    def recursive_unpack(self, value: Any, depth=0) -> Any:
        """
        Рекурсивно сдирает слои stringify, пока не доберется до объекта.
        Idempotent: Если это уже объект, возвращает его.
        """
        if depth > 5: return value # Breaker
        if value is None: return None

        # 1. Если это уже структура -> пытаемся пойти глубже (для вложенных полей)
        if isinstance(value, dict):
            return {k: self.recursive_unpack(v, depth + 1) for k, v in value.items()}
        if isinstance(value, list):
            return [self.recursive_unpack(v, depth + 1) for v in value]

        # 2. Если это строка -> пробуем распарсить
        if isinstance(value, str):
            value_clean = value.strip()
            if not value_clean or value_clean.lower() == 'null':
                return None

            # Эвристика: похоже ли это на JSON?
            if (value_clean.startswith('{') and value_clean.endswith('}')) or \
               (value_clean.startswith('[') and value_clean.endswith(']')):
                try:
                    parsed = json.loads(value_clean)
                    # Успех! Рекурсивно проверяем результат (вдруг там двойная упаковка)
                    return self.recursive_unpack(parsed, depth + 1)
                except (json.JSONDecodeError, TypeError):
                    pass # Не JSON, оставляем строкой

        return value

    def process_file(self, filepath):
        filename = os.path.basename(filepath)

        # Пропускаем сам скрипт и логи
        if filename in ["batch_json_fixer.py", LOG_FILENAME]: return
        if filename.endswith(BACKUP_SUFFIX): return

        logger.info(f"--- Processing: {filename} ---")

        try:
            with open(filepath, 'r', encoding='utf-8') as f:
                data = json.load(f)
        except Exception as e:
            logger.error(f"Failed to read {filename}: {e}")
            self.error_files += 1
            return

        if not isinstance(data, list):
            logger.warning(f"Skipping {filename}: Root is not a list.")
            return

        has_changes = False
        fixed_records = 0

        # --- LOGIC ---
        new_data = []
        for record in data:
            if not isinstance(record, dict):
                new_data.append(record)
                continue

            new_record = record.copy()
            record_changed = False

            for key, val in record.items():
                # Проверяем, нужно ли лечить это поле
                # 1. Если оно в списке FORCE_UNPACK
                # 2. Или если оно заканчивается на _json
                if key in FORCE_UNPACK_FIELDS or key.endswith("_json"):
                    fixed_val = self.recursive_unpack(val)

                    # Сравнение (нужно учитывать, что json.loads меняет типы, напр. кортеж на лист)
                    # Простой способ проверить изменение:
                    if val != fixed_val:
                        # Дополнительная проверка: не превратили ли мы строку в то же самое?
                        # Иногда unpack возвращает то же значение, если не смог распарсить.
                        # Нас интересует переход str -> list/dict
                        if isinstance(val, str) and isinstance(fixed_val, (dict, list)):
                            new_record[key] = fixed_val
                            record_changed = True
                            # logger.info(f"  Fixed field '{key}'") # Слишком много логов

            if record_changed:
                fixed_records += 1
                has_changes = True

            new_data.append(new_record)

        # --- SAVE ---
        if has_changes:
            # 1. Создаем бэкап
            backup_path = filepath + BACKUP_SUFFIX
            shutil.copy2(filepath, backup_path)
            logger.info(f"Backup created: {backup_path}")

            # 2. Перезаписываем оригинал
            with open(filepath, 'w', encoding='utf-8') as f_out:
                json.dump(new_data, f_out, ensure_ascii=False, indent=2)

            logger.info(f"SUCCESS: Updated {filename}. Fixed records: {fixed_records}")
            self.processed_files += 1
        else:
            logger.info(f"No changes needed for {filename}. File is clean.")

    def run(self):
        files = glob.glob(TARGET_EXTENSION)
        if not files:
            logger.warning("No JSON files found in current directory.")
            return

        logger.info(f"Found {len(files)} JSON files. Starting batch job...")

        for file in files:
            self.process_file(file)

        logger.info("--- BATCH COMPLETE ---")
        logger.info(f"Files Modified: {self.processed_files}")
        logger.info(f"Errors: {self.error_files}")

if __name__ == "__main__":
    fixer = JsonBatchFixer()
    fixer.run()
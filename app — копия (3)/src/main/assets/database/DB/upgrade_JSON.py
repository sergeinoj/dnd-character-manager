import json
import os
import logging

# 1. SETUP: Консольный логгер
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [%(levelname)s] %(message)s'
)
logger = logging.getLogger("JSON_MERGE_ARCHITECT")

# Определение разрешенных колонок согласно DDL для таблицы features
ALLOWED_COLUMNS = {
    "index_name", "name", "description", "level", "class_index",
    "subclass_index", "race_index", "subrace_index", "background_index",
    "choices_json", "spell_show_json", "change_rule", "prerequisites_json", "reference_json"
}

def validate_fields(data_list, source_name):
    """Проверяет наличие полей, отсутствующих в целевой схеме БД."""
    for item in data_list:
        idx = item.get("index_name", "UNKNOWN")
        for key in item.keys():
            if key not in ALLOWED_COLUMNS:
                logger.warning(f"Unmapped field detected in {source_name} (index: {idx}): [{key}]")

def atomic_merge(base_path, update_path, output_path):
    """Выполняет слияние и атомарную запись."""
    try:
        # Загрузка базового файла
        if not os.path.exists(base_path):
            logger.error(f"Base file {base_path} not found.")
            return

        with open(base_path, 'r', encoding='utf-8') as f:
            base_data = json.load(f)

        # Загрузка апдейта
        if not os.path.exists(update_path):
            logger.error(f"Update file {update_path} not found.")
            return

        with open(update_path, 'r', encoding='utf-8') as f:
            update_data = json.load(f)

        # Валидация полей относительно DDL (только логгирование)
        validate_fields(base_data, "BASE_FILE")
        validate_fields(update_data, "UPDATE_FILE")

        # Логика слияния: Маппинг по index_name
        # Используем словарь для быстрого поиска и замены
        merged_map = {item['index_name']: item for item in base_data}

        inserted_count = 0
        updated_count = 0

        for item in update_data:
            idx = item['index_name']
            if idx in merged_map:
                updated_count += 1
            else:
                inserted_count += 1
            merged_map[idx] = item # Update перезаписывает Base

        # Преобразование обратно в список
        final_list = list(merged_map.values())

        # АТОМАРНАЯ ЗАПИСЬ:
        # Пишем во временный файл, затем заменяем им оригинал
        temp_output = output_path + ".tmp"
        with open(temp_output, 'w', encoding='utf-8') as f:
            json.dump(final_list, f, ensure_ascii=False, indent=2)

        os.replace(temp_output, output_path)

        logger.info(f"Merge completed. Saved to: {output_path}")
        logger.info(f"Statistics: {updated_count} records updated, {inserted_count} new records added.")
        logger.info(f"Total records in final file: {len(final_list)}")

    except Exception as e:
        logger.error(f"MERGE CRITICAL FAILURE: {e}")
        if 'temp_output' in locals() and os.path.exists(temp_output):
            os.remove(temp_output)
        raise

if __name__ == "__main__":
    # Конфигурация путей
    FILE_BASE = "5e-SRD-Features.json"
    FILE_UPDATE = "Features_update.json"
    FILE_OUTPUT = "5e-SRD-Features.json"

    atomic_merge(FILE_BASE, FILE_UPDATE, FILE_OUTPUT)
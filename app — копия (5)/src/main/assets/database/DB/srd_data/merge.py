import json
import os
import logging
import sys

# ==========================================
# CONFIGURATION
# ==========================================
TARGET_FILE = "5e-SRD-Features.json"
PATCH_FILE = "expert_logic_injection_normalized.json"
OUTPUT_FILE = "5e-SRD-Features.json"  # Перезапись оригинала
LOG_FILE = "patching.log"

# Настройка логирования
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[logging.StreamHandler(sys.stdout), logging.FileHandler(LOG_FILE, encoding='utf-8')]
)
logger = logging.getLogger(__name__)

def run_patcher():
    logger.info("Запуск финальной проливки патча...")

    if not os.path.exists(TARGET_FILE):
        logger.error(f"Файл цели {TARGET_FILE} не найден!")
        return

    if not os.path.exists(PATCH_FILE):
        logger.error(f"Файл патча {PATCH_FILE} не найден!")
        return

    # 1. Загрузка данных
    with open(TARGET_FILE, 'r', encoding='utf-8') as f:
        target_data = json.load(f)

    with open(PATCH_FILE, 'r', encoding='utf-8') as f:
        patch_list = json.load(f)

    # 2. Индексация патча (для быстрого поиска)
    patch_map = {item['index_name']: item for item in patch_list}

    # 3. Применение патча (Update существующих)
    patched_count = 0
    new_items_count = 0
    found_indices = set()

    # Поля, которые разрешены в схеме (согласно твоему DDL)
    allowed_fields = [
        "index_name", "name", "description", "level", "class_index",
        "subclass_index", "race_index", "subrace_index", "background_index",
        "choices_json", "spell_show_json", "change_rule", "prerequisites_json", "reference_json"
    ]

    result_data = []

    for item in target_data:
        idx = item.get("index_name")
        if idx in patch_map:
            patch = patch_map[idx]
            # Обновляем поля из патча
            for key, value in patch.items():
                # Маппинг: переименовываем desc в description для базы
                target_key = "description" if key == "desc" else key

                # Маппинг: прячем mechanics в reference_json (т.к. колонки mechanics нет)
                if key == "mechanics":
                    item["reference_json"] = value
                elif target_key in allowed_fields:
                    item[target_key] = value

            found_indices.add(idx)
            patched_count += 1

        result_data.append(item)

    # 4. Вставка новых объектов (Insert тех, кого нет в оригинале)
    for idx, patch in patch_map.items():
        if idx not in found_indices:
            new_item = {field: None for field in allowed_fields}

            for key, value in patch.items():
                target_key = "description" if key == "desc" else key

                if key == "mechanics":
                    new_item["reference_json"] = value
                elif target_key in allowed_fields:
                    new_item[target_key] = value

            result_data.append(new_item)
            new_items_count += 1

    # 5. Массовая обработка ASI (если в патче нет конкретного класса)
    # Если мы видим фичу повышения характеристик, которая осталась пустой - даем ей логику
    for item in result_data:
        if "ability-score-improvement" in item['index_name'] and not item.get('choices_json'):
            # Берем логику из мастер-фичи или дефолтную
            asi_master = patch_map.get("ability-score-improvement")
            if asi_master:
                item["choices_json"] = asi_master.get("choices_json")
                patched_count += 1

    # 6. Сохранение (с бэкапом)
    backup_file = TARGET_FILE + ".bak"
    try:
        if os.path.exists(backup_file):
            os.remove(backup_file)
        os.rename(TARGET_FILE, backup_file)

        with open(OUTPUT_FILE, 'w', encoding='utf-8') as f:
            json.dump(result_data, f, ensure_ascii=False, indent=2)

        logger.info(f"Патч успешно применен!")
        logger.info(f" - Обновлено записей: {patched_count}")
        logger.info(f" - Добавлено новых записей: {new_items_count}")
        logger.info(f" - Создан бэкап: {backup_file}")

    except Exception as e:
        logger.error(f"Ошибка при записи файла: {e}")
        if os.path.exists(backup_file):
            os.rename(backup_file, TARGET_FILE)

if __name__ == "__main__":
    run_patcher()
import json
import os
import re
import logging

# Настройка логирования
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [%(levelname)s] %(message)s'
)
logger = logging.getLogger("FeatureMerger")

def merge_features_patch(output_file='5e-SRD-Features-Updated.json', pattern='part_'):
    # 1. Поиск файлов
    files = [f for f in os.listdir('.') if f.startswith(pattern) and f.endswith('.json')]

    if not files:
        logger.error(f"Файлы патча '{pattern}*.json' не найдены!")
        return

    # 2. Сортировка по номеру в названии
    def extract_number(filename):
        nums = re.findall(r'\d+', filename)
        return int(nums[0]) if nums else 0

    files.sort(key=extract_number)

    logger.info(f"Начинаю сборку патча из {len(files)} частей...")

    merged_features = []
    seen_indices = set()

    # 3. Объединение
    for filename in files:
        logger.info(f"Обработка {filename}...")
        try:
            with open(filename, 'r', encoding='utf-8') as f:
                data = json.load(f)

                if not isinstance(data, list):
                    logger.error(f"Ошибка: {filename} не является списком!")
                    continue

                for record in data:
                    # ВАЖНО: Ищем index_name, а не entity_index
                    idx = record.get("index_name")
                    if not idx:
                        logger.warning(f"Пропущена запись без index_name в {filename}")
                        continue

                    if idx in seen_indices:
                        logger.warning(f"Дубликат способности пропущен: {idx}")
                        continue

                    merged_features.append(record)
                    seen_indices.add(idx)

                logger.info(f"  Добавлено {len(data)} способностей.")

        except Exception as e:
            logger.error(f"Сбой при чтении {filename}: {e}")
            return

    # 4. Сохранение
    logger.info(f"ИТОГО: Собрано {len(merged_features)} уникальных способностей.")

    try:
        # Пытаемся сохранить сразу в папку с данными, если она существует
        target_path = os.path.join('srd_data', output_file) if os.path.exists('srd_data') else output_file

        with open(target_path, 'w', encoding='utf-8') as f_out:
            json.dump(merged_features, f_out, ensure_ascii=False, indent=2)

        logger.info(f"=== ФАЙЛ ОБНОВЛЕНИЯ СОЗДАН: {target_path} ===")
        logger.info("Теперь ты можешь запустить Master Importer для заливки этих данных в таблицу FEATURES.")
    except Exception as e:
        logger.error(f"Ошибка сохранения: {e}")

if __name__ == "__main__":
    merge_features_patch()
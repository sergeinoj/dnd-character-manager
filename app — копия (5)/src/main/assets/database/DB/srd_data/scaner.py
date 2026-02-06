import json
import os
import logging

# Настройка логгера
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [%(levelname)s] %(message)s'
)
logger = logging.getLogger("TargetExtractor")

class StrictSRDExtractor:
    """Извлекает только определения Черт и классовые ASI."""

    @staticmethod
    def is_target(obj):
        idx = obj.get("index_name", "")
        # Условие 1: Это сама Черта (начинается с feat-)
        if idx.startswith("feat-"):
            return True
        # Условие 2: Это классовый ASI (содержит ability-score-improvement)
        if "ability-score-improvement" in idx:
            return True
        # Условие 3: Базовая логика ASI
        if idx == "asi-logic":
            return True
        return False

    def run(self, input_file, output_file):
        if not os.path.exists(input_file):
            logger.error(f"Файл не найден: {input_file}")
            return

        try:
            with open(input_file, 'r', encoding='utf-8') as f:
                source_data = json.load(f)

            # Фильтрация
            filtered = [item for item in source_data if self.is_target(item)]

            # Сохранение в TXT (в формате JSON для читаемости)
            with open(output_file, 'w', encoding='utf-8') as f:
                json.dump(filtered, f, ensure_ascii=False, indent=2)

            logger.info(f"Успех! Извлечено объектов: {len(filtered)}")
            logger.info(f"Результат сохранен в: {output_file}")

        except Exception as e:
            logger.error(f"Ошибка при обработке: {e}")

if __name__ == "__main__":
    # Входные данные
    source_json = "5e-SRD-Features.json"
    target_txt = "feats_and_asi_extracted.txt"

    extractor = StrictSRDExtractor()
    extractor.run(source_json, target_txt)
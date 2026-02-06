import json
import os

def fix_background_features(input_file):
    output_file = "5e-SRD-Features-Fixed.json"

    if not os.path.exists(input_file):
        print(f"Ошибка: Файл {input_file} не найден.")
        return

    with open(input_file, 'r', encoding='utf-8') as f:
        features = json.load(f)

    fixed_count = 0

    for feat in features:
        idx = feat.get("index_name", "")

        # Обрабатываем только "внуков" предысторий
        if idx.startswith("bgf-"):
            choices = feat.get("choices_json")

            if choices and isinstance(choices, dict):
                c_type = choices.get("type")
                choose_count = choices.get("choose", 0)

                # Логика 1: Если это навыки (proficiencies) и выбора по факту нет
                # (количество опций равно количеству choose)
                if c_type == "proficiencies":
                    options = choices.get("from", {}).get("options", [])

                    if len(options) == choose_count and choose_count > 0:
                        # Собираем список для автоматического начисления
                        granted = []
                        for opt in options:
                            item = opt.get("item", {})
                            raw_idx = item.get("index", "")
                            # Убираем префикс skill- если он есть для чистоты базы
                            clean_idx = raw_idx.replace("skill-", "")
                            name = item.get("name", "").replace("Навык: ", "")
                            granted.append({"index": clean_idx, "name": name})

                        # Создаем reference_json (как строку для SQLite)
                        ref_data = {"granted_proficiencies": granted}
                        feat["reference_json"] = json.dumps(ref_data, ensure_ascii=False)

                        # Зануляем выбор, чтобы убрать дропдаун
                        feat["choices_json"] = None
                        fixed_count += 1
                        print(f"FIXED: {idx} -> навыки перенесены в статику.")

                # Логика 2: Если это снаряжение (equipment) внутри фичи-внука
                # Часто в SRD это фиксированный набор, а не выбор
                elif c_type in ["instruments", "artisan-tools", "gaming-sets", "equipment"]:
                    # Если это выбор категории (option_set_type: equipment_category), оставляем как есть.
                    # Но если это массив из одного элемента - тоже в статику.
                    options = choices.get("from", {}).get("options", [])
                    if len(options) == 1 and choose_count == 1:
                        # (Опционально можно добавить логику и для предметов здесь)
                        pass

        # Дополнительная проверка для ВСЕХ фич:
        # Если choices_json пустой массив или объект - делаем null
        if feat.get("choices_json") == [] or feat.get("choices_json") == {}:
            feat["choices_json"] = None

    with open(output_file, 'w', encoding='utf-8') as f:
        json.dump(features, f, ensure_ascii=False, indent=2)

    print(f"\nГотово! Обработано фич: {len(features)}")
    print(f"Исправлено предысторий: {fixed_count}")
    print(f"Новый файл: {output_file}")

if __name__ == "__main__":
    fix_background_features("5e-SRD-Features.json")
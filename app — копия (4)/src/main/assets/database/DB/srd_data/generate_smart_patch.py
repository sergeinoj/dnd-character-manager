import json
import re

AUDIT_FILE = "AUDIT_REPORT.txt"
OUTPUT_FILE = "5e-SRD-Patch-Data.json"

def categorize_entity(entity_id, source_context):
    """Пытается угадать тип сущности по ID и контексту ошибки"""

    # 1. Явные подклассы (из лога аудита)
    if "subclasses" in source_context:
        return "SUBCLASS (Подкласс)"

    # 2. Расовые черты (по ключевым словам и источнику ошибки)
    is_race_file = "Race" in source_context or "Subrace" in source_context
    if is_race_file:
        return "RACIAL TRAIT (Расовая черта)"

    # 3. Классовые умения
    is_class_file = "Class" in source_context or "Level" in source_context
    if is_class_file:
        # Уточняем класс по префиксу
        if "barbarian" in entity_id: return "CLASS FEATURE (Варвар)"
        if "bard" in entity_id: return "CLASS FEATURE (Бард)"
        if "cleric" in entity_id or "channel-divinity" in entity_id: return "CLASS FEATURE (Жрец)"
        if "druid" in entity_id: return "CLASS FEATURE (Друид)"
        if "fighter" in entity_id: return "CLASS FEATURE (Воин)"
        if "monk" in entity_id: return "CLASS FEATURE (Монах)"
        if "paladin" in entity_id: return "CLASS FEATURE (Паладин)"
        if "ranger" in entity_id: return "CLASS FEATURE (Следопыт)"
        if "rogue" in entity_id: return "CLASS FEATURE (Плут)"
        if "sorcerer" in entity_id or "metamagic" in entity_id: return "CLASS FEATURE (Чародей)"
        if "warlock" in entity_id: return "CLASS FEATURE (Колдун)"
        if "wizard" in entity_id: return "CLASS FEATURE (Волшебник)"
        return "CLASS FEATURE (Классовое умение)"

    return "UNKNOWN (Неизвестно)"

def generate_smart_template():
    data_map = {} # Используем словарь, чтобы убрать дубликаты

    try:
        with open(AUDIT_FILE, 'r', encoding='utf-8') as f:
            lines = f.readlines()
            for line in lines:
                # Парсим строку ошибки:
                # [ERROR] BROKEN LINK in 5e-SRD-Races.json: Entity 'halfling' refers to MISSING features -> 'brave'

                # Регулярка захватывает: Имя файла (1), ID сущности (2), Тип потери (3), ID потери (4)
                match = re.search(r"in (.*?): Entity '(.*?)'.*MISSING (.*?) -> '(.*?)'", line)

                if match:
                    filename = match.group(1)
                    missing_type_raw = match.group(3) # features или subclasses
                    missing_id = match.group(4)

                    context_hint = f"{missing_type_raw} (from {filename})"
                    category = categorize_entity(missing_id, context_hint)

                    # Формируем читаемое имя (удаляем дефисы, делаем заглавные)
                    readable_name = missing_id.replace("-", " ").title()

                    # Если ID уже есть, не дублируем, но можем дополнить инфо, если нужно
                    if missing_id not in data_map:
                        data_map[missing_id] = {
                            "index_name": missing_id,
                            "name": f"{readable_name} (TODO)",
                            "description": "TODO: Вставьте описание здесь.",
                            "__CONTEXT__": category, # Это поле не пойдет в БД, это подсказка для человека
                            "__SOURCE_FILE__": filename
                        }

    except FileNotFoundError:
        print(f"Файл {AUDIT_FILE} не найден.")
        return

    # Превращаем словарь в список и сортируем по Категории, потом по Имени
    sorted_data = sorted(data_map.values(), key=lambda x: (x['__CONTEXT__'], x['index_name']))

    with open(OUTPUT_FILE, 'w', encoding='utf-8') as f:
        json.dump(sorted_data, f, ensure_ascii=False, indent=2)

    print(f"Сгенерирован умный шаблон: {OUTPUT_FILE}")
    print(f"Найдено уникальных пропусков: {len(sorted_data)}")
    print("В файле добавлены поля __CONTEXT__ для помощи в заполнении.")

if __name__ == "__main__":
    generate_smart_template()
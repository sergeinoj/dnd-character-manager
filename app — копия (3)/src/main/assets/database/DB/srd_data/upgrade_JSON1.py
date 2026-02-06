import json
import re
import os

# --- КОНФИГУРАЦИЯ ЦЕН ПО РЕДКОСТИ (в медных монетах / cp) ---
MAGIC_PRICES = {
    "Обычный": 5000,        # 50 gp
    "Необычный": 25000,     # 250 gp
    "Редкий": 250000,       # 2500 gp
    "Очень Редкий": 2500000, # 25000 gp
    "Легендарный": 5000000, # 50000 gp
    "Артефакт": 10000000,   # 100000 gp
    "Разное": 5000          # По умолчанию
}

# Реальные цены стартовых наборов (из Player's Handbook)
PACK_PRICES = {
    "burglars-pack": 1600,
    "diplomats-pack": 3900,
    "dungeoneers-pack": 1200,
    "entertainers-pack": 4000,
    "explorers-pack": 1000,
    "priests-pack": 1900,
    "scholars-pack": 4000
}

# [НОВОЕ] Ручные исправления цен для предметов с нулевой стоимостью
MANUAL_FIXES = {
    "alms-box": 50,           # Коробка для милостыни: 5 см
    "block-of-incense": 100,  # Блок благовоний: 1 зм
    "censer": 500,            # Кадило: 5 зм
    "little-bag-of-sand": 1,  # Мешочек с песком: 1 мм
    "small-knife": 50,        # Маленький нож: 5 см
    "string-10-feet": 1,      # Бечевка: 1 мм
    "vestments": 500          # Облачения: 5 зм
}

# Иерархия категорий (Кто в какую папку идет)
CATEGORY_MAPPING = {
    "weapon": "shop-weapons", "simple-weapons": "shop-weapons", "martial-weapons": "shop-weapons",
    "melee-weapons": "shop-weapons", "ranged-weapons": "shop-weapons",
    "armor": "shop-armor", "light-armor": "shop-armor", "medium-armor": "shop-armor",
    "heavy-armor": "shop-armor", "shields": "shop-armor",
    "adventuring-gear": "shop-gear", "standard-gear": "shop-gear", "kits": "shop-gear",
    "artisans-tools": "shop-gear", "tools": "shop-gear", "gaming-sets": "shop-gear",
    "musical-instruments": "shop-gear", "other-tools": "shop-gear",
    "wondrous-items": "shop-magic", "rod": "shop-magic", "potion": "shop-magic",
    "ring": "shop-magic", "scroll": "shop-magic", "staff": "shop-magic", "wand": "shop-magic",
    "mounts-and-vehicles": "shop-transport", "land-vehicles": "shop-transport",
    "waterborne-vehicles": "shop-transport", "mounts-and-other-animals": "shop-transport",
    "equipment-packs": "shop-bundles", "bundles": "shop-bundles"
}

errors_log = []

def log_error(msg):
    print(f"[FIXED/LOG] {msg}")
    errors_log.append(msg)

def get_cost_obj(cp_total):
    """Превращает медяки в красивый объект cost_json"""
    if cp_total >= 100:
        return {"quantity": cp_total // 100, "unit": "gp"}
    elif cp_total >= 10:
        return {"quantity": cp_total // 10, "unit": "sp"}
    else:
        return {"quantity": cp_total, "unit": "cp"}

def parse_cost_text(text):
    """Парсит строки типа '5 gp' или '10 sp' в медяки"""
    if not text or text == "None": return 0
    match = re.search(r"(\d+)\s*(gp|sp|cp)", str(text).lower())
    if match:
        val = int(match.group(1))
        unit = match.group(2)
        if unit == "gp": return val * 100
        if unit == "sp": return val * 10
        return val
    return 0

def process_equipment(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        data = json.load(f)

    for item in data:
        idx = item.get("index_name")
        cp = 0

        # ПРИОРЕТЕТ 1: Ручные правки (alms-box, censer и т.д.)
        if idx in MANUAL_FIXES:
            cp = MANUAL_FIXES[idx]
        # ПРИОРЕТЕТ 2: Цены наборов
        elif idx in PACK_PRICES:
            cp = PACK_PRICES[idx]
        # ПРИОРЕТЕТ 3: Существующий cost_json
        elif "cost_json" in item and item["cost_json"] and item["cost_json"].get("quantity", 0) > 0:
            q = item["cost_json"].get("quantity", 0)
            u = item["cost_json"].get("unit", "gp")
            if u == "gp": cp = q * 100
            elif u == "sp": cp = q * 10
            else: cp = q
        # ПРИОРЕТЕТ 4: Парсинг текстового поля cost
        else:
            cp = parse_cost_text(item.get("cost", ""))

        # Запись по схеме v1.25
        item["cost_cp"] = cp
        item["cost_json"] = json.dumps(get_cost_obj(cp), ensure_ascii=False)

        # Если после всех попыток цена 0 - в лог (кроме технических бандлов)
        if cp == 0 and "bundle" not in idx:
            log_error(f"Предмет {idx} остался с нулевой стоимостью!")

    return data

def process_magic_items(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        data = json.load(f)

    for item in data:
        rarity = item.get("rarity", "Разное")
        cat = item.get("category_index", "")
        cp = MAGIC_PRICES.get(rarity, 5000)

        # Расходники в 2 раза дешевле
        if cat in ["potion", "scroll"]:
            cp = cp // 2

        item["cost_cp"] = cp
        item["cost_json"] = json.dumps(get_cost_obj(cp), ensure_ascii=False)

    return data

def process_categories(file_path):
    masters = [
        {"index_name": "shop-weapons", "name": "Оружие", "parent_index": None},
        {"index_name": "shop-armor", "name": "Доспехи", "parent_index": None},
        {"index_name": "shop-gear", "name": "Снаряжение", "parent_index": None},
        {"index_name": "shop-magic", "name": "Магия", "parent_index": None},
        {"index_name": "shop-transport", "name": "Транспорт", "parent_index": None},
        {"index_name": "shop-bundles", "name": "Услуги и Наборы", "parent_index": None},
    ]

    with open(file_path, 'r', encoding='utf-8') as f:
        data = json.load(f)

    for cat in data:
        idx = cat.get("index_name")
        cat["parent_index"] = CATEGORY_MAPPING.get(idx, "shop-gear")
        if "items_json" in cat:
            del cat["items_json"]

    return masters + data

def main():
    # Список файлов для обработки
    files = {
        "eq": "5e-SRD-Equipment.json",
        "mg": "5e-SRD-Magic-Items.json",
        "ct": "5e-SRD-Equipment-Categories.json"
    }

    # Обработка Оборудования
    if os.path.exists(files["eq"]):
        fixed = process_equipment(files["eq"])
        with open("FIXED_" + files["eq"], 'w', encoding='utf-8') as f:
            json.dump(fixed, f, ensure_ascii=False, indent=2)

    # Обработка Магии
    if os.path.exists(files["mg"]):
        fixed = process_magic_items(files["mg"])
        with open("FIXED_" + files["mg"], 'w', encoding='utf-8') as f:
            json.dump(fixed, f, ensure_ascii=False, indent=2)

    # Обработка Категорий
    if os.path.exists(files["ct"]):
        fixed = process_categories(files["ct"])
        with open("FIXED_" + files["ct"], 'w', encoding='utf-8') as f:
            json.dump(fixed, f, ensure_ascii=False, indent=2)

    with open("corrector_errors_log.txt", 'w', encoding='utf-8') as f:
        f.write("\n".join(errors_log))

    print(f"--- Исправление завершено. Оставшиеся аномалии: {len(errors_log)} ---")

if __name__ == "__main__":
    main()
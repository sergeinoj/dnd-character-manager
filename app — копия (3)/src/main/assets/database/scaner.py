import sqlite3
import json
import os

DB_NAME = 'dnd_clean.db'
OUTPUT_FILE = 'class_equipment.txt'

def scan_universal_equipment():
    if not os.path.exists(DB_NAME):
        print(f"Файл {DB_NAME} не найден!")
        return

    conn = sqlite3.connect(DB_NAME)
    cursor = conn.cursor()

    items_cache = {}

    # 1. ЗАГРУЗКА ОБЩЕГО СНАРЯЖЕНИЯ (Рюкзаки, факелы, инструменты)
    cursor.execute("SELECT index_name, name, cost_json, weight FROM equipment")
    for row in cursor.fetchall():
        idx, name, cost_raw, weight = row
        items_cache[idx] = {"name": name, "cost": format_cost(cost_raw), "weight": weight, "src": "EQUIP"}

    # 2. ЗАГРУЗКА ОРУЖИЯ (Мечи, топоры, луки)
    # В таблице weapons колонка имени называется 'label'
    try:
        cursor.execute("SELECT index_name, label, cost, weight FROM weapons")
        for row in cursor.fetchall():
            idx, name, cost, weight = row
            items_cache[idx] = {"name": name, "cost": cost, "weight": weight, "src": "WEAPON"}
    except: print("Таблица weapons не найдена или структура иная")

    # 3. ЗАГРУЗКА ДОСПЕХОВ (Кольчуги, кожанки, щиты)
    try:
        cursor.execute("SELECT index_name, name, cost, weight FROM armor")
        for row in cursor.fetchall():
            idx, name, cost, weight = row
            items_cache[idx] = {"name": name, "cost": cost, "weight": weight, "src": "ARMOR"}
    except: print("Таблица armor не найдена или структура иная")

    # 4. ЗАГРУЗКА КАТЕГОРИЙ
    categories = {}
    cursor.execute("SELECT category_index, item_index FROM equipment_category_links")
    for cat_idx, item_idx in cursor.fetchall():
        if cat_idx not in categories: categories[cat_idx] = []
        categories[cat_idx].append(item_idx)

    # 5. СКАНИРОВАНИЕ КЛАССОВ
    cursor.execute("SELECT index_name, name FROM classes")
    all_classes = cursor.fetchall()

    with open(OUTPUT_FILE, 'w', encoding='utf-8') as f:
        for c_idx, c_name in all_classes:
            f.write(f"=== КЛАСС: {c_name.upper()} ===\n")

            cursor.execute("""
                SELECT index_name, name, choices_json
                FROM features
                WHERE class_index = ? AND (level = 1 OR level IS NULL)
                AND (ui_group = 'INVENTORY' OR index_name LIKE '%equip%')
            """, (c_idx,))

            features = cursor.fetchall()
            if not features:
                f.write("  [Инвентарные способности не найдены]\n")

            for f_idx, f_name, f_choices_raw in features:
                f.write(f"  Способность: {f_name}\n")
                if f_choices_raw:
                    try:
                        choices = json.loads(f_choices_raw)
                        if isinstance(choices, dict): choices = [choices]
                        for choice in choices:
                            process_node_recursive(choice, categories, items_cache, f, indent="    ")
                    except: pass
                f.write("\n")
            f.write("-" * 80 + "\n\n")

    conn.close()
    print(f"Готово! Результат в {OUTPUT_FILE}")

def format_cost(cost_raw):
    """Превращает {'quantity': 10, 'unit': 'gp'} в '10 gp'"""
    if not cost_raw: return "0"
    try:
        data = json.loads(cost_raw)
        return f"{data.get('quantity', 0)} {data.get('unit', '')}"
    except: return str(cost_raw)

def process_node_recursive(node, categories_map, items_map, f, indent):
    if not isinstance(node, dict): return

    if "desc" in node:
        f.write(f"{indent}Выбор: {node['desc']}\n")

    # Если в узле прямо указан предмет
    if "item" in node and isinstance(node["item"], dict):
        item_idx = node["item"].get("index")
        write_item(item_idx, items_map, f, indent + "  * ")

    # Если в узле категория
    if node.get("option_set_type") == "equipment_category" or "equipment_category" in node:
        cat_data = node.get("equipment_category", node)
        cat_idx = cat_data.get("index") if isinstance(cat_data, dict) else None
        if cat_idx:
            f.write(f"{indent}  -> Категория '{cat_idx}':\n")
            for i_idx in categories_map.get(cat_idx, []):
                write_item(i_idx, items_map, f, indent + "     - ")

    # Обход вложенностей (options, from, choice)
    for key in ["options", "from", "choice"]:
        if key in node:
            val = node[key]
            if isinstance(val, list):
                for sub in val: process_node_recursive(sub, categories_map, items_map, f, indent + "  ")
            else:
                process_node_recursive(val, categories_map, items_map, f, indent)

def write_item(item_idx, items_map, f, prefix):
    item = items_map.get(item_idx)
    if item:
        f.write(f"{prefix}{item['name']} ({item_idx}) [Цена: {item['cost']} | Вес: {item['weight']}]\n")
    else:
        f.write(f"{prefix}{item_idx} [!] НЕ НАЙДЕН НИ В ОДНОЙ ТАБЛИЦЕ\n")

if __name__ == "__main__":
    scan_universal_equipment()
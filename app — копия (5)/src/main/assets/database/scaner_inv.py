import sqlite3
import json
import os
import sys

# ==========================================
# CONFIGURATION
# ==========================================
DB_PATH = 'dnd_clean.db'
OUTPUT_PATH = 'dnd_relational_map_v2.txt'

def parse_json(data):
    if not data: return []
    try:
        res = json.loads(data)
        return res if isinstance(res, list) else [res]
    except: return []

def get_items_detailed(cursor, cat_index):
    """
    Собирает предметы с указанием типа связи:
    [DIRECT] - предмет сам говорит "я принадлежу этой категории".
    [LINK]   - предмет привязан через таблицу связей.
    """
    items_found = []

    # 1. Прямые ссылки (item.category_index == cat_index)
    # Ищем во всех предметных таблицах
    item_tables = ['weapons', 'armor', 'equipment', 'magic_items']

    for table in item_tables:
        try:
            rows = cursor.execute(f"SELECT index_name, name FROM {table} WHERE category_index = ?", (cat_index,)).fetchall()
            for r in rows:
                items_found.append(f"[DIRECT][{table[0].upper()}] {r['name']} ({r['index_name']})")
        except sqlite3.Error: pass

    # 2. Ссылки через таблицу связей (equipment_category_links)
    links = cursor.execute("SELECT item_index FROM equipment_category_links WHERE category_index = ?", (cat_index,)).fetchall()

    for link in links:
        l_idx = link['item_index']
        # Пытаемся найти, что это за предмет
        found_info = None
        for table in item_tables:
            try:
                it = cursor.execute(f"SELECT name, index_name, category_index FROM {table} WHERE index_name = ?", (l_idx,)).fetchone()
                if it:
                    # Проверяем, не дублируем ли мы прямую связь
                    if it['category_index'] == cat_index:
                        # Это уже найдено в шаге 1, пропускаем чтобы не дублировать визуально,
                        # ИЛИ помечаем как [BOTH] если хочешь видеть дубли.
                        # Сейчас пропускаем.
                        found_info = "SKIP"
                    else:
                        found_info = f"[LINK  ][{table[0].upper()}] {it['name']} ({l_idx})"
                    break
            except: pass

        if found_info == "SKIP":
            continue
        elif found_info:
            items_found.append(found_info)
        else:
            items_found.append(f"[GHOST LINK] ⚠️ ID: {l_idx} (Not found in any table)")

    return sorted(list(set(items_found)))

def recursive_category_walker(cursor, parent_index, level, file_handle):
    """
    Рекурсивно обходит дерево категорий.
    """
    indent = "    " * level

    # SQL: Найти категории, у которых parent_index равен переданному (или NULL, если это корень)
    if parent_index is None:
        query = "SELECT * FROM equipment_categories WHERE parent_index IS NULL ORDER BY name"
        params = ()
    else:
        query = "SELECT * FROM equipment_categories WHERE parent_index = ? ORDER BY name"
        params = (parent_index,)

    categories = cursor.execute(query, params).fetchall()

    for cat in categories:
        cat_name = cat['name']
        cat_idx = cat['index_name']

        # Пишем заголовок категории
        icon = "📂" if level > 0 else "🌍"
        file_handle.write(f"\n{indent}{icon} [CAT] {cat_name} ({cat_idx})\n")

        # Получаем и пишем предметы этой категории
        items = get_items_detailed(cursor, cat_idx)
        if not items:
            file_handle.write(f"{indent}    (пусто)\n")
        else:
            for item in items:
                file_handle.write(f"{indent}    ├─ {item}\n")

        # РЕКУРСИЯ: Ищем детей этой категории
        recursive_category_walker(cursor, cat_idx, level + 1, file_handle)

def scan():
    if not os.path.exists(DB_PATH):
        print(f"Ошибка: {DB_PATH} не найден.")
        return

    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    cursor = conn.cursor()

    print("Запуск глубокого сканирования...")

    with open(OUTPUT_PATH, 'w', encoding='utf-8') as f:
        f.write("=== D&D 5.1 DEEP SCAN REPORT (v2.0) ===\n")
        f.write("Mode: Recursive Hierarchy Check\n")
        f.write("========================================\n\n")

        # --- 1. МАГАЗИН (ГЛАВНАЯ ЦЕЛЬ) ---
        f.write("--- SHOP HIERARCHY (Unlimited Depth) ---\n")
        # Запускаем рекурсию с корня (parent_index = None)
        recursive_category_walker(cursor, None, 0, f)

        # --- 2. КРАТКАЯ СВОДКА ПО ОСТАЛЬНОМУ ---
        f.write("\n\n--- QUICK STATS ---\n")
        tables = ['classes', 'races', 'backgrounds', 'spells', 'features', 'magic_items', 'weapons', 'armor', 'equipment']
        for t in tables:
            try:
                count = cursor.execute(f"SELECT COUNT(*) FROM {t}").fetchone()[0]
                f.write(f"Table '{t}': {count} records\n")
            except:
                f.write(f"Table '{t}': ERROR/MISSING\n")

    conn.close()
    print(f"Готово. Отчет сохранен в: {OUTPUT_PATH}")
    print("Проверь файл. Теперь там должна быть вложенность 3-го уровня (Инструменты -> Музыкальные).")

if __name__ == "__main__":
    scan()
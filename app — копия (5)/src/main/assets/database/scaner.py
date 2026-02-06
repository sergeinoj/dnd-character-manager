# Имя файла: cleric_lvl1_audit.py
import sqlite3
import json
import os

def analyze_cleric_lvl1():
    db_path = 'dnd_clean.db'
    output_file = 'cleric_lvl1_report.txt'

    if not os.path.exists(db_path):
        print(f"Ошибка: Файл {db_path} не найден.")
        return

    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()

    with open(output_file, 'w', encoding='utf-8') as f:
        f.write("=== ПОЛНЫЙ АУДИТ ЖРЕЦА: 1 УРОВЕНЬ ===\n\n")

        # 1. ЗАГОВОРЫ И СЛОТЫ (Чистая прогрессия)
        f.write("--- 1. ПРОГРЕССИЯ (Заговоры и Ячейки) ---\n")
        # Берем только ту строку, где JSON не пустой
        cursor.execute("""
            SELECT spellcasting_json FROM progression
            WHERE class_index = 'cleric' AND level = 1
            AND spellcasting_json IS NOT NULL AND spellcasting_json != ''
        """)
        rows = cursor.fetchall()

        valid_found = False
        for (sj,) in rows:
            try:
                data = json.loads(sj)
                if 'cantrips_known' in data:
                    f.write(f"Известно заговоров жреца: {data['cantrips_known']}\n")
                    f.write(f"Ячеек 1-го уровня: {data.get('spell_slots_level_1', 0)}\n")
                    valid_found = True
                    break
            except: continue
        if not valid_found: f.write("Внимание: Валидная строка прогрессии не найдена!\n")
        f.write("\n")

        # 2. ДОМЕННЫЕ ЗАКЛИНАНИЯ (Автоматические)
        f.write("--- 2. АВТОМАТИЧЕСКИЕ ЗАКЛИНАНИЯ ПО ДОМЕНАМ (1 уровень) ---\n")
        cursor.execute("""
            SELECT subclass_index, name, spell_show_json, choices_json
            FROM features
            WHERE class_index = 'cleric' AND level = 1
            AND (spell_show_json IS NOT NULL OR choices_json LIKE '%spell%')
        """)
        for sub, name, s_show, c_json in cursor.fetchall():
            f.write(f"Домен: {sub if sub else 'Базовый'} (Способность: {name})\n")
            if s_show and s_show != '[]':
                f.write(f"  [!] Даются автоматически: {s_show}\n")
            if c_json and 'spell' in c_json:
                f.write(f"  [?] Требуют выбора: {c_json}\n")
        f.write("\n")

        # 3. ПУЛ ДЛЯ ПОДГОТОВКИ (Весь список заклинаний жреца 1 уровня)
        f.write("--- 3. ДОСТУПНЫЕ ЗАКЛИНАНИЯ ДЛЯ ПОДГОТОВКИ (Prepared Pool) ---\n")
        f.write("(Жрец 1 уровня может подготовить [Мод. Мудрости + 1] заклинаний из этого списка)\n")

        # Ищем все заклинания 1 уровня, где в classes_json есть "cleric"
        cursor.execute("""
            SELECT index_name, name FROM spells
            WHERE level = 1 AND classes_json LIKE '%"cleric"%'
            ORDER BY name ASC
        """)
        cleric_lvl1_spells = cursor.fetchall()
        f.write(f"Всего в базе заклинаний Жреца 1-го уровня: {len(cleric_lvl1_spells)}\n")
        for idx, name in cleric_lvl1_spells:
            f.write(f"  - {name} [{idx}]\n")

        # 4. ЗАГОВОРЫ (Список для выбора)
        f.write("\n--- 4. ДОСТУПНЫЕ ЗАГОВОРЫ ДЛЯ ВЫБОРА ---\n")
        cursor.execute("""
            SELECT index_name, name FROM spells
            WHERE level = 0 AND classes_json LIKE '%"cleric"%'
            ORDER BY name ASC
        """)
        cleric_cantrips = cursor.fetchall()
        for idx, name in cleric_cantrips:
            f.write(f"  - {name} [{idx}]\n")

    conn.close()
    print(f"Аудит завершен. Результаты в {output_file}")

if __name__ == "__main__":
    analyze_cleric_lvl1()
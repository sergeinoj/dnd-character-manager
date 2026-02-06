import sqlite3
import json
import os

# Конфигурация
DB_NAME = "dnd_clean.db"  # Убедитесь, что файл базы в той же папке
OUTPUT_FILE = "sorcerer_audit_report.txt"

def audit_sorcerer():
    if not os.path.exists(DB_NAME):
        print(f"Ошибка: Файл {DB_NAME} не найден.")
        return

    conn = sqlite3.connect(DB_NAME)
    conn.row_factory = sqlite3.Row
    cursor = conn.cursor()

    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        f.write("=== D&D 5.1 SORCERER DATABASE AUDIT REPORT ===\n")
        f.write(f"Database: {DB_NAME}\n\n")

        # 1. ТАБЛИЦА CLASSES
        f.write("--- 1. ТАБЛИЦА: classes ---\n")
        cursor.execute("SELECT * FROM classes WHERE index_name = 'sorcerer'")
        sorcerer_class = cursor.fetchone()
        if sorcerer_class:
            for key in sorcerer_class.keys():
                f.write(f"{key}: {sorcerer_class[key]}\n")
        f.write("\n")

        # 2. ПОДКЛАССЫ
        f.write("--- 2. ТАБЛИЦА: subclasses ---\n")
        cursor.execute("SELECT * FROM subclasses WHERE class_index = 'sorcerer'")
        for row in cursor.fetchall():
            f.write(f"ID: {row['index_name']} | Name: {row['name']}\n")
        f.write("\n")

        # 3. ТАБЛИЦА PROGRESSION (Разбор уровней)
        f.write("--- 3. ТАБЛИЦА: progression (Поуровневый разбор) ---\n")
        cursor.execute("SELECT * FROM progression WHERE class_index = 'sorcerer' ORDER BY level")
        progression_data = cursor.fetchall()

        all_feature_indices = []
        for row in progression_data:
            f.write(f"Level {row['level']}:\n")
            f.write(f"  - Features JSON: {row['feature_indices_json']}\n")
            f.write(f"  - Spellcasting JSON: {row['spellcasting_json']}\n")

            try:
                indices = json.loads(row['feature_indices_json'])
                all_feature_indices.extend(indices)
            except:
                f.write("  [!] Ошибка парсинга feature_indices_json\n")
        f.write("\n")

        # 4. ТАБЛИЦА FEATURES (Способности)
        f.write("--- 4. ТАБЛИЦА: features (Детализация способностей) ---\n")
        # Собираем уникальные индексы из прогрессии + те, где class_index = sorcerer
        cursor.execute("SELECT * FROM features WHERE class_index = 'sorcerer' OR index_name IN ({})".format(
            ','.join(['?'] * len(all_feature_indices))), all_feature_indices)

        magic_choice_features = []
        for row in cursor.fetchall():
            f.write(f"Index: {row['index_name']} | Name: {row['name']} | UI_Group: {row['ui_group']}\n")
            if row['choices_json']:
                f.write(f"  - Choices: {row['choices_json']}\n")
                if "spell" in row['choices_json'].lower():
                    magic_choice_features.append(row['index_name'])
            if row['spell_show_json']:
                f.write(f"  - Spell Show: {row['spell_show_json']}\n")
            f.write("-" * 30 + "\n")
        f.write("\n")

        # 5. АНАЛИЗ ДУБЛИРОВАНИЯ МАГИИ
        f.write("--- 5. АНАЛИЗ ДУБЛИРОВАННЫХ ВЫБОРОВ МАГИИ ---\n")
        if magic_choice_features:
            f.write(f"Обнаружено {len(magic_choice_features)} способностей с выбором заклинаний:\n")
            for feat in magic_choice_features:
                f.write(f"  [!] {feat}\n")
            f.write("\nСовет: Если на 1 уровне в прогрессии указано более одного 'spell-choice', "
                    "выборы будут накладываться друг на друга в UI.\n\n")

        # 6. СПИСОК ЗАКЛИНАНИЙ (Для проверки привязки)
        f.write("--- 6. ТАБЛИЦА: spells (Привязка к классу) ---\n")
        cursor.execute("SELECT index_name, name, level FROM spells WHERE classes_json LIKE '%\"sorcerer\"%'")
        spells = cursor.fetchall()
        f.write(f"Всего заклинаний в списке чародея: {len(spells)}\n")
        # Группировка по кругам
        for lvl in range(10):
            lvl_spells = [s['name'] for s in spells if s['level'] == lvl]
            f.write(f"  Level {lvl}: {len(lvl_spells)} заклинаний\n")

    conn.close()
    print(f"Анализ завершен. Результат сохранен в {OUTPUT_FILE}")

if __name__ == "__main__":
    audit_sorcerer()
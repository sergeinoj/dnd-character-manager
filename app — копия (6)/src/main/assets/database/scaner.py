# Имя файла: deep_background_analyzer.py
import sqlite3
import json
import os

def deep_analyze(db_path="dnd_v1.db"):
    if not os.path.exists(db_path):
        print(f"Ошибка: Файл базы {db_path} не найден.")
        return

    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()

    # Имя выходного файла
    output_file = "background_architecture_full.txt"

    with open(output_file, "w", encoding="utf-8") as f:
        f.write("==================================================================================\n")
        f.write("D&D 5.1 SRD: BACKGROUND DEEP ARCHITECTURE MAP (v1.26)\n")
        f.write("Full Branch Resolution: Background -> Linked Features -> Choice Logic\n")
        f.write("==================================================================================\n")

        cursor.execute("SELECT * FROM backgrounds ORDER BY name ASC")
        backgrounds = cursor.fetchall()
        bg_cols = [d[0] for d in cursor.description]

        for bg_raw in backgrounds:
            bg = dict(zip(bg_cols, bg_raw))
            f.write(f"\nBACKGROUND: {bg['name'].upper()} ({bg['index_name']})\n")
            f.write(f"Starting Gold: {bg['starting_gold']} gp\n")

            # 1. Статическое снаряжение
            if bg['starting_equipment_json']:
                try:
                    eq = json.loads(bg['starting_equipment_json'])
                    f.write(f"    STATIC EQUIPMENT: {eq}\n")
                except: pass

            # 2. Раскрытие веток фич
            if bg['feature_indices_json']:
                indices = json.loads(bg['feature_indices_json'])
                f.write(f"    RESOLVING LINKED FEATURES ({len(indices)} branches):\n")

                for idx in indices:
                    cursor.execute("SELECT * FROM features WHERE index_name = ?", (idx,))
                    feat_row = cursor.fetchone()

                    if feat_row:
                        feat_cols = [d[0] for d in cursor.description]
                        feat = dict(zip(feat_cols, feat_row))

                        f.write(f"    -> BRANCH: [{feat['index_name']}] {feat['name']}\n")
                        f.write(f"       UI_GROUP: {feat['ui_group'] or 'GENERAL'}\n")

                        # Раскрываем логику выбора (Choices)
                        if feat['choices_json']:
                            try:
                                choices = json.loads(feat['choices_json'])
                                f.write(f"       >>> CHOICE LOGIC: {json.dumps(choices, ensure_ascii=False)}\n")
                            except: f.write("       >>> CHOICE LOGIC: [Malformed JSON]\n")

                        # Раскрываем метаданные (Reference)
                        if feat['reference_json']:
                            try:
                                ref = json.loads(feat['reference_json'])
                                f.write(f"       >>> REFERENCE DATA: {json.dumps(ref, ensure_ascii=False)}\n")
                            except: pass

                        # Проверка вложенных заклинаний
                        if feat['spell_show_json']:
                            f.write(f"       >>> GRANTED SPELLS: {feat['spell_show_json']}\n")
                    else:
                        f.write(f"    -> BRANCH: [{idx}] !!! NOT FOUND IN FEATURES TABLE !!!\n")

            # 3. Fluff (Tables)
            f.write(f"    FLUFF TABLES: Traits({count_json(bg['personality_traits_json'])}) | Ideals({count_json(bg['ideals_json'])}) | Bonds({count_json(bg['bonds_json'])}) | Flaws({count_json(bg['flaws_json'])})\n")
            f.write("-" * 80 + "\n")

    conn.close()
    print(f"Анализ завершен. Результат в файле: {output_file}")

def count_json(raw):
    try: return len(json.loads(raw)) if raw else 0
    except: return 0

if __name__ == "__main__":
    # Укажи здесь путь к своей БД
    db_default = "dnd_clean.db"
    deep_analyze(db_default)
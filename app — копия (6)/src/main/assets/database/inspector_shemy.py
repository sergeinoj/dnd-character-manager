# Имя файла: db_inspector.py
import sqlite3
import os
import argparse

def inspect_database(db_path):
    """
    Подключается к базе данных SQLite и выводит её полную схему.
    """
    if not os.path.exists(db_path):
        print(f"Ошибка: Файл базы данных не найден по пути: {db_path}")
        return

    conn = None
    try:
        # Устанавливаем соединение с базой данных
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()

        # 1. Получаем список всех таблиц в базе данных
        cursor.execute("SELECT name FROM sqlite_master WHERE type='table';")
        tables = cursor.fetchall()

        if not tables:
            print(f"В базе данных '{db_path}' таблицы не найдены.")
            return

        print(f"🔍 Анализ схемы базы данных: {db_path}\n")

        # 2. Для каждой таблицы получаем и выводим её структуру
        for table_name_tuple in tables:
            table_name = table_name_tuple[0]
            print(f"--- Таблица: {table_name} ---")

            # PRAGMA table_info() - это специальная команда SQLite для получения схемы
            cursor.execute(f"PRAGMA table_info('{table_name}');")
            columns = cursor.fetchall()

            # Форматированный вывод
            header = "| {:<25} | {:<15} | {:<10} | {:<10} | {:<12} |".format("Имя колонки", "Тип", "Not Null", "По умолч.", "PK (Ключ)")
            print(header)
            print("-" * len(header))

            for col in columns:
                # col[0] - cid, col[1] - name, col[2] - type, col[3] - notnull, col[4] - dflt_value, col[5] - pk
                col_name = col[1]
                col_type = col[2]
                not_null = bool(col[3])
                default_val = col[4] if col[4] is not None else "NULL"
                is_pk = "Да" if col[5] > 0 else "Нет"

                row = "| {:<25} | {:<15} | {:<10} | {:<10} | {:<12} |".format(col_name, col_type, str(not_null), str(default_val), is_pk)
                print(row)

            print("\n")

    except sqlite3.Error as e:
        print(f"Ошибка при работе с SQLite: {e}")
    finally:
        if conn:
            conn.close()

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Инспектор схемы базы данных SQLite.")
    parser.add_argument("db_file", type=str, help="Путь к файлу .db для анализа.")

    args = parser.parse_args()
    inspect_database(args.db_file)
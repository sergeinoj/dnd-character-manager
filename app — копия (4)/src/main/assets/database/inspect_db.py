import sqlite3

def inspect_db(db_path):
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()

    # Получаем список всех таблиц
    cursor.execute("SELECT name FROM sqlite_master WHERE type='table';")
    tables = cursor.fetchall()

    print(f"--- DATABASE INSPECTION: {db_path} ---")

    for table in tables:
        table_name = table[0]
        if table_name == 'sqlite_sequence': continue

        print(f"\nTABLE: {table_name}")
        cursor.execute(f"PRAGMA table_info({table_name});")
        columns = cursor.fetchall()

        # Индексы
        cursor.execute(f"PRAGMA index_list({table_name});")
        indexes = cursor.fetchall()

        print(f"{'ID':<3} | {'Name':<25} | {'Type':<10} | {'NotNull':<8} | {'PK':<3}")
        print("-" * 60)
        for col in columns:
            # col: (cid, name, type, notnull, dflt_value, pk)
            print(f"{col[0]:<3} | {col[1]:<25} | {col[2]:<10} | {bool(col[3]):<8} | {bool(col[5]):<3}")

        if indexes:
            print("  Indexes:")
            for idx in indexes:
                print(f"    - {idx[1]} (Unique: {bool(idx[2])})")

    conn.close()

if __name__ == "__main__":
    inspect_db('dnd_clean.db') # Убедись, что путь правильный
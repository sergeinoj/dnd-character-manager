import sqlite3

def verify():
    conn = sqlite3.connect("dnd_clean.db")
    cursor = conn.cursor()

    tables = [
        "classes", "subclasses", "progression", "features",
        "races", "subraces", "backgrounds", "spells",
        "equipment", "weapons", "armor", "magic_items"
    ]

    print(f"{'TABLE NAME':<20} | {'COUNT':<10}")
    print("-" * 35)

    for table in tables:
        try:
            cursor.execute(f"SELECT COUNT(*) FROM {table}")
            count = cursor.fetchone()[0]
            print(f"{table:<20} | {count:<10}")
        except Exception as e:
            print(f"{table:<20} | ERROR: {e}")

    # Выведем случайное заклинание для доказательства жизни
    print("\n[DATA SAMPLE: SPELLS]")
    cursor.execute("SELECT name, level, school FROM spells ORDER BY RANDOM() LIMIT 1")
    sample = cursor.fetchone()
    if sample:
        print(f"Случайное заклинание: {sample[0]} (Круг: {sample[1]}, Школа: {sample[2]})")

    # Выведем случайную фичу (проверим кастомные подрасы)
    print("\n[DATA SAMPLE: FEATURES]")
    cursor.execute("SELECT index_name, name FROM features WHERE background_index IS NOT NULL OR subrace_index IS NOT NULL ORDER BY RANDOM() LIMIT 1")
    sample_feat = cursor.fetchone()
    if sample_feat:
        print(f"Случайная кастомная фича: {sample_feat[1]} (Index: {sample_feat[0]})")

    conn.close()

if __name__ == "__main__":
    verify()
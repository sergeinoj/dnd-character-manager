# Имя файла: emergency_fix.py
import json
import os

def fix_file_with_decoder(filename):
    print(f"--- Diagnosing {filename} ---")
    if not os.path.exists(filename):
        print("  File not found.")
        return

    with open(filename, 'r', encoding='utf-8') as f:
        content = f.read().strip()

    decoder = json.JSONDecoder()
    pos = 0
    all_items = []
    chunk_count = 0

    while pos < len(content):
        # Пропускаем пробелы между кусками
        while pos < len(content) and content[pos].isspace():
            pos += 1

        if pos >= len(content):
            break

        try:
            # Пытаемся прочитать следующий JSON-объект с текущей позиции
            obj, end_idx = decoder.raw_decode(content[pos:])

            # Если это список — расширяем наш общий список
            if isinstance(obj, list):
                all_items.extend(obj)
                print(f"  Chunk {chunk_count+1}: Found list with {len(obj)} items.")
            else:
                # Если это одиночный объект — добавляем его
                all_items.append(obj)
                print(f"  Chunk {chunk_count+1}: Found single object.")

            pos += end_idx
            chunk_count += 1

        except json.JSONDecodeError as e:
            print(f"  CRITICAL ERROR at char {pos}: {e}")
            # Пытаемся пропустить мусор (например, запятую между массивами)
            pos += 1

    if chunk_count > 0:
        print(f"  Result: Merged {chunk_count} chunks into {len(all_items)} total items.")

        # Перезаписываем файл правильной версией
        with open(filename, 'w', encoding='utf-8') as f:
            json.dump(all_items, f, ensure_ascii=False, indent=2)
        print(f"  SUCCESS: {filename} rewritten correctly.")
    else:
        print("  No JSON data found.")

# Список файлов для лечения
files_to_check = [
    '5e-SRD-Features.json'
]

if __name__ == "__main__":
    for f in files_to_check:
        fix_file_with_decoder(f)
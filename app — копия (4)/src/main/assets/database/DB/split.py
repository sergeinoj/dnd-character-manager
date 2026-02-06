# Имя файла: split_spells.py
# --- НАЧАЛО ФАЙЛА ---
import json
import os
import math

def split_json(source_file, parts_count=6):
    if not os.path.exists(source_file):
        print(f"Ошибка: Файл {source_file} не найден!")
        return

    print(f"Читаю {source_file}...")
    with open(source_file, 'r', encoding='utf-8') as f:
        data = json.load(f)

    if not isinstance(data, list):
        print("Ошибка: Корневой элемент JSON должен быть списком (массивом)!")
        return

    total_items = len(data)
    items_per_file = math.ceil(total_items / parts_count)

    print(f"Всего заклинаний: {total_items}")
    print(f"Будет создано файлов: {parts_count}")
    print(f"Примерно по {items_per_file} заклинаний в каждом.")

    for i in range(parts_count):
        start_idx = i * items_per_file
        end_idx = start_idx + items_per_file
        chunk = data[start_idx:end_idx]

        if not chunk:
            break

        output_filename = f"part_6{i+1}.json"
        with open(output_filename, 'w', encoding='utf-8') as f_out:
            # indent=2 гарантирует красивую структуру в много строк
            json.dump(chunk, f_out, ensure_ascii=False, indent=2)

        print(f" -> Сохранен файл: {output_filename} ({len(chunk)} объектов)")

    print("\nГотово! Теперь переводить будет гораздо легче.")

if __name__ == "__main__":
    split_json('5e-SRD-Features.json', parts_count=6)
# --- КОНЕЦ ФАЙЛА ---
# Имя файла: split_spells.py
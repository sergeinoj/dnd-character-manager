import sqlite3
import json
import os
import logging

# Настройка Lead-уровня логирования
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [%(levelname)s] %(message)s'
)
logger = logging.getLogger("MasterImporter")

class SRDImporter:
    def __init__(self, db_path, json_dir, ddl_path):
        self.db_path = db_path
        self.json_dir = json_dir
        self.ddl_path = ddl_path
        self.conn = sqlite3.connect(db_path)
        self.cursor = self.conn.cursor()

    def setup_database(self):
        """Инициализация схемы на основе предоставленного DDL."""
        logger.info(f"Initializing schema from {self.ddl_path}...")
        with open(self.ddl_path, 'r', encoding='utf-8') as f:
            ddl_script = f.read()
        self.cursor.executescript(ddl_script)
        self.conn.commit()

    def get_table_columns(self, table_name):
        """Получает список колонок таблицы из БД."""
        self.cursor.execute(f"PRAGMA table_info({table_name})")
        return [info[1] for info in self.cursor.fetchall()]

    def serialize_value(self, value):
        """Приводит значение к формату SQLite (JSON string или скаляр)."""
        if value is None:
            return None
        if isinstance(value, (dict, list)):
            return json.dumps(value, ensure_ascii=False)
        if isinstance(value, bool):
            return 1 if value else 0
        return value

    def upsert(self, table_name, data_dict):
        """Идемпотентная вставка данных."""
        columns = self.get_table_columns(table_name)
        # Фильтруем данные: только те ключи, что есть в колонках БД
        valid_data = {k: self.serialize_value(v) for k, v in data_dict.items() if k in columns}

        # Логгируем несмапленные поля
        unmapped = set(data_dict.keys()) - set(columns) - {'url'}
        if unmapped:
            logger.warning(f"[{table_name}] Unmapped fields detected: {unmapped}")

        placeholders = ", ".join(["?"] * len(valid_data))
        col_names = ", ".join(valid_data.keys())
        sql = f"INSERT OR REPLACE INTO {table_name} ({col_names}) VALUES ({placeholders})"

        self.cursor.execute(sql, list(valid_data.values()))

    def import_equipment(self, file_path):
        """Специфичный импорт для Equipment (делит на 3 таблицы)."""
        with open(file_path, 'r', encoding='utf-8') as f:
            data = json.load(f)

        for item in data:
            cat = item.get("category_index", "").lower()
            if "weapon" in cat:
                self.upsert("weapons", item)
            elif "armor" in cat:
                self.upsert("armor", item)
            else:
                self.upsert("equipment", item)

    def import_categories(self, file_path):
        """Специфичный импорт для категорий и линков."""
        with open(file_path, 'r', encoding='utf-8') as f:
            data = json.load(f)

        for cat in data:
            # 1. Сама категория
            self.upsert("equipment_categories", cat)
            # 2. Линки предметов
            cat_idx = cat.get("index_name")
            items = json.loads(cat.get("items_json", "[]"))
            for item_idx in items:
                self.cursor.execute(
                    "INSERT OR IGNORE INTO equipment_category_links (category_index, item_index) VALUES (?, ?)",
                    (cat_idx, item_idx)
                )

    def run(self):
        # Соответствие файлов и таблиц
        mapping = {
            "5e-SRD-Alignments.json": "alignments",
            "5e-SRD-Backgrounds.json": "backgrounds",
            "5e-SRD-Classes.json": "classes",
            "5e-SRD-Damage-Types.json": "damage_types",
            "5e-SRD-Features.json": "features",
            "5e-SRD-Languages.json": "languages",
            "5e-SRD-Levels.json": "progression",
            "5e-SRD-Magic-Items.json": "magic_items",
            "5e-SRD-Magic-Schools.json": "magic_schools",
            "5e-SRD-Proficiencies.json": "proficiencies",
            "5e-SRD-Races.json": "races",
            "5e-SRD-Skills.json": "skills",
            "5e-SRD-Spells.json": "spells",
            "5e-SRD-Subclasses.json": "subclasses",
            "5e-SRD-Subraces.json": "subraces",
            "5e-SRD-Traits.json": "features", # Трейты льем в фичи
            "5e-SRD-Weapon-Properties.json": "weapon_properties"
        }

        try:
            self.setup_database()

            for file_name, table in mapping.items():
                path = os.path.join(self.json_dir, file_name)
                if not os.path.exists(path):
                    logger.error(f"File {file_name} not found, skipping...")
                    continue

                logger.info(f"Importing {file_name} into {table}...")
                with open(path, 'r', encoding='utf-8') as f:
                    records = json.load(f)
                    for r in records:
                        self.upsert(table, r)
                self.conn.commit()

            # Специальные случаи
            eq_path = os.path.join(self.json_dir, "5e-SRD-Equipment.json")
            if os.path.exists(eq_path):
                logger.info("Processing Equipment split...")
                self.import_equipment(eq_path)

            cat_path = os.path.join(self.json_dir, "5e-SRD-Equipment-Categories.json")
            if os.path.exists(cat_path):
                logger.info("Processing Equipment Categories and Links...")
                self.import_categories(cat_path)

            self.conn.commit()
            logger.info("!!! IMPORT SUCCESSFUL: dnd_clean.db is ready !!!")

        except Exception as e:
            self.conn.rollback()
            logger.error(f"FATAL ERROR during import: {e}")
            raise
        finally:
            self.conn.close()

if __name__ == "__main__":
    # Настройки путей
    DB_FILE = "dnd_clean.db"
    JSON_DIR = "srd_data"
    DDL_FILE = "dnd_schema.sql" # Сохрани SQL код схемы в этот файл

    importer = SRDImporter(DB_FILE, JSON_DIR, DDL_FILE)
    importer.run()
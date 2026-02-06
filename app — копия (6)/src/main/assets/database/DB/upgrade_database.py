import sqlite3
import json
import os
import logging

# Настройка логгера
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [%(levelname)s] %(message)s'
)
logger = logging.getLogger("ARCHITECT_ETL")

class SRDImporter:
    def __init__(self, db_path, json_dir, ddl_path):
        self.db_path = db_path
        self.json_dir = json_dir
        self.ddl_path = ddl_path
        self.conn = sqlite3.connect(db_path)
        self.cursor = self.conn.cursor()
        self.table_schemas = {}

    def init_db(self):
        """Инициализация схемы. DDL — абсолютный закон."""
        logger.info(f"Applying Schema v1.23 from {self.ddl_path}...")
        with open(self.ddl_path, 'r', encoding='utf-8') as f:
            ddl = f.read()
        self.cursor.executescript(ddl)
        self.conn.commit()

        # Кэшируем структуру таблиц для валидации
        self.cursor.execute("SELECT name FROM sqlite_master WHERE type='table';")
        tables = self.cursor.fetchall()
        for (table_name,) in tables:
            self.cursor.execute(f"PRAGMA table_info({table_name})")
            # Сохраняем {имя_колонки: (is_not_null)}
            self.table_schemas[table_name] = {row[1]: bool(row[3]) for row in self.cursor.fetchall()}

    def sanitize_record(self, table_name, record):
        """Маппинг данных JSON на колонки БД с типизацией."""
        schema = self.table_schemas.get(table_name)
        if not schema:
            return None

        sanitized = {}
        for col, is_not_null in schema.items():
            val = record.get(col)

            # Авто-конвертация типов
            if isinstance(val, (dict, list)):
                val = json.dumps(val, ensure_ascii=False)
            elif isinstance(val, bool):
                val = 1 if val else 0

            # Проверка NOT NULL ограничений
            if is_not_null and val is None and col != 'id':
                # Пытаемся найти альтернативные имена, если маппинг сбоит
                alt_val = record.get('index_name') if col == 'entity_index' else None
                if alt_val:
                    val = alt_val
                else:
                    raise ValueError(f"CRITICAL: Column '{col}' in table '{table_name}' cannot be NULL.")

            if val is not None:
                sanitized[col] = val

        return sanitized

    def upsert(self, table_name, record):
        """Идемпотентная вставка."""
        try:
            data = self.sanitize_record(table_name, record)
            if not data: return

            cols = ", ".join(data.keys())
            placeholders = ", ".join(["?"] * len(data))
            sql = f"INSERT OR REPLACE INTO {table_name} ({cols}) VALUES ({placeholders})"
            self.cursor.execute(sql, list(data.values()))
        except Exception as e:
            logger.error(f"  [SQL ERROR] Record {record.get('index_name') or record.get('entity_index')}: {e}")

    def route_equipment(self, records):
        """Специфическая логика для оборудования."""
        for r in records:
            cat = (r.get("category_index") or "").lower()
            if "weapon" in cat:
                self.upsert("weapons", r)
            elif "armor" in cat:
                self.upsert("armor", r)
            else:
                self.upsert("equipment", r)

    def process_all(self):
        # ТОЧНЫЙ МАППИНГ ФАЙЛОВ К ТАБЛИЦАМ
        file_map = {
            "5e-SRD-Alignments.json": "alignments",
            "5e-SRD-Backgrounds.json": "backgrounds",
            "5e-SRD-Classes.json": "classes",
            "5e-SRD-Subclasses.json": "subclasses", # Исправлено: льем в subclasses
            "5e-SRD-Damage-Types.json": "damage_types",
            "5e-SRD-Features.json": "features",
            "5e-SRD-Traits.json": "features",
            "5e-SRD-Languages.json": "languages",
            "5e-SRD-Levels.json": "progression", # Исправлено: льем в progression
            "5e-SRD-Magic-Items.json": "magic_items",
            "5e-SRD-Magic-Schools.json": "magic_schools",
            "5e-SRD-Proficiencies.json": "proficiencies",
            "5e-SRD-Races.json": "races",
            "5e-SRD-Subraces.json": "subraces",
            "5e-SRD-Skills.json": "skills",
            "5e-SRD-Spells.json": "spells",
            "5e-SRD-Weapon-Properties.json": "weapon_properties",
            "5e-SRD-Equipment-Categories.json": "equipment_categories"
        }

        self.init_db()

        for file_name, table in file_map.items():
            path = os.path.join(self.json_dir, file_name)
            if not os.path.exists(path): continue

            logger.info(f">>> PROCESSING: {file_name} --> TABLE: {table.upper()}")
            with open(path, 'r', encoding='utf-8') as f:
                data = json.load(f)
                for record in data:
                    self.upsert(table, record)
            self.conn.commit()

        # Обработка Equipment
        eq_path = os.path.join(self.json_dir, "5e-SRD-Equipment.json")
        if os.path.exists(eq_path):
            logger.info(">>> ROUTING POLYMORPHIC EQUIPMENT...")
            with open(eq_path, 'r', encoding='utf-8') as f:
                self.route_equipment(json.load(f))
            self.conn.commit()

        # Обработка линков категорий
        cat_path = os.path.join(self.json_dir, "5e-SRD-Equipment-Categories.json")
        if os.path.exists(cat_path):
            logger.info(">>> LINKING EQUIPMENT CATEGORIES...")
            with open(cat_path, 'r', encoding='utf-8') as f:
                for cat in json.load(f):
                    cat_idx = cat.get("index_name")
                    items = json.loads(cat.get("items_json", "[]"))
                    for item_idx in items:
                        self.cursor.execute(
                            "INSERT OR IGNORE INTO equipment_category_links (category_index, item_index) VALUES (?, ?)",
                            (cat_idx, item_idx)
                        )
            self.conn.commit()

        logger.info("==================================================")
        logger.info("FINAL ARCHITECT AUDIT REPORT")
        logger.info(f"Database Rebuild Complete: {self.db_path}")
        logger.info("==================================================")

if __name__ == "__main__":
    # Настройки
    CONFIG = {
        "db": "dnd_clean.db",
        "data_dir": "srd_data",
        "schema": "dnd_schema.sql"
    }

    importer = SRDImporter(CONFIG["db"], CONFIG["data_dir"], CONFIG["schema"])
    importer.process_all()
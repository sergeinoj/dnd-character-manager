-- D&D 5.1 Absolute Schema v1.26
PRAGMA journal_mode = WAL;
PRAGMA foreign_keys = ON;

-- ================================================================
-- 1. МАГАЗИН И ИНВЕНТАРЬ (SHOP & INVENTORY)
-- ================================================================

CREATE TABLE IF NOT EXISTS equipment_categories (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    index_name TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    parent_index TEXT
);

CREATE TABLE IF NOT EXISTS equipment_category_links (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    category_index TEXT NOT NULL,
    item_index TEXT NOT NULL,
    UNIQUE(category_index, item_index)
);

CREATE TABLE IF NOT EXISTS weapons (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    index_name TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    category_index TEXT,
    damage_dice TEXT,
    damage_type TEXT,
    weight REAL,
    properties_json TEXT,
    rarity TEXT,
    cost_json TEXT,
    cost_cp INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS armor (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    index_name TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    category_index TEXT,
    ac_base INTEGER,
    dex_bonus INTEGER,
    max_bonus INTEGER,
    str_minimum INTEGER,
    stealth_disadvantage INTEGER,
    weight REAL,
    rarity TEXT,
    cost_json TEXT,
    cost_cp INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS equipment (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    index_name TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    category_index TEXT,
    weight REAL,
    description TEXT,
    armor_class_json TEXT,
    str_minimum INTEGER,
    stealth_disadvantage INTEGER,
    damage_json TEXT,
    range_json TEXT,
    properties_json TEXT,
    contents_json TEXT,
    cost_json TEXT,
    cost_cp INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS magic_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    index_name TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    description TEXT,
    category_index TEXT,
    rarity TEXT,
    variant INTEGER,
    variants_json TEXT,
    image_url TEXT,
    reference_json TEXT,
    cost_json TEXT,
    cost_cp INTEGER DEFAULT 0
);

-- Индексы для магазина
CREATE INDEX IF NOT EXISTS idx_weapons_cost ON weapons(cost_cp);
CREATE INDEX IF NOT EXISTS idx_armor_cost ON armor(cost_cp);
CREATE INDEX IF NOT EXISTS idx_equipment_cost ON equipment(cost_cp);
CREATE INDEX IF NOT EXISTS idx_magic_items_cost ON magic_items(cost_cp);

-- ================================================================
-- 2. ОПЦИИ ПЕРСОНАЖА (CORE RULES) - [FIXED COMPATIBILITY]
-- ================================================================

CREATE TABLE IF NOT EXISTS classes (id INTEGER PRIMARY KEY AUTOINCREMENT, index_name TEXT NOT NULL UNIQUE, name TEXT NOT NULL, hit_die INTEGER, proficiency_choices_json TEXT, proficiencies_json TEXT, saving_throws_json TEXT, starting_equipment_json TEXT, starting_equipment_options_json TEXT, spellcasting_json TEXT, class_levels_url TEXT, multi_classing_json TEXT, subclasses_json TEXT, spells_url TEXT);


CREATE TABLE IF NOT EXISTS subclasses (id INTEGER PRIMARY KEY AUTOINCREMENT, index_name TEXT NOT NULL UNIQUE, class_index TEXT NOT NULL, name TEXT NOT NULL, subclass_flavor TEXT, desc TEXT, spells_json TEXT, subclass_levels_url TEXT);

CREATE TABLE IF NOT EXISTS progression (id INTEGER PRIMARY KEY AUTOINCREMENT, entity_index TEXT NOT NULL UNIQUE, level INTEGER NOT NULL, class_index TEXT NOT NULL, subclass_index TEXT, ability_score_bonuses INTEGER, prof_bonus INTEGER, feature_indices_json TEXT, class_specific_json TEXT, subclass_specific_json TEXT, spellcasting_json TEXT);

CREATE TABLE IF NOT EXISTS features (id INTEGER PRIMARY KEY AUTOINCREMENT, index_name TEXT NOT NULL UNIQUE, name TEXT NOT NULL, description TEXT, level INTEGER, class_index TEXT, subclass_index TEXT, race_index TEXT, subrace_index TEXT, background_index TEXT, choices_json TEXT, spell_show_json TEXT, change_rule INTEGER, prerequisites_json TEXT, reference_json TEXT, ui_group TEXT);

CREATE TABLE IF NOT EXISTS races (id INTEGER PRIMARY KEY AUTOINCREMENT, index_name TEXT NOT NULL UNIQUE, name TEXT NOT NULL, description TEXT, speed INTEGER, ability_bonuses_json TEXT, age TEXT, alignment TEXT, size TEXT, size_desc TEXT, languages_json TEXT, language_desc TEXT, traits_json TEXT, starting_proficiencies_json TEXT, starting_proficiency_options_json TEXT, language_options_json TEXT, subraces_json TEXT);

CREATE TABLE IF NOT EXISTS subraces (id INTEGER PRIMARY KEY AUTOINCREMENT, index_name TEXT NOT NULL UNIQUE, race_index TEXT NOT NULL, name TEXT NOT NULL, description TEXT, ability_bonuses_json TEXT, traits_json TEXT, starting_proficiencies_json TEXT, language_options_json TEXT);


CREATE TABLE IF NOT EXISTS backgrounds (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    index_name TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    starting_proficiencies_json TEXT,
    language_options_json TEXT,
    starting_equipment_json TEXT,
    starting_equipment_options_json TEXT,
    starting_gold INTEGER DEFAULT 0,
    feature_index TEXT,
    feature_name TEXT,
    feature_desc TEXT,
    feature_indices_json TEXT,
    personality_traits_json TEXT,
    ideals_json TEXT,
    bonds_json TEXT,
    flaws_json TEXT
);


CREATE TABLE IF NOT EXISTS alignments (id INTEGER PRIMARY KEY AUTOINCREMENT, index_name TEXT NOT NULL UNIQUE, name TEXT NOT NULL, abbreviation TEXT, desc TEXT);

CREATE TABLE IF NOT EXISTS spells (id INTEGER PRIMARY KEY AUTOINCREMENT, index_name TEXT NOT NULL UNIQUE, name TEXT NOT NULL, level INTEGER, school TEXT, casting_time TEXT, range TEXT, components_json TEXT, material TEXT, duration TEXT, concentration INTEGER, ritual INTEGER, description TEXT, higher_level TEXT, classes_json TEXT, damage_json TEXT, attack_type TEXT, dc_json TEXT, area_of_effect_json TEXT, heal_at_slot_level_json TEXT, subclasses_json TEXT);

CREATE TABLE IF NOT EXISTS magic_schools (id INTEGER PRIMARY KEY AUTOINCREMENT, index_name TEXT NOT NULL UNIQUE, name TEXT NOT NULL, description TEXT);

CREATE TABLE IF NOT EXISTS weapon_properties (id INTEGER PRIMARY KEY AUTOINCREMENT, index_name TEXT NOT NULL UNIQUE, name TEXT NOT NULL, description TEXT NOT NULL);
CREATE TABLE IF NOT EXISTS proficiencies (id INTEGER PRIMARY KEY AUTOINCREMENT, index_name TEXT NOT NULL UNIQUE, type TEXT, name TEXT NOT NULL, reference_json TEXT, classes_json TEXT, races_json TEXT);
CREATE TABLE IF NOT EXISTS skills (id INTEGER PRIMARY KEY AUTOINCREMENT, index_name TEXT NOT NULL UNIQUE, name TEXT NOT NULL, description TEXT, ability_score_index TEXT);
CREATE TABLE IF NOT EXISTS languages (id INTEGER PRIMARY KEY AUTOINCREMENT, index_name TEXT NOT NULL UNIQUE, name TEXT NOT NULL, type TEXT, script TEXT, description TEXT, typical_speakers_json TEXT);
CREATE TABLE IF NOT EXISTS damage_types (id INTEGER PRIMARY KEY AUTOINCREMENT, index_name TEXT NOT NULL UNIQUE, name TEXT NOT NULL, description TEXT);
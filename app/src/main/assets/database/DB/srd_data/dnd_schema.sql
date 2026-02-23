-- D&D 5.1 Absolute Schema v2.0 (Living Sheet Edition)
-- Status: INTEGRATED & UPGRADED
-- Compatible with: Resonance Engine & Event-Driven Automation

PRAGMA journal_mode = WAL;
PRAGMA foreign_keys = ON;

-- ======================================================================================
-- SECTION 1: CORE REFERENCE (ITEMS, SPELLS, CHARACTER OPTIONS)
-- (Preserved from v1.36)
-- ======================================================================================

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
    description TEXT,
    category_index TEXT,
    range_json TEXT,
    damage_dice TEXT,
    damage_type TEXT,
    weight REAL NOT NULL DEFAULT 0.0,
    properties_json TEXT,
    rarity TEXT,
    cost_json TEXT,
    cost_cp INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS armor (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    index_name TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    description TEXT,
    category_index TEXT,
    ac_base INTEGER NOT NULL DEFAULT 0,
    dex_bonus INTEGER NOT NULL DEFAULT 0,
    max_bonus INTEGER NOT NULL DEFAULT 0,
    str_minimum INTEGER NOT NULL DEFAULT 0,
    stealth_disadvantage INTEGER NOT NULL DEFAULT 0,
    weight REAL NOT NULL DEFAULT 0.0,
    rarity TEXT,
    cost_json TEXT,
    cost_cp INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS equipment (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    index_name TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    category_index TEXT,
    weight REAL NOT NULL DEFAULT 0.0,
    description TEXT,
    armor_class_json TEXT,
    str_minimum INTEGER NOT NULL DEFAULT 0,
    stealth_disadvantage INTEGER NOT NULL DEFAULT 0,
    damage_json TEXT,
    range_json TEXT,
    properties_json TEXT,
    contents_json TEXT,
    cost_json TEXT,
    cost_cp INTEGER NOT NULL DEFAULT 0
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
    cost_cp INTEGER NOT NULL DEFAULT 0,
    weight REAL NOT NULL DEFAULT 0.0,
    base_item_index TEXT,
    change_rule INTEGER NOT NULL DEFAULT 0,
    requires_attunement INTEGER NOT NULL DEFAULT 0,
    max_charges INTEGER NOT NULL DEFAULT 0,
    charge_reset_rule TEXT,
    bonus_ac INTEGER NOT NULL DEFAULT 0,
    bonus_attack INTEGER NOT NULL DEFAULT 0,
    bonus_damage INTEGER NOT NULL DEFAULT 0,
    bonus_save_dc INTEGER NOT NULL DEFAULT 0,
    granted_spells_json TEXT,
    stat_overrides_json TEXT,
    mechanics_json TEXT
);

CREATE TABLE IF NOT EXISTS classes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    index_name TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    hit_die INTEGER NOT NULL DEFAULT 0,
    primary_stat TEXT,
    caster_type TEXT,
    caster_weight REAL NOT NULL DEFAULT 0.0,
    proficiency_choices_json TEXT,
    proficiencies_json TEXT,
    saving_throws_json TEXT,
    starting_equipment_json TEXT,
    starting_equipment_options_json TEXT,
    spellcasting_json TEXT,
    class_levels_url TEXT,
    multi_classing_json TEXT,
    subclasses_json TEXT,
    spells_url TEXT
);

CREATE TABLE IF NOT EXISTS subclasses (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    index_name TEXT NOT NULL UNIQUE,
    class_index TEXT NOT NULL,
    name TEXT NOT NULL,
    sub_caster_weight REAL NOT NULL DEFAULT 0.0,
    subclass_flavor TEXT,
    desc TEXT,
    spells_json TEXT,
    subclass_levels_url TEXT,
    FOREIGN KEY(class_index) REFERENCES classes(index_name)
);

CREATE TABLE IF NOT EXISTS progression (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    entity_index TEXT NOT NULL UNIQUE,
    level INTEGER NOT NULL,
    class_index TEXT NOT NULL,
    subclass_index TEXT,
    max_charges INTEGER NOT NULL DEFAULT 0,
    resource_name TEXT,
    charge_reset_rule TEXT,
    max_charges_2 INTEGER NOT NULL DEFAULT 0,
    resource_name_2 TEXT,
    charge_reset_rule_2 TEXT,
    max_charges_3 INTEGER NOT NULL DEFAULT 0,
    resource_name_3 TEXT,
    charge_reset_rule_3 TEXT,
    die_count INTEGER NOT NULL DEFAULT 0,
    die_size INTEGER NOT NULL DEFAULT 0,
    scaling_bonus INTEGER NOT NULL DEFAULT 0,
    movement_bonus INTEGER NOT NULL DEFAULT 0,
    caster_level_increment REAL NOT NULL DEFAULT 0.0,
    is_pact_increment INTEGER NOT NULL DEFAULT 0,
    prep_formula_type TEXT,
    ability_score_bonuses INTEGER NOT NULL DEFAULT 0,
    prof_bonus INTEGER NOT NULL DEFAULT 0,
    feature_indices_json TEXT,
    class_specific_json TEXT,
    subclass_specific_json TEXT,
    spellcasting_json TEXT,
    FOREIGN KEY(class_index) REFERENCES classes(index_name)
);

CREATE TABLE IF NOT EXISTS features (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    index_name TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    description TEXT,
    level INTEGER,
    class_index TEXT,
    subclass_index TEXT,
    race_index TEXT,
    subrace_index TEXT,
    background_index TEXT,
    max_charges INTEGER NOT NULL DEFAULT 0,
    charge_reset_rule TEXT,
    choices_json TEXT,
    spell_show_json TEXT,
    change_rule INTEGER NOT NULL DEFAULT 0,
    prerequisites_json TEXT,
    reference_json TEXT,
    ui_group TEXT
);

CREATE TABLE IF NOT EXISTS races (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    index_name TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    description TEXT,
    speed INTEGER NOT NULL DEFAULT 30,
    ability_bonuses_json TEXT,
    age TEXT,
    alignment TEXT,
    size TEXT,
    size_desc TEXT,
    languages_json TEXT,
    language_desc TEXT,
    traits_json TEXT,
    starting_proficiencies_json TEXT,
    starting_proficiency_options_json TEXT,
    language_options_json TEXT,
    subraces_json TEXT
);

CREATE TABLE IF NOT EXISTS subraces (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    index_name TEXT NOT NULL UNIQUE,
    race_index TEXT NOT NULL,
    name TEXT NOT NULL,
    description TEXT,
    ability_bonuses_json TEXT,
    traits_json TEXT,
    starting_proficiencies_json TEXT,
    language_options_json TEXT
);

CREATE TABLE IF NOT EXISTS backgrounds (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    index_name TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    starting_proficiencies_json TEXT,
    language_options_json TEXT,
    starting_equipment_json TEXT,
    starting_equipment_options_json TEXT,
    starting_gold INTEGER NOT NULL DEFAULT 0,
    feature_index TEXT,
    feature_name TEXT,
    feature_desc TEXT,
    feature_indices_json TEXT,
    personality_traits_json TEXT,
    ideals_json TEXT,
    bonds_json TEXT,
    flaws_json TEXT
);

CREATE TABLE IF NOT EXISTS spells (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    index_name TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    level INTEGER NOT NULL DEFAULT 0,
    school TEXT,
    casting_time TEXT,
    range TEXT,
    components_json TEXT,
    material TEXT,
    duration TEXT,
    concentration INTEGER NOT NULL DEFAULT 0,
    ritual INTEGER NOT NULL DEFAULT 0,
    description TEXT,
    higher_level TEXT,
    classes_json TEXT,
    damage_json TEXT,
    attack_type TEXT,
    dc_json TEXT,
    area_of_effect_json TEXT,
    heal_at_slot_level_json TEXT,
    subclasses_json TEXT
);

CREATE TABLE IF NOT EXISTS alignments (id INTEGER PRIMARY KEY AUTOINCREMENT, index_name TEXT NOT NULL UNIQUE, name TEXT NOT NULL, abbreviation TEXT, desc TEXT);
CREATE TABLE IF NOT EXISTS magic_schools (id INTEGER PRIMARY KEY AUTOINCREMENT, index_name TEXT NOT NULL UNIQUE, name TEXT NOT NULL, description TEXT);
CREATE TABLE IF NOT EXISTS weapon_properties (id INTEGER PRIMARY KEY AUTOINCREMENT, index_name TEXT NOT NULL UNIQUE, name TEXT NOT NULL, description TEXT NOT NULL);
CREATE TABLE IF NOT EXISTS proficiencies (id INTEGER PRIMARY KEY AUTOINCREMENT, index_name TEXT NOT NULL UNIQUE, type TEXT, name TEXT NOT NULL, reference_json TEXT, classes_json TEXT, races_json TEXT);
CREATE TABLE IF NOT EXISTS skills (id INTEGER PRIMARY KEY AUTOINCREMENT, index_name TEXT NOT NULL UNIQUE, name TEXT NOT NULL, description TEXT, ability_score_index TEXT);
CREATE TABLE IF NOT EXISTS languages (id INTEGER PRIMARY KEY AUTOINCREMENT, index_name TEXT NOT NULL UNIQUE, name TEXT NOT NULL, type TEXT, script TEXT, description TEXT, typical_speakers_json TEXT);
CREATE TABLE IF NOT EXISTS damage_types (id INTEGER PRIMARY KEY AUTOINCREMENT, index_name TEXT NOT NULL UNIQUE, name TEXT NOT NULL, description TEXT);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_weapons_cost ON weapons(cost_cp);
CREATE INDEX IF NOT EXISTS idx_armor_cost ON armor(cost_cp);
CREATE INDEX IF NOT EXISTS idx_equipment_cost ON equipment(cost_cp);
CREATE INDEX IF NOT EXISTS idx_magic_items_cost ON magic_items(cost_cp);
CREATE INDEX IF NOT EXISTS idx_magic_items_category ON magic_items(category_index);
CREATE INDEX IF NOT EXISTS idx_magic_items_rarity ON magic_items(rarity);
CREATE INDEX IF NOT EXISTS idx_prog_search ON progression(class_index, level);
CREATE INDEX IF NOT EXISTS idx_feat_class ON features(class_index, level);
CREATE INDEX IF NOT EXISTS idx_feat_subclass ON features(subclass_index);


-- ======================================================================================
-- SECTION 2: LIVING MONSTER ENGINE (EVENT-DRIVEN UPDATE)
-- ======================================================================================

-- 2.1. CONDITIONS REGISTRY (NEW)
-- Хранит механику состояний (Паралич, Ослепление и т.д.)
CREATE TABLE IF NOT EXISTS conditions (
    index_name TEXT PRIMARY KEY NOT NULL,   -- 'poisoned', 'exhaustion_3'
    name TEXT NOT NULL,                     -- 'Отравление'
    description TEXT,                       -- Текст для UI
    ui_color_hex TEXT,                      -- Цвет для боевой консоли
    mechanics_json TEXT NOT NULL            -- Флаги: { "attack_disadvantage": true, "speed_mult": 0 }
);

-- 2.2. MONSTERS BASE (EXISTING)
CREATE TABLE IF NOT EXISTS monsters (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    index_name TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    name_ru TEXT,
    size TEXT,
    type TEXT,
    subtype TEXT,
    alignment TEXT,
    challenge_rating REAL,
    proficiency_bonus INTEGER,
    xp INTEGER,
    armor_class_json TEXT,
    hit_points INTEGER,
    hit_points_roll TEXT,
    hit_dice TEXT,
    hit_die_count INTEGER,
    hit_die_size INTEGER,
    hit_die_bonus INTEGER,
    speed_json TEXT,
    stats_json TEXT,
    condition_immunities_json TEXT,
    senses_json TEXT,
    languages TEXT,
    description TEXT,
    description_ru TEXT,
    desc_ru TEXT
);

-- 2.3. MONSTER ACTIONS (MODIFIED)
-- Добавлено поле `action_index` для связи с триггерной системой.
-- Старые поля `..._json` оставлены для совместимости, но логика переезжает в monster_action_effects.
CREATE TABLE IF NOT EXISTS monster_actions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    monster_index TEXT NOT NULL,
    
    -- NEW FIELD: Stable Latin Slug for logic linking (e.g., 'bite', 'web')
    action_index TEXT,                  

    name TEXT NOT NULL,
    desc TEXT,
    attack_bonus INTEGER,
    
    -- Legacy/Display blobs
    attack_json TEXT,
    damage_json TEXT,
    dc_json TEXT,
    usage_json TEXT,
    options_json TEXT,
    type TEXT,
    
    FOREIGN KEY(monster_index) REFERENCES monsters(index_name)
);

-- 2.4. ACTION EFFECTS & TRIGGERS (NEW)
-- Главная таблица автоматизации боя. Заменяет текстовый парсинг.
CREATE TABLE IF NOT EXISTS monster_action_effects (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    monster_index TEXT NOT NULL,
    action_index TEXT NOT NULL,             -- Ссылка на action_index из monster_actions
    
    -- TRIGGER (КОГДА?)
    trigger_event TEXT NOT NULL,            -- 'ON_HIT', 'ON_SAVE_FAIL', 'ALWAYS'
    trigger_condition TEXT,                 -- Предикат: 'TARGET_HP_0', 'SIZE_LE_MEDIUM'
    
    -- EFFECT (ЧТО?)
    effect_type TEXT NOT NULL,              -- 'DAMAGE', 'APPLY_CONDITION', 'GRAPPLE', 'SWALLOW'
    target TEXT NOT NULL DEFAULT 'TARGET',  -- 'TARGET', 'SELF', 'AOE'
    
    -- DATA (КАК?)
    payload_json TEXT NOT NULL,             -- { "dice": "2d6", "type": "poison" } или { "condition": "restrained" }
    
    save_dc_override INTEGER,               -- Если у эффекта свой DC
    save_stat TEXT,                         -- 'CON', 'STR'
    
    FOREIGN KEY(monster_index) REFERENCES monsters(index_name) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_effects_lookup ON monster_action_effects(monster_index, action_index);


-- 2.5. RECURSIVE ATTACK PATTERNS (NEW)
-- Деревья решений для Мультиатаки (И/ИЛИ)
CREATE TABLE IF NOT EXISTS monster_attack_patterns (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    monster_index TEXT NOT NULL,
    pattern_slug TEXT NOT NULL,             -- 'multiattack_standard'
    logic_operator TEXT NOT NULL,           -- 'AND', 'OR', 'XOR'
    description TEXT,                       -- Текст для UI
    
    FOREIGN KEY(monster_index) REFERENCES monsters(index_name) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS monster_attack_pattern_entries (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    pattern_id INTEGER NOT NULL,
    
    entry_type TEXT NOT NULL,               -- 'ACTION', 'PATTERN' (для вложенности)
    entry_index TEXT NOT NULL,              -- Ссылка на action_index или pattern_slug
    count INTEGER DEFAULT 1,                -- Количество атак
    
    FOREIGN KEY(pattern_id) REFERENCES monster_attack_patterns(id) ON DELETE CASCADE
);


-- 2.6. CONTAINER MECHANICS (NEW)
-- Для монстров, которые глотают персонажей.
CREATE TABLE IF NOT EXISTS monster_containers (
    monster_index TEXT NOT NULL,
    action_index TEXT NOT NULL,             -- Действие, вызывающее заглатывание
    
    capacity_size_limit TEXT,               -- 'MEDIUM'
    capacity_count INTEGER DEFAULT 1,       
    
    turn_start_damage_json TEXT,            -- Урон в начале хода жертвы
    escape_dc INTEGER,                      -- Сл проверки силы для выхода
    ejection_damage_threshold INTEGER,      -- Урон для выплевывания
    blinded_restrained BOOLEAN DEFAULT 1,   -- Накладывает ли авто-слепоту
    
    PRIMARY KEY(monster_index, action_index),
    FOREIGN KEY(monster_index) REFERENCES monsters(index_name) ON DELETE CASCADE
);


-- 2.7. LEGACY MONSTER TABLES (PRESERVED)
-- Оставлены для совместимости с существующим кодом загрузки.

CREATE TABLE IF NOT EXISTS monster_special_abilities (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    monster_index TEXT NOT NULL,
    name TEXT NOT NULL,
    desc TEXT,
    dc_json TEXT,
    usage_json TEXT,
    spellcasting_json TEXT,
    FOREIGN KEY(monster_index) REFERENCES monsters(index_name)
);

CREATE TABLE IF NOT EXISTS monster_legendary_actions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    monster_index TEXT NOT NULL,
    name TEXT NOT NULL,
    desc TEXT,
    attack_json TEXT,
    damage_json TEXT,
    dc_json TEXT,
    usage_json TEXT,
    cost INTEGER,
    FOREIGN KEY(monster_index) REFERENCES monsters(index_name)
);

CREATE TABLE IF NOT EXISTS monster_reactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    monster_index TEXT NOT NULL,
    name TEXT NOT NULL,
    desc TEXT,
    attack_json TEXT,
    damage_json TEXT,
    dc_json TEXT,
    usage_json TEXT,
    FOREIGN KEY(monster_index) REFERENCES monsters(index_name)
);

CREATE TABLE IF NOT EXISTS monster_proficiencies (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    monster_index TEXT NOT NULL,
    proficiency_index TEXT,
    value INTEGER,
    FOREIGN KEY(monster_index) REFERENCES monsters(index_name)
);

CREATE TABLE IF NOT EXISTS monster_damage_mods (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    monster_index TEXT NOT NULL,
    kind TEXT,
    value TEXT,
    FOREIGN KEY(monster_index) REFERENCES monsters(index_name)
);

-- Indexes for Monster Sub-tables
CREATE INDEX IF NOT EXISTS index_monster_actions_monster_index ON monster_actions(monster_index);
CREATE INDEX IF NOT EXISTS index_monster_special_abilities_monster_index ON monster_special_abilities(monster_index);
CREATE INDEX IF NOT EXISTS index_monster_legendary_actions_monster_index ON monster_legendary_actions(monster_index);
CREATE INDEX IF NOT EXISTS index_monster_reactions_monster_index ON monster_reactions(monster_index);
CREATE INDEX IF NOT EXISTS index_monster_proficiencies_monster_index ON monster_proficiencies(monster_index);
CREATE INDEX IF NOT EXISTS index_monster_damage_mods_monster_index ON monster_damage_mods(monster_index);
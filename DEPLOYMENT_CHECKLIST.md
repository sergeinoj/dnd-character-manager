# DEPLOYMENT CHECKLIST
## GitHub Publication - D&D Character Manager

---

## ✅ WHAT WE PUSH (Safe for GitHub)

### Source Code
```
app/src/main/java/com/dnd/app/        # All Kotlin source (204 files)
app/src/main/java/com/dnd/app/data/   # Data Layer
app/src/main/java/com/dnd/app/di/     # Dependency Injection
app/src/main/java/com/dnd/app/domain/ # Business Logic
app/src/main/java/com/dnd/app/ui/     # UI Components
```

### Build Configuration
```
build.gradle.kts                      # Root build
app/build.gradle.kts                  # App build
settings.gradle.kts                   # Settings
gradle.properties                     # Gradle config
gradle/                               # Wrapper files
gradlew                               # Unix wrapper
gradlew.bat                           # Windows wrapper
```

### ETL Pipeline (Code Only)
```
app/src/main/assets/database/DB/srd_data/upgrade_JSON.py  # ETL Script
app/src/main/assets/database/DB/srd_data/dnd_schema.sql   # Database Schema
```

### Documentation
```
README.md                             # Portfolio README
АНАЛИЗ_ПРОЕКТА.md                     # Architecture Analysis
.gitignore                            # Updated gitignore
```

### Standard SRD JSON Files (Minimal)
```
app/src/main/assets/database/DB/srd_data/
├── 5e-SRD-Alignments.json       # 3 KB   ✓
├── 5e-SRD-Backgrounds.json      # 12 KB  ✓
├── 5e-SRD-Damage-Types.json     # 3 KB   ✓
├── 5e-SRD-Languages.json        # 4 KB   ✓
├── 5e-SRD-Magic-Schools.json    # 3 KB   ✓
├── 5e-SRD-Skills.json           # 10 KB  ✓
├── 5e-SRD-Weapon-Properties.json # 4 KB  ✓
├── 5e-SRD-Classes.json          # 151 KB ⚠️ Verify SRD-only
├── 5e-SRD-Equipment.json        # 177 KB ✓
├── 5e-SRD-Equipment-Categories.json # 138 KB ✓
├── 5e-SRD-Features.json         # 356 KB ⚠️ Verify SRD-only
├── 5e-SRD-Levels.json           # 225 KB ⚠️ Verify SRD-only
├── 5e-SRD-Magic-Items.json      # 435 KB ✓
├── 5e-SRD-Monsters.json         # 1.3 MB ⚠️ May contain non-SRD
├── 5e-SRD-Proficiencies.json    # 47 KB  ✓
├── 5e-SRD-Races.json            # 26 KB  ✓
├── 5e-SRD-Spells.json           # 608 KB ✓
├── 5e-SRD-Subclasses.json       # 54 KB  ✓
├── 5e-SRD-Subraces.json         # 4 KB   ✓
├── 5e-SRD-Traits.json           # 56 KB  ✓
└── 5e-SRD-Conditions.json       # 7 KB   ✓
```

---

## ❌ WHAT WE IGNORE (DMCA Protection)

### Expanded Data Folders
```
app/src/main/assets/database/DB/srd_data/All_srd/     # Expanded SRD - DO NOT PUSH
app/src/main/assets/database/DB/srd_data/Bak/         # Backups - DO NOT PUSH
```

### Database Files
```
app/src/main/assets/database/dnd_clean.db             # 3.2 MB - TOO LARGE, may have expanded data
**/dnd_expanded.db                                     # Expanded database
**/dnd_full.db                                         # Full database
**/*_expanded*.db                                      # Any expanded DB
**/*user*.db                                           # User character databases
```

### Generated/Build Files
```
.gradle/                                               # Gradle cache
.idea/                                                 # IDE config
build/                                                 # Build output
app/build/                                             # App build output
*.iml                                                  # Module files
local.properties                                       # Local config
```

### Python Artifacts
```
__pycache__/                                           # Python cache
*.pyc, *.pyo                                          # Compiled Python
venv/, env/                                           # Virtual environments
migration.log                                         # ETL log file
```

### Custom/Expanded JSON
```
wild_shape_forms_localized.json                       # May have non-SRD monsters
*_custom_*.json                                       # Custom content
*_expanded_*.json                                     # Expanded content
```

---

## ⚠️ FILES REQUIRING VERIFICATION

| File | Issue | Action Required |
|------|-------|-----------------|
| `dnd_clean.db` | 3.2MB - too large, may contain expanded data | Generate new SRD-only DB from standard JSONs |
| `All_srd/5e-SRD-Features.json` | 1.0 MB vs 356 KB standard | Already ignored via gitignore |
| `All_srd/5e-SRD-Levels.json` | 504 KB vs 225 KB standard | Already ignored via gitignore |
| `5e-SRD-Monsters.json` | 1.3 MB - verify SRD-only | Compare with official 5e-SRD API |

---

## 🔧 PRE-PUSH ACTIONS

### 1. Verify .gitignore
```bash
git status --ignored
```
Ensure all expanded data folders are ignored.

### 2. Remove tracked ignored files (if any)
```bash
git rm --cached app/src/main/assets/database/DB/srd_data/All_srd/
git rm --cached app/src/main/assets/database/dnd_clean.db
```

### 3. Generate clean SRD database (Optional but recommended)
```bash
cd app/src/main/assets/database/DB/srd_data/
# Download fresh SRD JSONs from 5e-srd-api
python upgrade_JSON.py
# Result: dnd_clean.db with SRD-only data
```

### 4. Final verification
```bash
# Check what will be committed
git add .
git status

# Verify database size
ls -lh app/src/main/assets/database/dnd_clean.db
# Should be ~1-2 MB for SRD-only, NOT 3.2 MB
```

---

## 📋 FINAL CHECKLIST

- [ ] `.gitignore` updated and active
- [ ] `All_srd/` folder ignored
- [ ] `Bak/` folder ignored
- [ ] `dnd_clean.db` either removed or regenerated as SRD-only
- [ ] No `*user*.db` files in commit
- [ ] No `*_expanded*.json` files in commit
- [ ] `README.md` created with legal disclaimer
- [ ] No secrets/credentials in code
- [ ] Build succeeds with `./gradlew assembleDebug`

---

## 🚀 PUSH COMMAND

```bash
# After verification
git add .
git commit -m "Initial portfolio release: D&D 5e Character Manager engine"
git push origin main
```

---

*Generated: 2026-02-23*

from pathlib import Path
import re
import sys

root = Path(__file__).resolve().parents[1]
errors = []
gradle = (root / 'app/build.gradle.kts').read_text(encoding='utf-8')
db_code = (root / 'app/src/main/java/com/saamaka/dico/testeurs/database/DictionaryDatabase.kt').read_text(encoding='utf-8')
db_asset = root / 'app/src/main/assets/SaamakaDico_v11_25.db'
if not re.search(r'versionCode\s*=\s*357\b', gradle): errors.append('versionCode doit être 357')
if not re.search(r'versionName\s*=\s*"3\.9\.0"', gradle): errors.append('versionName doit être 3.9.0')
if not re.search(r'EMBEDDED_DB_VERSION\s*=\s*6\b', db_code): errors.append('EMBEDDED_DB_VERSION doit être 6')
if not db_asset.exists() or db_asset.stat().st_size <= 0: errors.append('base SQLite embarquée absente ou vide')
if errors:
    print('RELEASE NOT READY')
    for error in errors: print('-', error)
    sys.exit(1)
print('RELEASE READY CHECKS OK: versionCode=357 versionName=3.9.0 embeddedDb=6 asset=OK')

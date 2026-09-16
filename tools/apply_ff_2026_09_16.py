#!/usr/bin/env python3
"""Apply the 2026-09-16 ff tester corrections to the embedded Saamaka SQLite database.

Safe by default:
- verifies the expected current French/Saamaka values before changing an existing row;
- applies only the 10 explicit corrections, 2 active validations, and 1 new-entry proposal;
- does NOT execute the 54 deletion proposals;
- creates a local .bak copy before modifying the database;
- bumps DictionaryDatabase.EMBEDDED_DB_VERSION from 6 to 7 so installed apps refresh the asset.
"""
from __future__ import annotations

import json
import shutil
import sqlite3
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DB_PATH = ROOT / "app" / "src" / "main" / "assets" / "SaamakaDico_v11_25.db"
PATCH_PATH = ROOT / "tools" / "data" / "ff_2026-09-16.json"
DB_KT_PATH = ROOT / "app" / "src" / "main" / "java" / "com" / "saamaka" / "dico" / "testeurs" / "database" / "DictionaryDatabase.kt"
EXPECTED_DB_VERSION = 6
NEW_DB_VERSION = 7


def fail(message: str):
    raise SystemExit(f"ERROR: {message}")


def main() -> int:
    if not DB_PATH.exists():
        fail(f"database not found: {DB_PATH}")
    if not PATCH_PATH.exists():
        fail(f"patch data not found: {PATCH_PATH}")
    if not DB_KT_PATH.exists():
        fail(f"DictionaryDatabase.kt not found: {DB_KT_PATH}")

    data = json.loads(PATCH_PATH.read_text(encoding="utf-8"))
    backup = DB_PATH.with_suffix(DB_PATH.suffix + ".ff-2026-09-16.bak")
    if not backup.exists():
        shutil.copy2(DB_PATH, backup)
        print(f"Backup created: {backup.relative_to(ROOT)}")
    else:
        print(f"Backup already exists: {backup.relative_to(ROOT)}")

    con = sqlite3.connect(DB_PATH)
    con.row_factory = sqlite3.Row
    try:
        columns = {row[1] for row in con.execute("PRAGMA table_info(dictionnaire)")}
        required = {"id", "francais", "english", "nederlands", "saamaka", "categorie", "valide"}
        missing = required - columns
        if missing:
            fail(f"dictionnaire schema is missing columns: {sorted(missing)}")

        for patch in data["corrections"]:
            row = con.execute(
                "SELECT id, francais, saamaka FROM dictionnaire WHERE id = ?",
                (patch["id"],),
            ).fetchone()
            if row is None:
                fail(f"correction id {patch['id']} does not exist")
            if (row["francais"] or "").strip() != patch["expected_french"]:
                fail(
                    f"id {patch['id']} French mismatch: DB={row['francais']!r}, "
                    f"expected={patch['expected_french']!r}"
                )
            if (row["saamaka"] or "").strip() != patch["expected_saamaka"]:
                fail(
                    f"id {patch['id']} Saamaka mismatch: DB={row['saamaka']!r}, "
                    f"expected={patch['expected_saamaka']!r}"
                )

        con.execute("BEGIN IMMEDIATE")
        active_validations = set(data.get("active_validations", []))

        for patch in data["corrections"]:
            assignments = []
            values = []
            if "french" in patch:
                assignments.append("francais = ?")
                values.append(patch["french"])
            if "saamaka" in patch:
                assignments.append("saamaka = ?")
                values.append(patch["saamaka"])
            if patch["id"] in active_validations:
                assignments.append("valide = ?")
                values.append("V")
            values.append(patch["id"])
            con.execute(
                f"UPDATE dictionnaire SET {', '.join(assignments)} WHERE id = ?",
                values,
            )

        correction_ids = {p["id"] for p in data["corrections"]}
        for entry_id in active_validations:
            if entry_id not in correction_ids:
                con.execute("UPDATE dictionnaire SET valide = 'V' WHERE id = ?", (entry_id,))

        for entry in data.get("new_entries", []):
            existing = con.execute(
                """
                SELECT id FROM dictionnaire
                WHERE TRIM(LOWER(saamaka)) = TRIM(LOWER(?))
                  AND TRIM(LOWER(francais)) = TRIM(LOWER(?))
                LIMIT 1
                """,
                (entry["saamaka"], entry["french"]),
            ).fetchone()
            if existing:
                print(
                    f"New entry already present as id {existing['id']}: "
                    f"{entry['saamaka']} -> {entry['french']}"
                )
                continue

            next_id = con.execute("SELECT COALESCE(MAX(id), 1) + 1 FROM dictionnaire").fetchone()[0]
            con.execute(
                """
                INSERT INTO dictionnaire
                    (id, francais, english, nederlands, saamaka, categorie, valide)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                (
                    next_id,
                    entry["french"],
                    entry.get("english", ""),
                    entry.get("dutch", ""),
                    entry["saamaka"],
                    entry.get("category", ""),
                    "",
                ),
            )
            print(f"Inserted new entry id {next_id}: {entry['saamaka']} -> {entry['french']}")

        con.commit()

        for patch in data["corrections"]:
            row = con.execute(
                "SELECT francais, saamaka, valide FROM dictionnaire WHERE id = ?",
                (patch["id"],),
            ).fetchone()
            if "french" in patch and row["francais"] != patch["french"]:
                fail(f"post-check failed for French on id {patch['id']}")
            if "saamaka" in patch and row["saamaka"] != patch["saamaka"]:
                fail(f"post-check failed for Saamaka on id {patch['id']}")
            if patch["id"] in active_validations and row["valide"] != "V":
                fail(f"post-check failed for validation on id {patch['id']}")

        integrity = con.execute("PRAGMA integrity_check").fetchone()[0]
        if integrity != "ok":
            fail(f"SQLite integrity_check failed: {integrity}")
    except Exception:
        con.rollback()
        raise
    finally:
        con.close()

    kt = DB_KT_PATH.read_text(encoding="utf-8")
    old = f"const val EMBEDDED_DB_VERSION = {EXPECTED_DB_VERSION}"
    new = f"const val EMBEDDED_DB_VERSION = {NEW_DB_VERSION}"
    if new in kt:
        print(f"Embedded DB version already at {NEW_DB_VERSION}")
    elif old in kt:
        DB_KT_PATH.write_text(kt.replace(old, new, 1), encoding="utf-8")
        print(f"Embedded DB version bumped: {EXPECTED_DB_VERSION} -> {NEW_DB_VERSION}")
    else:
        fail("could not find the expected EMBEDDED_DB_VERSION line")

    print()
    print("Applied:")
    print(f"  corrections: {len(data['corrections'])}")
    print(f"  active validations: {len(active_validations)}")
    print(f"  new-entry proposals: {len(data.get('new_entries', []))}")
    print(f"  pending deletions NOT applied: {len(data.get('pending_deletions', []))}")
    print("SQLite integrity_check: ok")
    return 0


if __name__ == "__main__":
    sys.exit(main())

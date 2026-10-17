#!/usr/bin/env python3
"""Build ECDICT SQLite package and optionally upload to OSS.

Usage:
  python dist/ecdict_dict.py                          # default: dist/ecdict.txt
  python dist/ecdict_dict.py --input path/to/ecdict.txt [--version N] [--upload]
  python dist/ecdict_dict.py --sample [--version 1]   # tiny sample DB for local/dev

Input is ECDICT CSV (often named .txt): word, phonetic, definition, translation,
pos, collins, oxford, tag, bnc, frq, exchange, detail, audio
"""

from __future__ import annotations

import argparse
import csv
import json
import logging
import os
import sqlite3
import sys
import zipfile
from typing import Any, Dict, Iterable, Optional, Tuple

dist_dir = os.path.dirname(os.path.abspath(__file__))
if dist_dir not in sys.path:
    sys.path.insert(0, dist_dir)

logger = logging.getLogger(__name__)

CREATE_SQL = """
CREATE TABLE dict_entries (
  word TEXT NOT NULL,
  wordLower TEXT NOT NULL PRIMARY KEY,
  phonetic TEXT,
  translation TEXT NOT NULL,
  definition TEXT,
  tag TEXT,
  frq INTEGER
);
CREATE INDEX idx_dict_entries_prefix ON dict_entries(wordLower);
CREATE INDEX idx_dict_entries_frq ON dict_entries(frq);
"""

SAMPLE_ROWS = [
    ("abandon", "/əˈbændən/", "v. 放弃；抛弃\nn. 放任", "cet4 cet6 ky", 100),
    ("ability", "/əˈbɪləti/", "n. 能力；才能", "cet4 cet6", 200),
    ("able", "/ˈeɪbl/", "adj. 能够的；有能力的", "cet4", 50),
    ("about", "/əˈbaʊt/", "prep. 关于；大约\nadv. 大约", "zk", 10),
    ("above", "/əˈbʌv/", "prep. 在…上面\nadv. 在上面", "zk", 40),
    ("abroad", "/əˈbrɔːd/", "adv. 在国外；到国外", "cet4", 300),
    ("absence", "/ˈæbsəns/", "n. 缺席；缺乏", "cet4 cet6", 400),
    ("absolute", "/ˈæbsəluːt/", "adj. 绝对的；完全的", "cet4 cet6", 350),
    ("absorb", "/əbˈzɔːb/", "v. 吸收；使全神贯注", "cet4 cet6", 320),
    ("abstract", "/ˈæbstrækt/", "adj. 抽象的\nn. 摘要", "cet6 ky", 500),
]


def _save_json(path: str, data: Dict[str, Any]) -> None:
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def _normalize_word(word: str) -> str:
    return word.strip().lower().strip(".,!?;:\"'()[]{}")


# word, phonetic, translation, definition, tag, frq
Row = Tuple[str, str, str, Optional[str], Optional[str], Optional[int]]


def _build_db(db_path: str, rows: Iterable[Row]) -> int:
    if os.path.exists(db_path):
        os.remove(db_path)
    conn = sqlite3.connect(db_path)
    try:
        conn.executescript(CREATE_SQL)
        count = 0
        batch = []
        for word, phonetic, translation, definition, tag, frq in rows:
            word_lower = _normalize_word(word)
            if not word_lower or not translation:
                continue
            batch.append(
                (
                    word.strip(),
                    word_lower,
                    phonetic or None,
                    translation,
                    definition,
                    tag,
                    frq,
                )
            )
            if len(batch) >= 5000:
                conn.executemany(
                    """
                    INSERT OR REPLACE INTO dict_entries
                    (word, wordLower, phonetic, translation, definition, tag, frq)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """,
                    batch,
                )
                count += len(batch)
                batch.clear()
                if count % 100000 == 0:
                    logger.info("Inserted %s rows…", count)
        if batch:
            conn.executemany(
                """
                INSERT OR REPLACE INTO dict_entries
                (word, wordLower, phonetic, translation, definition, tag, frq)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                batch,
            )
            count += len(batch)
        conn.commit()
        return count
    finally:
        conn.close()


def _iter_ecdict(path: str) -> Iterable[Row]:
    # ECDICT CSV/TXT may be huge; raise field size limit
    csv.field_size_limit(min(sys.maxsize, 10 * 1024 * 1024))
    with open(path, "r", encoding="utf-8", errors="replace", newline="") as f:
        reader = csv.DictReader(f)
        for row in reader:
            word = (row.get("word") or "").strip()
            if not word:
                continue
            phonetic = (row.get("phonetic") or "").strip()
            definition = (row.get("definition") or "").strip() or None
            translation = (row.get("translation") or "").strip()
            if not translation:
                translation = definition or ""
            if not translation:
                continue
            tag = (row.get("tag") or "").strip() or None
            frq_raw = (row.get("frq") or "").strip()
            frq: Optional[int]
            try:
                frq = int(frq_raw) if frq_raw else None
            except ValueError:
                frq = None
            # ECDICT frq: lower = more frequent; app ORDER BY frq ASC
            yield word, phonetic, translation, definition, tag, frq


def _iter_sample() -> Iterable[Row]:
    # frq: lower = more common (matches ECDICT)
    for word, phonetic, translation, tag, frq in SAMPLE_ROWS:
        yield word, phonetic, translation, None, tag, frq


def _zip_db(db_path: str, zip_path: str) -> int:
    if os.path.exists(zip_path):
        os.remove(zip_path)
    with zipfile.ZipFile(zip_path, "w", compression=zipfile.ZIP_DEFLATED) as zf:
        zf.write(db_path, arcname="ecdict.db")
    return os.path.getsize(zip_path)


def main() -> None:
    default_input = os.path.join(dist_dir, "ecdict.txt")
    parser = argparse.ArgumentParser(description="Build ECDICT dict package for Linsia")
    parser.add_argument(
        "--input",
        "--csv",
        dest="input",
        default=None,
        help=f"Path to ECDICT CSV/TXT (default: {default_input})",
    )
    parser.add_argument("--sample", action="store_true", help="Build tiny sample DB instead of full dict")
    parser.add_argument("--version", type=int, default=1, help="Catalog version")
    parser.add_argument("--out-dir", default=None, help="Output directory (default dist/world)")
    parser.add_argument("--upload", action="store_true", help="Upload zip + dict_dist.json to OSS")
    args = parser.parse_args()

    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s - %(filename)s - %(funcName)s - %(lineno)d - %(levelname)s - %(message)s",
    )

    out_dir = os.path.abspath(args.out_dir or os.path.join(dist_dir, "world"))
    os.makedirs(out_dir, exist_ok=True)
    db_path = os.path.join(out_dir, "ecdict.db")
    zip_path = os.path.join(out_dir, "ecdict.db.zip")
    catalog_path = os.path.join(out_dir, "dict_dist.json")

    if args.sample:
        rows = _iter_sample()
        logger.info("Using built-in sample rows")
    else:
        input_path = os.path.abspath(args.input or default_input)
        if not os.path.isfile(input_path):
            raise FileNotFoundError(f"Missing ECDICT file: {input_path}")
        rows = _iter_ecdict(input_path)
        logger.info("Using ECDICT source: %s", input_path)

    logger.info("Building SQLite at %s", db_path)
    entry_count = _build_db(db_path, rows)
    size_bytes = _zip_db(db_path, zip_path)
    logger.info("Packed %s entries → %s (%s bytes)", entry_count, zip_path, size_bytes)

    # Public URL on the same bucket as learn_dist / wordbook zips.
    default_url = (
        "https://objectstorage.uk-london-1.oraclecloud.com/n/lr3rsi02p2te/b/bucket-linsia/o/ecdict.db.zip"
    )
    catalog: Dict[str, Any] = {
        "version": args.version,
        "url": default_url,
        "entryCount": entry_count,
        "sizeBytes": size_bytes,
    }

    if args.upload:
        from util.oss_util import OSSUtil

        oss = OSSUtil()
        zip_url = oss.upload_file_to_r2(zip_path, "ecdict.db.zip")
        if not zip_url:
            raise RuntimeError("Failed to upload ecdict.db.zip")
        catalog["url"] = zip_url
        _save_json(catalog_path, catalog)
        dist_url = oss.upload_file_to_r2(catalog_path, "dict_dist.json")
        logger.info("Uploaded catalog: %s", dist_url)
    else:
        _save_json(catalog_path, catalog)

    # Mirror seed catalog into feature resources when present
    seed_path = os.path.join(
        dist_dir,
        "..",
        "feature",
        "dict",
        "src",
        "commonMain",
        "composeResources",
        "files",
        "dict_dist.json",
    )
    seed_path = os.path.normpath(seed_path)
    if os.path.isdir(os.path.dirname(seed_path)):
        _save_json(seed_path, catalog)
        logger.info("Updated seed catalog: %s", seed_path)

    logger.info("Done. catalog=%s", catalog_path)


if __name__ == "__main__":
    main()

import argparse
import json
import logging
import os
import re
import shutil
import sys
from datetime import date
from io import BytesIO
from typing import Any, Dict, List, Optional

import requests

dist_dir = os.path.dirname(os.path.abspath(__file__))
if dist_dir not in sys.path:
    sys.path.insert(0, dist_dir)

from util.oss_util import OSSUtil


logger = logging.getLogger(__name__)

GUTENBERG_TOP_URL = "https://www.gutenberg.org/browse/scores/top"
GUTENDEX_POPULAR = "https://gutendex.com/books/?copyright=false&languages=en&sort=popular"
GUTENDEX_BOOK = "https://gutendex.com/books/{id}"
USER_AGENT = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
)
BOOK_LIMIT = 20
SKIP_IDS = {
    10,
    30,
    100,
    844,
    1080,
    1232,
    1513,
    1524,
    1531,
    1533,
    1952,
    2264,
    2265,
    2267,
    27761,
}
SKIP_SUBJECT_FRAGMENTS = (
    "drama",
    "comedies",
    "tragedies",
    "poetry",
    "poems",
    "sonnets",
    "political science",
    "essays",
    "oratory",
    "speeches",
    "bible",
    "sacred books",
    "dictionaries",
    "juvenile drama",
)
COVER_GUTENBERG = "https://www.gutenberg.org/cache/epub/{id}/pg{id}.cover.medium.jpg"
COVER_PGLAF = "https://gutenberg.pglaf.org/cache/epub/{id}/pg{id}.cover.medium.jpg"


def _headers() -> Dict[str, str]:
    return {"User-Agent": USER_AGENT, "Accept": "text/html, application/json, image/jpeg, */*"}


def _load_json(path: str) -> Dict[str, Any]:
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def _save_json(path: str, data: Dict[str, Any]) -> None:
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def _http_get(url: str, timeout: int = 60) -> requests.Response:
    response = requests.get(url, headers=_headers(), timeout=timeout)
    response.raise_for_status()
    return response


def parse_gutenberg_top_ids(html: str) -> List[int]:
    heading = re.search(
        r"Top\s+100\s+EBooks\s+last\s+30\s+days(.*?)(?:<h2\b|$)",
        html,
        flags=re.IGNORECASE | re.DOTALL,
    )
    section = heading.group(1) if heading else html
    ids: List[int] = []
    seen = set()
    for match in re.finditer(r'href=["\']/ebooks/(\d+)["\']', section, flags=re.IGNORECASE):
        book_id = int(match.group(1))
        if book_id in seen:
            continue
        seen.add(book_id)
        ids.append(book_id)
    return ids


def fetch_top_ids() -> List[int]:
    try:
        html = _http_get(GUTENBERG_TOP_URL).text
        ids = parse_gutenberg_top_ids(html)
        if ids:
            logger.info("Gutenberg top 30-day ids=%s", len(ids))
            return ids
        logger.warning("Gutenberg top page parsed 0 ids, falling back to Gutendex")
    except Exception as exc:
        logger.warning("Gutenberg top fetch failed: %s", exc)

    ids: List[int] = []
    url: Optional[str] = GUTENDEX_POPULAR
    while url and len(ids) < 120:
        payload = _http_get(url).json()
        for item in payload.get("results") or []:
            book_id = item.get("id")
            if isinstance(book_id, int):
                ids.append(book_id)
        url = payload.get("next")
    logger.info("Gutendex popular ids=%s", len(ids))
    return ids


def _author_name(authors: Any) -> str:
    if not isinstance(authors, list) or not authors:
        return ""
    raw = str(authors[0].get("name") or "").strip()
    if "," in raw:
        last, first = [part.strip() for part in raw.split(",", 1)]
        if first and last:
            return f"{first} {last}"
    return raw


def _subjects_blob(book: Dict[str, Any]) -> str:
    parts = []
    for key in ("subjects", "bookshelves"):
        values = book.get(key) or []
        if isinstance(values, list):
            parts.extend(str(v) for v in values)
    return " ".join(parts).lower()


def is_english_novel(book: Dict[str, Any]) -> bool:
    book_id = book.get("id")
    if book_id in SKIP_IDS:
        return False
    if str(book.get("media_type") or "Text").lower() != "text":
        return False
    languages = book.get("languages") or []
    if languages and "en" not in languages:
        return False
    blob = _subjects_blob(book)
    return not any(fragment in blob for fragment in SKIP_SUBJECT_FRAGMENTS)


def gutendex_cover(book: Dict[str, Any], book_id: int) -> str:
    formats = book.get("formats") or {}
    jpeg = formats.get("image/jpeg")
    if isinstance(jpeg, str) and jpeg.strip():
        return jpeg.strip()
    return COVER_GUTENBERG.format(id=book_id)


def fetch_book(book_id: int) -> Optional[Dict[str, Any]]:
    try:
        book = _http_get(GUTENDEX_BOOK.format(id=book_id)).json()
    except Exception as exc:
        logger.warning("Gutendex book %s failed: %s", book_id, exc)
        return None
    if not is_english_novel(book):
        return None
    title = str(book.get("title") or "").strip()
    if not title:
        return None
    return {
        "gutenbergId": book_id,
        "title": title,
        "author": _author_name(book.get("authors")),
        "coverGutenberg": gutendex_cover(book, book_id),
    }


def download_cover_bytes(book_id: int, preferred: str) -> Optional[bytes]:
    urls = [preferred, COVER_GUTENBERG.format(id=book_id), COVER_PGLAF.format(id=book_id)]
    seen = set()
    for url in urls:
        if not url or url in seen:
            continue
        seen.add(url)
        try:
            response = _http_get(url, timeout=30)
            content_type = (response.headers.get("Content-Type") or "").lower()
            if "html" in content_type:
                continue
            data = response.content
            if data and len(data) > 500:
                return data
        except Exception as exc:
            logger.info("Cover %s failed: %s", url, exc)
    return None


def next_version(existing_path: str) -> int:
    if not os.path.exists(existing_path):
        return 1
    try:
        current = int(_load_json(existing_path).get("version") or 0)
        return current + 1
    except Exception:
        return 1


def public_cover_url(oss: OSSUtil, file_key: str, uploaded: Optional[str]) -> str:
    if uploaded:
        return uploaded
    endpoint = (oss.S3_ENDPOINT_URL or "").rstrip("/")
    bucket = oss.S3_BUCKET_NAME or "bucket-linsia"
    if endpoint:
        return f"{endpoint}/{bucket}/{file_key}"
    return f"https://lr3rsi02p2te.compat.objectstorage.uk-london-1.oraclecloud.com/{bucket}/{file_key}"


def build_catalog(limit: int, upload_covers: bool, oss: Optional[OSSUtil]) -> Dict[str, Any]:
    books: List[Dict[str, Any]] = []
    for book_id in fetch_top_ids():
        if len(books) >= limit:
            break
        meta = fetch_book(book_id)
        if not meta:
            continue
        cover_url = meta["coverGutenberg"]
        if upload_covers and oss is not None:
            file_key = f"classic/covers/{book_id}.jpg"
            data = download_cover_bytes(book_id, meta["coverGutenberg"])
            if data:
                oss.s3.upload_fileobj(BytesIO(data), oss.S3_BUCKET_NAME, file_key)
                uploaded = public_cover_url(oss, file_key, None)
                logger.info("Uploaded cover %s", uploaded)
                cover_url = uploaded
            else:
                logger.warning("Keep Gutenberg cover for %s", book_id)
        books.append(
            {
                "rank": len(books) + 1,
                "gutenbergId": meta["gutenbergId"],
                "title": meta["title"],
                "author": meta["author"],
                "coverUrl": cover_url,
            }
        )
    if len(books) < limit:
        raise RuntimeError(f"Only found {len(books)} novels from Gutenberg top (need {limit})")
    return books


def main() -> None:
    parser = argparse.ArgumentParser(description="Build classic_book.json from Gutenberg Top")
    parser.add_argument("--limit", type=int, default=BOOK_LIMIT)
    parser.add_argument("--upload", action="store_true", help="Upload JSON and covers to bucket-linsia")
    args = parser.parse_args()

    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s - %(filename)s - %(funcName)s - %(lineno)d - %(levelname)s - %(message)s",
    )

    world_path = os.path.join(dist_dir, "world", "classic_book.json")
    bundled_path = os.path.join(
        os.path.dirname(dist_dir),
        "feature",
        "classicread",
        "src",
        "commonMain",
        "composeResources",
        "files",
        "classic_book.json",
    )

    oss = OSSUtil() if args.upload else None
    books = build_catalog(args.limit, upload_covers=args.upload, oss=oss)
    payload = {
        "version": next_version(world_path),
        "updatedAt": date.today().isoformat(),
        "books": books,
    }
    _save_json(world_path, payload)
    os.makedirs(os.path.dirname(bundled_path), exist_ok=True)
    shutil.copyfile(world_path, bundled_path)
    logger.info("Wrote %s and %s", world_path, bundled_path)

    if args.upload:
        assert oss is not None
        dist_url = oss.upload_file_to_r2(world_path, "classic_book.json")
        logger.info("Uploaded catalog %s", dist_url)


if __name__ == "__main__":
    main()

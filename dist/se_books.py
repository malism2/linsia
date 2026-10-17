import argparse
import json
import logging
import os
import re
import shutil
import sys
import time
from datetime import date
from typing import Any, Dict, List, Set
from urllib.parse import urljoin

import requests

dist_dir = os.path.dirname(os.path.abspath(__file__))
if dist_dir not in sys.path:
    sys.path.insert(0, dist_dir)

from util.oss_util import OSSUtil

logger = logging.getLogger(__name__)

BASE_URL = "https://standardebooks.org"
CATALOG_URL = f"{BASE_URL}/ebooks"
OSS_KEY = "se_books.json"
USER_AGENT = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
)

BOOK_LI_PATTERN = re.compile(
    r'<li[^>]*typeof="schema:Book"[^>]*(?:about|resource)="([^"]+)"[^>]*>(.*?)</li>',
    flags=re.IGNORECASE | re.DOTALL,
)


def _headers() -> Dict[str, str]:
    return {"User-Agent": USER_AGENT, "Accept": "text/html, application/json, */*"}


def _http_get(url: str, timeout: int = 60, retries: int = 5) -> requests.Response:
    last_error: Exception | None = None
    for attempt in range(retries):
        try:
            response = requests.get(url, headers=_headers(), timeout=timeout)
            response.raise_for_status()
            return response
        except requests.RequestException as exc:
            last_error = exc
            wait = min(2 ** attempt, 30)
            logger.warning("GET %s failed (%s), retry in %ss", url, exc, wait)
            time.sleep(wait)
    raise last_error if last_error else RuntimeError(f"GET failed: {url}")


def _load_json(path: str) -> Dict[str, Any]:
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def _save_json(path: str, data: Dict[str, Any]) -> None:
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def _abs_url(path: str) -> str:
    if path.startswith("http://") or path.startswith("https://"):
        return path
    return urljoin(BASE_URL, path)


def _book_id_from_path(path: str) -> str:
    path = path.strip().rstrip("/")
    for prefix in ("/ebooks/", "ebooks/"):
        if path.startswith(prefix):
            return path[len(prefix) :]
    return path.lstrip("/")


def epub_url_for_id(book_id: str) -> str:
    page_url = f"{BASE_URL}/ebooks/{book_id}"
    slug = book_id.replace("/", "_")
    return f"{page_url}/downloads/{slug}.epub?source=download"


def parse_listing_books(html: str) -> List[Dict[str, str]]:
    books: List[Dict[str, str]] = []
    for match in BOOK_LI_PATTERN.finditer(html):
        resource = match.group(1)
        block = match.group(2)
        title_match = re.search(
            r'property="schema:name"[^>]*>([^<]+)<',
            block,
            flags=re.IGNORECASE,
        )
        author_match = re.search(
            r'property="schema:author"[^>]*>.*?property="schema:name"[^>]*>([^<]+)<',
            block,
            flags=re.IGNORECASE | re.DOTALL,
        )
        img_match = re.search(r"<img\b[^>]*>", block, flags=re.IGNORECASE)
        src_match = (
            re.search(r'\bsrc="([^"]+)"', img_match.group(0), flags=re.IGNORECASE)
            if img_match
            else None
        )
        title = (title_match.group(1) if title_match else "").strip()
        author = (author_match.group(1) if author_match else "").strip()
        if not title:
            continue
        book_id = _book_id_from_path(resource)
        has_placeholder = "placeholder-cover" in block
        cover_url = "" if has_placeholder or not src_match else _abs_url(src_match.group(1))
        books.append(
            {
                "id": book_id,
                "title": title,
                "author": author,
                "coverUrl": cover_url,
            }
        )
    return books


def fetch_all_listing_books(max_pages: int = 0) -> List[Dict[str, str]]:
    seen: Set[str] = set()
    merged: List[Dict[str, str]] = []
    page = 1
    while True:
        if max_pages and page > max_pages:
            break
        url = CATALOG_URL if page == 1 else f"{CATALOG_URL}?page={page}"
        html = _http_get(url).text
        batch = parse_listing_books(html)
        if not batch:
            break
        added = 0
        for book in batch:
            if book["id"] in seen:
                continue
            seen.add(book["id"])
            merged.append(book)
            added += 1
        logger.info("Page %s: %s books (%s new, total %s)", page, len(batch), added, len(merged))
        if added == 0:
            break
        page += 1
        time.sleep(0.2)
    if not merged:
        raise RuntimeError("No books parsed from Standard Ebooks /ebooks")
    return merged


def next_version(existing_path: str) -> int:
    if not os.path.exists(existing_path):
        return 1
    try:
        current = int(_load_json(existing_path).get("version") or 0)
        return current + 1
    except Exception:
        return 1


def build_catalog(max_pages: int = 0) -> Dict[str, Any]:
    raw_books = fetch_all_listing_books(max_pages=max_pages)
    books: List[Dict[str, Any]] = []
    for rank, raw in enumerate(raw_books, start=1):
        book_id = raw["id"]
        books.append(
            {
                "id": book_id,
                "rank": rank,
                "title": raw["title"],
                "author": raw["author"],
                "coverUrl": raw["coverUrl"],
                "epubUrl": epub_url_for_id(book_id),
            }
        )
    world_path = os.path.join(dist_dir, "world", OSS_KEY)
    return {
        "version": next_version(world_path),
        "updatedAt": date.today().isoformat(),
        "source": "standardebooks.org/ebooks",
        "books": books,
    }


def main() -> None:
    parser = argparse.ArgumentParser(description="Build se_books.json from Standard Ebooks /ebooks")
    parser.add_argument("--upload", action="store_true", help=f"Upload {OSS_KEY} to bucket-linsia")
    parser.add_argument("--max-pages", type=int, default=0, help="Limit pages for testing (0 = all)")
    args = parser.parse_args()

    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s - %(filename)s - %(funcName)s - %(lineno)d - %(levelname)s - %(message)s",
    )

    world_path = os.path.join(dist_dir, "world", OSS_KEY)
    bundled_path = os.path.join(
        os.path.dirname(dist_dir),
        "feature",
        "classicread",
        "src",
        "commonMain",
        "composeResources",
        "files",
        OSS_KEY,
    )

    payload = build_catalog(max_pages=args.max_pages)
    _save_json(world_path, payload)
    os.makedirs(os.path.dirname(bundled_path), exist_ok=True)
    shutil.copyfile(world_path, bundled_path)
    logger.info("Wrote %s and %s (%s books)", world_path, bundled_path, len(payload["books"]))

    if args.upload:
        oss = OSSUtil()
        dist_url = oss.upload_file_to_r2(world_path, OSS_KEY)
        logger.info("Uploaded catalog %s", dist_url)


if __name__ == "__main__":
    main()

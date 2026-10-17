import argparse
import json
import logging
import os
import sys
from typing import Any, Dict

dist_dir = os.path.dirname(os.path.abspath(__file__))
# Allow running from repo root: ensure `dist/` is on sys.path for `util.*`
if dist_dir not in sys.path:
    sys.path.insert(0, dist_dir)

from util.oss_util import OSSUtil


logger = logging.getLogger(__name__)


def _load_json(path: str) -> Dict[str, Any]:
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def _save_json(path: str, data: Dict[str, Any]) -> None:
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)


def main() -> None:
    parser = argparse.ArgumentParser(description="Upload learn word resources to R2")
    parser.add_argument(
        "learn_word_path",
        help="Path to learn_word.json",
    )
    args = parser.parse_args()

    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s - %(filename)s - %(funcName)s - %(lineno)d - %(levelname)s - %(message)s",
    )

    learn_word_path = os.path.abspath(args.learn_word_path)
    learn_dist_path = os.path.join(os.path.dirname(learn_word_path), "learn_dist.json")

    if not os.path.exists(learn_word_path):
        raise FileNotFoundError(f"Missing input: {learn_word_path}")

    data = _load_json(learn_word_path)
    reops = data.get("reops", [])
    if not isinstance(reops, list):
        raise ValueError("learn_word.json: 'reops' must be a list")

    oss = OSSUtil()
    s3_endpoint_url = oss.S3_ENDPOINT_URL or ""

    updated = 0
    for idx, item in enumerate(reops):
        if not isinstance(item, dict):
            continue

        url = item.get("url")
        name = item.get("name")
        if not url:
            logger.info("Skip reop[%s]: missing 'url' (name=%s)", idx, name)
            continue
        if not name:
            raise ValueError(f"reops[{idx}] has 'url' but missing 'name'")
        if s3_endpoint_url and url.startswith(s3_endpoint_url):
            logger.info("Skip reop[%s]: already on S3 (name=%s)", idx, name)
            continue

        logger.info("Uploading reops[%s]: key=%s", idx, name)
        uploaded_url = oss.upload_file_to_r2(url, name + ".zip")
        if uploaded_url:
            item["url"] = uploaded_url
            updated += 1
        else:
            logger.warning("Upload failed for reops[%s]: key=%s (keep original url)", idx, name)

    out_data = dict(data)
    out_data["reops"] = reops
    _save_json(learn_dist_path, out_data)
    dist_url = oss.upload_file_to_r2(learn_dist_path, "learn_dist.json")

    logger.info("Done. Uploaded=%s, output=%s", updated, dist_url)


if __name__ == "__main__":
    main()


#!/usr/bin/env python3
"""Fetches venue photos from the Google Places API once per build.

This is the only place in the project that talks to the Places API. The
shipped app never calls it at runtime -- it just reads the manifest and
image files this script produces, bundled as assets exactly like the
venue catalog CSV. Re-run this script (or let CI run it) to refresh the
bundled photos; nothing about it runs on a user's device.

Usage:
    GOOGLE_PLACES_API_KEY=... python3 scripts/fetch_place_photos.py

Requires an *unrestricted-by-app* Places API (New) key -- the Android
app-restricted key used at runtime for anything else in this project
will not work here, since these requests don't come from the Android app.
"""
from __future__ import annotations

import csv
import hashlib
import json
import os
import shutil
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
CSV_PATH = REPO_ROOT / "app/src/main/assets/quartermuse_master_v11.csv"
PHOTOS_DIR = REPO_ROOT / "app/src/main/assets/photos"
MANIFEST_PATH = REPO_ROOT / "app/src/main/assets/photos_manifest.json"
PLACE_ID_CACHE_PATH = REPO_ROOT / "scripts/places_cache.json"

MAX_PHOTOS_PER_VENUE = 5
SEARCH_RADIUS_METERS = 350.0
MAX_PHOTO_WIDTH_PX = 1280
MAX_PHOTO_HEIGHT_PX = 960
PLACES_API_BASE = "https://places.googleapis.com/v1"


def sha1_id(venue: str, latitude_text: str, longitude_text: str) -> str:
    # Must match QuarterMuseSeed.kt's id derivation exactly so runtime
    # Place.id values line up with this manifest's keys.
    raw = f"{venue}|{latitude_text}|{longitude_text}".encode("utf-8")
    return hashlib.sha1(raw).hexdigest()


def load_venues() -> list[dict]:
    with CSV_PATH.open(encoding="utf-8-sig", newline="") as f:
        rows = list(csv.reader(f))
    header, rows = rows[0], rows[1:]
    assert header == ["Venue", "Latitude", "Longitude", "Category Tags"], f"Unexpected CSV header: {header}"

    venues = []
    for row in rows:
        if not any(field.strip() for field in row):
            continue
        venue, lat_text, lon_text, _tags = (field.strip() for field in row)
        venues.append({
            "id": sha1_id(venue, lat_text, lon_text),
            "venue": venue,
            "latitude": float(lat_text),
            "longitude": float(lon_text),
        })
    return venues


def api_request(method: str, url: str, api_key: str, body: dict | None = None, field_mask: str | None = None) -> dict:
    headers = {"X-Goog-Api-Key": api_key, "Content-Type": "application/json"}
    if field_mask:
        headers["X-Goog-FieldMask"] = field_mask
    data = json.dumps(body).encode("utf-8") if body is not None else None
    request = urllib.request.Request(url, data=data, headers=headers, method=method)
    with urllib.request.urlopen(request, timeout=30) as response:
        return json.loads(response.read().decode("utf-8"))


def resolve_place_id(venue: dict, api_key: str, cache: dict) -> str | None:
    cached = cache.get(venue["id"])
    if cached:
        return cached

    body = {
        "textQuery": venue["venue"],
        "maxResultCount": 1,
        "locationBias": {
            "circle": {
                "center": {"latitude": venue["latitude"], "longitude": venue["longitude"]},
                "radius": SEARCH_RADIUS_METERS,
            }
        },
    }
    try:
        result = api_request("POST", f"{PLACES_API_BASE}/places:searchText", api_key, body, field_mask="places.id")
    except urllib.error.HTTPError as error:
        print(f"  Text Search failed for {venue['venue']!r}: {error}", file=sys.stderr)
        return None

    places = result.get("places") or []
    if not places:
        return None
    place_id = places[0]["id"]
    cache[venue["id"]] = place_id
    return place_id


def fetch_photo_metadata(place_id: str, api_key: str) -> list[dict]:
    try:
        result = api_request(
            "GET", f"{PLACES_API_BASE}/places/{place_id}", api_key, field_mask="photos"
        )
    except urllib.error.HTTPError as error:
        print(f"  Place Details failed for {place_id}: {error}", file=sys.stderr)
        return []
    return result.get("photos", [])[:MAX_PHOTOS_PER_VENUE]


def download_photo(photo_name: str, api_key: str) -> bytes | None:
    url = (
        f"{PLACES_API_BASE}/{photo_name}/media"
        f"?maxWidthPx={MAX_PHOTO_WIDTH_PX}&maxHeightPx={MAX_PHOTO_HEIGHT_PX}&key={api_key}"
    )
    try:
        with urllib.request.urlopen(url, timeout=30) as response:
            return response.read()
    except urllib.error.HTTPError as error:
        print(f"  Photo download failed for {photo_name}: {error}", file=sys.stderr)
        return None


def main() -> int:
    api_key = os.environ.get("GOOGLE_PLACES_API_KEY", "").strip()
    if not api_key:
        print("GOOGLE_PLACES_API_KEY is not set -- skipping photo fetch. The app will build with no bundled photos.")
        return 0

    venues = load_venues()
    cache: dict = {}
    if PLACE_ID_CACHE_PATH.exists():
        cache = json.loads(PLACE_ID_CACHE_PATH.read_text(encoding="utf-8"))

    if PHOTOS_DIR.exists():
        shutil.rmtree(PHOTOS_DIR)
    PHOTOS_DIR.mkdir(parents=True)

    manifest: dict[str, list[dict]] = {}

    for venue in venues:
        place_id = resolve_place_id(venue, api_key, cache)
        if not place_id:
            print(f"No Google Place match for {venue['venue']!r}; skipping.")
            continue

        photos = fetch_photo_metadata(place_id, api_key)
        if not photos:
            continue

        venue_dir = PHOTOS_DIR / venue["id"]
        venue_dir.mkdir(parents=True, exist_ok=True)
        entries = []
        for index, photo in enumerate(photos):
            image_bytes = download_photo(photo["name"], api_key)
            if image_bytes is None:
                continue
            file_name = f"{index}.jpg"
            (venue_dir / file_name).write_bytes(image_bytes)
            entries.append({
                "file": file_name,
                "authors": [
                    {"name": author.get("displayName", ""), "uri": author.get("uri", "")}
                    for author in photo.get("authorAttributions", [])
                    if author.get("displayName")
                ],
                "googleMapsUri": photo.get("googleMapsUri", ""),
            })
            time.sleep(0.1)

        if entries:
            manifest[venue["id"]] = entries
            print(f"Fetched {len(entries)} photo(s) for {venue['venue']!r}.")
        else:
            venue_dir.rmdir()

    MANIFEST_PATH.write_text(json.dumps(manifest, indent=2, ensure_ascii=False), encoding="utf-8")
    PLACE_ID_CACHE_PATH.write_text(json.dumps(cache, indent=2, sort_keys=True), encoding="utf-8")
    print(f"Wrote {len(manifest)} venue photo entries to {MANIFEST_PATH}.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

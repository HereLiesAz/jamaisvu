#!/usr/bin/env python3
"""Fetches venue photos and business details from the Google Places API once per build.

This is the only place in the project that talks to the Places API. The
shipped app never calls it at runtime -- it just reads the manifests and
image files this script produces, bundled as assets exactly like the
venue catalog CSV. Re-run this script (or let CI run it) to refresh the
bundled data; nothing about it runs on a user's device.

Alongside photos, this fetches each venue's phone number, website,
address, opening hours, and Google's own place-type taxonomy, all
written to place_details_manifest.json. Place types feed the catalog's
search tags directly. Review text is fetched too, but purely to test it
against a fixed keyword vocabulary for additional search terms people
would plausibly search for -- the review text itself is never returned
from that step, never written to the manifest, and never bundled into
the app in any form. See extract_review_keywords().

Usage:
    GOOGLE_PLACES_API_KEY=... python3 scripts/fetch_place_photos.py

Requires an *unrestricted-by-app* Places API (New) key -- the Android
app-restricted key used at runtime for anything else in this project
will not work here, since these requests don't come from the Android app.
"""
from __future__ import annotations

import csv
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
PHOTOS_MANIFEST_PATH = REPO_ROOT / "app/src/main/assets/photos_manifest.json"
PLACE_DETAILS_MANIFEST_PATH = REPO_ROOT / "app/src/main/assets/place_details_manifest.json"
PLACE_ID_CACHE_PATH = REPO_ROOT / "scripts/places_cache.json"

MAX_PHOTOS_PER_VENUE = 5
MAX_REVIEWS_PER_VENUE = 5
SEARCH_RADIUS_METERS = 350.0
MAX_PHOTO_WIDTH_PX = 1280
MAX_PHOTO_HEIGHT_PX = 960
PLACES_API_BASE = "https://places.googleapis.com/v1"

PLACE_DETAILS_FIELD_MASK = ",".join([
    "photos",
    "nationalPhoneNumber",
    "websiteUri",
    "formattedAddress",
    "regularOpeningHours.weekdayDescriptions",
    "regularOpeningHours.periods",
    "types",
    "reviews.text",
])

# Google's own type taxonomy is broad; these are too generic to work as search tags.
GENERIC_PLACE_TYPES = {"point_of_interest", "establishment", "food", "store"}

# A fixed, curated vocabulary matched against review text to surface search terms a
# guest would actually type -- deliberately not frequency analysis or ML, which tend to
# surface generic filler words ("great", "service") rather than specific, searchable
# nouns. Expand this list over time; it's the only thing review text is ever used for.
REVIEW_KEYWORD_VOCABULARY = [
    "beignets", "gumbo", "jambalaya", "po boy", "poboy", "oysters", "crawfish",
    "etouffee", "red beans", "muffuletta", "king cake", "pralines", "sazerac",
    "hurricane", "hand grenade", "absinthe", "bloody mary", "mimosa", "bourbon",
    "whiskey", "craft beer", "espresso martini", "brunch", "cajun", "creole",
    "seafood", "steakhouse", "vegetarian", "vegan", "gluten free", "dessert",
    "fine dining", "jazz", "brass band", "blues", "live music", "karaoke",
    "dancing", "burlesque", "drag show", "trivia", "piano bar", "courtyard",
    "balcony", "rooftop", "patio", "speakeasy", "dive bar", "historic",
    "haunted", "romantic", "cozy", "reservations", "outdoor seating",
    "late night", "happy hour", "dog friendly", "wheelchair accessible",
]


def load_venues() -> list[dict]:
    with CSV_PATH.open(encoding="utf-8-sig", newline="") as f:
        rows = list(csv.reader(f))
    header, rows = rows[0], rows[1:]
    expected = ["Id", "Venue", "Latitude", "Longitude", "Category Tags"]
    assert header == expected, f"Unexpected CSV header: {header}"

    venues = []
    for row in rows:
        if not any(field.strip() for field in row):
            continue
        venue_id, venue, lat_text, lon_text, _tags = (field.strip() for field in row)
        venues.append({
            "id": venue_id,
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


def fetch_place_details(place_id: str, api_key: str) -> dict:
    try:
        return api_request(
            "GET", f"{PLACES_API_BASE}/places/{place_id}", api_key,
            field_mask=PLACE_DETAILS_FIELD_MASK,
        )
    except urllib.error.HTTPError as error:
        print(f"  Place Details failed for {place_id}: {error}", file=sys.stderr)
        return {}


def readable_type(place_type: str) -> str:
    return place_type.replace("_", " ").title()


def extract_review_keywords(reviews: list[dict]) -> list[str]:
    """Tests review text against a fixed vocabulary for search-relevant terms, then
    forgets the text. Returns only whichever vocabulary terms matched -- the review
    text itself never leaves this function."""
    combined = " ".join(review.get("text", {}).get("text", "") for review in reviews).lower()
    return [term for term in REVIEW_KEYWORD_VOCABULARY if term in combined]


def build_opening_hours_periods(regular_opening_hours: dict) -> list[dict]:
    periods = []
    for period in regular_opening_hours.get("periods", []):
        open_info = period.get("open", {})
        close_info = period.get("close")
        entry = {
            "openDay": open_info.get("day"),
            "openTime": f"{open_info.get('hour', 0):02d}:{open_info.get('minute', 0):02d}",
            "closeDay": close_info.get("day") if close_info else None,
            "closeTime": (
                f"{close_info.get('hour', 0):02d}:{close_info.get('minute', 0):02d}"
                if close_info else None
            ),
        }
        periods.append(entry)
    return periods


def build_place_details_entry(details: dict) -> dict:
    entry: dict = {}
    if details.get("nationalPhoneNumber"):
        entry["phone"] = details["nationalPhoneNumber"]
    if details.get("websiteUri"):
        entry["website"] = details["websiteUri"]
    if details.get("formattedAddress"):
        entry["address"] = details["formattedAddress"]

    opening_hours = details.get("regularOpeningHours", {})
    if opening_hours.get("weekdayDescriptions"):
        entry["weekdayDescriptions"] = opening_hours["weekdayDescriptions"]
    periods = build_opening_hours_periods(opening_hours)
    if periods:
        entry["periods"] = periods

    types = {readable_type(t) for t in details.get("types", []) if t not in GENERIC_PLACE_TYPES}
    review_keywords = set(extract_review_keywords(details.get("reviews", [])[:MAX_REVIEWS_PER_VENUE]))
    tags = sorted(types | review_keywords)
    if tags:
        entry["tags"] = tags

    return entry


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
        print(
            "GOOGLE_PLACES_API_KEY is not set -- skipping the Places fetch. The app will "
            "build with no bundled photos or business details."
        )
        return 0

    venues = load_venues()
    cache: dict = {}
    if PLACE_ID_CACHE_PATH.exists():
        cache = json.loads(PLACE_ID_CACHE_PATH.read_text(encoding="utf-8"))

    if PHOTOS_DIR.exists():
        shutil.rmtree(PHOTOS_DIR)
    PHOTOS_DIR.mkdir(parents=True)

    photos_manifest: dict[str, list[dict]] = {}
    place_details_manifest: dict[str, dict] = {}

    for venue in venues:
        place_id = resolve_place_id(venue, api_key, cache)
        if not place_id:
            print(f"No Google Place match for {venue['venue']!r}; skipping.")
            continue

        details = fetch_place_details(place_id, api_key)
        if not details:
            continue

        details_entry = build_place_details_entry(details)
        if details_entry:
            place_details_manifest[venue["id"]] = details_entry

        photos = details.get("photos", [])[:MAX_PHOTOS_PER_VENUE]
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
            photos_manifest[venue["id"]] = entries
            print(f"Fetched {len(entries)} photo(s) for {venue['venue']!r}.")
        else:
            venue_dir.rmdir()

    PHOTOS_MANIFEST_PATH.write_text(json.dumps(photos_manifest, indent=2, ensure_ascii=False), encoding="utf-8")
    PLACE_DETAILS_MANIFEST_PATH.write_text(
        json.dumps(place_details_manifest, indent=2, ensure_ascii=False), encoding="utf-8"
    )
    PLACE_ID_CACHE_PATH.write_text(json.dumps(cache, indent=2, sort_keys=True), encoding="utf-8")
    print(f"Wrote {len(photos_manifest)} venue photo entries to {PHOTOS_MANIFEST_PATH}.")
    print(f"Wrote {len(place_details_manifest)} venue detail entries to {PLACE_DETAILS_MANIFEST_PATH}.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

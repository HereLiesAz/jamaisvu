# Roadmap

The client's Build Priority list from [`product-direction.md`](product-direction.md),
tracked here so progress survives context resets instead of being re-derived from chat or
PR history each session. Update this file in the same PR that moves an item's status.

## Build priority

1. **Hotel selection and saved Home Lantern** -- *mostly done*
   - Done: a scrollable hotel picker (`HotelAnchorPrompt` in `ui/Lantern.kt`), "Use my
     location" (on-device GPS, no account) and "I'm not staying at a hotel," all persisted
     locally (`LamplightViewModel` + `HotelAnchor`); a proactive location fetch on app open
     (`ProactiveLocationEffect`) that checks the fix against the bundled hotel catalog
     (`HotelCatalog`, `nearestHotelWithin`) and offers a one-tap "Staying at X?" confirmation
     when it lands within ~120m of a known hotel; a persistent Home Lantern FAB (top-right)
     with a "Take me back" sheet.
   - Seed data caveat: `assets/hotels.csv` currently has 5 well-known French Quarter hotels
     with coordinates sourced from Wikipedia/mapping sites during this work, not from the
     client. Treat it as a starter list to expand, the same way the venue CSV grew --
     precision matters here specifically because the proximity-match depends on it.
2. **Home screen with one excellent "next move"** -- *not started*. The current home screen
   (`ExploreScreen`) is still search/filter over the full catalog, not a single
   recommendation card. This is the next screen-level piece of work.
3. **Place cards and place-detail view** -- *partial*. `MosaicPlaceCard` shows photo, name,
   and walk-time-from-hotel. `PlaceDetail` additionally shows open/closed-now status and a
   "DETAILS" section (phone, website, address, today's hours) when the Places pipeline
   found a match (`BundledPlaceDetails`, `OpeningHours.kt`) -- tappable phone/website open
   the dialer/browser. Still missing: "good for" tags, practical notes (dress code,
   cash/card, reservations -- not reliably available from Places data), and the
   **Go now** / **Add to tonight** / **Next nearby** actions, which depend on features later
   in this list (Tonight).
4. **"Tonight" three-to-four-stop loop** -- *not started*. Depends on #2/#3 groundwork.
5. **Maps handoff, including "Take me back"** -- *done* (this app is Android-only, so this
   is a Google Maps handoff; there's no Apple Maps counterpart to build here).
   `PlaceDetail`'s "Open in Maps" predates this work; `openWalkingDirections` in
   `ui/Lantern.kt` adds the Home Lantern's walking-directions handoff.
6. **Discover categories, especially Happy Hour** -- *not started as specified*. The
   Explore tab filters by whatever tags happen to be in the CSV, not the client's fixed
   eight categories (Drinks, Food, Happy Hour, Music, Shops, Indoor, Late, History). Needs
   either a category-mapping pass over the existing tags or a CSV column addition.
7. **Persona copy/ranking layer** -- *not started*.
8. **Lantern List** -- *partial, and diverging from spec*. Saved/Been There are now filter
   chips on the single Explore screen rather than separate tabs (the bottom navigation bar
   was removed entirely), but still aren't organized into the brief's Tonight/Later/Next
   trip sections. "Been There" as a concept isn't part of the client's Lantern List at all --
   worth a decision (keep it as a bonus feature alongside the new structure, fold its
   meaning into "Next trip," or drop it) rather than silently carrying it forward.

## Not in the original build-priority list, now in progress or queued

- **Business details and richer search tags** -- *done*. `scripts/fetch_place_photos.py`
  now fetches phone, website, address, and opening hours alongside photos, plus Google's
  place `types` and a small set of terms matched against review text against a fixed
  vocabulary (`REVIEW_KEYWORD_VOCABULARY`) -- review text itself is discarded immediately
  and never written to `place_details_manifest.json` or bundled into the app in any form.
  `BundledPlaceDetails.kt` loads it; `OpeningHours.kt` derives open/closed-now from the
  structured hours. The enriched tags widen free-text search (`ExploreScreen`) without
  cluttering the curated tag-filter chips, which stay CSV-only.
- **Group size and vibe questions** -- *selectors done, no recommendation logic yet*.
  `GroupSize` (Solo/2-4/5+) and `Vibe` (all 16 from the shelved Brief 1 pricing spec:
  Romantic, Curious, Business-Safe, etc.) in `Models.kt`, persisted like the hotel anchor.
  `MoodPrompt` (`ui/Mood.kt`) shows both questions together on one screen, reachable from a
  top-left icon button and shown once on first open (yielding to a pending hotel-detection
  confirmation rather than stacking dialogs). Kept as free selectors without reviving Brief
  1's paywall around them. Nothing reads these values yet to actually change what's
  recommended or how results are ranked -- that's the persona/vibe-driven layer, item 7
  below, not yet built.
- **Seen, as its own thing from Been** -- *done*. "Seen" (`LamplightViewModel.isSeen`/
  `markSeen`) is a new, auto-tracked record of having opened a place's detail screen at
  least once, with no "un-see." "Been" is untouched: still a deliberate manual toggle for an
  actual real-world visit. Both are separate filter chips (Saved / Been / Seen) on Explore.
- **Typography** -- *done*. Uncut Sans has no confirmed license for bundling; Archivo (SIL
  OFL, Google Fonts, true Black weight) replaces it as the app-wide default. Martian Mono
  (already cleared as license-safe, but not actually wired up until now) is applied at the
  utility-label call sites the brief names. See `design-system.md`.
- **Monetization**: confirmed business-side (e.g. paid placement or a claimed/verified
  listing), not a consumer paywall -- Brief 1's Free/One Night/One Week tiers stay shelved.
  The specific mechanism hasn't been chosen yet.
- **Expanded content catalogs**: background research is compiling more-comprehensive lists
  of New Orleans hotels, restaurants/bars, and landmarks/parks/tourist-activities, at the
  user's request, despite the tension with the client brief's explicit "deliberately small,
  curated list... not an inventory dump" principle for the venue catalog specifically (the
  hotel catalog was always meant to grow this way; the venue catalog is a real judgment call
  in progress). How the results get folded in -- especially whether they replace or
  supplement the curated 143-venue `quartermuse_master_v11.csv` -- is not yet decided.

## Explicitly out of scope for now

Per the client brief's "Explicitly Do Not Build" list -- see `product-direction.md` and
`design-system.md` for the full list and the reasoning. Brief 1's user-facing subscription
tiers stay shelved regardless of what shape business-side monetization eventually takes.

## Success test

> A hotel guest who has no plan can open Lamplight, choose a hotel, get somewhere genuinely
> good within ten minutes, and easily find their way back afterward.

Item 1 (this PR) and item 5 get the "find their way back" half working end to end. The
"get somewhere genuinely good" half needs items 2-3 before that test is actually passable.

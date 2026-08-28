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
3. **Place cards and place-detail view** -- *partial*. `MosaicPlaceCard` and `PlaceDetail`
   show photo, name, and (new) walk-time-from-hotel. Still missing: open/closed status,
   "good for" tags, practical notes (dress code, cash/card, reservations), and the
   **Go now** / **Add to tonight** / **Next nearby** actions -- most of these depend on
   catalog data the CSV doesn't carry yet (hours, practical notes) or on features later in
   this list (Tonight).
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

- **Business details** (phone, hours, website, address) and **richer search tags** (from
  Google's place `types` plus terms mined from review text, never the review text itself) --
  both extend the existing build-time Places pipeline (`scripts/fetch_place_photos.py`),
  the same "collect once" pattern already used for photos. Not yet built.
- **Group size and vibe questions** (Solo/2-4/5+; Romantic, Curious, Business-Safe, etc.) --
  originally defined in the shelved Brief 1 pricing spec as paid-tier axes, now wanted as
  free onboarding selectors shown together on one screen, separate from the hotel question.
  Not yet built.
- **Monetization**: confirmed business-side (e.g. paid placement or a claimed/verified
  listing), not a consumer paywall -- Brief 1's Free/One Night/One Week tiers stay shelved.
  The specific mechanism hasn't been chosen yet.

## Explicitly out of scope for now

Per the client brief's "Explicitly Do Not Build" list -- see `product-direction.md` and
`design-system.md` for the full list and the reasoning. Brief 1's user-facing subscription
tiers stay shelved regardless of what shape business-side monetization eventually takes.

## Success test

> A hotel guest who has no plan can open Lamplight, choose a hotel, get somewhere genuinely
> good within ten minutes, and easily find their way back afterward.

Item 1 (this PR) and item 5 get the "find their way back" half working end to end. The
"get somewhere genuinely good" half needs items 2-3 before that test is actually passable.

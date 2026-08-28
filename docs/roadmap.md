# Roadmap

The client's Build Priority list from [`product-direction.md`](product-direction.md),
tracked here so progress survives context resets instead of being re-derived from chat or
PR history each session. Update this file in the same PR that moves an item's status.

## Build priority

1. **Hotel selection and saved Home Lantern** -- *in progress*
   - Done: "Use my location" (on-device GPS, no account) and "I'm not staying at a hotel,"
     both persisted locally (`LamplightViewModel` + `HotelAnchor`); persistent Home Lantern
     FAB with a "Take me back" sheet (`ui/Lantern.kt`).
   - Not done: "Hotel selection from a short list" -- needs a curated hotel dataset
     (name + coordinates) from the client, the same way `quartermuse_master_v11.csv`
     supplies venues. Until then, guests can only set the anchor by standing at the hotel
     and using their location.
2. **Home screen with one excellent "next move"** -- *not started*. The current home tab
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
8. **Lantern List** -- *partial, and diverging from spec*. The existing Saved/Been There
   tabs aren't organized into the brief's Tonight/Later/Next trip sections. "Been There" as
   a concept isn't part of the client's Lantern List at all -- worth a decision (keep it as
   a bonus feature alongside the new structure, fold its meaning into "Next trip," or drop
   it) rather than silently carrying it forward.

## Explicitly out of scope for now

Per the client brief's "Explicitly Do Not Build" list -- see `product-direction.md` and
`design-system.md` for the full list and the reasoning. The monetization tiers in Brief 1
(`product-direction.md`) are shelved, not cancelled; revisit only if the client raises it
again.

## Success test

> A hotel guest who has no plan can open Lamplight, choose a hotel, get somewhere genuinely
> good within ten minutes, and easily find their way back afterward.

Item 1 (this PR) and item 5 get the "find their way back" half working end to end. The
"get somewhere genuinely good" half needs items 2-3 before that test is actually passable.

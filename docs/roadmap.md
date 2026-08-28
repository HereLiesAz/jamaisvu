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
   - Seed data: `assets/hotels.csv` now has 185 hotels across Orleans Parish plus the
     Kenner/Metairie airport cluster, replacing the original 5-hotel starter list.
     Sourced via Wikipedia's coordinates API, OpenStreetMap Overpass queries, Nominatim
     address geocoding, and targeted web searches for renamed/rebranded properties --
     not from the client, and not claimed as literally exhaustive (background research
     put it at roughly 75-85% of all individually-nameable operating lodging in the
     metro, higher for the neighborhoods a French-Quarter-visiting guest actually books
     in). Precision matters here specifically because the proximity-match depends on it;
     every coordinate has a cited source, none are guessed.
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
- **Featured, as a filter and a tag** -- *scaffold done, process deliberately not built
  yet*. `Place.featured` (`Models.kt`) is a new sixth CSV column
  (`quartermuse_master_v11.csv`, `Featured` = `TRUE`/`FALSE`), defaulting `FALSE` on all 419
  existing rows. Explore has a "Featured" filter chip alongside Saved/Been/Seen; a featured
  place also gets a small amber "FEATURED" badge on its mosaic card and its detail screen.
  What's deliberately not built: any way to actually set a place featured beyond hand-editing
  the CSV. That's the business/monetization mechanism above, not yet decided -- this is the
  display-only half, built ahead of it on request.
- **Expanded content catalogs**: background research compiled more-comprehensive lists of
  New Orleans hotels, restaurants/bars, and landmarks/parks/tourist-activities, at the
  user's explicit request overriding the client brief's "deliberately small, curated
  list... not an inventory dump" principle for the venue catalog. All three are done.
  - **Hotels**: see item 1 above -- a separate catalog from the venue CSV, used only for
    the picker and proximity match, so "complete" was never in tension with the curation
    principle there. 185 hotels, replacing the original 5-hotel starter list.
  - **Landmarks/parks/tourist-activities**: 64 new rows appended directly to
    `quartermuse_master_v11.csv`, same schema, checked against the pre-existing 143 venues
    for duplicates before merging (one, the Old U.S. Mint / New Orleans Jazz Museum, was
    already present under a different name and wasn't re-added). One tag was normalized on
    merge: the research used "Historic Landmark," the catalog's existing convention is
    "Historic Site" -- all 64 rows were rewritten to match before appending.
  - **Restaurants/bars**: 272 candidate rows researched; 22 with no sourced coordinate were
    excluded rather than guessed at (kept out of the catalog entirely -- see the research
    agent's own report for the excluded names if they're worth manually geocoding later).
    Of the remaining 250, 38 turned out to already be in the catalog (26 by exact id
    collision, 12 by exact case-insensitive name match once ids differed) and were skipped
    in favor of the existing curated entry. The other 212 were appended, tags normalized to
    the catalog's "; "-separated convention.
  - **Result**: `quartermuse_master_v11.csv` grew from 143 to 419 venues (420 lines
    including header). This is a real, deliberate departure from "deliberately small,
    curated" for raw count -- the user's call, made explicitly and more than once. Whether
    Explore/Discover need a "show me the curated highlights" vs. "show me everything"
    distinction now that the catalog is 3x its original size is an open design question,
    not yet raised with the user.
- **Kotlin Multiplatform + a web target, at the user's explicit request** -- *the approved
  10-PR sequence is complete; real end-to-end web verification (a live Pages check, actual
  browser behavior for geolocation/etc., the still-deferred photo-copying CI step) is what's
  left*. Adds a shareable web build (reached via a link or QR code, not meant to be
  search-discoverable) alongside the existing Android app, one shared UI/logic codebase for
  both, using Compose Multiplatform's web target (Kotlin/Wasm -- JetBrains labels this Beta,
  not stable, a risk accepted deliberately rather than glossed over). Also bumps the whole
  toolchain to current latest stable, and JDK to 21, per request. A 10-PR sequence, each
  keeping Android shippable throughout -- never breaking it to make progress on web:
  - **PR0** *(merged)*: JDK 21, Kotlin 2.4.10, `core-ktx` 1.19.0 (everything else was already
    latest stable), and a new `gradle/libs.versions.toml` version catalog, all on the
    then-single `:app` module.
  - **PR1** *(merged, #28 -- PR #27, "PR0," was itself only version bumps with no
    multiplatform code, which read as if nothing was happening; #28 is where the actual
    module split landed)*: split `:app` into `:shared` (a Kotlin Multiplatform library,
    `androidTarget()` only for now) and `:androidApp` (a thin `com.android.application`
    shell) -- zero commonMain, zero expect/actual yet, every file moved unchanged. Real AGP-9
    gotchas surfaced and resolved along the way, worth recording since they're easy to
    re-trip on in later PRs: an Android-targeted KMP library now needs
    `com.android.kotlin.multiplatform.library` (applied *alongside*, not instead of,
    `kotlin("multiplatform")`) rather than the older `com.android.library` combo; its DSL is
    new (`kotlin { android { ... } }`, `jvmToolchain(21)`, `withHostTest {}`, an
    `androidHostTest` source set replacing `androidUnitTest`); it needs
    `androidResources { enable = true }` explicitly or its own R class never generates; a
    library-module manifest needs fully-qualified component names, not `.MainActivity`-style
    relative ones, once its Gradle namespace differs from its classes' actual package; and
    manifest-referenced app-identity resources (the launcher icon, `app_name`, the base
    theme, `file_paths.xml`) needed to move into `:androidApp` itself rather than staying in
    `:shared`, since this plugin doesn't merge a KMP library's `res/` into a consuming app's
    build the way a classic library does. Most consequentially: the bare `test` Gradle task
    silently stopped running any of `:shared`'s unit tests at all (no error, just zero tests
    executed) -- CI's `./gradlew test assembleDebug`/`assembleRelease` are now
    `./gradlew allTests assembleDebug`/`assembleRelease`, the aggregate task that actually
    covers every Kotlin target's tests (and will keep covering wasmJs's once PR2 adds it).
  - **PR3** *(done ahead of PR2, #29 -- it didn't need the wasmJs work, which was still being
    researched)*: `Models.kt`, `Csv.kt`, `WalkTime.kt` moved into `commonMain`. Scope
    narrowed from the original plan on closer inspection: `OpeningHours.kt` turned out to use
    `java.time.*` throughout (not caught by an Android-import check, since that's a JVM-
    standard-library dependency, not an Android one) and needs a real `kotlinx-datetime`
    port, not a pure relocate -- still not done as of PR2, now that PR2 gives `:shared` an
    actual second target to verify it against. `WalkTime.kt` had the same category of hidden
    issue in miniature (`Math.toRadians`, resolved with no visible import) -- fixed in place.
    Full detail in `kmp-web-migration-plan.md`.
  - **PR2** *(this one)*: added a `wasmJs` target to `:shared` and a new `:webApp` module,
    with a `SharedTransitionLayout`/`sharedBounds` spike screen proving the shared-element
    transition this app's mosaic-to-detail hero animation depends on actually works on
    wasmJs, not just Android -- the single biggest technical risk in the plan, now resolved
    at the compile/API level. Wired up GitHub Pages deployment in CI
    (`build-web`/`deploy-web` jobs). Real Compose Multiplatform, not just Android Compose
    relocated -- `org.jetbrains.compose.*` multiplatform artifacts in `commonMain`, alongside
    the existing `androidx.compose.*` ones the pre-existing Android-only UI still uses (both
    coexist fine; JetBrains' Android-target Compose Multiplatform artifacts are themselves
    backed by the corresponding AndroidX ones). This sandbox's network policy blocks the
    Kotlin/Wasm toolchain's Node.js/Yarn setup (a GitHub-tarball fetch, `codeload.github.com`,
    is disallowed), so `allTests` and the actual `wasmJsBrowserDistribution` build couldn't be
    verified end-to-end locally -- verified instead: both wasmJs targets compile clean, and
    Android is fully unaffected (all 28 `:shared` tests, both APK variants). See
    `kmp-web-migration-plan.md`'s PR2 section for the full gotcha list. Real CI is the
    verification point for the actual web build and live Pages deploy.
  - **PR4** *(this one)*: added the `SettingsStore` seam (`commonMain`) with
    `AndroidSettingsStore` (`SharedPreferences`, unchanged behavior) and `BrowserSettingsStore`
    (`localStorage`, via the `kotlinx-browser` library). Narrower than the original plan's
    wording implied: `LamplightViewModel` itself stays in `androidMain` for now -- it still
    depends on `Application`/`Context`, `Location`, and the entire Android-only GitHub-update
    surface that PR9 is what actually extracts, so moving it early would mean either a class
    that still doesn't compile for wasmJs or doing later PRs' work out of order. It now
    constructs its own `AndroidSettingsStore` internally instead of raw `SharedPreferences`;
    real constructor injection (needed once `:webApp` also constructs the class) arrives with
    PR9. All 28 `:shared` tests and both APK variants still green. Full detail in
    `kmp-web-migration-plan.md`.
  - **PR5** *(this one)*: added the `LocationProvider`/`GeoPosition` seam (`commonMain`),
    replacing `android.location.Location` everywhere it's read. `AndroidLocationProvider`
    wraps the existing `requestOneTimeLocation` unchanged; `Lantern.kt`'s two location-using
    composables now go through it. `BrowserLocationProvider` -- this plan's flagged unknown --
    wraps `navigator.geolocation.getCurrentPosition` via a single `@JsFun`-bridged JS callback
    resolving a plain string, sidestepping the more failure-prone structured-object marshaling
    a naive port would need. Compiles clean on wasmJs, but **actual browser runtime behavior
    is not verified in this sandbox** (same `codeload.github.com` block as PR2) -- a real
    manual check belongs on the live Pages deploy once a web onboarding flow calls it (PR9).
    `LamplightViewModel` and `Lantern.kt` both stay in `androidMain` for now, same reasoning
    as PR4. All 28 `:shared` tests and both APK variants still green. Full detail in
    `kmp-web-migration-plan.md`.
  - **PR6** *(this one)*: added `rememberUrlOpener()` (`commonMain`, Android `Intent` /
    web `window.open`), applied to `PlaceDetail`'s phone/website rows and `openMaps` (the
    last now opens a cross-platform Google Maps URL instead of the Android-only `geo:`
    scheme -- a small real behavior change on Android, still opens the Maps app there).
    `openWalkingDirections` deliberately left untouched: its Maps-app-then-web-fallback
    logic doesn't fit the opener's plain "open this URL" shape without a real design
    decision, better made in PR9 alongside `Lantern.kt`'s actual move to `commonMain`. All
    28 `:shared` tests and both APK variants still green. Full detail in
    `kmp-web-migration-plan.md`.
  - **PR7** *(this one)*: rewrote `PhotoAttribution` to drop `AndroidView`/`TextView`/
    `Html.fromHtml` entirely in favor of `buildAnnotatedString` + `withLink(LinkAnnotation.Url(...))`
    -- core Compose text APIs, so this is a straight quality improvement on Android with no
    new dependency and no `commonMain` seam needed at all. Compiled clean on the first try.
    All 28 `:shared` tests and both APK variants still green. Full detail in
    `kmp-web-migration-plan.md`.
  - **PR8, fonts sub-piece** *(split from the original plan)*: moved the Archivo/Martian
    Mono TTFs and `Theme.kt` itself to `commonMain`, using Compose Multiplatform's
    resource-aware `Font(...)`. Most of the real work was getting Compose Resources' code
    generation to run at all (`generateResClass` needed forcing to `always` -- its default
    `Auto` heuristic doesn't fire for this module) and adding the runtime library
    (`org.jetbrains.compose.components:components-resources`) the generated code needs to
    compile.
  - **PR8's remaining scope** *(this one -- CSV/JSON, photo binaries, loading state)*: moved
    the venue/hotel CSVs and both their loaders to `commonMain` (`suspend`, `Res.readBytes`).
    `BundledPhotos`/`BundledPlaceDetails` had a second hidden Android-only dependency this
    plan's "Android imports only" check missed: `org.json.*`, replaced with
    `kotlinx.serialization.json`'s `JsonElement` tree navigation. A third hidden dependency,
    the `file:///android_asset/...` URI construction inside `BundledPhotos`, is now the
    `photoBaseUri()` seam (Android unchanged; wasmJs returns a relative `"photos/"` path).
    `LamplightViewModel`'s four eager catalog properties are now backed by a `Catalog?`
    loaded once via `viewModelScope.launch`, degrading to empty/`false` while loading rather
    than needing a dedicated loading screen -- existing call sites needed zero changes. Two
    things deliberately **not yet done**, flagged rather than silently left: the CI step that
    copies photo binaries into the web build (so `photoBaseUri()`'s web path currently 404s,
    a real but non-crashing gap -- `build-web` doesn't run `fetch_place_photos.py` at all
    today), and any real verification of the JSON loaders against actual manifest data
    (gitignored, only ever exists after a live Places API fetch this sandbox has no
    credentials to run -- only the graceful-empty path was exercised here). All 28 `:shared`
    tests (including the two relocated CSV suites) and both APK variants still green. Full
    detail, including the exact scope of what "not yet done" means, in
    `kmp-web-migration-plan.md`.
  - **PR9, complete**: `coil-compose` moved from `io.coil-kt:coil-compose:2.7.0`
    (Android-only) to `io.coil-kt.coil3:coil-compose:3.6.0` (the multiplatform rewrite),
    `coil-network-ktor3` deliberately deferred until `AsyncImage` actually needs real HTTP
    fetches on web. `LamplightViewModel`'s GitHub-releases self-update state/logic extracted
    into a new `GitHubUpdateController` (`androidMain`), exactly the "extract before moving
    the rest of the class" step this doc's plan called for -- fed into `LamplightApp`'s new
    `platformBanner` slot via a new `AndroidUpdateBanner` composable that `MainActivity`
    constructs and passes in, replacing the old inline `UpdateBanner`/`playUpdateStatus`
    logic. `LamplightViewModel` itself then moved to `commonMain` (constructor now takes
    `SettingsStore` directly, extends the multiplatform `ViewModel`), along with `Mood.kt`
    (no changes needed), `Lantern.kt` (needed two new seams: `rememberWalkingDirectionsOpener()`
    for the deferred `openWalkingDirections` app-then-web logic from PR6, and
    `rememberLocationRequester()` wrapping Android's permission-prompt dance so the browser's
    own automatic prompt is all web needs), and `LamplightApp.kt` itself -- this doc's own
    "biggest and riskiest single file" -- which needed `java.time` replaced with
    `kotlinx-datetime` 0.8.0, the AGP-generated `R` class replaced with Compose Resources'
    `Res.drawable`, and `androidx.activity.compose.BackHandler` (no multiplatform equivalent)
    given the same expect/actual treatment as everything else (a no-op on web). Finally wired
    `:webApp`'s actual entry point to construct the real `LamplightViewModel`/`LamplightApp`
    instead of the PR2 spike screen (now deleted) -- not spelled out as its own PR line item,
    but the actual point of moving the UI to `commonMain` in the first place. Also restyled
    the lamppost watermark per direct feedback (full screen height, behind the whole home
    screen, not just the explore header) -- not visually verified, no emulator or browser
    available in this sandbox to check it against. All 28 `:shared` tests, both wasmJs
    targets, and both APK variants still green throughout. Full detail in
    `kmp-web-migration-plan.md`.
  - **PR10, complete**: `codeql.yml`'s JDK pin (19, predating this migration) now matches
    the rest of the project's 21; `build-web` gets the same "Report Failure to Jules"
    auto-issue-filing `build-and-release` already had. Final Android-parity check: signing,
    versioning, and the sideload update flow are all a straight extraction, not a rewrite,
    and untouched Android-only surfaces (`MainActivity`, manifest, signing config) confirm
    it -- though, same as the watermark restyle above, with no on-device verification
    possible in this sandbox. See [`kmp-web-migration-plan.md`](kmp-web-migration-plan.md)
    for the full detail and the risks that carry forward past this plan's own scope (browser
    floor, bundle size, the deferred web photo-copying CI step from PR8, a Google Maps
    Platform compliance question worth a real check before publishing bundled photo content
    to a public static site).

## Explicitly out of scope for now

Per the client brief's "Explicitly Do Not Build" list -- see `product-direction.md` and
`design-system.md` for the full list and the reasoning. Brief 1's user-facing subscription
tiers stay shelved regardless of what shape business-side monetization eventually takes.

## Success test

> A hotel guest who has no plan can open Lamplight, choose a hotel, get somewhere genuinely
> good within ten minutes, and easily find their way back afterward.

Item 1 (this PR) and item 5 get the "find their way back" half working end to end. The
"get somewhere genuinely good" half needs items 2-3 before that test is actually passable.

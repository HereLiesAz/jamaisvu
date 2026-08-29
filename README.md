# Lamplight

*Best lit plans.*

Lamplight is a mobile-first French Quarter discovery concierge for the fixed QuarterMuse
venue database, not a broad social-discovery app. It may take visual inspiration from Pao's
editorial polish, but it is deliberately not a Pao clone: no social profiles, following,
user uploads, comments, likes, crowdsourced ratings, or open-ended global-city database. The
app does not accept user-submitted places and does not use a cloud database, account
system, or social backend.

The product north star: **a hotel guest opens Lamplight, confirms where they are staying,
and immediately gets one excellent, practical next move.** See
[`docs/product-direction.md`](docs/product-direction.md) for the full client brief this app
is being built against, [`docs/design-system.md`](docs/design-system.md) for the visual
system, and [`docs/roadmap.md`](docs/roadmap.md) for what's built versus still pending.

## Canonical content

The only places in the app come from:

`shared/src/commonMain/composeResources/files/quartermuse_master_v11.csv`

Each row contributes exactly:

- a stable id (a slug derived once from the venue name; this is the row's permanent identity)
- venue name
- latitude
- longitude
- the original semicolon-separated category tags

The id is what Saved/Been-There state and bundled photos are keyed on. Correcting a venue's name or coordinates later does not change its id, so a typo fix never resets a user's saved state or orphans that venue's photos -- only ever add new ids, never reuse or regenerate an existing one.

The app does not invent descriptions, reviews, ratings, neighborhoods, creators, or additional venues.

## What the app does

- Save a hotel (picked from a list, or any point via device location) as the **Home
  Lantern** -- a fixed anchor for the stay, no account required
- Fetch the device's location on open and sort the catalog by proximity immediately,
  before any hotel is even confirmed
- Offer a one-tap "Staying at [hotel]?" confirmation when that location lands within about
  120m of a known hotel
- Show approximate walk time (from the Home Lantern once set, otherwise from the device's
  current location) on place cards and the detail screen
- One-tap **Take me back**: walking directions from the Home Lantern icon, from any screen
- Search the catalog by venue name, original tag, or a broader set of search terms bundled
  from Google's place data, and filter by tag, Saved, or Been There
- Show a venue's phone, website, address, today's hours, and an open/closed-now status when
  the build-time Places pipeline found a match
- Open the exact catalog coordinates in the user's maps app
- Mark catalog places locally as Saved or Been There
- Show real Google Maps place photos for catalog venues, bundled at build time
- Show up to five venue-associated photos on the detail screen
- Display Google Maps and third-party/author attribution alongside Google-sourced photos

Home Lantern and Saved/Been There state are device-local SharedPreferences data. None of it
creates or modifies catalog content, and none of it requires an account.

## Home Lantern

On open, the app immediately requests a location fix (`ProactiveLocationEffect`) and starts
browsing right away -- sorted by proximity to that fix -- without waiting on a setup dialog.
If the fix lands within about 120m of a hotel in the bundled catalog (`HotelCatalog`,
`assets/hotels.csv`), a lightweight "Staying at [hotel]?" confirmation appears; otherwise
nothing is forced. The Home Lantern is reachable at any time from the persistent top-right
FAB, which opens a sheet offering "Set my hotel" / "Change hotel": a scrollable list of
known hotels to tap (no typing), "Use my location" (stand at the hotel, tap once, done), or
"I'm not staying at a hotel." The bundled hotel list is a small starter set the client
should expand, the same way the venue catalog grew -- see `docs/roadmap.md`.

Whichever way it's set, the answer is remembered locally (`LamplightViewModel` +
`HotelAnchor` in `Models.kt`) with no account and no server round-trip. Location access
(`ACCESS_FINE_LOCATION`) is a single on-demand fix each time (`LocationFix.kt`) -- never
background or continuous tracking.

The Home Lantern FAB (`ui/Lantern.kt`) shows the Four Panes mark. Tapping it opens a sheet
with the saved hotel name, a "Take me back" button that hands off to Google Maps walking
directions (`google.navigation:` intent, falling back to a maps.google.com URL if Maps
isn't installed), and "Change hotel." Place cards and the detail screen show an approximate
walk time from the anchor, or from the raw current-location fix before one's set
(`WalkTime.kt`, a straight-line haversine estimate at an average walking pace) --
intentionally not a routed ETA from a live directions API, matching the product direction's
"approximate," not promised, framing.

## Google Places photos and business details

The app itself never calls the Places API. `scripts/fetch_place_photos.py` calls it once per release build, and the app just bundles and reads the result -- the same treatment the venue CSV already gets.

The pipeline:

1. CI runs `scripts/fetch_place_photos.py` before Gradle builds, using a Places API (New) key.
2. For each venue, the script resolves a Google Place ID (Text Search, IDs-only) and reuses it from `scripts/places_cache.json` on later runs, since Google permits caching Place IDs indefinitely.
3. One Place Details call per venue fetches photo metadata, phone number, website, address, opening hours, Google's place `types`, and (transiently) review text. Up to five photos are downloaded as JPEG files into `shared/src/androidMain/assets/photos/<place-id>/` (Android-only -- see below) with a manifest at `shared/src/commonMain/composeResources/files/photos_manifest.json`. Phone/website/address/hours/tags go to `place_details_manifest.json` in that same commonMain resources directory -- place types feed tags directly, and review text is tested against a fixed keyword vocabulary for additional search terms (`REVIEW_KEYWORD_VOCABULARY`) and then discarded; **the review text itself is never written to that manifest or bundled into the app in any form.**
4. The two manifests and the venue CSV are Compose Multiplatform resources, read via `Res.readBytes(...)` on every target (Android and web). Photo binaries are the one exception, deliberately kept Android-only asset files rather than Compose resources (reading them that way is `suspend`-only, a poor fit for the up to ~2,095 JPEGs this pipeline can produce): `BundledPhotos`/`BundledPlaceDetails` build a photo's URI from a small `photoBaseUri()` seam instead, `file:///android_asset/photos/...` on Android. At runtime there are no network calls, no Places SDK dependency, and no API key shipped in the app. `OpeningHours.kt` derives an open/closed-now status from the structured hours.

Both manifests are regenerated by CI on every release build (`.gitignore`d, not committed); `scripts/places_cache.json` (place IDs only, no images or text) is committed and reused.

To fetch locally: `GOOGLE_PLACES_API_KEY=<server key> python3 scripts/fetch_place_photos.py`. The app builds fine without ever running the script; venues just show no photo and no business details.

### API key

The script needs a Google Maps Platform key with **Places API (New)** enabled and **no Android application restriction** -- it's called from a plain script, not the Android app, so an app-restricted key won't authenticate. Restrict it to the Places API only (and, if practical, to your CI runner's egress) instead. Add it to the repository as:

`GOOGLE_PLACES_SERVER_API_KEY`

This key is never bundled into the app; it's only used by CI (and by a maintainer running the script locally).

### Controlling cost

Google Maps Platform requires billing to be enabled for the Places API. If the goal is a $0 operating bill, use Cloud Console quota limits rather than relying on budget alerts. Quotas stop requests when their configured limit is reached; budget alerts do not stop usage.

The relevant current free usage caps include:

- Text Search Essentials (IDs Only): unlimited
- Place Details Pro: 5,000/month
- Place Details Photos / photo usage: 1,000/month

Because fetching now only happens once per release build rather than once per user per view, total usage is `(release builds per month) x 419 venues x (up to 5 photos)` -- bounded by how often you cut a release, not by how many people use the app. Set quotas below the applicable free caps with margin for accounting differences.

The venue count grew from 143 to 419 when New Orleans landmarks/parks/tourist-activities (64) and restaurants/bars (212) research was folded into `quartermuse_master_v11.csv`. At 419 venues, one release build alone can need up to 2,095 photo fetches (419 x 5) against a 1,000/month free cap -- a single release can now exceed the free tier on its own, not just across several. Either lower `MAX_PHOTOS_PER_VENUE` in the script, enable billing with a hard quota ceiling, or accept the overage cost per release; watch actual usage after the next release before deciding. `hotels.csv` is a separate, smaller catalog (name/coordinates only) never touched by this script, so it doesn't add to this cost.

## Google Maps Platform compliance

Google-sourced images are visually identified as **Google Maps** content. Photo metadata attribution and author attribution are displayed with the image when Google returns them.

When publishing the application, provide public Terms of Use and Privacy Policy URLs that satisfy Google Maps Platform requirements.

## Update notifications

The app checks for updates using whichever channel it was actually installed from, and never the other one:

- **Installed from Google Play**: `packageManager.getInstallSourceInfo(...)` (or `getInstallerPackageName` pre-API 30) reports the installer as `com.android.vending`. The app uses Play Core's in-app update API (`AppUpdateManager`) to check, download in the background (`FLEXIBLE` flow), and prompt to restart -- entirely through Google's own mechanism. GitHub is never contacted.
- **Installed any other way** (the signed APK from a GitHub Release, ADB, a file manager, etc.): `UpdateChecker.kt` calls the GitHub Releases API, compares the latest release's embedded `version_code` against the running app's own `versionCode`, and if newer, shows a banner offering to download it. Play Core is never touched.

  The app manages this download itself (`UpdateInstaller.kt`, via `DownloadManager`, to a known app-private file) rather than handing the URL to the browser -- knowing exactly where the file landed is what makes the rest of the flow possible: once it's done, the banner offers "Install," which hands the file to the system package installer as a `content://` URI (`FileProvider`, since Android 7 blocks a raw `file://` URI in an intent to another app). After the guest actually completes that install, the OS fires `MY_PACKAGE_REPLACED` to the app being replaced; a manifest-registered receiver (`UpdateInstalledReceiver`) uses that moment to delete the now-unneeded downloaded APK, so it doesn't linger in the app's storage indefinitely. If the guest downloads but never finishes the install, the file is simply left alone -- a later download attempt overwrites it regardless, since the destination filename never changes.

`detectInstallSource` (`UpdateChecker.kt`) makes this an either/or, not a "check both": a Play install only ever hears about Play updates, a sideloaded install only ever hears about GitHub releases.

This is why the release workflow's notes aren't just human-readable text -- they carry `version_code: N` / `version_name: X` lines the app parses, since a GitHub release's tag only encodes `major.minor`, not the full build-numbered version.

The published APK's filename (`<app-name>-release.apk`) is deliberately version-free and identical build to build, so `gh release upload --clobber` in the workflow actually replaces the previous asset instead of accumulating a new differently-named one next to it on every push. An earlier version embedded the version number in the filename, which silently defeated `--clobber` (each build's filename never matched the last one) and left every past build's APK piled up on the one floating release; `UpdateChecker.kt` picking the array's first `.apk` match then meant guests were served the *oldest* surviving build. Fixed on both sides: the filename is now stable, and asset selection picks by newest `created_at` rather than array order, as defense in depth.

## Release signing

Push/workflow-dispatch release builds reconstruct a temporary PKCS#12 keystore from the repository's split signing material and verify the resulting signing certificate before Gradle signs anything.

Consumed repository secrets:

- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`
- `KEYSTORE_OWNER`
- `KEYSTORE_SHA1`
- `KEYSTORE_SHA256`
- `KEYSTORE_PRIVATE`
- `KEYSTORE_PUBLIC`
- `KEYSTORE_CHAIN`
- `KEYSTORE_RSA`

Signing material is written only under the Actions runner's temporary directory and removed after the build.

## Package

`com.hereliesaz.lamplight`

## Build

```bash
./gradlew allTests assembleDebug
```

Requires JDK 21 and Android SDK 37. `allTests` is the aggregate test task across every
Kotlin target -- the plain `test` task does not run `:shared`'s tests (see `kmp-web-migration-plan.md`).

Two modules: `:shared` (a Kotlin Multiplatform library -- models, parsing, the ViewModel, and
the UI) and `:androidApp` (a thin `com.android.application` shell -- manifest, launcher icon,
release signing, the Android-only self-update flow). See
[`docs/kmp-web-migration-plan.md`](docs/kmp-web-migration-plan.md) for why, and what's next
(a `:webApp` module targeting Kotlin/Wasm, plus a plain Kotlin/JS fallback for browsers below
its WasmGC floor).

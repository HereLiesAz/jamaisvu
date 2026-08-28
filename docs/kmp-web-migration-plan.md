# Kotlin Multiplatform + Web migration plan

Approved by the user on 2026-08-28. Tracked here (not just in roadmap.md) because it's
detailed enough that a future session picking up the next PR in the sequence needs the full
thing, not a summary. See `roadmap.md`'s "Not in the original build-priority list" section
for the running progress checklist against this plan.

## Context

Lamplight was a single-module Android app (`:app`). The user wants it on the web as a
genuinely shareable app (reached via a link/QR code -- a hotel concierge desk, a
confirmation email -- not something that needs to rank in search results), while continuing
to ship on Android exactly as it does today. Alongside this, the toolchain moves to JDK 21
and every dependency to its current latest stable version.

Compose Multiplatform is the right fit for "shareable app, not a search result": one shared
UI codebase with Android, full feature reuse. Its web target (Kotlin/Wasm) is JetBrains-
labeled **Beta**, not stable -- real risk, accepted deliberately here rather than glossed
over, because the alternative (a separate server-rendered site) isn't needed for what this
web version is for.

Verified 2026-08-28 via live research: Kotlin 2.4.10 stable, Compose Multiplatform 1.12.0
stable (web/wasmJs target Beta; Kotlin/JS is now just a compatibility fallback, not a
maintained primary path), AGP 9.3.0 already current -- and AGP 9.0+ **requires** app entry
points to live in their own Gradle module, which settles the module-layout question rather
than leaving it a style choice. JDK 21 is what AGP needs at minimum (17) plus what Android
Studio itself runs on -- the sensible, IDE-aligned floor; nothing currently requires going
higher.

## Target module layout

```
settings.gradle.kts   include(":shared", ":androidApp", ":webApp")
shared/                 KMP library: androidTarget() + wasmJs { browser() }
androidApp/             com.android.application -- thin shell, Android-only surface
webApp/                 KMP, wasmJs-only -- thin shell, web entry point
```

- **`:shared`** -- `commonMain` holds the actual app: models, parsing, `LamplightViewModel`
  (its shared slice -- see below), and the bulk of the UI (`LamplightApp.kt`, `Lantern.kt`,
  `Mood.kt`, `Theme.kt`). `androidMain`/`wasmJsMain` hold only the platform sides of the seams
  below. Android namespace **must** differ from `androidApp`'s (e.g.
  `com.hereliesaz.lamplight.shared`) or AGP collides on the generated R class.
- **`:androidApp`** -- `MainActivity`, `AndroidManifest.xml`, `res/`, release signing (moved
  verbatim from the original `app/build.gradle.kts`), and the entire self-update surface
  (`UpdateChecker.kt`, `UpdateInstaller.kt`, `UpdateInstalledReceiver`, Play Core). Same
  `applicationId` as always -- this is the existing Play/sideload identity, not something to
  fork.
- **`:webApp`** -- the wasmJs entry point (`ComposeViewport { ... }` -- verify the exact
  current bootstrap API name against whatever Compose Multiplatform version is current when
  this is built; this has moved before), plus web-side construction of the browser-backed
  seam implementations below.
- `gradle/libs.versions.toml` (added in PR0) centralizes plugin/dependency versions across
  what's now several build files sharing them.

## The platform seams

Not a blanket "everything Android-only becomes expect/actual" -- the mechanism matches where
each capability is actually consumed:

- **Persistence** (replacing `SharedPreferences`) -- a plain `SettingsStore` interface + DI,
  not expect/actual, since `LamplightViewModel` is the only consumer and already takes a
  constructor argument this replaces. `AndroidSettingsStore` wraps the exact
  `SharedPreferences` calls it always used; `BrowserSettingsStore` backs onto `localStorage`.
  Six methods total (saved/visited/seen sets, hotel-anchor fields, group-size/vibe, two
  skip-flags) -- small enough hand-rolled that a third-party lib (e.g.
  multiplatform-settings) isn't worth an extra Beta-adjacent unknown on top of the web target
  itself.
- **Geolocation** (replacing `LocationManager`) -- same DI pattern, `LocationProvider`
  interface with `GeoPosition(latitude, longitude)` replacing `android.location.Location`
  everywhere it's read -- not just in `LocationFix.kt`, but also where
  `LamplightViewModel.currentLocation` is read directly in `ExploreScreen`/`PlaceDetail`.
  `AndroidLocationProvider` is close to a straight move of `LocationFix.kt` as it stands.
  `BrowserLocationProvider` (wrapping `navigator.geolocation`) has **no existing code to lean
  on** -- the least-known unknown in this plan; spike it early.
- **URL/tel/maps opening** -- this one genuinely fits `expect`/`actual`, because it's called
  from many scattered leaf composables (`DetailRow`, `openMaps`, dialer/website rows) where
  DI would mean threading an interface through every parameter list:
  `@Composable expect fun rememberUrlOpener(): (String) -> Unit`. Extract the maps-URL-
  building logic (already exists, in `openWalkingDirections`'s own fallback branch) into a
  plain shared function first; only the "launch it" part stays behind the seam. This also
  removes the need for `LocalContext` (Android-only; CMP has no common `Context`) from every
  commonMain composable that currently only needs it to open a link.
- **Walking directions app-then-web fallback** (`openWalkingDirections`) -- *not* Android-
  only forever the way a first pass might file it. A real web equivalent exists (the web URL
  branch it already falls back to), so this is an `expect`/`actual` case like URL-opening
  above, not a UI-slot-absent-on-web case like Play Core below.
- **Photo attribution** -- drop `AndroidView`/`Html.fromHtml`/`TextView`/
  `LinkMovementMethod` entirely. Compose's own `LinkAnnotation.Url` + `withLink` in
  `AnnotatedString.Builder` (core `compose.ui.text`, not platform interop) renders a
  clickable inline link on every CMP target natively. This is a straight quality improvement
  on Android too, not just a web-compat shim.
- **CSV/JSON bundled data** -- move to Compose Multiplatform resources
  (`shared/src/commonMain/composeResources/files/...`), read via `Res.readBytes(...)`.
  Update the four path constants in `scripts/fetch_place_photos.py` (CI writes these files).
  This makes the four `load(context)` functions `suspend`, which means
  `LamplightViewModel`'s eager `val places: List<Place> = QuarterMuseSeed.load(application)`
  no longer compiles as-is -- budget a deliberate "loading catalog" state UI decision for
  this, on Android too, not just web.
- **Photo binaries** -- do **not** route these through Compose resources (`Res.readBytes` is
  `suspend` to accommodate web's `fetch()`, a poor fit for up to ~2,095 JPEGs per the
  README's own cost math, and would bloat the wasmJs bundle for no benefit). Leave Android's
  existing `file:///android_asset/...` path alone; for web, have CI copy the same generated
  photo tree next to the deployed wasmJs bundle and construct plain relative HTTP URLs. Small
  `expect fun photoBaseUri(): String`-style seam, not a resource migration.
- **Bundled fonts** (Archivo, Martian Mono) -- move the TTFs to
  `shared/src/commonMain/composeResources/font/`; loading is uniform across targets, already
  OFL-licensed (covers web same as APK bundling). The mechanical change: CMP's resource-aware
  `Font(...)` is `@Composable`, so `Theme.kt`'s top-level `FontFamily` vals and the
  `Typography` construction move inside `LamplightTheme`'s body.

### Stays Android-only, no web counterpart, ever

Play Core in-app updates, and the entire GitHub-releases sideload flow (`UpdateChecker.kt`,
`UpdateInstaller.kt`, the receiver, the `FileProvider`/`REQUEST_INSTALL_PACKAGES` manifest
machinery, `detectInstallSource`/`InstallSource` -- a website has no "installer package," the
concept doesn't exist, not just lacks an implementation). These get a UI-slot design in
`LamplightHome` (an optional `platformBanner: @Composable () -> Unit = {}` parameter that
`androidApp` fills and `webApp` leaves empty), not expect/actual.

`LamplightViewModel` mixes shared state (saved/visited/seen, hotel anchor, mood) with
Android-only state (`installSource`, `githubUpdate`, `githubUpdateDownload`) in one class.
Extract the update-related fields into a separate Android-only controller **before** moving
the rest of the class to `commonMain` -- doing it in the other order makes for a messier
diff.

## PR sequence (never break Android to make progress on web)

- **PR0** *(merged)*: JDK 17->21, Kotlin/AGP/Compose-BOM bump to current stable, add
  `gradle/libs.versions.toml` -- entirely on the then-single `:app` module, so a break here
  is unambiguously about versions, not the module split.
- **PR1** *(this one)*: introduced `:shared` (`androidTarget()` only, no wasmJs yet) and
  `:androidApp`. Moved every existing file into `shared/src/androidMain` **unchanged**,
  package-for-package -- zero commonMain, zero expect/actual yet. Updated CI's APK output
  path and its test task (`test` -> `allTests`, see roadmap.md for why). Verified
  `./gradlew allTests assembleDebug`/`assembleRelease` with a real, clean build.
- **PR2**: add `wasmJs` to `:shared`, create `:webApp` with a real hello-world screen, and
  **stand up actual GitHub Pages deployment here** (not deferred to the last PR -- the
  least-validated part of the pipeline shouldn't be the last thing touched). Also spike
  `SharedTransitionLayout`/`SharedTransitionScope` here specifically (the mosaic-to-detail
  hero animation is this app's signature interaction, and shared-element transitions are
  exactly the kind of feature that lags basic layout support on a newer target) -- confirm it
  works on wasmJs before building the rest of the plan on the assumption that it does.
- **PR3**: move the already-framework-free files (`Models.kt`, `Csv.kt`, `OpeningHours.kt`,
  `WalkTime.kt` -- verified zero Android imports) into `commonMain` unchanged. Move all four
  test files into `commonTest`, converting JUnit4 to `kotlin.test`. Pure relocate, no logic
  changes -- all four tests already call pure functions directly, never `.load(context)`.
- **PR4**: persistence seam -- `SettingsStore`, refactor `LamplightViewModel`'s prefs access
  behind it, then move both to `commonMain` with `BrowserSettingsStore` wired into `:webApp`.
- **PR5**: geolocation seam -- same shape, plus the `Location` -> `GeoPosition` ripple
  through the ViewModel and the two UI read-sites. Budget real time on
  `BrowserLocationProvider`.
- **PR6**: URL-opening seam -- replace every raw `Intent`/`Uri`/`startActivity` call in
  `LamplightApp.kt`/`Lantern.kt` with `rememberUrlOpener()`.
- **PR7**: photo-attribution rewrite (self-contained).
- **PR8**: CSV/JSON/font -> Compose resources; photo binaries -> `photoBaseUri()` seam;
  convert the four loaders to `suspend`; deliberate loading-state UI; update
  `fetch_place_photos.py` paths.
- **PR9**: move the remaining bulk of the UI into `commonMain`. Extract the update-related
  ViewModel fields into an Android-only controller; wire the `platformBanner` slot. Bump
  Coil to 3.x + `coil-network-ktor3` (needed once `AsyncImage` lives in commonMain).
- **PR10**: CI polish (web-build failure reporting, align `codeql.yml`'s JDK pin to 21),
  docs, final check that Android's release-signing/versioning/update-checker behavior is
  unchanged.

## CI changes (`.github/workflows/build-and-release.yml`)

Bump `actions/setup-java@v4` to `java-version: '21'` (done, PR0). Add a **separate** job for
the web build (parallel to Android, and so a web-only failure doesn't get silently absorbed
by the existing Android-specific "Report Failure to Jules" step) -- build+test it on every
push/PR, gate the actual Pages publish step on push-to-main/workflow_dispatch, mirroring the
existing release-gating pattern. Use `actions/upload-pages-artifact` + `actions/deploy-pages`
(verify current major version tags at implementation time) rather than a `gh-pages` branch,
to avoid committing built JS/wasm binaries into git history on every push. One manual step
outside YAML: switch the repo's GitHub Pages source to "GitHub Actions" in settings, or the
workflow runs green and deploys nowhere.

## Risks to carry into execution

- **Browser floor**: wasmJs needs WasmGC -- Chrome 119+/Firefox 120+/Safari 18.2+. Hotel
  guests on unmanaged, older-iOS phones are a real fraction of this audience; there's no
  good fallback since `js` is now just a compatibility shim, not a maintained target. Worth
  a conscious call (accept the gap, or add `js` as a third build output later) rather than a
  silent one.
- **Bundle size**: a CMP-for-web "hello world" ships a non-trivial wasm+JS runtime payload
  before any app code, independent of the photo-bundling question above -- a real
  first-impression cost on hotel wifi.
- **`material-icons-extended`** is a large artifact -- verify wasmJs availability and
  bundle-size impact specifically, don't assume "it's Compose so it's fine."
- **Compliance flag, not an assertion**: the README's Google Maps Platform attribution
  requirement covers on-device display, which the photo-attribution rewrite preserves as-is.
  Publishing the underlying Places photo content to a public, statically-hosted Pages site
  is a distinct question from displaying it inside an installed app -- worth a specific
  compliance check before shipping, not an assumption that existing attribution handling
  covers it.
- **Effort, honestly**: PR0-PR2 are roughly a day or two each. PR4 onward is the real cost --
  three new platform seams (one with no existing code to adapt), a ~750-line file carrying
  the riskiest UI surface, plus debugging unfamiliar Beta-target failures as the dominant
  time sink, not writing the ported code itself. Realistically multiple weeks of elapsed
  work, not days.

## Critical files

- `settings.gradle.kts`, and whichever module's `build.gradle.kts` the current PR touches
- `shared/src/androidMain/kotlin/com/hereliesaz/lamplight/LamplightViewModel.kt` (the mixed
  shared/Android-only state to split, in PR9)
- `shared/src/androidMain/kotlin/com/hereliesaz/lamplight/ui/LamplightApp.kt` (~750 lines,
  the biggest and riskiest single file: photo attribution, shared-element transitions, every
  Intent handoff)
- `.github/workflows/build-and-release.yml`
- `scripts/fetch_place_photos.py` (CSV/JSON path constants CI writes)

## Verification, per PR

- `./gradlew allTests assembleDebug`/`assembleRelease` stay green on Android at every single
  PR in the sequence -- this is the non-negotiable invariant, not just a final check. (Note:
  the plain `test` task does NOT run `:shared`'s tests as of PR1 -- always use `allTests`.)
- From PR2 onward, the web job's own build (and, once deployed, the live Pages URL) is
  checked the same way -- don't let web-side breakage hide behind "Android's fine" for
  multiple PRs.
- PR3's relocated tests must still pass unchanged in `commonTest`.
- PR9's final state: manually confirm on a real Android build that release-signing,
  versioning, and the sideload update-check/download/install/cleanup flow (built just before
  this migration started) all still behave identically to before the migration.

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
- **PR0** *(merged, #27)*: also picked up a manual AGP 9.3.0->9.3.2 bump made directly on
  GitHub, merged into the branch during PR1.
- **PR1** *(merged, #28)*: introduced `:shared` (`androidTarget()` only, no wasmJs yet) and
  `:androidApp`. Moved every existing file into `shared/src/androidMain` **unchanged**,
  package-for-package -- zero commonMain, zero expect/actual yet. Updated CI's APK output
  path and its test task (`test` -> `allTests`, see roadmap.md for why). Verified
  `./gradlew allTests assembleDebug`/`assembleRelease` with a real, clean build, then again
  after merging in the AGP bump.
- **PR3** *(merged ahead of PR2 as its own PR, #29 -- it didn't depend on the wasmJs work, so
  it happened while that was still being researched)*: moved `Models.kt`, `Csv.kt`, and
  `WalkTime.kt` into `commonMain`, and `WalkTimeTest.kt` into `commonTest` (JUnit4 ->
  `kotlin.test`). **Scope corrected from the original plan** -- "verified zero Android
  imports" turned out to be necessary but not sufficient for multiplatform-safety:
  - `WalkTime.kt` called `Math.toRadians` -- `java.lang.Math`, resolved with no visible
    import (Kotlin/JVM auto-imports it), so a plain `grep "^import android"` never caught
    it. Not available outside JVM/Android targets; fixed in place with a two-line
    `degreesToRadians` helper using only `kotlin.math.PI`.
  - **`OpeningHours.kt` did NOT move** -- it uses `java.time.DayOfWeek`/`java.time.LocalTime`
    throughout (explicit imports, so easier to catch, but still invisible to an
    android-import-only grep). This is a real API migration (to `kotlinx-datetime` or
    equivalent), not a pure relocate. Stayed in `androidMain`; its test
    (`OpeningHoursTest.kt`) stayed in `androidHostTest` alongside it. Now that PR2 has
    actually given `:shared` a wasmJs target to verify against, this port is unblocked --
    still not done as of PR2, tracked as its own follow-up rather than folded silently into
    either PR.
  - `HotelCatalogTest.kt`/`QuarterMuseSeedTest.kt` also did **not** move: they test
    `HotelCatalog`/`QuarterMuseSeed`, which still have their `Context`-dependent `.load()`
    functions living in `androidMain` (planned to split in PR8, alongside the other
    asset-loaders) -- a `commonTest` file can't reference an `androidMain`-only symbol, since
    `commonTest` has to compile for every target, including ones where that symbol won't
    exist. Moving the tests without their subjects would have failed to compile.
  - `kotlin.test` has no delta-tolerance `assertEquals(expected, actual, delta)` overload for
    `Double` the way JUnit does -- `WalkTimeTest.kt`'s two floating-point comparisons needed
    a small local `assertApproxEquals` helper instead.
  - Needed one addition to `shared/build.gradle.kts` the original plan didn't call out:
    `commonTest.dependencies { implementation(kotlin("test")) }`.
- **PR2** *(this one)*: added `wasmJs` to `:shared`, created `:webApp` with a
  `SharedTransitionLayout`/`sharedBounds` spike screen (`WasmSpikeScreen.kt`, in
  `shared/src/commonMain` since both `:androidApp` and `:webApp` can reach it), and wired up
  GitHub Pages deployment in CI. `:shared:compileKotlinWasmJs` and `:webApp:compileKotlinWasmJs`
  both succeed, confirming the shared-element-transition mechanism this app's mosaic-to-detail
  hero animation depends on is genuinely available on wasmJs, not just on Android -- the
  single biggest technical risk in this plan, resolved at the API/compile level. Gotchas hit
  along the way, worth recording since they're easy to re-trip on:
  - The `org.jetbrains.compose` Gradle plugin (not just `kotlin.plugin.compose`, which only
    wires the compiler) is required for Compose Multiplatform; it's independent of and applied
    alongside the Kotlin Multiplatform and Kotlin Compose Compiler plugins, not a replacement
    for either.
  - `material3` for Compose Multiplatform tracks its own version line, decoupled from the main
    `composeMultiplatform` version -- verified directly against Maven Central (not just
    JetBrains' changelog) that `1.12.0-alpha03` is genuinely the latest available at
    `composeMultiplatform = 1.12.0`; no stable release exists yet at that line.
    `material-icons-extended` is frozen at `1.7.3`, its last-ever published version (Dec 2024).
  - The `compose.foundation`-style Gradle accessor is deprecated as of `composeMultiplatform`
    1.12.0 ("Specify dependency directly") -- used explicit `org.jetbrains.compose.*`
    coordinates via the version catalog instead, matching JetBrains' own current example
    projects.
  - `wasmJs { browser() }` alone isn't enough once a target has Compose UI code, even in a
    module that's conceptually a "library" (`:shared`) rather than an app:
    `checkComposeUiTestConfigurationForWasmJs` fails outright without also declaring
    `binaries.executable()` (CMP-4906) -- the Compose UI test runner needs the Skiko runtime,
    which only loads from a bundled executable.
  - `settings.gradle.kts`'s `repositoriesMode` had to move from `FAIL_ON_PROJECT_REPOS` to
    `PREFER_PROJECT`: the Kotlin/Wasm plugin registers its own repository for the Node.js/Yarn
    toolchain it downloads, and `FAIL_ON_PROJECT_REPOS` rejects that outright.
    `PREFER_SETTINGS` looks like the safer middle ground but isn't -- it silently never
    searches the project-added repo at all, so resolution just fails as if the repo didn't
    exist.
  - `webApp/src/wasmJsMain/resources/index.html` was deliberately **not** hand-written. The
    exact compiled JS bundle filename it would need to reference isn't independently
    verifiable in this sandbox (see below), and the Kotlin/Wasm toolchain already generates a
    correct one automatically as part of `wasmJsBrowserDistribution`, referencing whatever the
    real output filename is. A hand-guessed filename that's wrong fails silently in production
    (a blank page, a 404 in the browser console) in exactly the way that looks deployed but
    isn't -- worse than not shipping one at all.
  - This sandbox's outbound network policy blocks `codeload.github.com`, which breaks `yarn
    install`'s fetch of a GitHub-tarball dependency (`Kotlin/karma`) bundled into Kotlin/Wasm's
    Node.js toolchain setup (`kotlinWasmToolingSetup`). That task runs as soon as anything
    needs to *execute* wasmJs code (tests, or `wasmJsBrowserDistribution`'s dev tooling), not
    just compile it -- so `allTests` and the actual distribution build can't be verified
    end-to-end in this sandbox. What *is* verified locally: both wasmJs targets compile clean
    (`:shared:compileKotlinWasmJs`, `:webApp:compileKotlinWasmJs`), and Android is fully
    unaffected (`:shared:testAndroidHostTest` -- all 28 tests -- plus
    `:androidApp:assembleDebug`/`assembleRelease` all green). Real GitHub Actions CI, with
    unrestricted network, is the actual verification point for `wasmJsBrowserDistribution` and
    the live Pages deploy -- watch its `build-web`/`deploy-web` jobs specifically once this PR
    is up, not just `build-and-release`. One consequence worth flagging explicitly: that
    blocked `yarn install` also means this sandbox produced only an empty
    `kotlin-js-store/wasm/yarn.lock`, deliberately left uncommitted rather than checked in
    broken -- the Kotlin/JS-ecosystem convention is to commit this lockfile for reproducible
    builds, so once a real CI run generates a genuine one, pull it back and commit it for real
    in a follow-up, rather than treating its current absence as the intended end state.
- **PR4** *(this one)*: added the `SettingsStore` interface (`commonMain`) -- a small generic
  string/string-set/boolean key-value seam, deliberately not domain-shaped (no
  `saveHotelAnchor(...)`), so `LamplightViewModel`'s existing read/write calls could swap
  their backing store with a near-mechanical diff rather than a redesign. `AndroidSettingsStore`
  wraps the exact `SharedPreferences` calls it always used; `BrowserSettingsStore`
  (`shared/src/wasmJsMain`) wraps `localStorage` via `org.jetbrains.kotlinx:kotlinx-browser`
  (the JetBrains-maintained artifact for this -- `kotlinx.browser`/`org.w3c.dom` bindings were
  removed from being bundled automatically with wasmJs's stdlib and need this explicit
  dependency now), encoding string sets as newline-joined strings (safe here since place/hotel
  ids are plain CSV slugs, never containing a newline).
  **Scope narrower than the original plan's "then move both to commonMain" implied**:
  `LamplightViewModel` itself does **not** move to `commonMain` in this PR. Past the prefs
  calls this PR replaces, it still directly depends on `Application`/`Context`
  (`AndroidViewModel`'s base class, plus `QuarterMuseSeed.load(application)` and three other
  Context-taking loaders), `android.location.Location` (PR5's job), and the entire
  Android-only GitHub-update surface (`DownloadManager`, `BroadcastReceiver`,
  `detectInstallSource` -- explicitly staying Android-only per this doc's own "Stays
  Android-only" section, which already calls for extracting that surface into its own
  controller **before** the rest of the class moves). Moving the class now, before those
  other seams exist, would mean either a half-multiplatform class that still doesn't compile
  for wasmJs, or doing PR5/PR6/PR8/PR9's extraction work early and out of order. So
  `LamplightViewModel` stays in `androidMain` for now, constructing its own
  `AndroidSettingsStore(application)` internally -- the constructor signature is unchanged
  (`(application: Application)`), so `MainActivity`'s `viewModel()` call keeps working via
  the default reflection-based `AndroidViewModelFactory`, with no `ViewModelProvider.Factory`
  needed yet. That becomes necessary in PR9, when `:webApp` needs to construct its own
  instance with `BrowserSettingsStore` and Android needs an explicit factory too -- the
  natural point for real constructor injection, once the class actually has multiplatform
  callers on both sides.
- **PR5** *(this one)*: added `GeoPosition(latitude, longitude)` and the `LocationProvider`
  interface (`commonMain`), replacing `android.location.Location` everywhere it's read.
  `AndroidLocationProvider` wraps the existing `requestOneTimeLocation` (`LocationFix.kt`,
  untouched) and maps its result to `GeoPosition`; `Lantern.kt`'s two composables
  (`HotelAnchorPrompt`, `ProactiveLocationEffect`) now go through it instead of calling
  `requestOneTimeLocation` directly, so the seam is actually exercised, not just defined
  unused. `LamplightViewModel.currentLocation`/`setCurrentLocation` and every UI read site
  (`ExploreScreen`'s proximity sort, `MosaicPlaceCard`, `PlaceDetail`) needed **zero** other
  changes -- they only ever read `.latitude`/`.longitude`, which `GeoPosition` provides
  identically, so the type swap is fully transparent past the declaration sites themselves.
  `BrowserLocationProvider` was the real unknown, per this doc's own risk note -- resolved:
  - `navigator.geolocation.getCurrentPosition` is callback-based (success/error), not
    Promise-based, and its result is a structured JS object (`position.coords.latitude`, not
    a value that crosses the Kotlin/Wasm boundary cleanly on its own). Modeling that shape
    with `external interface`s and marshaled callback types is the more "proper" approach but
    has real Wasm/JS-interop syntax risk; instead, a single `@JsFun`-annotated external
    function wraps the whole callback-to-`Promise` conversion **in the inline JS body itself**
    and resolves a plain `"lat,lng"` string (empty string = denied/unsupported/failed),
    avoiding structured-object marshaling entirely. Parsed back into a `GeoPosition` on the
    Kotlin side with plain `String.split(",")` + `toDoubleOrNull()`.
  - `@JsFun`'s external function must declare `Promise<JsString>`, not `Promise<String>` --
    confirmed by the compiler, not guessed: `kotlinx-coroutines-core`'s wasmJs `.await()` is
    `suspend fun <T : JsAny?> Promise<T>.await(): T`, and plain `String` isn't a `JsAny`.
    `JsString.toString()` converts back to a Kotlin `String` after awaiting.
  - `@JsFun` and `Promise`/`JsString` sit behind `@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)`
    as of this Kotlin version -- the compiler's own opt-in warning is what surfaced the exact
    annotation name/package, again not guessed in advance.
  - Needed `kotlinx-coroutines-core` added explicitly to `wasmJsMain` (for `.await()`) --
    confirmed its wasmJs artifact (`kotlinx-coroutines-core-wasm-js`) exists and current
    latest is `1.11.0` via direct Maven Central check.
  - **Not verified in this sandbox**: actual runtime behavior in a real browser (a real
    permission prompt, a real GPS/network-location fix resolving through the callback chain).
    Only compilation is confirmed locally, for the same `codeload.github.com`-blocks-Node.js-
    toolchain reason as PR2. This is genuinely the least-verified piece of the migration so
    far and deserves a real manual check against the live Pages deploy once one exists (PR9,
    when a web onboarding flow that actually calls this exists) -- don't treat "compiles
    clean" as "confirmed working" for this one specifically.
  - Scope note matching PR4's: `LamplightViewModel` stays in `androidMain`; only its
    `currentLocation`-related internals changed type. `Lantern.kt` (the two composables using
    the seam) also stays in `androidMain` for now -- both move to `commonMain` together in
    PR9, once the class is otherwise free of Android-only dependencies.
- **PR6** *(this one)*: added `@Composable expect fun rememberUrlOpener(): (String) -> Unit`
  (`commonMain`) -- Android's `actual` does `Intent(ACTION_VIEW, Uri.parse(url))`; wasmJs's
  does `window.open(url, "_blank")`. Applied to the three single-URL call sites in
  `LamplightApp.kt`'s `PlaceDetail`: the phone (`tel:`) and website `DetailRow`s, and
  `openMaps` -- the last of which changes behavior slightly on Android, replacing the
  Android-only `geo:` URI scheme (no web equivalent at all) with Google's documented
  cross-platform Maps URL format (`mapsSearchUrl`, a plain shared function in `commonMain`),
  which still opens the Maps app on Android via its verified app link, or a browser tab on
  web. A `tel:` URI opened via `ACTION_VIEW` rather than the original `ACTION_DIAL` is not a
  behavior change either -- Android treats the two identically for a `tel:` URI (unlike
  `ACTION_CALL`, which actually auto-dials and needs a separate permission); using `ACTION_VIEW`
  everywhere keeps the opener's Android `actual` a single, uniform code path.
  **`openWalkingDirections` (`Lantern.kt`) deliberately NOT touched**, despite the original
  plan calling it "an expect/actual case like URL-opening above": on inspection, its
  Android-side behavior isn't a plain "open this URL" call the way the other three are -- it
  specifically targets the Maps app by package (`setPackage("com.google.android.apps.maps")`),
  catches `ActivityNotFoundException` if that app isn't installed, and only then falls back to
  the web URL. `rememberUrlOpener()`'s `(String) -> Unit` shape has no way to signal that
  first attempt's success/failure back to a caller, so representing this correctly would mean
  either changing the opener's return type (affecting the three call sites that don't need
  it) or a second, differently-shaped seam just for this one call site -- a real design
  decision better made together with PR9, when `Lantern.kt` actually moves to `commonMain` and
  there's a genuine second-platform caller to design against, not speculatively now with none.
  Left as-is: still Android-only, still calling raw `Intent`/`startActivity` directly.
- **PR7** *(this one)*: replaced `PhotoAttribution`'s `AndroidView`/`TextView`/
  `Html.fromHtml`/`LinkMovementMethod` with `buildAnnotatedString` + `withLink(LinkAnnotation.Url(...))`
  -- core `androidx.compose.ui.text` APIs, not platform interop, so this needed no new
  dependency and no `commonMain` seam at all; it's a self-contained rewrite entirely within
  `LamplightApp.kt` (still `androidMain`). The link's click handling deliberately still goes
  through PR6's `rememberUrlOpener()` (via `LinkAnnotation.Url`'s own
  `linkInteractionListener` parameter) rather than relying on `LinkAnnotation.Url`'s built-in
  default behavior (opening via Compose's own `LocalUriHandler`, which likely also works
  cross-platform on its own) -- keeping one single, already-verified "how this app opens a
  URL" code path rather than two different ones that happen to do the same thing. Author
  de-duplication preserved exactly (`distinctBy { it.name to it.uri }`, matching the old
  code's `parts.distinct()` on the fully-built HTML string). Compiled clean on the first try
  for both Android and wasmJs -- no iteration needed, unlike PR2/PR5's less-familiar APIs.
  All 28 `:shared` tests and both APK variants still green.
- **PR8** *(this one, fonts only -- split from the original plan)*: moved the Archivo and
  Martian Mono TTFs to `shared/src/commonMain/composeResources/font/`, moved `Theme.kt`
  itself to `commonMain` (colors/shapes/typography needed no changes to make this move --
  only the font construction did), and switched `Font(...)` to Compose Multiplatform's
  resource-aware overload (`org.jetbrains.compose.resources.Font`, not
  `androidx.compose.ui.text.font.Font` -- the two are separate overload sets from different
  packages, easy to get silently wrong since both are just named `Font`). Since that `Font`
  is itself `@Composable`, `ArchivoFamily`/`MartianMonoFamily`/the `Typography` construction
  all moved from top-level `val`s into `LamplightTheme`'s body, per the original plan's own
  prediction. `MartianMonoFamily` had 16 external call sites reading it as a plain top-level
  val (`LamplightApp.kt`/`Lantern.kt`) -- replaced with a `LocalMartianMonoFontFamily`
  `CompositionLocal` provided by `LamplightTheme`, the standard Compose pattern for exposing
  a composable-scoped value broadly without threading it through every parameter list.
  `ArchivoFamily` had zero external call sites (only ever read via
  `MaterialTheme.typography.bodyLarge`), so it needed no such exposure -- it's now fully
  private to `LamplightTheme`.
  **The real difficulty here wasn't the font/theme code -- it was getting the `Res` class to
  generate at all.** `generateComposeResClass`/`generateResourceAccessorsForCommonMain`
  (the tasks that produce it) reported `SKIPPED` with no useful reason
  (`info`-level logging only said `"task onlyIf 'Task satisfies onlyIf spec' is false"`).
  Root-caused by decompiling `compose-gradle-plugin-1.12.0.jar` directly (`javap` on the
  extracted class files, not guesswork): `ResourcesExtension.generateResClass` defaults to
  `Auto`, and whatever heuristic `Auto` uses to decide "does this module actually need a
  `Res` class" doesn't fire correctly for this module -- possibly a gap specific to the
  newer `com.android.kotlin.multiplatform.library` plugin, which this repo uses instead of
  the older `com.android.library` combo most Compose Resources documentation/examples
  assume. Fixed with an explicit, forced override:
  ```kotlin
  compose.resources {
      generateResClass = always
  }
  ```
  in `shared/build.gradle.kts`. A second, smaller gap once generation actually ran: the
  generated `Res.kt` itself failed to compile (`Unresolved reference 'FontResource'`,
  `'DrawableResource'`, `'ResourceItem'`, etc.) because the actual runtime library backing
  those types, `org.jetbrains.compose.components:components-resources`, wasn't a declared
  dependency yet -- applying the `org.jetbrains.compose` Gradle *plugin* (already done in
  PR2) handles resource file processing and code generation, but the generated code still
  needs this separate runtime artifact to actually compile and function. Added it via the
  version catalog, matching PR2's established pattern of explicit `org.jetbrains.compose.*`
  coordinates over the deprecated `compose.*` accessor. The confirmed generated package,
  for reference: `lamplight.shared.generated.resources` (root project name + module name,
  lowercased, since `packageOfResClass` defaults to blank) -- worth knowing before the next
  PR needs it again for CSV/JSON resources.
  Verified: compiles clean for both Android and wasmJs, all 28 `:shared` tests pass, both
  APK variants build.
- **PR8's remaining scope** *(this one -- CSV/JSON, photo binaries, loading state;
  deliberately not renumbered, to avoid churning PR9/PR10's references elsewhere)*:
  - The venue/hotel CSVs moved to `shared/src/commonMain/composeResources/files/`; both
    `QuarterMuseSeed`/`HotelCatalog` moved to `commonMain`, `.load()` now `suspend`, reading
    via `Res.readBytes("files/...").decodeToString()` instead of `context.assets.open(...)`.
    Their tests moved to `commonTest` unchanged (both already only ever called the pure
    `.parseCatalog(String)`, never `.load(context)`, so this was a pure relocate).
  - `BundledPhotos`/`BundledPlaceDetails` had a second hidden Android-only dependency this
    plan's original "Android imports only" check missed entirely, same category of gap PR3
    found in `WalkTime.kt`/`OpeningHours.kt`: `org.json.JSONObject`/`JSONArray` --
    Android's bundled JSON library, not part of the Kotlin/JVM standard library and not
    available on other targets. Replaced with `kotlinx.serialization.json`'s
    `JsonElement`/`JsonObject`/`JsonArray`/`JsonPrimitive` tree navigation (not
    `@Serializable` data classes -- no compiler plugin needed, just the runtime artifact,
    since the manifests' shape doesn't need static modeling to read this way). Confirmed via
    Maven Central that `kotlinx-serialization-json` publishes a `-wasm-js` artifact, current
    latest `1.11.0`. One small, deliberate behavior difference from the original: an opening
    period whose `openDay` key is present but not a real integer is now skipped instead of
    silently defaulting to Sunday (org.json's `optInt` fallback) -- treating malformed data
    as "not a period" as opposed to guessing a day for it, a real hygiene difference but not
    one worth losing sleep over on a value that's always been a valid integer in every
    manifest this pipeline has actually produced.
  - **The photo-URI construction hidden inside `BundledPhotos` -- `"file:///android_asset/photos/$placeId/$file"`
    -- was itself the third hidden Android-only dependency**, unrelated to org.json. Extracted
    into `expect fun photoBaseUri(): String` (`commonMain`): Android's `actual` returns the
    exact same `file:///android_asset/photos/` prefix (unchanged behavior); wasmJs's returns
    the plain relative path `"photos/"`, per this doc's original design (CI copies the
    generated photo tree next to the deployed wasmJs bundle rather than routing binaries
    through Compose resources at all -- `Res.readBytes` being `suspend` is a poor fit for up
    to ~2,095 JPEGs and would bloat the wasmJs bundle for no benefit).
    **Not yet done**: the actual CI step that copies `shared/src/androidMain/assets/photos/`
    into the web build's output. `build-web` (added in PR2) doesn't run
    `fetch_place_photos.py` at all today -- only `build-and-release` does, and the two are
    independent jobs on separate runners with no shared filesystem. Until that's wired up
    (needs either its own fetch step, sharing `build-and-release`'s output via
    upload/download-artifact, or a dependency between the jobs), `photoBaseUri()`'s web path
    resolves to URLs that 404 -- not a crash (`BundledPhotos.load()` already degrades to an
    empty map when the manifest itself is missing, and a 404'd image is just a blank photo
    slot, the same "no photo for this venue" case the UI already renders), but real, visible
    missing functionality on the live web build until it's done. Tracked as a known gap, not
    silently left unremarked.
  - `LamplightViewModel`'s eager `val places = QuarterMuseSeed.load(application)` (and the
    other three) couldn't survive `.load()` becoming `suspend` -- introduced a private
    `Catalog` data class (places/hotels/photosByPlace/placeDetailsByPlace together) loaded
    once in `init` via `viewModelScope.launch`, behind a nullable `mutableStateOf<Catalog?>`.
    **Deliberately did NOT build a dedicated "loading catalog" screen/UI treatment** despite
    this doc's own earlier note budgeting one: `places`/`hotels`/`tags`/`photosConfigured`
    all read through the `Catalog?` and degrade to empty-list/`false` while it's null, so
    every existing call site (`ExploreScreen`'s filtering, `HotelAnchorPrompt`'s hotel list,
    the "N places" count, the "no photos" message) needed zero changes -- they already
    handle an empty catalog gracefully (a guest opening the app sees "0 places" for a moment
    before the data appears, exactly the same shape as any other reactive Compose state
    populating asynchronously, not a hard loading gate). Bundled local assets load fast
    enough on Android that this is unlikely to even be visible in practice; it'll be more
    noticeable on web (a real network fetch, not local I/O) but is still a graceful
    empty-then-populated transition, not a broken or crashing one. A dedicated loading
    treatment is a real, deliberately deferred design decision, not an oversight -- it can be
    layered on later without an architecture change, since the state-based design already
    supports it either way.
  - One real, narrow behavior consequence of the above, worth being explicit about: the
    proximity check in `setCurrentLocation` reads `hotels` (now catalog-backed) to look for a
    nearby match -- if a location fix arrives before the catalog finishes loading, `hotels`
    is still empty and the check finds nothing, the same as if there were simply no fix yet.
    Not a crash or data loss (the guest just falls back to the manual hotel picker instead of
    getting the one-tap "Staying at X?" confirmation), and unlikely in practice given how
    fast catalog loading actually is, but a real, if narrow, race introduced by the
    conversion from eager-synchronous to async loading.
  - `scripts/fetch_place_photos.py`: `CSV_PATH` now reads from the new commonMain resources
    location; `PHOTOS_MANIFEST_PATH`/`PLACE_DETAILS_MANIFEST_PATH` now write there too;
    `PHOTOS_DIR` (the actual JPEGs) is unchanged, still `shared/src/androidMain/assets/photos/`.
    `.gitignore` updated to match (these three stay CI-generated, never committed, same as
    before -- only their path changed).
  Verified: compiles clean for both Android and wasmJs, all 28 `:shared` tests pass (including
  the two relocated CSV test suites), both APK variants build. The JSON loaders' actual
  runtime behavior against a real manifest is **not** verified locally -- `photos_manifest.json`/
  `place_details_manifest.json` are gitignored and only ever exist after a real
  `fetch_place_photos.py` run against a live Places API key, which this sandbox has neither
  reason nor credentials to do; both loaders' graceful-degrade-to-empty path (manifest
  missing entirely) is what actually ran here, same as a fresh clone would see today.
- **PR9**: move the remaining bulk of the UI into `commonMain`. Extract the update-related
  ViewModel fields into an Android-only controller; wire the `platformBanner` slot.
  - **Coil bump to 3.x done as its own isolated first step** *(this one)*: `coil-compose`
    moved from `io.coil-kt:coil-compose:2.7.0` to `io.coil-kt.coil3:coil-compose:3.6.0` --
    a new group id, not just a version bump (Coil 3.x is the multiplatform rewrite; 2.x is
    Android-only and no longer where new versions land). The one call site
    (`PhotoFrame`'s `AsyncImage(model = photo.uri, ...)`) needed only its import updated
    (`coil.compose` -> `coil3.compose`); the composable's own parameters are unchanged
    between 2.x and 3.x for this simple a usage. **Deliberately did not add
    `coil-network-ktor3` yet**, despite this doc's original plan bullet -- `AsyncImage`
    still lives in `LamplightApp.kt` (`androidMain`, not moved to `commonMain` yet), and
    every URI it's ever given today is a local `file:///android_asset/...` path, which
    Coil's core file-fetching component already handles with no network library involved.
    The network component only becomes necessary once `AsyncImage` actually moves to
    `commonMain` and needs to fetch photos over real HTTP on web (`photoBaseUri()`'s
    wasmJs `"photos/"` relative path resolving to an actual browser fetch) -- adding it now
    would be a dependency with nothing yet to justify it. Verified: compiles clean for
    Android and wasmJs, all 28 `:shared` tests pass, both APK variants build.
  - **Update-controller extraction done as its own isolated second step**:
    pulled `LamplightViewModel`'s GitHub-releases self-update state and logic (the
    `githubUpdate`/`githubUpdateDownload` state, the download-complete `BroadcastReceiver`,
    `startGitHubUpdateDownload`) into a new `GitHubUpdateController`
    (`shared/src/androidMain`), matching this doc's own earlier note: "Extract the
    update-related fields into a separate Android-only controller before moving the rest of
    the class to `commonMain`." `LamplightViewModel` now constructs one
    (`GitHubUpdateController(application, viewModelScope)`) and exposes
    `installSource`/`githubUpdate`/`githubUpdateDownload`/`startGitHubUpdateDownload` as thin
    pass-throughs -- every existing UI call site (`LamplightHome`'s `playUpdateStatus` check,
    `UpdateBanner`) needed zero changes, same non-invasive pattern as the `SettingsStore`/
    `LocationProvider` seams in PR4/PR5. `onCleared()` now just calls
    `updateController.dispose()`. `LamplightViewModel` itself and `LamplightApp.kt`'s UI
    stay in `androidMain` for now -- this is deliberately just the extraction, not the move;
    the actual `commonMain` UI move (and the `platformBanner` slot `UpdateBanner`/
    `rememberPlayUpdateStatus` will eventually plug into) is still ahead. Verified: compiles
    clean for Android and wasmJs, all 28 `:shared` tests pass, both APK variants build.
  - **The rest of PR9, done** *(this one)*: `LamplightViewModel` itself moved to `commonMain` --
    constructor now takes `SettingsStore` directly (the real injection PR4's note said would
    arrive here), extends the multiplatform `androidx.lifecycle.ViewModel` instead of
    `AndroidViewModel`, and drops the update-controller pass-throughs entirely (moved to a new
    `AndroidUpdateBanner` composable that `:androidApp`'s `MainActivity` feeds into
    `LamplightApp`'s `platformBanner` slot, replacing the old inline `UpdateBanner(vm,
    playUpdateStatus)` call). `MainActivity` constructs the ViewModel via `viewModel {
    LamplightViewModel(AndroidSettingsStore(application)) }` and the controller via `remember {
    GitHubUpdateController(application, rememberCoroutineScope()) }` -- not `lifecycleScope`,
    which needs its own extra dependency (`lifecycle-runtime-ktx`) this project doesn't
    otherwise need; `rememberCoroutineScope()` is already available inside `setContent` and is
    scoped correctly for a one-time startup fetch.

    `Mood.kt` had no Android-specific code at all, a pure relocate. `Lantern.kt` needed two new
    seams first: `rememberWalkingDirectionsOpener()` (Android still prefers the Maps app's
    turn-by-turn `google.navigation:` deep link, falling back to the same web directions URL
    every other target uses directly -- extracted as `walkingDirectionsUrl()`, a plain shared
    function alongside the existing `mapsSearchUrl()`), and `rememberLocationRequester()` (wraps
    whatever permission step a platform needs before a fix -- Android's
    `ActivityResultContracts.RequestPermission()` dance, bridged into a suspend function via
    `suspendCancellableCoroutine`; nothing extra on web, since the browser prompts on its own
    inside `BrowserLocationProvider`. Collapses what used to be two distinct error messages in
    `HotelAnchorPrompt` -- permission denied vs. fix failed -- into one, since the seam can't
    tell them apart and neither call site actually needed to).

    `LamplightApp.kt` itself -- this doc's own "biggest and riskiest single file" -- needed
    three more fixes: `java.time.DayOfWeek`/`LocalTime` to `kotlinx-datetime` 0.8.0 (confirmed
    via the library's own source on GitHub, not just Maven Central's version list, that 0.7.0
    removed `kotlinx.datetime.Clock`/`Instant` in favor of `kotlin.time`'s, and that `DayOfWeek`
    is no longer a `java.time` type alias -- it's `.isoDayNumber`, not `.value`; a `DayOfWeek(n)`
    factory, not `.of(n)`; and `LocalTime` is `Comparable` with no `.isBefore`, just `<`/`>=`);
    the AGP-generated `R` class to Compose Resources' `Res.drawable` (each generated accessor
    needs its own explicit import, confirmed by how the font resources already do it in
    `Theme.kt` -- there's no wildcard/implicit import of the generated package); and
    `androidx.activity.compose.BackHandler`, which has no multiplatform equivalent (confirmed by
    searching every cached dependency jar for a public one -- Material3 has its own *internal*
    `BackHandler` for sheet/drawer predictive-back, not something app code can call), given the
    same small expect/actual treatment as the rest -- a no-op on web, since this app never
    pushes a browser history entry a back gesture could intercept. Also moved `coil-compose`
    from an `androidMain`-only dependency to `commonMain`, and dropped a leftover unused
    `LocalContext.current` in `PhotoFrame`.

    Separately, restyled the lamppost watermark per direct feedback partway through this PR:
    full screen height instead of just the header row's, behind the whole home screen (the
    banner, the tune/Home-Lantern buttons, the explore screen) rather than just behind the
    explore screen's own header -- moved from `ExploreScreen` up into `LamplightHome`, sized via
    `fillMaxHeight().aspectRatio(...)` instead of a fixed width. Not visually verified against a
    real device or browser -- this sandbox has neither.

    Finally, wired `:webApp`'s actual entry point: `main()` now constructs
    `LamplightViewModel(BrowserSettingsStore())` and renders the real `LamplightApp`/
    `LamplightTheme`, behind a plain `remember {}` rather than the `viewModel {}` factory
    Android uses -- a page load has no config-change/recreation event for that factory to guard
    against, so the extra complexity (and the `LocalViewModelStoreOwner` question it would raise
    on a target with no prior verified answer) buys nothing here. This wasn't spelled out as its
    own line item anywhere in this plan's 10 PRs, but it's what moving the real UI to
    `commonMain` was for -- without it, the deployed web build would still only show the PR2
    spike screen (now retired, `WasmSpikeScreen.kt` deleted) despite the whole app compiling for
    wasmJs. Surfaced one real, if narrow, dependency-scope bug along the way: `:shared` declared
    `androidx.lifecycle.viewmodel.compose` as `implementation`, but `LamplightViewModel` (a
    public `:shared` class) extends `ViewModel` from that dependency -- any module referencing
    it needs `api`, not `implementation`, to see the supertype at all (Kotlin warned "may be
    forbidden soon" rather than failing outright; fixed regardless).

    Verified: compiles clean for Android and wasmJs (`:shared` and `:webApp` both),
    `:shared:testAndroidHostTest` -- all 28 tests still pass (including `OpeningHoursTest`'s
    actual runtime behavior against the new kotlinx-datetime parsing, not just its types -- the
    `LocalTime.parse("09:00")`-shaped strings this app's data already uses), both
    `:androidApp:assembleDebug`/`assembleRelease` succeed. `:webApp:wasmJsBrowserDistribution`
    itself -- the actual production bundle -- still can't be verified end-to-end in this sandbox
    (the same `codeload.github.com`-blocked Yarn/karma fetch as PR2/PR5); real CI remains the
    verification point for that specifically, and for the live Pages deploy this entry point now
    actually populates with app content instead of the spike screen.
- **PR10** *(this one)*: CI polish -- `codeql.yml`'s JDK pin (still 19, unrelated to and
  predating this migration, easy to miss since that workflow only runs on a weekly cron plus
  manual dispatch) now matches the rest of the project's 21; `build-web` gets the same "Report
  Failure to Jules" auto-issue-filing `build-and-release` already had, so a web-only build
  failure gets the same visibility an Android one does instead of just a bare red check. Docs
  updated (this section, and `roadmap.md`). Final Android-parity check: `:androidApp`'s
  release-signing, versioning, and sideload update-check/download/install/cleanup flow are all
  unchanged by this entire migration -- `GitHubUpdateController`/`AndroidUpdateBanner` (PR9) are
  a straight extraction of the exact same logic `LamplightViewModel`/`LamplightApp.kt` used to
  own directly, not a rewrite, and every other Android-only surface (`MainActivity`, the
  manifest, signing config) was untouched by the UI's move to `commonMain`. No manual on-device
  verification was possible in this sandbox (no emulator or connected device) -- `assembleRelease`
  succeeding with signing enabled, and every prior PR's own compile/test verification along the
  way, is what stands in for it here.

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
  guests on unmanaged, older-iOS phones are a real fraction of this audience. **Decided
  2026-08-29**: added `js { browser() }` to both `:shared` and `:webApp` as a genuine
  fallback, not a stub -- Compose Multiplatform's Skiko renderer uses a separate, baseline
  (non-GC) WASM module even under the classic `js` backend, so this target's actual floor is
  the ~2017 baseline-WASM one, far below WasmGC. Verified for real: both modules'
  `compileKotlinJs` succeed, `checkJsMainComposeLibrariesCompatibility` (a real Compose
  Gradle plugin task) passes, `ComposeViewport` resolves identically on both targets, and
  `jsProcessResources` produces the expected `index.html` + `skiko.wasm`/`skiko.mjs` pair.
  CMP-4906 (see PR2's gotcha list above) recurred on `:shared`'s new `js` target exactly as
  it did on wasmJs -- caught by real CI, not local verification, since narrower local task
  runs (`compileKotlinJs`, `testAndroidHostTest`) don't exercise
  `checkComposeUiTestConfigurationForJs` the way `allTests` does. Same fix, same target:
  `binaries.executable()`. A lesson worth stating plainly: for this project, only `allTests`
  itself is the real check -- a green narrower task set doesn't mean `allTests` is green too.
  A custom `webMain` intermediate source set (explicit `dependsOn` on both `jsMain` and
  `wasmJsMain`, not Kotlin's default hierarchy template -- `getByName("webMain")` failed at
  configuration time with "KotlinSourceSet with name 'webMain' not found" when tried first,
  so `kotlin.mpp.applyDefaultHierarchyTemplate=false` is now set in `gradle.properties`)
  holds the browser-interop code identical across both targets: `BrowserSettingsStore`,
  `PhotoBaseUri`, `UrlOpener`/walking-directions, `BackHandler`, and even `:webApp`'s
  `main()` and `index.html` itself. `BrowserLocationProvider` stays duplicated per target on
  purpose -- Kotlin/Wasm's `@JsFun` interop and classic Kotlin/JS's `js("...")` intrinsic are
  genuinely different mechanisms, not just different code shape. **What this does not yet
  do**: reach a real user. CI still only builds and deploys the wasmJs bundle; nothing
  detects WasmGC support or serves the `js` bundle instead, and this sandbox has the same
  Node/Yarn/Karma network gap for `jsBrowserDistribution` that already blocks verifying
  `wasmJsBrowserDistribution` locally. Tracked as its own open item in `TODO.md`.
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

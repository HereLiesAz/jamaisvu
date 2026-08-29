# TODO

Open items tracked in one place, pulled from `docs/roadmap.md`,
`docs/kmp-web-migration-plan.md`, `docs/design-system.md`, and things found
along the way. Check items off (or delete them) as they're resolved; add new
ones here instead of letting them live only in a chat transcript.

## Blocking the live web deploy

- [x] **Enable GitHub Pages for this repo.** Done (by the repo owner, not a
      code change) -- confirmed by the first-ever successful `deploy-web`
      run on a real push to `main`: `fetch-places-data`, `build-and-release`,
      `build-web`, and `deploy-web` all succeeded, including the actual
      "Deploy to GitHub Pages" step.
- [x] **Get real venue photos into the web build.** An adversarial audit
      caught that the first version of this (photo binaries merged into
      `dist/photos/` by `deploy-web`, after `build-web` already compiled)
      was inert: `BundledPhotos`/`BundledPlaceDetails.load()` read
      `photos_manifest.json`/`place_details_manifest.json` via Compose
      Resources (`Res.readBytes`), which has to exist *before* the wasmJs
      compile, not merged into the build's output afterward the way the
      JPEG binaries themselves correctly can be (those are fetched over
      plain HTTP at runtime, not compiled in) -- `build-web` never had
      access to them, so `BundledPhotos.load()` always returned empty on
      web regardless of the photos sitting right there in `dist/photos/`.
      Fixed by pulling the fetch out into its own `fetch-places-data` job
      that both `build-and-release` and `build-web` depend on; `build-web`
      now downloads the manifest JSON *before* compiling, so it's actually
      embedded in the wasmJs bundle. Also fixed a real pre-existing bug the
      audit surfaced while tracing this: `fetch_place_photos.py`'s CSV
      header assertion expected 5 columns, but the real CSV has 6
      (`Featured` was added at some point and this script never got
      updated) -- it would have crashed on every real run, before ever
      writing a manifest. **Confirmed working on the first real deploy**:
      `fetch-places-data` actually fetched (a real API key is configured),
      `build-web` downloaded the manifest before compiling, and the site
      deployed. Whether the live site actually *displays* the photos is
      still unverified -- see the index.html item just below, which was
      blocking that regardless of this fix.
- [x] **Add the missing `index.html`.** The live site 404'd at its root
      even after the first successful deploy -- traced via the deploy job's
      own archived-file listing to a bug that predates all of this session's
      work (since `:webApp` was first stood up): `wasmJsBrowserDistribution`
      produces only `.wasm`/`.js`/resource files, no HTML entry point, and
      nobody had a way to notice since Pages was never enabled before now.
      `ComposeViewport("lamplightWeb")`'s container-id contract (confirmed
      against the actual Compose Multiplatform source) requires a DOM
      element with that exact id to already exist, or it throws at runtime
      -- simplified to `ComposeViewport()` (null id, attaches directly to
      `<body>`) so a hand-written `index.html` doesn't need to keep an
      element id in lockstep with a Kotlin string, and added
      `webApp/src/wasmJsMain/resources/index.html` with a plain
      `<script src="webApp.js">`. Verified locally as far as possible: the
      file lands correctly in `:webApp:wasmJsProcessResources`'s output
      (a real, confirmed dependency of `wasmJsBrowserDistribution`), and
      `webApp.js` is confirmed as the real compiled entry filename from the
      actual CI deploy's own file listing, not guessed. **Confirmed on the
      live site after this merged and deployed**: `https://hereliesaz.github.io/lamplight/`
      now returns real HTML (`<title>Lamplight</title>`, a `webApp.js`
      script tag) instead of a 404 -- checked directly, not assumed. Still
      unverified: whether the wasmJs bundle actually boots and renders the
      Compose UI in a browser. Tried to check this for real with the
      Playwright/Chromium already available in this sandbox -- every
      attempt (default config, HTTP/2 disabled, an explicit proxy option)
      failed identically with a connection reset from this sandbox's own
      egress proxy tunneling to `hereliesaz.github.io`, distinct from the
      plain HTTP fetch above (which works fine through the same proxy) --
      a sandbox/tooling limitation, not something wrong with the deploy.
      A real device/browser check is still the next real verification.
- [ ] **Commit a real `kotlin-js-store/wasm/yarn.lock`.** The one in the repo
      is empty. Root cause is now pinned down precisely, not just assumed:
      `:kotlinWasmToolingSetup` needs a workspace-wide `yarn install`, which
      needs `karma` (Kotlin's fork, `Kotlin/karma`, test tooling for wasmJs
      -- pulled in for the whole Yarn workspace even when only building
      `:webApp`'s production bundle, not running tests) from
      `codeload.github.com`. In this sandbox that request gets a 403 --
      not from GitHub, but from this session's own GitHub-access gate
      (the same one `add_repo` manages): `git clone`/`git fetch` against
      public repos are served directly by a separate anonymous git proxy,
      but Yarn's own HTTPS tarball fetch isn't a `git` command, so it
      doesn't get that pass-through. Confirmed via `curl` against the
      literal failing URL -- the response body is Claude Code's own
      "GitHub access... not enabled for this session" message, not
      anything from GitHub. Requesting `add_repo` push access to
      `Kotlin/karma` (a third-party OSS repo) just to route a build tool
      around this would be a disproportionate ask for what's a
      local-verification convenience -- **real GitHub Actions runners have
      no such gate**, so this is confirmed sandbox-only and not a risk to
      the actual deploy pipeline. Still needs a run in an unrestricted
      environment (real CI, or a local machine) with the lockfile
      committed from there.

## Verification gaps (nothing here failed -- it's just never been checked)

- [ ] Real browser behavior for `BrowserLocationProvider` (permission
      prompt, actual GPS fix) -- compiles, never run in a browser.
- [x] `:webApp:wasmJsBrowserDistribution` and wasmJs `allTests` end-to-end --
      still blocked locally (same session-scoped GitHub-access gate as the
      Yarn lockfile above), but confirmed working for real: both succeeded
      in actual CI on PR #38 and on the first push to `main` that deployed.
- [ ] `:webApp:jsBrowserDistribution` and js `allTests` end-to-end -- same
      gap as the wasmJs line above, not yet closed the same way: nothing in
      CI builds or runs it yet (see "Serve the `js` fallback to real
      browsers" above), so unlike wasmJs there's no real-CI run to point to
      yet, only the local compile-task verification recorded there.
- [x] `BundledPhotos`/`BundledPlaceDetails` JSON parsing -- their `load()`
      itself still can't be exercised without a real Compose Resources
      manifest (no Places API key here to generate one for real), but the
      actual parsing logic is no longer untested: split out into a
      `parseManifest(text: String)` in each, fed fixture strings matching
      exactly what `scripts/fetch_place_photos.py` writes (checked against
      the script itself, not guessed), covering a fully populated entry,
      a null `closeDay`/`closeTime` (an overnight span with no recorded
      close), a malformed entry dropped without dropping its whole venue,
      and invalid JSON falling back to empty rather than crashing. 11 new
      tests pass.
- [ ] Visual check of the launcher icon and the full-height lamp watermark
      on a real device/emulator and in a real browser (PR #34) -- written
      and compiled correctly by inspection, never rendered.
- [ ] On-device Android check: release signing, versioning, and the
      sideload update-check/download/install/cleanup flow, after the whole
      Kotlin Multiplatform migration. Build-level checks (assemble,
      `allTests`) all pass; nothing here has run on an actual device.
- [ ] Narrow race, probably fine to just accept: a location fix that lands
      before the catalog finishes loading sees an empty hotel list and
      misses the proximity "Staying at X?" match, falling back to the
      manual picker instead. Rare in practice given how fast the catalog
      loads.

## Decisions (raised with the user 2026-08-29)

- [x] Business-side monetization mechanism -- hand-editing the CSV's
      `Featured` column stays the mechanism for now. No code change.
- [x] Explore/Discover curated vs. show-everything -- **Featured-first
      ordering**, not a Featured-only filter: the full catalog still shows
      (zero venues are marked Featured today, so a Featured-only view would
      currently be empty), Featured places just sort first. See below.
- [x] "Been There"'s future -- **stays exactly as it works today**, but a
      place marked Been now also gets framed as a "Next Trip" candidate
      (auto-fed from the same data, not a separate toggle). See below.
- [x] Browser floor for the web build -- add a `js` target as a
      compatibility fallback. See below.
- [x] Google Maps Platform compliance -- **closed, not a concern**: this
      build isn't going on a public app store or being publicly promoted,
      it's handed out personally. No compliance work needed.

- [x] **Explore: Featured-first sort.** Sort becomes a compound key --
      Featured first, then the existing proximity sort within each group.
      Grid never empties; nothing currently marked Featured just means no
      visible reordering yet, exactly as expected until the CSV is edited.
- [x] **"Been" -> Next Trip framing.** Kept the "Been" filter chip and
      `isVisited()` data exactly as today. When that filter is active, a
      "NEXT TRIP -- worth another look" header now shows above the results
      (matching the "GOOD FOR" section's visual pattern) -- no new toggle,
      no new stored state, just a reframing of the same data.
- [x] **Trimmed Explore's filter chips**, per explicit feedback mid-build --
      dropped the "All" chip and the one-chip-per-distinct-tag row
      entirely; Saved/Been/Seen/Featured are the only chips now, by-tag
      filtering is search's job. `LamplightViewModel.tags` (only ever fed
      that row) removed with it.
- [x] **Persona/ranking layer** (client brief #7) -- `MoodRanking.kt`'s
      `moodRelevanceScore()` reads the group-size/vibe answers and adds a
      ranking boost to Explore's sort (Featured, then mood score, then
      proximity) -- a ranking signal only, same philosophy as Featured
      itself, never a filter, so no combination of answers can ever empty
      the grid. Grounded in real tags wherever a direct match exists
      (Family-Friendly, First Timer/Tourist Essential, Solo Traveler
      Friendly are literal tags; Food First/Cocktails First/Music
      Tonight/Rain Plan reuse Discover's own category membership). The
      other 7 vibes are a curated subset of real tags -- a genuine
      judgment call, documented in the file, since there's no "romantic"
      or "business-safe" column in the data. Two answers deliberately
      contribute nothing rather than a guessed something: Low Walking
      (proximity's already a tiebreaker regardless of vibe) and the 2-4/5+
      group sizes (no group-size tag exists in this catalog beyond solo).
      12 new unit tests pass.
- [x] **`js` compatibility target.** Added `js { browser() }` alongside the
      existing `wasmJs { browser() }`, to both `:shared` and `:webApp`.
      Verified for real (not assumed): Compose Multiplatform's own Gradle
      plugin runs a `checkJsMainComposeLibrariesCompatibility` task and both
      `compileKotlinJs` tasks succeed, so the `js` target is genuinely
      Compose-supported, just not the flagship. `ComposeViewport` (the app's
      bootstrap call) resolves identically on both targets, and
      `jsProcessResources` produces the same `index.html` plus Skiko's own
      `skiko.wasm`/`skiko.mjs` pair -- Skiko's renderer uses a separate,
      baseline (non-GC) WASM module under `js` too, which is *why* this
      floor is genuinely lower: baseline WASM has near-universal browser
      support since ~2017, far below wasmJs's WasmGC requirement.
      A custom `webMain` intermediate source set (explicit `dependsOn`, not
      Kotlin's default hierarchy template -- `getByName("webMain")` failed
      at configuration time when tried first) holds the browser-interop code
      that's genuinely identical across both targets: `BrowserSettingsStore`,
      `PhotoBaseUri`, `UrlOpener`/walking-directions, `BackHandler`, and even
      `:webApp`'s `main()` and `index.html` themselves. `BrowserLocationProvider`
      is the one seam that stays duplicated per target on purpose -- the
      JS-interop mechanism itself differs (Kotlin/Wasm's `@JsFun` vs. plain
      Kotlin/JS's `js("...")` intrinsic), not just the code shape.
      `kotlin.mpp.applyDefaultHierarchyTemplate=false` added to
      `gradle.properties` to silence the resulting warning.
      Scope check: this makes `:shared` and `:webApp` *compile* for `js` on
      both modules, and all existing tests plus both Android APK variants
      stay green -- it does not yet make anything reach a real user. CI
      still only builds and deploys the wasmJs bundle; nothing detects
      WasmGC support or serves the `js` bundle instead. See the new item
      below.
- [ ] **Serve the `js` fallback to real browsers.** The `js` target compiles
      but nothing routes to it yet. Needs: a CI job that also builds
      `:webApp:jsBrowserDistribution` (this sandbox can't verify that task
      at all -- browser-distribution builds need a Node/Yarn/Karma toolchain
      download this sandbox's network doesn't allow, the same gap already
      blocking wasmJs distribution verification); a place to deploy the
      second bundle alongside the wasmJs one without the two colliding
      (both currently produce a same-named `webApp.js`, harmless while each
      target's dist output stays in its own directory, a real question once
      both are served from the same GitHub Pages root); and a small
      feature-detection bootstrap in `index.html` (or a shared loader
      script) that checks for WasmGC support and loads the right bundle.
      None of this is hard, but it's a real, undiscussed product decision
      (a heavier page-load path for the fallback case) rather than a
      mechanical follow-on to the target existing.

## Not started (client-brief features, per `docs/roadmap.md`'s build-priority list)

- [ ] Home screen "one next move" front door -- still just `ExploreScreen`'s
      search/filter grid.
- [ ] "Tonight" -- a 3-4 stop loop built from the mood/group-size answers.
- [x] Discover's 8 fixed categories (Happy Hour etc.) as their own screen --
      reachable via a new compass icon next to the mood/vibe icon on the
      home screen (a judgment call: the brief's own bottom-nav placement is
      blocked on the nav structure below not existing yet, so this was the
      lightest-weight entry point consistent with today's UI). Category
      membership is derived from each place's existing tags (CSV categories
      + Google place-types + review keywords) via a fixed keyword mapping,
      not stored -- see `discoverCategoriesFor()`. The 8 category taglines
      are new copy (short, category-level, not venue-specific); everything
      else reuses existing data, no per-venue editorial lines invented.
      7 new unit tests pass; not visually verified (no emulator/browser).
- [x] A persona/ranking layer that actually reads the group-size/vibe
      answers guests already give -- `MoodRanking.kt`'s `moodRelevanceScore()`,
      see the "Decisions" section above for full detail.
- [ ] Lantern List's Tonight/Later/Next-trip sections -- still only flat
      filter chips (Saved/Been/Seen/Featured), though "Been" now carries a
      lightweight "NEXT TRIP" header placeholder when active (see
      "'Been' -> Next Trip framing" above) -- not the full sectioned screen
      the brief describes.
- [x] Place detail: "good for" tags -- a fixed, ordered shortlist (Family-
      Friendly, Solo Traveler Friendly, LGBTQ+, Vegan-friendly, Vegetarian-
      friendly, First Timer Essential, Cheap Eats, Free Admission, Rainy
      Day Option) surfaced from a place's existing tags via `goodForTagsIn()`,
      its own row above the full tag list so they don't get lost in it. No
      new data, no invented copy -- every entry is a tag the catalog
      already carries. 5 new unit tests pass.
- [ ] Place detail: practical notes (dress code, cash-only, reservations)
      and the Go now/Add to tonight/Next nearby actions. Practical notes
      need new Places API fields (`paymentOptions`, `reservable`) that
      aren't fetched today -- a `fetch_place_photos.py` change needing a
      real API key to verify, not available in this sandbox. The actions
      need "Tonight" above to exist first.
- [ ] Four Panes' spec'd bottom-nav behavior (lit pane = current section,
      sequential lighting while a section loads) -- blocked on the
      Home/Tonight/Discover navigation structure above not existing yet.

## Bundle/perf, unmeasured

- [ ] wasmJs bundle size, still unmeasured (blocked -- this sandbox can't
      build `wasmJsBrowserDistribution`). `material-icons-extended` itself
      is now a checked fact, not a guess: 8 of the app's 13 distinct icons
      (Bookmark, BookmarkBorder, Explore, Language, Map, PhotoLibrary,
      Schedule, Tune) aren't in the small `material-icons-core` set every
      Material3 app gets for free, so the dependency is genuinely needed,
      not droppable outright. Its runtime jar is ~84MB against core's
      ~2MB, confirmed from the actual artifacts in the Gradle cache --
      whether Kotlin/Wasm's dead-code elimination actually strips the
      ~unused rest of it from the final bundle (the real question) is
      still unmeasured, same blocker as above. Hand-rewriting just those 8
      icons as local `ImageVector`s to drop the dependency entirely was
      considered and rejected: it would mean transcribing exact vector
      path data out of decompiled bytecode with no way to visually verify
      the result in this sandbox -- too easy to ship a subtly wrong icon
      with no way to catch it.
- [x] `coil-network-ktor3` -- added to `:webApp` (not `:shared`, Android
      needs no network engine at all) now that the CI change above gives
      `AsyncImage` real HTTP URLs to fetch on web. `:webApp`'s `main()`
      registers it via `setSingletonImageLoaderFactory` before rendering
      `LamplightApp`. Compiles clean; real image loading over the network
      still can't be verified in this sandbox (no wasmJs runtime to load a
      page in), so this is unverified beyond the type-check until it's live.

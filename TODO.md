# TODO

Open items tracked in one place, pulled from `docs/roadmap.md`,
`docs/kmp-web-migration-plan.md`, `docs/design-system.md`, and things found
along the way. Check items off (or delete them) as they're resolved; add new
ones here instead of letting them live only in a chat transcript.

## Blocking the live web deploy

- [ ] **Enable GitHub Pages for this repo.** `deploy-web` has failed on every
      push to `main` since it was added: `Settings -> Pages -> Build and
      deployment -> Source -> "GitHub Actions"` is not set, so
      `actions/deploy-pages` gets a 404 creating the deployment. `build-web`
      itself succeeds every time (the wasmJs bundle builds and uploads fine)
      -- this is the one remaining manual step, not a code fix. Direct link:
      https://github.com/HereLiesAz/lamplight/settings/pages
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
      embedded in the wasmJs bundle. A fetch failure still can't block
      either build (same `!cancelled()`-without-checking-the-result idiom
      as before). Also fixed a real pre-existing bug the audit surfaced
      while tracing this: `fetch_place_photos.py`'s CSV header assertion
      expected 5 columns, but the real CSV has 6 (`Featured` was added at
      some point and this script never got updated) -- it would have
      crashed on every real run, before ever writing a manifest. Verified
      the fix directly against the real CSV (419 venues parse cleanly);
      the CI job graph itself is still unverified against a live deploy --
      blocked on the Pages-enablement item above, and doubly untestable
      locally (this sandbox can't run `wasmJsBrowserDistribution`, and
      triggering a real `workflow_dispatch` run to check would spend real
      Places API quota just to verify a CI change, not worth it).
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
- [ ] `:webApp:wasmJsBrowserDistribution` and wasmJs `allTests` end-to-end --
      blocked locally by the same session-scoped GitHub-access gate as the
      Yarn lockfile above (confirmed by actually re-running it this
      session, not just assumed still-blocked); real CI is the actual
      verification point.
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

## Decisions to make (not yet raised with the user, or explicitly deferred)

- [ ] Business-side monetization mechanism (paid placement, claimed
      listing, something else) -- "Featured" has no way to be set today
      beyond hand-editing the CSV, blocked on this.
- [ ] Whether Explore/Discover needs a "curated highlights" vs. "show
      everything" mode now that the catalog is ~3x its original size.
- [ ] Whether "Been There" stays a bonus feature, folds into a "next trip"
      concept, or gets dropped.
- [ ] Browser floor for the web build: wasmJs needs Chrome 119+/Firefox
      120+/Safari 18.2+, no fallback for older devices. Accept the gap, or
      add a `js` target later as a compatibility fallback?
- [ ] Google Maps Platform compliance: does publishing bundled Places photos
      on a public, statically-hosted site (vs. inside an installed app)
      need anything beyond the existing attribution handling? Worth a real
      check before the web build is publicly promoted.

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
- [ ] A persona/ranking layer that actually reads the group-size/vibe
      answers guests already give (the prompts exist; nothing consumes them
      yet).
- [ ] Lantern List's Tonight/Later/Next-trip sections -- only flat filter
      chips (Saved/Been/Seen) exist today.
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

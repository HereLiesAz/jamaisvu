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
- [ ] **Copy photo binaries into the web build.** `build-web` never runs
      `scripts/fetch_place_photos.py`, so `photoBaseUri()`'s web-relative
      paths 404 once deployed (non-crashing -- renders as "no photo," same as
      a venue with none). Needs either its own fetch step in `build-web`, or
      sharing `build-and-release`'s output via upload/download-artifact.
- [ ] **Commit a real `kotlin-js-store/wasm/yarn.lock`.** The one in the repo
      is empty -- this sandbox's network policy blocks the
      `codeload.github.com` fetch Yarn needs, so it's never been generated
      for real. Needs a run in an unrestricted environment (real CI, or a
      local machine) with the lockfile committed from there.

## Verification gaps (nothing here failed -- it's just never been checked)

- [ ] Real browser behavior for `BrowserLocationProvider` (permission
      prompt, actual GPS fix) -- compiles, never run in a browser.
- [ ] `:webApp:wasmJsBrowserDistribution` and wasmJs `allTests` end-to-end --
      blocked locally by the same network policy as the Yarn lockfile above;
      real CI is the actual verification point.
- [ ] `BundledPhotos`/`BundledPlaceDetails` JSON loaders against a real
      manifest -- only the empty/missing-manifest path has ever run; no
      Places API key in this sandbox to generate a real one.
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
- [ ] Discover's 8 fixed categories (Happy Hour etc.) as their own screen.
- [ ] A persona/ranking layer that actually reads the group-size/vibe
      answers guests already give (the prompts exist; nothing consumes them
      yet).
- [ ] Lantern List's Tonight/Later/Next-trip sections -- only flat filter
      chips (Saved/Been/Seen) exist today.
- [ ] Place detail: "good for" tags, practical notes (dress code, cash-only,
      reservations), and the Go now/Add to tonight/Next nearby actions --
      the last of those needs "Tonight" above to exist first.
- [ ] Four Panes' spec'd bottom-nav behavior (lit pane = current section,
      sequential lighting while a section loads) -- blocked on the
      Home/Tonight/Discover navigation structure above not existing yet.

## Bundle/perf, unmeasured

- [ ] wasmJs bundle size, and `material-icons-extended` specifically --
      flagged as worth measuring, never actually measured. A Compose
      Multiplatform "hello world" already ships a non-trivial wasm+JS
      payload before any app code; hotel wifi makes first-impression load
      time a real concern, not just an aesthetic one.
- [ ] `coil-network-ktor3` -- deferred until `AsyncImage` needs to fetch a
      real HTTP URL on web (today every URI is either a local
      `file:///android_asset/...` path or the still-unfixed 404 above).

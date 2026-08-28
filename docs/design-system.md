# Design system

The implementation-facing half of [`product-direction.md`](product-direction.md)'s
"Aesthetic Direction" section. This file should stay in sync with
`app/src/main/java/com/hereliesaz/lamplight/ui/Theme.kt` -- if you change a token there,
update the value here too, and vice versa.

## Color

One accent, everywhere. There is no second brand hue -- states that used to be
color-coded (e.g. a green "visited" checkmark) are differentiated by icon shape and
placement instead, never by introducing another color.

| Token (`Theme.kt`) | Hex | Role |
|---|---|---|
| `Ink` | `#080A09` | App background. Near-black, not pure black. |
| `Panel` | `#111512` | Raised surfaces: nav bar, sheets, cards, banners. |
| `Amber` | `#FFC24B` | The only bright accent -- primary actions, selected states, active icons. |
| `Cream` | `#F2EFEA` | Primary text and icons (off-white, never pure white). |
| `Fog` | `#AFAFAA` | Secondary text, metadata, inactive icons. Neutral gray, no green cast. |

Anything not listed above (error red, container tones) lives in `LamplightColors` in
`Theme.kt` and is a supporting shade of one of the tokens above, not a new hue.

## Shape

Square, not rounded. `LamplightShapes` in `Theme.kt` keeps every corner radius at 0-4dp:

| Shape slot | Radius | Used by |
|---|---|---|
| `extraSmall` / `small` | 0dp | Text fields, chips, buttons |
| `medium` / `large` | 2dp | Sheets, dialogs |
| `extraLarge` | 4dp | Photo frames, mosaic tiles |

No elevation-heavy `Card` treatments beyond what's already in the mosaic grid; new surfaces
should default to a flat background plus a hairline divider (`Fog` at low alpha, 1dp) rather
than a shadowed card.

## Typography

**Integrated.** The client brief's original pick, Uncut Sans, is a commercial foundry
typeface with no confirmed license for bundling into this app -- rather than ship
unlicensed, **Archivo** (SIL OFL, Google Fonts, published by Omnibus-Type) stands in for it.
It was chosen specifically because, like Uncut Sans, it's designed for confident display
headlines and has a true Black weight, matching the `FontWeight.Black` already used
throughout for venue names and the wordmark. If the client secures an Uncut Sans license
later, swapping it back in is a one-file change (`ArchivoFamily` in `Theme.kt`).

- **`ArchivoFamily`** (`Theme.kt`) for display headlines, venue names, and primary CTAs.
  Set as `MaterialTheme.typography.bodyLarge`'s font, which is what every plain `Text()`
  call in this codebase resolves to when it doesn't specify its own style -- that's what
  makes it the effective app-wide default without touching every call site.
- **`MartianMonoFamily`** (`Theme.kt`, Evil Martians, SIL OFL) for functional/utility text:
  distance, category, time, status, coordinates, "OPEN," directional detail. Applied via an
  explicit `fontFamily = MartianMonoFamily` parameter at each such call site (eyebrow labels,
  walk-time text, open/closed status, hours, phone/website/address, place counts) --
  deliberately not the theme default, so it stays opt-in and legible at a glance in the code
  which text is "data" versus "editorial voice."

Font files live in `res/font/` as static-weight TTFs (`archivo_regular/_bold/_black`,
`martian_mono_regular/_bold`), fetched directly from Google Fonts' own CDN
(`fonts.gstatic.com`) rather than the web-optimized WOFF2 the browser-facing CSS API
normally serves, since Android's font resource system needs TTF/OTF.

## Photography

- Warm interior lighting; texture over posed shots (a bar top, a doorway, a stage).
- Real people only when candid, never stock-photo-perfect.
- Editorial cropping, moderate grain, realistic color.
- Never: Bourbon Street party imagery, beads/masks/fleur-de-lis/voodoo props, Mardi Gras
  purple/green/gold, tarot, skulls, Spanish moss, wrought iron, fog machines, or any other
  theme-park New Orleans shorthand.

Google-sourced photos keep their existing Google Maps attribution treatment
(`PhotoFrame`/`PhotoAttribution` in `LamplightApp.kt`) -- that's a Google Maps Platform
compliance requirement, not a style choice, and stays as-is regardless of the rest of the
visual system.

## The Four Panes lantern mark

A lantern reduced to a 2x2 pane grid -- geometric and legible at small sizes, never a
literal antique street lamp. Implemented twice, for two different needs:

- **Static mark** (`res/drawable/ic_launcher.xml`): the app launcher icon. Fixed, all four
  panes lit, amber on ink.
- **Dynamic mark** (`FourPanesMark` composable, `ui/Lantern.kt`): takes a `litCount: Int`
  (0-4) so it can represent state -- currently used by the Home Lantern FAB (1 pane lit =
  no hotel saved yet, 4 lit = anchor set). The client brief also specifies two behaviors not
  yet wired up:
  - Bottom navigation: the lit pane indicates the current section.
  - Loading state: panes illuminate one at a time.

  Both are straightforward extensions of `FourPanesMark` once the Home/Tonight/Discover
  navigation structure exists (see `roadmap.md`) -- don't build a second mark component for
  them.

## Explicit non-goals

Carried over from the product brief because they're as much a design constraint as a
product one: no user accounts/profiles, no social features (follows, likes, comments,
uploads), no star ratings or review counts, no infinite-scroll-as-primary-experience, no
in-app turn-by-turn navigation (Maps handoff only), no AI chat as the home screen, and no
mystical/witchy/theme-park New Orleans visual shorthand anywhere in the app.

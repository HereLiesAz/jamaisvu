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

**Not yet integrated** -- the app currently renders with the Compose default typeface. The
target system, per the client brief, is:

- **Uncut Sans** for display headlines, venue names, and primary CTAs.
- **Martian Mono** for functional/utility labels: distance, category, time, status, "OPEN,"
  coordinates.

Before bundling either as an app font resource (`res/font/`), confirm licensing:

- **Martian Mono** is open source (SIL OFL), published by Evil Martians -- safe to bundle
  once downloaded from its official source.
- **Uncut Sans** is an independent-foundry commercial typeface. Do not bundle a font file
  into the repo or app without confirming the license covers app distribution. Get the
  license file/receipt from whoever owns it before this ships.

Until fonts are integrated, keep the existing hierarchy (large black-weight headline, small
bold-caps eyebrow label, regular body) -- it approximates the target hierarchy with system
fonts and needs no rework beyond swapping the `FontFamily` once licensing is resolved.

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

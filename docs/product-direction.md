# Product direction

This file preserves the client's original design briefs verbatim, so the product intent
survives independently of any one conversation. When the two disagree, **Brief 2 wins** --
it is the client's explicit course-correction on top of Brief 1 ("Agreed. None of that.").

Treat this as source material, not as a spec to re-derive from memory each session:
[`design-system.md`](design-system.md) and [`roadmap.md`](roadmap.md) translate the active
parts of it into concrete tokens and a build checklist that should stay in sync with the code.

## Status

- **Brief 1 -- monetization tiers**: shelved. Not part of MVP scope. Kept below for later,
  in case the client revisits paid tiers once the free North Star experience is proven out.
- **Brief 2 -- North Star product and design direction**: active. This is what the app is
  currently being built against.

---

## Brief 2 (active): Product and Design Direction

We are building **Lamplight**, a mobile-first French Quarter discovery concierge.

**Tagline:** *Best lit plans.*

It may take inspiration from Pao's polished editorial discovery experience, but it must
**not** become a Pao clone: no social profiles, following, user uploads, comments, likes,
crowdsourced ratings, travel diaries, or an open-ended global-city database. Pao is broad
discovery; Lamplight is a highly curated, place-specific decision tool.

### Product North Star

**A hotel guest opens Lamplight, confirms where they are staying, and immediately gets one
excellent, practical next move.**

The app should remove "What do we do now?" fatigue -- not create more browsing.

The product philosophy is:

> **One anchor. One loop. One recommendation. One exit.**

"Exit" means a guest always has a clear next action:
- Walk there
- Add it to tonight
- Save it for later
- Get back to the hotel

The content database begins as a deliberately small, maintained list of French Quarter and
immediately adjacent places. Editorial judgment is the product; do not build an inventory
dump. This matches Lamplight's original curated-loop model rather than a broad
social-discovery network.

### North Star Hotel Feature

#### Hotel Anchor

On first open, ask one question:

> **Where are you staying?**

Allow:
- Hotel selection from a short list
- "Use my location"
- "I'm not staying at a hotel"

Once chosen, the hotel becomes the guest's **Home Lantern** -- their fixed point for the
entire stay.

The app should remember it without requiring an account.

#### What the hotel anchor does

Every recommendation should understand:
- Approximate walking distance from the hotel
- Approximate walk back
- Whether the guest is moving toward or away from their hotel
- Whether a stop makes sense for the current time of day
- Whether it is realistic before a reservation, tour, show, or bedtime

Use plain, useful phrasing:

- "Six-minute walk from your hotel."
- "On the way back."
- "Good final stop before heading in."
- "A two-stop loop from your lobby."
- "Three minutes from your hotel, open late."
- "You can be back by 10:15."

Do not pretend to offer emergency safety guarantees. The value is orientation, clarity, and
realistic walking decisions.

#### Return-to-hotel action

The persistent lantern icon is not merely a logo. It is the **Home Lantern**.

Tapping it should:
1. Show the selected hotel name.
2. Show a single large **"Take me back"** action.
3. Open walking directions in Apple Maps or Google Maps.
4. Optionally offer one useful nearby recommendation first: "One good last stop on your way
   back."

This is a major differentiator. The user should feel that Lamplight knows where their night
began and can always bring the plan back into focus. The app's lantern mark is already
intended to represent the navigation/product system as well as the brand.

### Core MVP Screens

#### Home / Now

This is the primary screen, not an endless feed.

Show:
- Greeting: "Good evening."
- Hotel anchor: "Starting from The Roosevelt."
- One strong recommendation: **"Your next good move."**
- One place card: photo, name, walk time, one-sentence editorial reason, open status.
- Primary button: **Go now**
- Secondary button: **Give me another**

Example:

> **Your next good move**
> French 75 Bar
> 8 min from your hotel · Open until 11 PM
> A quiet, polished first drink before the Quarter gets louder.
> **[Go now]**  **[Another option]**

#### Tonight

A lightweight, walkable sequence of three or four stops.

Show:
- Start point: the guest's hotel
- A short loop or directional sequence
- Total approximate walking time
- One featured stop at a time
- A final "Head home" action

Do not build a complex route optimizer. The logic should be curated and rule-based: open
hours, category, rough proximity, and hotel anchor.

#### Discover

This is the Pao-inspired portion: visually excellent but tightly edited.

Use only a small set of categories:
- Drinks
- Food
- Happy Hour
- Music
- Shops
- Indoor
- Late
- History

Use photography and sharp, one-line local editorial copy. Avoid star ratings, review
counts, user-posted captions, and generic tourist copy.

#### Place Detail

Each location gets:
- One strong image
- Name and category
- Open/closed status
- Walk time from hotel
- One concise reason to go
- "Good for" tags: first drink, date, solo, cheap, rainy, late, etc.
- Simple practical notes: dress expectation, cash/card, reservations, accessibility where
  known
- **Go now** button
- **Add to tonight** button
- **Next nearby** recommendation

#### Lantern List

Saved places should feel like a short personal shortlist, not a social profile.

Sections:
- Tonight
- Later
- Next trip

No public sharing mechanics are needed for MVP.

### Persona Layer

Keep the existing persona idea, but treat it as a **tone and ranking layer**, not an
elaborate character universe.

Each persona sees the same places, but with different ordering and language:

| Mode | Job | Example copy |
|---|---|---|
| Straight | Fast, practical, no-nonsense | "Strong cocktails. Eight-minute walk. Go before 9." |
| Atmospheric | More sensory and editorial | "A low-lit first drink before the street gets theatrical." |
| Local Friend | Warm, candid, lightly opinionated | "Tourists miss this because it doesn't yell at them from the sidewalk." |

The UI should never become costume-like or mystical. These are service modes, not avatars.
The concept was designed to use persona themes to filter and reorder the same curated
content, which keeps the build simple and the content system maintainable.

### Aesthetic Direction

Lamplight should feel like a **very good independent magazine became a night concierge**:
confident, spare, contemporary, slightly nocturnal, and practical.

#### Visual rules

- Near-black background, not pure black: charcoal, ink, or warm-black.
- Use **amber #FFC24B** as the only bright accent.
- Use off-white or very pale gray for primary text.
- Use muted gray for secondary labels and metadata.
- Use hairline divider rules instead of boxes, shadows, or excessive cards.
- Use flat surfaces and edge-to-edge photography.
- Avoid rounded cards; use square or minimally softened corners only.
- Keep generous whitespace and large type.
- Make the interface feel calm, not busy.

The locked core identity is near-black surfaces, hairline dividers, no rounded corners,
amber as the single accent color, and the Four Panes lantern mark.

#### Type

Use:
- **Uncut Sans** for display headlines, venue names, and primary calls to action.
- **Martian Mono** for functional labels: distance, category, time, status, coordinates,
  "OPEN," and directional detail.

A useful hierarchy:
- Big venue name
- Small monospaced utility line
- One strong editorial sentence
- One dominant action button

Avoid script, retro "New Orleans" display type, ornamental serif type, and anything that
looks haunted-house themed.

#### Photography

Photography is where the Pao influence is welcome.

Prioritize:
- Warm interior lighting
- Details that prove a place has texture: a bar top, a menu, a doorway, a drink, a stage, a
  neon reflection
- Real people only when candid and not stock-photo perfect
- Editorial cropping, grain in moderation, realistic colors

Avoid:
- Generic Bourbon Street party imagery
- Beads, masks, fleur-de-lis, swamp imagery, voodoo props, fake gaslamp romanticism
- Overly filtered "dark academia" images
- Anything that implies the city is an amusement-park version of itself

The brand must explicitly avoid Mardi Gras purple/green/gold, tarot, skulls, Spanish moss,
wrought iron, fog, and other stereotypical New Orleans visual shortcuts.

### Logo Behavior

Use the **Four Panes** lantern mark as a functional symbol, not decorative branding.

- Top-left: compact app mark/home control.
- Persistent bottom navigation: the lit pane indicates current section.
- Hotel anchor: the lantern icon means "home base."
- "Take me back" uses the lantern as a directional return symbol.
- Loading state: one pane illuminates at a time, quietly.

Do not draw a literal antique French Quarter street lamp. The mark should feel geometric,
digital, and legible at tiny sizes. The established Four Panes concept reduces a tapered
lantern to a 2x2 pane grid that can also signal the app's core content areas.

### Explicitly Do Not Build

Do not include:
- User accounts or public profiles
- Followers, likes, comments, or messaging
- User-uploaded locations or photos
- Ratings, reviews, or review counts
- A global/multi-city database
- Infinite scrolling as the primary experience
- Booking, payments, subscriptions, or business dashboards in the MVP
- Full turn-by-turn navigation inside the app
- AI chat as the home screen
- Tarot, mystical-card, witchy, or theme-park New Orleans visuals

### Build Priority

1. Hotel selection and saved **Home Lantern**
2. Home screen with one excellent "next move"
3. Place cards and place-detail view
4. "Tonight" three-to-four-stop loop
5. Apple/Google Maps handoff, including **Take me back**
6. Discover categories, especially **Happy Hour**
7. Persona copy/ranking layer
8. Lantern List

**Success test:** A hotel guest who has no plan can open Lamplight, choose a hotel, get
somewhere genuinely good within ten minutes, and easily find their way back afterward.

---

## Brief 1 (shelved): Pricing + Concierge System

Kept verbatim in case paid tiers get revisited later. Not currently being built against.

### Product principle

Lamplight has two inputs that shape every recommendation:

1. **Who are you moving with?** -- group size and group type
2. **What kind of time do you want?** -- itinerary vibe / concierge mode

These inputs should be quick, optional, and editable at any time. Never make users fill out
a lengthy travel-planner questionnaire.

The app should ask on first open:

> **Who's out tonight?**
> Solo · 2-4 people · 5+ people

Then:

> **What are we in the mood for?**
> Choose a vibe, not a schedule.

The hotel is saved as the **Home Lantern**, so recommendations can be built around a
realistic starting point and an easy return route.

### The three tiers

| Tier | Best for | Price concept | Core promise |
|---|---|---:|---|
| **Lamplight Free** | Solo travelers and groups of 2-4 | Free | "Never be stuck wondering what to do next." |
| **One Night** | A date, one great evening, a single special outing | One-time purchase, about $4.99-$7.99 | "Give us tonight. We'll make it make sense." |
| **One Week** | Hotel guests, long weekends, families, conferences, groups of 5+ | One-time stay pass, about $14.99-$24.99 | "A local concierge in your pocket for the whole stay." |

Avoid monthly subscriptions at launch. Visitors are in town for a short, defined window; a
24-hour and seven-day pass matches how they already think about a trip.

### Lamplight Free

Free should feel genuinely useful, fast, and safe. It exists to earn trust, build
hotel-partner adoption, and prove that Lamplight is better than opening Google Maps and
becoming overwhelmed.

Available to everyone:

- Save a hotel as the **Home Lantern**
- One-tap **Take me back** handoff to Apple Maps or Google Maps
- Current "Your next good move" recommendation
- Basic discovery by Drinks, Food, Happy Hour, Music, Shops, Indoor, Late, and History
- Curated place pages with walk time, open status, practical notes, and one editorial reason
  to go
- Basic hotel-to-stop walk-time context
- Save places to a Lantern List
- One simple 2-3 stop loop per day
- A small number of editorial "Insider Notes"
- Essential utility layer: restrooms, water, Wi-Fi, late food, accessibility notes,
  emergency practical guidance, and offline-ready saved addresses
- Vibe selection and switching

Free is for **solo travelers and groups of 2-4**. It should still work beautifully for them
because their decisions usually require taste and orientation, not complex coordination.

Free paywall moment -- do not gate the first useful result. Instead, let users reach the
moment where planning becomes complex:

> **Want Lamplight to plan the whole night?**
> Build a paced route, keep the group together, and know when to head home.
> **Unlock One Night**

For a group of 5+, show:

> **Groups this size need a little more choreography.**
> Unlock a group-ready plan with seated options, timing, practical stops, and a route that
> keeps everyone in the same orbit.

This frames payment as reduced hassle, not withheld information.

### One Night

**One Night** is the impulse buy. It should be for people who want the feeling of having a
plugged-in local friend plan their evening without losing spontaneity.

Unlocks:

- Full evening itinerary: typically 3-5 stops
- Time-aware pacing: first drink, dinner, music, late stop, return point
- Re-roll / "change the mood" without losing the entire plan
- Concierge rationale at each stop: why it fits *this* night
- Multiple stop timing suggestions: "Go now," "Better after 9," "Make this your final stop"
- Enhanced Insider Notes and two local alternatives per key stop
- A "rescue the night" tool: **Too loud / too full / too expensive / raining / we're tired /
  need food**
- A polished shareable itinerary link for a friend or partner
- Home Lantern return guidance and one suggested final stop on the walk back
- Best for solo, couples, and groups of 2-4

Suggested price:

- **$5.99** as the default
- **$7.99** on convention/festival nights or high-demand weekends, only if the experience is
  meaningfully enhanced with current practical information
- Include a friendly hotel-partner code, such as "Your hotel unlocked tonight's plan."

### One Week

**One Week** is the hotel-stay pass and the tier that supports family, business, and
large-group complexity. This is where Lamplight shifts from a discovery app into a
lightweight digital concierge.

Unlocks everything in One Night, plus:

- Seven days of itinerary generation and re-planning
- Daily "next good move" recommendations from the Home Lantern
- Multiple saved plans: morning, afternoon, dinner, rainy day, late night, departure day
- Full concierge library: dining, drinks, music, shopping, history, indoor refuge, family
  options, accessibility-aware suggestions, and practical services
- Hotel-aware timing: "You have 75 minutes before your reservation," "This is on your walk
  back," "Leave the Quarter by 8:20 to make your show"
- Multiple itineraries for different group moments: a parents' afternoon, a friends' night
  out, a business dinner, a couples' night
- More Insider Notes and locally specific "avoid this mistake" guidance
- Group coordination: a shareable plan, meeting point, start time, total walking estimate,
  and easy location handoff
- Large-group filters: seated, reservations recommended, quieter, easy bathrooms,
  low-walking, family-friendly, private-room inquiry, and open-late
- Concierge support prompt or hotel desk handoff for requests the app cannot solve

Suggested price:

- **$18.99** as the regular price
- **$14.99** through a hotel partner code
- **Included** for a hotel's premium room package, suite package, group booking, or
  direct-booking promotion

### Group-size axis

| Group size | Tier access | Planning approach | Things Lamplight prioritizes |
|---|---|---|---|
| **Solo** | Free, One Night, One Week | Confidence and low friction | Welcoming rooms, bar seating, low-pressure activities, easy walk back, clear practical notes |
| **2-4 people** | Free, One Night, One Week | Choice reduction and momentum | A flexible 2-4 stop sequence, mood fit, wait-time sensitivity, shared saved list |
| **5-8 people** | One Night or One Week | Coordination and reduced failure points | Seating, reservations, wider space, realistic timing, meeting point, bathroom access, walkability |
| **9+ people** | One Week or hotel/group partner access | Group itinerary / concierge request | Private or semi-private options, restaurant inquiry routing, transportation handoff, staggered arrivals, a simple host-facing plan |

For groups over 8, offer a clean inquiry handoff rather than promising automatic
reservation handling:

> **This needs a human touch.**
> Send your group details to a partner concierge or venue contact.

### Vibe concierge axis

Vibe is the emotional and practical filter. It is free to select because it makes the first
recommendation feel personal; the paid tiers unlock deeper, time-aware itineraries built
around it.

| Vibe | What Lamplight optimizes for | Sample app language |
|---|---|---|
| **Romantic** | Good lighting, a slower pace, strong drinks, intimate tables, a graceful walk back | "A night with a little architecture." |
| **Curious** | History, unusual shops, local detail, small museums, strange corners, smart conversation | "Follow the interesting thing." |
| **Early to Bed** | Earlier dinner, short walks, calm rooms, low noise, clear return timing | "A good night, still in bed by ten." |
| **Night Owl** | Late kitchens, live music, durable energy, late open hours, a final-stop plan | "No need to call it yet." |
| **Easygoing** | Low-stakes, no-reservation, casual, flexible, budget-aware | "No ceremony. Just a good time." |
| **Food First** | Meal timing, reservation guidance, walkable drinks before/after, dietary notes | "The plan begins with dinner." |
| **Cocktails First** | Great bars, pacing, not-too-loud options, food nearby, a safe final move | "Start properly." |
| **Music Tonight** | Set times, venue tone, proximity, food nearby, exit plan | "Let the set decide the night." |
| **History, Not Hokum** | Specific local history, architecture, museums, real people and place stories | "The good stories are documented." |
| **Rain Plan** | Indoor stops, covered routes, short walks, museums, food, hotel proximity | "Stay dry without staying in." |
| **Treat Us Well** | Polished service, reservations, better drinks, calmer rooms, memorable experience | "A little more considered." |
| **On a Budget** | Happy hours, inexpensive food, free culture, efficient walking | "Spend it where it counts." |
| **Business-Safe** | Reliable service, good conversation volume, receipts, seating, punctuality, polished but not stiff | "Good judgment, no corporate sheen." |
| **Family-Friendly** | Earlier hours, kid-friendly food, bathrooms, shade/indoor options, low-stress stops | "Everybody gets to enjoy it." |
| **Low Walking** | Short distances, accessible entries where known, rest points, ride-share handoffs | "See more, walk less." |
| **First Time Here** | Essential Quarter experiences, balanced with non-obvious stops, orientation | "The classics, edited properly." |

### Tier screen design

- Background: near-black.
- Amber is reserved for the selected tier and the primary purchase button.
- Use hairline dividers; no rounded pricing cards.
- Make **One Night** the visually centered recommendation.
- Use Martian Mono for duration and pricing labels: `24 HOURS · $5.99` and `7 DAYS ·
  $18.99`.
- Use Uncut Sans for headlines and benefit statements.
- Include the Four Panes lantern at the top, with one pane lit for Free, two for One Night,
  and all four panes lit for One Week.

### North Star Protocol (superseded by the Home Lantern feature above)

The original framing of the hotel-return feature, kept for the implementation detail it
specifies:

> A persistent, high-visibility extraction utility designed to override decision fatigue
> and disorientation.
>
> UI Trigger: a dedicated, single-tap floating action button (FAB) accessible from any
> screen in the application.
>
> Execution logic: capture the user's real-time GPS coordinate; pull the user's predefined
> basecamp coordinate stored locally on device initialization; call a pedestrian routing API
> set strictly to walking mode; suppress all discovery pins and render a route directly to
> the basecamp.

Lamplight's implementation renders this as a Maps handoff (Google Maps walking-directions
intent) rather than an in-app rendered polyline, per Brief 2's "Explicitly Do Not Build:
full turn-by-turn navigation inside the app."

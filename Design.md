# ApexTracker — Design

The specification for how this app looks and behaves visually: the token values, the
measurements behind them, and the reasoning that is not recoverable from the code.

> **⚠️ IDENTITY CHANGE — GRAPHITE (2026-07-30).** The Ember identity below (warm near-black +
> terracotta accent + Instrument Serif) has been **replaced** by GRAPHITE: a cold monochrome
> with a mono display voice and no accent hue (Plan.md Phase 2, superseding issue #139). The
> current authoritative values are in **[§0. Graphite](#0-graphite-current-identity)** immediately
> below. The rest of this document — decision log, type tables, palette tables, chart spec — still
> describes Ember and is retained for the reasoning and the trail; where §0 and a later section
> disagree on a value, **§0 wins**. Decisions 1–12 are annotated with what §0 supersedes. A full
> table-by-table rewrite is deferred (a deliberately un-run task, not an oversight — see the
> doc-drift warning above; the code in `ui/design/` is the tiebreaker of last resort).

## 0. Graphite (current identity)

The cold-monochrome identity. Settled in a grill session (Plan.md, thirteen decisions) and built
+ verified on a real Samsung SM-S931U1 in both themes and at 200% font, 2026-07-30.

**Why.** The owner's verdict on Ember: warm-dark + terracotta + serif reads as Claude's own
surfaces. Warmth is Claude's territory, so the move away is *cold* graphite; the serif screamed
loudest, so the display voice becomes a mono. With no accent, the app can only rest on the axes a
generic look doesn't occupy — the mono voice, density, and the heatmap.

**Type — `ui/design/ApexType.kt`.** Two faces (the serif trio is retired):
- **Martian Mono** (Regular, Medium) — the voice: all display + headline slots AND every
  `ApexNumerals` figure. Chosen over IBM Plex Mono and Space Mono on the style plate; it is the
  only one of the three drawn as a display face and no major consumer app owns it. It is *wide*,
  so display sizes run smaller/tighter than the old serif scale: display 33/27/22, headline
  20/17/16 sp, all negative-tracked. Numerals: hero 42, large 23, medium 13, small 10.
- **Geist** (Regular/Medium/SemiBold) — titles, body, labels, all running text.

**Colour — `ui/design/ApexPalette.kt`.** No accent. `primary` is ink (dark `Frost #E9EBEE`,
light `Char #191C20`) — filled buttons/FAB/nav are inverse blocks. Only hues are the semantics:
`Sage` (dark `#6FA88C` / light `#3F7A62`, met/positive) and `Crimson` (`error`; dark `#DC3D57` /
light `#AE1F38`, over/failed), and a negative state always also carries an icon or word.
- Dark surfaces: base `#0E0F11`, surface `#16181B`, raised `#1D2024`, elevated `#262A2F`,
  hairline `#5E656E`, faint `#2C3036`, text `#E9EBEE` / dim `#9AA1A9`.
- Light surfaces: base `#F3F4F6`, surface `#FFFFFF`, raised `#E9EBEE`, elevated `#DFE2E6`,
  hairline `#848B95`, faint `#D9DDE2`, text `#191C20` / dim `#575E66`.
- Measured on the plate (both bumped to clear the 3:1 floor): body-on-bg 16.1:1 dark, hairline
  3.2:1 dark / 3.1:1 light; Sage 7.0:1 dark / 4.6:1 light, Crimson 4.4:1 / 6.3:1.

**The heatmap is now fill-height BARS, not coloured squares** (`DashboardView.HeatCell`). A gray
ramp has less resolution than ember had, so intensity is carried by *geometry*: each cell is a
bar bottom-anchored in a fixed slot whose height = the fraction of the day's goals met; untracked
= empty slot, tracked-none-met = a 2px baseline stub, perfect = the slot filled solid in ink.
Opacity rises with height too (double-encoding for AMOLED at ~20dp). This is the signature and the
thing that stops the grid reading as GitHub's. The gray `heatRamp` survives in `ApexSemantics`
only for the Glance widgets. `ApexSemantics` gained `heatInk`/`heatSlot` for the bars.

**`ApexTheme` enum entry is now `GRAPHITE`** (was `EMBER`). Legacy persisted names fail `valueOf`
and fall back to default, which is correct. The Glance widgets (`GoalsWidget`/`StreakWidget`)
carry a hand-kept mirror of the graphite dark hexes — a met goal keeps Sage, everything else is
ink on graphite.

**Status.** Foundation (palette/type/tokens/heatmap/widgets) swapped globally and audited
on-device across all primary screens — no screen's hierarchy collapsed without the accent, so no
per-screen rescue work was needed (the ink primary + mono headlines carry it). Screenshot
baselines re-recorded. `Design.md`'s Ember tables (§2 onward) are the deferred rewrite.

### The container ladder — which M3 slot gets which tone

M3 has five container steps and an inverse trio; this design has four tones. They are mapped by
**role**, not by stretching four tones across five slots, so several steps deliberately share a
value. Both schemes define all of them (see below — this is where the 2026-07-30 audit landed).

| M3 slot | Reached by | Dark | Light |
|---|---|---|---|
| `surfaceContainerLowest` | — | `GraphiteBase` | `PaperBase` |
| `surfaceContainerLow` | `ModalBottomSheet` | `GraphiteSurface` | `PaperSurface` |
| `surfaceContainer` | `DropdownMenu`, scrolled `TopAppBar`, `NavigationBar` | `GraphiteElevated` | `PaperElevated` |
| `surfaceContainerHigh` | `AlertDialog`, `DatePickerDialog`, `TimePicker` | `GraphiteElevated` | `PaperElevated` |
| `surfaceContainerHighest` | `Card` | `GraphiteElevated` | `PaperElevated` |
| `surfaceDim` / `surfaceBright` | — | `GraphiteBase` / `GraphiteElevated` | `PaperRaised` / `PaperSurface` |
| `scrim` | `ModalBottomSheet` | `GraphiteBase` | `GraphiteBase` |
| `inverseSurface` | `Snackbar` container | `PaperRaised` | `GraphiteRaised` |
| `inverseOnSurface` | `Snackbar` text | `Char` | `Frost` |
| `inversePrimary` | `Snackbar` action label | `Char` | `Frost` |

Three of these are load-bearing:

**`surfaceTint` is `Color.Transparent`.** M3 composites it over `surface` at elevation, so any
non-transparent value drifts every raised surface toward ink. This design layers with authored
tones and hairlines instead, so the tint must contribute nothing.

**The snackbar is the app's only inverted surface.** The dark theme's snackbar is a *light*
surface (paper tones); the light theme's is a *dark* one (graphite tones). Left undefined, its
action label rendered Material's `Primary40` — purple. Because graphite has no accent, `primary`
and `onSurface` are the same value in every theme (both are just "ink"), so `inverseOnSurface` and
`inversePrimary` land on the same tone within each theme — that is correct, not a shortcut.

**Menus and dialogs share one tone** because the palette assigns `GraphiteElevated`/`PaperElevated`
to "menus, dialogs" as a single role. A menu opening *inside* a dialog (`RecurrencePickerDialog`,
the budget category dropdown) therefore has no tonal edge and separates on a hairline instead —
`apexMenuBorder()`, passed at every `ExposedDropdownMenu` call site. **Do not drop that border
while these two slots share a tone**; the menu becomes invisible against its host.

#### Audit: six undefined slots, found 2026-07-30

Resolving each M3 component's default `*Tokens.ContainerColor` back to its `ColorSchemeKeyTokens`
(rather than reading call sites — a call site that mentions no colour is exactly the one that
reaches an undefined slot) turned up six holes beyond the already-known `tertiaryContainer` one:

| Slot | Reached by | Was rendering |
|---|---|---|
| `surfaceContainer` | 4 `ExposedDropdownMenu`s, scrolled `TopAppBar` | baseline lavender — the reported off-palette dropdown |
| `surfaceContainerHigh` | **19 `AlertDialog`s and 4 `DatePickerDialog`s — not one overrode it** | baseline purple-tinted |
| `surfaceContainerLow` | the one `ModalBottomSheet` that set no `containerColor` (Dashboard day detail) | baseline |
| `inverseSurface`/`inverseOnSurface`/`inversePrimary` | 3 `SnackbarHost`s, all reachable | baseline — the action label was Material `Primary40/80` **purple** |
| `scrim` | 3 `ModalBottomSheet`s | baseline `#000` — benign, but undeclared |
| `surfaceTint` | anything at nonzero elevation | Material's default tint, drifting elevated surfaces off-palette |

This audit was written against the pre-graphite Ember palette and ported onto graphite's names
the same day it was found — the graphite rewrite above had carried over only the already-known
`tertiaryContainer` fix and reopened the rest of this exact hole under new names. That asymmetry
is itself the lesson: **when the palette is rewritten, re-run this audit; don't assume a prior
fix survives a rename.**

Two things guard it now:

- **`ApexPaletteSlotsTest`** asserts every component-default slot in both schemes is a token from
  the table above, that the two schemes define the same set, and that the snackbar actually
  inverts. Host-side and deterministic — no device needed.
- **The style plate's "Component surfaces — undefined-slot detector"** reads from `MenuDefaults`,
  `AlertDialogDefaults`, `SnackbarDefaults` and friends rather than from named slots, so it takes
  the same path the real component does. A tile whose hex isn't in the table above is a bug you
  can see.

**When adopting a new M3 component type, add its slots to both schemes and to the test.**

---

**Status (Ember-era, retained).** Foundation plus **all eight screens** shipped 2026-07-29. The
bottom bar's large-font behaviour and the category palette — the two things §8 was holding open —
are both resolved. What remains untouched: the settings sheets, `CalendarGrid`, and the
dialogs/editors, left out of the per-screen PRs deliberately to keep diffs reviewable. Per-screen
state is tracked in [Screen inventory](#screen-inventory).

**Relationship to the skill.** `.claude/skills/android-product-design/SKILL.md` is the *enforcing*
document — the rules and bans an agent applies while building. This file is the *reference* — the
values, the numbers, and the why. When a value appears in both, this file is authoritative; when
they disagree on a rule, the skill is.

**How to change this document.** Design decisions are settled on a device, not in a paragraph. If
you want to change a value here, change it on the style plate first
(`adb shell am start -n com.example.apextracker/com.example.apextracker.design.StylePlateActivity`),
look at it on a real panel in both themes, then update this file with what you measured. Several
values below are on their second or third revision precisely because the first one looked fine in
a hex editor and wrong on an AMOLED screen.

---

## 1. Decision log

Twelve decisions, settled deliberately. They are recorded here so they don't get
re-litigated by whoever (or whatever) touches this next. **Decisions 1–3, 12 are superseded by
§0 (Graphite, 2026-07-30)** — the rest still hold (motion tokens, hand-drawn charts, token
objects, IA frozen, one-screen-per-PR, style-plate-first, shapes deferred all carried over
unchanged). Kept for the reasoning trail, not as current law.

| # | Decision | Why |
|---|---|---|
| 1 | **Axis: Spotify structure + Claude typography** | Dark-first, dense, tight rhythm, motion present. Editorial display type, severe colour restraint. When they conflict: density wins on data screens, restraint wins on chrome. |
| 2 | **Instrument Serif + Geist + Geist Mono** | The app had `FontFamily.Default` on all 13 styles and no `res/font/` at all — no typographic identity whatsoever. Three faces, three jobs. |
| 3 | **One accent: Ember** | Four interchangeable accents (EMERALD/OCEAN/MAGMA/ROYAL) is customisation theatre; four accents means no identity. `RoyalPrimary` was literally Material Design 2 Purple 500. |
| 4 | **Hand-authored dark *and* light. No Dynamic Color** | Light mode was generated by `shiftColorForLightMode()` — an HSV multiply (sat ×1.3, val ×0.75) nobody had looked at against real content. Material You would hand the identity to the wallpaper. |
| 5 | **Stable material3 1.4.0; own motion tokens** | M3 Expressive's motion/shape APIs live only in 1.5.0-alpha. Alpha churn is not acceptable on an app holding real budget and study data — and adopting Google's motion personality wholesale is a different flavour of generic. |
| 6 | **Charts hand-drawn, spec'd** | Vico charts look like Vico charts. Hand-drawn ones can look like this app's. |
| 7 | **Token objects + component library** | Not the Compose Styles API (experimental, contradicts #5), and not "MaterialTheme + discipline" — the repo already proved discipline-without-enforcement fails: `Type.kt` documented a convention that 66 inline `fontWeight` overrides ignored. |
| 8 | **Visual + layout; IA frozen** | The stacked-rounded-cards shape is most of what read as generated, and no amount of font-swapping fixes it. But routes, nav, ViewModels and the data layer stay untouched — this is a working app. |
| 9 | **Foundation global, then one screen per PR** | Type + colour is ~60% of the win and it is global, so every screen improves the day it lands. Each screen after that is independently reviewable and revertible. |
| 10 | **Style plate before spec** | Writing the spec first would have locked in five values that turned out to be wrong on a real panel. See [§9](#9-what-the-device-changed). |
| 11 | **`graphics-shapes` deferred, not adopted** | Stable and wanted, but there is no use site until a screen needs shape morphing. An unused dependency is worse than a late one. |
| 12 | **A second identity stays possible** | `ApexTheme` remains an enum with one entry (`EMBER`) rather than being inlined, so a fully-authored monochrome + semantic-only theme is a drop-in. |

### The constraint that matters most

*(Superseded by §0 — the Ember framing below is inverted under Graphite and kept only for the
reasoning. Under Graphite there is no accent to rest on, so the constraint is sharper, not softer:
strip the screen to grayscale — it already is — and if it reads as an un-styled wireframe rather
than a deliberate instrument, the hierarchy is doing too little work.)*

This palette — warm near-black, a terracotta accent, a high-contrast serif display, a warm cream
light mode — sits on top of **two of the three looks AI-generated design currently defaults to**
(see `frontend-design`, which names them). That was an informed choice and the palette is not up
for renegotiation. The consequence is non-negotiable though:

> **The palette cannot be what makes this app distinctive.**

Distinctiveness has to come from the axes that default look does not occupy:

- **Tabular mono numerals as a primary visual element.** The default look is serif + sans. A
  tracker is mostly numbers; here they are monospaced, aligned, and given real size. *(Graphite
  goes further: the display voice is mono too, not just the numerals.)*
- **Density.** The default look is airy and editorial. This is dense and instrumental.
- **The heatmap is the signature** — the one place to spend boldness, and it must not read as
  GitHub's contribution graph. *(Graphite makes it fill-height bars — see §0.)*
- **Shape language.** The default look has no shape vocabulary at all.

---

## 2. Type

Three faces, three jobs. Bundled as static instances in `res/font/`, OFL text in
`assets/licenses/`. 820KB total.

Geist ships as static weights rather than its variable file on purpose: setting a `wght` axis in
Compose requires `FontVariation.Settings`, which is `@ExperimentalTextApi` in **every** overload.
Statics cost ~340KB more and need no opt-in, which is the trade decision #5 implies.

| Face | Job | Shipped weights |
|---|---|---|
| **Instrument Serif** | Display only, ≥20sp | Regular, Italic (one weight exists — never fake bold) |
| **Geist** | All UI text, labels, body | Regular, Medium, SemiBold |
| **Geist Mono** | Every number a user reads as a quantity | Regular, Medium |

### Scale

`ApexTypography` in `ui/design/ApexType.kt`. Display slots are the serif; everything else is Geist.

| Token | Face | Size | Line | Tracking | Weight |
|---|---|---|---|---|---|
| `displayLarge` | Serif | 44 | 48 | −0.5 | Regular |
| `displayMedium` | Serif | 34 | 40 | −0.25 | Regular |
| `displaySmall` | Serif | 27 | 34 | 0 | Regular |
| `headlineLarge` | Serif | 24 | 30 | 0 | Regular |
| `headlineMedium` | Serif | 21 | 27 | 0 | Regular |
| `headlineSmall` | Serif | 20 | 26 | 0 | Regular |
| `titleLarge` | Geist | 19 | 25 | −0.2 | Medium |
| `titleMedium` | Geist | 15 | 21 | 0 | SemiBold |
| `titleSmall` | Geist | 11 | 16 | **+1.4** | Medium |
| `bodyLarge` | Geist | 16 | 23 | 0 | Regular |
| `bodyMedium` | Geist | 14 | 20 | 0 | Regular |
| `bodySmall` | Geist | 12 | 17 | +0.1 | Regular |
| `labelLarge` | Geist | 14 | 19 | 0 | Medium |
| `labelMedium` | Geist | 12 | 16 | +0.2 | Medium |
| `labelSmall` | Geist | 11 | 15 | +0.3 | Medium |

`titleSmall` is the ALL-CAPS section eyebrow. It replaced a `FontWeight.Black` + 2sp-tracking
treatment: the serif now carries the emphasis that weight used to, so the eyebrow can go quiet and
label rather than shout.

### Numerals

`ApexNumerals` — deliberately **not** part of the Material type scale, because Material has no slot
meaning "this is a quantity", and routing numbers through `bodyLarge` is exactly how they end up in
the wrong face.

| Token | Size | Use |
|---|---|---|
| `hero` | 52 / Medium | The stopwatch. At most one per screen. |
| `large` | 26 / Medium | A headline statistic — today's total, month spend. |
| `medium` | 15 / Regular | Values in list rows. The workhorse. |
| `small` | 11 / Regular | Chart axis labels, dense secondary figures. |

Geist Mono is monospaced, so figures are inherently tabular — no font-feature setting needed. This
is not cosmetic: with proportional figures, a running stopwatch physically jitters as digits change
and a currency column never aligns.

### Rules

- Read type from `MaterialTheme.typography` or `ApexNumerals`. An inline `fontWeight =`,
  `fontSize =`, or `letterSpacing =` at a call site is a bug unless commented as a deliberate one-off.
- Instrument Serif never below 20sp, never on a control label, never faux-bolded.
- Every quantity in Geist Mono. Every one.

---

## 3. Colour

Two hand-authored palettes. Neither derives from the other.

The darks are deliberately **warm** near-blacks rather than the conventional cool `#121212`: a cool
grey under a warm terracotta accent reads as two unrelated decisions. Warming the substrate is most
of what makes the accent look chosen rather than dropped on.

### Dark

| Token | Hex | Role |
|---|---|---|
| `InkBase` | `#131210` | App background |
| `InkSurface` | `#1A1816` | Resting surface |
| `InkRaised` | `#221F1C` | Grouped content, sheet body |
| `InkElevated` | `#2B2724` | Menus, dialogs, pressed states |
| `InkLine` | `#5A544F` | Hairline dividers, borders — **never text** |
| `InkLineFaint` | `#332E2B` | Separation inside a group |
| `Bone` | `#EDE8E2` | Primary text — warm off-white, never pure white |
| `BoneDim` | `#A09890` | Secondary text, axis labels |

### Light

| Token | Hex | Role |
|---|---|---|
| `PaperBase` | `#F5F2ED` | App background |
| `PaperSurface` | `#FFFDFA` | Resting surface |
| `PaperRaised` | `#EBE6DE` | Grouped content |
| `PaperElevated` | `#E2DCD2` | Menus, dialogs |
| `PaperLine` | `#B3AA9A` | Hairlines — **never text** |
| `PaperLineFaint` | `#DCD5C9` | Separation inside a group |
| `Sable` | `#1B1815` | Primary text |
| `SableDim` | `#5F574E` | Secondary text |

### Semantics

| Role | Dark | Light | Meaning |
|---|---|---|---|
| **Ember** | `#D9613C` | `#B84A28` | The one brand colour. Emphasis and attention. **Never "good", never "bad".** |
| **Sage** | `#6FA88C` | `#3F7A62` | Met / under / on track. |
| **Alarm** | `#DC3D57` | `#AE1F38` | Over / error / missed. |

Two things about this triad are load-bearing:

**Alarm is crimson, not red.** The first pass used `#E0574E`, ~15° from Ember. On device, "Missed"
and "12 days" were marginal in dark and *literally the same colour* in light. Crimson separates on
hue **and** value, which is what makes it survive both themes. **Do not warm Alarm toward Ember** —
that regression degrades every over-budget and missed-goal state at once, silently, because nothing
crashes.

**Sage is not in the `ColorScheme`.** It lives only in `ApexSemantics.positive`. It was originally
`secondary`/`secondaryContainer`, which meant every M3 component defaulting to secondary — a
selected `FilterChip` most visibly — rendered "goal met" green for things with nothing to do with
goals. `secondary` is now a desaturated Ember: with one accent, secondary is a *variant* of it, not
a new hue.

Negative states are additionally always carried by an icon or a word, never by hue alone.

### Measured contrast

Computed on device against the real rendered palette, not from a spreadsheet. The style plate
prints these live.

| Pair | Dark | Light | Floor |
|---|---|---|---|
| Body text on background | **15.4:1** | **15.8:1** | ≥4.5 |
| Secondary text on background | **6.6:1** | **6.4:1** | ≥4.5 |
| Accent on background | **5.1:1** | **4.6:1** | ≥4.5 |
| Hairline on background | **2.5:1** | **2.1:1** | see below |

**`outline` is for borders, dividers and strokes only — never for text or for a meaningful icon.**
Raising it to hairline visibility made it a ~2.5:1 tone, and the app had 51 places using it as a text
or tint colour, all of which silently became sub-4.5:1 body text. Secondary text is
`onSurfaceVariant` (6.6:1 dark / 6.4:1 light). This is the one palette rule that regressed
accessibility across eleven files at once, so it is worth stating twice.

Hairlines started at 1.6:1 dark / 1.4:1 light — invisible. Since hairlines are what *replaces*
cards in this design, that silently broke the whole structural strategy, so they were raised. They
still sit under 3:1, and that is deliberate: hitting 3:1 on `#F5F2ED` needs roughly `#8A8175`,
which stops being a hairline and becomes a heavy rule. WCAG's 3:1 non-text requirement governs
interactive components and meaningful graphics, not dividers. **Accent on background in light mode
is 4.6:1 — only 0.1 above the floor.** Do not lighten `EmberLight`.

### Heatmap ramp

The signature surface, so it gets an explicit six-step scale rather than alpha-modulating the accent
at the call site.

| Bucket | Dark | Light | Meaning |
|---|---|---|---|
| `−1` | `#211F1E` | `#E7E4DF` | Untracked — no goals active that day |
| `0` | `#3B2C24` | `#DCCCBD` | Tracked, none met |
| `1` | `#6B3524` | `#EFC0A6` | |
| `2` | `#9A462A` | `#E29370` | |
| `3` | `#C05733` | `#C96844` | |
| `4` | `#EE7A4E` | `#A33C1C` | Perfect day |

Three things this encodes, all learned the hard way:

1. **It is not `primary` at 10/35/55/78/100% alpha.** That was the original. Alpha over a dark
   background compresses the low end so hard the grid nearly disappeared, and the middle steps did
   not separate from each other.
2. **Untracked and tracked-but-empty differ by *hue*, not visibility** — untracked is a cool
   neutral, tracked-at-zero is warm. An earlier fix made untracked near-background so it could not
   be mistaken for a failed day; that overcorrected and erased the grid's structure, so a user with
   little history saw a blank panel instead of the shape of their year. Both must be visible.
3. **Bucket 4 is brighter than the accent itself**, so a perfect day reads as a peak rather than as
   "the same colour as a button".

Only a perfect day hits bucket 4. A day with no active goals is a `null` fraction — an empty cell,
which is not the same thing as a 0.0 missed day.

---

## 4. Spacing, shape, motion

### Spacing — `ApexSpacing`

`hairline 2` · `xs 4` · `s 8` · `m 12` · `l 16` · `xl 24` · `xxl 40` (dp)

Six steps plus an optical nudge. The previous set was 1, 2, 4, 8, 12, 16, 20, 24 and 32dp chosen
per call site — that is what "no rhythm" looks like in practice. **A raw `.dp` literal in a layout
modifier is a bug.**

### Shape — `ApexShapes`

`cell 3` · `control 9` · `container 14` · `sheet 26` (dp)

Deliberately much tighter than the old set (16 ×14, 24 ×4, 20 ×3, 12 ×3, plus 28, 50, 4, 3). Heavy
rounding on every container is a large part of the generated-dashboard signature. A fourth radius
needs a written reason.

Two Material components ignore `Shapes` and need the shape passed explicitly — both were caught on
the plate:
- **`Button`** defaults to a full pill.
- **`FilterChip`** defaults to a pill.

### Motion — `ApexMotion`

| Token | Spec | Use |
|---|---|---|
| `snap` | spring, damping .9, stiffness High | Press/release feedback |
| `settle` | spring, damping .75, stiffness MediumLow | A value settling: a bar growing, a row reordering |
| `enter` | tween 280ms, emphasized decelerate | Content arriving |
| `exit` | tween 180ms, emphasized accelerate | Content leaving |
| `emphasis` | spring, damping .55, stiffness Medium | A moment worth noticing — a goal completing, a streak advancing |

`snap` and `settle` are physical (respond to gesture, interruptible). `enter` and `exit` are
choreographic (play once). `emphasis` is the only loose one and is **not a default** — never ambient.

A raw `spring()` or `tween()` at a call site is a bug, for the same reason a raw hex is. `exit` is
shorter than `enter` on purpose: departures should not be dwelt on.

**Decorative perpetual motion is banned.** The reference example is the two counter-rotating rings
that used to sit behind the study timer carrying zero information.

The app currently has **6 animation call sites across 13,603 lines**. Bringing that up is per-screen
work, not foundation work.

---

## 5. Components

`ui/design/ApexComponents.kt`. Screens compose from these rather than assembling Surfaces and Texts
locally — that local assembly is how the app got eight corner radii and 66 inline weight overrides.

| Component | Anatomy | Notes |
|---|---|---|
| `ApexSectionHeader` | ALL-CAPS `titleSmall` in `onSurfaceVariant`, optional trailing slot | **Not the accent.** An accent label on every section spends the accent on structure, leaving nothing to signal what actually matters. |
| `ApexDivider` | 1dp `outlineVariant` | The structural hairline. One weight, one colour. |
| `ApexStatRow` | label + optional supporting + mono value; optional leading slot; press state | The workhorse. `value` is separate from `supporting` so it can be mono — concatenating them forces the whole string into one face. 48dp target via `minimumInteractiveComponentSize()` when tappable. |
| `ApexChartFrame` | eyebrow + plot. No card, no shadow. | |
| `ApexGroup` | `surfaceVariant`, 14dp radius, **explicit `contentColor`** | Use sparingly. |
| `ApexEmptyState` | message + optional action | Requires a message that says what would fill it. |
| `apexMenuBorder()` | 1dp `outline` stroke | Not a component — the `border` every `DropdownMenu`/`ExposedDropdownMenu` must pass. Menus and dialogs share a tone (§0), so this is the only thing separating a menu from the dialog it opens on. `outline`, not `outlineVariant`: the faint hairline vanishes against the elevated tone. |

### The `Surface` dimming trap

`Surface(color = surfaceVariant)` sets `LocalContentColor` to `onSurfaceVariant`, which silently
dims **every** `Text` inside it — including the headline number the card exists to showcase. This
was visible on the style plate: the carded value rendered noticeably greyer than the un-carded one.
`ApexGroup` sets `contentColor` explicitly. Any new container must do the same.

### When a card is allowed

A card must earn itself by being genuinely liftable or tappable **as a unit**. Otherwise use an
eyebrow, a hairline, and vertical rhythm.

The banned pattern is specific: **a vertical run of `Card`s each with a small coloured label at the
top.** That is the single most recognisable generated-dashboard shape and removing it is why this
redesign exists. The app currently has 32 `Card(` call sites.

**Drop shadows on dark surfaces are cargo cult** — `shadowElevation` renders nothing over `#131210`.
The old chart cards were paying for an invisible effect. Use surface-tone layering.

---

## 6. Charts

Five hand-drawn `Canvas` surfaces: budget bar trend, screen-time trend, budget pie, study ring,
heatmap. No chart library (decision #6).

### Encoding rules

- **One axis. Never a dual-axis chart.** Two measures of different scale → two charts or index to a
  common base.
- **Sequential = one hue, light→dark.** The heatmap ramp is the instance. Never a rainbow.
- **Colour follows the entity, never its rank.** A filter that changes the visible category count
  must not repaint the survivors.
- **Text wears text tokens, never the series colour.** Values, labels and axis text stay in
  `onSurface`/`onSurfaceVariant`; a coloured mark beside them carries identity.
- **Status colours are reserved.** Ember/Sage/Alarm never appear as "category 4".

### Marks and axes

| Element | Spec |
|---|---|
| Axis labels | `ApexNumerals.small`, `onSurfaceVariant` |
| Axis ticks | Max and zero only — except duration axes, which take three (see below). Never a full grid. |
| Baseline | 1dp `outline` hairline. No box, no card, no shadow. |
| Bar radius | `ApexShapes.cell` (3dp) |
| Bar fill — current period | `primary` |
| Bar fill — other periods | `ApexSemantics.chartMuted` |
| Line weight | 2dp |
| Markers | ≥8dp |
| Gap between adjacent fills | 2dp of surface |
| Month labels | **Three letters, never truncated** |

That last one is a real bug being fixed, not a style preference:
`BudgetTrends.kt` used `getDisplayName(SHORT, locale).take(1)`, which renders **J F M A M J J A S
O N D** — three of six bars could be labelled "J".

**Why `chartMuted` is an authored token and not `onSurface.copy(alpha = …)`:** a single alpha that
reads correctly on the near-black substrate is almost invisible on paper. The Study chart's
comparison bars looked right in dark and vanished in light until this was split per theme.

### Duration axes

Duration labels go through `durationAxisLabels()` in `DurationFormat.kt`, which picks one unit
(h m / m / s) from the maximum — so a sub-minute week doesn't render three "0m"s.

The y-axis maximum comes from `niceAxisMaxMinutes()`, which rounds **up past** the peak (step
widening with magnitude: 10 / 30 / 60 minutes). Strictly past, so the tallest bar and any target
line are never flush with the top edge where they read as a border; and round, because scaling the
peak by a factor produces maxima like "1h 6m", which tells the reader nothing.

**Duration axes carry three ticks, not two**, via `durationAxisLabels()` fed that rounded maximum.
Three is the one exception to the max-and-zero rule above, and it exists because the helper forces a
single shared unit across the labels — without it a sub-minute week collapsed to three identical
"0m"s (Issue #97). Both duration charts (Study's week, Screen Time's week) use the same treatment;
if you change one, change the other.

### Empty states

Never a blank plot area. Draw the baseline and a label. `maxTotal == 0.0` is a legitimate state, not
an error.

### Categorical palette — APPLIED 2026-07-29

For budget categories. Validated with the `dataviz` validator (not eyeballed), in fixed order:

| # | Hex | | # | Hex |
|---|---|---|---|---|
| 1 | `#C75C8A` | | 5 | `#A0509F` |
| 2 | `#C1832B` | | 6 | `#3FA47C` |
| 3 | `#3E90C4` | | 7 | `#5A62CC` |
| 4 | `#9A5F2A` | | 8 | `#8E9432` |

Dark (`#131210`): **all checks pass** — lightness band, chroma floor, CVD separation (worst adjacent
deutan ΔE **12.9**), normal-vision floor (17.1), contrast ≥3:1.

Light (`#F5F2ED`): passes, with one **non-dismissable WARN** — `#C1832B`, `#3FA47C` and `#8E9432`
fall to 2.76–2.92:1 against paper. That obligates relief: **the pie chart must always carry a legend
with visible text labels.** A legend-less pie violates this spec.

Two things to know before touching this:

- **The order is computed, not aesthetic.** Assigned in sequence, adjacent pairs collapsed under
  deuteranopia — purple and blue are indistinguishable to a deutan viewer, and the worst adjacent
  pair was ΔE 3.3. Re-ordering the *same eight hues* took it to 12.9. Reordering this list breaks
  that; re-run `scripts/validate_palette.js` if you do.
- **Tritan separation is weak** (worst adjacent ΔE 4.0 on the gold↔pink pair). Direct labels are
  load-bearing, not optional garnish.

Assign in fixed order, never cycled. A ninth category folds into "Other" — it is never a generated hue.

Live in `CategoryPalette.kt` as `PALETTE`. Legacy `Category.colorHex` values are mapped onto these
slots on read rather than migrated — see [§8](#8-open-questions) for the mapping, the collapse table,
and what it costs.

---

## 7. The floor

Not stylistic. A screen failing any of these is not done.

- **Touch targets ≥48dp.** Every tappable thing. Where the visual is smaller — heatmap cells,
  colour swatches — the *target* is still 48dp via padding or `minimumInteractiveComponentSize()`.
- **Dynamic type.** All text in `sp`, no fixed-height text containers, layouts survive 200% font
  scale. There is a screenshot baseline at `fontScale = 2.0f`; check it.
- **Both themes correct.** Not "works" — correct.
- **Edge-to-edge.** Insets consumed deliberately. Nothing important under a system bar or the
  gesture handle.
- **Contrast.** Body ≥4.5:1, large text and non-text indicators ≥3:1, against the actual surface
  the element sits on — including every heatmap ramp step.
- **Accessibility.** Every non-decorative element carries a `contentDescription` from
  `strings.xml`; decorative ones take `null`. State is exposed via `semantics`
  (`selectable`/`toggleable`), never implied by colour alone.
- **Four states: loading, empty, error, offline.** "No data" is not an empty state. An empty screen
  is an invitation to act.
- **Localized.** All user-visible strings from `strings.xml`. No concatenation. No `.take(1)`
  truncation of localized names.

---

## 8. Open questions

**~~The category palette cannot just be swapped~~ — resolved 2026-07-29: map on read.** The 24
Google Calendar swatches are gone from the picker, which now offers the eight §6 hues. Stored values
are **never rewritten**: `resolveCategoryHex()` in `CategoryPalette.kt` maps whatever is in Room or
Firestore onto a palette slot at render time, and every surface that paints a category colour goes
through it. A one-time migration was the alternative; read-side mapping was chosen because it is
reversible by deleting one function and because an older build on another device still renders its own
colours correctly from the same rows.

The mapping is nearest-slot by cylindrical HSV distance, **hue-dominant** — a plain RGB distance drags
the whole pastel legacy set toward whichever palette entry is lightest, which is how a "nearest
colour" mapping ends up making everything brown. Saturation and value carry a little weight and earn
it on one real case: `#ff7537` (bright orange) and `#ac725e` (muted clay) are ~7° apart in hue but
belong on the gold and brown slots respectively.

How the 24 actually collapse — worth knowing, because **it is many-to-one and a collision is visible
immediately**:

| Slot | | Legacy swatches landing there |
|---|---|---|
| 0 pink `#C75C8A` | | `#d06b64` `#cca6ac` `#f691b2` |
| 1 gold `#C1832B` | | `#f83a22` `#fa573c` `#ff7537` `#ffad46` `#fbe983` `#fad165` |
| 2 blue `#3E90C4` | | `#9fe1e7` `#9fc6e7` `#4986e7` |
| 3 brown `#9A5F2A` | | `#ac725e` `#c2c2c2` `#cabdbf` |
| 4 purple `#A0509F` | | `#cd74e6` |
| 5 green `#3FA47C` | | `#42d692` `#16a765` `#92e1c0` |
| 6 indigo `#5A62CC` | | `#9a9cff` `#b99aff` `#a47ae2` |
| 7 olive `#8E9432` | | `#7bd148` `#b3dc6c` |

Two consequences to keep in mind:
- **There is deliberately no red slot**, because Ember is reserved. Reds resolve to gold, which is
  hue-preserving but not what a user picking `#f83a22` had in mind. Accepted.
- **Greys cannot stay grey** — the palette has a chroma floor, so `#c2c2c2` goes to brown, the least
  saturated-reading slot. Fixed rather than computed, since hue is undefined for an achromatic input.

Because collisions exist, **colour is never the only channel**: every legend row, limit row and
transaction row shows the category's name as text beside its dot.

New categories pre-select the first unused slot via `nextCategoryHex()`, so a run of new categories
does not all come out the same colour — the old picker defaulted every one of them to `colors[15]`.

**The perfect-day streak reads "Start a streak" each morning** until today's goals are ticked,
since the streak counts today inclusive. Pre-existing and deliberate, but now the hero of the home
screen rather than a small caption, so it is much more visible. Worth reconsidering.

**~~The bottom bar truncates at 200% font scale~~ — fixed 2026-07-29.** At or above font scale
**1.5** `AppBottomBar` drops its text labels and moves each name onto the icon's
`contentDescription`; below it, the labels render and the descriptions are null, so TalkBack never
announces a name twice. The threshold is `labelsFitAtFontScale()`, and the boundary is pinned by
baselines at 1.4 (labels, unwrapped) and 1.5 (icon-only). Material's label-less `NavigationBarItem`
is the platform fallback here; the alternative was worse for everyone, since those labels do not
ellipsize, they break mid-word.

The general rule this is an instance of: **a five-slot row of single words cannot survive 200% font
scale on a phone.** Whenever a fixed number of text slots has to share a screen width, the large-
scale plan is dropping to icons, not shrinking or clipping the text.

**Dark mode does not survive a cold start for signed-out users.** `MainActivity` holds it in
`rememberSaveable`, which a force-stop clears, so the app reopens dark regardless of the setting.
Currency was moved to DataStore for exactly this reason (Issue #76); the theme flag never was.
Pre-existing, unrelated to the redesign, but very visible now that light mode is worth using.

**`graphics-shapes` has no use site yet.** Adopt it in the first screen that needs state morphing.

**~~Every `ColorScheme` slot the app touches must be defined in both schemes~~ — audited and fixed
2026-07-30, see [§0's container-ladder section](#the-container-ladder--which-m3-slot-gets-which-tone).**
The original instance (`tertiaryContainer` missing from the dark scheme, rendering the Overview's
screen-time stat card in Material Purple) turned out to be one of seven — the full audit found
menus, all `AlertDialog`s, `DatePickerDialog`, a bottom sheet, and every snackbar colour also
undefined. Now guarded by `ApexPaletteSlotsTest` and the style plate, not just this paragraph.

---

## 9. What the device changed

Recorded because decision #10 — plate before spec — is the reason this document is worth trusting.
Five values were wrong on paper and only failed on a real panel:

1. **Hairlines were invisible** (1.6:1 / 1.4:1). Since hairlines replace cards here, the entire
   structural strategy was silently broken.
2. **The heatmap ramp compressed to nothing.** Alpha-over-dark killed the low end; middle steps did
   not separate.
3. **Then the fix overcorrected** — untracked went near-background, which erased the grid's
   structure for a user with little history.
4. **Ember and Alarm were the same colour in light mode.** The collision was predicted as a risk
   and turned out worse than predicted.
5. **A selected `FilterChip` rendered Sage green**, because Sage was sitting in `secondaryContainer`.

None of these are visible in a hex list. All five were obvious within seconds on the phone.

---

## 10. Screen inventory

IA is frozen (decision #8): routes, `NavHost`, bottom bar, screen inventory, ViewModels and the data
layer do not change. What changes per screen is layout, emphasis, component anatomy, and motion.

| Screen | State | Job / intent |
|---|---|---|
| `dashboard` (home) | **Redesigned 2026-07-29.** Streak hero (mono numeral + serif unit), de-carded Today section, heatmap at 30dp cells with an 8-week default window. Remaining: the grid is centred at ~76% width, and at 200% font scale the month label truncates to one letter. | Answer "how am I doing" in one glance. The heatmap is the signature and must own the screen. |
| `study_tracker` | **Redesigned 2026-07-29.** Rings deleted, timer on `ApexNumerals.hero`, de-carded history, chart spec'd with real axis labels. Remaining: the goal ring reads as a plain circle at 0 progress. | One dominant element (the stopwatch) plus controls. |
| `budget_tracker` | **Redesigned 2026-07-29.** Hero total on `ApexNumerals.hero` (was a `primaryContainer` card with a decorative accent circle), donut given a legend that names every slice, limits and trend de-carded, transaction rows de-carded off their per-row colour tints. Category palette applied via read-side mapping. Remaining: nothing known. | Densest data in the app. |
| `screen_time` | **Redesigned 2026-07-29.** Total on `ApexNumerals.hero` (was Instrument Serif in a tinted card), eyebrow section heads, de-carded device/app/history rows, chart on the shared duration-axis treatment. | Per-app list + trend. |
| `notes` | **Redesigned 2026-07-29.** Eyebrow title, de-carded rows on hairlines, mono timestamps, delete demoted off Alarm. | Calm dense list. |
| `reminders` | **Redesigned 2026-07-29.** De-carded rows, mono dates, overdue carried by icon + badge rather than repainting the row, exact-alarm banner given a real alert treatment. | Calm dense list. |
| `overview` | **Redesigned 2026-07-29.** One surface tone for all three stat cards (they were three different container hues, and the third was rendering in Material Purple), mono values, serif date, de-carded task rows, status is a type not a string. | Aggregates; drill-in targets. |
| `goals` | **Redesigned 2026-07-29.** Shared eyebrow headers, hairlines, 48dp row targets, real empty state. | Management surface, not a display surface. |
| Settings sheets | Not started | Form/row design. |

---

## 11. Verification

**Screenshot tests** are the regression net. 21 baselines across dark, light, and large-font
configurations. Run `find app/src/screenshotTestDebug/reference -name '*.png' | wc -l` rather than
trusting that number — it has already gone stale once.

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew validateDebugScreenshotTest
```

Record new baselines with `updateDebugScreenshotTest`. Baselines live in
`app/src/screenshotTestDebug/reference/` and are checked in; failures write an HTML report with
reference/actual/diff to `app/build/reports/screenshotTest/`.

Two setup facts worth not rediscovering: the enabling flag is required in **both**
`gradle.properties` and the module's `experimentalProperties`, and `@PreviewTest` needs an explicit
`screenshot-validation-api` dependency because the plugin only puts it on the runtime classpath.
Baseline filenames embed a hash of the preview's configuration, so changing a `@Preview`'s
parameters **orphans** its baseline rather than updating it — delete the stale file.

**Per screen, before calling it done:** both themes, 200% font scale, TalkBack pass
(`adb shell uiautomator dump` and grep for `content-desc`), and a look at it on the real device.
Layoutlib is not a phone.

**Prefer a screenshot preview over an emulator screenshot for theme coverage.** Driving the in-app
dark-mode toggle on the `Medium_Phone` AVD means tapping through a settings sheet on a CPU-starved
device; it is slow, it strays near real data, and it produces a one-off artefact. A `@PreviewTest`
pair for the same composable is deterministic, host-side, and stays as a regression net — the
Notes/Reminders row baselines exist for exactly this reason. Reserve the emulator for what only a
real runtime can show: scrolling, insets, navigation, live data.

**Do not poll `uiautomator dump` in a wait loop on this AVD.** It is expensive enough that a 2-second
poll during first composition will itself trigger an ANR dialog, which then looks like an app defect.
Use one generous sleep instead, and launch straight to a route with
`am start … --es navigate_to <route>` rather than tapping through the bottom bar.

**Then remove one thing.** Cut the least load-bearing element before shipping.

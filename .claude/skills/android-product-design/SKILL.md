---
name: android-product-design
description: ApexTracker's visual design law. Use whenever building or reshaping any Compose UI in this repo - screens, components, charts, dialogs, sheets, widgets. Covers the locked identity (type, color, motion, spacing), Android platform requirements, and the anti-generic constraints this app was redesigned to satisfy.
---

# ApexTracker Product Design

Derived from Anthropic's `frontend-design` skill (see LICENSE-NOTICE.md), narrowed to
Android/Compose and to this app's committed identity. Where this file and the general
skill disagree, this file wins - the identity below was decided deliberately, not
defaulted into.

Approach this as the design lead who owns one product's visual identity. The identity is
already chosen. Your job is not to re-invent it per screen; it is to apply it with enough
precision that a stranger could tell two ApexTracker screens belong to the same app, and
that neither was generated.

**Values live in `Design.md` at the repo root, not here.** That file is the reference: the full
type scale, both palettes with measured contrast ratios, the token tables, the chart spec, the
screen inventory, and the reasoning. This file carries the *rules*; when the two disagree on a
number, Design.md wins. Read it before designing a screen — enumerated values duplicated across
two documents rot, which this repo has already been bitten by (see the doc-accuracy note in
CLAUDE.md).

## The identity (locked - do not renegotiate per screen)

**Axis.** Structure from Spotify: dark-first, dense, tight vertical rhythm, motion
present everywhere. The identity is GRAPHITE — a cold monochrome instrument. Emphasis and
distinctiveness come from a mono voice, tabular figures, and density, NOT from a colour.
When density and restraint conflict, density wins on data screens and restraint wins on
chrome.

**Type.** Two faces, two jobs, no exceptions (the previous serif-led trio is retired):
- `MartianMono` - the voice. Display and headline slots (`displayLarge`…`headlineSmall`)
  AND every number a user reads as a quantity (`ApexNumerals`): currency, durations, the
  stopwatch, percentages, counts, axis labels, dates in tabular contexts. One mono speaking
  in both headlines and figures is what makes the app read as an instrument. Tabular figures
  are the point - numbers must not reflow when they change.
- `Geist` - everything conversational: titles, body, labels, controls. Paragraphs must
  never read as a terminal, so the mono stays out of running text.

Read type from `ApexType`/`MaterialTheme.typography`/`ApexNumerals`. An inline `fontWeight
=`, `fontSize =`, or `letterSpacing =` at a call site is a bug unless it is a documented
one-off with a comment saying why. Martian is a *wide* mono — display sizes are smaller and
more tightly tracked than a proportional face would be; do not "fix" that by enlarging them.

**Color.** No accent hue. Emphasis is carried by INK — `primary` is near-white (`Frost
#E9EBEE`) in dark, near-black (`Char #191C20`) in light — so a filled button is an inverse
block, not a coloured one. The ONLY hues in the app are the two semantics: `Sage` for
met/positive, `Crimson` (`error`) for over/failed — and a negative state always also carries
an icon or a word, never hue alone. Third-party app icons keep their own brand colours (they
are content, not chrome). Categorical colour exists only in data-vis, governed by the chart
palette. If a screen's hierarchy only works once you add a colour, the hierarchy is broken —
fix it with weight/size/position, do not add a hue.

Hand-authored cold-graphite dark and light palettes. **No Dynamic Color** - the wallpaper
does not get to repaint this app. No `Color(0xFF...)` outside `ui/design/`.

**Motion.** Every animation references a named `ApexMotion` token. No raw `spring()` or
`tween()` at a call site. Motion must carry information - state change, spatial
relationship, arrival, dismissal. Decorative perpetual motion is banned outright; the
counter-rotating rings that used to sit behind the study timer are the reference example
of what not to ship.

**Spacing and shape.** `ApexSpacing` and `ApexShapes` only. A raw `.dp` literal in a
layout modifier is a bug. Three radii exist; a fourth needs a written reason.

## Hard platform requirements

These are not stylistic. A screen that fails any of them is not done.

- **Touch targets >=48dp.** Every tappable thing. If the visual is smaller (heatmap cells,
  color swatches), the *target* is still 48dp via padding or `minimumInteractiveComponentSize`.
- **Dynamic type.** All text in `sp`, no fixed-height text containers, layouts survive
  200% font scale. Test at 200% before calling a screen done.
- **Both themes.** Every screen correct in dark and light. Not "works" - correct.
- **Edge-to-edge.** Content draws behind system bars; insets consumed deliberately, never
  by accident. Nothing important under a system bar or the gesture handle.
- **Contrast.** Body text >=4.5:1, large text and non-text indicators >=3:1, against the
  actual surface it sits on - including accent-on-surface and every heatmap ramp step.
- **Accessibility.** Every non-decorative element has a `contentDescription` from
  `strings.xml`; decorative ones take `null`. State is exposed via `semantics`
  (`selectable`, `toggleable`), not implied by color alone.
- **Four states, always.** Loading, empty, error, offline. An empty screen is an
  invitation to act, not a blank `Text`. "No data" is not an empty state.
- **Localized.** All user-visible strings from `strings.xml`. No concatenation, no
  `.take(1)` truncation of localized names.
- **Compose conventions.** Stateless composables, state hoisted, `Modifier` as the first
  optional parameter, previews for every component in both themes.

## Banned

Refuse these even if asked casually, and say why:

- Gradients as decoration. (A data-encoding ramp is not a gradient.)
- Glassmorphism, blur-behind, frosted surfaces. Explicitly rejected for this app.
- Drop shadows on dark surfaces. They are invisible on `#0E0F11`; `shadowElevation` there
  is cargo cult. Use surface-tone layering.
- The stacked-rounded-card template: a vertical run of `Card`s each with a small colored
  label at the top. This is the single most recognisable generated-dashboard shape and it
  is what this redesign exists to remove. Use hairlines, section headers, background
  shifts, and whitespace to separate content. A card must earn itself by being genuinely
  liftable or tappable as a unit.
- Pills and chips as decoration. A chip is a filter or a choice; it is not a label.
- Emoji in UI chrome.
- Icon-only controls without a label or a tooltip, outside the top bar.
- Any hue that isn't a semantic (Sage/Crimson). There is no accent to "add" — introducing
  a coloured highlight, a tinted card, or a branded button reintroduces the thing this
  identity removed. Emphasis is ink, size, weight, and position.

## Anti-generic constraint (read this before every screen)

This app is a cold monochrome — cool near-black dark, cold paper-white light, no accent hue,
a mono display voice. That deliberately sidesteps the warm-ink-and-serif look AI-generated
design (and Claude's own surfaces) default to. But monochrome has its own generic failure
mode: flat, feature-less gray. Distinctiveness has to be actively built from the axes the
default look does not occupy:

- **Mono as the voice, not just the numerals.** Display headlines AND figures are Martian
  Mono. The default look is serif + sans; here one instrument face carries the page. A
  tracker is mostly numbers — set them in mono, aligned, at real size, and let them lead.
- **Density.** The default look is airy and editorial. This app is dense and instrumental.
  Resist whitespace as a solution; monochrome makes empty space read as *unfinished*, so
  fill screens with structure (hairlines, eyebrows, rhythm), not padding.
- **The heatmap is the signature, and it is a full-square gray-ramp grid** — the classic
  GitHub contribution-graph read. A short-lived variant tried encoding intensity as bar height
  instead (each cell a bottom-anchored bar, perfect days snapping to a solid fill); it was
  reverted 2026-08-24 because it made the grid *harder* to scan at a glance, which defeats the
  point of a signature surface. Don't reintroduce bar-height geometry here. Everything around
  the heatmap stays quiet.
- **Shape language.** `androidx.graphics:graphics-shapes` morphing is available and the
  default look has no shape vocabulary at all. Use it for state transitions.

Before shipping a screen, ask: strip it to grayscale (it already is) — does it still read as
a deliberate instrument, or as an un-styled wireframe? If the latter, the hierarchy is doing
too little work. Add weight and structure, never a colour.

## Process

1. **Read the current screen first.** Screenshot it on device. Name what is specifically
   wrong before proposing anything.
2. **Plan in prose and ASCII before code.** Layout concept, what is emphasized, what is
   demoted, where motion goes, what the empty state says.
3. **Self-critique against the banned list and the anti-generic constraint** before
   writing Compose.
4. **Build against tokens only.** No literal colors, spacings, radii, durations.
5. **Verify on device**, both themes, 200% font scale, then screenshot. A picture is worth
   1000 tokens.
6. **Remove one thing.** Before calling it done, cut the least load-bearing element.

## Copy

Words are design material. Active voice, sentence case, plain verbs. Name things by what
the user controls, never by how the system is built. An action keeps its name through the
whole flow - a button that says "Save" produces a confirmation that says "Saved". Errors
say what happened and what to do; they do not apologize and are never vague. Be specific
rather than clever.

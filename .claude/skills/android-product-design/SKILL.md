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

## The identity (locked - do not renegotiate per screen)

**Axis.** Structure from Spotify: dark-first, dense, tight vertical rhythm, motion
present everywhere. Typography from Claude: editorial display face, real scale contrast,
severe color restraint. When they conflict, density wins on data screens and restraint
wins on chrome.

**Type.** Three faces, three jobs, no exceptions:
- `Instrument Serif` - display only, >=20sp. One weight exists; never fake bold, never
  use it below 20sp, never use it for a control label.
- `Geist` - all UI text, labels, body.
- `Geist Mono` - **every number a user reads as a quantity**: currency, durations, the
  stopwatch, percentages, counts, axis labels, dates in tabular contexts. Tabular figures
  are the point - numbers must not reflow when they change.

Read type from `ApexType`/`MaterialTheme.typography`. An inline `fontWeight =`, `fontSize
=`, or `letterSpacing =` at a call site is a bug unless it is a documented one-off with a
comment saying why.

**Color.** One accent: Ember `#D9613C` (dark) / `#B84A28` (light). It carries state and
emphasis and nothing else. There is no second brand color. Categorical color exists only
in data visualisation and is governed by the chart palette, not by taste at the call site.

Hand-authored dark and light palettes. **No Dynamic Color** - the wallpaper does not get
to repaint this app. No `Color(0xFF...)` outside `ui/theme/`.

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
- Drop shadows on dark surfaces. They are invisible on `#121417`; `shadowElevation` there
  is cargo cult. Use surface-tone layering.
- The stacked-rounded-card template: a vertical run of `Card`s each with a small colored
  label at the top. This is the single most recognisable generated-dashboard shape and it
  is what this redesign exists to remove. Use hairlines, section headers, background
  shifts, and whitespace to separate content. A card must earn itself by being genuinely
  liftable or tappable as a unit.
- Pills and chips as decoration. A chip is a filter or a choice; it is not a label.
- Emoji in UI chrome.
- Icon-only controls without a label or a tooltip, outside the top bar.
- More than one accent color on screen.

## Anti-generic constraint (read this before every screen)

This app's palette - near-black plus a warm terracotta accent, with a serif display and a
warm cream light mode - is close to two of the three looks that AI-generated design
currently defaults to. That was an informed choice, and the palette is not up for
renegotiation. The consequence is that **the palette cannot be what makes this app
distinctive.** Distinctiveness has to come from the axes the default look does not
occupy:

- **Tabular mono numerals as a primary visual element.** The default look is all serif and
  sans. Numbers here are set in mono, aligned, and given real size - a tracker is mostly
  numbers, so let them carry the page.
- **Density.** The default look is airy and editorial. This app is dense and instrumental.
  Resist whitespace as a solution.
- **The heatmap is the signature.** It is the one place to spend boldness. It must not
  read as GitHub's contribution graph - not green, not square-cornered-uniform, not
  bottom-anchored-left-to-right. Everything around it stays quiet.
- **Shape language.** `androidx.graphics:graphics-shapes` morphing is available and the
  default look has no shape vocabulary at all. Use it for state transitions.

Before shipping a screen, ask: if the accent were swapped to any other hue, would this
still be recognisably ApexTracker? If the answer is no, the design is resting on color.

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

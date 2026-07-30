# Plan — Papers feature, Graphite identity, personal README

Settled 2026-07-30 in a grill session. Three workstreams, executed in this order, on a hard
~$50 agent budget. Anything that doesn't fit the budget degrades into a self-contained GitHub
issue (with acceptance criteria and code pointers) that a smaller model can pick up cold —
that is the designated handoff mechanism, not a checklist in this file.

**Standing permission to the executing agent (Fable):** this plan is deliberately open-ended.
Improve, add, or cut within a phase as judgment dictates — including touching existing
features/UI where it serves the phase — as long as the thirteen decisions below are honored
and the budget order (Feature > Identity > README) is respected. Decisions are not up for
re-litigation; everything else is.

---

## Decision log

| # | Decision | Why |
|---|---|---|
| 1 | **Papers feature is the protected workstream; it builds first, under Ember** | When forced to rank against the budget, the owner protected the thing he'll use daily over the restyle. Built with design-system tokens, it restyles for free when the identity swaps. |
| 2 | **Reading is hybrid: knowledge layer in-app, rendering external** | Title/authors/abstract/TL;DR/memo live in the app; "Read" opens the PDF/page in a Custom Tab or system viewer. No in-app PDF reader — `PdfRenderer` gives bitmap pages with no text layer, `androidx.pdf` is alpha (forbidden by Design.md decision #5), and two-column PDFs on a 6" phone are miserable in any reader. The app's value is around the reading, not the rendering. |
| 3 | **Semantic Scholar API is the single metadata backbone** | Free, JSON, no XML parsing, machine-generated TL;DRs, citation counts, open-access PDF links, resolves arXiv IDs and DOIs, and has a recommendations endpoint. Fall back to the arXiv Atom API only if S2's freshness proves inadequate for the daily fetch. |
| 4 | **Sources are phased: manual add + curated seed (v1) → daily topic fetch (v2) → history-based recommendations (v3)** | All four were wanted; all four at once is scope creep. v1's two work on day one; recommendations need history to exist. v2/v3 are filed as issues when v1 ships. |
| 5 | **The system is a queue with a daily pick, wired into the goal engine** | The queue is the backlog; each day one item is promoted as today's pick. Reading feeds the Dashboard heatmap via a new goal metric — the feature inherits the app's existing habit machinery instead of growing its own. |
| 6 | **Memo is structured**: status (want-to-read / read / abandoned), a short "what I learned" text, an optional 1–5 signal | One glance at history says what a paper was and whether it mattered; the signal later feeds the recommender. Explicit paper-to-paper linking is deferred to an issue — under ~50 papers, memory does the linking. |
| 7 | **Issue #139's monochrome theme becomes THE identity, not a second option** | The owner's verdict on Ember: the warm-dark + terracotta + serif combination reads as Claude. Monochrome discipline — colour only where it carries meaning (Sage/Crimson) — is the identity, not a setting. |
| 8 | **Temperature: cold graphite, superseding the issue's warm grayscale** | Warm monochrome + serif is *more* Claude, not less — warmth is Claude's territory. Cool near-black dark, cold paper-white light. This is the single biggest "not Claude" lever. Starting point (device-verify before committing): dark `#0E0F11`/`#16181B`/text `#E8EAED`; light `#F4F5F7`/`#FFFFFF`/text `#1A1C1F`. |
| 9 | **Type goes mono-led; Instrument Serif is retired** | "The font screams Claude" — owner's words. The display voice becomes a monospace (big, confident mono headlines; body/UI stays a quiet sans). Shortlist real display monos — e.g. Martian Mono, IBM Plex Mono, Space Mono — and note that promoting Geist Mono to display duty has its own "screams Vercel" risk. Settle on the style plate, not in a paragraph. |
| 10 | **The heatmap's geometry is reshaped, not just recoloured** | It's the declared signature, and a six-step gray ramp has less resolution than ember had, exactly when the identity leans on it hardest. Prototype at least one shape-carries-intensity candidate (fill-height bars in the grid, scaled dots) against the gray grid on the style plate; judge on a real panel. |
| 11 | **Rollout: global token swap, on-device audit, then fix only the screens whose hierarchy collapsed** | Not a full 8-screen pass. Screens that leaned on Ember for hierarchy will look flat — expect ~3–4 (Dashboard, Budget, likely Overview) to need real work. |
| 12 | **README: before/after screenshot pairs, first-person voice** | Ember screenshots MUST be captured before the identity swap lands (cheap now, painful via git checkout later). Voice is the owner's own story, drafted from his words, edited by him. |
| 13 | **Budget order: Feature > Identity > README; all deferred work becomes GitHub issues** | The most-protected thing finishes first; the least-protected degrades into well-specified issues. Issues, not a plan-file checklist — this repo's own docs warn that in-file lists rot. |

Open calls the executing agent makes during the work (not pre-settled): whether `ApexTheme`
keeps `EMBER` as a selectable second identity or retires it; where Papers sits in navigation
(More sheet vs. promotion to the bottom bar); the exact mono face and body sans; whether the
papers goal is a new AUTO metric or MANUAL; entity/table naming; whether papers sync ships in
v1 or as a fast-follow issue.

---

## Phase 0 — Ember baseline capture (do first, it's cheap and irreversible-if-skipped)

Boot the emulator (`Medium_Phone` AVD — see the `android-emulator-available` memory; test warm,
per the `emulator-cold-start-anr` memory), seed representative data if needed, and screencap the
key screens under the current Ember identity: Dashboard (heatmap populated), Budget, Study,
Screen Time, Overview. Store under `docs/screenshots/ember/`. Also update issue #139 to record
that decisions 7–9 supersede its warm-grayscale framing.

## Phase 1 — Papers feature v1 (under Ember)

> **Fable prompt:**
> Read `Plan.md` decisions 1–6 and CLAUDE.md, then build the Papers feature v1 in
> ApexTracker. Data layer: a `Paper` entity (S2 paper id as natural key candidate, title,
> authors, year, venue, abstract, tldr, urls incl. open-access PDF, source enum, addedDate,
> readDate, status ∈ {WANT, READ, ABANDONED}, memo text, signal 1–5 nullable, plus
> cloudId/modifiedAt per app convention) with DAO, Room migration 15→16 following the
> additive `MIGRATION_14_15` pattern, and schema export. Network: a minimal OkHttp-based
> Semantic Scholar client — paper lookup by arXiv id / DOI / S2 URL and (behind an interface,
> for v2/v3) search + recommendations; all response parsing as pure unit-tested functions.
> UI (existing design tokens only — no raw dp/hex): a Papers screen with today's pick,
> the queue, and a reading history; an add-by-link flow (paste → fetched metadata preview →
> confirm); a paper detail sheet with abstract/TL;DR, an "Open paper" action (Custom Tab /
> system PDF viewer), and the structured memo editor. Ship a small curated seed list
> (foundational CS/ML papers) importable on first open. Goal engine: a new goal metric so
> "read a paper today" feeds the heatmap — follow `DashboardScoring.kt`'s AUTO-metric
> pattern, unit-tested. Sync via `FirebaseManager`'s 5-part shape if budget allows, else
> file it as an issue. File v2 (daily topic fetch) and v3 (S2 recommendations from
> history + signal) as self-contained issues. Verify: build, unit tests, lint, and an
> emulator pass; the owner smoke-tests before the phase is called done.

Gate: owner uses it for a few days if he wants. Phase 2 does not depend on the gate.

## Phase 2 — Graphite identity

> **Fable prompt:**
> Read `Plan.md` decisions 7–11, `Design.md`, `.claude/skills/android-product-design/SKILL.md`,
> and issue #139, then replace the Ember identity with the cold-graphite monochrome identity
> as the app default. Order of operations is non-negotiable: (1) style plate first — author
> the graphite dark+light ramps, render 2–3 display-mono candidates at real display sizes,
> and prototype the reshaped heatmap next to the gray grid; judge everything on a real
> panel/emulator and record measured contrast ratios before touching a single screen.
> (2) Swap the foundation: `ApexPalette.kt`, `ApexType.kt` (new font files in `res/font/`,
> OFL text in `assets/licenses/`, serif retired), `ApexTokens.kt` (new `ApexTheme` entry as
> default), heatmap ramp in `ApexSemantics`. Sage/Crimson semantics carry over untouched.
> (3) Audit all 8 screens on-device; re-touch only the ones whose hierarchy collapsed
> without the accent. (4) Update `Design.md` (decision log, measured values, §10) and the
> design skill's rules; re-record the screenshot baselines; deal with the two Glance
> widgets' hardcoded palette (follow or file an issue). Honor Design.md's anti-generic
> test throughout: with no accent, identity rests on type, density, and the heatmap.

## Phase 3 — README

> **Fable prompt:**
> Read `Plan.md` decisions 12–13, then rewrite `README.md` in the owner's first-person
> voice. The story, in his words: it started as a deliberately generic project to build a
> consistent building habit; it became a tool he actually reaches for daily instead of a
> scattered array of post-its, reminders, calendars and spreadsheets; the ideal it grew
> into is minimizing how much he *needs* his phone while maximizing the value of the time
> he does spend on it — every feature something he personally uses every day, a one-stop
> shop for organizing; not revolutionary, not new, perfectly fit for one person; and a
> vehicle for learning — Android app dev, device permissions, Firebase/Firestore, Room
> migrations, design systems. Embed before/after pairs from `docs/screenshots/ember/` and
> freshly captured graphite shots of the same screens. Include a compact features/stack
> section and a setup pointer, but keep the personal voice primary. Draft, then let the
> owner edit until it sounds like him.

---

## Budget mechanics

- Fable does architecture, design judgment, migrations, and the hard Canvas/plate work.
  Anything mechanical (string extraction, per-screen polish beyond the flat-screen fixes,
  v2/v3 paper sources, widget palette follow-up) is filed as an issue for cheaper models.
- Pure logic + unit tests first, always — that's what makes the scut work safely delegable.
- Verification is per-phase (build/test/lint + one emulator pass), not per-commit; the owner
  smoke-tests at phase gates, per his established phased-workflow preference.
- If the money runs out mid-phase: stop, file what remains as issues with exact code
  pointers, and update this file's status line below.

**Status:** Phase 0 complete (2026-07-30): Ember baselines captured on the SM-S931U1 into
`docs/screenshots/ember/` (dashboard, study_tracker, screen_time, budget_tracker, overview;
SystemUI demo mode used to clear personal notification icons — reuse for the "after" shots),
and issue #139 annotated with the superseding decisions. Capture notes: the `navigate_to`
extra only takes effect on a cold start — `am force-stop` before each `am start`. The Budget
screen recorded an empty month ($0.00) — an honest but visually thin "before"; match whatever
data state exists when capturing the graphite "after" pair.

**Phase 1 complete (2026-07-30, branch `feature/papers`):** the Papers reading log shipped and
was verified end-to-end on the SM-S931U1 — real v19→v20 migration on a populated DB (note:
CLAUDE.md's "v15" was already stale; the DB was at v19), seed import, mark-read with memo +
signal, history rendering, and the PAPERS goal metric evaluating "Met" on the Dashboard from a
real read. S2 API verified live (the unauthenticated pool 429s readily — v2 must back off).
Deferred to issues: #149 (daily topic fetch), #150 (recommendations), #151 (Firestore sync).
Uncommitted on `feature/papers` pending owner review. Next: Phase 2 (graphite identity), which
also restyles this feature for free.

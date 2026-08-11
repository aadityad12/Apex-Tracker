Screenshots embedded by the root [README.md](../../README.md).

Organised as before/after pairs of the visual redesign:

- `ember/` — the previous **Ember** identity (warm near-black + terracotta accent + serif
  display), captured 2026-07-30 before the redesign landed.
- `graphite/` — the current **Graphite** identity (cold monochrome, mono display, fill-height
  heatmap), captured 2026-07-30.

Each folder holds the same five screens: `dashboard.png`, `study_tracker.png`,
`screen_time.png`, `budget_tracker.png`, `overview.png`.

`features/` is different — not an identity pair, just one shot per feature that is hard to
describe in prose, captured 2026-08-11 on the Graphite identity: `widgets.png` (cropped to the
two home-screen widgets), `receipt_scan.png`, `papers_recommendations.png`.

Capture recipe (so a re-shoot matches): install the debug build, put the status bar into
SystemUI demo mode to hide personal icons, then for each screen `am force-stop` the app and
cold-launch it with `--es navigate_to <route>` before `screencap`.

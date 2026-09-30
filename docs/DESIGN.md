# ScreenOn design

A quiet Korean Android utility. One task, one screen, no account, advertisements, feed or subscription.

## Reference and interpretation

- [UI UX Pro Max](https://github.com/nextlevelbuilder/ui-ux-pro-max-skill), retrieved 2026-09-30. Applied its SKILL.md, `utility productivity minimal mobile --design-system` and `accessibility state toggle --stack jetpack-compose` searches, plus native pro-rules and accessibility/touch/performance checklist.
- Matching result: **Flat Design**, mobile utility/productivity, clean type, restrained motion, no complex onboarding. Its web landing-page/video pattern was intentionally discarded because this is a single-purpose native app. Teal guidance was adapted to pine/lime for explicit power state; no web typography downloads.
- Android native semantics, 48dp minimum interactive targets, system font (Korean fallback), system dark theme, edge-to-edge safe insets, scrollable compact/landscape/large-font layout, 520dp maximum content width.

## Visual system

| Role | Light | Dark |
|---|---|---|
| Background | #F5F6F2 | #101915 |
| Surface | #FFFFFF | #1D2A23 |
| Text | #142D27 | #ECF1E9 |
| Secondary text | #53635B | #B8C7BE |
| Active dial | #C7F69B | #C7F69B |
| Active dial text | #142D27 | #142D27 |

Identity and first sentence lead into a 204dp power control, explicit text status, elapsed time, ON/OFF segments, and home widget installation. The dial is also a switch; labeled ON/OFF controls support people who prefer explicit actions. No animations run continuously. A 180ms state color transition uses Compose's platform animation duration scale. State has text and switch semantics, not color alone.

Widget: dark pine panel, ScreenOn name opens app, separate 64×56dp ON/OFF target controls service. It uses explicit desired-state actions rather than inverting possibly stale launcher state.

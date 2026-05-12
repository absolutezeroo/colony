# 10 — Tech Debt Register

This is a **lightweight** technical debt register, intentionally small. Hearthbound has 7300+ lines of tech debt documentation at MVP-55; that's the anti-pattern we avoid.

## Format

Entries are concise. One line per debt unless context demands more. Statuses:

- `OPEN` — unresolved.
- `ACCEPTED` — known limitation we won't fix (long-term constraint).
- `DEFERRED` — postponed to a specific phase or version.
- `FIXED` — resolved (kept here for ~30 days for searchability, then archived).

## Archival

When an entry is `FIXED` and 30+ days old, move it to `docs/archive/tech-debt-archive.md` (when that file is needed). The live register stays under 200 lines.

## When to add an entry

Add an entry when:

- You take a shortcut you intend to revisit.
- You hit a NeoForge or Minecraft limitation you cannot fix.
- You document a deliberate scope cut.

Do **not** add an entry for:

- Every bug you fix (use commit messages and issues).
- Every refactor (use git log).
- Routine TODOs (use `// TODO(target):` in code).

---

## Active debt

(Empty at project start. Populated as development progresses.)

---

## Risk-flagged (advance warnings)

These are not "debt" yet but are areas where debt is likely to accumulate.

### Pathfinding

**Risk:** Custom pathfinding (decided after vanilla testing) is the largest single time sink in Phase 1. Initial implementation will likely be naive A* with `ColonyNodeEvaluator`. Performance at 50+ citizens is unknown.

**Plan:** Profile at month 6. If TPS drops below 18 with 30 citizens pathing, implement hierarchical pathfinding (V2 originally) ahead of schedule.

**Fallback:** Swap `ColonyPathNavigation` for vanilla `GroundPathNavigation` with `ColonyNodeEvaluator` only, accepting partial functionality temporarily.

### Persistence at scale

**Risk:** Per-colony NBT files lazy-loaded. Untested at 20+ colonies in a single world.

**Plan:** Stress test in month 9 (Phase 3) with synthetic colonies. Implement chunked persistence (split large colony NBTs across multiple files) if a single colony exceeds 2 MB.

### NeoForge 1.21.1 longevity

**Risk:** 1.21.1 is a stable target but the NeoForge ecosystem moves forward (1.21.5+, 1.22+). Our V1 timeline of 14-18 months may overshoot the version's relevance.

**Plan:** Re-evaluate at month 12. If 1.21.1 is in maintenance-only mode, plan a 1.21.x or later port before V1 release. The multi-module architecture should make this less painful than a mono-module port.

### BlockUI dependency (V2)

**Risk:** If we adopt BlockUI for V2 advanced GUI, we depend on LDT (MineColonies team) tooling. License compatibility verified, but API breakage risk remains.

**Plan:** Maintain vanilla `AbstractContainerScreen` fallback. BlockUI is opt-in, never required.

---

## Accepted limitations (V1)

Things we are explicitly **not** doing in V1 and have no plan to do later:

- **No multi-loader support.** NeoForge 1.21.1 only. A separate Fabric fork might happen post-V2 but is not committed.
- **No 3D freeform interior shapes.** 2D footprint + uniform vertical extent. Overhangs and balconies are not counted as room interior.
- **No auto-detection of room types.** Player designates explicitly. Always.
- **No vanilla villager integration.** Citizens are a separate species. Vanilla villagers function normally alongside.
- **No combat in V1.** No guards, no soldiers, no raid defense. Existing hostile mobs damage citizens as they would villagers, but no combat *roles* exist.
- **No breeding, aging, or natural death in V1.** Citizens are immortal except by combat or environmental death.

These are documented design decisions, not omissions to be revisited.

---

## Deferred to V2

Tracked features explicitly postponed:

- 4-axis reputation system (currently V1 has scalar mood).
- Economy (treasuries, wages, transactions, currency).
- Tax system (6 tax types, brackets, evasion).
- Inter-colony trade and marketplaces.
- 8-tier room quality (currently 3 tiers in V1).
- Adjacency bonuses between rooms.
- Schematic import (.nbt vanilla format).
- Citizen-to-citizen item handoff (porter chains).
- Customizable schedules and night shifts.
- Immigration (new citizens arriving from outside).
- Aging and natural death.
- Disease and injury.

See `08-ROADMAP.md` for V2 scope details.

---

## Conventions for entries

When adding an entry, use this format:

```markdown
### {Short identifier}

**Status:** OPEN | ACCEPTED | DEFERRED | FIXED
**Severity:** LOW | MEDIUM | HIGH | CRITICAL
**Discovered:** {phase or date}
**Target:** {phase or version, or "no plan"}

{One paragraph describing the limitation, why it exists, and what the plan is.}
```

Example:

```markdown
### Custom pathfinding initial implementation

**Status:** OPEN
**Severity:** MEDIUM
**Discovered:** Phase 1, month 1
**Target:** Profile and possibly optimize in Phase 2

The initial `ColonyNodeEvaluator` uses a naive A* with no path caching. Acceptable for development and small colonies. May require optimization (path caching, hierarchical search) when citizen count exceeds 30 in a colony. Profile at month 6 to confirm.
```

Keep entries concise. If the limitation requires more than 2 paragraphs to explain, it probably deserves its own design document, not a debt register entry.

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

### EntityCitizen state model is a stub

**Status:** OPEN
**Severity:** LOW
**Discovered:** Phase 1, month 1 (2026-05)
**Target:** Phase 1, months 2-4 as the trait/mood/job/intent systems land

`EntityCitizen` currently holds only `citizenId`, `displayName`, and a nullable `ColonyId`. The full field set described in `docs/05-CITIZEN-SYSTEM.md` §Entity model — appearance, home/work building, job, state, mood, needs, traits, skills, intent queue — is not present. This is by design for Month 1 (deliberate scope cut) but the doc-vs-code gap is wide enough to flag.

### Citizen spawn search does not expand per attempt

**Status:** OPEN
**Severity:** LOW
**Discovered:** Phase 1, month 1 (2026-05)
**Target:** Phase 2

`docs/05-CITIZEN-SYSTEM.md` §Spawning specifies "spawn search expands by 2 blocks per attempt". The current `CitizenSpawner.pickCandidate` samples uniformly inside a fixed `CITIZEN_SPAWN_SEARCH_RADIUS` (6) and walks vertically from `townHall.y` outward to find a valid Y. Works for flat terrain (which all test rigs and the founding flow currently rely on); in steep or buried terrain the founding cohort may fail silently after the retry-timeout. Phase 2 should either implement the documented expansion or replace the picker with a surface-walk over the full anchor footprint.

### `isValidSpawn` uses 1-block clearance, not full mob height

**Status:** OPEN
**Severity:** LOW
**Discovered:** Phase 1, month 1 (2026-05)
**Target:** Phase 2

`CitizenSpawner.isValidSpawn` checks one cell of air above the candidate. Vanilla `PathfinderMob` is 1.95 blocks tall by default, so a citizen spawned with a 2-block ceiling has zero head clearance and any future `MAX_HEALTH`/hitbox tweak could clip it into the block above. Acceptable today because every founding scenario provides ≥2 air blocks; revisit when player-placed Town Halls in heterogeneous terrain land.

### Citizen names hardcoded, not datapack-driven

**Status:** OPEN
**Severity:** LOW
**Discovered:** Phase 1, month 1 (2026-05)
**Target:** When the data-driven content layer (`docs/06-DATA-DRIVEN.md`) lands

`CitizenNamePool.randomName` reads from an in-code constant list. `docs/05-CITIZEN-SYSTEM.md` §Spawning specifies "randomly assigned name from the lang JSON pool". Migrate when the datapack/lang pipeline is wired.

### Colony Tool respawn restore is unconditional

**Status:** OPEN
**Severity:** LOW
**Discovered:** Phase 1, month 1 (2026-05)
**Target:** When `ColonyMetadata` carries the founder/owner UUID

`onPlayerRespawn` in `ColonyMod` grants any respawning player a Colony Tool if they don't already have one, regardless of whether they actually own a colony. The desired behavior (see `docs/04-BUILDING-SYSTEM.md` "Recovery") is to gate the restore on "the player has at least one colony associated with their UUID", but `ColonyManager`/`ColonyMetadata` currently store only the founding tick, not the founder UUID. Harden once an owner field exists or a `playerOwnedColonies(UUID)` index is added.

### `:testmod` SPI hole detector is disarmed

**Status:** OPEN
**Severity:** MEDIUM
**Discovered:** Phase 1, month 1 (2026-05)
**Target:** When the `ColonyAddon` / `ColonyAddonContext` SPI lands in `:api`

`docs/01-ARCHITECTURE.md` §Module structure positions `:testmod` as the SPI hole detector: it should compile against `:api` + vanilla MC only, so that any NeoForge leak through the API surface fails the build. Today `:testmod` applies `colony.mod-conventions` (full NeoForge) because the only entry point available is `@Mod`/`IEventBus` from FML — the `ColonyAddon` interface described in docs/01 lines 39 and 86 hasn't been written yet. As long as testmod can call NeoForge directly, the detector cannot fire. Switch `:testmod` to `colony.java-conventions` + `neoForge { neoFormVersion = ... }` once the addon SPI exists.

### PendingPlacement state not persisted across disconnect

**Status:** OPEN
**Severity:** LOW
**Discovered:** Phase 1, month 2 (2026-05)
**Target:** Phase 1, month 3 or later (when SavedData parity with `ColonyIndex` becomes worth the lift)

`docs/04-BUILDING-SYSTEM.md` §Placement workflow line 41 states: *"If the player disconnects during `PendingPlacement`, the state is saved and resumable via `/colony resume` at reconnect."* The current `PendingPlacementManager` is a plain `ConcurrentHashMap<UUID, PendingPlacement>` held in memory; disconnecting mid-paint drops the pending and there is no `/colony resume` command. Acceptable for V1 because the painting workflow is short (seconds, not minutes) and the Hut block stays in inventory, but the doc claim is currently a lie. Either implement persistence or weaken the doc to "transient" — both are fine.

### Outer-zone enclosure check is not implemented

**Status:** OPEN
**Severity:** MEDIUM
**Discovered:** Phase 1, month 2 (2026-05)
**Target:** Phase 1, month 3 (alongside room painting and the broader validation pipeline)

`docs/04-BUILDING-SYSTEM.md` §Placement workflow item 5.3 requires that *"the outer zone must enclose at least one valid interior space (flood-fill yields a non-trivial connected air region)"*. `ZoneValidator` only checks volume bounds, hut containment, building overlap, and chunk loading. A player can today register a 1×1 column of solid stone as an outer zone and the server accepts it. Land flood-fill enclosure detection with the broader async validation pipeline; until then, the zone is purely a spatial reservation, not proof of a functional building.

### `BuildingIndex` is dimension-blind

**Status:** OPEN
**Severity:** LOW
**Discovered:** Phase 1, month 2 (2026-05)
**Target:** Before huts are placed outside the overworld

`BuildingMetadata` carries `ColonyId`, `HutType`, `BlockPos`, and `AxisAlignedOuterZone` — no `ResourceKey<Level>`. `BuildingIndex` is stored as overworld SavedData and treats every entry as if it lives in one global coordinate space. Two huts at the same numeric coordinates in different dimensions would falsely report `hasOverlap`, and `findByPosition` would return whichever was registered first. ColonyIndex has the dimension field; BuildingIndex inherited the simpler shape from prompt 2.3 because every test case stayed in `minecraft:overworld`. Add a `ResourceKey<Level>` field to `BuildingMetadata` (with codec migration) once nether/end colony support is on the table.

### Outer-zone validation pipeline is single-pass

**Status:** OPEN
**Severity:** LOW
**Discovered:** Phase 1, month 2 (2026-05)
**Target:** Phase 1, months 4-7 (structural/material/tier evaluation)

`docs/04-BUILDING-SYSTEM.md` §Validation pipeline (lines 356-371) describes a 6-pass async evaluation: structural enclosure, material analysis, functional block scan, room evaluations, tier evaluation, notification. Today only the placement-time `ZoneValidator` (synchronous, on the server thread, four checks) exists. This is the right scope for Month 2 — tiers and rooms land later — but the doc reads as if the full pipeline is in place. Keep the doc honest by either marking that section "V1 month 4+" or splitting it from the placement-time validator that actually exists today.

### `AxisAlignedOuterZone` shape diverges from doc spec

**Status:** FIXED-IN-CODE
**Severity:** LOW
**Discovered:** Phase 1, month 2 (2026-05)
**Target:** Doc update only

`docs/04-BUILDING-SYSTEM.md` lines 51-58 specifies `record AxisAlignedOuterZone(UUID id, BlockPos hutBlockPosition, int north, south, west, east, up, down)` — directional offsets tethered to the hut block. The actual implementation is `record AxisAlignedOuterZone(BlockPos min, BlockPos max) implements OuterZone` with a normalizing `fromCorners` factory. The new shape is strictly simpler (overlap/containment are arithmetic on two corners) and makes the codec trivial. Update doc 04 to match the implementation; no code change needed.

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

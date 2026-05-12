# 08 — Roadmap & MVP Scope

## Honest preamble

This roadmap is dimensioned for a solo developer assisted by AI tooling (Claude Code in JetBrains), working ~15-20 hours per week. The estimates account for:

- The 2-4 month risk of custom pathfinding (your own decision, justified by vanilla testing).
- AI acceleration on boilerplate (2-3× speedup on Codec, datagen, payloads).
- AI non-acceleration on architectural decisions, debugging, in-world testing.
- Solo dev fatigue across 18+ months.

**Total V1 estimate: 14-18 months from project start to a public 1.0 release.** This is significantly faster than Hearthbound's trajectory because of multi-module discipline + early alpha + reduced doc overhead.

## Phase 0 — Bootstrap (2 weeks)

Set up the foundations. No gameplay yet.

| Day | Deliverable |
|---|---|
| 1-2 | `git init`, Gradle multi-module skeleton (`:core`, `:api`, `:common`, `:neoforge`, `:testmod`), `settings.gradle`, ModDevGradle config |
| 3-4 | `build-logic/` convention plugins (java, mod, publish), Spotless with Allman config, Checkstyle rules |
| 5 | First `./gradlew :neoforge:runClient` boots, displays "Hello Colony" in chat |
| 6-7 | GitHub Actions CI: build, spotless, checkstyle, gametest. Branch protection on `main`. |
| 8 | `:core` event bus, `Registry<T>`, `Identifier` POJOs. JUnit tests pass headless. |
| 9 | `:api` exposes `ColonyRegistries` constants. `:testmod` imports `:api` and compiles |
| 10 | First Codec dispatch working: a `JobType` registered, JSON loaded, decoded via dispatch |
| 11-12 | NeoForge attachment + SavedData scaffold for `ColonyIndex` + per-colony NBT files |
| 13 | First `CustomPacketPayload` flowing client→server with validation |
| 14 | Documentation pass: ensure all 11 docs reflect current state, push public repo, announce on Reddit/Discord |

**Phase 0 exit criteria:** project clones, builds, runs in dev, has CI green, has docs aligned, is public.

## Phase 1 — Core Loop Foundations (3 months)

The minimum to call the project "alive."

**Month 1:**

- `EntityCitizen` custom entity (model, texture stub, registration).
- Citizen spawning: Town Hall block, 4 citizens spawn on placement.
- Basic citizen state machine (IDLE, TRAVELING, RESTING).
- Custom pathfinding: `ColonyNodeEvaluator` initial version, `ColonyPathNavigation`, integration tests.
- Persistence: citizens survive server restart, attached to colony.

**Month 2:**

- Colony Tool item, modes (Zone, Storage, Link, Inspect).
- Placement interception for Hut blocks (`PendingPlacement` state).
- `AxisAlignedOuterZone` painting and validation (rectangular only in this phase).
- One Hut block: Residential Hut. Place it, designate AABB outer zone.
- `BuildingTierEvaluator` with 3 tiers and basic structural validators.

**Month 3:**

- Room painting (Freeform footprint).
- Room types: bedroom (V1 minimum).
- Room slot assignment in Building GUI.
- Citizens get assigned to home rooms, sleep there.
- Persistence of buildings, rooms, citizen assignments.

**Phase 1 exit criteria:** the player can place a Town Hall, see 4 citizens spawn, build a structure, place a Residential Hut, paint zones, designate a bedroom, and watch citizens sleep there at night. **First playable end-to-end loop.**

## Phase 2 — First Job (2 months)

A citizen who actually does something useful.

**Month 4:**

- Farmer Hut Building type with required Office room slot.
- Storage chest typing system: 5 roles, Colony Tool storage mode, in-world particle indicators.
- Farmer Hut storage slots: seeds_input, tools, harvest_output.
- Scarecrow anchor type, right-click with seed sets crop, GUI for zone N/S/W/E offsets.

**Month 5:**

- `FarmerJobBehavior`: take tool, take seeds, walk to scarecrow zone, plant, walk back, deposit.
- Hunger need for citizens: decay, threshold, cookhouse fallback at Town Hall.
- Basic mood system: well_fed, well_rested, homeless, starving modifiers.

**Phase 2 exit criteria:** a citizen hired as farmer plants wheat and stockpiles it. The player can survive a hypothetical winter on what the colony produces.

**🎯 Public alpha release target: end of Phase 2 (~6 months from start).**

The alpha goes on Modrinth with disclaimers: "very experimental, lots of bugs, single-player only for now, please test and report." The goal is to start collecting feedback ASAP, not to look polished.

## Phase 3 — Polish & Multiplayer (3 months)

Make it actually usable on a dedicated server.

**Month 7:**

- Dedicated server testing: 4 players, 1 colony each, simultaneous play.
- Permission system: NONE/VIEW/INTERACT/OFFICER/OWNER levels.
- Anti-cheat baseline: rate limits, distance validation.
- Network optimization: subscription model for deltas, bundle initial sync.

**Month 8:**

- Tier 2 (Developed) and Tier 3 (Established) Building requirements.
- Material coherence and decoration scoring.
- Freeform footprint for outer zones (extruded 2D footprint, L/T/U shapes).
- Bug fixing from alpha feedback.

**Month 9:**

- Cookhouse Building type, real food preparation.
- Kitchen room type, dining_room room type.
- Builder Job (manual block placement assistance, not template-based yet).
- Second alpha release: dedicated server tested.

**Phase 3 exit criteria:** 3-4 players on a dedicated server can each run a small colony with farmers and survive together. The build is stable enough for casual play.

## Phase 4 — Content Diversification (3 months)

Expand the world.

**Month 10-11:**

- Miner job + Quarry Pillar anchor.
- Lumberjack job + Lumber Post anchor.
- Office room type for management huts.
- Storage Room type.
- More citizen traits (target: 15-20 total).

**Month 12:**

- Tavern Building (V1 social hub).
- Citizens visit tavern for mood boost.
- 5 RoomType total (bedroom, kitchen, dining, office, storage_room).
- 5 BuildingType total (Town Hall, Residence, Farmer, Miner, Lumberjack).

**Phase 4 exit criteria:** the V1 content scope is complete. The mod supports an actual playthrough of multiple hours.

## Phase 5 — V1 Release Prep (1-2 months)

**Month 13:**

- Comprehensive in-world testing.
- Documentation cleanup, player-facing wiki.
- BlockUI integration (optional, opt-in; fallback to vanilla screens).
- Performance profiling and optimization passes.

**Month 14:**

- Bug bash week.
- Modrinth + CurseForge V1.0 release.
- Discord community launch.

**Phase 5 exit criteria:** stable, documented, playable, public.

## V1 scope — final list

What ships in V1.0:

**Buildings:**
- Town Hall (placed once per colony, free)
- Residential Hut (player housing)
- Farmer Hut
- Miner Hut
- Lumberjack Hut
- Cookhouse
- Tavern

**Citizen jobs:**
- Builder (assists block placement, manual)
- Farmer
- Miner
- Lumberjack
- Cook
- Innkeeper (tavern)

**Room types:**
- Bedroom
- Kitchen
- Dining Room
- Office
- Storage Room

**Anchor types:**
- Scarecrow (farming)
- Quarry Pillar (mining)
- Lumber Post (forestry)

**Citizen mechanics:**
- Custom entity with custom pathfinding
- 10-15 traits
- Hunger need
- Fatigue need
- Basic mood (single scalar)
- Fixed daily schedule per job

**Building mechanics:**
- Free-build with Hut block + zone designation
- AABB and freeform outer zones
- Freeform room sub-zones
- 3-tier system (Basic, Developed, Established)
- Storage chest typing (5 roles)
- Right-click anchor configuration

**Architecture:**
- Multi-module Gradle
- Public `:api` Maven artifact
- Server-authoritative
- Dedicated server support
- Datapack-overridable content

## V2 scope — what's deferred

Everything below is V2+ and intentionally excluded from V1:

- 4-axis reputation system (citizen_loyalty, citizen_peer, player_standing, colony_prosperity)
- Economy: treasuries, wages, transactions, currency
- Tax system: 6 tax types, brackets, evasion, treasury budgets
- Inter-colony trade and marketplaces
- Combat and military: guards, raids, walls
- Immigration: new citizens arrive from outside
- Disease, injury, aging
- Children and breeding
- 8-tier room quality (Squalid → Noble)
- Master Suite, Library, Workshop Bay, Tavern Hall, Barracks Dormitory
- Schematic import (vanilla .nbt files)
- Building demolition by Builder citizen
- Hierarchical pathfinding (room graph + fine path)
- Adjacency bonuses between rooms
- Citizen-to-citizen item handoff (porter chains)
- Customizable citizen schedules
- Night shifts
- BlockUI advanced screens (V1 uses vanilla AbstractContainerScreen)
- Quest or progression systems
- Custom world generation

## Risk register

The largest risks tracked from day one:

| Risk | Probability | Impact | Mitigation |
|---|---|---|---|
| Custom pathfinding takes longer than 4 months | High | High | Fallback to vanilla `GroundPathNavigation` with `ColonyNodeEvaluator` swap if blocked at month 5. Acceptable temporary degradation. |
| Solo dev burnout at month 12 | Medium | Critical | Public alpha at month 6 brings community feedback that re-energizes. Take 2-week breaks every 4 months. |
| Performance issues at 50+ citizens | Medium | High | Profile early (month 6). Tick budget enforced from Phase 1. Hierarchical pathfinding in V2 if needed. |
| Hearthbound or MineColonies releases a freeform feature first | Low | Medium | Differentiation via multi-module, public API, planned economy. We win on architecture and breadth, not first-mover. |
| NeoForge 1.21.1 deprecation before V1 ships | Low | High | Re-evaluate at month 12. If 1.21.1 abandoned, port to current latest before V1 release. |
| LGPL incompatibility with a critical dependency | Low | Medium | BlockUI is the only known dependency under non-LGPL license. Optional in V1, can drop in V2. |

## Decision points

Predetermined moments where we evaluate and possibly pivot:

- **End of Phase 0 (week 2):** Is the multi-module Gradle setup sustainable? If we hit dead ends, simplify to 2-3 modules instead of 5.
- **End of Phase 2 (month 6):** Has the public alpha attracted attention? If <100 downloads in first month, reconsider marketing/positioning.
- **End of Phase 3 (month 9):** Are dedicated server tests stable? If chronic networking bugs, pause feature work for hardening.
- **End of Phase 4 (month 12):** Is V1 scope still on track? If 2+ months behind, cut Miner or Lumberjack to ship.

## What success looks like

By V1.0:

- 1000+ unique downloads on Modrinth/CurseForge.
- 5+ modpack inclusions.
- 1+ community-developed addon using `:api`.
- A small but engaged Discord community.
- A reputation for clean architecture and reliable behavior.

By V2 (estimated +12-18 months past V1):

- The full differentiators implemented (reputation, economy, taxes).
- Recognition as a serious alternative to MineColonies for free-build players.
- An addon ecosystem.

## What failure looks like

We acknowledge this honestly:

- **Hard failure:** project abandoned before public alpha. Burnout, life event, scope mistake.
- **Soft failure:** project ships V1 but fewer than 100 unique users, no community. Build it, no one comes.
- **Misalignment failure:** V1 ships and is technically good but players want MineColonies-with-tweaks, not free-build. Pivot or accept niche audience.

Failure modes are documented to keep us honest. If we see signs at decision points, we adjust rather than coast.

# Phase 1 Retrospective

**Tag:** v0.4.0-alpha
**Window:** 2026-05 (project started 2026-05-12; Phase 1 closed 2026-05-14)
**Author:** absolutezeroo + Claude Opus 4.7 (1M context)
**Audience:** future-me.

This document is the post-mortem on Phase 1. It is honest. It lists things that worked, things that did not, things I got wrong, and things I dodged that I should not have dodged.

## Phase 1 deliverables, as shipped

The Phase 1 exit criterion in `docs/08-ROADMAP.md` reads:

> the player can place a Town Hall, see 4 citizens spawn, build a structure, place a Residential Hut, paint zones, designate a bedroom, and watch citizens sleep there at night. **First playable end-to-end loop.**

Status against that criterion:

| Step | Server logic | GameTest | Player-facing UI |
|---|---|---|---|
| Place Town Hall, found colony | ✅ `TownHallBlock` + `ColonyManager` | ✅ `TownHallFoundingGameTests` | ✅ block + chat feedback |
| 4 citizens spawn | ✅ `CitizenSpawner` two-phase | ✅ `TownHallFoundingGameTests`, `ColonyRegistrationGameTest` | ✅ |
| Player receives Colony Tool | ✅ `TownHallPlacementListener` + respawn restore | ✅ `ColonyToolGameTests` | ✅ |
| Place Residential Hut | ✅ `ResidenceHutBlock` + interception | ✅ `BuildingPlacementGameTests` | ✅ |
| Paint outer AABB zone | ✅ `ZoneValidator` + `BuildingIndex` | ✅ `PendingPlacementGameTests`, `BuildingPlacementGameTests` | ✅ HUD + wireframe |
| Right-click hut, open Building GUI | ❌ **GUI not implemented** | n/a | ❌ blocker |
| Designate primary bedroom | ✅ `RoomValidator` + `RoomIndex` (server) | ✅ `RoomRequirementGameTests` | ❌ no UI |
| Bed inside bedroom → status VALID | ✅ `RoomRequirementEvaluator` + `TaggedBlockDetector` | ✅ `RoomRequirementGameTests` | ❌ no UI |
| Assign citizen to bedroom | ✅ `CitizenAssignmentService` | ✅ `CitizenSleepGameTests` | ❌ no UI |
| Citizen walks to room at night | ✅ `ColonyGoToHomeRoomAtNightGoal` | ✅ `CitizenSleepGameTests::citizenWithAssignedRoomGoesToRoomAtNight` | n/a |
| Citizen sleeps in bed | ✅ `ColonySleepInBedGoal` | ✅ `CitizenSleepGameTests::citizenSleepsInBedAtNight` | n/a |
| Citizen leaves bed at day | ✅ `EntityCitizen.aiStep` wake handler (fixed during this audit) | ✅ `CitizenSleepGameTests::citizenLeavesBedAtDay` | n/a |
| Save/quit/reload, behavior persists | ✅ SavedData + entity NBT | ✅ `TownHallFoundingGameTests::founderRestartPersists`, `BuildingIndexTest`, `ColonyIndexTest` | n/a |

**Net:** every Phase 1 server-side mechanic is implemented and exercised by GameTests. The Building GUI stack — the single point where a player without commands can drive room designation and home-room assignment — was punted. Two tech-debt entries (`BuildingScreen Structure tab not implemented`, `BuildingScreen Citizens tab not implemented`) record the gap; the surfaces the future GUI will call (`RoomValidator.reevaluate`, `CitizenAssignmentService`, `RoomIndex.allInBuilding`) are all in place.

This is a partial Phase 1. The end-to-end loop is provable only through GameTests, not through a live player session. I am not pretending otherwise.

## What was easier than expected

- **Custom pathfinding scaffolding.** Originally flagged in roadmap risk register as 2–4 months of work. The current `ColonyNodeEvaluator` + `ColonyPathNavigation` + `ColonyAStarPathFinder` are intentionally minimal but already drive citizens through GameTests. No performance scare yet because citizen counts stay at 4.
- **Codec dispatch + datapack-driven detectors.** `FunctionalBlockDetectorRegistry` + `TaggedBlockDetector` + the three builtin JSON profiles (`beds.json`, `doors.json`, `windows.json`) landed cleanly in Month 2. The "register in `:api`, implement in `:common`, expose in `:neoforge`" contract held without friction.
- **Multi-module discipline.** Zero invariant violations in the audit: `:core` has no Minecraft imports, `:api` has no downward imports, `:common` has no `:neoforge` imports. Discipline-by-compiler works exactly as the design pillars claimed.
- **SavedData as the persistence primitive.** `ColonyIndex`, `BuildingIndex`, `RoomIndex`, `CitizenAssignmentIndex` all share the same shape: codec-based encode, single overworld-scoped instance, codec round-trip tested. Adding a new index is a 60-line copy of the previous one.
- **GameTest reach.** 29 GameTests across 9 files cover founding, spawning, persistence, tool cycling, placement, painting, room evaluation, and sleep — all running on `:neoforge:runGameTestServer` in ~1.5 seconds.

## What was harder than expected

- **The Building GUI.** I underestimated how much of Phase 1 was implicitly UI work. Room designation, room status display, citizen assignment, structure tab, citizens tab — all of these are "trivially server-side, real work client-side." Two consecutive Month-2/Month-3 prompts (Structure tab, Citizens tab) ended with the same conclusion: `BuildingScreen` doesn't exist; deferring to a dedicated GUI prompt. The GUI prompt never happened in Phase 1.
- **Wake-on-day.** The naive `ColonySleepInBedGoal` only wakes citizens it put to sleep itself, because `canUse()` short-circuits on `isSleeping()` and `canContinueToUse()` is therefore never invoked on externally-induced sleep. The bug only surfaced when `CitizenSleepGameTests::citizenLeavesBedAtDay` ran the audit-time end-to-end pass. Fixed by adding a one-method wake handler to `EntityCitizen.aiStep()`. I underestimated how easy it is for goal-based logic to claim ownership it doesn't have.
- **Doc-vs-code drift.** `docs/04-BUILDING-SYSTEM.md` describes `AxisAlignedOuterZone` as a record with six directional offsets relative to the Hut; the actual implementation uses two corners (`min`, `max`) — simpler, fits the codec better. This is now a `FIXED-IN-CODE` debt entry waiting for a doc update.
- **Save versioning.** CLAUDE.md and `docs/18-SAVE-VERSIONING.md` declare that *every* persisted record carries a `dataVersion` field, with migrations in `:common/persistence/migration/steps/`. Reality: only `ColonySnapshot` has `dataVersion`. `BuildingMetadata`, `RoomIndex.Entry`, `CitizenAssignmentIndex.Entry` do not. The migration steps directory does not exist. Either the policy bends to "the colony root snapshot is versioned, descendants ride along", or every index gains a version field. Tracked as a new tech-debt entry below.

## Design decisions that held up well

- **Server authority.** Every C2S interaction (`CycleColonyToolModePayload`, `ConfirmZonePaintingPayload`, `CancelPendingPlacementPayload`, `RegisterColonyPayload`, `SubscribePayload`/`UnsubscribePayload`) routes through a typed `CustomPacketPayload` validated server-side. No client mutates server state. The client mirrors (`PendingPlacementClientState`, `ZonePaintingClientState`) are read-only by construction.
- **Codec everywhere.** Every persisted record uses `RecordCodecBuilder.create(...)`. There is no raw `tag.putString("name", ...)` in `:common`. `tag.put(key, encoded.getOrThrow())` is the universal pattern. Survival across save/load is "free" once the codec exists.
- **Tag-driven functional block detection.** Beds, doors, windows are detected by querying block tags (`#minecraft:beds`, `#minecraft:doors`, `#colony:window`) rather than instanceof checks. This held up *exactly* as the data-driven pillar promised: every modded bed becomes a valid bed without code changes.
- **Custom citizens, not villagers.** `EntityCitizen extends PathfinderMob` from day one. Zero pressure to fall back to `Villager` even when it would have been faster, because the design pillar said no. Phase 2 jobs will need this room — villager profession hooks would have been the wrong path-of-least-resistance.
- **Two-phase spawning.** `CitizenSpawner`'s synchronous founding-tick attempts + retry queue cleanly handles "Town Hall placed in a 2×2 air pocket" without race conditions. GameTest `placingTownHallInBlockedSpaceQueuesSpawnAndEventuallyTimesOut` exercises both the queue and the giveup path.

## Design decisions that had to be revisited

- **`AxisAlignedOuterZone` shape.** Spec said six directional offsets tethered to the Hut. Implementation uses two corners. The two-corner form is strictly better for overlap math and codec simplicity. The doc lost.
- **PendingPlacement persistence.** `docs/04-BUILDING-SYSTEM.md` line 41 promises `/colony resume` after disconnect; today the state is an in-memory `ConcurrentHashMap` that drops on disconnect. The doc claim is currently a lie. Two paths: implement persistence, or weaken the doc to "transient." Tracked.
- **Room status persistence.** I initially considered persisting `RoomStatus` in `RoomIndex.Entry`. Decided not to — re-deriving via `RoomValidator.reevaluate(level, room)` is cheap and avoids the "saved status is stale because someone removed the bed" problem. Worked out. Documented in `RoomIndex` Javadoc.

## Unexpected tech debt

The audit surfaced four debt items not previously catalogued. They are added to `docs/10-TECH-DEBT.md`:

1. **`dataVersion` field is on `ColonySnapshot` only.** All other persisted records (BuildingMetadata, RoomIndex entries, CitizenAssignmentIndex entries) skip it. The migration framework directory in `:common/persistence/migration/steps/` does not exist. The CLAUDE.md "every persisted record" rule is wider than reality.
2. **GUI stack absent.** `BuildingScreen` and the entire Building GUI hierarchy were assumed by two Month-2/Month-3 features and not implemented. Already catalogued in two existing entries; calling out the systemic pattern.
3. **Wake-on-day was goal-coupled.** Fixed during this audit (the fix landed before tag). Adding because the failure mode — AI goal owning a side effect without owning the inverse — is something to watch for as more goals land in Phase 2.
4. **No automated check for design-pillar drift.** The audit found `:core` clean, `:api` clean, `:common` clean by Grep. None of this is checked by CI. A simple `./gradlew :core:checkInvariants` task that fails if any non-allowed import sneaks in would prevent silent regression. Tracked as a Phase 2 chore.

## Risks remaining for Phase 2

- **GUI debt blocks the player-facing loop.** Phase 2 ships the Farmer job. Without `BuildingScreen`, the player has no way to designate the Farmer Hut's Office room. Phase 2 cannot start gameplay loops without unblocking the GUI work. **Highest risk.**
- **Pathfinding has not been profiled.** Current GameTests use 1–4 citizens over short distances. Roadmap calls for the first real profile at month 6. If `ColonyAStarPathFinder` is naive in a way that breaks at 30 citizens, Phase 2 will discover it the worst possible way — mid-feature.
- **PendingPlacement non-persistence is now louder.** Phase 2 introduces more building types (Farmer Hut). Each one will inherit the same in-memory placement workflow. If a player disconnects mid-zone-paint of a Farmer Hut, they lose the workflow with no recovery. Acceptable for alpha but irritating; ship `/colony resume` in Phase 2.
- **Save format will move.** `dataVersion` discipline is unenforced for new records. The first time a Phase 2 prompt removes/renames a field on `RoomIndex.Entry`, existing alpha saves will fail to load with a hard codec exception rather than triggering a migration. Either enforce the rule in Phase 2 *before* any field change, or commit publicly to "alphas don't preserve saves."
- **No flood-fill enclosure check.** A player can register a 1×1×1 column of stone as a Building's outer zone today. Phase 2's tier evaluator will silently award tier 0 forever on these zones. Either implement flood-fill or document that tier 1+ requires manual override until then.
- **`:testmod` SPI hole detector is disarmed.** Until the `ColonyAddon` SPI exists in `:api`, `:testmod` carries full NeoForge access and the "fail the build if NeoForge leaks through `:api`" mechanism is dormant. Catch-22: the addon SPI prompt has not been written yet. Phase 2 should land at least a stub `ColonyAddon` interface so the detector arms.

## I should have asked clarification on…

- **The Building GUI prompt timing.** Two consecutive feature prompts deferred to "a dedicated GUI prompt" without specifying when that prompt fires. I assumed it would land before Phase 1 closed. I did not push back. The right move was: *"two features are now blocked on the GUI; should the GUI prompt run before the next feature prompt, or are we ending Phase 1 with the GUI deferred?"*
- **The smoke-test runner.** The user prompt asks to "watch citizen walk to bedroom and sleep" through a Minecraft client session. There is no client session available in a headless audit. I should have flagged this constraint upfront and offered the GameTest path as the substitute, rather than producing the smoke-test step list and the GameTest mapping as if they were the same thing.

## What I underestimated

- **How much of "Phase 1 done" lives in client UI, not server logic.** The roadmap document organizes Phase 1 by server-side capability (entity, spawning, pathfinding, painting, validation). The player experience hinges on the GUI surface. The two diverged.
- **The cost of layered goals interacting through entity state.** `ColonyGoToHomeRoomAtNightGoal` walks the citizen home; `ColonySleepInBedGoal` puts the citizen in bed. Both are triggered by *the same* time condition, with the same `isNight()` helper duplicated. Vanilla solves this with `Behavior` + `Activity` + memory modules; we're using raw `Goal`s. As Phase 2 adds more bedtime-adjacent behaviors (sleep-disrupted-by-monster, wake-to-eat, etc.), the duplication will sting.
- **How much GameTest coverage matters in a no-UI build.** With no `BuildingScreen`, the only way to verify "citizen sleeps in assigned bedroom" is `CitizenSleepGameTests`. That test caught a wake bug that would have been invisible until a player tried it. **GameTests are not a "nice to have" in this project's structure; they are the primary feedback loop.**

## What worked about the AI-assisted workflow

- **Audit prompts catch things commits don't.** Each commit on its own looked complete. The audit pass surfaced: (a) two tech-debt entries that recorded GUI gaps but no one ever read them together, (b) the wake-on-day bug, (c) the `dataVersion` rule erosion. The pattern: *prompts that land features* and *prompts that audit features* are different shapes of work; both are needed.
- **Tech-debt discipline.** Forcing every prompt to either implement or write a debt entry kept `docs/10-TECH-DEBT.md` from drifting into fiction. 13 entries, all real, all traceable.
- **GameTests as a contract.** Each feature prompt landed its own GameTests in the same commit. That's why the audit could verify "Phase 1 server logic works" by running the test server instead of trusting commit messages.

## Concrete next moves before Phase 2 starts

1. **Land the Building GUI.** Until then, Phase 2 (Farmer job) is provable only through GameTests, same as Phase 1. The alpha public release at end of Phase 2 cannot ship without UI.
2. **Pick a save-versioning posture.** Either widen `dataVersion` to every persisted record + scaffold `:common/persistence/migration/steps/`, or narrow the doc to "the colony root snapshot is versioned; descendants accept breaking changes during alpha."
3. **Update `docs/04-BUILDING-SYSTEM.md`** to match the implemented `AxisAlignedOuterZone` shape (two corners, not six offsets).
4. **Add a `./gradlew :verifyModuleBoundaries` task** that enforces the import rules. Today they hold because we re-grep them in audits; that is not sustainable.
5. **Profile pathfinding before adding more citizens.** Even a 30-citizen synthetic load test would surface whether the naive A* survives Phase 2 contention.

---

*This document is a snapshot of the state at v0.4.0-alpha. It is not a plan — `docs/08-ROADMAP.md` is. It is not a debt register — `docs/10-TECH-DEBT.md` is. It is a record of what Phase 1 actually felt like, so Phase 2 can be calibrated against reality rather than against the optimism that started Phase 1.*

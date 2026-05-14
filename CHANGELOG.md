# Changelog

All notable changes to Colony are recorded here. Format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); the project follows
[Semantic Versioning](https://semver.org/spec/v2.0.0.html). Pre-V1.0 pre-release
suffixes are used per `docs/19-RELEASE-COMMUNICATION.md`.

## [0.4.0-alpha] - 2026-05-14

**Phase 1 milestone: the server-side core loop is complete and validated.**

A citizen with an assigned home bedroom now walks to the room at sunset, sleeps
in a bed inside the room, and wakes at sunrise — including the failure case
where sleep was entered through any path other than the bedtime goal itself.
Combined with Month 1 (Town Hall + citizen spawning + persistence) and Month 2
(Colony Tool + Building placement + AABB zone painting), the full Phase 1
exit criterion is met on the server side. Player-facing UI for room
designation and home-room assignment is intentionally deferred to Phase 2
month 4 (tracked in `docs/10-TECH-DEBT.md`); the server surfaces the GUI will
call are all in place.

See `docs/phase-1-retrospective.md` for the honest post-mortem and
`docs/phase-1-smoke-test.md` for the per-step pass/fail audit.

### Added
- `FunctionalBlock` + `FunctionalBlockDetector` SPI in `:api`: pluggable block
  detection for room requirements.
- `TaggedBlockDetector` in `:common` plus three datapack-driven builtin
  profiles (`data/colony/functional_block_detector/{beds,doors,windows}.json`).
  Any modded bed/door/window joins the registry without a code change.
- `RoomType` + `RoomRequirement` interfaces (`:api`), `BedroomType` builtin
  (`:common`).
- `Room`, `RoomId`, `RoomStatus`, `FreeformZone` core types.
- `RoomValidator`, `RoomRequirementEvaluator`, `RoomIndex` SavedData. Room
  status is re-derived on demand rather than persisted; bed lookup uses the
  detector registry.
- `Citizen.assignedHomeRoom()` API extension. `CitizenAssignmentService` in
  `:api` plus `CitizenAssignmentServiceImpl` + `CitizenAssignmentIndex`
  SavedData in `:common`. Home-room persists on the entity NBT *and* in the
  inverse-lookup index.
- `ColonyGoToHomeRoomAtNightGoal` (priority 5) walks the citizen to the
  assigned room's bed at sunset.
- `ColonySleepInBedGoal` (priority 4) snaps the citizen onto an unoccupied
  bed inside the room. Vanilla `LivingEntity.startSleeping` handles pose +
  bed-occupied flag.
- `EntityCitizen.aiStep()` wake handler: any citizen sleeping at daytime is
  woken regardless of how it entered sleep. Caught by
  `CitizenSleepGameTests::citizenLeavesBedAtDay` during the audit pass and
  fixed pre-tag.
- 7 new GameTests (`RoomRequirementGameTests` × 4, `CitizenSleepGameTests`
  × 3). 29 GameTests total now passing on `:neoforge:runGameTestServer`.
- 11 new unit tests (`RoomRequirementEvaluatorTest`,
  `CitizenAssignmentServiceTest`). 110 unit tests total green across
  `:core` / `:api` / `:common`.
- `docs/phase-1-retrospective.md`: post-mortem.
- `docs/phase-1-smoke-test.md`: step-by-step audit results.
- Tech-debt entries: `dataVersion` discipline applies only to
  `ColonySnapshot`, goal-coupled wake (now FIXED-IN-CODE), no automated
  module-boundary check.

### Changed
- `docs/08-ROADMAP.md` reordered: Phase 2 now starts with the Building GUI
  prompt-group, ahead of Farmer-job content, because the GUI gap blocks the
  player-facing Phase 1 loop. Save-versioning posture decision is also
  moved up to Phase 2 month 4 as a precondition for any new persisted
  record.

### Save format
- Adds `colony_room_index` and `colony_citizen_assignments` SavedData.
- `EntityCitizen` NBT gains optional `AssignedHomeRoom` UUID field.
- 0.3.0-alpha saves load cleanly (no rooms, no assignments, empty indices).
  Pre-V1 alphas still do not guarantee forward compatibility.

### Known limitations
- No in-world UI for room designation or home-room assignment. Both are
  reachable only through the server-side surfaces today. See
  `docs/10-TECH-DEBT.md` "BuildingScreen Structure tab" and
  "BuildingScreen Citizens tab".
- `dataVersion` is only on `ColonySnapshot`; new SavedData records lack it.
  Any field-removing change in Phase 2 will fail to load existing alpha
  saves. Tracked.

## [0.3.0-alpha] - 2026-05-14

Phase 1 Month 2 milestone: the Building System lands. A player with a Town Hall
can pull out a Colony Tool, switch between four modes, intercept a Hut block
right-click, and paint an axis-aligned outer zone to register a Building.

### Added
- `ColonyToolMode` enum (`Zone`, `Storage`, `Link`, `Inspect`) backed by a
  data component; shift+scroll cycles through modes, server-validated, rate-limited.
- HUD overlay showing the current Colony Tool mode plus per-mode hint text;
  pending-placement HUD with volume readout and corner-state guidance.
- `/colony wand restore` (refunds a Colony Tool if lost) and `/colony info`
  (lists colonies and their buildings). Respawn-time top-up retained from Month 1.
- `HutType` SPI in `:api` plus first concrete `ResidenceHutType` registered
  through `ColonyBootstrap`.
- `ResidenceHutBlock` + `ResidenceHutBlockItem` intercepting placement: with
  no Colony Tool, the player gets a translated chat error; otherwise the
  server enters `PendingPlacement` state and the client renders a ghost preview.
- `PendingPlacementManager` (in-memory, per-player) tracking the placement
  workflow; `ConfirmZonePaintingPayload` and `CancelPendingPlacementPayload`
  C2S payloads driving the lifecycle.
- `AxisAlignedOuterZone` record with `contains`, `volume`, `blocksInZone`,
  `overlaps`, and a normalizing `fromCorners` factory. Persisted via `Codec`.
- `ZoneValidator` returning a sealed `ZoneValidationResult` (`Valid`/`Invalid`)
  with five `ZoneValidationError` variants — `TooSmall`, `TooLarge`,
  `DoesNotContainHutPos`, `OverlapsExistingBuilding`, `OutsideLoadedChunks`
  — each mapped to a translated chat key.
- `BuildingIndex` `SavedData` mirroring `ColonyIndex`: register, find,
  `allInColony`, `findByPosition`, `hasOverlap`, codec-based persistence.
- Two-click zone painting on the client with real-time AABB wireframe;
  color-coded green/yellow/red by local approximate validity.
- GameTests: 5 in `BuildingPlacementGameTests`, 3 in `PendingPlacementGameTests`,
  2 in `ColonyToolGameTests`. 22 GameTests total now passing.
- Unit tests: `AxisAlignedOuterZoneTest`, `ColonyToolModeTest`,
  `BuildingIndexTest` (incl. save/reload round-trip), `PendingPlacementManagerTest`,
  `ZoneValidatorTest`. 99 unit tests total across `:api` / `:core` / `:common`.
- Tech-debt entries: PendingPlacement non-persistence, missing enclosure
  flood-fill, dimension-blind BuildingIndex, single-pass validation pipeline,
  doc-vs-code drift on `AxisAlignedOuterZone` shape.

### Changed
- `.gitignore` widened from `common/logs/` to `**/logs/` (covers test-run
  log dirs that show up in any module).

### Save format
- Adds `colony_building_index` SavedData. 0.2.0-alpha saves load cleanly (no
  buildings, empty index); pre-V1 alphas still do not guarantee forward
  compatibility, but the placement format is stable for the duration of Phase 1.

## [0.2.0-alpha] - 2026-05-14

Phase 1 Month 1 milestone: a player can place a Town Hall, found a colony, and
watch four citizens spawn around it. Citizens persist across save/reload and
walk under a custom (currently scaffolded) pathfinder.

### Added
- `EntityCitizen` custom mob (extends `PathfinderMob`, not `Villager`), with
  identity, colony affiliation, and NBT persistence.
- `ColonyPathNavigation` + `ColonyNodeEvaluator` scaffolding so citizens path
  through Colony's evaluator rather than vanilla's `GroundPathNavigation`.
- `ColonyAStarPathFinder` (unit-tested) supplying the A* core for the custom
  navigation.
- Town Hall block + Colony Tool item; right-click flow founds a colony at the
  block position, sends "Colony founded!" feedback, and gives the player a
  Colony Tool.
- `ColonyManager`, `ColonyIndex`, `ColonySnapshot`, and per-colony NBT
  persistence under `world/data/colony/colonies/{uuid}.nbt`.
- `CitizenSpawner` two-phase spawner (synchronous founding-tick attempts plus
  a tick-driven retry queue) with `CitizenSpawnTicker` wiring on `ServerTickEvent.Post`.
- `RegisterColonyValidator` + `RegistrationRateLimiter` to gate concurrent
  founding requests.
- GameTests covering Town Hall founding, save/reload persistence, blocked-spawn
  retry/timeout behavior, citizen pathing, and colony registration. 12 GameTests
  total, all passing on `:neoforge:runGameTestServer`.
- Unit tests: 27 in `:core`, 41 in `:common` (68 total, all passing).
- Tech debt register entries documenting EntityCitizen's stub state model,
  spawn-search shortcuts, `:testmod` posture, and citizen name pool source.

### Changed
- `CitizenSpawner.pickCandidate` now performs a deterministic vertical walk
  from `townHall.y` outward for each randomly-chosen `(dx, dz)` column,
  replacing uniform 3-D random sampling. Eliminates intermittent
  `placingTownHallFoundsColonyAndSpawns4Citizens` GameTest failures in
  thin air-pocket test rigs.

### Save format
- New format introduced for V1. Pre-V1 alphas do not guarantee save
  compatibility; expect to start fresh worlds between alpha versions.

## [0.1.0-alpha.1]

Initial template scaffolding (multi-module Gradle, NeoForge 1.21.1, build
conventions, documentation set). Pre-release; no player-facing functionality.

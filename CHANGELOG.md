# Changelog

All notable changes to Colony are recorded here. Format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); the project follows
[Semantic Versioning](https://semver.org/spec/v2.0.0.html). Pre-V1.0 pre-release
suffixes are used per `docs/19-RELEASE-COMMUNICATION.md`.

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

# Changelog

All notable changes to Colony are recorded here. Format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); the project follows
[Semantic Versioning](https://semver.org/spec/v2.0.0.html). Pre-V1.0 pre-release
suffixes are used per `docs/19-RELEASE-COMMUNICATION.md`.

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

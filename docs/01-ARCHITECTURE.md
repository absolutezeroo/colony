# 01 — Architecture

## Overview

Colony uses **Gradle multi-module** to enforce architectural boundaries at compile time. Discipline alone (Hearthbound's approach) is insufficient — the compiler must refuse violations.

## Module structure

```
colony/
├── build-logic/                       Convention plugins for Gradle (DRY)
│   ├── settings.gradle
│   └── src/main/groovy/
│       ├── colony.java-conventions.gradle      JDK 21, JSpecify, Error Prone, NullAway, Spotless, Checkstyle
│       ├── colony.mod-conventions.gradle       ModDevGradle config, run configs, datagen
│       └── colony.publish-conventions.gradle   Maven publishing for :api
│
├── core/                              Java pure. No Minecraft. No NeoForge. JUnit-testable headless.
│   └── src/main/java/com/akikazu/colony/core/
│       ├── event/                     EventBus, Event, Subscribe, CancellableEvent
│       ├── registry/                  Identifier, Registry<T>, RegistryKey<T>, RegistryView<T>
│       ├── scheduler/                 TickScheduler, TickBudget, ScheduledTask
│       ├── codec/                     CodecHelpers, VersionedCodec, MigrationChain
│       ├── math/                      ColumnPos, BlockBox (POJOs, no vanilla)
│       └── package-info.java          @NullMarked at package level
│
├── api/                               Interfaces. Compiled against vanilla MC only via MDG vanilla-mode.
│   └── src/main/java/com/akikazu/colony/api/
│       ├── colony/                    Colony, ColonyId, ColonyView, ColonySettings
│       ├── citizen/                   Citizen, CitizenTrait, CitizenTraitType, CitizenIntent
│       ├── building/                  Building, BuildingType, BuildingModule, BuildingZone
│       │   ├── hut/                   HutType, HutTier
│       │   ├── room/                  Room, RoomType, RoomRequirement, RoomQualityInput
│       │   └── slot/                  BuildingSlot, StorageSlot, RoomSlot, AnchorSlot
│       ├── workzone/                  WorkZoneAnchor, AnchorConfiguration, AxisAlignedZone, FreeformZone
│       ├── job/                       Job, JobType, JobBehavior, JobContext
│       ├── storage/                   Storage, StorageRole, StorageFilter
│       ├── event/                     ColonyEvents (ColonyCreatedEvent, CitizenHiredEvent, ...)
│       ├── addon/                     ColonyAddon, ColonyAddonContext
│       └── registry/                  ColonyRegistries (typed accessors)
│
├── common/                            Implementation logic. Sees NeoForge, but loader-agnostic when feasible.
│   └── src/main/java/com/akikazu/colony/common/
│       ├── colony/impl/               ColonyImpl, ColonyManagerImpl
│       ├── citizen/impl/              CitizenImpl, citizens traits builtin
│       │   ├── pathfinding/           ColonyNodeEvaluator, ColonyPathNavigation
│       │   └── ai/                    BehaviorTree, IntentQueueProcessor
│       ├── building/impl/             BuildingImpl, BuildingModules, slot resolvers
│       ├── workzone/impl/             AnchorRegistry, ZoneIndex (R-tree spatial)
│       ├── job/impl/                  JobBehaviors builtin (idle, farmer)
│       ├── persistence/               Codecs, MigrationChain, ColonyPersistenceCoordinator
│       │   └── migration/steps/       v1_to_v2.java, etc.
│       └── package-info.java          @ApiStatus.Internal @NullMarked
│
├── neoforge/                          Loader-specific glue. The shipped jar.
│   └── src/main/java/com/akikazu/colony/neoforge/
│       ├── ColonyMod.java             @Mod entry point
│       ├── attachment/                ColonyAttachments (DataAttachmentTypes)
│       ├── network/                   ColonyPayloads, ColonyNetwork (CustomPacketPayload)
│       ├── command/                   ColonyCommands, sub-commands
│       ├── client/                    ColonyView client mirror, screens, renderers, HUD overlay
│       ├── entity/                    EntityCitizenRenderer, registrations
│       ├── block/                     HutBlock, AnchorBlock, registrations
│       ├── item/                      ColonyToolItem, registrations
│       ├── datagen/                   Generators (recipes, lang, tags, datapack content)
│       ├── addon/                     AddonLoader (ServiceLoader scan)
│       └── package-info.java          @ApiStatus.Internal
│
└── testmod/                           Test addon. Depends ONLY on :api. Validates SPI.
    └── src/main/java/com/akikazu/colony/testmod/
        └── TestAddon.java             Registers a test JobType, test BuildingType, test trait.
```

## Compile-time invariants

These are enforced by Gradle's dependency graph. Violations fail the build.

| Module | Can import from | Cannot import from |
|---|---|---|
| `:core` | (none) | Minecraft, NeoForge, any other module |
| `:api` | `:core`, vanilla Minecraft | `:common`, `:neoforge`, NeoForge |
| `:common` | `:core`, `:api`, Minecraft, NeoForge | `:neoforge`, `:testmod` |
| `:neoforge` | `:core`, `:api`, `:common`, Minecraft, NeoForge | `:testmod` |
| `:testmod` | `:core`, `:api`, Minecraft, NeoForge | `:common`, `:neoforge` |

`:testmod` deliberately cannot see `:common` or `:neoforge`. If `:testmod` can build a working addon using only `:api`, the addon SPI is validated. If it can't, the SPI has a hole.

## ModDevGradle vs NeoGradle

We use **ModDevGradle (MDG)** rather than NeoGradle Userdev. Rationale:

- MDG is the recommended toolchain as of 2025 (per `docs.neoforged.net`).
- MDG supports subprojects with `mods { sourceSet sourceSets.main }`.
- MDG provides "Vanilla mode" (`neoFormVersion = "..."`) — needed for `:api` to compile against vanilla MC without NeoForge.
- Simpler buildscripts, less boilerplate.

Hearthbound chose NeoGradle Userdev. That works but is legacy. We don't follow.

## Convention plugins

Three plugins in `build-logic/`:

**`colony.java-conventions`** — applied to every module. Defines:
- Java 21 toolchain
- JSpecify `@NullMarked` enforcement
- Error Prone + NullAway
- Spotless (Eclipse formatter with Allman config)
- Checkstyle (custom rules for blank-line-before-return, no-inline-comments)
- JUnit 5 BOM

**`colony.mod-conventions`** — applied to `:neoforge` and `:testmod`. Defines:
- NeoForge dependency via MDG
- Parchment mappings
- Run configurations (client, server, data, gameTestServer)
- Datagen wiring

**`colony.publish-conventions`** — applied to `:api` only. Defines:
- Maven publishing for `colony-api` artifact
- Sources jar + Javadoc jar
- POM metadata (license LGPL-3.0, SCM URL)

## Registries

Every content type is registered. No enums for content. Pattern:

```java
public final class ColonyRegistries
{
    public static final RegistryKey<JobType> JOB_TYPE =
        RegistryKey.of(Identifier.of("colony", "job_type"));

    public static final RegistryKey<HutType> HUT_TYPE =
        RegistryKey.of(Identifier.of("colony", "hut_type"));

    public static final RegistryKey<RoomType> ROOM_TYPE =
        RegistryKey.of(Identifier.of("colony", "room_type"));

    public static final RegistryKey<WorkZoneAnchorType> ANCHOR_TYPE =
        RegistryKey.of(Identifier.of("colony", "anchor_type"));

    public static final RegistryKey<StorageRoleType> STORAGE_ROLE_TYPE =
        RegistryKey.of(Identifier.of("colony", "storage_role_type"));

    private ColonyRegistries()
    {
    }
}
```

Adding a new content family = adding a constant. No switch anywhere in the codebase.

## Codec dispatch for data-driven content

Every polymorphic type has a `MapCodec` referenced from its `Type` registry entry. Dispatch is generic:

```java
public static final Codec<Job> DISPATCH =
    ColonyRegistries.JOB_TYPE
        .byNameCodec()
        .dispatch("type", Job::type, JobType::codec);
```

A JSON file `data/colony/job/lumberjack.json` with `"type": "colony:harvesting"` is decoded by the `JobType` registered under `colony:harvesting`. The codec yields a typed `Job` instance. Implementation-free, schema-free, automatic.

## Event bus separation

Two event buses run in parallel and **never mix**:

- **NeoForge `EventBus`** — vanilla events only (TickEvent, ServerStartingEvent, PlayerEvent, BlockEvent). Subscribed in `:neoforge`.
- **`ColonyEventBus`** (in `:api`) — all domain events (ColonyCreatedEvent, CitizenHiredEvent, BuildingDesignatedEvent, RoomScoredEvent). Subscribed by addons via `ColonyAddonContext`. Fireable from `:core` with no Minecraft dependency.

Domain events are unit-testable in JUnit headless. The bus uses Guava `EventBus` internally for simplicity.

## Persistence architecture

- **One file per colony** at `world/data/colony/colonies/{uuid}.nbt`. Loaded lazily on colony access.
- **`ColonyIndex`** is a singleton `SavedData` listing all known colony UUIDs + dimension + Town Hall position. Lightweight, always loaded.
- **`DataAttachment` on EntityCitizen** for per-citizen state that needs to sync (traits, mood, current intent). Syncable attachment type, backported in NeoForge 1.21.1.
- **`DataAttachment` on chunks** for spatial indexes (which buildings have outer zones touching this chunk).
- **Codec versioned** with `int dataVersion` at root of every persisted record. Migration via `Codec.dispatch` on version.

NeoForge does not provide DataFixerUpper for mods. We maintain our own migration chain in `:common/persistence/migration/steps/`.

## Networking

Uses NeoForge 1.21.1 `CustomPacketPayload` system. Three categories:

- **Snapshot payloads** (server → client): full state for a colony when the player opens its GUI. Heavy, rare.
- **Delta payloads** (server → client): incremental updates while GUI is open. Light, frequent.
- **Command payloads** (client → server): player actions. Validated server-side, never trusted.

All payloads are records implementing `CustomPacketPayload`, with `StreamCodec` for serialization. Registered via `RegisterPayloadHandlersEvent` in `:neoforge`.

## Pathfinding (custom)

Vanilla `PathNavigation` was tested and found insufficient for colony semantics:

- No concept of "claimed zone" (citizens path through other players' buildings).
- No avoidance of unsafe blocks specific to colony (lava in mining zones).
- No route preference for colony pathways vs ad-hoc routes.
- Performance degrades with many entities pathing simultaneously.

Custom approach in `:common/citizen/impl/pathfinding/`:

- **`ColonyNodeEvaluator`** extends `NodeEvaluator`, adds colony-specific cost modifiers and accessibility checks.
- **`ColonyPathNavigation`** wraps `GroundPathNavigation`, swaps the evaluator.
- **Path cache** per (citizen, destination) pair, valid for 60-120 ticks unless world changes detected in path region.
- **Hierarchical pathfinding** (V2): coarse path between rooms, fine path within room.

Estimated cost: 2-4 months solo to reach production-grade. Tracked as risk in roadmap.

## Tick budgeting

Citizens do not tick at 20 Hz. Instead:

- A single `LevelTickEvent.Post` handler in `:neoforge` delegates to `ColonyTickScheduler` in `:core`.
- The scheduler processes `IntentQueue` of citizens in round-robin, with a budget (e.g. 5ms/tick).
- Each citizen "thinks" at 2-5 Hz (planning intents), but renders/animates at 20 Hz (vanilla mob behavior).
- Background tasks (room re-scoring, structural validation) run on a separate threadpool, results applied main thread.

## Performance targets

- Cold colony load (50 citizens, 20 buildings): < 200ms.
- Tick budget per server tick: < 8ms for all colony logic combined.
- Room re-scoring (one room of 100 blocks): < 5ms async, result applied next tick.
- Outer zone validation (1000-column footprint): < 50ms.

Benchmarks live in `:core` and run in CI.

## CI

GitHub Actions:

- `build`: full `./gradlew check` on every push.
- `gametest`: `./gradlew :neoforge:runGameTestServer` validates in-world behavior.
- `style`: `spotlessCheck` + `checkstyleMain` enforces formatting and conventions.
- `publish`: on tag `v*`, publishes `colony-api` to Maven, uploads jars to GitHub Release.

Branch protection on `main`: PRs require CI green + 1 review (when team grows beyond solo dev).

## Why this architecture

It's an explicit response to two failures observed:

- **MineColonies' mono-repo with package separation** → 10+ years of god-classes and circular imports. Refactoring is impossible at this point.
- **Hearthbound's mono-module with package "discipline"** → `api/RoomType.java` already imports `common/HousingTier.java` at MVP-55. The leak is silent, accumulates, and locks them out of publishing `:api` separately.

Multi-module Gradle costs 1 week of setup at project start. It saves months of refactoring at month 12.

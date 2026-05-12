# 02 — Design Pillars

This document lists the **non-negotiable** rules for Colony. They cannot be relaxed without a project-wide RFC and explicit user approval. AI agents and contributors who propose changes that break these rules should be told no.

## 1. Server-authoritative everything

The dedicated server is the source of truth. The client renders state and sends actions.

- All colony state lives on the server.
- The client receives **snapshots** and **deltas** via `CustomPacketPayload`.
- Player actions go server-side as typed C2S payloads. The server validates and applies.
- The client **never** mutates colony/citizen/building/room/storage state directly.
- No client-side caching of state that contradicts the server.
- Dedicated server support is **mandatory from MVP-1**, not retrofitted later.

**Why:** retrofitting server-authority is what kills multiplayer mods. Build it in from day one.

## 2. Zero hardcode for content

No enum for content types. No switch on identifier strings. No "if (building instanceof Tavern)".

- Every content type is registered via `DeferredRegister` and exposed in `ColonyRegistries`.
- Polymorphic types use `Codec.dispatch` for serialization.
- All gameplay-tunable values come from `HearthboundServerConfig` or datapack JSON.
- Tags (`#colony:*`) for compatibility checks.
- Adding a new content variant must be possible **without modifying core code** — only by registering and providing JSON.

**Why:** every hardcoded special case is a future bug, a future refactor, and an addon dev who can't extend the mod.

## 3. Modularity by compiler, not by discipline

Gradle multi-module enforces boundaries. See `01-ARCHITECTURE.md`.

- `:core` cannot import Minecraft.
- `:api` cannot import `:common` or `:neoforge`.
- `:testmod` cannot import `:common` or `:neoforge`.
- Violations fail the build.

**Why:** Hearthbound has `api/RoomType.java` importing `common/HousingTier.java` and didn't notice for 55 MVPs. Discipline-based separation always leaks.

## 4. Free-build first

The default player experience is "build your structure yourself, designate it as functional."

- No prefab schematics shipped with the mod.
- No "Builder reconstructs a level-3 hut" gameplay loop.
- Players paint zones (outer building + interior rooms) with the Colony Tool.
- Quality emerges from what the player built, scored by the mod.

Schematic import via vanilla `.nbt` files is a **V2 convenience feature**, never the primary gameplay.

**Why:** this is the unique differentiator vs MineColonies. If we drift back into schematics, we have no reason to exist.

## 5. Custom citizens

Citizens are `EntityCitizen extends PathfinderMob`. Not `Villager`. Not subclass of vanilla.

- Custom model, custom texture, custom AI.
- Vanilla villagers exist in parallel, untouched. Players can keep their villager trading economy.
- Citizens have traits (typed, registered), skills (per-job XP), mood (modifier stack), peer relationships (V2 sparse graph).

**Why:** vanilla villagers are tightly coupled to professions, raid mechanics, trading, breeding. Hijacking them creates conflicts with every other mod that touches villagers. Better to coexist.

## 6. Custom pathfinding

Vanilla `PathNavigation` was tested by the user and found insufficient for colony semantics. We build our own.

- `ColonyNodeEvaluator` with colony-aware cost (avoid claimed zones, prefer paths).
- Path cache to avoid recomputation.
- Hierarchical pathfinding (coarse → fine) in V2 if needed for performance.

**Cost acknowledged:** 2-4 months of focused work to reach production quality. Tracked in roadmap as a risk.

**Why:** if vanilla pathfinding was good enough, MineColonies wouldn't have 10 years of pathfinding bug reports. The mod's core gameplay depends on citizens being reliable. We can't outsource this to vanilla.

## 7. No god classes

Composition by typed modules. Each module has one responsibility.

- A `Building` has a list of `BuildingModule`. It does not contain residential logic + workshop logic + storage logic.
- A `Citizen` has a list of `CitizenTrait`. It does not have 30 methods for every possible behavior.
- A `Job` has a `JobBehavior` (stateless) and a state record. Behavior is reusable, state is per-instance.

**Why:** MineColonies' `AbstractEntityAIBasic` is the textbook anti-pattern. Don't repeat.

## 8. Codec-versioned persistence

Every persisted record has an `int dataVersion`. Migrations are explicit and versioned.

- No raw NBT reads. Codec only.
- Migrations live in `:common/persistence/migration/steps/v{N}_to_v{N+1}.java`.
- Self-tests for each migration step in `:common/persistence/migration/selftest/`.

**Why:** save format changes will happen. Without migrations, the first breaking change destroys every player's save. Plan for it.

## 9. Public `:api` artifact

`:api` is published to Maven (GitHub Packages initially, possibly Modrinth Maven later) as a separate artifact. Addons depend on `colony-api` only.

- Semver strict on `:api`. MAJOR.MINOR.PATCH.
- `@ApiStatus.Internal` annotated on packages that aren't stable.
- Breaking changes only at MAJOR version, with deprecation cycle.

**Why:** if addons can't depend on a stable API, the ecosystem doesn't grow. MineColonies doesn't publish a real API; their "addons" are forks. We do better.

## 10. Ship early, ship ugly

Public alpha at MVP-15 maximum. Open issues, public Discord, real feedback.

- The first alpha will have 1-2 jobs, 2-3 hut types, no economy, no reputation. **That is fine.**
- The mod must be **playable end-to-end** before any V2 work begins.
- "Polish before release" is forbidden. Polish is a V2 activity once we know what players actually use.

**Why:** Hearthbound is at MVP-55 with zero public alpha. They are building in the dark. We don't.

## Non-goals (explicit)

These will **not** be in Colony, ever (or until we change this document with project-wide approval):

- Multi-loader support (Fabric, Quilt). NeoForge 1.21.1 only.
- Pre-built schematic packs shipped with the mod. Schematics are an optional V2 import feature, not gameplay.
- Vanilla villager integration. Citizens are a separate species.
- 3D freeform interiors (overhangs, balconies counted as room space). 2D footprint + uniform height only.
- Auto-detection of room types based on contents. Player designates explicitly.
- Combat or military in V1.
- Inter-colony trade or diplomacy in V1.
- A mobile companion app, web dashboard, or any external tooling beyond the mod itself.

## When to push back

If anyone (user, contributor, AI agent) proposes a change that breaks one of these pillars:

1. State which pillar is at risk.
2. Quote the specific rule.
3. Ask whether they want to amend this document (project-wide RFC) or drop the proposal.

Pillars are not preferences. Breaking them silently is how Hearthbound and MineColonies ended up where they are.

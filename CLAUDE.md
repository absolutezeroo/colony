# CLAUDE.md — AI Agent Operating Instructions

This file briefs Claude Code (and other AI assistants) for working on the Colony codebase. Read it before any task.

## Project identity

- **Name:** Colony
- **Loader:** NeoForge 1.21.1 only. No Fabric. No multi-version.
- **Namespace:** `com.akikazu.colony`
- **License:** LGPL-3.0-or-later
- **Java:** 21
- **Status:** active development, pre-alpha

## What to read before each task

For any non-trivial implementation task:

1. `docs/01-ARCHITECTURE.md` — to know which module owns the code you'll write
2. `docs/02-DESIGN-PILLARS.md` — to check no rule is being broken
3. `docs/03-CODE-STYLE.md` — formatting and naming, non-negotiable
4. The relevant system doc(s):
   - `04-BUILDING-SYSTEM.md` — for buildings, zones, rooms, anchors, storage
   - `05-CITIZEN-SYSTEM.md` — for citizens, traits, jobs, pathfinding
   - `06-DATA-DRIVEN.md` — for JSON content, codecs, registries
   - `07-NETWORKING.md` — for payloads, snapshots, deltas
   - `11-ECONOMY-V1-V2.md` — for any economy/wallet/treasury concern (V1 stub + V2 frozen)
   - `12-UX-GUI.md` — for any GUI/HUD/overlay work
   - `13-TESTING.md` — for test additions or changes
   - `14-DATAGEN.md` — for new JSON outputs or recipes
   - `15-API-REPUTATION.md` — for V2 reputation API stubs in V1 + V2 implementation
   - `16-API-ROOM-ADJACENCY.md` — for V2 room adjacency API stubs in V1 + V2 implementation
   - `17-API-SCHEMATIC-IMPORT.md` — for V2 schematic import API stubs in V1 + V2 implementation
   - `18-SAVE-VERSIONING.md` — for any persistence work or migration step
   - `19-RELEASE-COMMUNICATION.md` — for release tagging, changelog, announcements

Do **not** read all docs every session. Load only what's relevant to the task.

For V2 API work in V1: read the corresponding `15-*` / `16-*` / `17-*` doc to ensure the no-op stub matches the frozen contract. Modifying the contract requires a project-wide RFC.

Consult official NeoForge 1.21.1 documentation (`docs.neoforged.net/docs/1.21.1/`) before using a NeoForge API. Do not invent APIs from memory — they change between minor versions.

## Non-negotiable rules

**Architecture:**
- Multi-module Gradle. `:core` (pure Java), `:api`, `:common`, `:neoforge`, `:testmod`.
- `:core` cannot import Minecraft. Enforce via Gradle dependency rules.
- `:api` cannot import `:common` or `:neoforge`. Enforce same way.
- Adding a new content type means: register in `:api`, implement in `:common`, expose in `:neoforge`. Never inline content in `:core`.

**Server authority:**
- Client never mutates colony, citizen, hut, room, inventory, request, or task state directly.
- All player actions go through typed C2S `CustomPacketPayload`, validated server-side.
- Snapshots flow server→client. Client renders, never decides.

**Zero hardcode:**
- No enum for content types (jobs, huts, rooms, taxes). All registered via `DeferredRegister` and JSON.
- No switch on identifier strings. Use Codec dispatch.
- All gameplay-tunable values come from `HearthboundServerConfig` or datapack JSON, never inlined.

**Persistence:**
- Codec-based, never raw NBT manipulation.
- Versioned with `dataVersion` field on every persisted record.
- Migrations declared in `:common/persistence/migration/steps/`.

**Custom citizens, not villagers:**
- Citizens are `EntityCitizen extends PathfinderMob`. Never `Villager` subclass.
- Custom pathfinding via `ColonyNodeEvaluator` (vanilla `PathNavigation` tested and insufficient — see `docs/05-CITIZEN-SYSTEM.md`).

## Forbidden patterns

- Inline comments in implementation code. Comments explain intent at method/class level, not line-by-line.
- God classes. Compose by typed modules.
- Reflection. Use registries and SPI.
- Static state. Services are instance-based, registered in DI containers or per-level data.
- Direct `Minecraft.getInstance()` from common/server code.
- Catching `Exception` broadly. Catch specific types.

## Code style summary

Allman braces, blank lines before `return`/`if`, no inline comments. Full rules in `docs/03-CODE-STYLE.md`. Enforced via Spotless + Checkstyle. CI fails on style violations.

## Workflow per task

1. Read the relevant docs (not all).
2. Read existing code that touches the area you'll modify.
3. Write a one-paragraph implementation plan, confirm with user if non-trivial.
4. Implement minimal scope, no scope creep.
5. Add unit tests in `:core` if logic is pure. GameTests in `:testmod` if behavior in-world.
6. Run `./gradlew check` locally. CI must pass.
7. Update `docs/10-TECH-DEBT.md` if you took a shortcut.
8. Commit with conventional commit message: `feat(building): add freeform footprint validation`.

## TODO format

Bad:
```java
// TODO fix later
```

Good:
```java
// TODO(V2-economy): wire treasury credits when wage payment lands.
```

Every TODO must reference a milestone or system. Untargeted TODOs are rejected in PR review.

## When to push back on the user

If the user asks for something that breaks a rule in this file or in `docs/02-DESIGN-PILLARS.md`, say so explicitly. Don't silently comply.

If the user asks for an architectural decision you don't have context for (e.g. "should this be in `:common` or `:neoforge`?"), ask before writing 200 lines of code in the wrong module.

## What this project is NOT

- Not a MineColonies clone. Don't suggest "doing it like MineColonies does."
- Not Hearthbound. Different licensing model, different scope, different module structure.
- Not multi-loader. Don't suggest abstractions for "future Fabric port."
- Not feature-complete. V1 scope is intentionally minimal. See `docs/08-ROADMAP.md`.

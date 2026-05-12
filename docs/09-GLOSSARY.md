# 09 — Glossary

Reference for project-specific terminology and naming conventions. Read before contributing to avoid term drift.

## Core terms

**Colony** — The top-level unit of play. A colony is founded by placing a Town Hall block. It owns Buildings, Citizens, and (V2) economy state.

**Citizen** — A custom entity (`EntityCitizen`) affiliated with a colony. Not a vanilla villager. Has traits, skills, mood, and (when assigned) a home and a job.

**Town Hall** — The central Building of a colony. Founded once per colony. Houses initial citizens until proper residential huts exist.

**Building** — A player-built structure designated as functional by placing a Hut block inside it and painting an outer zone. Composed of rooms, storage chests, and linked anchors.

**Hut block** — The physical block placed by the player to mark a Building's role and trigger the placement workflow. Examples: Town Hall block, Residence Hut block, Farmer Hut block.

**Outer zone** — The spatial extent of a Building, defined by the player at placement. Can be rectangular (AABB) or freeform (2D footprint extruded vertically).

**Room** — A sub-zone inside a Building's outer zone. Painted by the player, assigned a function (bedroom, kitchen, etc.), and validated against the `RoomType` requirements.

**Room slot** — A declared role expected by a `BuildingType` (e.g. a Residence declares `primary_bedroom` as required, `kitchen` as optional). The player fills slots by painting rooms.

**Storage slot** — A declared storage role expected by a `BuildingType` (e.g. a Farmer Hut declares `seeds_input`, `tools`, `harvest_output`). The player fills slots by typing chests via the Colony Tool.

**Anchor** — A block placed outside a Building, marking a work zone. Examples: scarecrow (farming), quarry pillar (mining), lumber post (forestry). Configured by right-clicking with type-specific items.

**Anchor slot** — A declared link expected by a `BuildingType` (e.g. a Farmer Hut declares `fields` slot accepting `colony:anchor/scarecrow`, max count derived from tier).

**Colony Tool** — The unique item handed to the player at colony founding. Single tool with modes (Zone, Storage, Link, Inspect) cycled via shift+scroll. Persistent, non-droppable, non-stackable.

**Zone Wand** — Synonym used informally for the Colony Tool when discussing the Zone mode specifically. The official name is **Colony Tool**.

**Tier** — A derived property of a Building expressing its quality and capacity. V1 has three: Basic, Developed, Established. Tier is derived from structural score, never paid for, never chosen.

**Structural score** — A computed quality value (0.0 to 1.0) for a Building, aggregating structural integrity, material coherence, decoration score, and other validators.

**Functional block** — A vanilla or modded block recognized by the mod as serving a colony purpose (a bed, a workstation, a window). Detected via JSON-defined `FunctionalBlockDetector`.

**Filter** — A JSON-defined set of items (by tag or explicit list) used to constrain what a storage slot accepts or what an anchor processes.

## Architecture terms

**`:core`** — Gradle module containing pure Java with no Minecraft or NeoForge dependency. Unit-testable headless.

**`:api`** — Gradle module containing public interfaces and types. Compiled against vanilla Minecraft only. Published as `colony-api` Maven artifact for addons.

**`:common`** — Gradle module containing implementation logic. Sees Minecraft and NeoForge but tries to stay loader-agnostic where feasible.

**`:neoforge`** — Gradle module containing loader-specific glue, the final jar. Registrations, payloads, GUIs, entry point.

**`:testmod`** — Gradle module containing a test addon. Depends ONLY on `:api`. Validates that the addon SPI is sufficient.

**Snapshot** — A complete state object sent server → client when the player opens a GUI. Heavy, rare.

**Delta** — An incremental update sent server → client while a GUI is subscribed. Light, frequent.

**Command payload** — A typed message sent client → server requesting a state mutation. Validated and applied server-side.

**ColonyView** — The client-side mirror of a colony's state, maintained from snapshots and deltas. Read-only on the client.

**Server authority** — The non-negotiable rule that the dedicated server owns all gameplay truth. Client renders, server decides.

## Citizen terms

**Trait** — A stable characteristic of a citizen, assigned at spawn, immutable. Affects job eligibility, productivity, mood thresholds. Example: `colony:trait/green_thumb`.

**Skill** — A per-job XP and level value, increased by working in that job. Multiplies productivity, unlocks higher job tiers.

**Mood** — A scalar value (V1) or 4-axis vector (V2) representing the citizen's emotional state. Computed from a stack of typed `MoodModifier`s.

**Mood modifier** — A typed contribution to mood, with source, magnitude, decay curve, and expiration. Example: `colony:mood/well_fed` magnitude +10 over 1 in-game day.

**Need** — A consumable state that drives behavior. V1 has hunger and fatigue.

**Job** — The assignment of a citizen to a work Building. Composed of `JobType` (registered) + `JobBehavior` (stateless implementation) + per-citizen state.

**Intent** — A short-term action a citizen executes (move to X, take item from Y, plant crop at Z). Citizens execute intents from an `IntentQueue`.

**Schedule** — The hours during which a citizen works, breaks, and rests. V1 has fixed schedules per job type.

## Identifier conventions

All identifiers follow `namespace:path` format with `lower_snake_case`:

- `colony:job/farmer`
- `colony:hut_type/farmer_hut`
- `colony:room_type/bedroom`
- `colony:tier/basic`
- `colony:trait/green_thumb`
- `colony:mood/well_fed`
- `colony:storage_role/input`
- `colony:anchor/scarecrow`
- `colony:filter/seeds`

Namespaces:

- `colony` — content shipped by the base mod.
- `minecraft` — vanilla content referenced by us.
- `addon_name` — content from third-party addons (mods using `:api`).

## Tag conventions

Tags use `#namespace:category/path`:

- `#colony:filter/seeds` — seeds accepted by farmer-related logic.
- `#colony:additional_seeds` — extension point for modded seeds.
- `#colony:hazard/lava` — blocks considered hazardous in rooms.
- `#minecraft:beds` — referenced for bed detection.

## Naming conventions in code

| Element | Convention | Example |
|---|---|---|
| Package | `lowercase.dotted` | `com.akikazu.colony.api.building.room` |
| Class | `PascalCase` | `BuildingTierEvaluator` |
| Interface (general) | `PascalCase`, no prefix | `BuildingTier` |
| Interface (service) | `PascalCase`, often `*Service` suffix | `BuildingService`, `PathfindingService` |
| Record | `PascalCase` | `ColonySnapshot` |
| Enum | `PascalCase`, members `UPPER_SNAKE` | `PermissionLevel.OWNER` |
| Method | `camelCase`, verb-first | `evaluateTier()`, `findColony()` |
| Field | `camelCase`, noun | `structuralScore`, `citizenCapacity` |
| Constant | `UPPER_SNAKE` | `HUNGER_FEED_THRESHOLD` |
| Type parameter | Single uppercase | `T`, `E`, `K`, `V` |
| Method-local variable | `camelCase`, brief but clear | `tier`, `nextIntent`, `eval` |
| Test method | `behaviorUnderCondition` or `should<X>When<Y>` | `evaluatesTier1WhenStructuralAboveThreshold` |

## Forbidden namings

- `MyClass`, `BaseClass`, `AbstractFoo` (excessively generic without explaining the role)
- `IFoo` (Hungarian-notation-style interface prefix)
- `xxxData`, `xxxObject`, `xxxBean` (vague type suffixes)
- `manager`, `handler`, `processor` without qualification (`InventoryManager` ok, `Manager` not)
- Method-local variables shorter than 2 characters except loop counters (`i`, `j`)

## Forbidden terms

- "Pawn" (RimWorld term, we use "Citizen")
- "Settler" (Hearthbound term, we use "Citizen")
- "Villager" (vanilla MC, we never refer to our citizens this way)
- "Schematic" except when discussing the V2 import feature explicitly
- "Hut" used standalone — always specify which: "Town Hall", "Residence Hut", "Farmer Hut"
- "Mod" when referring to addons consuming our API — use "addon" to disambiguate from the core mod

## Version vocabulary

- **V1** — the first stable release. Scope: see `08-ROADMAP.md`.
- **V2** — the first post-V1 milestone. Adds reputation, economy, taxes, more content.
- **Alpha** — pre-V1 public release, unstable, expected to break saves.
- **Beta** — pre-V1 release candidate, save-stable but feature-incomplete.
- **MVP** — internal increment between releases. We avoid Hearthbound's MVP-1 through MVP-55 numbering; instead we work in 2-week iterations targeted at the phases in the roadmap.

## When in doubt

If a term in code or documentation is ambiguous, propose adding it to this glossary in your PR. Term ambiguity is a project-wide concern, not a per-file decision.

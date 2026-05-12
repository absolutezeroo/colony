# 06 — Data-Driven Content

Every gameplay value, type, and content variant that can be expressed in JSON, is in JSON. Hardcoded values in Java are reserved for invariants and infrastructure.

## Why data-driven

- **Iteration speed.** Tuning numbers without recompiling shortens the design-test-tweak loop.
- **Modpack authors.** They can override behaviors per pack without touching Java.
- **Mod compatibility.** Filters and tags integrate new items (modded seeds, modded tools) automatically.
- **Addon developers.** They contribute new content via JSON + minimal Java registration, not core forks.

## Source hierarchy

Datapack > Server config > Default JSON (shipped in jar).

- **Default JSON** ships in `src/main/resources/data/colony/`. These are the baseline values.
- **Server config** in `world/serverconfig/colony-server.toml` overrides specific values (difficulty knobs).
- **Datapack** in `world/datapacks/{pack}/data/colony/` overrides JSON files. Standard Minecraft datapack loading rules apply.

## JSON locations

Per content category, files live in:

```
data/colony/
├── job/                          JobType profiles
│   └── farmer.json
├── hut_type/                     HutType profiles (Town Hall, Farmer Hut, etc.)
│   └── farmer_hut.json
├── room_type/                    RoomType profiles
│   ├── bedroom.json
│   ├── kitchen.json
│   └── dining_room.json
├── building_type/                BuildingType (= HutType + structural rules + tier definitions)
│   └── residence.json
├── tier_requirement/             Tier requirement variants
│   └── min_volume.json           (not really a variant, but registry for type)
├── functional_block_detector/    Detectors that map blocks to functions
│   ├── beds.json
│   ├── workstations.json
│   └── decorations.json
├── filter/                       Item filters used by storage slots, anchors, etc.
│   ├── seeds.json
│   ├── farming_tools.json
│   └── crops.json
├── anchor_type/                  WorkZoneAnchorType profiles
│   └── scarecrow.json
├── trait/                        CitizenTrait pool
│   ├── strong.json
│   ├── green_thumb.json
│   └── ...
└── difficulty_preset/            Difficulty presets (V2)
    ├── easy.json
    ├── normal.json
    └── hard.json
```

Tags follow vanilla conventions:

```
data/colony/tags/
├── items/
│   ├── seeds.json
│   ├── farming_tools.json
│   └── crops.json
└── blocks/
    ├── beds.json
    ├── workstations/smithing.json
    └── decorations/wall_art.json
```

## Codec dispatch pattern

Every polymorphic type uses `Codec.dispatch`:

```java
public static final Codec<Job> DISPATCH =
    ColonyRegistries.JOB_TYPE
        .byNameCodec()
        .dispatch("type", Job::type, JobType::codec);
```

Each `JobType` provides its own `MapCodec<? extends Job>`. The JSON file references `"type": "colony:harvesting"` and the dispatch decodes via the matching codec.

## Example: a complete JobType profile

`data/colony/job/farmer.json`:

```json
{
  "type": "colony:harvesting",
  "behavior": "colony:tree_chopping_alike",
  "max_workers": 1,
  "required_tools": ["#colony:filter/farming_tools"],
  "required_traits": [],
  "preferred_traits": ["colony:trait/green_thumb"],
  "work_radius": 16,
  "reputation_gain_per_action": 0.0,
  "compatible_anchor_types": ["colony:anchor/scarecrow"],
  "min_housing_tier": "colony:room_tier/basic"
}
```

`type` selects the codec. `behavior` references a `JobBehavior` registered in `:common`. The rest is the schema declared by the `colony:harvesting` codec.

## Example: a tier definition

`data/colony/building_type/farmer_hut.json` (partial):

```json
{
  "id": "colony:building_type/farmer_hut",
  "category": "colony:building_category/production",
  "tiers": [
    {
      "id": "tier_0_unbuilt",
      "rank": 0,
      "structural_threshold": { "min": 0.0 },
      "requirements": [],
      "capacities": {
        "citizens": 0,
        "max_linked_anchors": 0,
        "max_storage_chests_per_slot": 0,
        "unlocked_room_tiers": [],
        "productivity_modifier": 0.0,
        "upkeep_cost_per_day": 0
      }
    },
    {
      "id": "tier_1_basic",
      "rank": 1,
      "structural_threshold": { "min": 0.20 },
      "requirements": [
        { "type": "colony:tier_req/min_volume", "value": 48 },
        { "type": "colony:tier_req/required_room_slots", "slots": ["office"] },
        { "type": "colony:tier_req/structural_integrity", "roof": true, "foundation": true }
      ],
      "capacities": {
        "citizens": 1,
        "max_linked_anchors": 1,
        "max_storage_chests_per_slot": 1,
        "unlocked_room_tiers": ["colony:room_tier/basic"],
        "productivity_modifier": 0.65,
        "upkeep_cost_per_day": 1
      }
    }
  ]
}
```

Tier requirements use `Codec.dispatch` on `"type"`. Adding a new requirement type means registering a new `TierRequirementType` with its codec.

## Example: a filter

`data/colony/filter/seeds.json`:

```json
{
  "matches": {
    "tags": ["#minecraft:seeds", "#colony:additional_seeds"],
    "items": [
      "minecraft:potato",
      "minecraft:carrot",
      "minecraft:beetroot_seeds"
    ]
  }
}
```

The `#colony:additional_seeds` tag is provided as an empty tag in the default data; modpacks add modded seeds by appending to that tag without overriding the filter JSON.

## Example: a functional block detector

`data/colony/functional_block_detector/beds.json`:

```json
{
  "type": "colony:tagged_blocks",
  "function": "colony:bed",
  "tag": "#minecraft:beds",
  "access_requirements": {
    "min_walkable_adjacent": 2,
    "min_clearance_above": 2,
    "max_adjacent_density_of_same_type": 0.25
  },
  "base_quality": 1.0,
  "quality_modifiers": [
    { "block_tag": "#colony:plush_beds", "quality_multiplier": 1.4 }
  ]
}
```

The detector iterates blocks in a Building's zone, identifies beds via the `#minecraft:beds` tag, applies access requirements, and produces `FunctionalBlock` records consumed by room scoring.

## Schemas and validation

Every JSON file is loaded via a `Codec<T>` that performs strict validation. Malformed JSON:

- Generates an `ERROR` log entry with file path, parse error, and missing/extra fields.
- The content is skipped (not loaded). Other content continues to load.
- A summary report at server start lists all failed loads.

This prevents one bad JSON from breaking the entire mod.

## ReloadableResourceManager

JSON content is reloaded on `/reload` (vanilla command) via `AddReloadListenerEvent` in `:neoforge`. The reload:

1. Validates all JSON files.
2. Replaces in-memory registries atomically (swap pattern, not in-place mutation).
3. Re-evaluates affected runtime state (buildings re-scored if their referenced types changed).
4. Reports success or failure to the player via chat.

## Conditions and compat

NeoForge `conditions` block is supported on every JSON:

```json
{
  "neoforge:conditions": [
    { "type": "neoforge:mod_loaded", "modid": "create" }
  ],
  "type": "colony:harvesting",
  "behavior": "colony:millstone_alike",
  ...
}
```

This file only loads if Create is present. Used for compat profiles bundled with the mod (when integrating with Create, Farmer's Delight, Mekanism, etc.).

## Provided baseline content (V1)

Shipped in the default datapack:

- 5 `RoomType`s: bedroom, kitchen, dining_room, office, storage_room.
- 3 `BuildingType`s: town_hall, residence, farmer_hut.
- 2 `JobType`s: builder, farmer.
- 1 `AnchorType`: scarecrow.
- 10 `CitizenTrait`s.
- ~15 `FunctionalBlockDetector`s for beds, doors, windows, workstations, decorations, cooking, storage.
- Filters for seeds, crops, farming_tools.
- Tier requirements: min_volume, required_room_slots, structural_integrity, material_coherence, material_tier.

V2 expands these significantly. V1 is the minimum to demonstrate the system.

## What's hardcoded (and why)

Some things stay in Java because JSON would be inappropriate or unnecessary:

- **Codec implementations themselves** — they're code.
- **Pathfinding algorithms** — algorithmic, not tunable.
- **Tier evaluation logic** — the algorithm is fixed, only the requirements are JSON.
- **Network payload structure** — defined by records, not data.
- **GUI layouts (V1)** — Java code. BlockUI XML in V2 if we adopt it.
- **The list of registry types themselves** — `ColonyRegistries.JOB_TYPE` etc. are static constants. Java.

The rule of thumb: **structure in Java, parameters in JSON**.

## What's NOT in V1 datapack

- Recipe modifications for vanilla items.
- Custom world generation for colony-friendly biomes.
- Quest or progression content (no quest system in V1 or V2 planned).
- Visual styling (textures, models) — those are resource pack territory, separate.

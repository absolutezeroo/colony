# 04 — Building System

This document specifies how buildings, zones, rooms, anchors, storage, and tiers work in Colony.

## Conceptual model

```
Colony
└── Building (outer zone, contains a Hut block)
    ├── Rooms (sub-zones inside the outer zone, one function each)
    ├── Storage chests (typed, inside the outer zone)
    └── Linked Anchors (outside the outer zone, configured by right-click)
```

A **Building** is a structure the player constructed and designated as functional by placing a Hut block.
A **Room** is a sub-area inside a Building, painted by the player, with one assigned function (bedroom, kitchen, dining, etc.).
A **Storage chest** is a vanilla chest within the Building's outer zone, typed by right-click to a specific role.
An **Anchor** is a marker placed outside the Building (scarecrow, quarry pillar) that defines a work zone.

## Placement workflow

When the player right-clicks with a Hut block (e.g. `colony:farmer_hut`):

1. The server intercepts the placement event.
2. If the player does not have a Colony Tool in their inventory, the placement is cancelled and a chat message instructs them to recover it via `/colony wand restore`.
3. Otherwise, the server enters `PendingPlacement` state for this player:
   - The Hut block remains in the inventory (not consumed yet).
   - A ghost preview of the Hut renders semi-transparently at the target position.
   - HUD instructions appear: "Designate this hut's zone, then press Enter to confirm."
   - The Colony Tool auto-activates in `Zone` mode.
4. The player paints the outer zone (rectangular or freeform footprint, see below).
5. Real-time validation:
   - The outer zone must contain the target Hut position.
   - The outer zone must not overlap with any other Building.
   - The outer zone must enclose at least one valid interior space (flood-fill yields a non-trivial connected air region).
6. The player presses Enter:
   - On success: the Hut block places automatically, the outer zone is registered, the Building is created with `unbuildedTier`.
   - On failure: the error is displayed; the player can adjust and re-attempt.
7. The player presses Esc: full cancellation. No state change.

If the player disconnects during `PendingPlacement`, the state is saved and resumable via `/colony resume` at reconnect.

## Outer zone types

Two variants of the `OuterZone` sealed interface:

### AxisAlignedOuterZone (default, rectangular)

Defined by six integer offsets from the Hut block position: north, south, east, west, up, down. Trivial to paint (two clicks: first corner, second corner) and validate (`BlockBox.contains(pos)` is O(1)).

```java
public record AxisAlignedOuterZone(
    UUID id,
    BlockPos hutBlockPosition,
    int north, int south, int west, int east,
    int up, int down)
    implements OuterZone
{
    @Override
    public boolean contains(BlockPos pos)
    {
        return boundingBox().contains(pos);
    }
}
```

### FreeformOuterZone (optional, footprint-based)

Defined by a 2D footprint (set of `ColumnPos` representing (X, Z) coordinates) and a vertical extent (`bottomY` to `topY`). The footprint is painted column by column; the zone is the footprint extruded vertically.

```java
public record FreeformOuterZone(
    UUID id,
    BlockPos hutBlockPosition,
    ImmutableSet<ColumnPos> footprint,
    int bottomY, int topY)
    implements OuterZone
{
    @Override
    public boolean contains(BlockPos pos)
    {
        if (pos.getY() < bottomY || pos.getY() > topY)
        {
            return false;
        }

        return footprint.contains(new ColumnPos(pos.getX(), pos.getZ()));
    }
}
```

This supports L-shape, T-shape, U-shape buildings. The player switches between rectangular and freeform mode with shift+scroll on the Colony Tool.

**Constraint:** the footprint must be topologically connected (no orphan column blobs). Validated at confirmation.

**Why not 3D freeform:** painting in 3D with overhangs and balconies is significantly harder UX. 2D footprint + uniform height covers ~95% of buildings cleanly.

## Building tiers (3 paliers in V1)

The tier is derived from the structural quality of the Building. **It is not chosen by the player. It is not paid for. It is what the structure deserves.**

V1 has three tiers:

| Tier ID | Display | Structural min | Notes |
|---|---|---|---|
| `colony:tier/0_unbuilt` | Unbuilt | 0.0 | Hut block placed, but structure too poor to function |
| `colony:tier/1_basic` | Basic | 0.20 | First functional tier |
| `colony:tier/2_developed` | Developed | 0.50 | Mid-game |
| `colony:tier/3_established` | Established | 0.75 | High investment |

Each tier exposes capacities:

```java
public record TierCapacities(
    int citizenCapacity,
    int maxLinkedAnchors,
    int maxStorageChestsPerSlot,
    Set<Identifier> unlockedRoomTiers,
    float productivityModifier,
    float upkeepCostPerDay)
{
}
```

V2 will expand to 8 tiers (Squalid → Noble) to match the room quality scale. V1 stays at 3 to keep scope manageable.

### Tier evaluation

`BuildingTierEvaluator` iterates declared tiers in descending order of rank, returns the first whose requirements all pass:

```java
public BuildingTier evaluateTier(BuildingType type, BuildingEvaluation eval)
{
    List<BuildingTier> sortedDesc = type.tiers().stream()
        .sorted((a, b) -> Integer.compare(b.rank(), a.rank()))
        .toList();

    for (BuildingTier tier : sortedDesc)
    {
        if (tier.structuralThreshold().min() > eval.structuralScore())
        {
            continue;
        }

        boolean allMet = tier.requirements().stream()
            .map(req -> req.evaluate(eval))
            .allMatch(TierRequirementResult::passes);

        if (allMet)
        {
            return tier;
        }
    }

    return type.unbuildedTier();
}
```

### Tier transitions

- **Upgrade:** immediate when structural score crosses threshold.
- **Downgrade:** 3 in-game days grace period. During the grace, the building displays a warning. If structure is restored, downgrade is cancelled.
- **Over-capacity handling:** if a downgrade reduces citizen capacity below current employees, employees are not fired. They become `over_capacity`, productivity halved colony-wide for this building. Player must restore structure or manually unassign.

## Rooms

Rooms are sub-zones inside the Building's outer zone. **One room = one function.** The player chooses the function explicitly.

### Room creation workflow

1. Player opens the Building GUI, navigates to the Structure tab.
2. Sees the list of declared `RoomSlot`s for this Building type (e.g. for a Residence: `primary_bedroom` required, `secondary_bedroom` optional ×3, `kitchen` optional ×1, `dining` optional ×1).
3. Clicks "Designate" for a slot.
4. The Colony Tool activates in `Room` mode with the slot context. HUD: "Designating: primary_bedroom for Residence #2".
5. Player paints the room footprint inside the outer zone (left click to add, right click to remove, shift-click for 3×3 brush, ctrl-click for flood-fill bounded by walls).
6. Real-time validation:
   - All painted blocks must be inside the parent Building's outer zone.
   - The room must not overlap with sibling rooms in the same Building.
   - The room must satisfy the `RoomType` requirements at confirmation.
7. Enter to confirm. The room is created and assigned to the slot atomically.

### Room shape

Rooms use the same `FreeformZone` model as outer zones: 2D footprint + uniform ceiling height. L-shapes, T-shapes, U-shapes are supported.

### Room types in V1

Five basic room types:

- `colony:room/bedroom` — required: 1-2 beds. Provides 1 housing slot.
- `colony:room/kitchen` — required: cooking station + storage. Enables food preparation.
- `colony:room/dining_room` — required: table + chairs. Provides social bonus when adjacent to kitchen.
- `colony:room/office` — required: desk. Required for management huts.
- `colony:room/storage_room` — required: minimum chest count. Provides extra storage tier-capacity.

V2 adds: `master_suite`, `library`, `workshop_bay`, `tavern_hall`, `barracks_dormitory`, etc.

### Room requirements

Each `RoomType` declares requirements as a list of `RoomRequirement`. JSON-driven:

```json
{
  "id": "colony:room/bedroom",
  "volume_range": { "min": 16, "max": 96 },
  "requirements": [
    { "type": "colony:req/functional_block_count", "function": "colony:bed", "min": 1, "max": 2 },
    { "type": "colony:req/enclosure", "min_wall_ratio": 0.85 },
    { "type": "colony:req/access", "min_doors": 1, "max_doors": 2 }
  ],
  "block_relations": {
    "quality_inputs": [
      { "function": "colony:window", "weight": 0.15, "curve": "logarithmic" },
      { "function": "colony:storage/personal", "weight": 0.15 },
      { "function": "colony:decoration/wall_art", "weight": 0.15, "curve": "exponential_asymptote" }
    ],
    "tolerated": ["colony:furniture/chair", "colony:furniture/desk"],
    "out_of_place": [
      { "function": "colony:workstation/*", "penalty": 0.30 },
      { "function": "colony:cooking/*", "penalty": 0.25 }
    ],
    "forbidden": ["colony:hazard/lava", "colony:hazard/mob_spawner"]
  },
  "provides": {
    "colony:role/housing_slot": 1
  }
}
```

Functional blocks are scanned in the room zone; their counts and types feed scoring and validation.

### Room quality

A room's quality score (0.0 to 1.0) is computed from:

- **Weighted sum of quality inputs** present in the room, each input passed through its declared curve (logarithmic, exponential asymptote, etc.) to enforce diminishing returns.
- **Penalties** for out-of-place blocks.
- **Bonuses** for material coherence and adjacency to compatible rooms (see below).

V1 uses a simple weighted sum. V2 adds the RimWorld "weakest-stat dominates" formula for stronger anti-cheese.

### Room adjacency (V2)

Two adjacent rooms can be `WALL_SHARED`, `WALL_WITH_DOOR`, `OPEN_PASSAGE`, or `DIRECTLY_CONNECTED`. The relationship modifies room quality:

- Kitchen ↔ Dining Room via passage = +15% to dining quality (convenient food service).
- Bedroom ↔ Workshop Bay via any link = -30% to bedroom quality (industrial noise).
- Bedroom ↔ Library via shared wall = +10% (quiet neighbor).

Adjacency is detected by examining shared frontier blocks between rooms. Deferred to V2.

## Storage chests

Storage chests are vanilla `ChestBlock` instances inside the outer zone of a Building. They are **typed** by the player to a role.

### Roles in V1

- `colony:storage_role/input` — player deposits, citizens take. Read-only for citizens (no deposit back).
- `colony:storage_role/output` — citizens deposit, player takes. Read-only for player-deposit.
- `colony:storage_role/tools` — bidirectional, contains tools that citizens grab when working.
- `colony:storage_role/materials` — bidirectional, intermediate stock.
- `colony:storage_role/general` — fallback, no filter.

### Filtering

Each `StorageSlot` declares a filter (tag or item set). The filter restricts what items can be deposited. For example, the farmer hut's `seeds_input` slot filters to `#colony:filter/seeds`.

```json
{
  "id": "colony:filter/seeds",
  "matches": {
    "tags": ["#minecraft:seeds"],
    "items": ["minecraft:potato", "minecraft:carrot", "minecraft:beetroot_seeds"]
  }
}
```

Mods can add filters via datapack. The farmer's storage slot picks up new compatible seeds automatically.

### Chest typing workflow

1. Player opens the Building GUI, navigates to Storage tab.
2. Sees declared `StorageSlot`s with current chest counts (e.g. "seeds_input: 1/3").
3. Clicks "Designate chests" for a slot.
4. The Colony Tool activates in `Storage` mode with the slot context.
5. Player right-clicks a chest in the world:
   - If the chest is inside the Building's outer zone and not already typed for another slot, it's assigned to this slot.
   - Otherwise, the action is rejected with a clear message.
6. Visual feedback: each typed chest emits subtle colored particles (blue for input, green for output, yellow for tools, etc.).
7. Player exits typing mode by pressing Esc or switching tool mode.

### Constraints

- A chest can only be typed for one slot at a time.
- Re-typing requires explicit "Clear" action on the previous slot first.
- A chest moved outside its Building's outer zone (e.g. by piston) becomes `orphaned`. Marked in GUI but contents preserved.

## Anchors

Anchors are blocks placed **outside** the Building, defining work zones. Examples: scarecrow for fields, quarry pillar for mining, lumber post for tree harvesting, fishing buoy for fishing zones, pasture stake for animal grazing.

### Anchor configuration

Each anchor has:

1. A **zone** (rectangular `AxisAlignedZone` by default, freeform optional in V2). Defined via a GUI with N/S/W/E/U/D offsets, with real-time wireframe preview in the world.
2. A **type-specific configuration** stored on the anchor itself, set via right-click with a relevant item.

### Right-click configuration patterns (V1)

**Scarecrow:**
- Right-click with seed item → sets `assignedCrop`. Consumes 1 seed for visual confirmation.
- Right-click with empty hand → opens GUI for zone + pattern + reset.

**Quarry pillar (V2):**
- Right-click with pickaxe → sets minimum tool tier to mine with.
- Right-click with shulker box → sets preserve list (ores not mined).

**Lumber post (V2):**
- Right-click with sapling → sets primary tree species for replanting.

Pattern is generalized via `AnchorInteractionHandler` registry. Mods register their own handlers for custom anchor types.

### Anchor linking to a Building

An anchor placed alone is **dormant**. To activate, it must be linked to a compatible Building:

1. Player opens the Building GUI, navigates to Work Zones tab.
2. Sees declared `AnchorSlot`s (e.g. for Farmer Hut: `fields` slot, accepts `colony:anchor/scarecrow`, max linked count = `f(tier)`).
3. Sees a list of compatible anchors within range (default 64 blocks).
4. Clicks "Link" on the desired anchor. The link is created.

Alternative: place a new anchor via "Get new {AnchorType} item" button in the GUI. The item is automatically pre-linked to this Building when placed.

## Colony Tool

The single multi-purpose item handled to the player at colony foundation.

### Modes

Cycled via shift+scroll on the tool:

- **Zone** — paint outer zones for Buildings (during `PendingPlacement`) or room sub-zones (when invoked from a Building GUI with slot context).
- **Storage** — type chests by right-click (when invoked from a Building GUI Storage tab).
- **Link** — link/unlink anchors to Buildings.
- **Inspect** — display info on Buildings, rooms, chests, anchors on hover.

### Recovery

If lost (death in lava, etc.), the player runs `/colony wand restore` to receive a new one. The item is non-droppable, non-stackable, persistent.

### Visibility

The tool's icon shows the current mode. The HUD displays mode-specific hints.

## Validation pipeline

When a Building is created or modified, the following passes run (async on a dedicated thread, results applied main thread):

1. **Structural validators** — enclosure (walls/roof/floor), access (door reachable), support (foundation present), light, mob safety.
2. **Material analysis** — dominant material, coherence ratio, material tier.
3. **Functional block scan** — single pass through the outer zone, dispatches each block to all interested `FunctionalBlockDetector`s.
4. **Room evaluations** — for each room, run `RoomRequirement` checks and compute quality.
5. **Tier evaluation** — `BuildingTierEvaluator` picks the highest valid tier.
6. **Notification** — UI updates, events fired (`BuildingScoredEvent`, `TierChangedEvent`).

Re-validation triggers:

- Block change inside the outer zone (debounced, 100 ticks).
- Manual via GUI "Re-evaluate" button.
- Time-based (every N in-game days for decay/maintenance).

## What's deferred to V2

- Room adjacency bonuses/penalties.
- 8-tier quality scale (Squalid → Noble) — V1 uses 3 tiers.
- Master Suite, Library, Workshop Bay, Tavern Hall room types.
- Schematic import (.nbt vanilla format).
- Building decay without maintenance.
- Hierarchical structures (buildings containing sub-buildings).
- Anchor configuration for non-farming types (quarry, lumber, fishing, pasture).
- Inter-building logistics (porter citizens hauling between Buildings).

## What's NOT planned

- Auto-detection of room types from contents. Player designates explicitly. Always.
- 3D freeform room interiors with overhangs. Footprint + uniform height only.
- Building demolition by Builder citizen. Player breaks blocks manually if they want to deconstruct.

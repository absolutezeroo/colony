# 16 — API: Room Adjacency System (V2 frozen)

This document specifies the public API for V2 room adjacency bonuses and penalties. The interfaces below ship in `:api` from V1.0 with no-op implementations.

V1 rooms are evaluated independently. V2 adds inter-room relationships: a Dining Room next to a Kitchen gets a quality bonus, a Bedroom next to a Workshop gets a penalty.

---

## Why this is a separate API

Adjacency could be hardcoded inside `RoomEvaluator`. Doing so would:

- Lock the adjacency rules to base mod content only.
- Prevent addons from declaring "my custom room type loves being next to a smithy."
- Tangle scoring logic with geometric detection.

A dedicated `RoomAdjacencyService` separates the geometry detection (which rooms are adjacent and how) from the scoring (what bonuses/penalties apply). Addons can hook either side independently.

---

## Adjacency types

```java
package com.akikazu.colony.api.building.room;

public enum RoomAdjacencyType
{
    NONE,
    WALL_SHARED,
    WALL_WITH_DOOR,
    OPEN_PASSAGE,
    DIRECTLY_CONNECTED
}
```

**Semantics:**

- `NONE` — rooms do not share any frontier blocks.
- `WALL_SHARED` — rooms share at least one wall block (solid block belonging to both rooms' perimeters). No opening exists.
- `WALL_WITH_DOOR` — shared frontier includes a door (vanilla `DoorBlock` or tagged `#colony:passable_door`).
- `OPEN_PASSAGE` — shared frontier has air blocks (visible opening, no door).
- `DIRECTLY_CONNECTED` — rooms have no separating wall at all; their footprints touch directly. Rare but valid for "open floor plan" designs.

**Detection rule:**
For each pair of rooms in the same Building, examine their boundary blocks. The strongest adjacency type wins (DIRECTLY_CONNECTED > OPEN_PASSAGE > WALL_WITH_DOOR > WALL_SHARED > NONE).

---

## Core interface

```java
package com.akikazu.colony.api.building.room;

import com.akikazu.colony.api.registry.Identifier;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.stream.Stream;

@NullMarked
public interface RoomAdjacencyService
{
    RoomAdjacencyType detectAdjacency(Room a, Room b);

    Stream<RoomAdjacency> adjacenciesOf(Room room);

    Stream<RoomAdjacency> adjacenciesInBuilding(BuildingId building);

    List<AdjacencyEffect> computeEffects(Room room);

    void invalidate(BuildingId building);
}
```

**Contract:**
- `detectAdjacency` is symmetric: `detectAdjacency(a, b) == detectAdjacency(b, a)`. The service computes from block geometry; order does not matter.
- `adjacenciesOf` returns adjacencies where `room` is one side. Each `RoomAdjacency` exposes the *other* room.
- `computeEffects` returns the list of bonuses and penalties that apply to `room` due to its neighbors. Each effect is typed.
- `invalidate` clears the adjacency cache for a building. Called when any room in the building changes shape or is added/removed.

---

## Adjacency record

```java
public record RoomAdjacency(
    Room otherRoom,
    RoomAdjacencyType type,
    int sharedSurfaceCount)
{
}
```

`sharedSurfaceCount` is the number of block faces shared between the two rooms. Useful for thresholding ("only count adjacencies with >= 4 shared faces").

---

## Effects

```java
public record AdjacencyEffect(
    Room source,
    Identifier effectType,
    float magnitude,
    Identifier reason)
{
}
```

Adjacency effects modify a room's quality score:

```java
public interface Room
{
    default float baseQuality() { /* ... */ }

    default float adjustedQuality(RoomAdjacencyService service)
    {
        float base = baseQuality();
        float adjacencySum = service.computeEffects(this).stream()
            .map(AdjacencyEffect::magnitude)
            .reduce(0.0f, Float::sum);

        return Math.max(0.0f, Math.min(1.0f, base + adjacencySum));
    }
}
```

The room's "displayed quality" in GUI is always `adjustedQuality`. The base quality is internal.

---

## Declaring adjacency rules (JSON)

V2 rules are JSON-defined per `RoomType`:

```json
{
  "id": "colony:room/dining_room",
  "adjacency_modifiers": [
    {
      "neighbor": "colony:room/kitchen",
      "via": ["wall_with_door", "open_passage", "directly_connected"],
      "quality_bonus": 0.15,
      "reason": "colony:reason/convenient_food_service"
    },
    {
      "neighbor": "colony:room/bedroom",
      "via": ["open_passage", "directly_connected"],
      "quality_penalty": 0.10,
      "reason": "colony:reason/noise_to_private_space"
    }
  ]
}
```

**Validation rules** (enforced by `AdjacencyModifierCodec`):
- `quality_bonus` and `quality_penalty` are mutually exclusive (use the right one for the sign).
- `via` is a non-empty list of `RoomAdjacencyType` values (NONE not allowed).
- Magnitudes are in [0.0, 1.0].
- `reason` is a translation key, used in tooltips.

---

## Addon registration

```java
package com.akikazu.colony.api.building.room;

import com.akikazu.colony.api.registry.Identifier;

public interface AdjacencyRule
{
    Identifier sourceRoomType();

    Identifier neighborRoomType();

    java.util.Set<RoomAdjacencyType> applicableVia();

    float magnitude();

    Identifier reason();
}

public interface AdjacencyRuleRegistry
{
    void register(AdjacencyRule rule);

    java.util.stream.Stream<AdjacencyRule> rulesFor(Identifier sourceType);
}
```

Addons register rules via the registry. JSON loading populates the registry at datapack load.

**Conflict resolution:**
If two rules apply to the same room pair via the same `RoomAdjacencyType`, **the magnitudes are summed**, not the highest wins. This allows additive stacking when intentional, and avoids hidden conflict resolution. Modpack authors can configure rule sets that sum sensibly.

---

## Geometry detection algorithm

The detection algorithm is deterministic and exposed publicly so addons can validate their understanding:

```
For each room pair (A, B) in the building:
    if A.boundingBox.intersects(B.boundingBox) is false:
        return NONE
    
    sharedBoundary = set of BlockPos where:
        - pos is in A.floor or above A.floor up to A.ceiling
        - pos is in B.floor or above B.floor up to B.ceiling
        - pos is on the boundary of one but not interior of the other
    
    if sharedBoundary is empty:
        return NONE
    
    if exists pos in sharedBoundary where blockstate at pos is air AND
       no block separates the two interiors:
        return DIRECTLY_CONNECTED
    
    if exists pos in sharedBoundary where blockstate at pos is air:
        return OPEN_PASSAGE
    
    if exists pos in sharedBoundary where blockstate at pos is tagged #colony:passable_door:
        return WALL_WITH_DOOR
    
    return WALL_SHARED
```

The algorithm runs per-pair, lazily, cached. Recomputation triggers when a block changes inside either room's bounding box.

---

## Performance considerations

For a building with N rooms, the pairwise space is O(N²). At N=10 rooms (a large building), 45 pairs. At N=20 (a massive estate), 190 pairs. Acceptable.

Detection per pair is O(boundary_size), where boundary_size = sum of floor + 2 × wall area. For a 4×4×3 room, ~50 blocks. So a 20-room estate computes ~9500 boundary checks per full re-evaluation. Done async on building re-scan, < 100ms typical.

Cache invalidation:
- Block change inside room R invalidates only R's adjacencies (N-1 pairs, not N²).
- Room added/removed invalidates the entire building's adjacency cache.

---

## V1 stub implementation

In V1.0, `RoomAdjacencyService` ships as no-op:

```java
@ApiStatus.Internal
public final class V1NoOpAdjacencyService implements RoomAdjacencyService
{
    @Override
    public RoomAdjacencyType detectAdjacency(Room a, Room b)
    {
        return RoomAdjacencyType.NONE;
    }

    @Override
    public Stream<RoomAdjacency> adjacenciesOf(Room room)
    {
        return Stream.empty();
    }

    @Override
    public Stream<RoomAdjacency> adjacenciesInBuilding(BuildingId building)
    {
        return Stream.empty();
    }

    @Override
    public List<AdjacencyEffect> computeEffects(Room room)
    {
        return List.of();
    }

    @Override
    public void invalidate(BuildingId building)
    {
        // No-op.
    }
}
```

This means in V1:
- All rooms report NONE adjacencies.
- `room.adjustedQuality()` equals `room.baseQuality()`.
- Addons that depend on adjacency see consistent "no neighbors" reports.

V2 replaces this with the real implementation.

---

## Migration V1 → V2

No data migration needed. V1 had no adjacency data persisted. V2 computes adjacencies fresh on first building scan after upgrade.

The migration step is empty:

```java
public CompoundTag migrate(CompoundTag v1Data)
{
    return v1Data; // Adjacency data is computed, not persisted.
}
```

---

## Adjacency examples (V2 base content)

The base mod ships these rules in V2:

| Source Room | Neighbor | Via | Effect |
|---|---|---|---|
| Dining Room | Kitchen | door/passage/direct | +15% quality |
| Dining Room | Bedroom | passage/direct | -10% quality |
| Bedroom | Library | shared wall, door | +10% quality |
| Bedroom | Workshop Bay | any | -30% quality |
| Bedroom | Kitchen | shared wall | -5% quality |
| Bedroom | Kitchen | door/passage | -20% quality |
| Throne Room (V2) | Bedroom | any | -25% (prestige incompatibility) |
| Library | Workshop Bay | passage | -15% quality |
| Storage Room | Kitchen | door | +5% (convenience) |
| Storage Room | Workshop Bay | door | +10% (convenience) |

These are JSON-defined. Modpack authors can override the entire ruleset.

---

## What V1.0 commits to API-wise

- `RoomAdjacencyType` enum, 5 values.
- `RoomAdjacency` record.
- `AdjacencyEffect` record.
- `RoomAdjacencyService` interface, 5 methods.
- `AdjacencyRule` interface.
- `AdjacencyRuleRegistry` interface.
- `Room.adjustedQuality(RoomAdjacencyService)` default method.

Frozen. Changes after V1.0 are MAJOR version bumps.

## What V1.0 does NOT commit to

- Detection algorithm correctness for pathological geometries (room with a hole inside another room, recursive nesting). V2 may refine the algorithm; addons should not assume.
- Cache eviction strategy.
- Whether adjacency rules apply transitively (A boosts B boosts C — no, only direct pairs).
- Floor-vs-ceiling adjacency (rooms stacked vertically with shared floor/ceiling). V2 ships horizontal only; vertical may be added in V2.x.

---

## Future expansion considered

- **3D adjacency**: rooms above/below each other. Not in V2.0; possibly V2.x. Addons should not assume.
- **Soft adjacency**: rooms within N blocks but not directly touching (e.g. across a courtyard). Not planned. Use the explicit door/passage model.
- **Cross-building adjacency**: a room in building A boosting a room in building B. Not planned. Buildings are independent units.

If any of these become important, they get a separate `:api` version bump.

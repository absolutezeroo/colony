# 17 — API: Schematic Import System (V2 frozen)

This document specifies the V2 schematic import system, intentionally **opt-in** and **non-blocking**. The free-build experience remains the default. Schematic import is a convenience for players who want to spawn a prefab structure as a starting point.

V1.0 ships zero schematic import. V2 adds it as a side-feature, never replacing the core workflow.

---

## Philosophy

We made a hard design pillar: "Free-build first, schematic optional" (`02-DESIGN-PILLARS.md`). This document does **not** revisit that decision. It specifies how the optional schematic mechanic works without compromising the pillar.

**What schematic import is for:**
- Players who want a starting structure (a vanilla village blueprint, a community-shared building) to then designate and modify.
- Modpack authors who want to ship pre-designed colonies as starter content.
- Builders who created a structure in creative mode and want to deploy it in survival.

**What schematic import is NOT for:**
- Replacing the Hut placement workflow.
- Forcing players into specific structures.
- A `Builder` citizen reconstructing schematics over time (V3+ if ever).
- MineColonies-style "level 1 → level 5 schematic upgrade" gameplay.

A schematic, in our system, is a **starting state**, not a contract.

---

## File format

Schematics are **vanilla Minecraft `.nbt` structure files**. Same format as the vanilla Structure Block uses. Stored at:

```
world/data/colony/schematics/{player_uuid}/{name}.nbt    (player-scoped)
world/data/colony/schematics/shared/{name}.nbt           (server-shared)
```

We do **not** invent a custom format. Reasons:

- Vanilla `.nbt` is well-documented, well-tooled.
- Players already know how to create them via Structure Block.
- WorldEdit can export `.nbt` directly.
- We avoid a forever-maintenance schema.

The vanilla structure format encodes blocks + block entities. We **strip inventory contents** on import (chests, barrels arrive empty). This prevents two abuses:

- Item duplication (importing a schematic that contains stored items).
- Pre-populated typed chests claiming to be already designated.

---

## Core interface

```java
package com.akikazu.colony.api.schematic;

import net.minecraft.world.level.block.entity.StructureBlockEntity;
import org.jspecify.annotations.NullMarked;

import java.nio.file.Path;
import java.util.UUID;

@NullMarked
public interface SchematicService
{
    SchematicImportResult importSchematic(
        SchematicSource source,
        SchematicPlacementContext context);

    java.util.List<SchematicInfo> listAvailable(UUID requestor);

    SchematicInfo describe(SchematicSource source);

    boolean canImport(UUID requestor, SchematicSource source);
}

public sealed interface SchematicSource permits FileSource, EmbeddedSource, ResourceSource
{
    record FileSource(Path path) implements SchematicSource {}
    record EmbeddedSource(UUID schematicId) implements SchematicSource {}
    record ResourceSource(net.minecraft.resources.ResourceLocation resourceId) implements SchematicSource {}
}
```

**Contract:**

- `importSchematic` is the entry point. Returns success or detailed failure.
- `listAvailable` returns schematics the requesting player can use (own files + shared + datapack-provided).
- `describe` gives metadata without performing import.
- `canImport` checks permissions and resource availability without side effects.

All operations are server-side. Clients request imports via `RequestSchematicImportPayload`.

---

## Placement context

```java
public record SchematicPlacementContext(
    UUID requestor,
    BlockPos origin,
    Rotation rotation,
    Mirror mirror,
    PlacementFlags flags)
{
}

public record PlacementFlags(
    boolean replaceExistingBlocks,
    boolean integrateWithTerrain,
    boolean autoDesignateOuterZone,
    boolean preserveInventories)
{
}
```

`PlacementFlags` controls behavior:
- `replaceExistingBlocks`: if true, schematic overwrites world. Default: false (requires empty space).
- `integrateWithTerrain`: if true, blocks below the schematic origin are smoothed (auto-foundation). Default: false.
- `autoDesignateOuterZone`: if true, after placement, automatically opens the Pending Placement workflow with the schematic's bounding box as the outer zone. Default: true.
- `preserveInventories`: **always overridden to false** for security. Inventories are never imported.

---

## Import result

```java
public sealed interface SchematicImportResult permits Success, Rejected
{
    record Success(
        BlockBox placedRegion,
        int blocksPlaced,
        int blocksSkipped,
        java.util.Optional<UUID> pendingPlacementHutId)
        implements SchematicImportResult
    {
    }

    record Rejected(java.util.List<SchematicImportError> errors)
        implements SchematicImportResult
    {
    }
}

public sealed interface SchematicImportError
{
    record FileNotFound(Path path) implements SchematicImportError {}
    record MalformedFile(String reason) implements SchematicImportError {}
    record InsufficientSpace(BlockBox required) implements SchematicImportError {}
    record OverlapsClaimedZone(BuildingId conflictingBuilding) implements SchematicImportError {}
    record PermissionDenied(String reason) implements SchematicImportError {}
    record OutsideColonyClaim() implements SchematicImportError {}
    record TooLarge(int volume, int maxVolume) implements SchematicImportError {}
    record ProhibitedBlocks(java.util.List<BlockState> prohibited) implements SchematicImportError {}
}
```

**Important constraints:**
- A schematic cannot place blocks that violate world rules (no spawners, no command blocks for non-OP players).
- A schematic placement that would overlap another player's building is rejected.
- A schematic placement requires the player to be inside their colony claim (V3+) or anywhere if no claim system.

---

## Workflow: import + designate

Default workflow when `autoDesignateOuterZone = true`:

1. Player runs `/colony schematic import {name}` or uses the Schematic Browser GUI (V2.x).
2. Server validates the schematic file and computes the bounding box at the requested position.
3. Server performs the block placement (blocks placed, entities skipped, inventories empty).
4. Server enters `PendingPlacement` state for the player with the schematic's bounding box as the proposed outer zone.
5. Player now sees the same Pending Placement UI as if they were placing a Hut block normally. They can adjust the zone (shrink, expand, switch to freeform), then pick which Hut block to place inside.
6. On confirm, the Building is created normally.

This means **schematic import doesn't bypass any validation**. The Building still needs to satisfy structural requirements, the Hut block still goes through normal placement, etc. The schematic just placed blocks.

If `autoDesignateOuterZone = false`, the schematic is just placed and the player handles designation manually later.

---

## Permission model

Schematic import touches the world significantly. Permissions are stricter than normal placement:

```java
public enum SchematicPermissionLevel
{
    NONE,           // Cannot import any schematic
    OWN_ONLY,       // Can import schematics in their own folder
    SHARED,         // Can import shared schematics + own
    RESOURCE,       // Can import datapack-bundled schematics + above
    UNRESTRICTED    // Server admin
}
```

Default per environment:
- Single-player: `UNRESTRICTED`.
- LAN: `UNRESTRICTED` for host, `SHARED` for guests.
- Dedicated server: `OWN_ONLY` for normal players, `UNRESTRICTED` for OPs.

Configurable via server config `schematic.default_permission_level`.

---

## Size limits

Schematics have hard size limits to prevent abuse:

```java
public final class SchematicLimits
{
    public static final int DEFAULT_MAX_VOLUME = 8192;     // 32×32×8 typical
    public static final int DEFAULT_MAX_BLOCKS = 4096;     // non-air blocks
    public static final int DEFAULT_MAX_DIMENSIONS = 48;   // max side length
}
```

Configurable in server config:
- `schematic.max_volume`
- `schematic.max_blocks`
- `schematic.max_dimensions`

A schematic exceeding any limit is rejected with `TooLarge` error.

---

## Prohibited blocks

The server maintains a list of block types that cannot be in imported schematics:

```java
// Tag: #colony:prohibited_in_schematic
- Any block from #minecraft:spawners
- minecraft:command_block, minecraft:repeating_command_block, minecraft:chain_command_block
- minecraft:structure_block, minecraft:jigsaw_block
- minecraft:barrier, minecraft:light
- Any block with non-empty inventory at export time
```

A schematic containing any of these is rejected with `ProhibitedBlocks` error before any placement.

Modpack authors can extend the prohibition list via the `#colony:prohibited_in_schematic` block tag.

---

## Block entity handling

The schematic format encodes block entities (chests, furnaces, etc.). On import:

- The block is placed.
- The block entity is created with **default state**.
- Inventory contents are **not** copied.
- Customizations like signed banners, named items, custom names on chests are **not** preserved.

This means a schematic of a tavern arrives with empty barrels, no signs reading "Welcome", no named storage. The player can re-do these post-import.

Why so strict: avoids inventory duplication exploits, avoids unverifiable trust on what the schematic claims.

---

## Schematic browsing GUI (V2.x)

```java
package com.akikazu.colony.api.schematic;

public interface SchematicBrowser
{
    java.util.List<SchematicInfo> browse(UUID requestor);

    SchematicInfo selected();

    void preview(SchematicSource source, BlockPos previewOrigin);

    void confirmImport(SchematicPlacementContext context);

    void cancelPreview();
}
```

The browser shows a list with thumbnails (rendered server-side, sent to client). Player selects, sees a preview wireframe in the world, can rotate/mirror, then confirms.

V2.0 ships text-based browsing only (`/colony schematic list`, `/colony schematic preview {name}`). The GUI is V2.x.

---

## Resource-pack schematics

Modpack authors can ship schematics in datapacks:

```
data/{modid}/schematics/{name}.nbt
```

The `SchematicService.listAvailable` includes these for all players with `RESOURCE` permission or higher.

Naming convention encouraged: `data/{modid}/schematics/{category}/{name}.nbt`, e.g. `data/myaddon/schematics/medieval/blacksmith.nbt`.

---

## V1 stub implementation

In V1.0, `SchematicService` ships as a stub:

```java
@ApiStatus.Internal
public final class V1NoOpSchematicService implements SchematicService
{
    @Override
    public SchematicImportResult importSchematic(SchematicSource source, SchematicPlacementContext context)
    {
        return new Rejected(java.util.List.of(
            new SchematicImportError.PermissionDenied("Schematic import is V2+; not available in V1.0")));
    }

    @Override
    public java.util.List<SchematicInfo> listAvailable(UUID requestor)
    {
        return java.util.List.of();
    }

    @Override
    public SchematicInfo describe(SchematicSource source)
    {
        return SchematicInfo.unavailable();
    }

    @Override
    public boolean canImport(UUID requestor, SchematicSource source)
    {
        return false;
    }
}
```

`/colony schematic` commands respond with "Schematic import will be available in V2.0" in V1.

---

## Migration considerations

No migration required when V2 lands. Schematic import is additive — no existing data is reinterpreted.

V2.0 just needs to swap the no-op service for the real one. Players who held schematics in their world data folder (manually placed) become able to use them.

---

## What V1.0 commits to API-wise

- `SchematicService` interface, 4 methods.
- `SchematicSource` sealed interface, 3 variants.
- `SchematicPlacementContext` record.
- `PlacementFlags` record.
- `SchematicImportResult` sealed interface, 2 variants.
- `SchematicImportError` sealed interface, 8 variants.
- `SchematicPermissionLevel` enum, 5 values.
- `SchematicLimits` constants.
- `SchematicBrowser` interface (functional in V2.x, dormant in V2.0).

Frozen for V1.0. Future additions are MINOR; modifications are MAJOR.

---

## What we explicitly do NOT support

- **Builder citizens reconstructing schematics over time.** MineColonies pattern. We instantly place or reject. No "build queue."
- **Schematic upgrade levels.** A schematic is a one-shot import. No "import level 1, later import level 2 to upgrade."
- **Online schematic marketplaces.** Players share files manually. No in-game browser of external sources.
- **Encrypted or paid schematics.** Anti-feature.
- **Server-side schematic generation from prompts.** Not in scope.

---

## V3+ possibilities (not committed)

If V2 schematic import proves popular, V3 may consider:

- **Builder citizen reconstruction** as an opt-in alternative workflow (instant placement still default).
- **Live progress visualization** for slow reconstruction (Builder mode).
- **Multi-level schematic packs** for modpack authors who want progression structures.

None of these are committed. They are explicitly listed in `10-TECH-DEBT.md` as "considered, not promised."

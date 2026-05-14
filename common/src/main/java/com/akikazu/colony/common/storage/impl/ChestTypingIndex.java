package com.akikazu.colony.common.storage.impl;

import com.akikazu.colony.api.building.BuildingId;
import com.akikazu.colony.api.colony.ColonyId;
import com.akikazu.colony.core.registry.Identifier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Always-loaded index of every chest the player has typed to a storage slot.
 *
 * <p>
 * Sibling to {@link com.akikazu.colony.common.building.BuildingIndex}: persisted as a single overworld-level
 * {@link SavedData} record, keyed by {@link BlockPos} so position lookups are O(1). The chest's {@link TypedChest}
 * record is the source of truth — vanilla chest block entities are never tagged or otherwise mutated.
 */
public final class ChestTypingIndex extends SavedData
{
    public static final String DATA_NAME = "colony_chest_typing_index";

    private static final Codec<List<TypedChest>> ENTRIES_CODEC = TypedChest.CODEC.listOf();

    private final Map<BlockPos, TypedChest> typedChests;

    public ChestTypingIndex()
    {
        this.typedChests = new LinkedHashMap<>();
    }

    private ChestTypingIndex(Map<BlockPos, TypedChest> entries)
    {
        this.typedChests = new LinkedHashMap<>(entries);
    }

    public static Factory<ChestTypingIndex> factory()
    {
        return new Factory<>(ChestTypingIndex::new, ChestTypingIndex::load);
    }

    public static ChestTypingIndex get(ServerLevel level)
    {
        return level.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(factory(), DATA_NAME);
    }

    public void assign(BlockPos pos, ColonyId colony, BuildingId building, Identifier slotId, Identifier role)
    {
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(colony, "colony");
        Objects.requireNonNull(building, "building");
        Objects.requireNonNull(slotId, "slotId");
        Objects.requireNonNull(role, "role");

        TypedChest existing = typedChests.get(pos);

        if (existing != null && !existing.building().equals(building))
        {
            throw new IllegalStateException(
                    "Chest at %s is already typed for building %s; cannot reassign to %s"
                            .formatted(pos, existing.building(), building));
        }

        typedChests.put(pos.immutable(), new TypedChest(pos.immutable(), colony, building, slotId, role));
        setDirty();
    }

    public boolean clear(BlockPos pos)
    {
        Objects.requireNonNull(pos, "pos");

        TypedChest removed = typedChests.remove(pos);

        if (removed == null)
        {
            return false;
        }

        setDirty();

        return true;
    }

    public Optional<TypedChest> findAt(BlockPos pos)
    {
        Objects.requireNonNull(pos, "pos");

        return Optional.ofNullable(typedChests.get(pos));
    }

    public Stream<TypedChest> inBuilding(BuildingId building)
    {
        Objects.requireNonNull(building, "building");

        return typedChests.values().stream().filter(c -> c.building().equals(building));
    }

    public Stream<TypedChest> inSlot(BuildingId building, Identifier slotId)
    {
        Objects.requireNonNull(building, "building");
        Objects.requireNonNull(slotId, "slotId");

        return typedChests.values().stream()
                .filter(c -> c.building().equals(building))
                .filter(c -> c.slotId().equals(slotId));
    }

    public Map<BlockPos, TypedChest> entries()
    {
        return Collections.unmodifiableMap(typedChests);
    }

    public int size()
    {
        return typedChests.size();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider)
    {
        List<TypedChest> snapshot = List.copyOf(typedChests.values());
        DataResult<Tag> encoded = ENTRIES_CODEC.encodeStart(NbtOps.INSTANCE, snapshot);
        tag.put("entries", encoded.getOrThrow());

        return tag;
    }

    static ChestTypingIndex load(CompoundTag tag, HolderLookup.Provider provider)
    {
        if (!tag.contains("entries"))
        {
            return new ChestTypingIndex();
        }

        List<TypedChest> decoded = ENTRIES_CODEC
                .parse(NbtOps.INSTANCE, tag.get("entries"))
                .getOrThrow();

        Map<BlockPos, TypedChest> rebuilt = new LinkedHashMap<>();

        for (TypedChest entry : decoded)
        {
            rebuilt.put(entry.position(), entry);
        }

        return new ChestTypingIndex(rebuilt);
    }
}

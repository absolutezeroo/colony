package com.akikazu.colony.common.building;

import com.akikazu.colony.api.building.AxisAlignedOuterZone;
import com.akikazu.colony.api.building.BuildingId;
import com.akikazu.colony.api.colony.ColonyId;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

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
 * Always-loaded index of every building registered on the server.
 *
 * <p>
 * Mirrors {@link com.akikazu.colony.common.colony.ColonyIndex}: persisted as a single overworld-level {@link SavedData}
 * record, cheap to scan for overlap checks during the painting workflow, and never holds the heavier per-room/per-chest
 * state. V1 uses a linear scan for overlap and {@link #findByPosition} — the building counts we expect (well under a
 * thousand per save) make a spatial index premature. The hook is here when that changes.
 */
public final class BuildingIndex extends SavedData
{
    public static final String DATA_NAME = "colony_building_index";

    private static final Codec<Entry> ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuildingId.CODEC.fieldOf("id").forGetter(Entry::id),
            BuildingMetadata.CODEC.fieldOf("metadata").forGetter(Entry::metadata))
            .apply(instance, Entry::new));

    private static final Codec<List<Entry>> ENTRIES_CODEC = ENTRY_CODEC.listOf();

    private record Entry(BuildingId id, BuildingMetadata metadata)
    {
    }

    private final Map<BuildingId, BuildingMetadata> entries;

    public BuildingIndex()
    {
        this.entries = new LinkedHashMap<>();
    }

    private BuildingIndex(Map<BuildingId, BuildingMetadata> entries)
    {
        this.entries = new LinkedHashMap<>(entries);
    }

    public static Factory<BuildingIndex> factory()
    {
        return new Factory<>(BuildingIndex::new, BuildingIndex::load);
    }

    public static BuildingIndex get(ServerLevel level)
    {
        return level.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(factory(), DATA_NAME);
    }

    public boolean register(BuildingId id, BuildingMetadata metadata)
    {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(metadata, "metadata");

        if (entries.containsKey(id))
        {
            return false;
        }

        entries.put(id, metadata);
        setDirty();

        return true;
    }

    public Map<BuildingId, BuildingMetadata> entries()
    {
        return Collections.unmodifiableMap(entries);
    }

    public int size()
    {
        return entries.size();
    }

    public Optional<BuildingMetadata> find(BuildingId id)
    {
        Objects.requireNonNull(id, "id");

        return Optional.ofNullable(entries.get(id));
    }

    public Stream<BuildingMetadata> allInColony(ColonyId colony)
    {
        Objects.requireNonNull(colony, "colony");

        return entries.values().stream().filter(m -> m.colony().equals(colony));
    }

    public Optional<BuildingMetadata> findByPosition(BlockPos pos)
    {
        Objects.requireNonNull(pos, "pos");

        return entries.values().stream()
                .filter(m -> m.outerZone().contains(pos))
                .findFirst();
    }

    public boolean hasOverlap(AxisAlignedOuterZone candidate)
    {
        Objects.requireNonNull(candidate, "candidate");

        return entries.values().stream().anyMatch(m -> m.outerZone().overlaps(candidate));
    }

    public Optional<BuildingId> findOverlapping(AxisAlignedOuterZone candidate)
    {
        Objects.requireNonNull(candidate, "candidate");

        return entries.entrySet().stream()
                .filter(e -> e.getValue().outerZone().overlaps(candidate))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider)
    {
        List<Entry> snapshot = entries.entrySet().stream()
                .map(e -> new Entry(e.getKey(), e.getValue()))
                .toList();
        DataResult<Tag> encoded = ENTRIES_CODEC.encodeStart(NbtOps.INSTANCE, snapshot);
        tag.put("entries", encoded.getOrThrow());

        return tag;
    }

    static BuildingIndex load(CompoundTag tag, HolderLookup.Provider provider)
    {
        if (!tag.contains("entries"))
        {
            return new BuildingIndex();
        }

        List<Entry> decoded = ENTRIES_CODEC
                .parse(NbtOps.INSTANCE, tag.get("entries"))
                .getOrThrow();

        Map<BuildingId, BuildingMetadata> rebuilt = new LinkedHashMap<>();

        for (Entry entry : decoded)
        {
            rebuilt.put(entry.id(), entry.metadata());
        }

        return new BuildingIndex(rebuilt);
    }
}

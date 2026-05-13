package com.akikazu.colony.common.colony;

import com.akikazu.colony.api.colony.ColonyId;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

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

/**
 * Lightweight, always-loaded index of every colony known to the dedicated server.
 *
 * <p>
 * Persisted as a single overworld-level {@link SavedData} record. The full colony state lives in its own per-colony
 * file (see {@code docs/01-ARCHITECTURE.md}); this class only keeps the entries needed to answer "which colonies
 * exist?" and "where is each Town Hall?" without loading the heavy snapshots.
 */
public final class ColonyIndex extends SavedData
{
    public static final String DATA_NAME = "colony_index";

    private static final Codec<Entry> ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ColonyId.CODEC.fieldOf("id").forGetter(Entry::id),
            ColonyMetadata.CODEC.fieldOf("metadata").forGetter(Entry::metadata))
            .apply(instance, Entry::new));

    private static final Codec<List<Entry>> ENTRIES_CODEC = ENTRY_CODEC.listOf();

    private record Entry(ColonyId id, ColonyMetadata metadata)
    {
    }

    private final Map<ColonyId, ColonyMetadata> entries;

    public ColonyIndex()
    {
        this.entries = new LinkedHashMap<>();
    }

    private ColonyIndex(Map<ColonyId, ColonyMetadata> entries)
    {
        this.entries = new LinkedHashMap<>(entries);
    }

    public static Factory<ColonyIndex> factory()
    {
        return new Factory<>(ColonyIndex::new, ColonyIndex::load);
    }

    public static ColonyIndex get(ServerLevel level)
    {
        return level.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(factory(), DATA_NAME);
    }

    public boolean register(ColonyId id, ColonyMetadata metadata)
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

    public Map<ColonyId, ColonyMetadata> entries()
    {
        return Collections.unmodifiableMap(entries);
    }

    public int size()
    {
        return entries.size();
    }

    public boolean contains(ColonyId id)
    {
        return entries.containsKey(id);
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

    static ColonyIndex load(CompoundTag tag, HolderLookup.Provider provider)
    {
        if (!tag.contains("entries"))
        {
            return new ColonyIndex();
        }

        List<Entry> decoded = ENTRIES_CODEC
                .parse(NbtOps.INSTANCE, tag.get("entries"))
                .getOrThrow();

        Map<ColonyId, ColonyMetadata> rebuilt = new LinkedHashMap<>();

        for (Entry entry : decoded)
        {
            rebuilt.put(entry.id(), entry.metadata());
        }

        return new ColonyIndex(rebuilt);
    }
}

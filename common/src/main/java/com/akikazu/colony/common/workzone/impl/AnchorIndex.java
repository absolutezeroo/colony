package com.akikazu.colony.common.workzone.impl;

import com.akikazu.colony.api.building.BuildingId;
import com.akikazu.colony.api.workzone.AnchorId;
import com.akikazu.colony.core.registry.Identifier;
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
 * Always-loaded directory of every placed anchor on the server. Sibling to
 * {@link com.akikazu.colony.common.building.BuildingIndex} and
 * {@link com.akikazu.colony.common.storage.impl.ChestTypingIndex}: persisted as a single overworld-level
 * {@link SavedData} record so lookups by {@link AnchorId} or {@link BlockPos} stay O(1) without touching loaded chunks.
 *
 * <p>
 * Source of truth is the anchor's {@link net.minecraft.world.level.block.entity.BlockEntity}; this index holds only the
 * metadata needed to answer "is this position an anchor?" and "which anchors does building X own?" without paging in
 * the full per-anchor state.
 */
public final class AnchorIndex extends SavedData
{
    public static final String DATA_NAME = "colony_anchor_index";

    private static final Codec<Entry> ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            AnchorId.CODEC.fieldOf("id").forGetter(Entry::id),
            AnchorMetadata.CODEC.fieldOf("metadata").forGetter(Entry::metadata))
            .apply(instance, Entry::new));

    private static final Codec<List<Entry>> ENTRIES_CODEC = ENTRY_CODEC.listOf();

    private record Entry(AnchorId id, AnchorMetadata metadata)
    {
    }

    private final Map<AnchorId, AnchorMetadata> entries;

    public AnchorIndex()
    {
        this.entries = new LinkedHashMap<>();
    }

    private AnchorIndex(Map<AnchorId, AnchorMetadata> entries)
    {
        this.entries = new LinkedHashMap<>(entries);
    }

    public static Factory<AnchorIndex> factory()
    {
        return new Factory<>(AnchorIndex::new, AnchorIndex::load);
    }

    public static AnchorIndex get(ServerLevel level)
    {
        return level.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(factory(), DATA_NAME);
    }

    public boolean registerIfAbsent(AnchorId id, Identifier type, BlockPos position, Optional<BuildingId> link)
    {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(link, "link");

        if (entries.containsKey(id))
        {
            return false;
        }

        entries.put(id, new AnchorMetadata(type, position.immutable(), link));
        setDirty();

        return true;
    }

    public boolean updateLink(AnchorId id, Optional<BuildingId> link)
    {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(link, "link");

        AnchorMetadata existing = entries.get(id);

        if (existing == null)
        {
            return false;
        }

        entries.put(id, existing.withLink(link));
        setDirty();

        return true;
    }

    public boolean remove(AnchorId id)
    {
        Objects.requireNonNull(id, "id");

        if (entries.remove(id) == null)
        {
            return false;
        }

        setDirty();

        return true;
    }

    public Optional<AnchorMetadata> find(AnchorId id)
    {
        Objects.requireNonNull(id, "id");

        return Optional.ofNullable(entries.get(id));
    }

    public Optional<Map.Entry<AnchorId, AnchorMetadata>> findByPosition(BlockPos pos)
    {
        Objects.requireNonNull(pos, "pos");

        return entries.entrySet().stream()
                .filter(e -> e.getValue().position().equals(pos))
                .map(e -> (Map.Entry<AnchorId, AnchorMetadata>) new java.util.AbstractMap.SimpleImmutableEntry<>(
                        e.getKey(), e.getValue()))
                .findFirst();
    }

    public Stream<Map.Entry<AnchorId, AnchorMetadata>> allInDimension()
    {
        return entries.entrySet().stream();
    }

    public Stream<Map.Entry<AnchorId, AnchorMetadata>> allLinkedTo(BuildingId building)
    {
        Objects.requireNonNull(building, "building");

        return entries.entrySet().stream()
                .filter(e -> e.getValue().linkedBuilding().map(b -> b.equals(building)).orElse(false));
    }

    public Map<AnchorId, AnchorMetadata> entries()
    {
        return Collections.unmodifiableMap(entries);
    }

    public int size()
    {
        return entries.size();
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

    static AnchorIndex load(CompoundTag tag, HolderLookup.Provider provider)
    {
        if (!tag.contains("entries"))
        {
            return new AnchorIndex();
        }

        List<Entry> decoded = ENTRIES_CODEC
                .parse(NbtOps.INSTANCE, tag.get("entries"))
                .getOrThrow();

        Map<AnchorId, AnchorMetadata> rebuilt = new LinkedHashMap<>();

        for (Entry entry : decoded)
        {
            rebuilt.put(entry.id(), entry.metadata());
        }

        return new AnchorIndex(rebuilt);
    }
}

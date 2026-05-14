package com.akikazu.colony.common.citizen.assignment;

import com.akikazu.colony.api.building.room.RoomId;
import com.akikazu.colony.api.citizen.CitizenId;
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
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Always-loaded index of citizen→home-room assignments.
 *
 * <p>
 * Mirrors the shape of {@link com.akikazu.colony.common.colony.ColonyIndex ColonyIndex} and
 * {@link com.akikazu.colony.common.building.BuildingIndex BuildingIndex}: a single overworld-level {@link SavedData}
 * record that acts as the source of truth for the inverse {@code RoomId → citizens} lookup. The forward
 * {@code CitizenId → RoomId} mapping is also mirrored on {@code EntityCitizen} NBT for cheap goal evaluation, but the
 * index is what {@link com.akikazu.colony.api.citizen.CitizenAssignmentService#citizensInRoom CitizensInRoom} reads
 * from so a server reload doesn't need to scan every loaded entity.
 */
public final class CitizenAssignmentIndex extends SavedData
{
    public static final String DATA_NAME = "colony_citizen_assignments";

    private static final Codec<Entry> ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CitizenId.CODEC.fieldOf("citizen").forGetter(Entry::citizen),
            RoomId.CODEC.fieldOf("room").forGetter(Entry::room))
            .apply(instance, Entry::new));

    private static final Codec<List<Entry>> ENTRIES_CODEC = ENTRY_CODEC.listOf();

    private record Entry(CitizenId citizen, RoomId room)
    {
    }

    private final Map<CitizenId, RoomId> entries;

    public CitizenAssignmentIndex()
    {
        this.entries = new LinkedHashMap<>();
    }

    private CitizenAssignmentIndex(Map<CitizenId, RoomId> entries)
    {
        this.entries = new LinkedHashMap<>(entries);
    }

    public static Factory<CitizenAssignmentIndex> factory()
    {
        return new Factory<>(CitizenAssignmentIndex::new, CitizenAssignmentIndex::load);
    }

    public static CitizenAssignmentIndex get(ServerLevel level)
    {
        Objects.requireNonNull(level, "level");

        return level.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(factory(), DATA_NAME);
    }

    public void assign(CitizenId citizen, RoomId room)
    {
        Objects.requireNonNull(citizen, "citizen");
        Objects.requireNonNull(room, "room");

        RoomId previous = entries.put(citizen, room);

        if (!room.equals(previous))
        {
            setDirty();
        }
    }

    public void unassign(CitizenId citizen)
    {
        Objects.requireNonNull(citizen, "citizen");

        if (entries.remove(citizen) != null)
        {
            setDirty();
        }
    }

    public Optional<RoomId> roomOf(CitizenId citizen)
    {
        Objects.requireNonNull(citizen, "citizen");

        return Optional.ofNullable(entries.get(citizen));
    }

    public Stream<CitizenId> citizensInRoom(RoomId room)
    {
        Objects.requireNonNull(room, "room");

        return entries.entrySet().stream()
                .filter(e -> room.equals(e.getValue()))
                .map(Map.Entry::getKey);
    }

    public Map<CitizenId, RoomId> entries()
    {
        return Collections.unmodifiableMap(entries);
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

    static CitizenAssignmentIndex load(CompoundTag tag, HolderLookup.Provider provider)
    {
        if (!tag.contains("entries"))
        {
            return new CitizenAssignmentIndex();
        }

        List<Entry> decoded = ENTRIES_CODEC
                .parse(NbtOps.INSTANCE, tag.get("entries"))
                .getOrThrow();

        Map<CitizenId, RoomId> rebuilt = new LinkedHashMap<>();

        for (Entry entry : decoded)
        {
            rebuilt.put(entry.citizen(), entry.room());
        }

        return new CitizenAssignmentIndex(rebuilt);
    }
}

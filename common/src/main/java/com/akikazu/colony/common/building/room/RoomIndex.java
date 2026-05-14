package com.akikazu.colony.common.building.room;

import com.akikazu.colony.api.building.BuildingId;
import com.akikazu.colony.api.building.room.FreeformZone;
import com.akikazu.colony.api.building.room.Room;
import com.akikazu.colony.api.building.room.RoomId;
import com.akikazu.colony.common.bootstrap.ColonyBootstrap;
import com.akikazu.colony.core.registry.Identifier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Always-loaded index of designated rooms.
 *
 * <p>
 * Mirrors the role of {@link com.akikazu.colony.common.building.BuildingIndex BuildingIndex}: a single overworld-level
 * {@link SavedData} record that lets server-side systems (citizen goals, the future Building GUI) look up the geometry
 * of a designated {@link Room} by its {@link RoomId} without scanning every loaded chunk.
 *
 * <p>
 * V1 stores only the minimum needed to navigate citizens home: building affiliation, room type id, and footprint.
 * {@link com.akikazu.colony.api.building.room.RoomStatus RoomStatus} is intentionally not persisted — it is re-derived
 * by {@link RoomValidator#reevaluate(ServerLevel, Room) reevaluate} when the server actually needs it. The room type is
 * rehydrated from the {@link ColonyBootstrap#roomTypes ROOM_TYPE} registry on load.
 */
public final class RoomIndex extends SavedData
{
    public static final String DATA_NAME = "colony_room_index";

    private static final Codec<Entry> ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RoomId.CODEC.fieldOf("id").forGetter(Entry::id),
            BuildingId.CODEC.fieldOf("building").forGetter(Entry::building),
            Identifier.CODEC.fieldOf("type").forGetter(Entry::type),
            FreeformZone.CODEC.fieldOf("zone").forGetter(Entry::zone))
            .apply(instance, Entry::new));

    private static final Codec<List<Entry>> ENTRIES_CODEC = ENTRY_CODEC.listOf();

    public record Entry(RoomId id, BuildingId building, Identifier type, FreeformZone zone)
    {
        public Optional<Room> rehydrate()
        {
            return ColonyBootstrap.roomTypes()
                    .get(type)
                    .map(roomType -> new Room(
                            id.value(),
                            building,
                            roomType,
                            zone,
                            com.akikazu.colony.api.building.room.RoomStatus.valid()));
        }
    }

    private final Map<RoomId, Entry> entries;

    public RoomIndex()
    {
        this.entries = new LinkedHashMap<>();
    }

    private RoomIndex(Map<RoomId, Entry> entries)
    {
        this.entries = new LinkedHashMap<>(entries);
    }

    public static Factory<RoomIndex> factory()
    {
        return new Factory<>(RoomIndex::new, RoomIndex::load);
    }

    public static RoomIndex get(ServerLevel level)
    {
        Objects.requireNonNull(level, "level");

        return level.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(factory(), DATA_NAME);
    }

    public RoomId register(Room room)
    {
        Objects.requireNonNull(room, "room");

        RoomId id = new RoomId(room.id());
        entries.put(id, new Entry(id, room.building(), room.type().id(), room.zone()));
        setDirty();

        return id;
    }

    public Optional<Entry> findEntry(RoomId id)
    {
        Objects.requireNonNull(id, "id");

        return Optional.ofNullable(entries.get(id));
    }

    public Optional<Room> find(RoomId id)
    {
        return findEntry(id).flatMap(Entry::rehydrate);
    }

    public Stream<Room> allInBuilding(BuildingId building)
    {
        Objects.requireNonNull(building, "building");

        return entries.values().stream()
                .filter(e -> e.building().equals(building))
                .map(Entry::rehydrate)
                .flatMap(Optional::stream);
    }

    public int size()
    {
        return entries.size();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider)
    {
        List<Entry> snapshot = List.copyOf(entries.values());
        DataResult<Tag> encoded = ENTRIES_CODEC.encodeStart(NbtOps.INSTANCE, snapshot);
        tag.put("entries", encoded.getOrThrow());

        return tag;
    }

    static RoomIndex load(CompoundTag tag, HolderLookup.Provider provider)
    {
        if (!tag.contains("entries"))
        {
            return new RoomIndex();
        }

        List<Entry> decoded = ENTRIES_CODEC
                .parse(NbtOps.INSTANCE, tag.get("entries"))
                .getOrThrow();

        Map<RoomId, Entry> rebuilt = new LinkedHashMap<>();

        for (Entry entry : decoded)
        {
            rebuilt.put(entry.id(), entry);
        }

        return new RoomIndex(rebuilt);
    }
}

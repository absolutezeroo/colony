package com.akikazu.colony.common.citizen.assignment;

import com.akikazu.colony.api.building.room.RoomId;
import com.akikazu.colony.api.citizen.CitizenAssignmentService;
import com.akikazu.colony.api.citizen.CitizenId;

import net.minecraft.server.level.ServerLevel;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Server-thread-only {@link CitizenAssignmentService} backed by {@link CitizenAssignmentIndex}.
 *
 * <p>
 * Constructed per {@link ServerLevel}; instances hold no mutable state of their own — all reads and writes go through
 * the SavedData. The accompanying {@code EntityCitizen} also mirrors its own assignment in entity NBT so the bedtime
 * goal does not have to look up the index on every {@code canUse()} tick.
 */
public final class CitizenAssignmentServiceImpl implements CitizenAssignmentService
{
    private final CitizenAssignmentIndex index;

    private CitizenAssignmentServiceImpl(CitizenAssignmentIndex index)
    {
        this.index = index;
    }

    public static CitizenAssignmentServiceImpl get(ServerLevel level)
    {
        Objects.requireNonNull(level, "level");

        return new CitizenAssignmentServiceImpl(CitizenAssignmentIndex.get(level));
    }

    public static CitizenAssignmentServiceImpl wrapping(CitizenAssignmentIndex index)
    {
        return new CitizenAssignmentServiceImpl(Objects.requireNonNull(index, "index"));
    }

    @Override
    public void assignHomeRoom(CitizenId citizen, RoomId room)
    {
        Objects.requireNonNull(citizen, "citizen");
        Objects.requireNonNull(room, "room");

        index.assign(citizen, room);
    }

    @Override
    public void unassignHomeRoom(CitizenId citizen)
    {
        Objects.requireNonNull(citizen, "citizen");

        index.unassign(citizen);
    }

    @Override
    public Stream<CitizenId> citizensInRoom(RoomId room)
    {
        Objects.requireNonNull(room, "room");

        return index.citizensInRoom(room);
    }

    public Optional<RoomId> assignedHomeRoom(CitizenId citizen)
    {
        Objects.requireNonNull(citizen, "citizen");

        return index.roomOf(citizen);
    }
}

package com.akikazu.colony.api.citizen;

import com.akikazu.colony.api.building.room.RoomId;

import org.jetbrains.annotations.ApiStatus;

import java.util.stream.Stream;

/**
 * Server-side surface for binding citizens to {@link com.akikazu.colony.api.building.room.Room rooms}. V1 wires only
 * the home-room assignment used by the sleep behavior; later milestones will layer work-room and other affiliations on
 * top.
 *
 * <p>
 * Implementations live in {@code :common} and persist their state alongside the colony's other always-loaded indices —
 * see the corresponding entity NBT and per-colony saved data for the actual storage layout. Mutating calls must run on
 * the server thread; {@link #citizensInRoom(RoomId)} is safe to call from any thread that holds a stable view of the
 * underlying record.
 */
@ApiStatus.NonExtendable
public interface CitizenAssignmentService
{
    void assignHomeRoom(CitizenId citizen, RoomId room);

    void unassignHomeRoom(CitizenId citizen);

    Stream<CitizenId> citizensInRoom(RoomId room);
}

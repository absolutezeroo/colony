package com.akikazu.colony.api.building.room;

import com.akikazu.colony.api.building.BuildingId;

import java.util.Objects;
import java.util.UUID;

/**
 * A designated sub-zone within a {@link com.akikazu.colony.api.building.Building Building}, bound to exactly one
 * {@link RoomType}. Created by the room painting workflow once the player presses Enter; the painting itself is
 * validated independently and is not re-run at re-evaluation time.
 *
 * <p>
 * {@link #status()} reflects the result of the most recent {@link RoomRequirement} pass. Failing rooms remain in the
 * registry so the player can iterate; the Building GUI surfaces the failure reasons and offers a manual re-evaluate
 * action.
 */
public record Room(
        UUID id,
        BuildingId building,
        RoomType type,
        FreeformZone zone,
        RoomStatus status)
{
    public Room
    {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(building, "building");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(zone, "zone");
        Objects.requireNonNull(status, "status");
    }

    public static Room of(BuildingId building, RoomType type, FreeformZone zone, RoomStatus status)
    {
        return new Room(UUID.randomUUID(), building, type, zone, status);
    }

    public Room withStatus(RoomStatus newStatus)
    {
        return new Room(id, building, type, zone, newStatus);
    }
}

package com.akikazu.colony.common.building.room;

import com.akikazu.colony.api.building.BuildingId;
import com.akikazu.colony.api.building.room.FreeformZone;
import com.akikazu.colony.api.building.room.Room;
import com.akikazu.colony.api.building.room.RoomStatus;
import com.akikazu.colony.api.building.room.RoomType;
import com.akikazu.colony.common.building.room.RoomRequirementEvaluator.RequirementEvaluation;

import net.minecraft.server.level.ServerLevel;

import java.util.Objects;

/**
 * Server-side entry point for the moment the player presses Enter on a painted room footprint.
 *
 * <p>
 * Designed to be loader-agnostic so the {@code :neoforge} payload handler can call it from a packet, GameTests can call
 * it from a synthetic context, and unit tests can exercise {@link RoomRequirementEvaluator#evaluateAgainst} directly.
 * Validation here is the binary V1 pass — geometric constraints (zone inside outer zone, no overlap with siblings) are
 * checked during painting; this class only re-runs the {@link RoomRequirementEvaluator requirement pass} and packages
 * the outcome into a {@link Room} with the matching {@link RoomStatus}.
 */
public final class RoomValidator
{
    private RoomValidator()
    {
    }

    public static Room confirm(ServerLevel level, BuildingId building, RoomType type, FreeformZone zone)
    {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(building, "building");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(zone, "zone");

        RequirementEvaluation evaluation = RoomRequirementEvaluator.evaluate(level, zone, type.requirements());
        RoomStatus status = RoomRequirementEvaluator.toStatus(evaluation);

        return Room.of(building, type, zone, status);
    }

    public static Room reevaluate(ServerLevel level, Room room)
    {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(room, "room");

        RequirementEvaluation evaluation = RoomRequirementEvaluator.evaluate(
                level,
                room.zone(),
                room.type().requirements());
        RoomStatus status = RoomRequirementEvaluator.toStatus(evaluation);

        return room.withStatus(status);
    }
}

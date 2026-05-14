package com.akikazu.colony.api.building.room;

import com.akikazu.colony.core.registry.Identifier;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

/**
 * Marker type for a registered room. Implementations are produced by content registrations and dispatched via
 * {@link com.akikazu.colony.api.registry.ColonyRegistries#ROOM_TYPE}.
 *
 * <p>
 * A room type carries the list of {@link RoomRequirement}s that must hold for a designated room of this type to be
 * marked {@link RoomStatus.Valid valid} at confirmation time. V1 only wires
 * {@link RoomRequirement.FunctionalBlockCountRequirement} through to evaluation; the other variants are declared so
 * JSON profiles can already enumerate them.
 */
@ApiStatus.NonExtendable
public interface RoomType
{
    Identifier id();

    List<RoomRequirement> requirements();
}

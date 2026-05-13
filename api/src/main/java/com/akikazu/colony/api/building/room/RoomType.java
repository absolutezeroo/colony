package com.akikazu.colony.api.building.room;

import com.akikazu.colony.core.registry.Identifier;
import org.jetbrains.annotations.ApiStatus;

/**
 * Marker type for a registered room. Implementations are produced by content registrations and dispatched via
 * {@link com.akikazu.colony.api.registry.ColonyRegistries#ROOM_TYPE}.
 */
@ApiStatus.NonExtendable
public interface RoomType
{
    Identifier id();
}

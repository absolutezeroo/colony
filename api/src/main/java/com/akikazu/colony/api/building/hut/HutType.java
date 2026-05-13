package com.akikazu.colony.api.building.hut;

import com.akikazu.colony.core.registry.Identifier;
import org.jetbrains.annotations.ApiStatus;

/**
 * Marker type for a registered hut. Implementations are produced by content registrations and dispatched via
 * {@link com.akikazu.colony.api.registry.ColonyRegistries#HUT_TYPE}.
 */
@ApiStatus.NonExtendable
public interface HutType
{
    Identifier id();
}

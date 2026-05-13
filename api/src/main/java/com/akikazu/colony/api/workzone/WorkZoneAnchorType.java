package com.akikazu.colony.api.workzone;

import com.akikazu.colony.core.registry.Identifier;
import org.jetbrains.annotations.ApiStatus;

/**
 * Marker type for a registered work-zone anchor. Implementations are produced by content registrations and dispatched
 * via {@link com.akikazu.colony.api.registry.ColonyRegistries#ANCHOR_TYPE}.
 */
@ApiStatus.NonExtendable
public interface WorkZoneAnchorType
{
    Identifier id();
}

package com.akikazu.colony.api.storage;

import com.akikazu.colony.core.registry.Identifier;
import org.jetbrains.annotations.ApiStatus;

/**
 * Marker type for a registered storage role. Implementations are produced by content registrations and dispatched via
 * {@link com.akikazu.colony.api.registry.ColonyRegistries#STORAGE_ROLE_TYPE}.
 */
@ApiStatus.NonExtendable
public interface StorageRoleType
{
    Identifier id();
}

package com.akikazu.colony.api.building.hut;

import com.akikazu.colony.core.registry.Identifier;

import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.ApiStatus;

/**
 * Marker type for a registered hut. Implementations are produced by content registrations and dispatched via
 * {@link com.akikazu.colony.api.registry.ColonyRegistries#HUT_TYPE}.
 *
 * <p>
 * Each registered hut exposes a stable identifier and a localized display name used in HUDs, chat acknowledgements, and
 * GUI labels during the {@code PendingPlacement} workflow.
 */
@ApiStatus.NonExtendable
public interface HutType
{
    Identifier id();

    Component displayName();
}

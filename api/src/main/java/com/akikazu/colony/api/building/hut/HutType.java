package com.akikazu.colony.api.building.hut;

import com.akikazu.colony.api.storage.StorageSlot;
import com.akikazu.colony.api.workzone.AnchorSlot;
import com.akikazu.colony.core.registry.Identifier;

import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.ApiStatus;

import java.util.List;

/**
 * Marker type for a registered hut. Implementations are produced by content registrations and dispatched via
 * {@link com.akikazu.colony.api.registry.ColonyRegistries#HUT_TYPE}.
 *
 * <p>
 * Each registered hut exposes a stable identifier and a localized display name used in HUDs, chat acknowledgements, and
 * GUI labels during the {@code PendingPlacement} workflow. {@link #storageSlots()} declares the typed-chest slots the
 * hut owns; {@link #anchorSlots()} declares the work-zone anchors it can link. Both default to empty so existing hut
 * types stay source-compatible until they opt in.
 */
@ApiStatus.NonExtendable
public interface HutType
{
    Identifier id();

    Component displayName();

    default List<StorageSlot> storageSlots()
    {
        return List.of();
    }

    default List<AnchorSlot> anchorSlots()
    {
        return List.of();
    }
}

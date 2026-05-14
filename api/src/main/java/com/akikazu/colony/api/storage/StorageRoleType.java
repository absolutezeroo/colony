package com.akikazu.colony.api.storage;

import com.akikazu.colony.core.registry.Identifier;

import net.minecraft.network.chat.Component;

/**
 * Registered storage role for typed chests. Each role pairs an {@link ItemFilter} with display metadata used by HUDs,
 * the chest-typing particle indicator, and slot vetting. Implementations are produced by content registrations and
 * dispatched via {@link com.akikazu.colony.api.registry.ColonyRegistries#STORAGE_ROLE_TYPE}.
 *
 * <p>
 * The five V1 roles (input / output / tools / materials / general) live in {@code :common} under
 * {@code com.akikazu.colony.common.storage.role}; mod-added roles register through the same registry from their own
 * bootstrap.
 */
public interface StorageRoleType
{
    Identifier id();

    Component displayName();

    int particleColor();

    ItemFilter filter();
}

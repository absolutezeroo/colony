package com.akikazu.colony.common.storage.role;

import com.akikazu.colony.api.storage.ItemFilter;
import com.akikazu.colony.api.storage.StorageRoleType;
import com.akikazu.colony.api.storage.StorageRoles;
import com.akikazu.colony.common.storage.filter.AnyItemFilter;
import com.akikazu.colony.core.registry.Identifier;

import net.minecraft.network.chat.Component;

/**
 * Built-in {@code general} {@link StorageRoleType}: the catch-all role. Accepts every stack and is used by huts that
 * declare a single fallback chest line rather than role-specific lines.
 */
public final class GeneralRoleType implements StorageRoleType
{
    public static final Identifier ID = StorageRoles.GENERAL;

    public static final GeneralRoleType INSTANCE = new GeneralRoleType();

    private static final Component DISPLAY_NAME = Component.translatable("colony.storage_role.general");

    private static final int PARTICLE_COLOR = 0xBDC3C7;

    private GeneralRoleType()
    {
    }

    @Override
    public Identifier id()
    {
        return ID;
    }

    @Override
    public Component displayName()
    {
        return DISPLAY_NAME;
    }

    @Override
    public int particleColor()
    {
        return PARTICLE_COLOR;
    }

    @Override
    public ItemFilter filter()
    {
        return AnyItemFilter.INSTANCE;
    }
}

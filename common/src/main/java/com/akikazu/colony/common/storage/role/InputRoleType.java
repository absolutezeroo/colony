package com.akikazu.colony.common.storage.role;

import com.akikazu.colony.api.storage.ItemFilter;
import com.akikazu.colony.api.storage.StorageRoleType;
import com.akikazu.colony.api.storage.StorageRoles;
import com.akikazu.colony.common.storage.filter.AnyItemFilter;
import com.akikazu.colony.core.registry.Identifier;

import net.minecraft.network.chat.Component;

/**
 * Built-in {@code input} {@link StorageRoleType}: chests where the player deposits and citizens take. The role itself
 * accepts every stack; narrowing (e.g. seeds-only) happens at the {@link com.akikazu.colony.api.storage.StorageSlot
 * slot} level via {@code additionalFilter}.
 */
public final class InputRoleType implements StorageRoleType
{
    public static final Identifier ID = StorageRoles.INPUT;

    public static final InputRoleType INSTANCE = new InputRoleType();

    private static final Component DISPLAY_NAME = Component.translatable("colony.storage_role.input");

    private static final int PARTICLE_COLOR = 0x3A7BD5;

    private InputRoleType()
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

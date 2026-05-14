package com.akikazu.colony.common.storage.role;

import com.akikazu.colony.api.storage.ItemFilter;
import com.akikazu.colony.api.storage.StorageRoleType;
import com.akikazu.colony.api.storage.StorageRoles;
import com.akikazu.colony.common.storage.filter.AnyItemFilter;
import com.akikazu.colony.core.registry.Identifier;

import net.minecraft.network.chat.Component;

/**
 * Built-in {@code output} {@link StorageRoleType}: chests where citizens deposit and the player takes. Like
 * {@link InputRoleType}, the role accepts every stack and per-slot narrowing decides what each individual output line
 * is willing to hold.
 */
public final class OutputRoleType implements StorageRoleType
{
    public static final Identifier ID = StorageRoles.OUTPUT;

    public static final OutputRoleType INSTANCE = new OutputRoleType();

    private static final Component DISPLAY_NAME = Component.translatable("colony.storage_role.output");

    private static final int PARTICLE_COLOR = 0x2ECC71;

    private OutputRoleType()
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

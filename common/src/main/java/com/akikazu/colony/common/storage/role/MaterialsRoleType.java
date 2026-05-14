package com.akikazu.colony.common.storage.role;

import com.akikazu.colony.api.storage.ItemFilter;
import com.akikazu.colony.api.storage.StorageRoleType;
import com.akikazu.colony.api.storage.StorageRoles;
import com.akikazu.colony.common.storage.filter.AnyItemFilter;
import com.akikazu.colony.core.registry.Identifier;

import net.minecraft.network.chat.Component;

/**
 * Built-in {@code materials} {@link StorageRoleType}: bidirectional intermediate stock — flour, planks, wool — that
 * both producer and consumer huts share. Role-level filter is permissive; per-slot filters identify the specific
 * material a given hut wants.
 */
public final class MaterialsRoleType implements StorageRoleType
{
    public static final Identifier ID = StorageRoles.MATERIALS;

    public static final MaterialsRoleType INSTANCE = new MaterialsRoleType();

    private static final Component DISPLAY_NAME = Component.translatable("colony.storage_role.materials");

    private static final int PARTICLE_COLOR = 0xE67E22;

    private MaterialsRoleType()
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

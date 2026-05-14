package com.akikazu.colony.common.storage.role;

import com.akikazu.colony.api.storage.ItemFilter;
import com.akikazu.colony.api.storage.StorageRoleType;
import com.akikazu.colony.api.storage.StorageRoles;
import com.akikazu.colony.common.storage.filter.TaggedItemFilter;
import com.akikazu.colony.core.registry.Identifier;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * Built-in {@code tools} {@link StorageRoleType}. Chests typed with this role only accept items in the
 * {@link #TOOLS_TAG} item tag — populated via datagen and overridable by datapacks. Bidirectional: citizens take what
 * they need and return tools after their shift.
 */
public final class ToolsRoleType implements StorageRoleType
{
    public static final Identifier ID = StorageRoles.TOOLS;

    public static final TagKey<Item> TOOLS_TAG = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("colony", "tools"));

    public static final ToolsRoleType INSTANCE = new ToolsRoleType();

    private static final Component DISPLAY_NAME = Component.translatable("colony.storage_role.tools");

    private static final int PARTICLE_COLOR = 0xF1C40F;

    private static final ItemFilter FILTER = new TaggedItemFilter(TOOLS_TAG);

    private ToolsRoleType()
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
        return FILTER;
    }
}

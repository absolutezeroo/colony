package com.akikazu.colony.common.building.impl;

import com.akikazu.colony.api.building.hut.HutType;
import com.akikazu.colony.core.registry.Identifier;

import net.minecraft.network.chat.Component;

/**
 * Built-in {@link HutType} for the residence hut, registered under {@code colony:residence_hut}. First hut type in the
 * Phase 2 pending-placement workflow; additional types follow the same pattern in later prompts.
 */
public final class ResidenceHutType implements HutType
{
    public static final Identifier ID = Identifier.of("colony", "residence_hut");

    public static final ResidenceHutType INSTANCE = new ResidenceHutType();

    private static final Component DISPLAY_NAME = Component.translatable("hut.colony.residence_hut");

    private ResidenceHutType()
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
}

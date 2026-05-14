package com.akikazu.colony.neoforge.gametest.fixture;

import com.akikazu.colony.api.building.hut.HutType;
import com.akikazu.colony.api.storage.StorageRoles;
import com.akikazu.colony.api.storage.StorageSlot;
import com.akikazu.colony.core.registry.Identifier;

import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Test-only {@link HutType} carrying a single {@link StorageSlot} with configurable per-tier capacity. GameTests in
 * {@link com.akikazu.colony.neoforge.gametest.ChestTypingGameTests} attach this directly to a {@code BuildingMetadata}
 * registered into {@code BuildingIndex} — it is deliberately *not* registered in
 * {@link com.akikazu.colony.common.bootstrap.ColonyBootstrap} so it cannot leak into a real save and trip the registry-
 * lookup codec on load.
 */
public final class TestHutTypeWithStorage implements HutType
{
    public static final Identifier ID = Identifier.of("colony", "test/hut_with_storage");

    public static final Identifier SLOT_INPUT = Identifier.of("colony", "test_input");

    private static final Component DISPLAY_NAME = Component.translatable("hut.colony.test_hut_with_storage");

    private final List<StorageSlot> slots;

    public TestHutTypeWithStorage(int capacityAtTier1)
    {
        this.slots = List.of(StorageSlot.withConstantCapacity(SLOT_INPUT, StorageRoles.INPUT, capacityAtTier1));
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
    public List<StorageSlot> storageSlots()
    {
        return slots;
    }
}

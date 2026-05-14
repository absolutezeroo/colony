package com.akikazu.colony.common.storage.impl;

import com.akikazu.colony.api.building.BuildingId;
import com.akikazu.colony.api.colony.ColonyId;
import com.akikazu.colony.api.storage.StorageRoles;
import com.akikazu.colony.core.registry.Identifier;

import net.minecraft.core.BlockPos;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChestTypingIndexTest
{
    private static final ColonyId COLONY = ColonyId.random();

    private static final BuildingId BUILDING_A = BuildingId.random();

    private static final BuildingId BUILDING_B = BuildingId.random();

    private static final Identifier SLOT_INPUT = Identifier.of("colony", "test_input");

    private static final Identifier SLOT_OUTPUT = Identifier.of("colony", "test_output");

    @Test
    void assignStoresEntry()
    {
        ChestTypingIndex index = new ChestTypingIndex();
        BlockPos pos = new BlockPos(1, 2, 3);

        index.assign(pos, COLONY, BUILDING_A, SLOT_INPUT, StorageRoles.INPUT);

        Optional<TypedChest> found = index.findAt(pos);

        assertTrue(found.isPresent());
        assertEquals(BUILDING_A, found.get().building());
        assertEquals(SLOT_INPUT, found.get().slotId());
        assertEquals(StorageRoles.INPUT, found.get().role());
    }

    @Test
    void assignRejectsDifferentBuilding()
    {
        ChestTypingIndex index = new ChestTypingIndex();
        BlockPos pos = new BlockPos(0, 0, 0);

        index.assign(pos, COLONY, BUILDING_A, SLOT_INPUT, StorageRoles.INPUT);

        assertThrows(IllegalStateException.class,
                () -> index.assign(pos, COLONY, BUILDING_B, SLOT_INPUT, StorageRoles.INPUT));
    }

    @Test
    void assignAllowsRetypingSameBuilding()
    {
        ChestTypingIndex index = new ChestTypingIndex();
        BlockPos pos = new BlockPos(0, 0, 0);

        index.assign(pos, COLONY, BUILDING_A, SLOT_INPUT, StorageRoles.INPUT);
        index.assign(pos, COLONY, BUILDING_A, SLOT_OUTPUT, StorageRoles.OUTPUT);

        assertEquals(SLOT_OUTPUT, index.findAt(pos).orElseThrow().slotId());
    }

    @Test
    void clearRemovesEntry()
    {
        ChestTypingIndex index = new ChestTypingIndex();
        BlockPos pos = new BlockPos(5, 6, 7);
        index.assign(pos, COLONY, BUILDING_A, SLOT_INPUT, StorageRoles.INPUT);

        assertTrue(index.clear(pos));
        assertTrue(index.findAt(pos).isEmpty());
        assertFalse(index.clear(pos));
    }

    @Test
    void inBuildingFiltersByBuilding()
    {
        ChestTypingIndex index = new ChestTypingIndex();
        index.assign(new BlockPos(0, 0, 0), COLONY, BUILDING_A, SLOT_INPUT, StorageRoles.INPUT);
        index.assign(new BlockPos(1, 0, 0), COLONY, BUILDING_A, SLOT_OUTPUT, StorageRoles.OUTPUT);
        index.assign(new BlockPos(2, 0, 0), COLONY, BUILDING_B, SLOT_INPUT, StorageRoles.INPUT);

        List<TypedChest> inA = index.inBuilding(BUILDING_A).toList();
        List<TypedChest> inB = index.inBuilding(BUILDING_B).toList();

        assertEquals(2, inA.size());
        assertEquals(1, inB.size());
    }

    @Test
    void inSlotFiltersByBuildingAndSlot()
    {
        ChestTypingIndex index = new ChestTypingIndex();
        index.assign(new BlockPos(0, 0, 0), COLONY, BUILDING_A, SLOT_INPUT, StorageRoles.INPUT);
        index.assign(new BlockPos(1, 0, 0), COLONY, BUILDING_A, SLOT_INPUT, StorageRoles.INPUT);
        index.assign(new BlockPos(2, 0, 0), COLONY, BUILDING_A, SLOT_OUTPUT, StorageRoles.OUTPUT);

        assertEquals(2, index.inSlot(BUILDING_A, SLOT_INPUT).count());
        assertEquals(1, index.inSlot(BUILDING_A, SLOT_OUTPUT).count());
        assertEquals(0, index.inSlot(BUILDING_B, SLOT_INPUT).count());
    }
}

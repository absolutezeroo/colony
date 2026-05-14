package com.akikazu.colony.common.building;

import com.akikazu.colony.api.building.AxisAlignedOuterZone;
import com.akikazu.colony.api.building.BuildingId;
import com.akikazu.colony.api.colony.ColonyId;
import com.akikazu.colony.common.bootstrap.ColonyBootstrap;
import com.akikazu.colony.common.building.impl.ResidenceHutType;

import net.minecraft.core.BlockPos;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildingIndexTest
{
    private static final ColonyId COLONY = ColonyId.random();

    @BeforeAll
    static void registerHuts()
    {
        ColonyBootstrap.register();
    }

    private static BuildingMetadata residenceAt(BlockPos hutPos, AxisAlignedOuterZone zone)
    {
        return new BuildingMetadata(COLONY, ResidenceHutType.INSTANCE, hutPos, zone);
    }

    @Test
    void registerAddsEntry()
    {
        BuildingIndex index = new BuildingIndex();
        BuildingId id = BuildingId.random();
        AxisAlignedOuterZone zone = new AxisAlignedOuterZone(new BlockPos(0, 0, 0), new BlockPos(3, 3, 3));

        assertTrue(index.register(id, residenceAt(new BlockPos(1, 1, 1), zone)));
        assertEquals(1, index.size());

        Optional<BuildingMetadata> found = index.find(id);
        assertTrue(found.isPresent());
        assertSame(ResidenceHutType.INSTANCE, found.get().hutType());
    }

    @Test
    void registerRejectsDuplicate()
    {
        BuildingIndex index = new BuildingIndex();
        BuildingId id = BuildingId.random();
        BuildingMetadata meta = residenceAt(
                BlockPos.ZERO,
                new AxisAlignedOuterZone(new BlockPos(0, 0, 0), new BlockPos(3, 3, 3)));

        assertTrue(index.register(id, meta));
        assertFalse(index.register(id, meta), "Duplicate registration must be rejected");
    }

    @Test
    void hasOverlapDetectsOverlap()
    {
        BuildingIndex index = new BuildingIndex();
        AxisAlignedOuterZone existing = new AxisAlignedOuterZone(new BlockPos(0, 0, 0), new BlockPos(5, 5, 5));
        index.register(BuildingId.random(), residenceAt(new BlockPos(2, 2, 2), existing));

        AxisAlignedOuterZone overlap = new AxisAlignedOuterZone(new BlockPos(4, 4, 4), new BlockPos(8, 8, 8));

        assertTrue(index.hasOverlap(overlap));
    }

    @Test
    void hasOverlapAcceptsNonOverlap()
    {
        BuildingIndex index = new BuildingIndex();
        AxisAlignedOuterZone existing = new AxisAlignedOuterZone(new BlockPos(0, 0, 0), new BlockPos(5, 5, 5));
        index.register(BuildingId.random(), residenceAt(new BlockPos(2, 2, 2), existing));

        AxisAlignedOuterZone disjoint = new AxisAlignedOuterZone(new BlockPos(10, 0, 0), new BlockPos(14, 5, 5));

        assertFalse(index.hasOverlap(disjoint));
    }

    @Test
    void findByPositionReturnsContainingBuilding()
    {
        BuildingIndex index = new BuildingIndex();
        BuildingId targetId = BuildingId.random();
        AxisAlignedOuterZone zone = new AxisAlignedOuterZone(new BlockPos(0, 0, 0), new BlockPos(5, 5, 5));
        index.register(targetId, residenceAt(new BlockPos(2, 2, 2), zone));

        index.register(
                BuildingId.random(),
                residenceAt(
                        new BlockPos(20, 0, 0),
                        new AxisAlignedOuterZone(new BlockPos(18, 0, 0), new BlockPos(22, 4, 4))));

        Optional<BuildingMetadata> found = index.findByPosition(new BlockPos(3, 3, 3));
        assertTrue(found.isPresent());
        assertEquals(targetId, index.entries().entrySet().stream()
                .filter(e -> e.getValue().equals(found.get()))
                .findFirst()
                .map(java.util.Map.Entry::getKey)
                .orElseThrow());

        assertTrue(index.findByPosition(new BlockPos(100, 0, 0)).isEmpty());
    }

    @Test
    void allInColonyFiltersByColony()
    {
        BuildingIndex index = new BuildingIndex();
        ColonyId other = ColonyId.random();

        index.register(BuildingId.random(), residenceAt(
                BlockPos.ZERO, new AxisAlignedOuterZone(BlockPos.ZERO, new BlockPos(3, 3, 3))));
        index.register(BuildingId.random(), new BuildingMetadata(
                other,
                ResidenceHutType.INSTANCE,
                new BlockPos(10, 0, 0),
                new AxisAlignedOuterZone(new BlockPos(10, 0, 0), new BlockPos(13, 3, 3))));

        assertEquals(1, index.allInColony(COLONY).count());
        assertEquals(1, index.allInColony(other).count());
    }
}

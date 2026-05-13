package com.akikazu.colony.common.colony;

import com.akikazu.colony.api.colony.ColonyId;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit coverage for the part of {@link ColonyManager}'s contract that does not require a live {@code ServerLevel} or an
 * on-disk world: id allocation, index entry, and {@code find} semantics. The disk-write and event-dispatch wings of
 * {@code createColony} are exercised end-to-end by {@code TownHallFoundingGameTests}.
 */
class ColonyManagerTest
{
    private static ColonyMetadata anyMetadata()
    {
        return new ColonyMetadata(Level.OVERWORLD, BlockPos.ZERO, 0L);
    }

    @Test
    void createColonyAssignsRandomId()
    {
        Set<ColonyId> seen = new HashSet<>();

        for (int i = 0; i < 32; i++)
        {
            ColonyId id = ColonyId.random();

            assertNotNull(id);
            assertTrue(seen.add(id), "ColonyId.random() should produce a fresh value every call");
        }
    }

    @Test
    void createColonyRegistersInIndex()
    {
        ColonyIndex index = new ColonyIndex();
        ColonyId id = ColonyId.random();

        assertTrue(index.register(id, anyMetadata()));
        assertTrue(index.contains(id));
        assertEquals(1, index.size());
    }

    @Test
    void findReturnsRegisteredColony()
    {
        ColonyIndex index = new ColonyIndex();
        ColonyId id = ColonyId.random();
        ColonyMetadata metadata = new ColonyMetadata(Level.OVERWORLD, new BlockPos(7, 64, -3), 99L);

        index.register(id, metadata);

        Optional<ColonyMetadata> found = index.find(id);

        assertTrue(found.isPresent());
        assertEquals(metadata, found.get());
    }

    @Test
    void findReturnsEmptyForUnknown()
    {
        ColonyIndex index = new ColonyIndex();

        assertFalse(index.find(ColonyId.random()).isPresent());
    }

    @Test
    void allInDimensionFiltersByDimensionKey()
    {
        ColonyIndex index = new ColonyIndex();
        ColonyId overworldA = ColonyId.random();
        ColonyId overworldB = ColonyId.random();
        ColonyId nether = ColonyId.random();

        index.register(overworldA, new ColonyMetadata(Level.OVERWORLD, BlockPos.ZERO, 0L));
        index.register(overworldB, new ColonyMetadata(Level.OVERWORLD, new BlockPos(10, 70, 10), 0L));
        index.register(nether, new ColonyMetadata(Level.NETHER, BlockPos.ZERO, 0L));

        Set<ColonyId> inOverworld = index.allInDimension(Level.OVERWORLD).collect(java.util.stream.Collectors.toSet());

        assertEquals(Set.of(overworldA, overworldB), inOverworld);
    }
}

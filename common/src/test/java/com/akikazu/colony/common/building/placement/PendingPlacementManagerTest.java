package com.akikazu.colony.common.building.placement;

import com.akikazu.colony.api.building.hut.HutType;
import com.akikazu.colony.common.building.impl.ResidenceHutType;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PendingPlacementManagerTest
{
    private static final HutType HUT = ResidenceHutType.INSTANCE;

    @Test
    void startCreatesPending()
    {
        PendingPlacementManager manager = new PendingPlacementManager();
        UUID player = UUID.randomUUID();
        BlockPos pos = new BlockPos(4, 64, -7);

        manager.start(player, HUT, pos, Level.OVERWORLD, 12L);

        Optional<PendingPlacement> found = manager.get(player);

        assertTrue(found.isPresent(), "Pending placement should exist after start()");
        PendingPlacement pending = found.get();
        assertEquals(player, pending.playerUuid());
        assertSame(HUT, pending.hutType());
        assertEquals(pos, pending.targetHutPos());
        assertEquals(Level.OVERWORLD, pending.dimension());
        assertEquals(12L, pending.startedAtTick());
        assertEquals(PendingPlacementState.AWAITING_PAINTING, pending.state());
    }

    @Test
    void startReplacesExisting()
    {
        PendingPlacementManager manager = new PendingPlacementManager();
        UUID player = UUID.randomUUID();

        manager.start(player, HUT, new BlockPos(0, 0, 0), Level.OVERWORLD, 0L);
        manager.start(player, HUT, new BlockPos(10, 80, 10), Level.OVERWORLD, 50L);

        Optional<PendingPlacement> found = manager.get(player);

        assertTrue(found.isPresent(), "Pending placement should exist after replace");
        assertEquals(new BlockPos(10, 80, 10), found.get().targetHutPos());
        assertEquals(50L, found.get().startedAtTick());
    }

    @Test
    void cancelRemovesPending()
    {
        PendingPlacementManager manager = new PendingPlacementManager();
        UUID player = UUID.randomUUID();

        manager.start(player, HUT, new BlockPos(0, 0, 0), Level.OVERWORLD, 0L);
        Optional<PendingPlacement> removed = manager.cancel(player);

        assertTrue(removed.isPresent(), "cancel() should return the removed pending");
        assertTrue(manager.get(player).isEmpty(), "After cancel(), get() should be empty");
    }

    @Test
    void getReturnsEmptyForUnknownPlayer()
    {
        PendingPlacementManager manager = new PendingPlacementManager();

        assertTrue(manager.get(UUID.randomUUID()).isEmpty());
    }

    @Test
    void confirmRemovesPending()
    {
        PendingPlacementManager manager = new PendingPlacementManager();
        UUID player = UUID.randomUUID();

        manager.start(player, HUT, BlockPos.ZERO, Level.OVERWORLD, 0L);
        Optional<PendingPlacement> removed = manager.confirm(player);

        assertTrue(removed.isPresent(), "confirm() should return the removed pending");
        assertTrue(manager.get(player).isEmpty(), "After confirm(), get() should be empty");
    }
}

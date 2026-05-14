package com.akikazu.colony.api.building;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.google.gson.JsonElement;

import net.minecraft.core.BlockPos;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AxisAlignedOuterZoneTest
{
    @Test
    void containsAcceptsPointsInside()
    {
        AxisAlignedOuterZone zone = new AxisAlignedOuterZone(new BlockPos(0, 0, 0), new BlockPos(4, 4, 4));

        assertTrue(zone.contains(new BlockPos(0, 0, 0)));
        assertTrue(zone.contains(new BlockPos(4, 4, 4)));
        assertTrue(zone.contains(new BlockPos(2, 2, 2)));
    }

    @Test
    void containsRejectsPointsOutside()
    {
        AxisAlignedOuterZone zone = new AxisAlignedOuterZone(new BlockPos(0, 0, 0), new BlockPos(4, 4, 4));

        assertFalse(zone.contains(new BlockPos(-1, 0, 0)));
        assertFalse(zone.contains(new BlockPos(5, 0, 0)));
        assertFalse(zone.contains(new BlockPos(0, -1, 0)));
        assertFalse(zone.contains(new BlockPos(0, 5, 0)));
        assertFalse(zone.contains(new BlockPos(0, 0, -1)));
        assertFalse(zone.contains(new BlockPos(0, 0, 5)));
    }

    @Test
    void volumeIsCorrect()
    {
        AxisAlignedOuterZone unit = new AxisAlignedOuterZone(BlockPos.ZERO, BlockPos.ZERO);
        assertEquals(1, unit.volume());

        AxisAlignedOuterZone cube = new AxisAlignedOuterZone(new BlockPos(0, 0, 0), new BlockPos(4, 4, 4));
        assertEquals(125, cube.volume());

        AxisAlignedOuterZone slab = new AxisAlignedOuterZone(new BlockPos(0, 0, 0), new BlockPos(9, 0, 9));
        assertEquals(100, slab.volume());
    }

    @Test
    void fromCornersNormalizes()
    {
        AxisAlignedOuterZone a = AxisAlignedOuterZone.fromCorners(
                new BlockPos(5, 10, 3),
                new BlockPos(0, 2, 1));

        assertEquals(new BlockPos(0, 2, 1), a.min());
        assertEquals(new BlockPos(5, 10, 3), a.max());

        AxisAlignedOuterZone b = AxisAlignedOuterZone.fromCorners(
                new BlockPos(-3, -10, -1),
                new BlockPos(3, 10, 1));

        assertEquals(new BlockPos(-3, -10, -1), b.min());
        assertEquals(new BlockPos(3, 10, 1), b.max());
    }

    @Test
    void constructorRejectsInvertedCorners()
    {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AxisAlignedOuterZone(new BlockPos(5, 0, 0), new BlockPos(0, 0, 0)));
    }

    @Test
    void overlapsDetectsTouchingBoxes()
    {
        AxisAlignedOuterZone a = new AxisAlignedOuterZone(new BlockPos(0, 0, 0), new BlockPos(3, 3, 3));
        AxisAlignedOuterZone touching = new AxisAlignedOuterZone(new BlockPos(3, 0, 0), new BlockPos(6, 3, 3));
        AxisAlignedOuterZone disjoint = new AxisAlignedOuterZone(new BlockPos(4, 0, 0), new BlockPos(6, 3, 3));

        assertTrue(a.overlaps(touching), "touching boxes share a face block");
        assertFalse(a.overlaps(disjoint), "fully separated boxes must not overlap");
    }

    @Test
    void blocksInZoneEnumeratesEveryBlockExactlyOnce()
    {
        AxisAlignedOuterZone zone = new AxisAlignedOuterZone(new BlockPos(0, 0, 0), new BlockPos(1, 1, 1));
        List<BlockPos> seen = new ArrayList<>();

        for (BlockPos pos : zone.blocksInZone())
        {
            seen.add(pos.immutable());
        }

        assertEquals(8, seen.size());
        assertTrue(seen.contains(new BlockPos(0, 0, 0)));
        assertTrue(seen.contains(new BlockPos(1, 1, 1)));
    }

    @Test
    void codecRoundTrips()
    {
        AxisAlignedOuterZone original = new AxisAlignedOuterZone(new BlockPos(-3, 4, 7), new BlockPos(2, 8, 10));

        JsonElement encoded = AxisAlignedOuterZone.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
        DataResult<AxisAlignedOuterZone> parsed = AxisAlignedOuterZone.CODEC.parse(JsonOps.INSTANCE, encoded);

        assertTrue(parsed.result().isPresent(), "codec should successfully roundtrip");
        assertEquals(original, parsed.result().get());
    }
}

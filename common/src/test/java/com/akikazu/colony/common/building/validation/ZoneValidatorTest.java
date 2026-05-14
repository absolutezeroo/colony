package com.akikazu.colony.common.building.validation;

import com.akikazu.colony.api.building.AxisAlignedOuterZone;

import net.minecraft.core.BlockPos;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZoneValidatorTest
{
    @Test
    void validatesMinVolume()
    {
        AxisAlignedOuterZone tiny = new AxisAlignedOuterZone(BlockPos.ZERO, new BlockPos(1, 1, 1));
        List<ZoneValidationError> errors = new ArrayList<>();

        ZoneValidator.validateVolume(tiny, errors);

        assertEquals(1, errors.size());
        ZoneValidationError.TooSmall err = assertInstanceOf(ZoneValidationError.TooSmall.class, errors.get(0));
        assertEquals(ZoneValidator.MIN_VOLUME, err.minRequired());
        assertEquals(tiny.volume(), err.actual());
    }

    @Test
    void validatesMaxVolume()
    {
        AxisAlignedOuterZone huge = new AxisAlignedOuterZone(BlockPos.ZERO, new BlockPos(31, 31, 31));
        List<ZoneValidationError> errors = new ArrayList<>();

        ZoneValidator.validateVolume(huge, errors);

        assertEquals(1, errors.size());
        ZoneValidationError.TooLarge err = assertInstanceOf(ZoneValidationError.TooLarge.class, errors.get(0));
        assertEquals(ZoneValidator.MAX_VOLUME, err.maxAllowed());
        assertEquals(huge.volume(), err.actual());
    }

    @Test
    void acceptsMidVolume()
    {
        AxisAlignedOuterZone ok = new AxisAlignedOuterZone(BlockPos.ZERO, new BlockPos(3, 3, 3));
        List<ZoneValidationError> errors = new ArrayList<>();

        ZoneValidator.validateVolume(ok, errors);

        assertTrue(errors.isEmpty(), "Volume 64 must pass [16, 4096] bound");
    }

    @Test
    void validatesContainsHutPos()
    {
        AxisAlignedOuterZone zone = new AxisAlignedOuterZone(BlockPos.ZERO, new BlockPos(4, 4, 4));
        BlockPos outside = new BlockPos(10, 10, 10);
        List<ZoneValidationError> errors = new ArrayList<>();

        ZoneValidator.validateContainsHutPos(zone, outside, errors);

        assertEquals(1, errors.size());
        ZoneValidationError.DoesNotContainHutPos err = assertInstanceOf(ZoneValidationError.DoesNotContainHutPos.class,
                errors.get(0));
        assertEquals(outside, err.hutPos());
    }

    @Test
    void containedHutPosProducesNoError()
    {
        AxisAlignedOuterZone zone = new AxisAlignedOuterZone(BlockPos.ZERO, new BlockPos(4, 4, 4));
        List<ZoneValidationError> errors = new ArrayList<>();

        ZoneValidator.validateContainsHutPos(zone, new BlockPos(2, 2, 2), errors);

        assertTrue(errors.isEmpty());
    }

    @Test
    void zoneValidationResultInvalidRejectsEmptyErrors()
    {
        ZoneValidationResult.Invalid first = new ZoneValidationResult.Invalid(
                List.of(new ZoneValidationError.OutsideLoadedChunks()));

        assertFalse(first.errors().isEmpty());
    }
}

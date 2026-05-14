package com.akikazu.colony.common.building.validation;

import com.akikazu.colony.api.building.BuildingId;

import net.minecraft.core.BlockPos;

/**
 * Typed cause for a rejected zone painting attempt. Returned from {@code ZoneValidator.validate} via
 * {@link ZoneValidationResult.Invalid}. The chat message presented to the player is derived from the first error in the
 * list — see {@code colony.message.zone_validation.*} in the lang file.
 */
public sealed interface ZoneValidationError
{
    record TooSmall(int actual, int minRequired) implements ZoneValidationError
    {
    }

    record TooLarge(int actual, int maxAllowed) implements ZoneValidationError
    {
    }

    record DoesNotContainHutPos(BlockPos hutPos) implements ZoneValidationError
    {
    }

    record OverlapsExistingBuilding(BuildingId conflict) implements ZoneValidationError
    {
    }

    record OutsideLoadedChunks() implements ZoneValidationError
    {
    }
}

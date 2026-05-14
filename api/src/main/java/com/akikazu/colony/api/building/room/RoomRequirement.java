package com.akikazu.colony.api.building.room;

import com.akikazu.colony.core.registry.Identifier;

import java.util.Objects;

/**
 * Declarative gate a room must satisfy at confirmation time to be marked
 * {@link com.akikazu.colony.api.building.room.RoomStatus.Valid valid}.
 *
 * <p>
 * Variants are loaded from {@code room_type/*.json} via codec dispatch on a {@code "type"} field. V1 only wires
 * {@link FunctionalBlockCountRequirement} through to the evaluator — {@link EnclosureRequirement} and
 * {@link AccessRequirement} carry their typed parameters so JSON profiles can already declare them, and are evaluated
 * in later prompts.
 */
public sealed interface RoomRequirement
{
    /**
     * Asserts the room contains between {@code min} and {@code max} (inclusive) blocks tagged with the supplied
     * function identifier. Counted by scanning the room zone and asking every registered
     * {@link com.akikazu.colony.api.building.functional.FunctionalBlockDetector} whose
     * {@link com.akikazu.colony.api.building.functional.FunctionalBlockDetector#detects() detects()} matches.
     */
    record FunctionalBlockCountRequirement(Identifier function, int min, int max) implements RoomRequirement
    {
        public FunctionalBlockCountRequirement
        {
            Objects.requireNonNull(function, "function");

            if (min < 0)
            {
                throw new IllegalArgumentException("min must be >= 0, got " + min);
            }

            if (max < min)
            {
                throw new IllegalArgumentException(
                        "max must be >= min, got min=" + min + " max=" + max);
            }
        }
    }

    /**
     * Asserts the room is enclosed: at least {@code minWallRatio} of the room's frontier blocks are walls. Placeholder
     * variant; evaluation is wired in a later prompt.
     */
    record EnclosureRequirement(float minWallRatio) implements RoomRequirement
    {
        public EnclosureRequirement
        {
            if (minWallRatio < 0.0f || minWallRatio > 1.0f)
            {
                throw new IllegalArgumentException(
                        "minWallRatio must be in [0, 1], got " + minWallRatio);
            }
        }
    }

    /**
     * Asserts the room exposes between {@code minDoors} and {@code maxDoors} (inclusive) door blocks. Placeholder
     * variant; evaluation is wired in a later prompt.
     */
    record AccessRequirement(int minDoors, int maxDoors) implements RoomRequirement
    {
        public AccessRequirement
        {
            if (minDoors < 0)
            {
                throw new IllegalArgumentException("minDoors must be >= 0, got " + minDoors);
            }

            if (maxDoors < minDoors)
            {
                throw new IllegalArgumentException(
                        "maxDoors must be >= minDoors, got minDoors=" + minDoors + " maxDoors=" + maxDoors);
            }
        }
    }
}

package com.akikazu.colony.api.building.room;

import java.util.List;
import java.util.Objects;

/**
 * Result of {@link RoomRequirement} evaluation, attached to a designated room.
 *
 * <p>
 * Rooms that fail requirements are still created (so the player can fix them without redoing the painting work) but
 * marked {@link Invalid} with the typed list of failures. The Building GUI surfaces the failures and offers a manual
 * re-evaluate action; re-evaluation is also triggered by the standard debounced block-change pass.
 */
public sealed interface RoomStatus
{
    static RoomStatus valid()
    {
        return Valid.INSTANCE;
    }

    static RoomStatus invalid(List<RequirementError> errors)
    {
        return new Invalid(errors);
    }

    record Valid() implements RoomStatus
    {
        public static final Valid INSTANCE = new Valid();
    }

    record Invalid(List<RequirementError> errors) implements RoomStatus
    {
        public Invalid
        {
            Objects.requireNonNull(errors, "errors");

            if (errors.isEmpty())
            {
                throw new IllegalArgumentException("Invalid status must carry at least one error");
            }

            errors = List.copyOf(errors);
        }
    }

    /**
     * One failed requirement, paired with a localized-key-stable English reason that callers render in tooltips.
     */
    record RequirementError(RoomRequirement requirement, String reason)
    {
        public RequirementError
        {
            Objects.requireNonNull(requirement, "requirement");
            Objects.requireNonNull(reason, "reason");
        }
    }
}

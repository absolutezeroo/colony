package com.akikazu.colony.common.building.validation;

import com.akikazu.colony.api.building.AxisAlignedOuterZone;

import java.util.List;
import java.util.Objects;

/**
 * Outcome of {@code ZoneValidator.validate}.
 *
 * <p>
 * Sealed: callers exhaustively switch on the two variants. {@link Valid} carries the (normalized) zone the caller
 * should register; {@link Invalid} carries the typed errors so the chat / HUD layer can localize them.
 */
public sealed interface ZoneValidationResult
{
    record Valid(AxisAlignedOuterZone zone) implements ZoneValidationResult
    {
        public Valid
        {
            Objects.requireNonNull(zone, "zone");
        }
    }

    record Invalid(List<ZoneValidationError> errors) implements ZoneValidationResult
    {
        public Invalid
        {
            Objects.requireNonNull(errors, "errors");

            if (errors.isEmpty())
            {
                throw new IllegalArgumentException("Invalid result must carry at least one error");
            }

            errors = List.copyOf(errors);
        }
    }
}

package com.akikazu.colony.common.colony.registration;

import java.util.Locale;

/**
 * Stable, machine-readable reason codes for a rejected colony registration request.
 *
 * <p>
 * Sent over the wire to the requesting client; the client maps codes to localized user-facing messages. Keeping the
 * code set closed (an enum) ensures the server cannot ship a code the client cannot interpret.
 */
public enum RegistrationRejection
{
    NO_PERMISSION, NAME_LENGTH, NAME_CHARS, OUT_OF_RANGE, RATE_LIMITED, DUPLICATE_ID;

    public String wireCode()
    {
        return name().toLowerCase(Locale.ROOT);
    }
}

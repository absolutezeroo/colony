package com.akikazu.colony.common.colony.registration;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Pure validation rules for an incoming colony registration request.
 *
 * <p>
 * Lives in {@code :common} (no Minecraft import) so the rules can be exercised by headless JUnit tests. The wire-side
 * handler in {@code :neoforge} composes these checks with the Minecraft-specific range check and the rate limiter to
 * produce a single accept/reject outcome.
 */
public final class RegisterColonyValidator
{
    public static final int MIN_NAME_LENGTH = 3;

    public static final int MAX_NAME_LENGTH = 32;

    public static final int DEFAULT_RANGE_BLOCKS = 64;

    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z0-9 _\\-]+$");

    private RegisterColonyValidator()
    {
    }

    public static Optional<RegistrationRejection> validateName(String name)
    {
        int length = name.length();

        if (length < MIN_NAME_LENGTH || length > MAX_NAME_LENGTH)
        {
            return Optional.of(RegistrationRejection.NAME_LENGTH);
        }

        if (!NAME_PATTERN.matcher(name).matches())
        {
            return Optional.of(RegistrationRejection.NAME_CHARS);
        }

        return Optional.empty();
    }

    public static boolean isWithinRange(
            double playerX,
            double playerY,
            double playerZ,
            double targetX,
            double targetY,
            double targetZ,
            int maxBlocks)
    {
        double dx = playerX - targetX;
        double dy = playerY - targetY;
        double dz = playerZ - targetZ;
        double distSq = dx * dx + dy * dy + dz * dz;
        double maxSq = (double) maxBlocks * (double) maxBlocks;

        return distSq <= maxSq;
    }
}

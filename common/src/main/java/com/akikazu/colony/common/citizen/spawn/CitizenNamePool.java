package com.akikazu.colony.common.citizen.spawn;

import net.minecraft.util.RandomSource;

import java.util.List;

/**
 * Hardcoded display-name pool for newly spawned citizens.
 *
 * <p>
 * Intentionally 20 entries so that the four-citizen founding cohort almost never sees duplicates. JSON-driven trait /
 * name pools land in a later prompt; isolating the literal list here keeps the migration to a one-file change.
 */
public final class CitizenNamePool
{
    private static final List<String> NAMES = List.of(
            "Alice", "Bob", "Charlie", "Diana", "Erik",
            "Fiona", "Greta", "Hans", "Iris", "Jakob",
            "Kira", "Leo", "Mia", "Niko", "Olga",
            "Petra", "Quinn", "Rolf", "Stella", "Tobias");

    private CitizenNamePool()
    {
    }

    public static String randomName(RandomSource random)
    {
        return NAMES.get(random.nextInt(NAMES.size()));
    }

    public static List<String> all()
    {
        return NAMES;
    }
}

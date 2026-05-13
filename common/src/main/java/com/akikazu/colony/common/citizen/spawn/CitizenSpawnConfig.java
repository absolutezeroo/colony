package com.akikazu.colony.common.citizen.spawn;

/**
 * Tunables for the initial-citizen spawn flow invoked at colony founding.
 *
 * <p>
 * These are code constants today but are slated to move into {@code HearthboundServerConfig} (see CLAUDE.md) once the
 * server-config layer lands. Defaults match {@code docs/05-CITIZEN-SYSTEM.md}.
 */
public final class CitizenSpawnConfig
{
    public static final int STARTING_CITIZEN_COUNT = 4;

    public static final int CITIZEN_SPAWN_SEARCH_RADIUS = 6;

    public static final int CITIZEN_SPAWN_VERTICAL_SEARCH = 3;

    public static final int CITIZEN_SPAWN_RETRY_ATTEMPTS = 5;

    public static final int CITIZEN_SPAWN_RETRY_TICK_INTERVAL = 20;

    public static final int CITIZEN_SPAWN_RETRY_TIMEOUT_TICKS = 100;

    private CitizenSpawnConfig()
    {
    }
}

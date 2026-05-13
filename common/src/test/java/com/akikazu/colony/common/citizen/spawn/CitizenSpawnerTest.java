package com.akikazu.colony.common.citizen.spawn;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-logic coverage for {@link CitizenSpawner#isValidSpawnCells}. The {@link CitizenSpawner#isValidSpawn} overload
 * that operates on {@code BlockState} is exercised end-to-end by {@code TownHallFoundingGameTests}; it cannot be unit
 * tested here because instantiating a {@code BlockState} pulls in {@code Bootstrap.bootStrap()} which fails outside a
 * loaded NeoForge environment.
 */
class CitizenSpawnerTest
{
    @Test
    void isValidSpawnAcceptsAirAboveSolid()
    {
        assertTrue(CitizenSpawner.isValidSpawnCells(true, true, true));
    }

    @Test
    void isValidSpawnRejectsAirAboveAir()
    {
        assertFalse(CitizenSpawner.isValidSpawnCells(true, true, false));
    }

    @Test
    void isValidSpawnRejectsSolidAtPos()
    {
        assertFalse(CitizenSpawner.isValidSpawnCells(false, true, true));
    }

    @Test
    void isValidSpawnRejectsNonAirAbove()
    {
        assertFalse(CitizenSpawner.isValidSpawnCells(true, false, true));
    }
}

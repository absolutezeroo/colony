package com.akikazu.colony.api.item;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class ColonyToolModeTest
{
    @Test
    void nextWrapsAroundFromInspectToZone()
    {
        assertEquals(ColonyToolMode.STORAGE, ColonyToolMode.ZONE.next());
        assertEquals(ColonyToolMode.LINK, ColonyToolMode.STORAGE.next());
        assertEquals(ColonyToolMode.INSPECT, ColonyToolMode.LINK.next());
        assertEquals(ColonyToolMode.ZONE, ColonyToolMode.INSPECT.next());
    }

    @Test
    void previousWrapsAroundFromZoneToInspect()
    {
        assertEquals(ColonyToolMode.INSPECT, ColonyToolMode.ZONE.previous());
        assertEquals(ColonyToolMode.ZONE, ColonyToolMode.STORAGE.previous());
        assertEquals(ColonyToolMode.STORAGE, ColonyToolMode.LINK.previous());
        assertEquals(ColonyToolMode.LINK, ColonyToolMode.INSPECT.previous());
    }

    @Test
    @SuppressWarnings("EnumOrdinal")
    void byOrdinalRoundTrips()
    {
        for (ColonyToolMode mode : ColonyToolMode.values())
        {
            assertEquals(mode, ColonyToolMode.byOrdinal(mode.ordinal()));
        }
    }

    @Test
    void byOrdinalOutOfRangeFallsBackToDefault()
    {
        assertEquals(ColonyToolMode.DEFAULT, ColonyToolMode.byOrdinal(-1));
        assertEquals(ColonyToolMode.DEFAULT, ColonyToolMode.byOrdinal(99));
    }

    @Test
    void getNameMatchesSerializedName()
    {
        for (ColonyToolMode mode : ColonyToolMode.values())
        {
            assertEquals(mode.getName(), mode.getSerializedName());
        }
    }
}

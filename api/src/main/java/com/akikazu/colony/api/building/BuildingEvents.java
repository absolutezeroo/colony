package com.akikazu.colony.api.building;

import com.akikazu.colony.core.event.Event;

import java.util.Objects;
import java.util.UUID;

/**
 * Public event surface for buildings. Mirrors {@link com.akikazu.colony.api.colony.ColonyEvents}: POJO events
 * dispatched through the colony event bus exposed by {@code :common}.
 */
public final class BuildingEvents
{
    private BuildingEvents()
    {
    }

    /**
     * Fired once a building has been validated, its Hut block placed, and a record registered in the building index.
     * Listeners run synchronously on the server thread.
     */
    public record BuildingCreatedEvent(Building building, UUID creator) implements Event
    {
        public BuildingCreatedEvent
        {
            Objects.requireNonNull(building, "building");
            Objects.requireNonNull(creator, "creator");
        }
    }
}

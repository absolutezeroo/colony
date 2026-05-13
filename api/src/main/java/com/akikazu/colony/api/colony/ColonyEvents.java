package com.akikazu.colony.api.colony;

import com.akikazu.colony.core.event.Event;

import java.util.Objects;
import java.util.UUID;

/**
 * Public event surface for colony lifecycle. Events here are POJOs dispatched through the {@code ColonyEventBus}
 * exposed by {@code :common}; addons compiled against {@code :api} can subscribe without depending on Minecraft's event
 * bus.
 */
public final class ColonyEvents
{
    private ColonyEvents()
    {
    }

    /**
     * Fired immediately after a colony has been registered in {@code ColonyIndex} and its initial per-colony record
     * written. Listeners run synchronously on the server thread; throwing aborts the founding flow and rolls back the
     * caller, so handlers must stay short and avoid blocking I/O.
     */
    public record ColonyFoundedEvent(Colony colony, UUID founder) implements Event
    {
        public ColonyFoundedEvent
        {
            Objects.requireNonNull(colony, "colony");
            Objects.requireNonNull(founder, "founder");
        }
    }
}

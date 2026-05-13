package com.akikazu.colony.common.colony;

import com.akikazu.colony.core.event.EventBus;
import com.akikazu.colony.core.event.EventBuses;

/**
 * Singleton holder for the in-process colony event bus.
 *
 * <p>
 * Distinct from NeoForge's mod event bus by design: colony events are domain events with no Minecraft semantics. They
 * are dispatched synchronously on the calling thread (usually the server thread) so listeners that mutate world state
 * remain race-free. Tests subscribe directly via {@link #get()}, run, then unsubscribe — no per-test setup beyond that.
 */
public final class ColonyEventBus
{
    private static final EventBus INSTANCE = EventBuses.create("colony");

    private ColonyEventBus()
    {
    }

    public static EventBus get()
    {
        return INSTANCE;
    }
}

package com.akikazu.colony.core.event;

/**
 * Static factory for {@link EventBus} instances. The {@code EventBus} constructor is package-private so that creation
 * always flows through this factory.
 */
public final class EventBuses
{
    private EventBuses()
    {
    }

    public static EventBus create(String name)
    {
        return new EventBus(name);
    }
}

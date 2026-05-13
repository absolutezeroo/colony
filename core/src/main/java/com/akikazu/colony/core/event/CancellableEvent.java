package com.akikazu.colony.core.event;

/**
 * Event variant whose dispatch can be vetoed by a subscriber. The bus does not itself short-circuit on cancellation;
 * each subscriber decides whether to honour an existing veto by checking {@link #isCancelled()}.
 */
public interface CancellableEvent extends Event
{
    void cancel();

    boolean isCancelled();
}

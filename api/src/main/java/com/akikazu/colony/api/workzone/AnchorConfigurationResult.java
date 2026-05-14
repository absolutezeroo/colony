package com.akikazu.colony.api.workzone;

import net.minecraft.network.chat.Component;

import java.util.Objects;

/**
 * Outcome of an {@link AnchorConfigurationHandler#apply} attempt. Sealed so the dispatch layer (block right-click
 * handler) exhaustively switches over the two variants and produces the right side effects: chat feedback and stack
 * decrement on {@link Applied}; chat feedback only on {@link Rejected}.
 */
public sealed interface AnchorConfigurationResult
{
    record Applied(Component feedbackMessage, boolean consumeOneItem) implements AnchorConfigurationResult
    {
        public Applied
        {
            Objects.requireNonNull(feedbackMessage, "feedbackMessage");
        }
    }

    record Rejected(Component errorMessage) implements AnchorConfigurationResult
    {
        public Rejected
        {
            Objects.requireNonNull(errorMessage, "errorMessage");
        }
    }
}

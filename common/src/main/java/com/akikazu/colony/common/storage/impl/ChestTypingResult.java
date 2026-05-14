package com.akikazu.colony.common.storage.impl;

/**
 * Outcome of a {@link ChestTypingService#assignChest} attempt. Sealed so callers exhaustively switch over the two
 * variants; the failure cases collapse into a flat {@link Reason} enum so the chat-formatting and test-assertion layers
 * stay terse.
 */
public sealed interface ChestTypingResult
{
    record Success(TypedChest typed) implements ChestTypingResult
    {
    }

    record Rejected(Reason reason) implements ChestTypingResult
    {
    }

    enum Reason
    {
        NOT_A_CHEST, OUTSIDE_BUILDING, UNKNOWN_SLOT, CAPACITY_FULL, ALREADY_TYPED, NO_PERMISSION
    }
}

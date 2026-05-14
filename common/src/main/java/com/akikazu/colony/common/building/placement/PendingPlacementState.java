package com.akikazu.colony.common.building.placement;

/**
 * Lifecycle of a {@link PendingPlacement}. The player paints the outer zone while in {@link #AWAITING_PAINTING}; once
 * the painting succeeds validation the state moves to {@link #AWAITING_CONFIRMATION} until the player presses Enter or
 * Esc. Confirmation logic itself lands in prompt 2.3.
 */
public enum PendingPlacementState
{
    AWAITING_PAINTING, AWAITING_CONFIRMATION
}

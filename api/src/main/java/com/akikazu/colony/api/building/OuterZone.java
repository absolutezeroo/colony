package com.akikazu.colony.api.building;

import org.jetbrains.annotations.ApiStatus;

/**
 * Spatial envelope of a {@link Building}: the painted region that contains the Hut block and any rooms / typed storage
 * chests. See {@code docs/04-BUILDING-SYSTEM.md} — V1 ships an axis-aligned variant; the freeform variant lands in
 * prompt 2.3.
 */
@ApiStatus.NonExtendable
public sealed interface OuterZone permits AxisAlignedOuterZone
{
}

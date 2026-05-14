package com.akikazu.colony.neoforge.network;

import com.akikazu.colony.core.registry.Identifier;

import net.minecraft.core.BlockPos;

import org.jspecify.annotations.Nullable;

/**
 * Holder for the client-side view of an active pending placement. Receives data from
 * {@link SetPendingPlacementClientPayload} via {@link ColonyPayloads}; client rendering and key-handling code reads
 * from it. The class deliberately contains no Minecraft client imports so referencing it from server-side packet
 * dispatcher code does not pull client classes onto a dedicated server's classpath.
 */
public final class PendingPlacementClientState
{
    private static volatile @Nullable Snapshot current;

    private PendingPlacementClientState()
    {
    }

    public static void apply(SetPendingPlacementClientPayload payload)
    {
        Identifier hutTypeId = payload.hutTypeId();

        if (hutTypeId == null)
        {
            current = null;

            return;
        }

        current = new Snapshot(hutTypeId, payload.targetPos());
    }

    public static @Nullable Snapshot current()
    {
        return current;
    }

    public static void clear()
    {
        current = null;
    }

    public record Snapshot(Identifier hutTypeId, BlockPos targetPos)
    {
    }
}

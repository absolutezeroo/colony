package com.akikazu.colony.neoforge.network;

import net.minecraft.core.BlockPos;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side mirror of the typed chests the player should see particle indicators on. Updated by
 * {@link ChestTypedClientPayload}; held as a plain {@link Map} of position → role colour so the dispatcher in
 * {@link ColonyPayloads} can reference this class on both dist sides without dragging Minecraft client classes onto a
 * dedicated server's classpath.
 *
 * <p>
 * V1 tracks only chests typed during the current session — there is no initial sync of the full
 * {@link com.akikazu.colony.common.storage.impl.ChestTypingIndex} on player join yet. Once players regularly span
 * sessions with typed chests, a join-time push payload will hydrate this map from the server. (TODO(prompt 2.5):
 * initial typed-chest sync to subscribed clients.)
 */
public final class ChestTypingClientState
{
    private static final Map<BlockPos, Integer> TYPED = new ConcurrentHashMap<>();

    private ChestTypingClientState()
    {
    }

    public static void apply(ChestTypedClientPayload payload)
    {
        TYPED.put(payload.position().immutable(), payload.particleColor());
    }

    public static void forget(BlockPos position)
    {
        TYPED.remove(position);
    }

    public static Map<BlockPos, Integer> snapshot()
    {
        return Collections.unmodifiableMap(TYPED);
    }

    public static void clear()
    {
        TYPED.clear();
    }
}

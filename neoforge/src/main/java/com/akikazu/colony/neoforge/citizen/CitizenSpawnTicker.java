package com.akikazu.colony.neoforge.citizen;

import com.akikazu.colony.common.citizen.spawn.CitizenSpawner;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * NeoForge bridge that pumps the {@link CitizenSpawner} retry queue once per server tick.
 *
 * <p>
 * Pure delegation: holds no state of its own. Iterating every level rather than only the founding level lets the
 * spawner serve multi-dimension colonies once cross-dimension founding lands; today only the overworld is exercised but
 * the per-level keying inside the spawner means the cost stays O(pending), not O(level).
 */
public final class CitizenSpawnTicker
{
    @SubscribeEvent
    public void onServerTickPost(ServerTickEvent.Post event)
    {
        MinecraftServer server = event.getServer();

        for (ServerLevel level : server.getAllLevels())
        {
            CitizenSpawner.tickPending(level);
        }
    }
}

package com.akikazu.colony.neoforge.network;

import com.akikazu.colony.common.building.placement.PendingPlacementManager;
import com.akikazu.colony.common.colony.registration.RegistrationRateLimiter;
import com.akikazu.colony.common.storage.impl.SlotSelectionManager;

import net.minecraft.server.MinecraftServer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-{@link MinecraftServer} bag of transient network-tier services.
 *
 * <p>
 * Rate limiter and subscription tracker are runtime-only: they exist for the server's lifetime, are never persisted,
 * and reset on server stop. Keyed by {@code MinecraftServer} instance rather than a single static singleton so that
 * integrated client/server, dedicated server, and gametest server contexts each get their own isolated state.
 *
 * <p>
 * Removed on {@code ServerStoppedEvent} (see {@link com.akikazu.colony.neoforge.ColonyMod}) to release entries when the
 * integrated server shuts down between single-player sessions.
 */
public final class ColonyServerSession
{
    private static final Map<MinecraftServer, ColonyServerSession> INSTANCES = new ConcurrentHashMap<>();

    private final RegistrationRateLimiter rateLimiter;

    private final ColonyToolCycleRateLimiter toolCycleLimiter;

    private final ColonySubscriptionService subscriptions;

    private final PendingPlacementManager pendingPlacements;

    private final SlotSelectionManager slotSelections;

    private ColonyServerSession()
    {
        this.rateLimiter = RegistrationRateLimiter.defaultLimiter();
        this.toolCycleLimiter = new ColonyToolCycleRateLimiter();
        this.subscriptions = new ColonySubscriptionService();
        this.pendingPlacements = new PendingPlacementManager();
        this.slotSelections = new SlotSelectionManager();
    }

    public static ColonyServerSession get(MinecraftServer server)
    {
        return INSTANCES.computeIfAbsent(server, ignored -> new ColonyServerSession());
    }

    public static void release(MinecraftServer server)
    {
        INSTANCES.remove(server);
    }

    public RegistrationRateLimiter rateLimiter()
    {
        return rateLimiter;
    }

    public ColonyToolCycleRateLimiter toolCycleLimiter()
    {
        return toolCycleLimiter;
    }

    public ColonySubscriptionService subscriptions()
    {
        return subscriptions;
    }

    public PendingPlacementManager pendingPlacements()
    {
        return pendingPlacements;
    }

    public SlotSelectionManager slotSelections()
    {
        return slotSelections;
    }
}

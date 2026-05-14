package com.akikazu.colony.neoforge.network;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player tick-based throttle for {@link CycleColonyToolModePayload}.
 *
 * <p>
 * The HUD lets the player spam shift+scroll; without a guard the client could flood the wire with mode changes. A
 * 2-tick window matches the cadence the HUD's animation can keep up with and stays well above mouse input rate, so
 * legitimate cycles always succeed.
 */
public final class ColonyToolCycleRateLimiter
{
    public static final long COOLDOWN_TICKS = 2L;

    private final long cooldownTicks;

    private final Map<UUID, Long> lastAttempt = new ConcurrentHashMap<>();

    public ColonyToolCycleRateLimiter()
    {
        this(COOLDOWN_TICKS);
    }

    public ColonyToolCycleRateLimiter(long cooldownTicks)
    {
        this.cooldownTicks = cooldownTicks;
    }

    public boolean tryAcquire(UUID player, long currentTick)
    {
        Long previous = lastAttempt.get(player);

        if (previous != null && (currentTick - previous) < cooldownTicks)
        {
            return false;
        }

        lastAttempt.put(player, currentTick);

        return true;
    }

    public void forget(UUID player)
    {
        lastAttempt.remove(player);
    }
}

package com.akikazu.colony.common.colony.registration;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Per-player cooldown gate for {@code RegisterColonyPayload} attempts.
 *
 * <p>
 * Anti-cheat baseline (see {@code docs/07-NETWORKING.md}): refuses more than one attempt per cooldown window. The clock
 * is injected so JUnit can drive time deterministically without sleeping.
 */
public final class RegistrationRateLimiter
{
    public static final Duration DEFAULT_COOLDOWN = Duration.ofSeconds(30);

    private final long cooldownMillis;

    private final LongSupplier clockMillis;

    private final Map<UUID, Long> lastAttempt;

    public RegistrationRateLimiter(Duration cooldown, LongSupplier clockMillis)
    {
        this.cooldownMillis = cooldown.toMillis();
        this.clockMillis = clockMillis;
        this.lastAttempt = new ConcurrentHashMap<>();
    }

    public static RegistrationRateLimiter defaultLimiter()
    {
        return new RegistrationRateLimiter(DEFAULT_COOLDOWN, System::currentTimeMillis);
    }

    public boolean tryAcquire(UUID player)
    {
        long now = clockMillis.getAsLong();
        Long previous = lastAttempt.get(player);

        if (previous != null && (now - previous) < cooldownMillis)
        {
            return false;
        }

        lastAttempt.put(player, now);

        return true;
    }

    public void forget(UUID player)
    {
        lastAttempt.remove(player);
    }
}

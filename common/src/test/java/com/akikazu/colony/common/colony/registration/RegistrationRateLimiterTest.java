package com.akikazu.colony.common.colony.registration;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistrationRateLimiterTest
{
    @Test
    void firstAttemptIsAccepted()
    {
        AtomicLong clock = new AtomicLong(0L);
        RegistrationRateLimiter limiter = new RegistrationRateLimiter(Duration.ofSeconds(30), clock::get);
        UUID player = UUID.randomUUID();

        assertTrue(limiter.tryAcquire(player));
    }

    @Test
    void secondAttemptWithinWindowIsRejected()
    {
        AtomicLong clock = new AtomicLong(0L);
        RegistrationRateLimiter limiter = new RegistrationRateLimiter(Duration.ofSeconds(30), clock::get);
        UUID player = UUID.randomUUID();

        assertTrue(limiter.tryAcquire(player));

        clock.set(Duration.ofSeconds(10).toMillis());

        assertFalse(limiter.tryAcquire(player));
    }

    @Test
    void attemptAtExactlyCooldownBoundaryIsAccepted()
    {
        AtomicLong clock = new AtomicLong(0L);
        RegistrationRateLimiter limiter = new RegistrationRateLimiter(Duration.ofSeconds(30), clock::get);
        UUID player = UUID.randomUUID();

        assertTrue(limiter.tryAcquire(player));

        clock.set(Duration.ofSeconds(30).toMillis());

        assertTrue(limiter.tryAcquire(player));
    }

    @Test
    void separatePlayersDoNotShareCooldown()
    {
        AtomicLong clock = new AtomicLong(0L);
        RegistrationRateLimiter limiter = new RegistrationRateLimiter(Duration.ofSeconds(30), clock::get);
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();

        assertTrue(limiter.tryAcquire(alice));
        assertTrue(limiter.tryAcquire(bob));

        clock.set(Duration.ofSeconds(5).toMillis());

        assertFalse(limiter.tryAcquire(alice));
        assertFalse(limiter.tryAcquire(bob));
    }

    @Test
    void rejectedAttemptDoesNotResetCooldown()
    {
        AtomicLong clock = new AtomicLong(0L);
        RegistrationRateLimiter limiter = new RegistrationRateLimiter(Duration.ofSeconds(30), clock::get);
        UUID player = UUID.randomUUID();

        assertTrue(limiter.tryAcquire(player));

        clock.set(Duration.ofSeconds(5).toMillis());
        assertFalse(limiter.tryAcquire(player));

        clock.set(Duration.ofSeconds(29).toMillis());
        assertFalse(limiter.tryAcquire(player));

        clock.set(Duration.ofSeconds(30).toMillis());
        assertTrue(limiter.tryAcquire(player));
    }

    @Test
    void forgetClearsCooldown()
    {
        AtomicLong clock = new AtomicLong(0L);
        RegistrationRateLimiter limiter = new RegistrationRateLimiter(Duration.ofSeconds(30), clock::get);
        UUID player = UUID.randomUUID();

        assertTrue(limiter.tryAcquire(player));

        limiter.forget(player);

        assertTrue(limiter.tryAcquire(player));
    }
}

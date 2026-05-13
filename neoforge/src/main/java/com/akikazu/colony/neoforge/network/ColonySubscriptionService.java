package com.akikazu.colony.neoforge.network;

import com.akikazu.colony.api.colony.ColonyId;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks per-player subscriptions to colony delta streams.
 *
 * <p>
 * Bidirectional indexing: {@code byPlayer} answers "what does this player listen to?" for fast cleanup on disconnect,
 * {@code byColony} answers "who should receive a delta for this colony?" for fast fan-out on state changes. Reads are
 * unmodifiable; mutations route through {@link #subscribe}, {@link #unsubscribe}, and {@link #forgetPlayer}.
 *
 * <p>
 * Day 13 wires the subscription tracking only. Once delta payloads land, the dispatcher will call
 * {@link #subscribersOf(ColonyId)} to compute the target audience for each delta.
 */
public final class ColonySubscriptionService
{
    private final Map<UUID, Set<ColonyId>> byPlayer;

    private final Map<ColonyId, Set<UUID>> byColony;

    public ColonySubscriptionService()
    {
        this.byPlayer = new ConcurrentHashMap<>();
        this.byColony = new ConcurrentHashMap<>();
    }

    public void subscribe(UUID player, ColonyId colony)
    {
        byPlayer.computeIfAbsent(player, ignored -> ConcurrentHashMap.newKeySet()).add(colony);
        byColony.computeIfAbsent(colony, ignored -> ConcurrentHashMap.newKeySet()).add(player);
    }

    public void unsubscribe(UUID player, ColonyId colony)
    {
        Set<ColonyId> colonies = byPlayer.get(player);

        if (colonies != null)
        {
            colonies.remove(colony);

            if (colonies.isEmpty())
            {
                byPlayer.remove(player);
            }
        }

        Set<UUID> players = byColony.get(colony);

        if (players != null)
        {
            players.remove(player);

            if (players.isEmpty())
            {
                byColony.remove(colony);
            }
        }
    }

    public void forgetPlayer(UUID player)
    {
        Set<ColonyId> colonies = byPlayer.remove(player);

        if (colonies == null)
        {
            return;
        }

        for (ColonyId colony : colonies)
        {
            Set<UUID> players = byColony.get(colony);

            if (players == null)
            {
                continue;
            }

            players.remove(player);

            if (players.isEmpty())
            {
                byColony.remove(colony);
            }
        }
    }

    public boolean isSubscribed(UUID player, ColonyId colony)
    {
        Set<ColonyId> colonies = byPlayer.get(player);

        return colonies != null && colonies.contains(colony);
    }

    public Set<UUID> subscribersOf(ColonyId colony)
    {
        Set<UUID> players = byColony.get(colony);

        if (players == null)
        {
            return Collections.emptySet();
        }

        return Collections.unmodifiableSet(new HashSet<>(players));
    }

    public Set<ColonyId> coloniesFor(UUID player)
    {
        Set<ColonyId> colonies = byPlayer.get(player);

        if (colonies == null)
        {
            return Collections.emptySet();
        }

        return Collections.unmodifiableSet(new HashSet<>(colonies));
    }
}

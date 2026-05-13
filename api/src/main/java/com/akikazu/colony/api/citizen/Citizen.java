package com.akikazu.colony.api.citizen;

import com.akikazu.colony.api.colony.ColonyId;

import org.jetbrains.annotations.ApiStatus;

import java.util.Optional;

/**
 * Public, read-only contract for a citizen observed at runtime. Implementations live in {@code :common}.
 *
 * <p>
 * The interface intentionally exposes no setters: mutation flows through dedicated service interfaces added in later
 * milestones. Addons compiled against {@code :api} should treat instances as snapshots of state at call time.
 */
@ApiStatus.NonExtendable
public interface Citizen
{
    CitizenId id();

    String displayName();

    /**
     * Colony affiliation, may be empty if the citizen is not yet assigned (e.g. spawned but the colony record isn't
     * ready).
     */
    Optional<ColonyId> colony();
}

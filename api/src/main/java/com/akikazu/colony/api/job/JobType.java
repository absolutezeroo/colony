package com.akikazu.colony.api.job;

import com.akikazu.colony.core.registry.Identifier;
import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.ApiStatus;

/**
 * Marker type for a registered job. Implementations are produced by content registrations and dispatched via
 * {@link com.akikazu.colony.api.registry.ColonyRegistries#JOB_TYPE}.
 *
 * <p>
 * Each {@code JobType} carries the {@link MapCodec} used to encode and decode {@link Job} instances of its variant. The
 * codec dispatch driven by {@code JobType} is the entry point for data-driven job content.
 */
@ApiStatus.NonExtendable
public interface JobType
{
    Identifier id();

    MapCodec<? extends Job> codec();
}

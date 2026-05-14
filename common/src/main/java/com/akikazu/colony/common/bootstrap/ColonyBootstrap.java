package com.akikazu.colony.common.bootstrap;

import com.akikazu.colony.api.building.hut.HutType;
import com.akikazu.colony.api.job.JobType;
import com.akikazu.colony.api.registry.ColonyRegistries;
import com.akikazu.colony.common.building.impl.ResidenceHutType;
import com.akikazu.colony.common.job.impl.IdleJobType;
import com.akikazu.colony.core.registry.Registry;
import com.akikazu.colony.core.registry.RegistryView;
import com.akikazu.colony.core.registry.SimpleRegistry;

/**
 * Static bootstrap for built-in colony content. Invoked once at mod construction by the loader entry point, this
 * populates the in-process {@link Registry registries} with every type shipped in the base jar and then freezes them.
 *
 * <p>
 * Idempotent: a second call after {@link #register()} is a no-op so reloads in dev environments do not double-register.
 */
public final class ColonyBootstrap
{
    private static final Registry<JobType> JOB_TYPES = new SimpleRegistry<>(ColonyRegistries.JOB_TYPE);

    private static final Registry<HutType> HUT_TYPES = new SimpleRegistry<>(ColonyRegistries.HUT_TYPE);

    private static volatile boolean registered;

    private ColonyBootstrap()
    {
    }

    public static synchronized void register()
    {
        if (registered)
        {
            return;
        }

        JOB_TYPES.register(IdleJobType.ID, IdleJobType.INSTANCE);
        JOB_TYPES.freeze();

        HUT_TYPES.register(ResidenceHutType.ID, ResidenceHutType.INSTANCE);
        HUT_TYPES.freeze();

        registered = true;
    }

    public static Registry<JobType> jobTypes()
    {
        return JOB_TYPES;
    }

    public static RegistryView<JobType> jobTypesView()
    {
        return JOB_TYPES;
    }

    public static Registry<HutType> hutTypes()
    {
        return HUT_TYPES;
    }

    public static RegistryView<HutType> hutTypesView()
    {
        return HUT_TYPES;
    }
}

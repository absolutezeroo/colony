package com.akikazu.colony.common.job.impl;

import com.akikazu.colony.api.job.Job;
import com.akikazu.colony.api.job.JobType;
import com.akikazu.colony.core.registry.Identifier;
import com.mojang.serialization.MapCodec;

/**
 * The built-in idle {@link JobType}, registered under {@code colony:idle}. Its codec is a {@link MapCodec#unit unit}
 * codec because {@link IdleJob} carries no state.
 */
public final class IdleJobType implements JobType
{
    public static final Identifier ID = Identifier.of("colony", "idle");

    public static final IdleJobType INSTANCE = new IdleJobType();

    private static final MapCodec<IdleJob> CODEC = MapCodec.unit(IdleJob.INSTANCE);

    private IdleJobType()
    {
    }

    @Override
    public Identifier id()
    {
        return ID;
    }

    @Override
    public MapCodec<? extends Job> codec()
    {
        return CODEC;
    }
}

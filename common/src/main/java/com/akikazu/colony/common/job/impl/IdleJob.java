package com.akikazu.colony.common.job.impl;

import com.akikazu.colony.api.job.Job;
import com.akikazu.colony.api.job.JobType;

/**
 * The default no-op job assigned to citizens who have no work. Carries no state beyond its {@link JobType}; encoding
 * yields {@code { "type": "colony:idle" }} and nothing else.
 */
public record IdleJob() implements Job
{
    public static final IdleJob INSTANCE = new IdleJob();

    @Override
    public JobType type()
    {
        return IdleJobType.INSTANCE;
    }
}

package com.akikazu.colony.api.job;

/**
 * A concrete job instance loaded from data. Each {@code Job} carries a back-reference to the {@link JobType} that
 * produced it; the type's {@link JobType#codec()} is used by the dispatch codec to round-trip this instance.
 */
public interface Job
{
    JobType type();
}

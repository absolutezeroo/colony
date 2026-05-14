package com.akikazu.colony.api.registry;

import com.akikazu.colony.api.building.functional.FunctionalBlockDetector;
import com.akikazu.colony.api.building.hut.HutType;
import com.akikazu.colony.api.building.room.RoomType;
import com.akikazu.colony.api.job.Job;
import com.akikazu.colony.api.job.JobType;
import com.akikazu.colony.api.storage.StorageRoleType;
import com.akikazu.colony.api.workzone.WorkZoneAnchorType;
import com.akikazu.colony.core.registry.Identifier;
import com.akikazu.colony.core.registry.Registry;
import com.akikazu.colony.core.registry.RegistryKey;
import com.mojang.serialization.Codec;

/**
 * Typed registry keys for every content family Colony exposes. Adding a new family means adding a constant here; no
 * switch on string identifiers anywhere else in the codebase.
 */
public final class ColonyRegistries
{
    public static final RegistryKey<JobType> JOB_TYPE = RegistryKey.of(Identifier.of("colony", "job_type"));

    public static final RegistryKey<HutType> HUT_TYPE = RegistryKey.of(Identifier.of("colony", "hut_type"));

    public static final RegistryKey<RoomType> ROOM_TYPE = RegistryKey.of(Identifier.of("colony", "room_type"));

    public static final RegistryKey<WorkZoneAnchorType> ANCHOR_TYPE = RegistryKey
            .of(Identifier.of("colony", "anchor_type"));

    public static final RegistryKey<StorageRoleType> STORAGE_ROLE_TYPE = RegistryKey
            .of(Identifier.of("colony", "storage_role_type"));

    public static final RegistryKey<FunctionalBlockDetector> FUNCTIONAL_BLOCK_DETECTOR = RegistryKey
            .of(Identifier.of("colony", "functional_block_detector"));

    private ColonyRegistries()
    {
    }

    /**
     * Builds the polymorphic {@link Codec} that decodes a {@link Job} by reading its {@code "type"} field, looking up
     * the matching {@link JobType} in the supplied registry, and dispatching to the type's {@link JobType#codec()
     * codec}.
     */
    public static Codec<Job> jobDispatchCodec(Registry<JobType> registry)
    {
        return registry.byNameCodec().dispatch(
                "type",
                Job::type,
                JobType::codec);
    }
}

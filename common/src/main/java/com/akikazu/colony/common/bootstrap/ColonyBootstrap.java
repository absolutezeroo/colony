package com.akikazu.colony.common.bootstrap;

import com.akikazu.colony.api.building.hut.HutType;
import com.akikazu.colony.api.building.room.RoomType;
import com.akikazu.colony.api.job.JobType;
import com.akikazu.colony.api.registry.ColonyRegistries;
import com.akikazu.colony.api.storage.StorageRoleType;
import com.akikazu.colony.common.building.functional.FunctionalBlockDetectorRegistry;
import com.akikazu.colony.common.building.functional.TaggedBlockDetector;
import com.akikazu.colony.common.building.impl.ResidenceHutType;
import com.akikazu.colony.common.building.room.BedroomType;
import com.akikazu.colony.common.job.impl.IdleJobType;
import com.akikazu.colony.common.storage.role.GeneralRoleType;
import com.akikazu.colony.common.storage.role.InputRoleType;
import com.akikazu.colony.common.storage.role.MaterialsRoleType;
import com.akikazu.colony.common.storage.role.OutputRoleType;
import com.akikazu.colony.common.storage.role.ToolsRoleType;
import com.akikazu.colony.core.registry.Identifier;
import com.akikazu.colony.core.registry.Registry;
import com.akikazu.colony.core.registry.RegistryView;
import com.akikazu.colony.core.registry.SimpleRegistry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * Static bootstrap for built-in colony content. Invoked once at mod construction by the loader entry point, this
 * populates the in-process {@link Registry registries} with every type shipped in the base jar and then freezes them.
 *
 * <p>
 * Idempotent: a second call after {@link #register()} is a no-op so reloads in dev environments do not double-register.
 */
public final class ColonyBootstrap
{
    public static final TagKey<Block> COLONY_WINDOW_TAG = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("colony", "window"));

    private static final Registry<JobType> JOB_TYPES = new SimpleRegistry<>(ColonyRegistries.JOB_TYPE);

    private static final Registry<HutType> HUT_TYPES = new SimpleRegistry<>(ColonyRegistries.HUT_TYPE);

    private static final Registry<RoomType> ROOM_TYPES = new SimpleRegistry<>(ColonyRegistries.ROOM_TYPE);

    private static final Registry<StorageRoleType> STORAGE_ROLE_TYPES = new SimpleRegistry<>(
            ColonyRegistries.STORAGE_ROLE_TYPE);

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

        ROOM_TYPES.register(BedroomType.ID, BedroomType.INSTANCE);
        ROOM_TYPES.freeze();

        registerStorageRoles();

        registerBuiltinDetectors();

        registered = true;
    }

    private static void registerStorageRoles()
    {
        STORAGE_ROLE_TYPES.register(InputRoleType.ID, InputRoleType.INSTANCE);
        STORAGE_ROLE_TYPES.register(OutputRoleType.ID, OutputRoleType.INSTANCE);
        STORAGE_ROLE_TYPES.register(ToolsRoleType.ID, ToolsRoleType.INSTANCE);
        STORAGE_ROLE_TYPES.register(MaterialsRoleType.ID, MaterialsRoleType.INSTANCE);
        STORAGE_ROLE_TYPES.register(GeneralRoleType.ID, GeneralRoleType.INSTANCE);
        STORAGE_ROLE_TYPES.freeze();
    }

    private static void registerBuiltinDetectors()
    {
        FunctionalBlockDetectorRegistry detectors = FunctionalBlockDetectorRegistry.get();
        detectors.registerType(TaggedBlockDetector.TYPE_ID, TaggedBlockDetector.CODEC);

        detectors.registerBuiltin(TaggedBlockDetector.ofBlockTag(
                Identifier.of("colony", "detector/beds"),
                Identifier.of("colony", "bed"),
                BlockTags.BEDS));

        detectors.registerBuiltin(TaggedBlockDetector.ofBlockTag(
                Identifier.of("colony", "detector/doors"),
                Identifier.of("colony", "door"),
                BlockTags.DOORS));

        detectors.registerBuiltin(TaggedBlockDetector.ofBlockTag(
                Identifier.of("colony", "detector/windows"),
                Identifier.of("colony", "window"),
                COLONY_WINDOW_TAG));
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

    public static Registry<RoomType> roomTypes()
    {
        return ROOM_TYPES;
    }

    public static RegistryView<RoomType> roomTypesView()
    {
        return ROOM_TYPES;
    }

    public static Registry<StorageRoleType> storageRoles()
    {
        return STORAGE_ROLE_TYPES;
    }

    public static RegistryView<StorageRoleType> storageRolesView()
    {
        return STORAGE_ROLE_TYPES;
    }
}

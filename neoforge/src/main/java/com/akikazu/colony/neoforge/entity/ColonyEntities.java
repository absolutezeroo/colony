package com.akikazu.colony.neoforge.entity;

import com.akikazu.colony.common.citizen.entity.EntityCitizen;
import com.akikazu.colony.neoforge.ColonyMod;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registry holder for Colony's custom entity types. Owned by {@code :neoforge} because the loader-specific
 * {@link DeferredRegister} machinery cannot live in {@code :common}.
 */
public final class ColonyEntities
{
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister
            .create(BuiltInRegistries.ENTITY_TYPE, ColonyMod.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<EntityCitizen>> CITIZEN = ENTITIES.register(
            "citizen",
            () -> EntityType.Builder.of(EntityCitizen::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .build("citizen"));

    private ColonyEntities()
    {
    }

    public static void register(IEventBus modEventBus)
    {
        ENTITIES.register(modEventBus);
    }
}

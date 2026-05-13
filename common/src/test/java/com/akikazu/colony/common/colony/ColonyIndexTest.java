package com.akikazu.colony.common.colony;

import com.akikazu.colony.api.colony.ColonyId;

import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColonyIndexTest
{
    private static final RegistryAccess EMPTY_LOOKUP = RegistryAccess.EMPTY;

    @Test
    void persistsAndReloads()
    {
        ColonyIndex original = new ColonyIndex();
        ColonyId id = ColonyId.random();
        ColonyMetadata metadata = new ColonyMetadata(Level.OVERWORLD, new BlockPos(10, 64, -20), 1234L);

        assertTrue(original.register(id, metadata));
        assertEquals(1, original.size());

        CompoundTag saved = original.save(new CompoundTag(), EMPTY_LOOKUP);
        ColonyIndex reloaded = ColonyIndex.load(saved, EMPTY_LOOKUP);

        assertEquals(1, reloaded.size());
        assertTrue(reloaded.contains(id));

        ColonyMetadata retrieved = Objects.requireNonNull(reloaded.entries().get(id));
        assertEquals(Level.OVERWORLD, retrieved.dimension());
        assertEquals(new BlockPos(10, 64, -20), retrieved.townHallPos());
        assertEquals(1234L, retrieved.foundedAtTick());
    }

    @Test
    void registerRejectsDuplicateId()
    {
        ColonyIndex index = new ColonyIndex();
        ColonyId id = ColonyId.random();
        ColonyMetadata metadata = new ColonyMetadata(Level.OVERWORLD, BlockPos.ZERO, 0L);

        assertTrue(index.register(id, metadata));
        assertFalse(index.register(id, metadata));
        assertEquals(1, index.size());
    }

    @Test
    void registerPreservesMultipleColoniesAcrossDimensions()
    {
        ColonyIndex index = new ColonyIndex();
        ColonyId overworldId = ColonyId.random();
        ColonyId netherId = ColonyId.random();
        ResourceKey<Level> custom = ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath("colony", "test_dim"));

        index.register(overworldId, new ColonyMetadata(Level.OVERWORLD, BlockPos.ZERO, 0L));
        index.register(netherId, new ColonyMetadata(custom, new BlockPos(1, 2, 3), 42L));

        CompoundTag tag = index.save(new CompoundTag(), EMPTY_LOOKUP);
        ColonyIndex reloaded = ColonyIndex.load(tag, EMPTY_LOOKUP);

        assertEquals(2, reloaded.size());

        ColonyMetadata first = Objects.requireNonNull(reloaded.entries().get(overworldId));
        ColonyMetadata second = Objects.requireNonNull(reloaded.entries().get(netherId));
        assertEquals(Level.OVERWORLD, first.dimension());
        assertEquals(custom, second.dimension());
        assertEquals(42L, second.foundedAtTick());
    }
}

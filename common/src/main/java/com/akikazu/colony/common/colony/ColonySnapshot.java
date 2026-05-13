package com.akikazu.colony.common.colony;

import com.akikazu.colony.api.citizen.CitizenId;
import com.akikazu.colony.api.colony.Colony;
import com.akikazu.colony.api.colony.ColonyId;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Objects;

/**
 * Persisted form of a {@link Colony}. Encoded via {@link #CODEC} into the per-colony NBT file. The {@code dataVersion}
 * field gates the migration chain described in {@code docs/18-SAVE-VERSIONING.md}; bump {@link #CURRENT_VERSION} and
 * add a migration step in {@code :common/persistence/migration/steps/} when the on-disk shape changes.
 */
public record ColonySnapshot(
        int dataVersion,
        ColonyId id,
        String name,
        BlockPos townHallPos,
        ResourceKey<Level> dimension,
        long foundedAtTick,
        List<CitizenId> citizens)
{

    public static final int CURRENT_VERSION = 1;

    public static final Codec<ColonySnapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("dataVersion").forGetter(ColonySnapshot::dataVersion),
            ColonyId.CODEC.fieldOf("id").forGetter(ColonySnapshot::id),
            Codec.STRING.fieldOf("name").forGetter(ColonySnapshot::name),
            BlockPos.CODEC.fieldOf("townHallPos").forGetter(ColonySnapshot::townHallPos),
            ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(ColonySnapshot::dimension),
            Codec.LONG.fieldOf("foundedAtTick").forGetter(ColonySnapshot::foundedAtTick),
            CitizenId.CODEC.listOf().fieldOf("citizens").forGetter(ColonySnapshot::citizens))
            .apply(instance, ColonySnapshot::new));

    public ColonySnapshot
    {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(townHallPos, "townHallPos");
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(citizens, "citizens");
        citizens = List.copyOf(citizens);
    }

    public static ColonySnapshot of(
            ColonyId id,
            String name,
            BlockPos townHallPos,
            ResourceKey<Level> dimension,
            long foundedAtTick,
            List<CitizenId> citizens)
    {
        return new ColonySnapshot(CURRENT_VERSION, id, name, townHallPos, dimension, foundedAtTick, citizens);
    }

    public static ColonySnapshot empty(
            ColonyId id,
            String name,
            BlockPos townHallPos,
            ResourceKey<Level> dimension,
            long foundedAtTick)
    {
        return of(id, name, townHallPos, dimension, foundedAtTick, List.of());
    }
}

package com.akikazu.colony.common.colony;

import com.akikazu.colony.api.colony.Colony;
import com.akikazu.colony.api.colony.ColonyId;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;

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
        BlockPos townHallPos)
{

    public static final int CURRENT_VERSION = 1;

    public static final Codec<ColonySnapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("dataVersion").forGetter(ColonySnapshot::dataVersion),
            ColonyId.CODEC.fieldOf("id").forGetter(ColonySnapshot::id),
            Codec.STRING.fieldOf("name").forGetter(ColonySnapshot::name),
            BlockPos.CODEC.fieldOf("townHallPos").forGetter(ColonySnapshot::townHallPos))
            .apply(instance, ColonySnapshot::new));

    public ColonySnapshot
    {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(townHallPos, "townHallPos");
    }

    public static ColonySnapshot of(ColonyId id, String name, BlockPos townHallPos)
    {
        return new ColonySnapshot(CURRENT_VERSION, id, name, townHallPos);
    }
}

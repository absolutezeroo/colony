package com.akikazu.colony.neoforge.network;

import com.akikazu.colony.api.colony.ColonyId;
import com.akikazu.colony.common.colony.ColonyIndex;
import com.akikazu.colony.common.colony.ColonyMetadata;
import com.akikazu.colony.common.colony.ColonySnapshot;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Shared registration logic invoked by both the {@link RegisterColonyPayload} handler and the {@code /colony register}
 * server command. Keeping the wire path and the command path on the same call avoids drift between the two.
 */
public final class ColonyRegistrationService
{
    private ColonyRegistrationService()
    {
    }

    public static Result register(ServerLevel level, ColonyId id, String name, BlockPos townHallPos)
    {
        ColonyIndex index = ColonyIndex.get(level);

        ResourceKey<Level> dimension = level.dimension();
        long foundedAtTick = level.getGameTime();
        ColonyMetadata metadata = new ColonyMetadata(dimension, townHallPos, foundedAtTick);
        ColonySnapshot snapshot = ColonySnapshot.empty(id, name, townHallPos, dimension, foundedAtTick);

        boolean accepted = index.register(snapshot.id(), metadata);

        return new Result(snapshot, accepted);
    }

    public record Result(ColonySnapshot snapshot, boolean accepted)
    {
    }
}

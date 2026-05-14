package com.akikazu.colony.common.building.validation;

import com.akikazu.colony.api.building.AxisAlignedOuterZone;
import com.akikazu.colony.api.colony.ColonyId;
import com.akikazu.colony.common.building.BuildingIndex;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Server-side validator for proposed outer zones produced by the two-click painting workflow.
 *
 * <p>
 * The validator collects every applicable error rather than short-circuiting on the first — the HUD already
 * approximates locally and may have missed a check (overlap, chunk loading); surfacing the full list lets the chat
 * handler pick the most useful one to show. Volume bounds are intentionally generous so V1 player creativity is not
 * pinched; tighter per-hut bounds land with {@link com.akikazu.colony.api.building.hut.HutType} extensions later.
 */
public final class ZoneValidator
{
    public static final int MIN_VOLUME = 16;

    public static final int MAX_VOLUME = 4096;

    private ZoneValidator()
    {
    }

    public static ZoneValidationResult validate(
            ServerLevel level,
            AxisAlignedOuterZone proposedZone,
            BlockPos hutPos,
            ColonyId colony)
    {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(proposedZone, "proposedZone");
        Objects.requireNonNull(hutPos, "hutPos");
        Objects.requireNonNull(colony, "colony");

        List<ZoneValidationError> errors = new ArrayList<>();

        validateVolume(proposedZone, errors);
        validateContainsHutPos(proposedZone, hutPos, errors);
        validateNoOverlap(level, proposedZone, errors);
        validateLoadedChunks(level, proposedZone, errors);

        if (errors.isEmpty())
        {
            return new ZoneValidationResult.Valid(proposedZone);
        }

        return new ZoneValidationResult.Invalid(errors);
    }

    static void validateVolume(AxisAlignedOuterZone zone, List<ZoneValidationError> errors)
    {
        int volume = zone.volume();

        if (volume < MIN_VOLUME)
        {
            errors.add(new ZoneValidationError.TooSmall(volume, MIN_VOLUME));

            return;
        }

        if (volume > MAX_VOLUME)
        {
            errors.add(new ZoneValidationError.TooLarge(volume, MAX_VOLUME));
        }
    }

    static void validateContainsHutPos(AxisAlignedOuterZone zone, BlockPos hutPos, List<ZoneValidationError> errors)
    {
        if (!zone.contains(hutPos))
        {
            errors.add(new ZoneValidationError.DoesNotContainHutPos(hutPos));
        }
    }

    private static void validateNoOverlap(
            ServerLevel level,
            AxisAlignedOuterZone zone,
            List<ZoneValidationError> errors)
    {
        BuildingIndex index = BuildingIndex.get(level);
        index.findOverlapping(zone)
                .ifPresent(conflict -> errors.add(new ZoneValidationError.OverlapsExistingBuilding(conflict)));
    }

    private static void validateLoadedChunks(
            ServerLevel level,
            AxisAlignedOuterZone zone,
            List<ZoneValidationError> errors)
    {
        int minChunkX = zone.min().getX() >> 4;
        int minChunkZ = zone.min().getZ() >> 4;
        int maxChunkX = zone.max().getX() >> 4;
        int maxChunkZ = zone.max().getZ() >> 4;

        for (int cx = minChunkX; cx <= maxChunkX; cx++)
        {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++)
            {
                LevelChunk chunk = level.getChunkSource().getChunk(cx, cz, false);

                if (chunk == null)
                {
                    errors.add(new ZoneValidationError.OutsideLoadedChunks());

                    return;
                }
            }
        }
    }
}

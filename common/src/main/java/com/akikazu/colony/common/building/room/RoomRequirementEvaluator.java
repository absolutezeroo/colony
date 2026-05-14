package com.akikazu.colony.common.building.room;

import com.akikazu.colony.api.building.functional.FunctionalBlock;
import com.akikazu.colony.api.building.functional.FunctionalBlockDetector;
import com.akikazu.colony.api.building.room.FreeformZone;
import com.akikazu.colony.api.building.room.RoomRequirement;
import com.akikazu.colony.api.building.room.RoomRequirement.FunctionalBlockCountRequirement;
import com.akikazu.colony.api.building.room.RoomStatus;
import com.akikazu.colony.api.building.room.RoomStatus.RequirementError;
import com.akikazu.colony.common.building.functional.FunctionalBlockDetectorRegistry;
import com.akikazu.colony.core.registry.Identifier;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Evaluates a {@link com.akikazu.colony.api.building.room.RoomType RoomType}'s {@link RoomRequirement requirements}
 * against the live world.
 *
 * <p>
 * Two-stage design: first scan the room's {@link FreeformZone} once to produce a flat {@code List<FunctionalBlock>} via
 * every registered {@link FunctionalBlockDetector}; then evaluate each requirement against that scan result. Stage 1 is
 * the only stage that touches the level — stage 2 is pure and unit-tested with synthetic block lists.
 *
 * <p>
 * V1 only wires {@link FunctionalBlockCountRequirement}; the other variants are recognized and skipped (treated as
 * always-passing for now). They will gain dedicated evaluation passes in later prompts.
 */
public final class RoomRequirementEvaluator
{
    private RoomRequirementEvaluator()
    {
    }

    public static RequirementEvaluation evaluate(
            ServerLevel level,
            FreeformZone zone,
            List<RoomRequirement> requirements)
    {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(zone, "zone");
        Objects.requireNonNull(requirements, "requirements");

        FunctionalBlockDetectorRegistry registry = FunctionalBlockDetectorRegistry.get();
        Collection<FunctionalBlockDetector> detectors = registry.all().values();

        List<FunctionalBlock> scanned = scanZone(level, zone, detectors);

        return evaluateAgainst(scanned, requirements);
    }

    public static List<FunctionalBlock> scanZone(
            ServerLevel level,
            FreeformZone zone,
            Collection<FunctionalBlockDetector> detectors)
    {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(zone, "zone");
        Objects.requireNonNull(detectors, "detectors");

        return scanZone(level::getBlockState, level, zone, detectors);
    }

    static List<FunctionalBlock> scanZone(
            Function<BlockPos, BlockState> blockGetter,
            ServerLevel level,
            FreeformZone zone,
            Collection<FunctionalBlockDetector> detectors)
    {
        List<FunctionalBlock> hits = new ArrayList<>();

        for (BlockPos pos : zone.blocksInZone().toList())
        {
            BlockState state = blockGetter.apply(pos);

            if (state.isAir())
            {
                continue;
            }

            BlockInWorld biw = new BlockInWorld(level, pos, false);

            for (FunctionalBlockDetector detector : detectors)
            {
                if (detector.matches(biw))
                {
                    hits.add(new FunctionalBlock(detector.detects(), pos, state));
                }
            }
        }

        return hits;
    }

    public static RequirementEvaluation evaluateAgainst(
            List<FunctionalBlock> scannedBlocks,
            List<RoomRequirement> requirements)
    {
        Objects.requireNonNull(scannedBlocks, "scannedBlocks");

        return evaluateForCounts(countByFunction(scannedBlocks), requirements);
    }

    public static RequirementEvaluation evaluateForCounts(
            Map<Identifier, Integer> functionCounts,
            List<RoomRequirement> requirements)
    {
        Objects.requireNonNull(functionCounts, "functionCounts");
        Objects.requireNonNull(requirements, "requirements");

        Map<RoomRequirement, RequirementResult> results = new LinkedHashMap<>();

        for (RoomRequirement requirement : requirements)
        {
            results.put(requirement, evaluateOne(requirement, functionCounts));
        }

        return new RequirementEvaluation(Map.copyOf(results));
    }

    public static RoomStatus toStatus(RequirementEvaluation evaluation)
    {
        Objects.requireNonNull(evaluation, "evaluation");

        List<RequirementError> failures = new ArrayList<>();

        for (Map.Entry<RoomRequirement, RequirementResult> entry : evaluation.results().entrySet())
        {
            if (entry.getValue() instanceof RequirementResult.Failed failed)
            {
                failures.add(new RequirementError(entry.getKey(), failed.reason()));
            }
        }

        if (failures.isEmpty())
        {
            return RoomStatus.valid();
        }

        return RoomStatus.invalid(failures);
    }

    private static RequirementResult evaluateOne(RoomRequirement requirement, Map<Identifier, Integer> counts)
    {
        if (requirement instanceof FunctionalBlockCountRequirement count)
        {
            int observed = counts.getOrDefault(count.function(), 0);

            if (observed < count.min())
            {
                return RequirementResult.failed(
                        "%s count %d below min %d".formatted(count.function(), observed, count.min()));
            }

            if (observed > count.max())
            {
                return RequirementResult.failed(
                        "%s count %d above max %d".formatted(count.function(), observed, count.max()));
            }

            return RequirementResult.PASSED;
        }

        return RequirementResult.PASSED;
    }

    private static Map<Identifier, Integer> countByFunction(List<FunctionalBlock> blocks)
    {
        Map<Identifier, Integer> counts = new LinkedHashMap<>();

        for (FunctionalBlock block : blocks)
        {
            counts.merge(block.function(), 1, Integer::sum);
        }

        return counts;
    }

    public record RequirementEvaluation(Map<RoomRequirement, RequirementResult> results)
    {
        public RequirementEvaluation
        {
            Objects.requireNonNull(results, "results");
            results = Map.copyOf(results);
        }

        public boolean allPassed()
        {
            return results.values().stream().allMatch(RequirementResult::passes);
        }
    }

    public sealed interface RequirementResult
    {
        Passed PASSED = new Passed();

        static RequirementResult passed()
        {
            return PASSED;
        }

        static RequirementResult failed(String reason)
        {
            return new Failed(reason);
        }

        boolean passes();

        record Passed() implements RequirementResult
        {
            @Override
            public boolean passes()
            {
                return true;
            }
        }

        record Failed(String reason) implements RequirementResult
        {
            public Failed
            {
                Objects.requireNonNull(reason, "reason");
            }

            @Override
            public boolean passes()
            {
                return false;
            }
        }
    }
}

package com.akikazu.colony.common.building.room;

import com.akikazu.colony.api.building.room.RoomRequirement;
import com.akikazu.colony.api.building.room.RoomRequirement.FunctionalBlockCountRequirement;
import com.akikazu.colony.api.building.room.RoomStatus;
import com.akikazu.colony.common.building.room.RoomRequirementEvaluator.RequirementEvaluation;
import com.akikazu.colony.common.building.room.RoomRequirementEvaluator.RequirementResult;
import com.akikazu.colony.core.registry.Identifier;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-logic coverage for the requirement-counting half of {@link RoomRequirementEvaluator}. The zone-scanning half
 * lives in the level and is covered by {@code RoomRequirementGameTests} — instantiating real {@code BlockState}s here
 * would pull in {@code Bootstrap.bootStrap()} which fails outside a loaded NeoForge environment.
 */
class RoomRequirementEvaluatorTest
{
    private static final Identifier BED = Identifier.of("colony", "bed");

    @Test
    void countsBedsInZone()
    {
        FunctionalBlockCountRequirement req = new FunctionalBlockCountRequirement(BED, 1, 5);

        RequirementEvaluation evaluation = RoomRequirementEvaluator.evaluateForCounts(
                Map.of(BED, 3),
                List.of(req));

        assertEquals(1, evaluation.results().size());
        assertInstanceOf(RequirementResult.Passed.class, evaluation.results().get(req));
        assertTrue(evaluation.allPassed());
    }

    @Test
    void failsWhenBelowMin()
    {
        FunctionalBlockCountRequirement req = new FunctionalBlockCountRequirement(BED, 2, 4);

        RequirementEvaluation evaluation = RoomRequirementEvaluator.evaluateForCounts(
                Map.of(BED, 1),
                List.of(req));

        RequirementResult result = evaluation.results().get(req);
        RequirementResult.Failed failed = assertInstanceOf(RequirementResult.Failed.class, result);
        assertTrue(failed.reason().contains("below min"));
        assertFalse(evaluation.allPassed());
    }

    @Test
    void failsWhenAboveMax()
    {
        FunctionalBlockCountRequirement req = new FunctionalBlockCountRequirement(BED, 1, 2);

        RequirementEvaluation evaluation = RoomRequirementEvaluator.evaluateForCounts(
                Map.of(BED, 3),
                List.of(req));

        RequirementResult result = evaluation.results().get(req);
        RequirementResult.Failed failed = assertInstanceOf(RequirementResult.Failed.class, result);
        assertTrue(failed.reason().contains("above max"));
    }

    @Test
    void passesAtExactMin()
    {
        FunctionalBlockCountRequirement req = new FunctionalBlockCountRequirement(BED, 1, 2);

        RequirementEvaluation evaluation = RoomRequirementEvaluator.evaluateForCounts(
                Map.of(BED, 1),
                List.of(req));

        assertInstanceOf(RequirementResult.Passed.class, evaluation.results().get(req));
    }

    @Test
    void passesAtExactMax()
    {
        FunctionalBlockCountRequirement req = new FunctionalBlockCountRequirement(BED, 1, 2);

        RequirementEvaluation evaluation = RoomRequirementEvaluator.evaluateForCounts(
                Map.of(BED, 2),
                List.of(req));

        assertInstanceOf(RequirementResult.Passed.class, evaluation.results().get(req));
    }

    @Test
    void evaluationWithoutRequirementsPasses()
    {
        RequirementEvaluation evaluation = RoomRequirementEvaluator.evaluateForCounts(Map.of(), List.of());

        assertTrue(evaluation.allPassed());
        assertEquals(Map.of(), evaluation.results());
    }

    @Test
    void toStatusReturnsValidWhenAllPass()
    {
        FunctionalBlockCountRequirement req = new FunctionalBlockCountRequirement(BED, 1, 2);
        RequirementEvaluation evaluation = RoomRequirementEvaluator.evaluateForCounts(
                Map.of(BED, 1),
                List.of(req));

        assertInstanceOf(RoomStatus.Valid.class, RoomRequirementEvaluator.toStatus(evaluation));
    }

    @Test
    void toStatusReturnsInvalidWithReasonsWhenFailing()
    {
        FunctionalBlockCountRequirement req = new FunctionalBlockCountRequirement(BED, 1, 2);
        RequirementEvaluation evaluation = RoomRequirementEvaluator.evaluateForCounts(
                Map.of(),
                List.of(req));

        RoomStatus.Invalid invalid = assertInstanceOf(
                RoomStatus.Invalid.class,
                RoomRequirementEvaluator.toStatus(evaluation));

        assertEquals(1, invalid.errors().size());
        assertEquals(req, invalid.errors().get(0).requirement());
    }

    @SuppressWarnings("unused")
    private static List<RoomRequirement> reqsOf(RoomRequirement... reqs)
    {
        return List.of(reqs);
    }
}

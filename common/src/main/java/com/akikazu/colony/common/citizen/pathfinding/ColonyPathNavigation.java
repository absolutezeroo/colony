package com.akikazu.colony.common.citizen.pathfinding;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.Target;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * V1 minimum-viable {@link PathNavigation} for citizens.
 *
 * <p>
 * Wraps {@link ColonyNodeEvaluator} + {@link ColonyAStarPathFinder} inside a {@link PathFinder} subclass that ignores
 * vanilla's BinaryHeap-backed search and instead delegates to our generic A* loop. Movement-direct shortcuts are
 * disabled — every move is pathfound — so we can validate the custom pipeline end-to-end before adding optimisations.
 */
public class ColonyPathNavigation extends PathNavigation
{
    private final ColonyNodeEvaluator colonyEvaluator = new ColonyNodeEvaluator();

    private final ColonyAStarPathFinder colonyPathFinder = new ColonyAStarPathFinder();

    public ColonyPathNavigation(Mob mob, Level level)
    {
        super(mob, level);
    }

    @Override
    protected PathFinder createPathFinder(int maxVisitedNodes)
    {
        this.nodeEvaluator = colonyEvaluator;

        return new PathFinder(colonyEvaluator, maxVisitedNodes)
        {
            @Override
            public @Nullable Path findPath(
                    PathNavigationRegion region,
                    Mob pathMob,
                    Set<BlockPos> targets,
                    float maxRange,
                    int accuracy,
                    float searchDepthMultiplier)
            {
                colonyEvaluator.prepare(region, pathMob);

                try
                {
                    Node start = colonyEvaluator.getStart();

                    if (start == null)
                    {
                        return null;
                    }

                    Set<Target> targetSet = new HashSet<>(targets.size());

                    for (BlockPos pos : targets)
                    {
                        targetSet.add(colonyEvaluator.getTarget(pos.getX(), pos.getY(), pos.getZ()));
                    }

                    int visitBudget = (int) ((float) maxVisitedNodes * searchDepthMultiplier);

                    return colonyPathFinder.findPath(
                            colonyEvaluator,
                            start,
                            targetSet,
                            maxRange,
                            visitBudget);
                }
                finally
                {
                    colonyEvaluator.done();
                }
            }
        };
    }

    @Override
    protected boolean canUpdatePath()
    {
        return this.mob.onGround() || this.mob.isInWater();
    }

    @Override
    protected Vec3 getTempMobPos()
    {
        return this.mob.position();
    }

    @Override
    protected boolean canMoveDirectly(Vec3 from, Vec3 to)
    {
        return false;
    }
}

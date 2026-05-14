package com.akikazu.colony.common.citizen.pathfinding;

import com.akikazu.colony.common.citizen.entity.EntityCitizen;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.Target;

import org.jspecify.annotations.Nullable;

/**
 * V1 minimum-viable {@link NodeEvaluator} for citizens.
 *
 * <p>
 * Walks an 8-neighbour grid around the current node. For each neighbour cell the evaluator first tries the same Y, then
 * a single step up (with {@link #CLIMB_PENALTY}), then up to {@link #MAX_FALL_DISTANCE} steps down (with
 * {@link #FALL_COST_PER_BLOCK} per block fallen). Diagonals get a slightly higher base cost. The evaluator does
 * <em>no</em> caching, no claimed-zone logic, no trait-aware climb cost — those are deferred to V2 (see
 * {@code docs/05-CITIZEN-SYSTEM.md}).
 */
public final class ColonyNodeEvaluator extends NodeEvaluator
{
    static final float BASE_WALK_COST = 1.0f;

    static final float DIAGONAL_WALK_COST = 1.4f;

    static final float CLIMB_PENALTY = 1.5f;

    static final float FALL_COST_PER_BLOCK = 0.5f;

    static final int MAX_FALL_DISTANCE = 4;

    private final BlockPos.MutableBlockPos scratch = new BlockPos.MutableBlockPos();

    private @Nullable EntityCitizen owner;

    @Override
    public void prepare(PathNavigationRegion region, Mob mob)
    {
        super.prepare(region, mob);

        this.owner = (mob instanceof EntityCitizen citizen) ? citizen : null;
    }

    @Override
    public void done()
    {
        this.owner = null;

        super.done();
    }

    @Override
    public Node getStart()
    {
        BlockPos pos = (owner != null) ? owner.blockPosition() : mob.blockPosition();

        return getNode(pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public Target getTarget(double x, double y, double z)
    {
        return getTargetNodeAt(x, y, z);
    }

    @Override
    public int getNeighbors(Node[] neighbors, Node current)
    {
        int count = 0;

        for (int dx = -1; dx <= 1; dx++)
        {
            for (int dz = -1; dz <= 1; dz++)
            {
                if (dx == 0 && dz == 0)
                {
                    continue;
                }

                Node neighbor = findValidNeighbor(current.x + dx, current.y, current.z + dz);

                if (neighbor != null)
                {
                    neighbors[count++] = neighbor;
                }
            }
        }

        return count;
    }

    @Override
    public PathType getPathType(PathfindingContext context, int x, int y, int z)
    {
        return context.getPathTypeFromState(x, y, z);
    }

    @Override
    public PathType getPathTypeOfMob(PathfindingContext context, int x, int y, int z, Mob mob)
    {
        return getPathType(context, x, y, z);
    }

    /**
     * Computes the cost of an A* edge {@code from -> to}. Edge cost = horizontal base (axis vs diagonal) plus a
     * vertical component that depends on the sign of the Y delta. Public so {@link ColonyAStarPathFinder} can reuse it
     * without duplicating the constants.
     */
    public float edgeCost(Node from, Node to)
    {
        int dx = Math.abs(to.x - from.x);
        int dz = Math.abs(to.z - from.z);
        int dy = to.y - from.y;

        boolean diagonal = (dx + dz) == 2;
        float horizontal = diagonal ? DIAGONAL_WALK_COST : BASE_WALK_COST;

        float vertical = 0.0f;

        if (dy > 0)
        {
            vertical = CLIMB_PENALTY * dy;
        }

        if (dy < 0)
        {
            vertical = FALL_COST_PER_BLOCK * -dy;
        }

        return horizontal + vertical;
    }

    private @Nullable Node findValidNeighbor(int x, int y, int z)
    {
        if (isWalkable(x, y, z))
        {
            return getNode(x, y, z);
        }

        if (isWalkable(x, y + 1, z))
        {
            return getNode(x, y + 1, z);
        }

        for (int delta = 1; delta <= MAX_FALL_DISTANCE; delta++)
        {
            if (isWalkable(x, y - delta, z))
            {
                return getNode(x, y - delta, z);
            }
        }

        return null;
    }

    private boolean isWalkable(int x, int y, int z)
    {
        if (currentContext == null)
        {
            return false;
        }

        BlockState feet = currentContext.getBlockState(scratch.set(x, y, z));

        if (NodeEvaluator.isBurningBlock(feet))
        {
            return false;
        }

        if (!feet.getCollisionShape(currentContext.level(), scratch).isEmpty())
        {
            return false;
        }

        BlockState head = currentContext.getBlockState(scratch.set(x, y + 1, z));

        if (NodeEvaluator.isBurningBlock(head))
        {
            return false;
        }

        if (!head.getCollisionShape(currentContext.level(), scratch).isEmpty())
        {
            return false;
        }

        BlockState floor = currentContext.getBlockState(scratch.set(x, y - 1, z));

        if (NodeEvaluator.isBurningBlock(floor))
        {
            return false;
        }

        return !floor.getCollisionShape(currentContext.level(), scratch).isEmpty();
    }
}

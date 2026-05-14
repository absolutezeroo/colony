package com.akikazu.colony.common.citizen.pathfinding;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.Target;

import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * V1 minimum-viable A* implementation for Colony pathfinding.
 *
 * <p>
 * The algorithm itself is generic over a {@link Graph} interface so the search loop can be unit-tested with synthetic
 * graphs that don't require a Minecraft world ({@link #search(Graph, Object, int)}). The {@link #findPath} entry point
 * adapts the generic search to vanilla {@link Node}/{@link Target}/{@link Path} types so it can be plugged into
 * {@link net.minecraft.world.level.pathfinder.PathFinder}.
 *
 * <p>
 * Limitations (intentional for V1, hardened in later prompts): no caching, no hierarchical search, no jump-point
 * optimisation, no parallelism. Plain A* on a grid with an admissible Manhattan heuristic.
 */
public final class ColonyAStarPathFinder
{
    /**
     * Generic graph contract consumed by the A* search loop. {@link N} must have value-equality semantics
     * ({@code equals}/{@code hashCode}) so visited-set tracking works.
     */
    public interface Graph<N>
    {
        Iterable<Edge<N>> neighbors(N node);

        float heuristic(N node);

        boolean isGoal(N node);
    }

    public record Edge<N>(N to, float cost)
    {
    }

    private record QueueEntry<N>(N node, float fScore)
    {
    }

    public ColonyAStarPathFinder()
    {
    }

    /**
     * Generic A*. Returns the shortest path from {@code start} to any node for which {@link Graph#isGoal} returns
     * {@code true}, including both endpoints, or {@code null} if no path exists or the visit budget was exceeded.
     */
    public static <N> @Nullable List<N> search(Graph<N> graph, N start, int maxNodesVisited)
    {
        Map<N, N> cameFrom = new HashMap<>();
        Map<N, Float> gScore = new HashMap<>();
        Set<N> closed = new HashSet<>();
        PriorityQueue<QueueEntry<N>> openSet = new PriorityQueue<>(Comparator.comparingDouble(QueueEntry::fScore));

        gScore.put(start, 0.0f);
        openSet.add(new QueueEntry<>(start, graph.heuristic(start)));

        if (graph.isGoal(start))
        {
            return reconstructPath(cameFrom, start);
        }

        int visited = 0;

        while (!openSet.isEmpty())
        {
            QueueEntry<N> entry = openSet.poll();
            N current = entry.node();

            if (closed.contains(current))
            {
                continue;
            }

            if (graph.isGoal(current))
            {
                return reconstructPath(cameFrom, current);
            }

            visited++;

            if (visited > maxNodesVisited)
            {
                return null;
            }

            closed.add(current);

            for (Edge<N> edge : graph.neighbors(current))
            {
                N next = edge.to();

                if (closed.contains(next))
                {
                    continue;
                }

                float tentativeG = gScore.getOrDefault(current, Float.MAX_VALUE) + edge.cost();

                if (tentativeG < gScore.getOrDefault(next, Float.MAX_VALUE))
                {
                    cameFrom.put(next, current);
                    gScore.put(next, tentativeG);
                    openSet.add(new QueueEntry<>(next, tentativeG + graph.heuristic(next)));
                }
            }
        }

        return null;
    }

    /**
     * Vanilla-typed entry point. Builds an internal {@link Graph} adapter around {@code evaluator}, runs the generic
     * search, and wraps the resulting node sequence in a {@link Path}. {@code maxRange} caps the geometric distance
     * from {@code start} that any neighbour is allowed to live at; nodes beyond it are pruned.
     */
    public @Nullable Path findPath(
            ColonyNodeEvaluator evaluator,
            Node start,
            Set<Target> targets,
            float maxRange,
            int maxNodesVisited)
    {
        if (targets.isEmpty())
        {
            return null;
        }

        Node[] neighborBuf = new Node[8];

        Graph<Node> graph = new Graph<>()
        {
            @Override
            public Iterable<Edge<Node>> neighbors(Node node)
            {
                int count = evaluator.getNeighbors(neighborBuf, node);
                List<Edge<Node>> out = new ArrayList<>(count);

                for (int i = 0; i < count; i++)
                {
                    Node candidate = neighborBuf[i];

                    if (start.distanceTo(candidate) > maxRange)
                    {
                        continue;
                    }

                    out.add(new Edge<>(candidate, evaluator.edgeCost(node, candidate)));
                }

                return out;
            }

            @Override
            public float heuristic(Node node)
            {
                float best = Float.MAX_VALUE;

                for (Target target : targets)
                {
                    float distance = node.distanceManhattan(target);

                    if (distance < best)
                    {
                        best = distance;
                    }
                }

                return best;
            }

            @Override
            public boolean isGoal(Node node)
            {
                for (Target target : targets)
                {
                    if (target.x == node.x && target.y == node.y && target.z == node.z)
                    {
                        return true;
                    }
                }

                return false;
            }
        };

        List<Node> nodePath = search(graph, start, maxNodesVisited);

        if (nodePath == null || nodePath.isEmpty())
        {
            return null;
        }

        Node end = nodePath.get(nodePath.size() - 1);
        BlockPos targetPos = new BlockPos(end.x, end.y, end.z);

        return new Path(new ArrayList<>(nodePath), targetPos, true);
    }

    private static <N> List<N> reconstructPath(Map<N, N> cameFrom, N goal)
    {
        Deque<N> reversed = new ArrayDeque<>();
        N cursor = goal;
        reversed.addFirst(cursor);

        while (cameFrom.containsKey(cursor))
        {
            cursor = cameFrom.get(cursor);
            reversed.addFirst(cursor);
        }

        return new ArrayList<>(reversed);
    }
}

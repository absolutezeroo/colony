package com.akikazu.colony.common.citizen.pathfinding;

import com.akikazu.colony.common.citizen.pathfinding.ColonyAStarPathFinder.Edge;
import com.akikazu.colony.common.citizen.pathfinding.ColonyAStarPathFinder.Graph;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-logic unit tests for {@link ColonyAStarPathFinder#search(Graph, Object, int)}. These do not pull Minecraft so
 * they can run in {@code :common}'s test source set without booting a NeoForge environment.
 */
class ColonyAStarPathFinderTest
{
    @Test
    void findsPathOnSimpleGraph()
    {
        AdjacencyGraph<String> graph = AdjacencyGraph.zeroHeuristic("C");
        graph.addEdge("A", "B", 1.0f);
        graph.addEdge("B", "C", 1.0f);

        List<String> path = ColonyAStarPathFinder.search(graph, "A", 100);

        assertEquals(List.of("A", "B", "C"), path);
    }

    @Test
    void findsShortestPathWhenMultipleExist()
    {
        AdjacencyGraph<String> graph = AdjacencyGraph.zeroHeuristic("D");
        graph.addEdge("A", "B", 1.0f);
        graph.addEdge("B", "D", 1.0f);
        graph.addEdge("A", "C", 5.0f);
        graph.addEdge("C", "D", 5.0f);

        List<String> path = ColonyAStarPathFinder.search(graph, "A", 100);

        assertEquals(List.of("A", "B", "D"), path);
    }

    @Test
    void returnsNullWhenNoPath()
    {
        AdjacencyGraph<String> graph = AdjacencyGraph.zeroHeuristic("Z");
        graph.addEdge("A", "B", 1.0f);
        graph.addEdge("Y", "Z", 1.0f);

        List<String> path = ColonyAStarPathFinder.search(graph, "A", 100);

        assertNull(path);
    }

    @Test
    void returnsNullWhenExceedsMaxNodes()
    {
        AdjacencyGraph<Integer> graph = AdjacencyGraph.zeroHeuristic(99);

        for (int i = 0; i < 99; i++)
        {
            graph.addEdge(i, i + 1, 1.0f);
        }

        List<Integer> path = ColonyAStarPathFinder.search(graph, 0, 5);

        assertNull(path);
    }

    @Test
    void heuristicIsAdmissibleOnGridGraph()
    {
        GridGraph grid = new GridGraph(10, 10, new GridPos(9, 9));

        List<GridPos> path = ColonyAStarPathFinder.search(grid, new GridPos(0, 0), 1000);

        assertNotNull(path);

        float actualCost = 0.0f;

        for (int i = 1; i < path.size(); i++)
        {
            actualCost += grid.edgeCost(path.get(i - 1), path.get(i));
        }

        float startHeuristic = grid.heuristic(new GridPos(0, 0));

        assertTrue(
                startHeuristic <= actualCost,
                "Manhattan heuristic " + startHeuristic + " must not exceed actual path cost " + actualCost);
    }

    private static final class AdjacencyGraph<N> implements Graph<N>
    {
        private final Map<N, List<Edge<N>>> edges = new HashMap<>();

        private final Set<N> goals = new HashSet<>();

        static <N> AdjacencyGraph<N> zeroHeuristic(N goal)
        {
            AdjacencyGraph<N> graph = new AdjacencyGraph<>();
            graph.goals.add(goal);

            return graph;
        }

        void addEdge(N from, N to, float cost)
        {
            edges.computeIfAbsent(from, k -> new ArrayList<>()).add(new Edge<>(to, cost));
            edges.computeIfAbsent(to, k -> new ArrayList<>());
        }

        @Override
        public Iterable<Edge<N>> neighbors(N node)
        {
            return edges.getOrDefault(node, List.of());
        }

        @Override
        public float heuristic(N node)
        {
            return 0.0f;
        }

        @Override
        public boolean isGoal(N node)
        {
            return goals.contains(node);
        }
    }

    private record GridPos(int x, int y)
    {
    }

    private static final class GridGraph implements Graph<GridPos>
    {
        private final int width;

        private final int height;

        private final GridPos goal;

        GridGraph(int width, int height, GridPos goal)
        {
            this.width = width;
            this.height = height;
            this.goal = goal;
        }

        @Override
        public Iterable<Edge<GridPos>> neighbors(GridPos node)
        {
            List<Edge<GridPos>> result = new ArrayList<>(4);
            int[][] deltas = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };

            for (int[] delta : deltas)
            {
                int nx = node.x() + delta[0];
                int ny = node.y() + delta[1];

                if (nx < 0 || ny < 0 || nx >= width || ny >= height)
                {
                    continue;
                }

                result.add(new Edge<>(new GridPos(nx, ny), 1.0f));
            }

            return result;
        }

        @Override
        public float heuristic(GridPos node)
        {
            return Math.abs(node.x() - goal.x()) + Math.abs(node.y() - goal.y());
        }

        @Override
        public boolean isGoal(GridPos node)
        {
            return node.equals(goal);
        }

        float edgeCost(GridPos from, GridPos to)
        {
            return Math.abs(from.x() - to.x()) + Math.abs(from.y() - to.y());
        }
    }
}

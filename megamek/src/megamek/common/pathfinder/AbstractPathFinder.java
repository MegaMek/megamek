/*
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.pathfinder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;

import megamek.common.pathfinder.filters.Filter;
import megamek.logging.MMLogger;

/**
 * This class provides a skeletal implementation of pathfinder algorithm in a given directed graph.
 * <p>
 * It uses a generalisation of Dijkstra algorithm. User must provide methods that allow traversing the graph and
 * evaluating paths. All needed methods have been encapsulated and separated in classes:
 * <ul>
 * <li>DestinationNodeFactory and EdgeNeighborsFactory - responsible for
 * representing graph</li>
 * <li>Filter - Filters edges that are produced by EdgeNeighborsFactory. It
 * allows EdgeNeighborsFactory to be a general use class.</li>
 * <li>Comparator - compares paths according to generated cost.</li>
 * <li>EdgeRelaxer - relaxes node cost.</li>
 * <li>StopCondition - responsible for halting if user does not want to
 * traverse whole graph.</li>
 * </ul>
 *
 * @param <N> the type of nodes in the graph.
 * @param <C> the type of computed lowest cost for a node. If needed this type can contain information for recreating
 *            the path.
 * @param <E> the type of directed edges used by the graph.
 *
 * @author Saginatio
 */
public class AbstractPathFinder<N, C, E> {
    private static final MMLogger logger = MMLogger.create(AbstractPathFinder.class);

    // after switching to java 8 and including java.util.function some of this
    // subclasses should be removed

    // way of checking multiple conditions and returning their alternation.
    private static class StopConditionsAlternation<E> implements StopCondition<E> {
        private final List<StopCondition<? super E>> conditions = new ArrayList<>();

        @Override
        public boolean shouldStop(E e) {
            boolean stop = false;
            for (StopCondition<? super E> cond : conditions) {
                stop = cond.shouldStop(e) | stop;
            }
            return stop;
        }
    }

    private AdjacencyMap<E> adjacencyMap;

    private PriorityQueue<E> candidates;

    private Comparator<E> comparator;
    private DestinationMap<N, E> destinationMap;
    private EdgeRelaxer<C, E> edgeRelaxer;

    private final List<Filter<E>> filters = new ArrayList<>();

    private final Map<N, C> pathsCosts = new HashMap<>();

    private final StopConditionsAlternation<E> stopCondition = new StopConditionsAlternation<>();

    /**
     * @param edgeDestinationMap functional interface for retrieving destination node of an edge.
     * @param edgeRelaxer        functional interface for calculating relaxed cost.
     * @param edgeAdjacencyMap   functional interface for retrieving neighbouring edges.
     * @param edgeComparator     implementation of path comparator. Each path is defined by its last edge. <i>(path:=
     *                           edge concatenated with the best path to the source of the edge)</i>
     */
    public AbstractPathFinder(DestinationMap<N, E> edgeDestinationMap, EdgeRelaxer<C, E> edgeRelaxer,
          AdjacencyMap<E> edgeAdjacencyMap, Comparator<E> edgeComparator) {
        if (edgeDestinationMap == null
              || edgeRelaxer == null
              || edgeAdjacencyMap == null
              || edgeComparator == null) {
            throw new IllegalArgumentException("Arguments must be non null:"
                  + stopCondition + edgeDestinationMap + edgeRelaxer + edgeAdjacencyMap + edgeComparator);
        }
        this.destinationMap = edgeDestinationMap;
        this.edgeRelaxer = edgeRelaxer;
        this.adjacencyMap = edgeAdjacencyMap;
        this.comparator = edgeComparator;

        candidates = new PriorityQueue<>(100, edgeComparator);
    }

    /**
     * Adds an EdgeFilter. If this method is invoked multiple times: an edge is removed from the graph iff at least one
     * filter removes it.
     *
     * @see Filter
     */
    public void addFilter(Filter<E> edgeFilter) {
        filters.add(Objects.requireNonNull(edgeFilter));
    }

    public void removeAllFilters() {
        filters.clear();
    }

    /**
     * @see StopCondition
     */
    public void addStopCondition(StopCondition<E> stopCondition) {
        this.stopCondition.conditions.add(Objects.requireNonNull(stopCondition));
    }

    /**
     * Computes shortest paths to nodes in the graph.
     *
     * @param startingEdges a collection of possible starting edges.
     */
    public void run(Collection<E> startingEdges) {
        try {
            if (!candidates.isEmpty()) {
                candidates.clear();
                pathsCosts.clear();
            }
            candidates.addAll(startingEdges);
            while (!candidates.isEmpty()) {
                // remove the best candidate from the queue
                E e = candidates.remove();
                // get the destination node
                N node = destinationMap.getDestination(e);
                C cost = pathsCosts.get(node);
                // check if the candidate edge gives better cost
                C newCost = edgeRelaxer.doRelax(cost, e, comparator);
                if (newCost != null) {
                    // we have a better path to this node, so we can update it
                    pathsCosts.put(node, newCost);
                    Collection<E> filteredNeighbours = adjacencyMap.getAdjacent(e);
                    for (Filter<E> f : filters) {
                        filteredNeighbours = f.doFilter(filteredNeighbours);
                    }
                    candidates.addAll(filteredNeighbours);
                }

                if (stopCondition.shouldStop(e)) {
                    break;
                }
            }
        } catch (OutOfMemoryError ex) {
            logger.error(
                  "Not enough memory to analyse all options. Try setting time limit to lower value, or increase java memory limit.",
                  ex);
        } catch (IllegalArgumentException ex) {
            logger.debug("Lost sight of a unit while plotting predicted paths", ex);
        } catch (Exception ex) {
            // Do something, don't just swallow the exception, good lord
            logger.error("", ex);
        }
    }

    /**
     * Computes shortest paths to nodes in the graph.
     *
     * @param start a starting edge.
     */
    public void run(E start) {
        run(Collections.singleton(start));
    }

    /**
     * @return edge comparator used by this AbstractPathFinder
     */
    public Comparator<E> getComparator() {
        return comparator;
    }

    /**
     * @return calculated cost for this node or null if this node has not been reached.
     */
    protected C getCostOf(N node) {
        return pathsCosts.get(node);
    }

    /**
     * Returns the cost map. <b>Important:</b> Neither the returned map, nor its elements, should be modified.
     *
     * @return map Node to LowestCost
     */
    protected Map<N, C> getPathCostMap() {
        return pathsCosts;
    }

    /**
     * @see AdjacencyMap
     */
    public void setAdjacencyMap(AdjacencyMap<E> edgeNeighborsFactory) {
        this.adjacencyMap = Objects.requireNonNull(edgeNeighborsFactory);
    }

    public AdjacencyMap<E> getAdjacencyMap() {
        return adjacencyMap;
    }

    /**
     * Sets comparator.
     *
     * @param comparator implementation of path comparator. Each path is uniquely defined by its last edge. <i>(path:=
     *                   an edge concatenated with the best path to the source of the edge)</i>
     */
    public void setComparator(Comparator<E> comparator) {
        this.comparator = Objects.requireNonNull(comparator);
        this.candidates = new PriorityQueue<>(100, comparator);
    }

    /**
     * @see DestinationMap
     */
    public void setDestinationMap(DestinationMap<N, E> nodeFactory) {
        this.destinationMap = Objects.requireNonNull(nodeFactory);
    }

    protected DestinationMap<N, E> getDestinationMap() {
        return destinationMap;
    }

    /**
     * @see EdgeRelaxer
     */
    public void setEdgeRelaxer(EdgeRelaxer<C, E> costRelaxer) {
        this.edgeRelaxer = Objects.requireNonNull(costRelaxer);
    }
}

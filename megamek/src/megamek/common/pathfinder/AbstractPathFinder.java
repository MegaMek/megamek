package megamek.common.pathfinder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import megamek.common.logging.DefaultMmLogger;
import megamek.common.logging.LogLevel;
import megamek.common.logging.MMLogger;

/**
 * This class provides a skeletal implementation of path finder algorithm in a
 * given directed graph.
 * 
 * It uses a generalisation of Dijkstra algorithm. User must provide methods
 * that allow traversing the graph and evaluating paths. All needed methods have
 * been encapsulated and separated in classes:
 * <ul>
 * <li/>DeestinationNodeFactory and EdgeNeighborsFactory - responsible for
 * representing graph
 * <li/>Filter - Filters edges that are produced by EdgeNeighborsFactory. It
 * allows EdgeNeighborsFactory to be a general use class.
 * <li/>Comparator - compares paths according to generated cost.
 * <li/>EdgeRelaxer - relaxes node cost.
 * <li/>StopCondition - responsible for halting if user does not want to
 * traverse whole graph.
 * </ul>
 * 
 * 
 * 
 * @author Saginatio
 * 
 * @param <N> the type of nodes in the graph.
 * @param <C> the type of computed lowest cost for a node. If needed this type
 *            can contain information for recreating the path.
 * @param <E> the type of directed edges used by the graph.
 * 
 * @see PathFinderUtility
 */
public class AbstractPathFinder<N, C, E> {

    //after switching to java 8 and including java.util.function some of this
    //subclasses should be removed

    /**
     * Factory for retrieving neighbouring edges.
     * 
     * @param <E> the type of directed edges used by the graph.
     */
    public interface AdjacencyMap<E> {
        /**
         * @param e a directed edge
         * @return all the edges that lead from destination node of e
         */
        public Collection<E> getAdjacent(E e);
    }

    /**
     * Represents a function for retrieving destination node of an edge.
     * 
     * @param <N> the type of nodes in the graph
     * @param <E> the type of directed edges used by the graph
     */
    public interface DestinationMap<N, E> {
        /**
         * Returns a destination node of a given edge.
         * 
         * @param e a directed edge
         * @return the destination node of the given edge
         */
        public N getDestination(E e);
    }

    /**
     * Represents a function that relaxes an edge.
     * 
     * @param <C> the type of computed lowest cost for a node
     * @param <E> the type of directed edges used by the graph
     * 
     * @see <a
     *      href=http://masters.donntu.edu.ua/2006/ggeo/ganushchak/library/art8
     *      .htm> Description of relaxation </a>
     */
    public interface EdgeRelaxer<C, E> {
        /**
         * Relaxes an edge.
         * 
         * @param v best value till now. Might be null.
         * @param e candidate for the new best value
         * @param comparator edge comparator
         * @return new best value or null if no relaxation happened
         */
        public C doRelax(C v, E e, Comparator<E> comparator);
    }

    /**
     * Represents a function that allows removing unwanted objects from a
     * collection.
     * 
     */
    public static abstract class Filter<T> {
        /**
         * Returns filtered collection by removing those objects that fail
         * {@link #shouldStay(T)} test.
         * 
         * @param collection collection to be filtered
         * @return filtered collection
         */
        public Collection<T> doFilter(Collection<T> collection) {
            List<T> filteredMoves = new ArrayList<>();
            for (T e : collection) {
                if (shouldStay(e))
                    filteredMoves.add(e);
            }
            return filteredMoves;
        }

        /**
         * Tests if the object should stay in the collection.
         * 
         * @param object tested object
         * @return true if the object should stay in the collection
         */
        public abstract boolean shouldStay(T object);
    }

    /**
     * The stop condition that is processed after every successful relaxation.
     * 
     * @param <E> the type of directed edges used by the graph
     */
    public interface StopCondition<E> {
        /**
         * @param e the last edge that was successfully relaxed
         * @return true iff algorithm should stop searching for new paths
         */
        public boolean shouldStop(E e);
    }

    //way of checking multiple conditions and returning their alternation.
    private static class StopConditionsAlternation<E> implements StopCondition<E> {
        private List<StopCondition<? super E>> conditions = new ArrayList<>();

        @Override
        public boolean shouldStop(E e) {
            boolean stop = false;
            for (StopCondition<? super E> cond : conditions) {
                stop = cond.shouldStop(e) | stop;
            }
            return stop;
        }
    }

    /**
     * A timeout stop condition. The shouldStop() returns answer based on time
     * elapsed since initialisation or last restart() call.
     */
    public static class StopConditionTimeout<E> implements AbstractPathFinder.StopCondition<E> {
        //this class should be redesigned to use an executor.
        private E lastEdge;
        private long start;
        private long stop;
        final int timeout;

        public boolean timeoutEngaged;

        public StopConditionTimeout(int timeoutMillis) {
            this.timeout = timeoutMillis;
            restart();
        }

        public E getLastEdge() {
            return lastEdge;
        }

        public int getTimeout() {
            return timeout;
        }

        public void restart() {
            start = System.currentTimeMillis();
            stop = start + timeout;
            lastEdge = null;
            timeoutEngaged = false;
        }

        @Override
        public boolean shouldStop(E e) {
            if (System.currentTimeMillis() > stop) {
                timeoutEngaged = true;
                lastEdge = e;
                return true;
            }
            return false;
        }

        public boolean wasTimeoutEngaged() {
            return timeoutEngaged;
        }

    }

    private AdjacencyMap<E> adjacencyMap;

    private PriorityQueue<E> candidates;

    private Comparator<E> comparator;
    private DestinationMap<N, E> destinationMap;
    private EdgeRelaxer<C, E> edgeRelaxer;

    private List<Filter<E>> filters = new ArrayList<>();

    private Map<N, C> pathsCosts = new HashMap<>();

    private StopConditionsAlternation<E> stopCondition = new StopConditionsAlternation<>();

    private MMLogger logger;
    private MMLogger getLogger() {
        return logger == null ? logger = DefaultMmLogger.getInstance() : logger;
    }
    
    /**
     * @param edgeDestinationMap functional interface for retrieving destination
     *            node of an edge.
     * @param edgeRelaxer functional interface for calculating relaxed cost.
     * @param edgeAdjacencyMap functional interface for retrieving neighbouring
     *            edges.
     * @param edgeComparator implementation of path comparator. Each path is
     *            defined by its last edge. <i>(path:= edge concatenated with
     *            best path to the source of the edge)</i>
     */
    public AbstractPathFinder(DestinationMap<N, E> edgeDestinationMap, EdgeRelaxer<C, E> edgeRelaxer,
            AdjacencyMap<E> edgeAdjacencyMap, Comparator<E> edgeComparator) {
        if (edgeDestinationMap == null
                || edgeRelaxer == null
                || edgeAdjacencyMap == null
                || edgeComparator == null) {
            throw new IllegalArgumentException("Arguments must be non null:" ////$NON-NLS-1$
                    + stopCondition + edgeDestinationMap + edgeRelaxer + edgeAdjacencyMap + edgeComparator);
        }
        this.destinationMap = edgeDestinationMap;
        this.edgeRelaxer = edgeRelaxer;
        this.adjacencyMap = edgeAdjacencyMap;
        this.comparator = edgeComparator;

        candidates = new PriorityQueue<E>(100, edgeComparator);
    }

    /**
     * Adds an EdgeFilter. If this method is invoked multiple times: an edge is
     * removed from the graph iff at least one filter removes it.
     * 
     * @see Filter
     */
    public void addFilter(Filter<E> edgeFilter) {
        if (edgeFilter == null)
            throw new NullPointerException();
        filters.add(edgeFilter);
    }

    public void removeAllFilters() {
        filters.clear();
    }

    /**
     * @see StopCondition
     */
    public void addStopCondition(StopCondition<E> stopCondition) {
        if (stopCondition == null)
            throw new NullPointerException();
        this.stopCondition.conditions.add(stopCondition);
    }

    /**
     * Computes shortest paths to nodes in the graph.
     * 
     * @param startingEdges a collection of possible starting edges.
     */
    public void run(Collection<E> startingEdges) {
        final String METHOD_NAME = "run";
        
        try {
            if (candidates.size() > 0) {
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
                    Collection<E> neighbours = adjacencyMap.getAdjacent(e);
                    Collection<E> filteredNeighbours = neighbours;
                    for (Filter<E> f : filters) {
                        filteredNeighbours = f.doFilter(filteredNeighbours);
                    }
                    candidates.addAll(filteredNeighbours);
                }
                if (stopCondition.shouldStop(e))
                    break;
            }
        } catch (OutOfMemoryError e) {
            final String memoryMessage = "Not enough memory to analyse all options."//$NON-NLS-1$
                    + " Try setting time limit to lower value, or "//$NON-NLS-1$
                    + "increase java memory limit.";
            
            getLogger().log(this.getClass(), METHOD_NAME, LogLevel.ERROR, memoryMessage, e);
        } catch(Exception e) {
            getLogger().log(this.getClass(), METHOD_NAME, e); //do something, don't just swallow the exception, good lord
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
     * @param node
     * @return calculated cost for this node or null if this node has not been
     *         reached.
     */
    protected C getCostOf(N node) {
        return pathsCosts.get(node);
    }

    /**
     * Returns the cost map. <b>Important:</b> Neither the returned map, nor its
     * elements, should be modified.
     * 
     * @return map Node -> LowestCost
     */
    protected Map<N, C> getPathCostMap() {
        return pathsCosts;
    }

    /**
     * @see AdjacencyMap
     */
    public void setAdjacencyMap(AdjacencyMap<E> edgeNeighborsFactory) {
        if (edgeNeighborsFactory == null)
            throw new NullPointerException();
        this.adjacencyMap = edgeNeighborsFactory;
    }
    
    public AdjacencyMap<E> getAdjacencyMap() {
        return adjacencyMap;
    }

    /**
     * Sets comparator.
     * 
     * @param comparator implementation of path comparator. Each path is
     *            uniquely defined by its last edge. <i>(path:= an edge
     *            concatenated with the best path to the source of the edge)</i>
     */
    public void setComparator(Comparator<E> comparator) {
        if (comparator == null)
            throw new NullPointerException();
        this.comparator = comparator;
        this.candidates = new PriorityQueue<E>(100, comparator);
    }

    /**
     * @see DestinationMap
     */
    public void setDestinationMap(DestinationMap<N, E> nodeFactory) {
        if (nodeFactory == null)
            throw new NullPointerException();
        this.destinationMap = nodeFactory;
    }
    
    protected DestinationMap<N, E> getDestinationMap() {
        return destinationMap;
    }

    /**
     * @see EdgeRelaxer
     */
    public void setEdgeRelaxer(EdgeRelaxer<C, E> costRelaxer) {
        if (costRelaxer == null)
            throw new NullPointerException();
        this.edgeRelaxer = costRelaxer;
    }

}

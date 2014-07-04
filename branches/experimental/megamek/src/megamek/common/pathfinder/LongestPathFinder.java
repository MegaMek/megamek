package megamek.common.pathfinder;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import megamek.common.Coords;
import megamek.common.IGame;
import megamek.common.MovePath;
import megamek.common.MovePath.MoveStepType;
import megamek.common.pathfinder.MovePathFinder.MovePathLegalityFilter;

public class LongestPathFinder extends MovePathFinder<Deque<MovePath>> {

    public LongestPathFinder(int maxMP, MoveStepType stepType, IGame game) {
        super(new LongestPathRelaxer(), new NextStepsAdjacencyMap(stepType), new MovePathMinMPMaxDistanceComparator(), game);
        addFilter(new MovePathLengthFilter(maxMP));
        addFilter(new MovePathLegalityFilter(game));
    }

    public static class MovePathMinMPMaxDistanceComparator extends MovePathMPCostComparator {
        @Override
        public int compare(MovePath first, MovePath second) {
            int s = super.compare(first, second);
            if (s != 0)
                return s;
            else
                return second.getHexesMoved() - first.getHexesMoved();
        }
    }

    static public class LongestPathRelaxer implements EdgeRelaxer<Deque<MovePath>, MovePath> {
        @Override
        public Deque<MovePath> doRelax(Deque<MovePath> v, MovePath mpCandidate, Comparator<MovePath> comparator) {
            if (mpCandidate == null)
                throw new NullPointerException();
            if (v == null) {
                return new ArrayDeque<>(Collections.singleton(mpCandidate));
            }
            while (!v.isEmpty()) {
                MovePath topMP = v.getLast();
                //topMP has less or equal 'movement points used' since it was taken from 
                //candidates priority queue earlier
                if (topMP.getHexesMoved() >= mpCandidate.getHexesMoved()) {
                    return null; //current path is better
                } else { // i.e. topMP.getHexesMoved() < mp.getHexesMoved() )
                    if (topMP.getMpUsed() < mpCandidate.getMpUsed()) {
                        //topMP travels less but uses less movement points so we should keep it.
                        v.addLast(mpCandidate);
                        return v;
                    } else { //i.e. topMP.getMpUsed() == mp.getMpUsed()
                        v.removeLast(); //mpCandidate is better                    }
                    }
                }
            }
            if (v.isEmpty())
                v.addLast(mpCandidate);

            return v;
        }

    }

    /**
     * Returns the longest move path to a hex at given coordinates. If multiple
     * paths reach coords with different final facings, the best one is chosen.
     * If none paths are present then {@code null} is returned.
     * 
     * 
     * @param coordinates - the coordinates of the hex
     * @return the shortest move path to hex at given coordinates
     */
    public MovePath getComputedPath(Coords coords) {
        Deque<MovePath> q = getCost(coords, new Comparator<Deque<MovePath>>() {
            @Override
            public int compare(Deque<MovePath> q1, Deque<MovePath> q2) {
                MovePath mp1 = q1.getLast(), mp2 = q2.getLast();
                int t = mp2.getHexesMoved() - mp1.getHexesMoved();
                if (t != 0)
                    return t;
                else
                    return mp1.getMpUsed() - mp2.getMpUsed();
            }
        });
        if (q != null)
            return q.getLast();
        else
            return null;
    }

    /**
     * Returns a map of all computed longest paths. This also includes paths
     * that are shorter but use strictly less movement points.
     * 
     * @return a map of all computed shortest paths.
     */
    public List<MovePath> getAllComputedPathsUnordered() {
        Collection<Deque<MovePath>> queues = getPathCostMap().values();
        List<MovePath> l = new ArrayList<>();
        for (Deque<MovePath> q : queues) {
            l.addAll(q);
        }
        return l;
    }
}

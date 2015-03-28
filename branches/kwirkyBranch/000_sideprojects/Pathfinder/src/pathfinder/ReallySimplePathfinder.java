/*
 * 
 */
package pathfinder;

import pathfinder.elements.IBoard;
import pathfinder.elements.ICoord;
import pathfinder.elements.IPath;
import pathfinder.elements.ReallySimplePath;

/**
 * This pathfinder is really dumb but really fast. Proof of concept.
 * @author kwirkyj
 */
public class ReallySimplePathfinder implements IPathfinder {

    /**
     * Go from start to end in the most linear path possible, ignoring any
     * cost of 'terrain' or 'turning.'
     * @param board Board instance to check if coords are valid.
     * @param start Starting coordinate.
     * @param end Ending coordinate.
     * @throws ImpossiblePathException if start or end coord are not found on the board.
     * @return empty ReallSimplePath iff start == end; 
     *         else, ReallySimplePath leading to end, starting point omitted.
     */
    @Override
    public IPath getPath(IBoard board, ICoord start, ICoord end) {
        ReallySimplePath p = new ReallySimplePath();
        if (! (board.hasCoord(end) && board.hasCoord(start))){
            throw new ImpossiblePathException();
        }
        if (start.equals(end)) return p;
        ICoord current = start;
        while (!current.equals(end)) {
            current = findNextClosest(current, end);
            p.append(current);
        }
        return p;
    }
    
    /**
     * utility method to get the next coordinate closest to the endpoint.
     * @param current
     * @param end
     * @return 
     */
    private ICoord findNextClosest(ICoord current, ICoord end) {
        ICoord[] adjoining = current.getAdjoining();
        double[] end_cart = end.getCartesian();
        double least_dist = Float.MAX_VALUE;
        int least_i = -1;
        for (int i = 0; i < adjoining.length; i++) {
            ICoord c = adjoining[i];
            if (end.equals(c)) { return c; }
            double[] adj_cart = c.getCartesian();
            double dx = end_cart[0] - adj_cart[0];
            double dy = end_cart[1] - adj_cart[1];
            double dist =  Math.sqrt(Math.pow(dx,2) + Math.pow(dy,2));
            if (dist < least_dist) {
                least_dist = dist;
                least_i = i;
            }
        }
        return adjoining[least_i];
    }    
}

/*
 * 
 */
package pathfinder.elements;

/**
 * A high-level interface for operations one expects from a coordinate of
 * any system (XY, UV, etc.). Coordinate values are expected as integers.
 * For simplicity in this application domain, a coordinate is simultaneously
 * treated as a node in an unweighted graph, with each coord aware of
 * what its adjacent neighbors should be; whether they actually exist 
 * is for other classes to verify, if necessary.
 * @author kwirkyj
 */
public interface ICoord {
    /**
     * Coordinates that are adjacent to this coord.
     * @return Array of Coords adjacent to this coordinate; order undefined.
     */
    public ICoord[] getAdjoining();
    
    /**
     * Get this Coord's coordinates.
     * @return <code>int[]</code> of coordinates (e.g., (x,y)).
     */
    public int[] getCoords();
    
    /**
     * Convert to exact cartesian coordinate.
     * @param scale Factor between systems.
     * @return <code>double[]</code>
     */
    public double[] getCartesian(double scale);
    
    /**
     * Convert to exact cartesian coordinate with 1:1 scale between systems.
     * @return <code>double[]</code>
     */
    public double[] getCartesian();
}

/*
 * 
 */
package pathfinder.elements;

/**
 * Interface for a board-like object. For this application domain,
 * a board is treated as a grid, simplifying the class structures.
 * @author kwirkyj
 */
public interface IBoard {
    /**
     * Check if the coordinate is on the board.
     * @param c A given coordinate.
     * @return True iff the coordinate is part of the board.
     */
    public boolean hasCoord(ICoord c);
    
    /**
     * Get the object at given coordinates if it is part of the board.
     * @param c A given coordinate.
     * @return <code>null</code> iff <code>!board.contains(c)</code>;
     *         else, the <code>Coord</code>-like object at given coordinates.
     */
    public ICoord getCoord(ICoord c);
    
    /**
     * Get the extents of the board; implementation-dependent pattern. 
     * @return Array defining the extents of the board pattern.
     */
    public ICoord[] getExtents();
    
    /**
     * TODO: decide behavior of 'islands' and single-hex 'peninsula' or 'isthmus.'
     * Get a list of all coords along the boundary of the board, starting at
     * the 'topmost' coord.
     * @return 
     */
    // public ArrayList<ICoord> getEdge();
}

/*
 * 
 */
package pathfinder;

import pathfinder.elements.IBoard;
import pathfinder.elements.ICoord;
import pathfinder.elements.IPath;

/**
 * High-level interface for all path-finders. We expect that any pathfinder
 * will support the finding of a path.
 * TODO: any benefit by changing to an abstract class?
 * @author kwirkyj
 */
public interface IPathfinder {
    /**
     * Get the path from start to end to end.
     * TODO: throw various exceptions rather than returning null?
     * @param board Instance of a board.
     * @param start Coordinate of starting point.
     * @param end Coordinate of endpoint.
     * @return A Path to go from start to end; null if impossible.
     */
    public IPath getPath(IBoard board, ICoord start, ICoord end);
}

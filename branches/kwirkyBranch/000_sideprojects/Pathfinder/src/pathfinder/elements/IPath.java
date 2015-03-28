/*
 * 
 */
package pathfinder.elements;

import java.util.ListIterator;

/**
 * High-level interface of a path.
 * Consists of a sequence (some kind of list) of ICoord elements,
 * the exact implementation details of either being irrelevant to this
 * Interface.
 * @author kwirkyj
 */
public interface IPath {
    //TODO: ICoord -> IMoveAction?
    
    /**
     * Get a list iterator to access a path's elements.
     * @return ListIterator&lt;ICoord&gt;
     */
    public ListIterator<ICoord> getListIterator();
    
    /**
     * Number of steps in path.
     * @return int.
     */
    public int getLength();
    
    /**
     * Cost of the path.
     * @return float.
     */
    public float getCost();
    
    /**
     * Add an element to the end of the path.
     * @param c ICoord instance to append.
     */
    public void append(ICoord c);
    
    /**
     * Join two paths.
     * @param p IPath instance to append.
     */
    public void append(IPath p);
}

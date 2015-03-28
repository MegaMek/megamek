/*
 * 
 */
package pathfinder.elements;

import java.util.LinkedList;
import java.util.ListIterator;

/**
 * A sequence of base ICoord objects, in theory constituting a path
 * from one point in a graph to another in an unbroken continuity of
 * coordinates/nodes. Relies on path-finders and -validators to ensure
 * correctness.
 * 
 * (hint: it's really just a fancy wrapper for a (probably) LinkedList.)
 * @author kwirkyj
 */
public class ReallySimplePath implements IPath {
    private LinkedList<ICoord> ll;
    
    public ReallySimplePath() {
        this.ll = new LinkedList<>();
    }

    @Override
    public ListIterator<ICoord> getListIterator() {
        return this.ll.listIterator();
    }

    @Override
    public int getLength() {
        return this.ll.size();
    }

    /**
     * Cost of the path.
     * @return float of getLength().
     */
    @Override
    public float getCost() {
        return (float) this.ll.size();
    }

    @Override
    public void append(ICoord c) {
        if (c == null) {
            throw new IllegalArgumentException(
                      "ICoord element cannot be null");
        }
        this.ll.add(c);
    }

    @Override
    public void append(IPath p) {
        ListIterator pi = p.getListIterator();
        while (pi.hasNext()) {
            this.ll.add((ICoord) pi.next());
        }
    }
}

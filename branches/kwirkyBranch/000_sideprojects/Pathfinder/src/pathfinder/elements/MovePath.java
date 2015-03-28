/*
 * 
 */
package pathfinder.elements;

import java.util.ListIterator;

/**
 * A sequence of Positions constituting an unbroken movement (in theory;
 * in practice, this class does not impose constraints on its membership,
 * instead relying on those that populate it to do the job right).
 * TODO: everything
 * @author kwirkyj
 */
public class MovePath implements IPath {

    @Override
    public ListIterator<ICoord> getListIterator() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getLength() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public float getCost() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void append(ICoord c) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void append(IPath p) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}

/*
 * 
 */
package pathfinder.elements;

/**
 * Represents a coordinate with a facing.
 * TODO: everything
 * TODO: pros/cons of 'implements ICoord' vs 'extends CoordHexY'
 * @author kwirkyj
 */
public class Position implements ICoord {
    private CoordHexY coord;
    private Facing facing;
    
    public Position getAhead() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public Position getBehind() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    //TODO: get a list of all adjacent ICoords/Positions?

    @Override
    public ICoord[] getAdjoining() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int[] getCoords() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double[] getCartesian(double scale) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double[] getCartesian() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}

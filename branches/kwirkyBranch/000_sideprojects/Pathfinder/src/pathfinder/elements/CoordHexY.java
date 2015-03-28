/*
 * 
 */
package pathfinder.elements;

/**
 * Coordinate on a grid-style hex plane, ala Battletech, left-side high.
 * (Nominally 'upper-left corner' at (1,1), with (2,1) being SE.) 
 * @author kwirkyj
 */
public class CoordHexY implements ICoord {
    private int x;
    private int y;
    
    public CoordHexY() {
        this.x = 1;
        this.y = 1;
    }
    
    public CoordHexY(final int x, final int y) {
        this.x = x;
        this.y = y;
    }
    
    public CoordHexY(final CoordHexY other) {
        this.x = other.x;
        this.y = other.y;
    }
    
    public CoordHexY(final CoordUV other) {
        int[] uv = other.getCoords();
        this.x = uv[1] + 1;
        this.y = 1 - uv[0] - ((int) Math.floor(uv[1] / 2));
    }
    
    public void setX(final int x) {
        this.x = x;
    }
    
    public void setY(final int y) {
        this.y = y;
    }
    
    /**
     * Get array of adjoining CoordHexY elements;
     * @return array length-six, clockwise from 'North' (x, y-1).
     */
    @Override
    public ICoord[] getAdjoining() {
        return (this.x % 2 == 0) ? 
            new CoordHexY[] {
                new CoordHexY(this.x, this.y-1),
                new CoordHexY(this.x+1, this.y),
                new CoordHexY(this.x+1, this.y+1),
                new CoordHexY(this.x, this.y+1),
                new CoordHexY(this.x-1, this.y+1),
                new CoordHexY(this.x-1, this.y)}
            : new CoordHexY[] {
                new CoordHexY(this.x, this.y-1),
                new CoordHexY(this.x+1, this.y-1),
                new CoordHexY(this.x+1, this.y),
                new CoordHexY(this.x, this.y+1),
                new CoordHexY(this.x-1, this.y),
                new CoordHexY(this.x-1, this.y-1)};
    }

    @Override
    public int[] getCoords() {
        return new int[] {this.x, this.y};
    }
    
    @Override
    public double[] getCartesian(double scale) {
        double adjacent = Math.sqrt(3) / 2;
        double extraY = (this.x % 2 == 0) ? -0.5 : 0.0;
        return new double[]{
            (this.x - 1) * adjacent * scale,
            (extraY - (this.y - 1)) * scale
        };
    }
    
    @Override
    public double[] getCartesian() {
        return this.getCartesian(1.0);
    }
    
    @Override
    public boolean equals(Object o) {
        try {
            return this.equals((CoordHexY) o);
        } catch (Exception e) {
            return false;
        }
    }
    
    public boolean equals(final CoordHexY o) {
        return (o.x == this.x && o.y == this.y) ? true : false;
    }
    
    @Override
    /**
     * machine-generated
     */
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + this.x;
        hash = 71 * hash + this.y;
        return hash;
    }
    
    @Override
    public String toString() {
        return "XY("+this.x+","+this.y+")";
    }
}

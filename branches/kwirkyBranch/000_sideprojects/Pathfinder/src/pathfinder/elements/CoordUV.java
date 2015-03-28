/*
 * 
 */
package pathfinder.elements;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author kwirkyj
 */
public class CoordUV implements ICoord {
    private int u;
    private int v;
    
    public CoordUV() {
        this.u = 0;
        this.v = 0;
    }
    
    public CoordUV(int u, int v) {
        this.u = u;
        this.v = v;
    }
    
    public CoordUV(final CoordUV other) {
        this.u = other.u;
        this.v = other.v;
    }
    
    public CoordUV(final CoordHexY other) {
        int[] oc = other.getCoords();
        int x = oc[0] - 1;
        int y = oc[1] - 1;
        this.v = x;
        this.u = -y - ((int) Math.floor(x / 2));
    }
        
    
    public int getU() {
        return this.u;
    }
    
    public int getV() {
        return this.v;
    }
    
    @Override
    public ICoord[] getAdjoining() {
        return new CoordUV[] {
            new CoordUV(this.u + 1, this.v),
            new CoordUV(this.u, this.v + 1),
            new CoordUV(this.u - 1, this.v + 1),
            new CoordUV(this.u - 1, this.v),
            new CoordUV(this.u, this.v -1),
            new CoordUV(this.u + 1, this.v -1)};
    }
    
    @Override
    public int[] getCoords() {
        int[] c = {this.u, this.v};
        return c;
    }
    
    @Override
    public double[] getCartesian(final double scale) {
        double adjacent = Math.sqrt(3) / 2;
        double opposite = 0.5;
        double x = scale * this.v * adjacent;
        double y = scale * (this.u + this.v * opposite);
        return new double[] {x, y};
    }
    
    @Override 
    public double[] getCartesian() {
        return this.getCartesian(1.0);
    }
    
    @Override
    public String toString() {
        return "UV("+this.u+","+this.v+")";
    }
    
    @Override
    public boolean equals(Object other) {
        try {
            return this.equals((CoordUV) other);
        } catch (Exception e) {
            return false;
        }
    }
    
    public boolean equals(CoordUV other) {
        if (other.getU() == this.u && other.getV() == this.v) {
            return true;
        }
        return false;
    }

    @Override
    /**
     * machine-generated
     */
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + this.u;
        hash = 71 * hash + this.v;
        return hash;
    }
}

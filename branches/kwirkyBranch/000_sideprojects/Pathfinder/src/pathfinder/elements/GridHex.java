/*
 * 
 */
package pathfinder.elements;

/**
 * One hex/cell of a GridBoard. Contains data on terrain, obstructions
 * (, units?) that occupy this hex.
 * TODO: unit location knowledge?
 * @author kwirkyj
 */
public class GridHex extends CoordHexY {
    private int height = 1; /** height of the terrain in this hex. */
    
    public GridHex() {
        super();
    }
    
    public GridHex(final int x, final int y) {
        super(x,y);
    }
    
    public GridHex(final CoordHexY c) {
        super(c);
    }
    
    public void setHeight(final int height) {
        this.height = height;
    }
    
    public int getHeight() {
        return this.height;
    }
}

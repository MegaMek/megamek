/*
 * 
 */
package pathfinder.elements;

import java.util.ArrayList;

/**
 * A rectangular Battletech-like board grid, 'origin' at (1,1).
 * @author kwirkyj
 */
public class GridBoard implements IBoard {
    private static final int DEFAULT_WIDTH = 16;
    private static final int DEFAULT_HEIGHT = 9;
    private int width, height;
    private ArrayList<GridHex> hexes;

    /**
     * Create a board with the default dimensions.
     */
    public GridBoard() {
        initializeBoard(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    /**
     * Create a board with the provided dimensions.
     * @param width Integer greater than or equal to zero.
     * @param height Integer greater than or equal to zero.
     * @throws IllegalArgumentException iff width or heigh parameters are 
     *         negative.
     */
    public GridBoard(final int width, final int height) 
            throws IllegalArgumentException {
        initializeBoard(width, height);
    }

    /**
     * Create a board with the provided coordinate as its extent.
     * @param corner CoordHexY, cannot have values less than 1.
     * @throws IllegalArgumentException iff corner coord has coordinates 
     *         less than 1.
     */
    public GridBoard(final ICoord corner) 
            throws IllegalArgumentException {
        int[] xy = corner.getCoords();
        initializeBoard(xy[0], xy[1]);
    }

    private void initializeBoard(final int w, final int h) 
            throws IllegalArgumentException{
        if (w < 1) {
            throw new IllegalArgumentException(
                    "width must be greater than zero");
        }
        if (h < 1) {
            throw new IllegalArgumentException(
                    "height must be greater than zero");
        }
        this.width = w;
        this.height = h;
        this.hexes = new ArrayList<>(w*h);
        // neat hack: i == (x-1*w) + (y-1)
        for (int x = 1; x <= w; x++) {
            for (int y = 1; y <= h; y++) {
                this.hexes.add(new GridHex(x, y));
            }
        }
    }

    @Override
    public boolean hasCoord(final ICoord c) {
        int[] xy = c.getCoords();
        if (xy[0] < 1)           return false;
        if (xy[1] < 1)           return false;
        if (xy[0] > this.width)  return false;
        if (xy[1] > this.height) return false;
        return true;
    }

    /**
     * Get the GridHex at the given coordinates.
     * @param c Coordinate point, probably GridHex or CoordHexY
     * @return null iff coordinate is not part of the board;
     *         else, reference to GridHex with same coordinates.
     */
    @Override
    public GridHex getCoord(final ICoord c) {
        if (!this.hasCoord(c)) return null;
        return this.hexes.get(indexOf(c));
    }

    /**
     * Set a coordinate with a provided GridHex.
     * @param h GridHex object to set.
     * @throws ArrayIndexOutOfBoundsException if provided hex is not 
     *         part of the board. 
     *         (Not wrapped in IllegalArgumentException for performance
     *         concerns, despite suggesting implementation details.)
     */
    public void setCoord(final GridHex h) {
        this.hexes.set(indexOf(h), h);
    }

    private int indexOf(final ICoord c) {
        int[] xy = c.getCoords();
        return (xy[0]-1) * this.width + (xy[1]-1);
    }

    /**
     * The the corners of the grid board.
     * @return Array of CoordHexY, upper-right (1,1) and the lower-left.
     */
    @Override
    public ICoord[] getExtents() {
        CoordHexY upper_right = new CoordHexY(); // default constructor (1,1)
        CoordHexY lower_left = new CoordHexY(this.width, this.height);
        return new ICoord[]{upper_right, lower_left};
    }
}


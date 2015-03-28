/*
 * 
 */
package pathfinder.elements;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
//import static org.junit.Assert.fail;
//import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 *
 * @author kwirkyj
 */
@RunWith(JUnit4.class)
public class GridBoardTest {
    
    @Test
    public void testConstructorNoArgs() {
        GridBoard b = new GridBoard();
        CoordHexY[] expected = new CoordHexY[] {
            new CoordHexY( 1,1),
            new CoordHexY(16,9)
        };
        assertArrayEquals(expected, b.getExtents());
    }
    
    @Test
    public void testConstructorDimensions() {
        GridBoard b = new GridBoard(8,12);
        CoordHexY[] expected = new CoordHexY[] {
            new CoordHexY(1, 1),
            new CoordHexY(8,12)
        };
        assertArrayEquals(expected, b.getExtents());
    }
    
    @Test
    public void testConstructorDimensionsOneOne() {
        // degenerate case, I guess?
        GridBoard b = new GridBoard(1,1);
        CoordHexY[] expected = new CoordHexY[] {
            new CoordHexY(1, 1),
            new CoordHexY(1, 1)
        };
        assertArrayEquals(expected, b.getExtents());
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testConstructorDimensionsNegative() {
        GridBoard b = new GridBoard(-1, 3);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testConstructorDimensionsZero() {
        GridBoard b = new GridBoard(20, 0);
    }
    
    @Test
    public void testConstructorICoord() {
        GridHex corner = new GridHex(8,4);
        GridBoard b = new GridBoard(corner);
        assertEquals(corner, b.getExtents()[1]);
        assertTrue("has (1,1)", b.hasCoord(new CoordHexY(1,1)));
        assertTrue("has (8,4)", b.hasCoord(new GridHex(2,3)));
        assertFalse("lacks (0,-1)", b.hasCoord(new CoordHexY(0,-1)));
        assertFalse("lacks (8,5)", b.hasCoord(new GridHex(5,5)));
        assertFalse("lacks (9,4)", b.hasCoord(new CoordHexY(5,5)));
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testConstructorICoordBad() {
        GridBoard b = new GridBoard(new GridHex(0,4));
    }
    
//    @Test
//    public void testConstructorGridBoard() {
//        fail("todo");
//    }

    @Test
    public void testGetCoordMissing() {
        GridBoard b = new GridBoard();
        assertNull("should lack (20,20)", b.getCoord(new CoordHexY(20,20)));
    }

    @Test
    public void testGetCoord() {
        GridBoard b = new GridBoard();
        CoordHexY c = new CoordHexY(3,5);
        GridHex h = b.getCoord(c);
        h.setHeight(3);
        b.setCoord(h);
        assertEquals(3, b.getCoord(c).getHeight());
    }
    
    @Test(expected=ArrayIndexOutOfBoundsException.class)
    public void testSetCoordOutOfBounds() {
        GridBoard b = new GridBoard();
        b.setCoord(new GridHex(-3,2));
    }
}

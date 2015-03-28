/*
 * 
 */
package pathfinder;

import java.util.ListIterator;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import pathfinder.elements.CoordHexY;
import pathfinder.elements.GridBoard;
import pathfinder.elements.IPath;

/**
 *
 * @author kwirkyj
 */
@RunWith(JUnit4.class)
public class ReallySimplePathfinderTest {
    private static GridBoard board;
    private static ReallySimplePathfinder finder;
    private CoordHexY start;
    private CoordHexY end;
    
    @BeforeClass
    public static void setUpClass() {
        board = new GridBoard(16,9);
        finder = new ReallySimplePathfinder();
    }
    
    @After
    public void tearDown() {
        this.start = null;
        this.end = null;
    }

    @Test
    public void testGetDegeneratePath() {
        start = new CoordHexY(3,4);
        end = new CoordHexY(3,4);
        IPath p = finder.getPath(board, start, end);
        assertEquals(0, p.getLength());
    }
    
    @Test
    public void testNextTo1() {
        start = new CoordHexY(3,4);
        end = new CoordHexY(3,3);
        IPath p = finder.getPath(board, start, end);
        assertEquals(1, p.getLength());
        ListIterator li = p.getListIterator();
        assertEquals("element should be endpoint", 
                     end, (CoordHexY) li.next());
    }
    
    @Test
    public void testNextTo2() {
        start = new CoordHexY(3,4);
        end = new CoordHexY(2,3);
        IPath p = finder.getPath(board, start, end);
        assertEquals(1, p.getLength());
        ListIterator li = p.getListIterator();
        assertEquals("element should be endpoint", 
                     end, (CoordHexY) li.next());
    }
    
    @Test
    public void testAboveByThree() {
        start = new CoordHexY(3,5);
        end = new CoordHexY(3,2);
        IPath p = finder.getPath(board, start, end);
        assertEquals(3, p.getLength());
        ListIterator li = p.getListIterator();
        assertEquals("first step = HexY(3,4)", 
                     new CoordHexY(3,4), (CoordHexY) li.next());
        assertEquals("second step = HexY(3,3)", 
                     new CoordHexY(3,3), (CoordHexY) li.next());
        assertEquals("third should be endpoint (3,2)", 
                     end, (CoordHexY) li.next());
    }
    
    @Test
    public void testDiagonal() {
        start = new CoordHexY(3,4);
        end = new CoordHexY(4,2);
        IPath p = finder.getPath(board, start, end);
        assertEquals(2, p.getLength());
        ListIterator li = p.getListIterator();
        assertEquals(new CoordHexY(4,3), (CoordHexY) li.next());
        assertEquals("third should be endpoint (4,2)", 
                     end, (CoordHexY) li.next());
    }
    
    @Test(expected=ImpossiblePathException.class)
    public void testOutsideBoundsStart() {
        start = new CoordHexY(0,3);
        end = new CoordHexY(3,2);
        IPath p = finder.getPath(board, start, end);
    }
    
    @Test(expected=ImpossiblePathException.class)
    public void testOutsideBoundsEnd() {
        start = new CoordHexY(4,3);
        end = new CoordHexY(3,20);
        IPath p = finder.getPath(board, start, end);
    }
}

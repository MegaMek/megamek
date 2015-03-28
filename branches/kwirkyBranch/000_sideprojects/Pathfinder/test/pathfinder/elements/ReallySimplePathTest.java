/*
 * 
 */
package pathfinder.elements;

import java.util.ListIterator;
import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
//import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 *
 * @author kwirkyj
 */
@RunWith(JUnit4.class)
public class ReallySimplePathTest {
    private static CoordHexY c0;
    private static CoordHexY c1;
    private static CoordHexY c2;
    private static CoordHexY c3;
    private static CoordHexY c4;
    private ReallySimplePath path;
    
    @BeforeClass
    public static void setUpClass() {
        c0 = new CoordHexY();
        c1 = new CoordHexY();
        c2 = new CoordHexY();
        c3 = new CoordHexY();
        c4 = new CoordHexY();
    }
    
    @Before
    public void setUp() {
        path = new ReallySimplePath();
    }
    
    @Test
    public void testAppendElements() {
        assertEquals("empty path should be empty", 0, path.getLength());
        assertEquals("empty path should be free", 0f, path.getCost(), 1e-8);
        path.append(c0);
        assertEquals("should have length == 1", 1, path.getLength());
        assertEquals("should cost one", 1f, path.getCost(), 1e-8);
        path.append(c1);
        path.append(c0);
        assertEquals("duplicates should contribute to length, too", 
                     3, path.getLength());
        assertEquals("should cost three", 3f, path.getCost(), 1e-8);
    }

    @Test
    public void testAppendPath() {
        ReallySimplePath other = new ReallySimplePath();
        other.append(c0);
        other.append(c1);
        path.append(c4);
        
        path.append(other);
        assertEquals(3, path.getLength());
        ListIterator li = path.getListIterator();
        li.next();
        li.next();
        CoordHexY c = (CoordHexY) li.next();
        assertEquals("last element should be c1", c1, c);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testAppendNull() {
        GridHex h = null;
        path.append(h);
    }
    
    @Test
    public void testAppendEmptyPath() {
        ReallySimplePath other = new ReallySimplePath();
        path.append(c2);
        path.append(c3);
        path.append(other);
        assertEquals("appending of empty path adds none", 2, path.getLength());
    }
}

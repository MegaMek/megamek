/*
 * 
 */
package pathfinder.elements;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
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
public class GridHexTest {

    @Test 
    public void testGetAdjoining1() {
        GridHex c = new GridHex(1,2);
        ICoord[] expected = new ICoord[] {
            new GridHex(1,1),
            new GridHex(2,1),
            new GridHex(2,2),
            new GridHex(1,3),
            new GridHex(0,2),
            new GridHex(0,1)};
        assertArrayEquals(expected, c.getAdjoining());
    }

    @Test
    public void testHeight() {
        GridHex h = new GridHex();
        assertEquals("default height of 1", 1, h.getHeight());
        h.setHeight(5);
        assertEquals("positive height 5", 5, h.getHeight());
        h.setHeight(-3);
        assertEquals("negative height -3", -3, h.getHeight());
    }
}

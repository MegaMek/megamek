/*
 * 
 */
package pathfinder.elements;

//import java.util.HashSet;
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
public class CoordUVTest {

    @Test
    public void testGetAdjoining() {
        CoordUV c = new CoordUV(4,-1);
        CoordUV[] expected = new CoordUV[] {
            new CoordUV(5,-1),
            new CoordUV(4, 0),
            new CoordUV(3, 0),
            new CoordUV(3,-1),
            new CoordUV(4,-2),
            new CoordUV(5,-2)};
        assertArrayEquals(expected, c.getAdjoining());
    }
    
    @Test
    public void testGetCoords() {
        CoordUV c = new CoordUV();
        int[] ex = {0,0};
        assertEquals(2, c.getCoords().length);
        assertArrayEquals(ex, c.getCoords());
        
        c = new CoordUV(-3, 6);
        ex[0] = -3;
        ex[1] = 6;
        assertArrayEquals(ex, c.getCoords());
    }
    
    @Test
    public void testGetCartesianScaled() {
        double[][] expecteds = {
            { 0.0,       0.0},
            { 1.7320508, 2.0},
            { 0.8660254,-1.5},
            {-0.8660254, 0.5}
        };
        int[][] coords = {
            { 0, 0},
            { 2, 4},
            {-4, 2},
            { 2,-2}
        };
        assert(coords.length == expecteds.length); // sanity-check
        for (int i = 0; i < coords.length; i++) {
            int u = coords[i][0];
            int v = coords[i][1];
            CoordUV c = new CoordUV(u, v);
            double[] expected = expecteds[i];
            double[] actual = c.getCartesian(0.5);
            assertArrayEquals(expected, actual, 0.0001);
        }
    }
    
    @Test
    public void testGetCartesian() {
        double[][] expecteds = {
            { 0.0,       0.0},
            { 1.7320508, 2.0},
            { 0.8660254,-1.5},
            {-0.8660254, 0.5}
        };
        int[][] coords = {
            { 0, 0},
            { 1, 2},
            {-2, 1},
            { 1,-1}
        };
        assert(coords.length == expecteds.length); // sanity-check
        for (int i = 0; i < coords.length; i++) {
            int u = coords[i][0];
            int v = coords[i][1];
            CoordUV c = new CoordUV(u, v);
            double[] expected = expecteds[i];
            double[] actual = c.getCartesian();
            assertArrayEquals(expected, actual, 0.0001);
        }
    }
    
    @Test
    public void testToString() {
        CoordUV c = new CoordUV();
        assertEquals("UV(0,0)", c.toString());
        c = new CoordUV(-3, 6);
        assertEquals("UV(-3,6)", c.toString());
    }
    
    @Test
    public void testFromUV() {
        CoordUV center = new CoordUV();
        CoordUV elsewhere = new CoordUV(3,-4);
        assertEquals(center, new CoordUV(center));
        assertEquals(elsewhere, new CoordUV(elsewhere));
    }
    
    @Test
    public void testFromHexY() {
        CoordHexY center = new CoordHexY();
        CoordHexY elsewhere = new CoordHexY(3, 4);
        CoordUV expectElse = new CoordUV(-4, 2);
        assertEquals(new CoordUV(), new CoordUV(center));
        assertEquals(expectElse, new CoordUV(elsewhere));
    }
}

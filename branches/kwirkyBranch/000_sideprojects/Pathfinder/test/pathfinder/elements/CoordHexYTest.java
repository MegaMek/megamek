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
public class CoordHexYTest {

    @Test 
    public void testGetAdjoining1() {
        CoordHexY c = new CoordHexY(1,2);
        ICoord[] expected = new ICoord[] {
            new CoordHexY(1,1),
            new CoordHexY(2,1),
            new CoordHexY(2,2),
            new CoordHexY(1,3),
            new CoordHexY(0,2),
            new CoordHexY(0,1)};
        assertArrayEquals(expected, c.getAdjoining());
    }

    @Test 
    public void testGetAdjoining2() {
        CoordHexY c = new CoordHexY(2,2);
        ICoord[] expected = new ICoord[] {
            new CoordHexY(2,1),
            new CoordHexY(3,2),
            new CoordHexY(3,3),
            new CoordHexY(2,3),
            new CoordHexY(1,3),
            new CoordHexY(1,2)};
        assertArrayEquals(expected, c.getAdjoining());
    }

    @Test
    public void testGetCoords() {
        int[] one_one = {1,1};
        int[] five_three = {5,3};
        CoordHexY nil = new CoordHexY();
        CoordHexY placed = new CoordHexY(5,3);
        assertArrayEquals(one_one, nil.getCoords());
        assertArrayEquals(five_three, placed.getCoords());
    }
    
    @Test
    public void testGetCartesianScaled() {
        double[][] expecteds = {
            { 0.0, 0.0},
            { 5.196152422706632,-3.0},
            { 1.732050807568877,-7.0},
            { 1.732050807568877, 3.0},
            {-3.4641016151377544,-2.0},
            {-3.4641016151377544, 2.0},
        };
        int[][] coords = {
            { 1, 1},
            { 4, 2},
            { 2, 4},
            { 2,-1},
            {-1, 2},
            {-1, 0}
        };
        assert(coords.length == expecteds.length); // sanity-check
        for (int i = 0; i < coords.length; i++) {
            int u = coords[i][0];
            int v = coords[i][1];
            CoordHexY c = new CoordHexY(u, v);
            double[] expected = expecteds[i];
            double[] actual = c.getCartesian(2.0);
            assertArrayEquals(expected, actual, 0.0001);
        }
    }
    
    @Test
    public void testGetCartesian() {
        double[][] expecteds = {
            { 0.0,      0.0},
            { 2.598076211353316,-1.5},
            { 0.866025403784439,-3.5},
            { 0.866025403784439, 1.5},
            {-1.732050807568877,-1.0},
            {-1.732050807568877, 1.0}
        };
        int[][] coords = {
            { 1, 1},
            { 4, 2},
            { 2, 4},
            { 2,-1},
            {-1, 2},
            {-1, 0}
        };
        assert(coords.length == expecteds.length); // sanity-check
        for (int i = 0; i < coords.length; i++) {
            int u = coords[i][0];
            int v = coords[i][1];
            CoordHexY c = new CoordHexY(u, v);
            double[] expected = expecteds[i];
            double[] actual = c.getCartesian();
            assertArrayEquals(expected, actual, 0.0001);
        }
    }
    
    @Test
    public void testFromHexY() {
        CoordHexY center = new CoordHexY();
        CoordHexY elsewhere = new CoordHexY(3,-4);
        assertEquals(center, new CoordHexY(center));
        assertEquals(elsewhere, new CoordHexY(elsewhere));
    }
    
    @Test
    public void testFromUV() {
        CoordUV center = new CoordUV();
        CoordUV elsewhere = new CoordUV(-5, 2);
        CoordHexY expectElse = new CoordHexY(3, 5);
        assertEquals(new CoordHexY(), new CoordHexY(center));
        assertEquals(expectElse, new CoordHexY(elsewhere));
    }
}

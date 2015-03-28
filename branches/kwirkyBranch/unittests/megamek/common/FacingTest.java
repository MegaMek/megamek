/*
 *
 */

package megamek.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** 
 *
 * @author KwirkyJ
 */
@RunWith(JUnit4.class)
public class FacingTest {
    private static Facing n, none, ne, nw, s;

    @BeforeClass
    public static void setUpClass() {
        n    = Facing.N;
        none = Facing.NONE;
        ne   = Facing.NE;
        nw   = Facing.NW;
        s    = Facing.S;
    }

    @Test
    public void testGetOppositeN() {
        assertEquals("opposite N not S", s, n.getOpposite());
    }

    @Test
    public void testGetOppositeNone() {
        assertEquals("opposite None not None", none, none.getOpposite());
    }

    @Test
    public void testGetNextClockwiseN() {
        assertEquals("clockwise of N not NE", ne, n.getNextClockwise());
    }

    @Test
    public void testGetNextClockwiseNONE() {
        assertEquals("clockwise of NONE not NONE", none, none.getNextClockwise());
    }

    @Test
    public void testGetNextCounterClockwiseN() {
        assertEquals("counterclockwise of N not NW", nw, n.getNextCounterClockwise());
    }

    @Test
    public void testGetNextCounterClockwiseNONE() {
        assertEquals("counterclockwise of NONE not NONE", none, none.getNextCounterClockwise());
    }

    @Test
    public void testIntValues() {
        assertEquals("intValue of NW not 5", 5, nw.getIntValue());
        assertEquals("valueOf 5 not NW", nw, Facing.valueOfInt(5));
        assertEquals("intValue of NONE not 6", 6, none.getIntValue());
    }

    @Test
//    @Ignore("ignore me")
    public void testValueOfIntWrapAround() {
        //TODO: is NONE never to be accessible via Facing.valueOfInt(n) ?
        assertEquals("valueOf 6 not N", n, Facing.valueOfInt(6));
    }
}


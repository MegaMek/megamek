/*
 * 
 */
package pathfinder.elements;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 *
 * @author kwirkyj
 */
@RunWith(JUnit4.class)
public class FacingTest {

    @Test
    public void testValues() {
        Facing[] expected = {Facing.N, Facing.NE, Facing.SE, Facing.S,
            Facing.SW, Facing.NW, Facing.NONE};
        assertArrayEquals("enum arrays should be equivalent", 
                expected, Facing.values());
    }

    @Test
    public void testValueOfName() {
        assertEquals("N->N", Facing.N, Facing.valueOf("N"));
        assertEquals("NW->NW", Facing.NW, Facing.valueOf("NW"));
        assertEquals("NONE->NONE", Facing.NONE, Facing.valueOf("NONE"));
    }

    @Test
    public void testValueOfInt() {
        assertEquals("0->N", Facing.N, Facing.valueOf(0));
        assertEquals("5->NW", Facing.NW, Facing.valueOf(5));
        assertEquals("6->NONE", Facing.NONE, Facing.valueOf(6));
        assertEquals("7->NONE", Facing.NONE, Facing.valueOf(7));
        assertEquals("-1->NONE", Facing.NONE, Facing.valueOf(-1));
    }
    
    @Test
    public void testNextClockwise() {
        assertEquals("NONE->NONE", Facing.NONE, Facing.NONE.nextClockwise());
        assertEquals("SE->S", Facing.S, Facing.SE.nextClockwise());
        assertEquals("NW->N", Facing.N, Facing.NW.nextClockwise());
    }
    
    @Test
    public void testNextCounterClockwise() {
        assertEquals("NONE->NONE", Facing.NONE, Facing.NONE.nextCounterClockwise());
        assertEquals("S->SE", Facing.SE, Facing.S.nextCounterClockwise());
        assertEquals("N->NW", Facing.NW, Facing.N.nextCounterClockwise());
    }
    
    @Test
    public void testOpposite() {
        assertEquals("NONE->NONE", Facing.NONE, Facing.NONE.opposite());
        assertEquals("SE->NW", Facing.NW, Facing.SE.opposite());
        assertEquals("N->S", Facing.S, Facing.N.opposite());
    }
    
    @Test
    public void testIntOf() {
        final Facing[] fs = Facing.values();
        for (int i = 0; i < fs.length; i++) {
            Facing f = fs[i];
            assertEquals("indices should match", i, Facing.intOf(f));
        }
    }
}

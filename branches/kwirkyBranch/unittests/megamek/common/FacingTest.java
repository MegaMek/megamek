/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
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


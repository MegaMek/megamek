/**
 * Tests for the class {@link BoardDimensions}
 * 
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License,
 * version 2, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, it is available online at:
 *   http://www.gnu.org/licenses/gpl-2.0.html 
 */
package megamek.common;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class BoardDimensionsTests {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public final void testInstantiation() {
        BoardDimensions b = new BoardDimensions(1, 1);
        assertEquals(1, b.width());
        assertEquals(1, b.height());
        assertEquals(1, b.numHexes());

        b = new BoardDimensions(Integer.MAX_VALUE, Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, b.width());
        assertEquals(Integer.MAX_VALUE, b.height());
        assertEquals((long) Integer.MAX_VALUE * Integer.MAX_VALUE, b.numHexes());

        b = new BoardDimensions(10, 10);
        assertEquals(10, b.width());
        assertEquals(10, b.height());
        assertEquals(100, b.numHexes());

        b = new BoardDimensions(100, 80);
        assertEquals(100, b.width());
        assertEquals(80, b.height());
        assertEquals(8000, b.numHexes());
    }

    @Test
    public final void testCopyConstructor() {
        BoardDimensions b = new BoardDimensions(20, 10);
        BoardDimensions c = new BoardDimensions(b);

        assertEquals(20, c.width());
        assertEquals(10, c.height());
    }

    @SuppressWarnings("unused")
    @Test(expected = IllegalArgumentException.class)
    public final void testCopyConstructorNullArgument() {
        BoardDimensions b = new BoardDimensions(null);
    }

    @SuppressWarnings("unused")
    @Test(expected = IllegalArgumentException.class)
    public final void testIllegalWidth() {
        BoardDimensions b = new BoardDimensions(0, 1);
    }

    @SuppressWarnings("unused")
    @Test(expected = IllegalArgumentException.class)
    public final void testIllegalHeight() {
        BoardDimensions b = new BoardDimensions(1, 0);
    }

    @Test
    public final void testEqualsObject() {
        BoardDimensions b = new BoardDimensions(10, 10);
        assertTrue(b.equals(b));

        Object x = new BoardDimensions(10, 10);
        assertTrue(b.equals(x));
        assertTrue(x.equals(b));

        BoardDimensions d = new BoardDimensions(10, 10);
        assertTrue(b.equals(d));
        assertTrue(d.equals(b));
    }

    @Test
    public final void testNotEqualsObject() {
        BoardDimensions b = new BoardDimensions(10, 10);
        assertFalse(b.equals("10x10"));
        assertFalse(b.equals(new BoardDimensions(10, 5)));
        assertFalse(b.equals(new BoardDimensions(5, 10)));
    }

    @Test
    public final void testClone() {
        BoardDimensions b = new BoardDimensions(10, 10);
        assertFalse(b.clone() == b);
        assertTrue(b.clone().equals(b));
    }

    @Test
    public final void testToString() {
        assertEquals("10x10", new BoardDimensions(10, 10).toString());
        assertEquals("80x50", new BoardDimensions(80, 50).toString());
    }

    @Test
    public final void testCompareTo() {
        assertEquals(0,
                new BoardDimensions(10, 10).compareTo(new BoardDimensions(10,
                        10)));
        assertEquals(0, new BoardDimensions(Integer.MAX_VALUE,
                Integer.MAX_VALUE).compareTo(new BoardDimensions(
                Integer.MAX_VALUE, Integer.MAX_VALUE)));
        assertEquals(-1,
                new BoardDimensions(10, 10).compareTo(new BoardDimensions(
                        Integer.MAX_VALUE, Integer.MAX_VALUE)));
        assertEquals(1, new BoardDimensions(Integer.MAX_VALUE,
                Integer.MAX_VALUE).compareTo(new BoardDimensions(10, 10)));
        assertEquals(-1,
                new BoardDimensions(10, 20).compareTo(new BoardDimensions(20,
                        10)));
        assertEquals(1,
                new BoardDimensions(20, 10).compareTo(new BoardDimensions(10,
                        20)));
    }
}

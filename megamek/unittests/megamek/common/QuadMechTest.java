/*
 * Copyright (C) 2019 - The MegaMek Team
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.common;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Test;

public class QuadMechTest {

    @Test
    public void cannotTwistWithoutExtendedTorsoTwistQuirk() {
        QuadMech quad = mock(QuadMech.class);
        when(quad.isProne()).thenReturn(false);
        when(quad.hasQuirk(anyString())).thenReturn(false);
        when(quad.getFacing()).thenReturn(2);
        when(quad.canChangeSecondaryFacing()).thenCallRealMethod();
        when(quad.isValidSecondaryFacing(anyInt())).thenCallRealMethod();
        
        assertTrue(quad.isValidSecondaryFacing(quad.getFacing()));
        assertFalse(quad.isValidSecondaryFacing(quad.getFacing() + 1));
        assertFalse(quad.isValidSecondaryFacing(quad.getFacing() + 2));
        assertFalse(quad.isValidSecondaryFacing(quad.getFacing() + 3));
        assertFalse(quad.isValidSecondaryFacing(quad.getFacing() - 1));
        assertFalse(quad.isValidSecondaryFacing(quad.getFacing() - 2));
        assertFalse(quad.isValidSecondaryFacing(quad.getFacing() - 3));
    }

    @Test
    public void extendedTorsoTwistQuirkAllowsTwist() {
        QuadMech quad = mock(QuadMech.class);
        when(quad.isProne()).thenReturn(false);
        when(quad.hasQuirk(anyString())).thenReturn(true);
        when(quad.getFacing()).thenReturn(2);
        when(quad.canChangeSecondaryFacing()).thenCallRealMethod();
        when(quad.isValidSecondaryFacing(anyInt())).thenCallRealMethod();
        
        assertTrue(quad.isValidSecondaryFacing(quad.getFacing()));
        assertTrue(quad.isValidSecondaryFacing(quad.getFacing() + 1));
        assertFalse(quad.isValidSecondaryFacing(quad.getFacing() + 2));
        assertFalse(quad.isValidSecondaryFacing(quad.getFacing() + 3));
        assertTrue(quad.isValidSecondaryFacing(quad.getFacing() - 1));
        assertFalse(quad.isValidSecondaryFacing(quad.getFacing() - 2));
        assertFalse(quad.isValidSecondaryFacing(quad.getFacing() - 3));
    }

    @Test
    public void cannotTwistWhileProneEvenWithQuirk() {
        QuadMech quad = mock(QuadMech.class);
        when(quad.isProne()).thenReturn(true);
        when(quad.hasQuirk(anyString())).thenReturn(true);
        when(quad.getFacing()).thenReturn(2);
        when(quad.canChangeSecondaryFacing()).thenCallRealMethod();
        when(quad.isValidSecondaryFacing(anyInt())).thenCallRealMethod();
        
        assertTrue(quad.isValidSecondaryFacing(quad.getFacing()));
        assertFalse(quad.isValidSecondaryFacing(quad.getFacing() + 1));
        assertFalse(quad.isValidSecondaryFacing(quad.getFacing() + 2));
        assertFalse(quad.isValidSecondaryFacing(quad.getFacing() + 3));
        assertFalse(quad.isValidSecondaryFacing(quad.getFacing() - 1));
        assertFalse(quad.isValidSecondaryFacing(quad.getFacing() - 2));
        assertFalse(quad.isValidSecondaryFacing(quad.getFacing() - 3));
    }
    
    @Test
    public void testClipSecondaryFacingWithoutTwist() {
        QuadMech quad = mock(QuadMech.class);
        when(quad.isProne()).thenReturn(false);
        when(quad.hasQuirk(anyString())).thenReturn(false);
        when(quad.getFacing()).thenReturn(2);
        when(quad.canChangeSecondaryFacing()).thenCallRealMethod();
        when(quad.isValidSecondaryFacing(anyInt())).thenCallRealMethod();
        when(quad.clipSecondaryFacing(anyInt())).thenCallRealMethod();
        
        assertEquals(quad.clipSecondaryFacing(quad.getFacing()), quad.getFacing());
        assertEquals(quad.clipSecondaryFacing(quad.getFacing() + 1), quad.getFacing());
        assertEquals(quad.clipSecondaryFacing(quad.getFacing() + 2), quad.getFacing());
        assertEquals(quad.clipSecondaryFacing(quad.getFacing() + 3), quad.getFacing());
        assertEquals(quad.clipSecondaryFacing(quad.getFacing() - 1), quad.getFacing());
        assertEquals(quad.clipSecondaryFacing(quad.getFacing() - 2), quad.getFacing());
        assertEquals(quad.clipSecondaryFacing(quad.getFacing() - 3), quad.getFacing());
    }
    
    @Test
    public void testClipSecondaryFacingWithExtendedTorsoTwist() {
        QuadMech quad = mock(QuadMech.class);
        when(quad.isProne()).thenReturn(false);
        when(quad.hasQuirk(anyString())).thenReturn(true);
        when(quad.getFacing()).thenReturn(2);
        when(quad.canChangeSecondaryFacing()).thenCallRealMethod();
        when(quad.isValidSecondaryFacing(anyInt())).thenCallRealMethod();
        when(quad.clipSecondaryFacing(anyInt())).thenCallRealMethod();
        
        assertEquals(quad.clipSecondaryFacing(quad.getFacing()), quad.getFacing());
        assertEquals(quad.clipSecondaryFacing(quad.getFacing() + 1), quad.getFacing() + 1);
        assertEquals(quad.clipSecondaryFacing(quad.getFacing() + 2), quad.getFacing() + 1);
        assertEquals(Math.abs(quad.getFacing() - quad.clipSecondaryFacing(quad.getFacing() + 3)), 1);
        assertEquals(quad.clipSecondaryFacing(quad.getFacing() - 1), quad.getFacing() - 1);
        assertEquals(quad.clipSecondaryFacing(quad.getFacing() - 2), quad.getFacing() - 1);
        assertEquals(Math.abs(quad.getFacing() - quad.clipSecondaryFacing(quad.getFacing() - 3)), 1);
    }
}

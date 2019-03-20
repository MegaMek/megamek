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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

public class QuadVeeTest {

    @Test
    public void canTwistAllDirectionsInVeeMode() {
        QuadVee qv = mock(QuadVee.class);
        when(qv.isProne()).thenReturn(false);
        when(qv.getFacing()).thenReturn(2);
        when(qv.getConversionMode()).thenReturn(QuadVee.CONV_MODE_VEHICLE);
        when(qv.canChangeSecondaryFacing()).thenCallRealMethod();
        when(qv.isValidSecondaryFacing(anyInt())).thenCallRealMethod();
        
        assertTrue(qv.isValidSecondaryFacing(qv.getFacing()));
        assertTrue(qv.isValidSecondaryFacing(qv.getFacing() + 1));
        assertTrue(qv.isValidSecondaryFacing(qv.getFacing() + 2));
        assertTrue(qv.isValidSecondaryFacing(qv.getFacing() + 3));
        assertTrue(qv.isValidSecondaryFacing(qv.getFacing() - 1));
        assertTrue(qv.isValidSecondaryFacing(qv.getFacing() - 2));
        assertTrue(qv.isValidSecondaryFacing(qv.getFacing() - 3));
    }

    @Test
    public void canTwistAllDirectionsInMechMode() {
        QuadVee qv = mock(QuadVee.class);
        when(qv.isProne()).thenReturn(false);
        when(qv.getFacing()).thenReturn(2);
        when(qv.getGyroHits()).thenReturn(0);
        when(qv.getGyroType()).thenReturn(Mech.GYRO_STANDARD);
        when(qv.getConversionMode()).thenReturn(QuadVee.CONV_MODE_MECH);
        when(qv.canChangeSecondaryFacing()).thenCallRealMethod();
        when(qv.isValidSecondaryFacing(anyInt())).thenCallRealMethod();
        
        assertTrue(qv.isValidSecondaryFacing(qv.getFacing()));
        assertTrue(qv.isValidSecondaryFacing(qv.getFacing() + 1));
        assertTrue(qv.isValidSecondaryFacing(qv.getFacing() + 2));
        assertTrue(qv.isValidSecondaryFacing(qv.getFacing() + 3));
        assertTrue(qv.isValidSecondaryFacing(qv.getFacing() - 1));
        assertTrue(qv.isValidSecondaryFacing(qv.getFacing() - 2));
        assertTrue(qv.isValidSecondaryFacing(qv.getFacing() - 3));
    }

    @Test
    public void canTwistTwoSidesWithGyroHit() {
        QuadVee qv = mock(QuadVee.class);
        when(qv.isProne()).thenReturn(false);
        when(qv.getFacing()).thenReturn(2);
        when(qv.getGyroHits()).thenReturn(1);
        when(qv.getGyroType()).thenReturn(Mech.GYRO_STANDARD);
        when(qv.getConversionMode()).thenReturn(QuadVee.CONV_MODE_MECH);
        when(qv.canChangeSecondaryFacing()).thenCallRealMethod();
        when(qv.isValidSecondaryFacing(anyInt())).thenCallRealMethod();
        
        assertTrue(qv.isValidSecondaryFacing(qv.getFacing()));
        assertTrue(qv.isValidSecondaryFacing(qv.getFacing() + 1));
        assertTrue(qv.isValidSecondaryFacing(qv.getFacing() + 2));
        assertFalse(qv.isValidSecondaryFacing(qv.getFacing() + 3));
        assertTrue(qv.isValidSecondaryFacing(qv.getFacing() - 1));
        assertTrue(qv.isValidSecondaryFacing(qv.getFacing() - 2));
        assertFalse(qv.isValidSecondaryFacing(qv.getFacing() - 3));
    }

    @Test
    public void cannotTwistWithDestroyedGyro() {
        QuadVee qv = mock(QuadVee.class);
        when(qv.isProne()).thenReturn(false);
        when(qv.getFacing()).thenReturn(2);
        when(qv.getGyroHits()).thenReturn(2);
        when(qv.getGyroType()).thenReturn(Mech.GYRO_STANDARD);
        when(qv.getConversionMode()).thenReturn(QuadVee.CONV_MODE_MECH);
        when(qv.canChangeSecondaryFacing()).thenCallRealMethod();
        when(qv.isValidSecondaryFacing(anyInt())).thenCallRealMethod();
        
        assertTrue(qv.isValidSecondaryFacing(qv.getFacing()));
        assertTrue(qv.isValidSecondaryFacing(qv.getFacing() + 1));
        assertFalse(qv.isValidSecondaryFacing(qv.getFacing() + 2));
        assertFalse(qv.isValidSecondaryFacing(qv.getFacing() + 3));
        assertTrue(qv.isValidSecondaryFacing(qv.getFacing() - 1));
        assertFalse(qv.isValidSecondaryFacing(qv.getFacing() - 2));
        assertFalse(qv.isValidSecondaryFacing(qv.getFacing() - 3));
    }
    
    @Test
    public void testClipSecondaryFacingWithUndamagedGyro() {
        QuadVee qv = mock(QuadVee.class);
        when(qv.isProne()).thenReturn(false);
        when(qv.getFacing()).thenReturn(2);
        when(qv.getGyroHits()).thenReturn(0);
        when(qv.getGyroType()).thenReturn(Mech.GYRO_STANDARD);
        when(qv.getConversionMode()).thenReturn(QuadVee.CONV_MODE_MECH);
        when(qv.canChangeSecondaryFacing()).thenCallRealMethod();
        when(qv.isValidSecondaryFacing(anyInt())).thenCallRealMethod();
        when(qv.clipSecondaryFacing(anyInt())).thenCallRealMethod();
        
        assertEquals(qv.clipSecondaryFacing(qv.getFacing()), qv.getFacing());
        assertEquals(qv.clipSecondaryFacing(qv.getFacing() + 1), qv.getFacing() + 1);
        assertEquals(qv.clipSecondaryFacing(qv.getFacing() + 2), qv.getFacing() + 2);
        assertEquals(Math.abs(qv.getFacing() - qv.clipSecondaryFacing(qv.getFacing() + 3)), 3);
        assertEquals(qv.clipSecondaryFacing(qv.getFacing() - 1), qv.getFacing() - 1);
        assertEquals(qv.clipSecondaryFacing(qv.getFacing() - 2), qv.getFacing() - 2);
        assertEquals(Math.abs(qv.getFacing() - qv.clipSecondaryFacing(qv.getFacing() - 3)), 3);
    }
    
    @Test
    public void testClipSecondaryFacingWithOneGyroHit() {
        QuadVee qv = mock(QuadVee.class);
        when(qv.isProne()).thenReturn(false);
        when(qv.getFacing()).thenReturn(2);
        when(qv.getGyroHits()).thenReturn(1);
        when(qv.getGyroType()).thenReturn(Mech.GYRO_STANDARD);
        when(qv.getConversionMode()).thenReturn(QuadVee.CONV_MODE_MECH);
        when(qv.canChangeSecondaryFacing()).thenCallRealMethod();
        when(qv.isValidSecondaryFacing(anyInt())).thenCallRealMethod();
        when(qv.clipSecondaryFacing(anyInt())).thenCallRealMethod();
        
        assertEquals(qv.clipSecondaryFacing(qv.getFacing()), qv.getFacing());
        assertEquals(qv.clipSecondaryFacing(qv.getFacing() + 1), qv.getFacing() + 1);
        assertEquals(qv.clipSecondaryFacing(qv.getFacing() + 2), qv.getFacing() + 2);
        assertEquals(Math.abs(qv.getFacing() - qv.clipSecondaryFacing(qv.getFacing() + 3)), 2);
        assertEquals(qv.clipSecondaryFacing(qv.getFacing() - 1), qv.getFacing() - 1);
        assertEquals(qv.clipSecondaryFacing(qv.getFacing() - 2), qv.getFacing() - 2);
        assertEquals(Math.abs(qv.getFacing() - qv.clipSecondaryFacing(qv.getFacing() - 3)), 2);
    }
    
    @Test
    public void testClipSecondaryFacingWithDestroyedGyro() {
        QuadVee qv = mock(QuadVee.class);
        when(qv.isProne()).thenReturn(false);
        when(qv.getFacing()).thenReturn(2);
        when(qv.getGyroHits()).thenReturn(2);
        when(qv.getGyroType()).thenReturn(Mech.GYRO_STANDARD);
        when(qv.getConversionMode()).thenReturn(QuadVee.CONV_MODE_MECH);
        when(qv.canChangeSecondaryFacing()).thenCallRealMethod();
        when(qv.isValidSecondaryFacing(anyInt())).thenCallRealMethod();
        when(qv.clipSecondaryFacing(anyInt())).thenCallRealMethod();
        
        assertEquals(qv.clipSecondaryFacing(qv.getFacing()), qv.getFacing());
        assertEquals(qv.clipSecondaryFacing(qv.getFacing() + 1), qv.getFacing() + 1);
        assertEquals(qv.clipSecondaryFacing(qv.getFacing() + 2), qv.getFacing() + 1);
        assertEquals(Math.abs(qv.getFacing() - qv.clipSecondaryFacing(qv.getFacing() + 3)), 1);
        assertEquals(qv.clipSecondaryFacing(qv.getFacing() - 1), qv.getFacing() - 1);
        assertEquals(qv.clipSecondaryFacing(qv.getFacing() - 2), qv.getFacing() - 1);
        assertEquals(Math.abs(qv.getFacing() - qv.clipSecondaryFacing(qv.getFacing() - 3)), 1);
    }
}

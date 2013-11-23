/*
 * MegaMek - Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
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
package megamek.client.bot.princess;

import megamek.common.logging.LogLevel;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

/**
 * @author Deric Page (deric.page@nisc.coop) (ext 2335)
 * @version %Id%
 * @since 11/22/13 8:33 AM
 */
@RunWith(JUnit4.class)
public class PrincessTest {

    @Test
    public void testCalculateAdjustment() {
        Princess mockPrincess = Mockito.mock(Princess.class);
        Mockito.doNothing().when(mockPrincess).log(Mockito.any(Class.class), Mockito.anyString(),
                                                   Mockito.any(LogLevel.class), Mockito.anyString());
        Mockito.when(mockPrincess.calculateAdjustment(Mockito.anyString())).thenCallRealMethod();

        // Test a +3 adjustment.
        String ticks = "+++";
        int expected = 3;
        int actual = mockPrincess.calculateAdjustment(ticks);
        Assert.assertEquals(expected, actual);

        // Test a -2 adjustment.
        ticks = "--";
        expected = -2;
        actual = mockPrincess.calculateAdjustment(ticks);
        Assert.assertEquals(expected, actual);

        // Test an adjustment with some bad characters.
        ticks = "+4";
        expected = 1;
        actual = mockPrincess.calculateAdjustment(ticks);
        Assert.assertEquals(expected, actual);

        // Test an adjustment with nothing but bad characters.
        ticks = "5";
        expected = 0;
        actual = mockPrincess.calculateAdjustment(ticks);
        Assert.assertEquals(expected, actual);

        // Test an empty ticks argument.
        ticks = "";
        expected = 0;
        actual = mockPrincess.calculateAdjustment(ticks);
        Assert.assertEquals(expected, actual);

        // Test a null ticks argument.
        ticks = null;
        expected = 0;
        actual = mockPrincess.calculateAdjustment(ticks);
        Assert.assertEquals(expected, actual);
    }
}

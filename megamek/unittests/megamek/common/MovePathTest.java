/**
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
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

import java.util.Vector;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @version $Id$
 * @since 12/23/13 9:16 AM
 */
@RunWith(JUnit4.class)
public class MovePathTest {

    @Test
    public void testGetLastStep() {
        Game mockGame = Mockito.mock(Game.class);
        Entity mockMech = Mockito.mock(BipedMech.class);

        Vector<MoveStep> stepVector = new Vector<>();

        MoveStep mockStep1 = Mockito.mock(MoveStep.class);
        stepVector.add(mockStep1);

        MoveStep mockStep2 = Mockito.mock(MoveStep.class);
        stepVector.add(mockStep2);

        MoveStep mockStep3 = Mockito.mock(MoveStep.class);
        stepVector.add(mockStep3);

        MoveStep mockStep4 = Mockito.mock(MoveStep.class);
        stepVector.add(mockStep4);

        MovePath testPath = Mockito.spy(new MovePath(mockGame, mockMech));
        Mockito.doReturn(stepVector).when(testPath).getStepVector();

        Assert.assertEquals(mockStep4, testPath.getLastStep());

        stepVector.add(null);
        Assert.assertEquals(mockStep4, testPath.getLastStep());
    }
}

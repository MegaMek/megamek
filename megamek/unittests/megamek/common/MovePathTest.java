/*
 * Copyright (c) 2000-2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Vector;

import org.junit.jupiter.api.Test;

import megamek.common.planetaryconditions.PlanetaryConditions;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 12/23/13 9:16 AM
 */
class MovePathTest {

    @Test
    void testGetLastStep() {
        Game mockGame = mock(Game.class);
        PlanetaryConditions mockPC = new PlanetaryConditions();
        mockPC.setGravity(1.0f);
        when(mockGame.getPlanetaryConditions()).thenReturn(mockPC);

        Entity mockMek = mock(BipedMek.class);

        Vector<MoveStep> stepVector = new Vector<>();

        MoveStep mockStep1 = mock(MoveStep.class);
        stepVector.add(mockStep1);

        MoveStep mockStep2 = mock(MoveStep.class);
        stepVector.add(mockStep2);

        MoveStep mockStep3 = mock(MoveStep.class);
        stepVector.add(mockStep3);

        MoveStep mockStep4 = mock(MoveStep.class);
        stepVector.add(mockStep4);

        MovePath testPath = spy(new MovePath(mockGame, mockMek));
        doReturn(stepVector).when(testPath).getStepVector();

        assertEquals(mockStep4, testPath.getLastStep());

        stepVector.add(null);
        assertEquals(mockStep4, testPath.getLastStep());
    }
}

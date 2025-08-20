/*
 * Copyright (c) 2000-2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Vector;

import megamek.common.enums.MoveStepType;
import megamek.common.game.Game;
import megamek.common.moves.MovePath;
import megamek.common.moves.MoveStep;
import megamek.common.options.GameOptions;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import org.junit.jupiter.api.Test;

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

    @Test
    void testAddStep() {
        Game mockGame = mock(Game.class);
        GameOptions mockOptions = mock(GameOptions.class);
        PlanetaryConditions mockPC = new PlanetaryConditions();
        mockPC.setGravity(1.0f);

        when(mockGame.getPlanetaryConditions()).thenReturn(mockPC);
        when(mockGame.getOptions()).thenReturn(mockOptions);

        Entity mockMek = mock(BipedMek.class);

        MovePath movePath = new MovePath(mockGame, mockMek);
        try {
            for (MoveStepType stepType : MoveStepType.values()) {
                MovePath pathToTest = movePath.clone();
                pathToTest.addStep(stepType);
            }
        } catch (Exception e) {
            fail("Exception thrown while adding step: " + e.getMessage());
        }

    }
}

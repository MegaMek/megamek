/*
 * Copyright (c) 2000-2005 - Ben Mazur (bmazur@sev.org).
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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

import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 10/30/13 9:25 AM
 */
@RunWith(value = JUnit4.class)
public class CrewTest {
    @Test
    public void testGetBVSkillMultiplier() {
        Game mockGame = Mockito.mock(Game.class);
        GameOptions mockOptions = Mockito.mock(GameOptions.class);

        // Test a 5/6 pilot
        testGetBVSkillMultiplier(mockGame, mockOptions, false, 5, 6, 0.86);
        testGetBVSkillMultiplier(mockGame, mockOptions, true, 5, 6, 0.86);

        // Test a 5/6 pilot
        testGetBVSkillMultiplier(mockGame, mockOptions, false, 4, 5, 1.0);
        testGetBVSkillMultiplier(mockGame, mockOptions, true, 4, 5, 1.0);

        // Test a 3/4 pilot
        testGetBVSkillMultiplier(mockGame, mockOptions, false, 3, 4, 1.32);
        testGetBVSkillMultiplier(mockGame, mockOptions, true, 3, 4, 1.32);

        // Test a 2/6 pilot
        testGetBVSkillMultiplier(mockGame, mockOptions, false, 2, 6, 1.35);
        testGetBVSkillMultiplier(mockGame, mockOptions, true, 2, 6, 1.33);

        // Test a 0/0 pilot
        testGetBVSkillMultiplier(mockGame, mockOptions, false, 0, 0, 2.42);
        testGetBVSkillMultiplier(mockGame, mockOptions, true, 0, 0, 2.7);
    }

    private void testGetBVSkillMultiplier(final Game mockGame, final GameOptions mockOptions, final boolean useAlternatePilotBVMod, final int gunnery, final int piloting, final double expected) {
        Mockito.when(mockOptions.booleanOption(Mockito.eq(OptionsConstants.ADVANCED_ALTERNATE_PILOT_BV_MOD)))
                .thenReturn(useAlternatePilotBVMod);
        Assert.assertEquals(expected, Crew.getBVSkillMultiplier(gunnery, piloting, mockGame), 0.001);
    }
}

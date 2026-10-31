/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.common.compute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.options.OptionsConstants;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for the to-hit odds shown in the weapon panel (issue #9332). A target number of 3 is 97.2% on 2d6, and 99.54%
 * for a gunner with Natural Aptitude, who rolls 3d6 and keeps the best two. The display used to round that 99.54% up to
 * 100%, so a gunner who could still miss looked certain to hit. It now rounds down to one decimal place.
 */
class HitOddsDisplayTest {

    private static final double EXACT = 0.0;

    @Test
    @DisplayName("A target number of 3 shows 97.2% on 2d6")
    void targetNumberThreeOnTwoDice() {
        assertEquals(97.2, Compute.oddsAsDisplayPercent(Compute.oddsAbove(3, false)), EXACT);
    }

    @Test
    @DisplayName("A target number of 3 with Natural Aptitude shows 99.5%, never 100%")
    void targetNumberThreeWithNaturalAptitudeIsNotCertain() {
        assertEquals(99.5, Compute.oddsAsDisplayPercent(Compute.oddsAbove(3, true)), EXACT);
    }

    @Test
    @DisplayName("A roll that cannot fail still shows 100%")
    void certainRollShowsOneHundred() {
        assertEquals(100.0, Compute.oddsAsDisplayPercent(Compute.oddsAbove(2, false)), EXACT);
        assertEquals(100.0, Compute.oddsAsDisplayPercent(Compute.oddsAbove(2, true)), EXACT);
    }

    @Test
    @DisplayName("Every 2d6 and Natural Aptitude odds value rounds down to one decimal place")
    void everyOddsValueRoundsDown() {
        double[] twoDice = { 97.2, 91.6, 83.3, 72.2, 58.3, 41.6, 27.7, 16.6, 8.3, 2.7 };
        double[] naturalAptitude = { 99.5, 98.1, 94.9, 89.3, 80.5, 68.0, 52.3, 35.6, 19.9, 7.4 };
        for (int targetNumber = 3; targetNumber <= 12; targetNumber++) {
            assertEquals(twoDice[targetNumber - 3],
                  Compute.oddsAsDisplayPercent(Compute.oddsAbove(targetNumber, false)), EXACT,
                  "2d6, target number " + targetNumber);
            assertEquals(naturalAptitude[targetNumber - 3],
                  Compute.oddsAsDisplayPercent(Compute.oddsAbove(targetNumber, true)), EXACT,
                  "Natural Aptitude, target number " + targetNumber);
        }
        assertEquals(0.0, Compute.oddsAsDisplayPercent(Compute.oddsAbove(13, false)), EXACT);
    }

    @Test
    @DisplayName("Gunnery specialization alone does not switch the roll to 3d6")
    void specializationDoesNotGrantNaturalAptitude() {
        Crew crew = new Crew(CrewType.SINGLE);
        crew.getOptions().getOption(OptionsConstants.GUNNERY_SPECIALIST).setValue(Crew.SPECIAL_ENERGY);

        assertFalse(crew.isUseNaturalAptitudeGunnery());
    }

    @Test
    @DisplayName("Natural Aptitude in Gunnery switches the roll to 3d6, with or without specialization")
    void naturalAptitudeSwitchesToThreeDice() {
        Crew crew = new Crew(CrewType.SINGLE);
        crew.getOptions().getOption(OptionsConstants.GUNNERY_SPECIALIST).setValue(Crew.SPECIAL_ENERGY);
        crew.setHasNaturalAptitudeGunnery(true, 0);

        assertTrue(crew.isUseNaturalAptitudeGunnery());
    }
}

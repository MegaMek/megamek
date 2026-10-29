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
 * Tests for the to-hit odds shown in the weapon panel (issue #9332). A target number of 3 is 97% on 2d6, and 99.54% for
 * a gunner with Natural Aptitude, who rolls 3d6 and keeps the best two. The display used to round that 99.54% up to
 * 100%, so a gunner who could still miss looked certain to hit.
 */
class HitOddsDisplayTest {

    @Test
    @DisplayName("A target number of 3 shows 97% on 2d6")
    void targetNumberThreeOnTwoDice() {
        assertEquals(97, Compute.oddsAsDisplayPercent(Compute.oddsAbove(3, false)));
    }

    @Test
    @DisplayName("A target number of 3 with Natural Aptitude shows 99%, never 100%")
    void targetNumberThreeWithNaturalAptitudeIsNotCertain() {
        assertEquals(99, Compute.oddsAsDisplayPercent(Compute.oddsAbove(3, true)));
    }

    @Test
    @DisplayName("A roll that cannot fail still shows 100%")
    void certainRollShowsOneHundred() {
        assertEquals(100, Compute.oddsAsDisplayPercent(Compute.oddsAbove(2, false)));
        assertEquals(100, Compute.oddsAsDisplayPercent(Compute.oddsAbove(2, true)));
    }

    @Test
    @DisplayName("Ordinary odds round to the nearest whole percent")
    void ordinaryOddsRound() {
        assertEquals(42, Compute.oddsAsDisplayPercent(Compute.oddsAbove(8, false)));
        assertEquals(98, Compute.oddsAsDisplayPercent(Compute.oddsAbove(4, true)));
        assertEquals(3, Compute.oddsAsDisplayPercent(Compute.oddsAbove(12, false)));
        assertEquals(0, Compute.oddsAsDisplayPercent(Compute.oddsAbove(13, false)));
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

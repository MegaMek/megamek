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
package megamek.common.loaders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;

import megamek.common.equipment.EquipmentType;
import megamek.common.units.EntityWeightClass;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Regression test for <a href="https://github.com/MegaMek/megamek/issues/8788">#8788</a>: the unit cache classed every
 * Battle Armor squad by its squad transport weight (4-6 tons) instead of its suit weight, so nearly all of them landed
 * in Assault. The summary must carry the weight class of the suit.
 */
class BattleArmorSummaryWeightClassTest {

    private static final String UNIT_DIR = "testresources/megamek/common/units/";

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @Test
    void powerArmorLightSquadIsUltraLight() {
        assertEquals(EntityWeightClass.WEIGHT_ULTRA_LIGHT, weightClassOf("Aerie PA(L) (Sqd4).blk"));
    }

    @Test
    void mediumSuitSquadIsMedium() {
        assertEquals(EntityWeightClass.WEIGHT_MEDIUM, weightClassOf("Elemental BA [Laser] (Sqd5).blk"));
    }

    @Test
    void heavySuitSquadIsHeavy() {
        assertEquals(EntityWeightClass.WEIGHT_HEAVY, weightClassOf("Black Wolf BA (ER Pulse) (Sqd5).blk"));
    }

    @Test
    void mekStillUsesItsTonnage() {
        assertEquals(EntityWeightClass.WEIGHT_ASSAULT, weightClassOf("Atlas AS7-D.mtf"));
    }

    private static int weightClassOf(String fileName) {
        MekSummary summary = MekSummaryCache.getSummaryFromFile(new File(UNIT_DIR + fileName));
        assertNotNull(summary, "Summary should be built for " + fileName);
        return summary.getWeightClass();
    }
}

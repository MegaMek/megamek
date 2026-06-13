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
 * MekWarrior, BattleMek, `Mek and AeroTek are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MekWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Mek;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Regression test for the Virtual Reality Piloting Pod (VRPP) BV multiplier.
 *
 * <p>Per Interstellar Operations: Alternate Eras p.183, a Mek equipped with a VRPP cockpit has its Battle Value
 * multiplied by 1.4 to reflect the MekWarrior's improved Piloting and Gunnery skills due to the system interface.
 * Pre-fix, {@code MekBVCalculator.processSummarize()} had no branch for {@link Mek#COCKPIT_VRRP}, so VRPP-equipped Meks
 * reported the unmodified base BV.</p>
 */
public class VrppCockpitBVTest {

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    private Game game;

    @BeforeEach
    void setUp() {
        game = new Game();
        game.addPlayer(0, new Player(0, "Test Player"));
    }

    private Mek createTestMek(int cockpitType, String modelLabel) {
        Mek mek = new BipedMek();
        mek.setGame(game);
        mek.setId(1);
        mek.setChassis("Test");
        mek.setModel(modelLabel);
        mek.setCrew(new Crew(CrewType.SINGLE));
        mek.setOwner(game.getPlayer(0));
        mek.setWeight(50.0);
        mek.setOriginalWalkMP(5);
        mek.setCockpitType(cockpitType);
        return mek;
    }

    @Test
    void vrppMultipliesBaseBVBy1_4() {
        Mek standard = createTestMek(Mek.COCKPIT_STANDARD, "Std");
        Mek vrpp = createTestMek(Mek.COCKPIT_VRRP, "VRPP");

        int standardBv = standard.calculateBattleValue(true, true);
        int vrppBv = vrpp.calculateBattleValue(true, true);

        assertTrue(standardBv > 0, "Standard cockpit BV should be positive (got " + standardBv + ")");
        // BV is rounded to int after the multiplier; allow tolerance of 1 BV either way.
        assertEquals(Math.round(standardBv * 1.4), vrppBv, 1,
              "VRPP cockpit BV should be 1.4x standard cockpit BV (IO:AE p.183). "
                    + "Standard: " + standardBv + ", VRPP: " + vrppBv);
    }
}

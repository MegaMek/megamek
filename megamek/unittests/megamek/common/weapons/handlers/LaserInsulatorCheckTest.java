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
package megamek.common.weapons.handlers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Vector;

import megamek.common.MMRandom;
import megamek.common.Report;
import megamek.common.compute.Compute;
import megamek.common.equipment.Engine;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.loaders.MekFileParser;
import megamek.common.units.BipedMek;
import megamek.common.units.Mek;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Every handler of a laser that can take an insulator makes the insulator check, so the check itself must leave a laser
 * without a working insulator alone (TO:AUE p.134).
 */
class LaserInsulatorCheckTest {

    /** Dice that always roll the middle: every d6 is a 4, so a 2D6 roll is 8. */
    private static final class MiddleDice extends MMRandom {
        @Override
        public int randomInt(int maxValue) {
            return maxValue / 2;
        }

        @Override
        public float randomFloat() {
            return 0.5f;
        }
    }

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void fixDice() {
        Compute.setRNG(new MiddleDice());
    }

    @AfterEach
    void restoreDice() {
        Compute.setRNG(MMRandom.R_DEFAULT);
    }

    private static Mounted<?> laserWith(String laser, String... enhancements) throws Exception {
        Mek mek = new BipedMek();
        mek.setWeight(50.0);
        mek.setEngine(new Engine(200, Engine.NORMAL_ENGINE, 0));
        Mounted<?> weapon = mek.addEquipment(EquipmentType.get(laser), Mek.LOC_RIGHT_TORSO);
        for (String enhancement : enhancements) {
            mek.addEquipment(EquipmentType.get(enhancement), Mek.LOC_RIGHT_TORSO);
        }
        MekFileParser.postLoadInit(mek);
        return weapon;
    }

    @Test
    void anInsulatedPulseLaserFailsOnARollOfTwo() throws Exception {
        Mounted<?> laser = laserWith("ISMediumPulseLaser", "ISLaserInsulator");
        InsulatedLaserWeaponHandler.checkInsulator(2, laser, 0, new Vector<Report>());
        assertTrue(laser.isHit());
    }

    @Test
    void aLaserWithoutAWorkingInsulatorIsLeftAlone() throws Exception {
        Mounted<?> bare = laserWith("ISMediumPulseLaser");
        InsulatedLaserWeaponHandler.checkInsulator(2, bare, 0, new Vector<Report>());
        assertFalse(bare.isHit());

        Mounted<?> broken = laserWith("ISMediumPulseLaser", "ISLaserInsulator");
        broken.getEnhancement(MiscType.F_LASER_INSULATOR).setDestroyed(true);
        InsulatedLaserWeaponHandler.checkInsulator(2, broken, 0, new Vector<Report>());
        assertFalse(broken.isHit());
    }

    @Test
    void onlyARollOfTwoChecksTheInsulator() throws Exception {
        Mounted<?> laser = laserWith("ISMediumPulseLaser", "ISLaserInsulator");
        InsulatedLaserWeaponHandler.checkInsulator(3, laser, 0, new Vector<Report>());
        assertFalse(laser.isHit());
    }
}

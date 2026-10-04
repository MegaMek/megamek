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
package megamek.common.verifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import megamek.common.TechConstants;
import megamek.common.equipment.Engine;
import megamek.common.equipment.EquipmentType;
import megamek.common.options.OptionsConstants;
import megamek.common.units.BipedMek;
import megamek.common.units.Mek;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Construction rules for the Virtual Reality Piloting Pod cockpit (IO:AE p.63): 3 tons, and no Cramped Cockpit or
 * Rumble Seat design quirk.
 */
class TestMekVrppCockpitTest {

    private static final String CRAMPED_COCKPIT_REJECTION = "Virtual Reality Piloting Pods may not use the Cramped "
          + "Cockpit quirk.";
    private static final String RUMBLE_SEAT_REJECTION = "Virtual Reality Piloting Pods may not use the Rumble Seat "
          + "quirk.";

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    private static Mek newMekWithCockpit(int cockpitType) {
        Mek mek = new BipedMek();
        mek.setWeight(60.0);
        mek.setEngine(new Engine(240, Engine.NORMAL_ENGINE, 0));
        mek.setTechLevel(TechConstants.T_IS_EXPERIMENTAL);
        mek.setStructureType(EquipmentType.T_STRUCTURE_STANDARD);
        mek.setCockpitType(cockpitType);
        return mek;
    }

    private static TestMek newTestMek(Mek mek) {
        EntityVerifier entityVerifier = EntityVerifier.getInstance(new File(
              "testresources/data/mekfiles/UnitVerifierOptions.xml"));
        return new TestMek(mek, entityVerifier.mekOption, null);
    }

    private static String getMekVerifierReport(Mek mek) {
        StringBuffer report = new StringBuffer();
        newTestMek(mek).correctEntity(report, mek.getTechLevel());
        return report.toString();
    }

    @Test
    void vrppCockpitWeighsThreeTons() {
        assertEquals(3.0, newTestMek(newMekWithCockpit(Mek.COCKPIT_VRRP)).getWeightCockpit());
        assertEquals(4.0, newTestMek(newMekWithCockpit(Mek.COCKPIT_TORSO_MOUNTED)).getWeightCockpit());
    }

    @Test
    void vrppCockpitSitsInTheCenterTorso() {
        TestMek testMek = newTestMek(newMekWithCockpit(Mek.COCKPIT_VRRP));
        assertTrue(testMek.isCockpitLocation(Mek.LOC_CENTER_TORSO));
        assertFalse(testMek.isCockpitLocation(Mek.LOC_HEAD));
    }

    @Test
    void vrppRejectsCrampedCockpitQuirk() {
        Mek mek = newMekWithCockpit(Mek.COCKPIT_VRRP);
        mek.getQuirks().getOption(OptionsConstants.QUIRK_NEG_CRAMPED_COCKPIT).setValue(true);
        assertTrue(getMekVerifierReport(mek).contains(CRAMPED_COCKPIT_REJECTION));
    }

    @Test
    void vrppRejectsRumbleSeatQuirk() {
        Mek mek = newMekWithCockpit(Mek.COCKPIT_VRRP);
        mek.getQuirks().getOption(OptionsConstants.QUIRK_POS_RUMBLE_SEAT).setValue(true);
        assertTrue(getMekVerifierReport(mek).contains(RUMBLE_SEAT_REJECTION));
    }

    @Test
    void vrppWithoutThoseQuirksIsNotRejectedForThem() {
        String report = getMekVerifierReport(newMekWithCockpit(Mek.COCKPIT_VRRP));
        assertFalse(report.contains(CRAMPED_COCKPIT_REJECTION), report);
        assertFalse(report.contains(RUMBLE_SEAT_REJECTION), report);
    }

    @Test
    void torsoMountedCockpitMayStillUseThoseQuirks() {
        Mek mek = newMekWithCockpit(Mek.COCKPIT_TORSO_MOUNTED);
        mek.getQuirks().getOption(OptionsConstants.QUIRK_NEG_CRAMPED_COCKPIT).setValue(true);
        mek.getQuirks().getOption(OptionsConstants.QUIRK_POS_RUMBLE_SEAT).setValue(true);
        String report = getMekVerifierReport(mek);
        assertFalse(report.contains(CRAMPED_COCKPIT_REJECTION), report);
        assertFalse(report.contains(RUMBLE_SEAT_REJECTION), report);
    }
}

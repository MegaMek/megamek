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

package megamek.common.options;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponType;
import megamek.common.exceptions.LocationFullException;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.QuadMek;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests the legality rules in {@link Quirks#isQuirkLegalFor(IOption, Entity)}, focused on the Overhead Arms quirk (BMM
 * p.85): its mutual exclusion with Low-Mounted Arms and its eligibility requirements.
 */
@DisplayName("Quirks legality")
class QuirksTest {

    private static WeaponType mediumLaserType;

    @BeforeAll
    static void setUpAll() {
        EquipmentType.initializeTypes();
        mediumLaserType = (WeaponType) EquipmentType.get("ISMediumLaser");
    }

    private static IOption quirkOption(Entity entity, String quirkName) {
        return entity.getQuirks().getOption(quirkName);
    }

    private static void setQuirk(Entity entity, String quirkName) {
        entity.getQuirks().getOption(quirkName).setValue(true);
    }

    private static BipedMek bipedWithArmLaser() throws LocationFullException {
        BipedMek mek = new BipedMek();
        mek.addEquipment(mediumLaserType, Mek.LOC_RIGHT_ARM);
        return mek;
    }

    @Nested
    @DisplayName("Overhead Arms / Low-Mounted Arms mutual exclusion")
    class MutualExclusion {

        @Test
        @DisplayName("Overhead Arms is illegal when the Mek already has Low-Mounted Arms")
        void overheadArmsRejectedWithLowArms() throws LocationFullException {
            BipedMek mek = bipedWithArmLaser();
            setQuirk(mek, OptionsConstants.QUIRK_NEG_LOW_ARMS);

            assertFalse(Quirks.isQuirkLegalFor(quirkOption(mek, OptionsConstants.QUIRK_POS_OVERHEAD_ARMS), mek),
                  "Overhead Arms must not be combinable with Low-Mounted Arms");
        }

        @Test
        @DisplayName("Low-Mounted Arms is illegal when the Mek already has Overhead Arms")
        void lowArmsRejectedWithOverheadArms() throws LocationFullException {
            BipedMek mek = bipedWithArmLaser();
            setQuirk(mek, OptionsConstants.QUIRK_POS_OVERHEAD_ARMS);

            assertFalse(Quirks.isQuirkLegalFor(quirkOption(mek, OptionsConstants.QUIRK_NEG_LOW_ARMS), mek),
                  "Low-Mounted Arms must not be combinable with Overhead Arms");
        }

        @Test
        @DisplayName("Overhead Arms is legal on an eligible Mek without Low-Mounted Arms")
        void overheadArmsLegalWithoutLowArms() throws LocationFullException {
            BipedMek mek = bipedWithArmLaser();

            assertTrue(Quirks.isQuirkLegalFor(quirkOption(mek, OptionsConstants.QUIRK_POS_OVERHEAD_ARMS), mek),
                  "Overhead Arms should be legal on a biped with an arm-mounted direct-fire weapon");
        }
    }

    @Nested
    @DisplayName("Overhead Arms eligibility")
    class Eligibility {

        @Test
        @DisplayName("Legal for a biped with an arm-mounted direct-fire weapon")
        void legalForBipedWithArmDirectFireWeapon() throws LocationFullException {
            BipedMek mek = bipedWithArmLaser();

            assertTrue(Quirks.isQuirkLegalFor(quirkOption(mek, OptionsConstants.QUIRK_POS_OVERHEAD_ARMS), mek));
        }

        @Test
        @DisplayName("Illegal for a non-Omni biped lacking any arm-mounted direct-fire weapon")
        void illegalForBipedWithoutArmDirectFireWeapon() throws LocationFullException {
            BipedMek mek = new BipedMek();
            // Direct-fire weapon, but mounted in the center torso rather than an arm.
            mek.addEquipment(mediumLaserType, Mek.LOC_CENTER_TORSO);

            assertFalse(Quirks.isQuirkLegalFor(quirkOption(mek, OptionsConstants.QUIRK_POS_OVERHEAD_ARMS), mek),
                  "A Mek with no direct-fire weapon in its arms cannot take Overhead Arms");
        }

        @Test
        @DisplayName("Legal for an OmniMek even without an arm-mounted direct-fire weapon")
        void legalForOmniWithoutArmWeapon() {
            BipedMek mek = new BipedMek();
            mek.setOmni(true);

            assertTrue(Quirks.isQuirkLegalFor(quirkOption(mek, OptionsConstants.QUIRK_POS_OVERHEAD_ARMS), mek),
                  "An OmniMek is exempt from the arm-weapon requirement");
        }

        @Test
        @DisplayName("Illegal for a quad Mek, which has no arms")
        void illegalForQuad() throws LocationFullException {
            QuadMek mek = new QuadMek();
            mek.addEquipment(mediumLaserType, Mek.LOC_RIGHT_ARM);

            assertFalse(Quirks.isQuirkLegalFor(quirkOption(mek, OptionsConstants.QUIRK_POS_OVERHEAD_ARMS), mek),
                  "A quad Mek has no arms and cannot take Overhead Arms");
        }
    }
}

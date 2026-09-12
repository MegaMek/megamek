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

package megamek.common.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.equipment.enums.MiscTypeFlag;
import megamek.common.exceptions.LocationFullException;
import megamek.common.units.BipedMek;
import megamek.common.units.Mek;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Verifies that missile fire control can be switched on and off, per BMM p.12 (Electronics), and that switching it
 * off actually withdraws its guidance.
 *
 * <p>The Apollo is the case that matters most in play. It removes the +1 to hit MRMs carry and gives -1 on the
 * cluster roll instead, so being able to switch it off is a real choice between accuracy and volume.</p>
 */
class FireControlToggleTest {

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    private static MiscType fireControl(String internalName) {
        EquipmentType equipmentType = EquipmentType.get(internalName);
        assertNotNull(equipmentType, internalName + " should exist");
        assertTrue(equipmentType instanceof MiscType, internalName + " should be MiscType");
        return (MiscType) equipmentType;
    }

    /**
     * Declares a mode switch and advances a round so it takes effect, which is what the End Phase does in play. A
     * fire control switch is not instant, so setting the mode alone only queues it.
     */
    private static void switchOffAndAdvanceRound(Mounted<?> fireControl) {
        fireControl.setMode(Mounted.MODE_OFF);
        fireControl.newRound(1);
    }

    /** Mounts a piece of fire control on a bare Mek so its mode can be switched. */
    private static MiscMounted mountedFireControl(String internalName) throws LocationFullException {
        Mek mek = new BipedMek();
        Mounted<?> mounted = mek.addEquipment(fireControl(internalName), Mek.LOC_LEFT_TORSO);
        assertTrue(mounted instanceof MiscMounted, internalName + " should mount as MiscMounted");
        return (MiscMounted) mounted;
    }

    @Nested
    @DisplayName("Modes are defined")
    class ModesAreDefined {

        @Test
        @DisplayName("Every missile fire control system offers On and Off")
        void everyFireControlSystemOffersOnAndOff() {
            String[] internalNames = { "ISArtemisIV", "CLArtemisIV", "CLArtemisV", "ISArtemisIVProto", "ISApollo" };

            for (String internalName : internalNames) {
                MiscType fireControl = fireControl(internalName);
                assertEquals(2, fireControl.getModesCount(),
                      internalName + " should offer exactly On and Off");
                assertEquals(Mounted.MODE_ON, fireControl.getMode(0).getName(),
                      internalName + " should start switched on");
                assertEquals(Mounted.MODE_OFF, fireControl.getMode(1).getName(),
                      internalName + " should offer Off");
            }
        }

        @Test
        @DisplayName("Switching takes effect in the End Phase, like other electronics")
        void switchingIsNotInstant() throws LocationFullException {
            MiscMounted apollo = mountedFireControl("ISApollo");

            assertFalse(apollo.canInstantSwitch(1),
                  "A fire control switch is declared now and takes effect in the End Phase, matching ECM suites");
        }

        @Test
        @DisplayName("A freshly mounted system is switched on")
        void aFreshlyMountedSystemIsOn() throws LocationFullException {
            MiscMounted apollo = mountedFireControl("ISApollo");

            assertFalse(apollo.isModeTurnedOff(), "Fire control starts switched on, as it always has");
            assertTrue(EquipmentActivation.isGuidanceActive(apollo, MiscType.F_APOLLO),
                  "An undamaged, switched-on Apollo guides the shot");
        }
    }

    @Nested
    @DisplayName("Switching off withdraws the guidance")
    class SwitchingOffWithdrawsGuidance {

        @Test
        @DisplayName("A switched-off Apollo no longer guides")
        void aSwitchedOffApolloNoLongerGuides() throws LocationFullException {
            MiscMounted apollo = mountedFireControl("ISApollo");

            assertTrue(EquipmentActivation.isGuidanceActive(apollo, MiscType.F_APOLLO),
                  "The Apollo guides while switched on");

            apollo.setMode(Mounted.MODE_OFF);

            assertFalse(apollo.isModeTurnedOff(),
                  "The switch is declared, not instant, so the Apollo still guides this round");
            assertTrue(apollo.isModeTurnedOffNextRound(),
                  "The switch should be queued for the End Phase");
            assertTrue(EquipmentActivation.isGuidanceActive(apollo, MiscType.F_APOLLO),
                  "Shots taken this round still get the Apollo bonus");

            apollo.newRound(1);

            assertTrue(apollo.isModeTurnedOff(), "The Apollo should now read as switched off");
            assertFalse(EquipmentActivation.isGuidanceActive(apollo, MiscType.F_APOLLO),
                  "A switched-off Apollo must not guide the shot; this is the whole point of RFE #7362");
        }

        @Test
        @DisplayName("Every Artemis flavour stops guiding when switched off")
        void everyArtemisStopsGuidingWhenSwitchedOff() throws LocationFullException {
            record Guidance(String internalName, MiscTypeFlag flag) { }

            Guidance[] systems = {
                  new Guidance("ISArtemisIV", MiscType.F_ARTEMIS),
                  new Guidance("CLArtemisIV", MiscType.F_ARTEMIS),
                  new Guidance("CLArtemisV", MiscType.F_ARTEMIS_V),
                  new Guidance("ISArtemisIVProto", MiscType.F_ARTEMIS_PROTO) };

            for (Guidance system : systems) {
                MiscMounted mounted = mountedFireControl(system.internalName());

                assertTrue(EquipmentActivation.isGuidanceActive(mounted, system.flag()),
                      system.internalName() + " should guide while switched on");

                switchOffAndAdvanceRound(mounted);

                assertFalse(EquipmentActivation.isGuidanceActive(mounted, system.flag()),
                      system.internalName() + " must stop guiding when switched off");
            }
        }

        @Test
        @DisplayName("A destroyed system does not guide whatever its mode says")
        void aDestroyedSystemDoesNotGuide() throws LocationFullException {
            MiscMounted apollo = mountedFireControl("ISApollo");
            apollo.setDestroyed(true);

            assertFalse(apollo.isModeTurnedOff(), "The mode is untouched");
            assertFalse(EquipmentActivation.isGuidanceActive(apollo, MiscType.F_APOLLO),
                  "Damage still overrides the mode, as it did before this change");
        }

        @Test
        @DisplayName("Guidance is not confused between systems")
        void guidanceIsNotConfusedBetweenSystems() throws LocationFullException {
            MiscMounted artemis = mountedFireControl("ISArtemisIV");

            assertTrue(EquipmentActivation.isGuidanceActive(artemis, MiscType.F_ARTEMIS));
            assertFalse(EquipmentActivation.isGuidanceActive(artemis, MiscType.F_APOLLO),
                  "An Artemis is not an Apollo");
            assertFalse(EquipmentActivation.isGuidanceActive(artemis, MiscType.F_ARTEMIS_V),
                  "Artemis IV is not Artemis V");
        }

        @Test
        @DisplayName("Nothing linked means no guidance")
        void nothingLinkedMeansNoGuidance() {
            assertFalse(EquipmentActivation.isGuidanceActive(null, MiscType.F_APOLLO),
                  "A weapon with nothing linked to it has no fire control");
        }
    }

    @Nested
    @DisplayName("Unit level check")
    class UnitLevelCheck {

        @Test
        @DisplayName("A unit reports active guidance only while its system is on")
        void aUnitReportsActiveGuidanceOnlyWhileOn() throws LocationFullException {
            Mek mek = new BipedMek();
            Mounted<?> mounted = mek.addEquipment(fireControl("ISApollo"), Mek.LOC_LEFT_TORSO);

            assertTrue(EquipmentActivation.hasActiveGuidance(mek, MiscType.F_APOLLO),
                  "The unit mounts a working Apollo, so the MRM saturation attack is offered");

            switchOffAndAdvanceRound(mounted);

            assertFalse(EquipmentActivation.hasActiveGuidance(mek, MiscType.F_APOLLO),
                  "With the Apollo off the unit should not be offered the saturation attack");
        }

        @Test
        @DisplayName("A unit with no fire control reports none")
        void aUnitWithNoFireControlReportsNone() {
            Mek mek = new BipedMek();

            assertFalse(EquipmentActivation.hasActiveGuidance(mek, MiscType.F_APOLLO));
            assertFalse(EquipmentActivation.hasActiveGuidance(mek, MiscType.F_ARTEMIS));
        }
    }

    @Nested
    @DisplayName("Equipment without an Off mode is unaffected")
    class EquipmentWithoutAnOffModeIsUnaffected {

        @Test
        @DisplayName("A system with no modes still guides")
        void aSystemWithNoModesStillGuides() throws LocationFullException {
            // The targeting computer defines Normal and Aimed shot, neither of them Off, so the switched-off test
            // must never suppress it. This guards the helper against reading an unrelated mode as "off".
            MiscType targetingComputer = fireControl("ISTargeting Computer");
            assertTrue(targetingComputer.getModesCount() > 0, "The targeting computer has modes");

            Mek mek = new BipedMek();
            Mounted<?> mounted = mek.addEquipment(targetingComputer, Mek.LOC_LEFT_TORSO);

            assertFalse(mounted.isModeTurnedOff(),
                  "A system whose modes do not include Off is never read as switched off");
        }
    }
}

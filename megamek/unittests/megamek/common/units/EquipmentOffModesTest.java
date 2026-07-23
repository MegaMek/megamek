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

package megamek.common.units;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.equipment.EquipmentActivation;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.Sensor;
import megamek.common.exceptions.LocationFullException;
import megamek.common.options.GameOptions;
import megamek.common.weapons.Weapon;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for the equipment activation/deactivation ("Off" mode) rules: a deactivated active probe senses
 * nothing, a deactivated ECM suite projects no field, deactivated C3 gear provides no network benefit but keeps its
 * network membership for automatic rejoin, deactivated heat sinks dissipate nothing (per individual mount, covering
 * all heat sink types), a deactivated Improved Heavy Laser is not explosive, and Gauss rifles can always be powered
 * down. Mode switches are declared at any time but only take effect in the End Phase, which the tests emulate by
 * calling {@link Mounted#newRound(int)} on the switched mount.
 */
class EquipmentOffModesTest {

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    /** Emulates the End Phase mode switch: pending equipment modes apply at the round rollover. */
    private static void applyPendingMode(Mounted<?> mounted) {
        mounted.newRound(1);
    }

    private static Mounted<?> addEquipment(Mek mek, String internalName, int location) throws LocationFullException {
        EquipmentType equipmentType = EquipmentType.get(internalName);
        assertNotNull(equipmentType, "Equipment type " + internalName + " must exist");
        return mek.addEquipment(equipmentType, location);
    }

    @Test
    void deactivatedProbeProvidesNoSensing() throws LocationFullException {
        BipedMek mek = new BipedMek();
        Mounted<?> probe = addEquipment(mek, Sensor.BAP, Mek.LOC_RIGHT_TORSO);

        assertTrue(mek.hasBAP(false), "A freshly mounted probe defaults to On");

        probe.setMode("Off");
        assertTrue(mek.hasBAP(false), "The switch is only declared - no effect before the End Phase");

        applyPendingMode(probe);
        assertFalse(mek.hasBAP(false), "A deactivated probe senses nothing");

        probe.setMode("On");
        applyPendingMode(probe);
        assertTrue(mek.hasBAP(false), "Reactivating the probe restores its function");
    }

    @Test
    void deactivatedEcmProjectsNoField() throws LocationFullException {
        BipedMek mek = new BipedMek();
        Mounted<?> ecm = addEquipment(mek, "ISGuardianECMSuite", Mek.LOC_RIGHT_TORSO);

        assertTrue(mek.hasActiveECM(), "A freshly mounted Guardian ECM defaults to projecting ECM");

        ecm.setMode("Off");
        applyPendingMode(ecm);
        assertFalse(mek.hasActiveECM(), "A deactivated ECM suite projects no field");

        ecm.setMode("ECM");
        applyPendingMode(ecm);
        assertTrue(mek.hasActiveECM(), "Reactivating the suite restores the field");
    }

    @Test
    void deactivatedC3KeepsNetworkMembershipForRejoin() throws LocationFullException {
        BipedMek mek = new BipedMek();
        Mounted<?> c3i = addEquipment(mek, "ISC3iUnit", Mek.LOC_RIGHT_TORSO);
        mek.setC3NetId("C3i.42");

        assertFalse(EquipmentActivation.isC3SwitchedOff(mek), "A freshly mounted C3i defaults to On");

        c3i.setMode("Off");
        applyPendingMode(c3i);
        assertTrue(EquipmentActivation.isC3SwitchedOff(mek), "Switched-off C3 gear provides no network benefit");
        assertEquals("C3i.42", mek.getC3NetId(), "Network membership survives deactivation");
        assertTrue(mek.hasC3i(), "Capability stays presence-based so wiring/serialization keep the membership");

        c3i.setMode("On");
        applyPendingMode(c3i);
        assertFalse(EquipmentActivation.isC3SwitchedOff(mek), "Reactivated C3 gear rejoins its previous network");
        assertEquals("C3i.42", mek.getC3NetId(), "The previous network is restored on reactivation");
    }

    @Test
    void deactivatedNovaCewsStopsFunctionButStaysNetworkMember() throws LocationFullException {
        BipedMek mek = new BipedMek();
        Mounted<?> nova = addEquipment(mek, Sensor.NOVA, Mek.LOC_RIGHT_TORSO);

        assertTrue(mek.hasActiveNovaCEWS(), "A freshly mounted Nova CEWS defaults to functioning");
        assertFalse(EquipmentActivation.isC3SwitchedOff(mek));

        nova.setMode("Off");
        applyPendingMode(nova);
        assertFalse(mek.hasActiveNovaCEWS(), "A deactivated Nova CEWS does not function");
        assertFalse(mek.hasBAP(false), "The Off mode also silences the Nova's probe half");
        assertTrue(mek.hasNovaCEWS(), "Presence-based networking capability survives for membership purposes");
        assertTrue(EquipmentActivation.isC3SwitchedOff(mek), "The Off mode also cuts the Nova's C3 benefit");
    }

    @Test
    void deactivatedHeatSinksDissipateNothingPerMount() throws LocationFullException {
        BipedMek mek = new BipedMek();
        for (int sinkNumber = 0; sinkNumber < 10; sinkNumber++) {
            addEquipment(mek, EquipmentTypeLookup.SINGLE_HS, Entity.LOC_NONE);
        }
        assertEquals(10, mek.getHeatCapacity(false, false));
        assertEquals(10, mek.getActiveSinks());

        // switch off one specific sink
        Mounted<?> firstSink = mek.getMisc().get(0);
        firstSink.setMode("Off");
        assertEquals(10, mek.getHeatCapacity(false, false), "No effect before the End Phase");
        assertEquals(9, mek.getActiveSinksNextRound(), "The pending change is visible as next round's count");

        applyPendingMode(firstSink);
        assertEquals(9, mek.getHeatCapacity(false, false), "A deactivated heat sink dissipates nothing");
        assertEquals(9, mek.getActiveSinks());
    }

    @Test
    void bulkActiveSinkControlMapsOntoIndividualMounts() throws LocationFullException {
        BipedMek mek = new BipedMek();
        for (int sinkNumber = 0; sinkNumber < 10; sinkNumber++) {
            addEquipment(mek, EquipmentTypeLookup.SINGLE_HS, Entity.LOC_NONE);
        }

        mek.setActiveSinksNextRound(4);
        assertEquals(4, mek.getActiveSinksNextRound(), "The bulk control declares next round's active count");
        assertEquals(10, mek.getActiveSinks(), "No effect before the End Phase");

        for (Mounted<?> mounted : mek.getMisc()) {
            applyPendingMode(mounted);
        }
        assertEquals(4, mek.getActiveSinks(), "Six specific sinks are now switched off");
        assertEquals(4, mek.getHeatCapacity(false, false));

        mek.resetSinks();
        for (Mounted<?> mounted : mek.getMisc()) {
            applyPendingMode(mounted);
        }
        assertEquals(10, mek.getActiveSinks(), "resetSinks switches every sink back on");
    }

    @Test
    void prototypeDoubleHeatSinkIsSwitchable() throws LocationFullException {
        BipedMek mek = new BipedMek();
        Mounted<?> prototypeSink = addEquipment(mek, EquipmentTypeLookup.IS_DOUBLE_HS_PROTOTYPE, Entity.LOC_NONE);

        assertEquals(2, mek.getHeatCapacity(false, false), "A prototype double heat sink dissipates 2");

        prototypeSink.setMode("Off");
        applyPendingMode(prototypeSink);
        assertEquals(0, mek.getHeatCapacity(false, false),
              "Prototype double heat sinks are switchable like all other sink types");
    }

    @Test
    void deactivatedImprovedHeavyLaserIsNotExplosive() throws LocationFullException {
        BipedMek mek = new BipedMek();
        Mounted<?> laser = addEquipment(mek, "CLImprovedMediumHeavyLaser", Mek.LOC_RIGHT_TORSO);

        assertTrue(laser.getType().isExplosive(laser, false),
              "An activated Improved Heavy Laser explodes on a critical hit");

        laser.setMode("Off");
        applyPendingMode(laser);
        assertTrue(laser.isModeTurnedOff());
        assertFalse(laser.getType().isExplosive(laser, false),
              "A deactivated Improved Heavy Laser cannot explode");
    }

    @Test
    void gaussRiflesCanAlwaysBePoweredDown() {
        Weapon gaussRifle = (Weapon) EquipmentType.get("ISGaussRifle");
        assertNotNull(gaussRifle, "IS Gauss Rifle must exist");

        // Without any game option, adapting to default options must offer the power modes
        gaussRifle.adaptToGameOptions(new GameOptions());
        assertTrue(gaussRifle.hasModeType("Powered Down"),
              "Powering down a gauss rifle no longer requires the TacOps game option");
        assertTrue(gaussRifle.hasModeType("Powered Up"));
    }
}

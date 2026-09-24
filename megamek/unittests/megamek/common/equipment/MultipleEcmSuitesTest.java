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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import megamek.common.exceptions.LocationFullException;
import megamek.common.game.Game;
import megamek.common.options.GameOptions;
import megamek.common.units.BipedMek;
import megamek.common.units.Mek;
import megamek.server.ServerHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for the single-ECM-suite rule: a unit may use only one ECM suite at a time, of any type (TM p.213,
 * CO p.200). Covers which suites count as in use, which one the game keeps when it has to decide for itself, how the
 * suites are told apart when they read alike, and the server-side rejection of a second activation.
 *
 * <p>Reported as issue #8765, where the Mantis Light Attack VTOL (ECCM) deploys with both of its Guardian suites
 * active because every suite defaults to its first mode, which is {@code "ECM"}.</p>
 */
class MultipleEcmSuitesTest {

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    /**
     * Finds a mode by name rather than assuming an index. {@code EquipmentType} instances are shared across every
     * unit in the JVM, and {@code Entity.setGameOptions()} rewrites the mode list on the ECM types when the TacOps
     * ECCM or Ghost Target options are on - a test class that ran earlier can leave Guardian ECM carrying
     * {@code [ECM, ECCM, Off]} instead of {@code [ECM, Off]}, which moves every index after the first.
     */
    private static int modeIndex(MiscMounted mounted, String modeName) {
        for (int index = 0; index < mounted.getType().getModesCount(); index++) {
            if (mounted.getType().getMode(index).equals(modeName)) {
                return index;
            }
        }
        return fail("Equipment " + mounted.getName() + " must offer a " + modeName + " mode");
    }

    private static MiscMounted addEquipment(Mek mek, String internalName, int location) throws LocationFullException {
        EquipmentType equipmentType = EquipmentType.get(internalName);
        assertNotNull(equipmentType, "Equipment type " + internalName + " must exist");
        return (MiscMounted) mek.addEquipment(equipmentType, location);
    }

    /** A Mek carrying two Guardian ECM suites, the shape of the Mantis Light Attack VTOL (ECCM). */
    private static BipedMek mekWithTwoGuardianSuites() throws LocationFullException {
        BipedMek mek = new BipedMek();
        addEquipment(mek, "ISGuardianECMSuite", Mek.LOC_RIGHT_TORSO);
        addEquipment(mek, "ISGuardianECMSuite", Mek.LOC_LEFT_TORSO);
        return mek;
    }

    @Test
    void unitDeploysWithEverySuiteInUse() throws LocationFullException {
        BipedMek mek = mekWithTwoGuardianSuites();

        assertEquals(2, EquipmentActivation.ecmSuitesInUseNextRound(mek).size(),
              "Both suites default to ECM, which is the state issue #8765 reports");
    }

    @Test
    void switchedOffSuiteIsNotInUse() throws LocationFullException {
        BipedMek mek = mekWithTwoGuardianSuites();
        MiscMounted secondSuite = mek.getMisc().get(1);

        secondSuite.setMode(Mounted.MODE_OFF);

        List<MiscMounted> suitesInUse = EquipmentActivation.ecmSuitesInUseNextRound(mek);
        assertEquals(1, suitesInUse.size(),
              "The switch is declared now and applies in the End Phase, so it counts straight away");
        assertSame(mek.getMisc().get(0), suitesInUse.get(0), "The suite left alone is the one still in use");
    }

    @Test
    void inoperableSuiteIsNotInUse() throws LocationFullException {
        BipedMek mek = mekWithTwoGuardianSuites();

        mek.getMisc().get(1).setDestroyed(true);

        assertEquals(1, EquipmentActivation.ecmSuitesInUseNextRound(mek).size(),
              "A destroyed suite is not being used");
    }

    @Test
    void suiteInEccmModeStillCountsAsInUse() throws LocationFullException {
        BipedMek mek = mekWithTwoGuardianSuites();
        GameOptions options = mock(GameOptions.class);
        when(options.booleanOption(anyString())).thenReturn(false);
        when(options.booleanOption("tacops_eccm")).thenReturn(true);
        Game game = mock(Game.class);
        when(game.getOptions()).thenReturn(options);
        mek.setGame(game);
        mek.setGameOptions();

        MiscMounted secondSuite = mek.getMisc().get(1);
        assertTrue(secondSuite.setMode("ECCM") >= 0, "The ECCM option must add an ECCM mode");

        assertEquals(2, EquipmentActivation.ecmSuitesInUseNextRound(mek).size(),
              "A suite set to ECCM is being used just as much as one set to ECM (TM p.213)");
    }

    @Test
    void angelSuiteIsKeptOverAStandardSuite() throws LocationFullException {
        BipedMek mek = new BipedMek();
        addEquipment(mek, "ISGuardianECMSuite", Mek.LOC_RIGHT_TORSO);
        MiscMounted angelSuite = addEquipment(mek, "ISAngelECMSuite", Mek.LOC_LEFT_TORSO);

        MiscMounted keptSuite = EquipmentActivation.preferredEcmSuite(
              EquipmentActivation.ecmSuitesInUseNextRound(mek));

        assertSame(angelSuite, keptSuite, "Angel ECM outranks a standard suite, as ECCMComparator already has it");
    }

    @Test
    void firstSuiteIsKeptWhenBothAreTheSameType() throws LocationFullException {
        BipedMek mek = mekWithTwoGuardianSuites();

        MiscMounted keptSuite = EquipmentActivation.preferredEcmSuite(
              EquipmentActivation.ecmSuitesInUseNextRound(mek));

        assertSame(mek.getMisc().get(0), keptSuite, "With nothing to choose between them the first mount is kept");
    }

    @Test
    void identicalSuitesAreNumberedApart() throws LocationFullException {
        BipedMek mek = mekWithTwoGuardianSuites();

        String firstLabel = EquipmentActivation.ecmSuiteLabel(mek, mek.getMisc().get(0));
        String secondLabel = EquipmentActivation.ecmSuiteLabel(mek, mek.getMisc().get(1));

        assertTrue(firstLabel.contains("#1"), "Expected a suite number in " + firstLabel);
        assertTrue(secondLabel.contains("#2"), "Expected a suite number in " + secondLabel);
        assertNotEquals(firstLabel, secondLabel, "Two Guardian suites must not read alike");
    }

    @Test
    void loneSuiteIsNotNumbered() throws LocationFullException {
        BipedMek mek = new BipedMek();
        MiscMounted onlySuite = addEquipment(mek, "ISGuardianECMSuite", Mek.LOC_RIGHT_TORSO);

        assertFalse(EquipmentActivation.ecmSuiteLabel(mek, onlySuite).contains("#"),
              "A unit with one suite has nothing to tell apart");
    }

    @Test
    void activatingASecondSuiteIsRejected() throws LocationFullException {
        BipedMek mek = mekWithTwoGuardianSuites();
        MiscMounted secondSuite = mek.getMisc().get(1);
        secondSuite.setMode(Mounted.MODE_OFF);

        assertTrue(ServerHelper.isSecondEcmSuiteActivation(mek, secondSuite, modeIndex(secondSuite, MiscType.MODE_ECM)),
              "The first suite is already in use, so the second may not be switched on");
    }

    @Test
    void switchingASuiteOffIsAlwaysAllowed() throws LocationFullException {
        BipedMek mek = mekWithTwoGuardianSuites();

        MiscMounted secondSuite = mek.getMisc().get(1);

        assertFalse(ServerHelper.isSecondEcmSuiteActivation(mek, secondSuite, modeIndex(secondSuite, Mounted.MODE_OFF)),
              "Switching a suite off can never put a second one into use");
    }

    @Test
    void theOnlySuiteInUseMayChangeMode() throws LocationFullException {
        BipedMek mek = new BipedMek();
        MiscMounted onlySuite = addEquipment(mek, "ISGuardianECMSuite", Mek.LOC_RIGHT_TORSO);

        assertFalse(ServerHelper.isSecondEcmSuiteActivation(mek, onlySuite, modeIndex(onlySuite, MiscType.MODE_ECM)),
              "A unit with one suite is never in conflict with itself");
    }

    @Test
    void nonEcmEquipmentIsUnaffected() throws LocationFullException {
        BipedMek mek = mekWithTwoGuardianSuites();
        MiscMounted probe = addEquipment(mek, Sensor.BAP, Mek.LOC_CENTER_TORSO);

        assertFalse(ServerHelper.isSecondEcmSuiteActivation(mek, probe, 0),
              "The ECM rule says nothing about an active probe");
    }
}

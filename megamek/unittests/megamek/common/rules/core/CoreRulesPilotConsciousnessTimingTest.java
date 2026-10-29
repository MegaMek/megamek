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
package megamek.common.rules.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.Report;
import megamek.common.enums.GamePhase;
import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import megamek.common.units.LandAirMek;
import megamek.common.units.Mek;
import org.junit.jupiter.api.Test;

/**
 * Core Rules covers only Meks, so only a Mek's consciousness roll waits for the end of the phase. Every other unit,
 * including a LandAirMek in fighter mode, rolls the moment the pilot is hit, as in Total Warfare (issue #9352).
 */
class CoreRulesPilotConsciousnessTimingTest {

    /** Report 3902: "takes a pilot hit. Roll at end of phase to see if the pilot is conscious." */
    private static final int REPORT_ROLL_DEFERRED = 3902;

    /** Report 6030: the consciousness roll itself. */
    private static final int REPORT_CONSCIOUSNESS_ROLL = 6030;

    private final CoreRulesPilot coreRulesPilot = new CoreRulesPilot();

    @Test
    void aerospaceFighterPilotRollsImmediately() {
        assertRollsImmediately(new AeroSpaceFighter());
    }

    @Test
    void mekPilotRollWaitsForEndOfPhase() {
        assertRollIsDeferred(new BipedMek());
    }

    @Test
    void landAirMekInFighterModeRollsImmediately() {
        assertRollsImmediately(landAirMek(LandAirMek.CONV_MODE_FIGHTER));
    }

    @Test
    void landAirMekInAirMekModeRollWaitsForEndOfPhase() {
        assertRollIsDeferred(landAirMek(LandAirMek.CONV_MODE_AIR_MEK));
    }

    @Test
    void landAirMekInMekModeRollWaitsForEndOfPhase() {
        assertRollIsDeferred(landAirMek(LandAirMek.CONV_MODE_MEK));
    }

    private void assertRollsImmediately(Entity entity) {
        List<Integer> reportIds = hitPilotDuringFiring(entity);

        assertTrue(reportIds.contains(REPORT_CONSCIOUSNESS_ROLL),
              "The consciousness roll should be made at once, but the reports were " + reportIds);
        assertFalse(reportIds.contains(REPORT_ROLL_DEFERRED),
              "The report should not promise a roll at the end of the phase");
        assertFalse(entity.getCrew().hasPendingConRolls(), "No roll should be left waiting for the end of the phase");
    }

    private void assertRollIsDeferred(Entity entity) {
        List<Integer> reportIds = hitPilotDuringFiring(entity);

        assertTrue(reportIds.contains(REPORT_ROLL_DEFERRED),
              "The report should say the roll is made at the end of the phase, but the reports were " + reportIds);
        assertFalse(reportIds.contains(REPORT_CONSCIOUSNESS_ROLL), "The roll should not be made yet");
        assertTrue(entity.getCrew().hasPendingConRolls(), "The roll should be waiting for the end of the phase");
    }

    /** Gives the pilot one hit during the Firing phase and returns the ids of the reports produced. */
    private List<Integer> hitPilotDuringFiring(Entity entity) {
        entity.getCrew().setHits(1, 0);
        List<Report> reports = coreRulesPilot.pilotHits(entity, 1, 1, 0, false, GamePhase.FIRING);
        return reports.stream().map(report -> report.messageId).toList();
    }

    private static LandAirMek landAirMek(int conversionMode) {
        LandAirMek landAirMek = new LandAirMek(Mek.GYRO_STANDARD, Mek.COCKPIT_STANDARD, LandAirMek.LAM_STANDARD);
        landAirMek.setConversionMode(conversionMode);
        return landAirMek;
    }
}

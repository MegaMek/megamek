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
package megamek.common.rules.tacops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.CriticalSlot;
import megamek.common.HexTarget;
import megamek.common.board.Coords;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Aero;
import megamek.common.units.Crew;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import megamek.common.units.Targetable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Scanning under Total Warfare p.187: no roll with working sensors, 8+ under hostile ECM, visual inspection at 3
 * hexes for units without sensors, and no scanning from the air.
 */
class TacOpsScanningTest {

    /** The rules with the ECM question answered by the test rather than by the board. */
    private static final class RulesWithEcmAnswer extends TacOpsScanning {
        private boolean targetInsideHostileEcm = false;

        @Override
        protected boolean isInsideHostileEcm(Entity scanner, Targetable target) {
            return targetInsideHostileEcm;
        }
    }

    private final RulesWithEcmAnswer rules = new RulesWithEcmAnswer();
    private Mek scanner;
    private Mek enemy;

    @BeforeEach
    void setUp() {
        scanner = mock(Mek.class);
        when(scanner.getCrew()).thenReturn(mock(Crew.class));
        when(scanner.getPosition()).thenReturn(new Coords(3, 3));
        enemy = mock(Mek.class);
        when(enemy.getPosition()).thenReturn(new Coords(9, 3));
    }

    @Test
    void testStandardSensorsScanAnythingInLineOfSightWithNoRoll() {
        TargetRoll roll = rules.scanTargetRoll(scanner, enemy);

        assertEquals(TargetRoll.AUTOMATIC_SUCCESS, roll.getValue());
        assertFalse(roll.needsRoll());
        assertEquals(TacOpsScanning.SENSOR_RANGE, rules.scanningRange(scanner, enemy), "distance is the board's, not the rule's");
    }

    @Test
    void testATargetInsideHostileEcmNeedsAnEightOnTwoDice() {
        rules.targetInsideHostileEcm = true;

        TargetRoll roll = rules.scanTargetRoll(scanner, enemy);
        assertEquals(TacOpsScanning.ECM_TARGET_NUMBER, roll.getValue());
        assertTrue(roll.needsRoll());
    }

    @Test
    void testAHexInsideHostileEcmAlsoNeedsAnEightOnTwoDice() {
        rules.targetInsideHostileEcm = true;
        HexTarget objectiveHex = new HexTarget(new Coords(9, 3), 0, Targetable.TYPE_HEX_CLEAR);

        TargetRoll roll = rules.scanTargetRoll(scanner, objectiveHex);

        assertEquals(TacOpsScanning.ECM_TARGET_NUMBER, roll.getValue(),
              "jamming covers an objective as much as a unit");
        assertTrue(roll.needsRoll());
    }

    @Test
    void testASensorHitLeavesOnlyVisualInspectionWhichEcmCannotJam() {
        when(scanner.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_SENSORS, Mek.LOC_HEAD)).thenReturn(1);
        rules.targetInsideHostileEcm = true;

        assertEquals(TacOpsScanning.VISUAL_INSPECTION_RANGE, rules.scanningRange(scanner, enemy));
        assertEquals(TargetRoll.AUTOMATIC_SUCCESS, rules.scanTargetRoll(scanner, enemy).getValue());
    }

    @Test
    void testConventionalInfantryOnlyInspectVisually() {
        Infantry platoon = mock(Infantry.class);
        when(platoon.getCrew()).thenReturn(mock(Crew.class));
        when(platoon.isConventionalInfantry()).thenReturn(true);

        assertEquals(TacOpsScanning.VISUAL_INSPECTION_RANGE, rules.scanningRange(platoon, enemy));
        assertEquals(TargetRoll.AUTOMATIC_SUCCESS, rules.scanTargetRoll(platoon, enemy).getValue());
    }

    @Test
    void testAnAirborneAerospaceUnitCannotScanButAGroundedOneCan() {
        Aero fighter = mock(Aero.class);
        when(fighter.getCrew()).thenReturn(mock(Crew.class));
        when(fighter.isAero()).thenReturn(true);
        when(fighter.isAirborne()).thenReturn(true);
        Targetable hexTarget = mock(Targetable.class);

        assertEquals(TargetRoll.IMPOSSIBLE, rules.scanTargetRoll(fighter, hexTarget).getValue());
        assertEquals(0, rules.scanningRange(fighter, hexTarget));

        when(fighter.isAirborne()).thenReturn(false);
        assertEquals(TargetRoll.AUTOMATIC_SUCCESS, rules.scanTargetRoll(fighter, hexTarget).getValue());
    }
}

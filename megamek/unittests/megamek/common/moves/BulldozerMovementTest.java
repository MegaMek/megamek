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
package megamek.common.moves;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.GameBoardTestCase;
import megamek.common.enums.MoveStepType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.exceptions.LocationFullException;
import megamek.common.options.OptionsConstants;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.SupportTank;
import megamek.common.units.Tank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Movement-rule tests for a vehicle bulldozer (TacOps): a bulldozer lets a vehicle enter a rubble hex its motive type
 * would normally bar, and the Clear Rubble stance is legal only for a bulldozer vehicle in a rubble hex while the
 * bulldozer optional rule is enabled.
 */
class BulldozerMovementTest extends GameBoardTestCase {

    static {
        // A unit starts at 0101 facing south, so one FORWARDS step enters 0102.
        initializeBoard("BULLDOZER_RUBBLE_AHEAD", """
              size 1 3
              hex 0101 0 "" ""
              hex 0102 0 "rubble:1" ""
              hex 0103 0 "" ""
              end""");
        initializeBoard("BULLDOZER_ON_RUBBLE", """
              size 1 3
              hex 0101 0 "rubble:1" ""
              hex 0102 0 "" ""
              hex 0103 0 "" ""
              end""");
        // 0102 is a cleared rubble hex: the rubble is gone (clear terrain) with only the cosmetic fluff path overlay.
        initializeBoard("BULLDOZER_CLEARED_AHEAD", """
              size 1 3
              hex 0101 0 "" ""
              hex 0102 0 "fluff:2001" ""
              hex 0103 0 "" ""
              end""");
    }

    private Tank wheeledTank(boolean withBulldozer) throws LocationFullException {
        Tank tank = new Tank();
        if (withBulldozer) {
            tank.addEquipment(EquipmentType.get(EquipmentTypeLookup.BULLDOZER), Tank.LOC_FRONT);
        }
        return tank;
    }

    @Test
    @DisplayName("A wheeled vehicle without a bulldozer may not enter rubble")
    void wheeledVehicleBarredFromRubble() throws LocationFullException {
        setBoard("BULLDOZER_RUBBLE_AHEAD");
        MovePath path = getMovePathFor(wheeledTank(false), EntityMovementMode.WHEELED, MoveStepType.FORWARDS);
        assertFalse(path.isMoveLegal(), "A wheeled vehicle is normally barred from entering rubble");
    }

    @Test
    @DisplayName("A wheeled vehicle with a bulldozer may enter rubble")
    void wheeledBulldozerMayEnterRubble() throws LocationFullException {
        setBoard("BULLDOZER_RUBBLE_AHEAD");
        MovePath path = getMovePathFor(wheeledTank(true), EntityMovementMode.WHEELED, MoveStepType.FORWARDS);
        assertTrue(path.isMoveLegal(),
              "A bulldozer lets a vehicle enter a rubble hex it would otherwise be barred from");
    }

    @Test
    @DisplayName("A wheeled SUPPORT vehicle with a bulldozer may enter rubble")
    void wheeledSupportBulldozerMayEnterRubble() throws LocationFullException {
        // Support vehicles re-implement isLocationProhibited, so they need the override too (regression for the
        // Hesiod-class playtest bug where a support bulldozer could not enter rubble).
        SupportTank supportTank = new SupportTank();
        supportTank.addEquipment(EquipmentType.get(EquipmentTypeLookup.BULLDOZER), Tank.LOC_FRONT);
        setBoard("BULLDOZER_RUBBLE_AHEAD");
        MovePath path = getMovePathFor(supportTank, EntityMovementMode.WHEELED, MoveStepType.FORWARDS);
        assertTrue(path.isMoveLegal(),
              "A bulldozer support vehicle must be able to enter rubble to clear it");
    }

    @Test
    @DisplayName("A wheeled SUPPORT vehicle without a bulldozer may not enter rubble")
    void wheeledSupportWithoutBulldozerBarredFromRubble() {
        SupportTank supportTank = new SupportTank();
        setBoard("BULLDOZER_RUBBLE_AHEAD");
        MovePath path = getMovePathFor(supportTank, EntityMovementMode.WHEELED, MoveStepType.FORWARDS);
        assertFalse(path.isMoveLegal(), "A support vehicle with no bulldozer is barred from rubble");
    }

    @Test
    @DisplayName("Any wheeled vehicle may enter a cleared (now-clear) rubble hex")
    void wheeledVehicleMayEnterClearedHex() {
        // A cleared rubble hex is genuinely clear terrain (only a cosmetic fluff path overlay remains), so any
        // wheeled vehicle - no bulldozer needed - may enter it.
        Tank tank = new Tank();
        setBoard("BULLDOZER_CLEARED_AHEAD");
        MovePath path = getMovePathFor(tank, EntityMovementMode.WHEELED, MoveStepType.FORWARDS);
        assertTrue(path.isMoveLegal(), "A cleared rubble hex is passable to all units as clear terrain");
    }

    @Test
    @DisplayName("A cleared rubble hex (no rubble left) cannot be cleared again")
    void clearedHexCannotBeCleared() throws LocationFullException {
        setBoard("BULLDOZER_CLEARED_AHEAD");
        // The cleared hex holds no rubble, so Clear Rubble is not a legal action there even for a bulldozer.
        Tank tank = wheeledTank(true);
        getGame().getOptions().getOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_BULLDOZER).setValue(true);
        MovePath path = getMovePathFor(tank, EntityMovementMode.WHEELED, MoveStepType.FORWARDS,
              MoveStepType.CLEAR_RUBBLE);
        assertFalse(path.isMoveLegal(), "A hex with no rubble cannot be cleared");
    }

    @Test
    @DisplayName("A bulldozer vehicle in a rubble hex may declare Clear Rubble when the optional rule is on")
    void clearRubbleLegalWithBulldozerAndOption() throws LocationFullException {
        setBoard("BULLDOZER_ON_RUBBLE");
        getGame().getOptions().getOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_BULLDOZER).setValue(true);
        MovePath path = getMovePathFor(wheeledTank(true), EntityMovementMode.WHEELED, MoveStepType.CLEAR_RUBBLE);
        assertTrue(path.isMoveLegal(),
              "A bulldozer vehicle in a rubble hex may clear it with the optional rule enabled");
    }

    @Test
    @DisplayName("Clear Rubble is illegal without a bulldozer")
    void clearRubbleIllegalWithoutBulldozer() throws LocationFullException {
        setBoard("BULLDOZER_ON_RUBBLE");
        getGame().getOptions().getOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_BULLDOZER).setValue(true);
        MovePath path = getMovePathFor(wheeledTank(false), EntityMovementMode.WHEELED, MoveStepType.CLEAR_RUBBLE);
        assertFalse(path.isMoveLegal(), "Clearing rubble requires a working bulldozer");
    }

    @Test
    @DisplayName("Clear Rubble is illegal when the optional rule is off")
    void clearRubbleIllegalWithOptionOff() throws LocationFullException {
        setBoard("BULLDOZER_ON_RUBBLE");
        getGame().getOptions().getOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_BULLDOZER).setValue(false);
        MovePath path = getMovePathFor(wheeledTank(true), EntityMovementMode.WHEELED, MoveStepType.CLEAR_RUBBLE);
        assertFalse(path.isMoveLegal(), "Clearing rubble requires the bulldozer optional rule to be enabled");
    }
}

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import megamek.common.GameBoardTestCase;
import megamek.common.enums.MoveStepType;
import megamek.common.units.BipedMek;
import megamek.common.units.EntityMovementType;
import megamek.common.units.Mek;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests that a hull-down Mek may still brace an arm against the hex in front of it (TO:AR p.82 bars bracing only after
 * jumping). The test unit stands in hex 0101 facing south, so the hex it braces against is 0102.
 */
class HullDownBraceTest extends GameBoardTestCase {

    static {
        initializeBoard("HILL_IN_FRONT", """
              size 2 2
              hex 0101 0 "" ""
              hex 0201 0 "" ""
              hex 0102 1 "" ""
              hex 0202 0 "" ""
              end""");
        initializeBoard("FLAT_IN_FRONT", """
              size 2 2
              hex 0101 0 "" ""
              hex 0201 0 "" ""
              hex 0102 0 "" ""
              hex 0202 0 "" ""
              end""");
    }

    private static MovePath addBrace(MovePath movePath) {
        return movePath.clone().addStep(MoveStepType.BRACE, Mek.LOC_RIGHT_ARM);
    }

    private static void assertBraceLegal(MovePath movePath, String message) {
        MoveStep lastStep = movePath.getLastStep();
        assertEquals(MoveStepType.BRACE, lastStep.getType(), "The brace step should be the last step");
        assertNotEquals(EntityMovementType.MOVE_ILLEGAL, lastStep.getMovementType(true), message);
    }

    private static void assertBraceIllegal(MovePath movePath, String message) {
        MoveStep lastStep = movePath.getLastStep();
        assertEquals(MoveStepType.BRACE, lastStep.getType(), "The brace step should be the last step");
        assertEquals(EntityMovementType.MOVE_ILLEGAL, lastStep.getMovementType(true), message);
    }

    @Test
    @DisplayName("A standing Mek may brace against a hill in front of it")
    void standingMekCanBrace() {
        setBoard("HILL_IN_FRONT");
        BipedMek mek = new BipedMek();

        MovePath movePath = addBrace(getMovePathFor(mek));

        assertBraceLegal(movePath, "A standing Mek with a Level 1 hex in front should be able to brace");
    }

    @Test
    @DisplayName("A Mek that goes hull down this turn may then brace against a hill in front of it")
    void mekGoingHullDownCanBrace() {
        setBoard("HILL_IN_FRONT");
        BipedMek mek = new BipedMek();

        MovePath movePath = addBrace(getMovePathFor(mek, MoveStepType.HULL_DOWN));

        assertBraceLegal(movePath, "Going hull down should not stop the Mek from bracing (TO:AR p.82)");
    }

    @Test
    @DisplayName("A Mek that starts the turn hull down may brace against a hill in front of it")
    void mekAlreadyHullDownCanBrace() {
        setBoard("HILL_IN_FRONT");
        BipedMek mek = new BipedMek();
        mek.setHullDown(true);

        MovePath movePath = addBrace(getMovePathFor(mek));

        assertBraceLegal(movePath, "A Mek that is already hull down should still be able to brace (TO:AR p.82)");
    }

    @Test
    @DisplayName("A hull-down Mek still needs a Level 1 hex in front of it to brace")
    void hullDownMekNeedsTerrainToBrace() {
        setBoard("FLAT_IN_FRONT");
        BipedMek mek = new BipedMek();

        MovePath movePath = addBrace(getMovePathFor(mek, MoveStepType.HULL_DOWN));

        assertBraceIllegal(movePath, "Hull down does not replace the terrain requirement for bracing");
    }

    @Test
    @DisplayName("A prone Mek still cannot brace")
    void proneMekCannotBrace() {
        setBoard("HILL_IN_FRONT");
        BipedMek mek = new BipedMek();
        mek.setProne(true);

        MovePath movePath = addBrace(getMovePathFor(mek));

        assertBraceIllegal(movePath, "A prone Mek must not be able to brace");
    }
}

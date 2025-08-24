/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.compute.Compute;
import megamek.common.game.Game;
import megamek.common.options.GameOptions;
import megamek.common.rolls.TargetRoll;
import org.junit.jupiter.api.Test;

class TargetRollTest {

    @Test
    void removeAutomaticTest() {
        TargetRoll roll = basicTargetRoll();
        roll.addModifier(TargetRoll.AUTOMATIC_SUCCESS, "mod");
        roll.addModifier(TargetRoll.AUTOMATIC_FAIL, "mod");
        roll.removeAutos();

        assertEquals(0, roll.getValue());
        assertTrue(roll.needsRoll());

        roll.addModifier(TargetRoll.IMPOSSIBLE, "mod");
        roll.addModifier(2, "mod");
        roll.addModifier(TargetRoll.AUTOMATIC_FAIL, "mod");
        roll.addModifier(-2, "mod");
        roll.removeAutos();
        assertFalse(roll.needsRoll());

        roll.addModifier(TargetRoll.IMPOSSIBLE, "mod");
        roll.addModifier(2, "mod");
        roll.addModifier(TargetRoll.AUTOMATIC_FAIL, "mod");
        roll.addModifier(-2, "mod");
        roll.removeAutos(true);
        assertTrue(roll.needsRoll());
    }

    @Test
    void needsRollTest() {
        TargetRoll roll = basicTargetRoll();
        assertTrue(roll.needsRoll());
        roll.addModifier(TargetRoll.IMPOSSIBLE, "mod");
        assertFalse(roll.needsRoll());

        roll = basicTargetRoll();
        roll.addModifier(TargetRoll.AUTOMATIC_SUCCESS, "mod");
        assertFalse(roll.needsRoll());

        roll = basicTargetRoll();
        roll.addModifier(TargetRoll.AUTOMATIC_FAIL, "mod");
        assertFalse(roll.needsRoll());

        roll = basicTargetRoll();
        roll.addModifier(TargetRoll.CHECK_FALSE, "mod");
        assertFalse(roll.needsRoll());
    }

    private TargetRoll basicTargetRoll() {
        TargetRoll roll = new TargetRoll();
        roll.addModifier(-2, "mod");
        roll.addModifier(2, "mod");
        roll.addModifier(1, "mod");
        roll.addModifier(-1, "mod");
        return roll;
    }

    @Test
    void impossibleTest() {
        TargetRoll roll = new TargetRoll(TargetRoll.IMPOSSIBLE, "inconceivable");
        assertEquals(TargetRoll.IMPOSSIBLE, roll.getValue());
        assertEquals("inconceivable", roll.getDesc());

        roll.addModifier(-2, "ignored bonus");

        assertEquals(TargetRoll.IMPOSSIBLE, roll.getValue());
        assertEquals("inconceivable", roll.getDesc());
    }

    @Test
    void automaticFailureTest() {
        TargetRoll roll = new TargetRoll(TargetRoll.AUTOMATIC_FAIL, "inconceivable");
        assertEquals(TargetRoll.AUTOMATIC_FAIL, roll.getValue());
        assertEquals("inconceivable", roll.getDesc());

        roll.addModifier(-2, "ignored bonus");

        assertEquals(TargetRoll.AUTOMATIC_FAIL, roll.getValue());
        assertEquals("inconceivable", roll.getDesc());
    }

    @Test
    void automaticSuccessTest() {
        TargetRoll roll = new TargetRoll(TargetRoll.AUTOMATIC_SUCCESS, "great success");
        assertEquals(TargetRoll.AUTOMATIC_SUCCESS, roll.getValue());
        assertEquals("great success", roll.getDesc());

        roll.addModifier(+2, "ignored malus");

        assertEquals(TargetRoll.AUTOMATIC_SUCCESS, roll.getValue());
        assertEquals("great success", roll.getDesc());
    }

    @Test
    void checkFalseTest() {
        TargetRoll roll = new TargetRoll(TargetRoll.CHECK_FALSE, "check one, check one two");
        assertEquals(TargetRoll.CHECK_FALSE, roll.getValue());
        assertEquals("check one, check one two", roll.getDesc());

        roll.addModifier(+2, "ignored malus");

        assertEquals(TargetRoll.CHECK_FALSE, roll.getValue());
        assertEquals("check one, check one two", roll.getDesc());
    }

    @Test
    void checkFalseSupersedesTest() {
        TargetRoll roll = basicTargetRoll();
        roll.addModifier(TargetRoll.IMPOSSIBLE, "mod");
        roll.addModifier(TargetRoll.AUTOMATIC_SUCCESS, "mod");
        roll.addModifier(TargetRoll.AUTOMATIC_FAIL, "mod");
        roll.addModifier(TargetRoll.CHECK_FALSE, "mod");
        assertFalse(roll.needsRoll());
        assertEquals(roll.getValue(), TargetRoll.CHECK_FALSE);

        roll = basicTargetRoll();
        roll.addModifier(TargetRoll.CHECK_FALSE, "mod");
        roll.addModifier(TargetRoll.IMPOSSIBLE, "mod");
        roll.addModifier(TargetRoll.AUTOMATIC_SUCCESS, "mod");
        roll.addModifier(TargetRoll.AUTOMATIC_FAIL, "mod");
        assertFalse(roll.needsRoll());
        assertEquals(roll.getValue(), TargetRoll.CHECK_FALSE);
    }

    @Test
    void getDescNegativeFirstMod() {
        TargetRoll roll = new TargetRoll();

        roll.addModifier(-1, "first");
        roll.addModifier(2, "second");
        roll.addModifier(-3, "third");
        roll.addModifier(0, "fourth");

        assertEquals(-2, roll.getValue());
        assertEquals("-2", roll.getValueAsString());
    }

    @Test
    void getDescPositiveFirstMod() {
        TargetRoll roll = new TargetRoll();

        roll.addModifier(1, "first");
        roll.addModifier(-2, "second");
        roll.addModifier(3, "third");
        roll.addModifier(0, "fourth");

        assertEquals(2, roll.getValue());
        assertEquals("2", roll.getValueAsString());
    }

    // Check to-hit roll mods for VTOL, WiGE, jumping Hovers, etc.
    private Game setupGame() {
        Game game = new Game();
        GameOptions gOp = new GameOptions();
        game.setOptions(gOp);
        return game;
    }

    @Test
    void addOneForFlyingVTOL() {
        int distance = 1;
        boolean jumped = false;
        boolean vtol = true;
        Game game = setupGame();
        ToHitData thd = Compute.getTargetMovementModifier(distance, jumped, vtol, game);
        assertEquals(1, thd.getValue());
    }

    @Test
    void addOneForJumping() {
        int distance = 1;
        boolean jumped = true;
        boolean vtol = false;
        Game game = setupGame();
        ToHitData thd = Compute.getTargetMovementModifier(distance, jumped, vtol, game);
        assertEquals(1, thd.getValue());
    }
}

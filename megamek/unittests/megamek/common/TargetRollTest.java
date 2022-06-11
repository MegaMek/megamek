/*
 * Copyright (c) 2020-2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TargetRollTest {
    @Test
    public void impossibleTest() {
        TargetRoll roll = new TargetRoll(TargetRoll.IMPOSSIBLE, "inconceivable");
        assertEquals(TargetRoll.IMPOSSIBLE, roll.getValue());
        assertEquals("Impossible", roll.getValueAsString());
        assertEquals("inconceivable", roll.getDesc());

        roll.addModifier(-2, "ignored bonus");

        assertEquals(TargetRoll.IMPOSSIBLE, roll.getValue());
        assertEquals("Impossible", roll.getValueAsString());
        assertEquals("inconceivable", roll.getDesc());
    }

    @Test
    public void automaticFailureTest() {
        TargetRoll roll = new TargetRoll(TargetRoll.AUTOMATIC_FAIL, "inconceivable");
        assertEquals(TargetRoll.AUTOMATIC_FAIL, roll.getValue());
        assertEquals("Automatic Failure", roll.getValueAsString());
        assertEquals("inconceivable", roll.getDesc());

        roll.addModifier(-2, "ignored bonus");

        assertEquals(TargetRoll.AUTOMATIC_FAIL, roll.getValue());
        assertEquals("Automatic Failure", roll.getValueAsString());
        assertEquals("inconceivable", roll.getDesc());
    }

    @Test
    public void automaticSuccessTest() {
        TargetRoll roll = new TargetRoll(TargetRoll.AUTOMATIC_SUCCESS, "great success");
        assertEquals(TargetRoll.AUTOMATIC_SUCCESS, roll.getValue());
        assertEquals("Automatic Success", roll.getValueAsString());
        assertEquals("great success", roll.getDesc());

        roll.addModifier(+2, "ignored malus");

        assertEquals(TargetRoll.AUTOMATIC_SUCCESS, roll.getValue());
        assertEquals("Automatic Success", roll.getValueAsString());
        assertEquals("great success", roll.getDesc());
    }
    
    @Test
    public void checkFalseTest() {
        TargetRoll roll = new TargetRoll(TargetRoll.CHECK_FALSE, "check one, check one two");
        assertEquals(TargetRoll.CHECK_FALSE, roll.getValue());
        assertEquals("Did not need to roll", roll.getValueAsString());
        assertEquals("check one, check one two", roll.getDesc());

        roll.addModifier(+2, "ignored malus");

        assertEquals(TargetRoll.CHECK_FALSE, roll.getValue());
        assertEquals("Did not need to roll", roll.getValueAsString());
        assertEquals("check one, check one two", roll.getDesc());
    }

    @Test
    public void getDescNegativeFirstMod() {
        TargetRoll roll = new TargetRoll();

        roll.addModifier(-1, "first");
        roll.addModifier(2, "second");
        roll.addModifier(-3, "third");
        roll.addModifier(0, "fourth");

        assertEquals("-1 (first) + 2 (second) - 3 (third) + 0 (fourth)", roll.getDesc());
        assertEquals(-2, roll.getValue());
        assertEquals("-2", roll.getValueAsString());
    }

    @Test
    public void getDescPositiveFirstMod() {
        TargetRoll roll = new TargetRoll();

        roll.addModifier(1, "first");
        roll.addModifier(-2, "second");
        roll.addModifier(3, "third");
        roll.addModifier(0, "fourth");

        assertEquals("1 (first) - 2 (second) + 3 (third) + 0 (fourth)", roll.getDesc());
        assertEquals(2, roll.getValue());
        assertEquals("2", roll.getValueAsString());
    }
}

/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link InfantryCombatResult} focusing on result creation
 * and interpretation of TOAR Infantry vs. Infantry combat outcomes.
 */
public class InfantryCombatResultTest {

    @Test
    void testEliminated() {
        InfantryCombatResult result = InfantryCombatResult.eliminated();

        assertEquals(InfantryCombatResult.ResultType.ELIMINATED, result.getType());
        assertEquals(0, result.getAttackerCasualties());
        assertEquals(100, result.getDefenderCasualties());
        assertTrue(result.isDefenderEliminated());
        assertFalse(result.isAttackerRepulsed());
        assertFalse(result.isPartialControl());
    }

    @Test
    void testRepulsed() {
        InfantryCombatResult result = InfantryCombatResult.repulsed(30);

        assertEquals(InfantryCombatResult.ResultType.REPULSED, result.getType());
        // Repulsed takes half damage
        assertEquals(15, result.getAttackerCasualties());
        assertEquals(0, result.getDefenderCasualties());
        assertFalse(result.isDefenderEliminated());
        assertTrue(result.isAttackerRepulsed());
        assertFalse(result.isPartialControl());
    }

    @Test
    void testPartial_DefenderEliminated() {
        InfantryCombatResult result = InfantryCombatResult.partial(25, true);

        assertEquals(InfantryCombatResult.ResultType.PARTIAL, result.getType());
        assertEquals(25, result.getAttackerCasualties());
        assertEquals(100, result.getDefenderCasualties());
        assertTrue(result.isDefenderEliminated());
        assertFalse(result.isAttackerRepulsed());
        assertTrue(result.isPartialControl());
    }

    @Test
    void testPartial_DefenderHalfDamage() {
        InfantryCombatResult result = InfantryCombatResult.partial(40, false);

        assertEquals(InfantryCombatResult.ResultType.PARTIAL, result.getType());
        assertEquals(40, result.getAttackerCasualties());
        // Defender takes half of attacker's casualties
        assertEquals(20, result.getDefenderCasualties());
        assertFalse(result.isDefenderEliminated());
        assertFalse(result.isAttackerRepulsed());
        assertTrue(result.isPartialControl());
    }

    @Test
    void testCasualties() {
        InfantryCombatResult result = InfantryCombatResult.casualties(45, 55);

        assertEquals(InfantryCombatResult.ResultType.CASUALTIES, result.getType());
        assertEquals(45, result.getAttackerCasualties());
        assertEquals(55, result.getDefenderCasualties());
        assertFalse(result.isDefenderEliminated());
        assertFalse(result.isAttackerRepulsed());
        assertFalse(result.isPartialControl());
    }

    @Test
    void testCasualties_EqualLosses() {
        InfantryCombatResult result = InfantryCombatResult.casualties(50, 50);

        assertEquals(InfantryCombatResult.ResultType.CASUALTIES, result.getType());
        assertEquals(50, result.getAttackerCasualties());
        assertEquals(50, result.getDefenderCasualties());
    }

    @Test
    void testToString_Eliminated() {
        InfantryCombatResult result = InfantryCombatResult.eliminated();
        String str = result.toString();

        assertTrue(str.contains("Eliminated") || str.contains("E"));
    }

    @Test
    void testToString_Repulsed() {
        InfantryCombatResult result = InfantryCombatResult.repulsed(30);
        String str = result.toString();

        assertTrue(str.contains("Repulsed") || str.contains("R"));
    }

    @Test
    void testToString_Partial() {
        InfantryCombatResult result = InfantryCombatResult.partial(25, false);
        String str = result.toString();

        assertTrue(str.contains("Partial") || str.contains("P"));
    }

    @Test
    void testToString_Casualties() {
        InfantryCombatResult result = InfantryCombatResult.casualties(45, 55);
        String str = result.toString();

        assertTrue(str.contains("45") && str.contains("55"));
    }
}

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

package megamek.common.compute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.InfantryCombatResult;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link InfantryCombatTables} focusing on ratio calculation,
 * action resolution, and crew casualties conversion (TOAR p. 173-174).
 */
public class InfantryCombatTablesTest {

    // ==================== Ratio Calculation Tests ====================

    @Test
    void testCalculateRatio_EvenMatch() {
        assertEquals("1:1", InfantryCombatTables.calculateRatio(28, 28));
        assertEquals("1:1", InfantryCombatTables.calculateRatio(50, 50));
    }

    @Test
    void testCalculateRatio_AttackerAdvantage() {
        // 2:1 ratio (exactly 2.0)
        assertEquals("2:1", InfantryCombatTables.calculateRatio(50, 25));

        // 2.6:1 still rounds to 2:1 (TOAR: "2.1 in the attacker's favor would become 2:1")
        assertEquals("2:1", InfantryCombatTables.calculateRatio(65, 25));

        // 3:1 ratio (ratio must be >= 3.0)
        assertEquals("3:1", InfantryCombatTables.calculateRatio(75, 25));

        // >3:1 ratio (ratio significantly > 3.0) - Rules don't support this??? It says to always round
        // towards defenders? So how would this...?
        //assertEquals(">3:1", InfantryCombatTables.calculateRatio(100, 25));
    }

    @Test
    void testCalculateRatio_DefenderAdvantage() {
        // 1:2 ratio
        assertEquals("1:2", InfantryCombatTables.calculateRatio(25, 50));

        // 1:3 ratio
        assertEquals("1:3", InfantryCombatTables.calculateRatio(25, 75));

        // 1:3< ratio
        assertEquals("1:3<", InfantryCombatTables.calculateRatio(25, 100));
    }

    @Test
    void testCalculateRatio_SlightDifferences() {
        // 3:2 ratio (slight advantage)
        assertEquals("3:2", InfantryCombatTables.calculateRatio(60, 40));

        // 2:3 ratio (slight disadvantage)
        assertEquals("2:3", InfantryCombatTables.calculateRatio(40, 60));
    }

    @Test
    void testCalculateRatio_ZeroStrength() {
        // Both zero
        assertEquals("1:1", InfantryCombatTables.calculateRatio(0, 0));

        // Attacker zero
        assertEquals("1:3<", InfantryCombatTables.calculateRatio(0, 28));

        // Defender zero
        assertEquals(">3:1", InfantryCombatTables.calculateRatio(28, 0));
    }

    @Test
    void testCalculateRatio_TOARExamples() {
        // TOAR p. 172 example: "a ratio of 2.1:1 in the attacker's favor would become 2:1"
        // Attacker MPS = 21, Defender MPS = 10 → ratio = 2.1 → uses 2:1 column
        assertEquals("2:1", InfantryCombatTables.calculateRatio(21, 10));

        // TOAR "round in favor of defender" - unless you reach the next tier, round DOWN
        // Ratio 1.4 < 1.5, so rounds DOWN to 1:1
        assertEquals("1:1", InfantryCombatTables.calculateRatio(14, 10)); // 1.4 → "1:1"
        assertEquals("1:1", InfantryCombatTables.calculateRatio(11, 10)); // 1.1 → "1:1"
        assertEquals("1:1", InfantryCombatTables.calculateRatio(14, 10)); // 1.4 → "1:1"
        assertEquals("1:1", InfantryCombatTables.calculateRatio(149, 100)); // 1.49 → "1:1"

        // When you reach the tier exactly (>= 1.5), you get that tier
        assertEquals("3:2", InfantryCombatTables.calculateRatio(150, 100)); // 1.50 → "3:2"
        assertEquals("3:2", InfantryCombatTables.calculateRatio(15, 10)); // 1.5 → "3:2"

        // Verify 2:1 threshold (>= 2.0)
        assertEquals("3:2", InfantryCombatTables.calculateRatio(199, 100)); // 1.99 → "3:2"
        assertEquals("2:1", InfantryCombatTables.calculateRatio(200, 100)); // 2.00 → "2:1"
    }

    // ==================== Action Resolution Tests ====================

    @Test
    void testResolveAction_EvenMatch_Snake_Eyes() {
        // 1:1 ratio, roll 2 (worst outcome)
        InfantryCombatResult result = InfantryCombatTables.resolveAction("1:1", 2);

        assertNotNull(result);
        // Should be 75%/25% casualties (worst for attacker in even fight)
        assertEquals(75, result.getAttackerCasualties());
        assertEquals(25, result.getDefenderCasualties());
    }

    @Test
    void testResolveAction_EvenMatch_Boxcars() {
        // 1:1 ratio, roll 12 (best outcome for attacker)
        InfantryCombatResult result = InfantryCombatTables.resolveAction("1:1", 12);

        assertNotNull(result);
        // Should be 25%/75% casualties (best for attacker in even fight)
        assertEquals(25, result.getAttackerCasualties());
        assertEquals(75, result.getDefenderCasualties());
        assertEquals(InfantryCombatResult.ResultType.CASUALTIES, result.getType());
    }

    @Test
    void testResolveAction_AttackerOverwhelming() {
        // >3:1 ratio, roll 12 (attacker dominates)
        InfantryCombatResult result = InfantryCombatTables.resolveAction(">3:1", 12);

        assertNotNull(result);
        // Should be 5%/55% casualties (minimal attacker losses)
        assertEquals(5, result.getAttackerCasualties());
        assertEquals(55, result.getDefenderCasualties());
    }

    @Test
    void testResolveAction_AttackerSeverelyOutnumbered() {
        // 1:3< ratio, roll 2 (worst for severely outnumbered attacker)
        InfantryCombatResult result = InfantryCombatTables.resolveAction("1:3<", 2);

        assertNotNull(result);
        assertEquals(InfantryCombatResult.ResultType.REPULSED, result.getType());
        // Repulsed means attacker takes half damage, defender takes 0
        assertEquals(0, result.getAttackerCasualties()); // 1% / 2 = 0
        assertEquals(0, result.getDefenderCasualties());
    }

    @Test
    void testResolveAction_PartialControl() {
        // 1:2 ratio, roll 12 (Partial control achieved)
        InfantryCombatResult result = InfantryCombatTables.resolveAction("1:2", 12);

        assertNotNull(result);
        assertEquals(InfantryCombatResult.ResultType.PARTIAL, result.getType());
        assertTrue(result.isPartialControl());
    }

    @Test
    void testResolveAction_Repulsed() {
        // 1:3 ratio, roll 7 (Repulsed)
        InfantryCombatResult result = InfantryCombatTables.resolveAction("1:3", 7);

        assertNotNull(result);
        assertEquals(InfantryCombatResult.ResultType.REPULSED, result.getType());
    }

    @Test
    void testResolveAction_AllRatios() {
        // Test that all ratio strings resolve without errors
        String[] ratios = {"1:3<", "1:3", "1:2", "2:3", "1:1", "3:2", "2:1", "3:1", ">3:1"};

        for (String ratio : ratios) {
            for (int roll = 2; roll <= 12; roll++) {
                InfantryCombatResult result = InfantryCombatTables.resolveAction(ratio, roll);
                assertNotNull(result, "Ratio " + ratio + " roll " + roll + " should resolve");
            }
        }
    }

    // ==================== Crew Casualties Table Tests ====================

    @Test
    void testGetCrewHits_ZeroCasualties() {
        assertEquals(0, InfantryCombatTables.getCrewHits(0));
        assertEquals(0, InfantryCombatTables.getCrewHits(-5));
    }

    @Test
    void testGetCrewHits_OneHit() {
        assertEquals(1, InfantryCombatTables.getCrewHits(1));
        assertEquals(1, InfantryCombatTables.getCrewHits(10));
        assertEquals(1, InfantryCombatTables.getCrewHits(20));
    }

    @Test
    void testGetCrewHits_TwoHits() {
        assertEquals(2, InfantryCombatTables.getCrewHits(21));
        assertEquals(2, InfantryCombatTables.getCrewHits(30));
        assertEquals(2, InfantryCombatTables.getCrewHits(35));
    }

    @Test
    void testGetCrewHits_ThreeHits() {
        assertEquals(3, InfantryCombatTables.getCrewHits(36));
        assertEquals(3, InfantryCombatTables.getCrewHits(45));
        assertEquals(3, InfantryCombatTables.getCrewHits(50));
    }

    @Test
    void testGetCrewHits_FourHits() {
        assertEquals(4, InfantryCombatTables.getCrewHits(51));
        assertEquals(4, InfantryCombatTables.getCrewHits(60));
        assertEquals(4, InfantryCombatTables.getCrewHits(65));
    }

    @Test
    void testGetCrewHits_FiveHits() {
        assertEquals(5, InfantryCombatTables.getCrewHits(66));
        assertEquals(5, InfantryCombatTables.getCrewHits(75));
        assertEquals(5, InfantryCombatTables.getCrewHits(80));
    }

    @Test
    void testGetCrewHits_SixHits() {
        assertEquals(6, InfantryCombatTables.getCrewHits(81));
        assertEquals(6, InfantryCombatTables.getCrewHits(90));
        assertEquals(6, InfantryCombatTables.getCrewHits(100));
        assertEquals(6, InfantryCombatTables.getCrewHits(150));
    }

    @Test
    void testGetCrewHits_BoundaryValues() {
        // Test exact boundary values
        assertEquals(1, InfantryCombatTables.getCrewHits(20));
        assertEquals(2, InfantryCombatTables.getCrewHits(21));

        assertEquals(2, InfantryCombatTables.getCrewHits(35));
        assertEquals(3, InfantryCombatTables.getCrewHits(36));

        assertEquals(3, InfantryCombatTables.getCrewHits(50));
        assertEquals(4, InfantryCombatTables.getCrewHits(51));

        assertEquals(4, InfantryCombatTables.getCrewHits(65));
        assertEquals(5, InfantryCombatTables.getCrewHits(66));

        assertEquals(5, InfantryCombatTables.getCrewHits(80));
        assertEquals(6, InfantryCombatTables.getCrewHits(81));
    }
}

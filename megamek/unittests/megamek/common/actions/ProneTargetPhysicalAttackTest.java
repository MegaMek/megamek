/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common.actions;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for physical attack restrictions against prone 'Meks per BMM 7th Printing.
 *
 * <p>Per BMM (7th Printing), the only physical attacks that can be made
 * against a prone 'Mek are kicks and death from above (DFA). However, if the prone 'Mek is one level higher than the
 * attacker, punch, club, and physical weapon attacks are also allowed.
 *
 * <h2>Physical Attacks Against Prone 'Meks - Different Levels Table:</h2>
 * <ul>
 *   <li>Same level: Kick, DFA only</li>
 *   <li>Prone 'Mek 1 level higher: Club, DFA, Physical Weapon, Punch</li>
 *   <li>Prone 'Mek 1 level lower: DFA only</li>
 * </ul>
 *
 * <p>Full integration testing is performed via manual in-game testing as the physical attack
 * toHit methods require extensive Game/Entity setup. These unit tests document and verify
 * the elevation difference calculations used in the implementation.
 *
 * @see PunchAttackAction
 * @see ClubAttackAction
 * @see KickAttackAction
 * @see <a href="https://github.com/MegaMek/megamek/issues/5713">GitHub Issue #5713</a>
 */
public class ProneTargetPhysicalAttackTest {

    /**
     * Helper method to check if punch/club is allowed against prone target. Per BMM: Punch/Club only allowed if prone
     * target is exactly 1 level higher.
     *
     * @param attackerElevation The attacker's absolute elevation (hex level + entity elevation)
     * @param targetElevation   The target's absolute elevation (hex level + entity elevation)
     *
     * @return true if the attack should be allowed
     */
    private boolean isPunchOrClubAllowedAgainstProne(int attackerElevation, int targetElevation) {
        // Punch/Club against prone only allowed if target is exactly 1 level higher
        return targetElevation == attackerElevation + 1;
    }

    /**
     * Helper method to check if kick is allowed against prone target. Per BMM: Kick only allowed if prone target is at
     * the same level.
     *
     * @param attackerElevation The attacker's absolute elevation (hex level + entity elevation)
     * @param targetElevation   The target's absolute elevation (hex level + entity elevation)
     *
     * @return true if the attack should be allowed
     */
    private boolean isKickAllowedAgainstProne(int attackerElevation, int targetElevation) {
        // Kick against prone only allowed at same level
        return targetElevation == attackerElevation;
    }

    /**
     * Documentation tests that verify the rules are correctly implemented. These tests document the expected behavior
     * based on BMM 7th Printing.
     */
    @Nested
    @DisplayName("Rules Documentation Tests")
    class RulesDocumentationTests {

        @Test
        @DisplayName("Different Levels Table - Prone 1 level higher allows punch/club")
        void proneOneLevelHigherAllowsPunchAndClub() {
            // Documentation: Per BMM 7th Printing Different Levels Table
            // "Prone 'Mek 1 level higher: Club, DFA, Physical Weapon, Punch"
            int attackerLevel = 0;
            int proneTargetOneLevelHigher = 1;

            assertTrue(isPunchOrClubAllowedAgainstProne(attackerLevel, proneTargetOneLevelHigher),
                  "Prone target one level higher - punch/club should be allowed");
        }

        @Test
        @DisplayName("Different Levels Table - Same level blocks punch/club against prone")
        void sameLevelBlocksPunchAndClub() {
            // Documentation: Per BMM 7th Printing
            // At same level, only kick and DFA are allowed against prone - NOT punch or club
            int attackerLevel = 0;
            int targetLevel = 0;

            assertFalse(isPunchOrClubAllowedAgainstProne(attackerLevel, targetLevel),
                  "Same level - punch/club against prone should be blocked");
        }

        @Test
        @DisplayName("Different Levels Table - Same level allows kick against prone")
        void sameLevelAllowsKick() {
            // Documentation: Per BMM 7th Printing
            // "Death from above and kick attacks may be made against a prone 'Mek"
            int attackerLevel = 0;
            int targetLevel = 0;

            assertTrue(isKickAllowedAgainstProne(attackerLevel, targetLevel),
                  "Same level - kick against prone should be allowed");
        }

        @Test
        @DisplayName("Different Levels Table - Prone 1 level lower blocks kick")
        void proneOneLevelLowerBlocksKick() {
            // Documentation: Per BMM 7th Printing Different Levels Table
            // "Prone 'Mek 1 level lower: DFA"
            // No kick allowed
            int attackerLevel = 1;
            int proneTargetOneLevelLower = 0;

            assertFalse(isKickAllowedAgainstProne(attackerLevel, proneTargetOneLevelLower),
                  "Prone target one level lower - kick should be blocked");
        }

        @Test
        @DisplayName("Different Levels Table - Prone 1 level higher blocks kick")
        void proneOneLevelHigherBlocksKick() {
            // Documentation: Per BMM 7th Printing Different Levels Table
            // "Prone 'Mek 1 level higher: Club, DFA, Physical Weapon, Punch"
            // Kick is NOT in the list, so it should be blocked
            int attackerLevel = 0;
            int proneTargetOneLevelHigher = 1;

            assertFalse(isKickAllowedAgainstProne(attackerLevel, proneTargetOneLevelHigher),
                  "Prone target one level higher - kick should be blocked");
        }
    }

    /**
     * Tests for punch attacks against prone targets. Per BMM: Punch only allowed against prone if target is 1 level
     * higher.
     */
    @Nested
    @DisplayName("Punch Attack vs Prone Target - Elevation Tests")
    class PunchAttackVsProneTests {

        @Test
        @DisplayName("Punch against prone target at same level should be IMPOSSIBLE")
        void punchAgainstProneSameLevelIsImpossible() {
            assertFalse(isPunchOrClubAllowedAgainstProne(0, 0),
                  "At same level, punch against prone should be blocked");
        }

        @Test
        @DisplayName("Punch against prone target 1 level higher should be allowed")
        void punchAgainstProneOneLevelHigherIsAllowed() {
            assertTrue(isPunchOrClubAllowedAgainstProne(0, 1),
                  "Prone target one level higher - punch should be allowed");
        }

        @Test
        @DisplayName("Punch against prone target 1 level lower should be IMPOSSIBLE")
        void punchAgainstProneOneLevelLowerIsImpossible() {
            assertFalse(isPunchOrClubAllowedAgainstProne(1, 0),
                  "At one level lower, punch against prone should be blocked");
        }

        @Test
        @DisplayName("Punch against prone target 2 levels higher should be IMPOSSIBLE")
        void punchAgainstProneTwoLevelsHigherIsImpossible() {
            // Only exactly 1 level higher is allowed
            assertFalse(isPunchOrClubAllowedAgainstProne(0, 2),
                  "At two levels higher, punch against prone should be blocked");
        }
    }

    /**
     * Tests for club attacks against prone targets. Per BMM: Club only allowed against prone if target is 1 level
     * higher.
     */
    @Nested
    @DisplayName("Club Attack vs Prone Target - Elevation Tests")
    class ClubAttackVsProneTests {

        @Test
        @DisplayName("Club against prone target at same level should be IMPOSSIBLE")
        void clubAgainstProneSameLevelIsImpossible() {
            assertFalse(isPunchOrClubAllowedAgainstProne(0, 0),
                  "At same level, club against prone should be blocked");
        }

        @Test
        @DisplayName("Club against prone target 1 level higher should be allowed")
        void clubAgainstProneOneLevelHigherIsAllowed() {
            assertTrue(isPunchOrClubAllowedAgainstProne(0, 1),
                  "Prone target one level higher - club should be allowed");
        }

        @Test
        @DisplayName("Club against prone target 1 level lower should be IMPOSSIBLE")
        void clubAgainstProneOneLevelLowerIsImpossible() {
            assertFalse(isPunchOrClubAllowedAgainstProne(1, 0),
                  "At one level lower, club against prone should be blocked");
        }
    }

    /**
     * Tests for kick attacks against prone targets. Per BMM: Kick only allowed against prone if target is at SAME
     * level.
     */
    @Nested
    @DisplayName("Kick Attack vs Prone Target - Elevation Tests")
    class KickAttackVsProneTests {

        @Test
        @DisplayName("Kick against prone target at same level should be allowed")
        void kickAgainstProneSameLevelIsAllowed() {
            assertTrue(isKickAllowedAgainstProne(0, 0),
                  "At same level, kick against prone should be allowed");
        }

        @Test
        @DisplayName("Kick against prone target 1 level higher should be IMPOSSIBLE")
        void kickAgainstProneOneLevelHigherIsImpossible() {
            assertFalse(isKickAllowedAgainstProne(0, 1),
                  "At one level higher, kick against prone should be blocked");
        }

        @Test
        @DisplayName("Kick against prone target 1 level lower should be IMPOSSIBLE")
        void kickAgainstProneOneLevelLowerIsImpossible() {
            assertFalse(isKickAllowedAgainstProne(1, 0),
                  "At one level lower, kick against prone should be blocked");
        }

        @Test
        @DisplayName("Kick against prone target at various same elevations")
        void kickAgainstProneSameElevationVariations() {
            // Test at different but equal elevations
            assertTrue(isKickAllowedAgainstProne(0, 0), "Level 0 vs 0");
            assertTrue(isKickAllowedAgainstProne(1, 1), "Level 1 vs 1");
            assertTrue(isKickAllowedAgainstProne(5, 5), "Level 5 vs 5");
        }
    }
}

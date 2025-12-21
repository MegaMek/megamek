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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.Hex;
import megamek.common.ToHitData;
import megamek.common.game.Game;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for physical attack restrictions against prone 'Meks per BMM 7th Printing.
 *
 * <p>Per BMM (7th Printing), the only physical attacks that can be made against a prone 'Mek
 * are kicks and death from above (DFA). However, if the prone 'Mek is one level higher than
 * the attacker, punch, club, and physical weapon attacks are also allowed.
 *
 * <h2>Physical Attacks Against Prone 'Meks - Different Levels Table:</h2>
 * <ul>
 *   <li>Same level: Kick, DFA only</li>
 *   <li>Prone 'Mek 1 level higher: Club, DFA, Physical Weapon, Punch</li>
 *   <li>Prone 'Mek 1 level lower: DFA only</li>
 * </ul>
 *
 * @see PunchAttackAction
 * @see ClubAttackAction
 * @see KickAttackAction
 * @see <a href="https://github.com/MegaMek/megamek/issues/5713">GitHub Issue #5713</a>
 */
public class ProneTargetPhysicalAttackTest {

    private Game mockGame;
    private GameOptions mockOptions;
    private Mek mockAttacker;
    private Mek mockTarget;
    private Hex mockAttackerHex;
    private Hex mockTargetHex;

    @BeforeEach
    void setUp() {
        // Mock game options
        mockOptions = mock(GameOptions.class);
        when(mockOptions.booleanOption(OptionsConstants.QUIRK_NEG_NO_ARMS)).thenReturn(false);
        when(mockOptions.booleanOption(any(String.class))).thenReturn(false);

        // Mock game
        mockGame = mock(Game.class);
        when(mockGame.getOptions()).thenReturn(mockOptions);

        // Mock hexes
        mockAttackerHex = mock(Hex.class);
        mockTargetHex = mock(Hex.class);

        // Mock attacker - a standard Mek that can punch
        mockAttacker = mock(Mek.class);
        when(mockAttacker.getId()).thenReturn(1);
        when(mockAttacker.isHullDown()).thenReturn(false);
        when(mockAttacker.entityIsQuad()).thenReturn(false);
        when(mockAttacker.getArmsFlipped()).thenReturn(false);
        when(mockAttacker.isLocationBad(anyInt())).thenReturn(false);
        when(mockAttacker.hasQuirk(any(String.class))).thenReturn(false);
        when(mockAttacker.hasWorkingSystem(anyInt(), anyInt())).thenReturn(true);
        when(mockAttacker.weaponFiredFrom(anyInt())).thenReturn(false);
        when(mockAttacker.hasActiveShield(anyInt())).thenReturn(false);
        when(mockAttacker.getGrappled()).thenReturn(Entity.NONE);
        when(mockAttacker.relHeight()).thenReturn(2); // Standard Mek height
        when(mockAttacker.getHeight()).thenReturn(2);

        // Mock target - a Mek that can be attacked
        mockTarget = mock(Mek.class);
        when(mockTarget.getId()).thenReturn(2);
        when(mockTarget.getHeight()).thenReturn(2);
        when(mockTarget.isImmobile()).thenReturn(false);

        // Set up game to return hexes
        when(mockGame.getHexOf(mockAttacker)).thenReturn(mockAttackerHex);
        when(mockGame.getHexOf(mockTarget)).thenReturn(mockTargetHex);
        when(mockGame.getEntity(1)).thenReturn(mockAttacker);
    }

    /**
     * Tests for punch attacks against prone targets.
     * Per BMM: Punch only allowed against prone target if target is exactly 1 level higher.
     */
    @Nested
    @DisplayName("Punch Attack vs Prone Target")
    class PunchAttackVsProneTests {

        @Test
        @DisplayName("Punch against prone target at same level should be IMPOSSIBLE")
        void punchAgainstProneSameLevelIsImpossible() {
            // Set up: Both at level 0
            when(mockAttackerHex.getLevel()).thenReturn(0);
            when(mockTargetHex.getLevel()).thenReturn(0);
            when(mockAttacker.getElevation()).thenReturn(0);
            when(mockTarget.getElevation()).thenReturn(0);
            when(mockTarget.isProne()).thenReturn(true);

            ToHitData result = PunchAttackAction.toHit(mockGame, 1, mockTarget,
                  PunchAttackAction.RIGHT, false);

            assertTrue(result.getValue() == ToHitData.IMPOSSIBLE,
                  "Punch against prone target at same level should be impossible");
            assertTrue(result.getDesc().toLowerCase().contains("prone"),
                  "Error message should mention prone");
        }

        @Test
        @DisplayName("Punch against prone target 1 level higher should be allowed")
        void punchAgainstProneOneLevelHigherIsAllowed() {
            // Set up: Attacker at level 0, prone target at level 1
            when(mockAttackerHex.getLevel()).thenReturn(0);
            when(mockTargetHex.getLevel()).thenReturn(1);
            when(mockAttacker.getElevation()).thenReturn(0);
            when(mockTarget.getElevation()).thenReturn(0);
            when(mockTarget.isProne()).thenReturn(true);

            ToHitData result = PunchAttackAction.toHit(mockGame, 1, mockTarget,
                  PunchAttackAction.RIGHT, false);

            // Should not be impossible due to prone restriction
            // (may still be impossible for other reasons in full integration)
            assertTrue(result.getValue() != ToHitData.IMPOSSIBLE ||
                        !result.getDesc().toLowerCase().contains("prone"),
                  "Punch against prone target 1 level higher should not be blocked by prone rule");
        }

        @Test
        @DisplayName("Punch against prone target 1 level lower should be IMPOSSIBLE")
        void punchAgainstProneOneLevelLowerIsImpossible() {
            // Set up: Attacker at level 1, prone target at level 0
            when(mockAttackerHex.getLevel()).thenReturn(1);
            when(mockTargetHex.getLevel()).thenReturn(0);
            when(mockAttacker.getElevation()).thenReturn(0);
            when(mockTarget.getElevation()).thenReturn(0);
            when(mockTarget.isProne()).thenReturn(true);

            ToHitData result = PunchAttackAction.toHit(mockGame, 1, mockTarget,
                  PunchAttackAction.RIGHT, false);

            assertTrue(result.getValue() == ToHitData.IMPOSSIBLE,
                  "Punch against prone target 1 level lower should be impossible");
        }

        @Test
        @DisplayName("Punch against non-prone target at same level should be allowed")
        void punchAgainstNonProneSameLevelIsAllowed() {
            // Set up: Both at level 0, target NOT prone
            when(mockAttackerHex.getLevel()).thenReturn(0);
            when(mockTargetHex.getLevel()).thenReturn(0);
            when(mockAttacker.getElevation()).thenReturn(0);
            when(mockTarget.getElevation()).thenReturn(0);
            when(mockTarget.isProne()).thenReturn(false);

            ToHitData result = PunchAttackAction.toHit(mockGame, 1, mockTarget,
                  PunchAttackAction.RIGHT, false);

            // Should not fail due to prone restriction
            assertTrue(result.getValue() != ToHitData.IMPOSSIBLE ||
                        !result.getDesc().toLowerCase().contains("prone"),
                  "Punch against non-prone target should not be blocked by prone rule");
        }
    }
}

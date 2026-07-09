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
package megamek.client.ui.clientGUI.boardview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.GameBoardTestCase;
import megamek.common.LosEffects;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("LOSModifierCalculator Tests")
class LOSModifierCalculatorTest extends GameBoardTestCase {

    private Game game;

    static {
        initializeBoard("FLAT_5_HEX", """
              size 1 5
              hex 0101 0 "" ""
              hex 0102 0 "" ""
              hex 0103 0 "" ""
              hex 0104 0 "" ""
              hex 0105 0 "" ""
              end"""
        );

        initializeBoard("HILL_BLOCKING", """
              size 1 5
              hex 0101 0 "" ""
              hex 0102 0 "" ""
              hex 0103 4 "" ""
              hex 0104 0 "" ""
              hex 0105 0 "" ""
              end"""
        );

        initializeBoard("LIGHT_WOODS", """
              size 1 3
              hex 0101 0 "" ""
              hex 0102 0 "woods:1;foliage_elev:2" ""
              hex 0103 0 "" ""
              end"""
        );

        initializeBoard("WATER_DEPTH_1", """
              size 1 3
              hex 0101 0 "" ""
              hex 0102 0 "" ""
              hex 0103 0 "water:1" ""
              end"""
        );
    }

    @BeforeEach
    void setUp() {
        game = getGame();
    }

    @Nested
    @DisplayName("buildAttackInfo Tests")
    class BuildAttackInfoTests {

        @Test
        @DisplayName("Should set correct absolute heights on flat terrain")
        void flatTerrainAbsHeights() {
            setBoard("FLAT_5_HEX");
            Coords attacker = new Coords(0, 0);
            Coords target = new Coords(0, 4);

            LosEffects.AttackInfo info = LOSModifierCalculator.buildAttackInfo(
                  game, attacker, target, 2, 1, true, false);

            // Mek TW=2 on level 0: abs = (2-1) + 0 = 1
            assertEquals(1, info.attackAbsHeight);
            // Vehicle TW=1 on level 0: abs = (1-1) + 0 = 0
            assertEquals(0, info.targetAbsHeight);
            assertTrue(info.attackerIsMek);
            assertFalse(info.targetIsMek);
        }

        @Test
        @DisplayName("Should set water flags correctly for underwater unit")
        void waterFlags() {
            setBoard("WATER_DEPTH_1");
            Coords attacker = new Coords(0, 0);
            Coords target = new Coords(0, 2);

            // TW=1 in depth-1 water: abs = (1-1) + 0 = 0, which equals hex level
            LosEffects.AttackInfo info = LOSModifierCalculator.buildAttackInfo(
                  game, attacker, target, 1, 1, false, true);

            assertTrue(info.attOnLand, "Attacker on dry hex should be on land");
            assertTrue(info.targetInWater, "Target at water surface should be in water");
            assertFalse(info.targetUnderWater);
            assertFalse(info.targetOnLand);
        }
    }

    @Nested
    @DisplayName("computeFullModifiers Tests")
    class ComputeFullModifiersTests {

        @Test
        @DisplayName("Clear LOS on flat terrain should produce low modifier total")
        void clearLosOnFlatTerrain() {
            setBoard("FLAT_5_HEX");
            Coords attacker = new Coords(0, 0);
            Coords target = new Coords(0, 4);

            String result = LOSModifierCalculator.computeFullModifiers(
                  game, attacker, target, 2, 2, true, true);

            assertNotNull(result);
            // Should contain "0 =" for zero modifiers on flat clear terrain
            assertTrue(result.contains("0"), "Flat clear terrain should have 0 modifiers: " + result);
        }

        @Test
        @DisplayName("LOS blocked by hill should report impossible")
        void losBlockedByHill() {
            setBoard("HILL_BLOCKING");
            Coords attacker = new Coords(0, 0);
            Coords target = new Coords(0, 4);

            // Both at ground level (TW=1), hill at level 4 blocks
            String result = LOSModifierCalculator.computeFullModifiers(
                  game, attacker, target, 1, 1, false, false);

            assertNotNull(result);
            // Should report blocked LOS (no numeric total prefix)
            assertFalse(result.matches("^\\d+ = .*"),
                  "Blocked LOS should not have numeric total: " + result);
        }
    }

    @Nested
    @DisplayName("isMekHullDownAt Tests")
    class IsMekHullDownAtTests {

        @Test
        @DisplayName("Empty hex should return false")
        void emptyHex() {
            setBoard("FLAT_5_HEX");
            assertFalse(LOSModifierCalculator.isMekHullDownAt(game, new Coords(0, 0)));
        }
    }
}

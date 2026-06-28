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
package megamek.common.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import megamek.common.Hex;
import megamek.common.ToHitData;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.enums.MiscTypeFlag;
import megamek.common.game.Game;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WoodsClearingAttackAction} validation logic.
 *
 * <p>Per TM pp.241-243, to clear woods an entity must have a working chainsaw or dual saw,
 * be in or adjacent to a wooded hex, and not be prone or immobile.</p>
 */
class WoodsClearingAttackActionTest {

    private Game mockGame;
    private Board mockBoard;
    private Entity mockEntity;
    private Coords targetPos;

    @BeforeEach
    void setUp() {
        mockGame = mock(Game.class);
        mockBoard = mock(Board.class);
        when(mockGame.getBoard()).thenReturn(mockBoard);
        when(mockGame.getBoard(0)).thenReturn(mockBoard);

        Coords entityPos = new Coords(5, 5);
        targetPos = new Coords(5, 6); // Adjacent hex

        mockEntity = mock(Entity.class);
        when(mockEntity.getPosition()).thenReturn(entityPos);
        when(mockEntity.isProne()).thenReturn(false);
        when(mockEntity.isImmobile()).thenReturn(false);
        when(mockEntity.hasWorkingMisc(MiscType.F_CLUB, MiscTypeFlag.S_CHAINSAW)).thenReturn(true);
        when(mockEntity.hasWorkingMisc(MiscType.F_CLUB, MiscTypeFlag.S_DUAL_SAW)).thenReturn(false);
        when(mockEntity.getSecondaryFacing()).thenReturn(3); // Facing south toward target

        // Set up a mock chainsaw in the CT (forward arc) for arc checking
        MiscMounted mockSaw = mock(MiscMounted.class);
        MiscType mockSawType = mock(MiscType.class);
        when(mockSaw.isReady()).thenReturn(true);
        when(mockSaw.getType()).thenReturn(mockSawType);
        when(mockSaw.getLocation()).thenReturn(Mek.LOC_CENTER_TORSO);
        when(mockSaw.isRearMounted()).thenReturn(false);
        when(mockSawType.hasFlag(MiscType.F_CLUB)).thenReturn(true);
        when(mockSawType.hasFlag(MiscTypeFlag.S_CHAINSAW)).thenReturn(true);
        when(mockSawType.hasFlag(MiscTypeFlag.S_DUAL_SAW)).thenReturn(false);
        when(mockEntity.getMisc()).thenReturn(List.of(mockSaw));

        // Target hex has woods
        Hex woodsHex = new Hex();
        woodsHex.addTerrain(new Terrain(Terrains.WOODS, 1));
        when(mockBoard.getHex(targetPos)).thenReturn(woodsHex);
    }

    @Nested
    @DisplayName("hasWorkingSaw()")
    class HasWorkingSawTests {

        @Test
        @DisplayName("Entity with chainsaw has working saw")
        void entityWithChainsaw() {
            assertTrue(WoodsClearingAttackAction.hasWorkingSaw(mockEntity));
        }

        @Test
        @DisplayName("Entity with dual saw has working saw")
        void entityWithDualSaw() {
            when(mockEntity.hasWorkingMisc(MiscType.F_CLUB, MiscTypeFlag.S_CHAINSAW)).thenReturn(false);
            when(mockEntity.hasWorkingMisc(MiscType.F_CLUB, MiscTypeFlag.S_DUAL_SAW)).thenReturn(true);
            assertTrue(WoodsClearingAttackAction.hasWorkingSaw(mockEntity));
        }

        @Test
        @DisplayName("Entity with no saw does not have working saw")
        void entityWithNoSaw() {
            when(mockEntity.hasWorkingMisc(MiscType.F_CLUB, MiscTypeFlag.S_CHAINSAW)).thenReturn(false);
            when(mockEntity.hasWorkingMisc(MiscType.F_CLUB, MiscTypeFlag.S_DUAL_SAW)).thenReturn(false);
            assertFalse(WoodsClearingAttackAction.hasWorkingSaw(mockEntity));
        }
    }

    @Nested
    @DisplayName("canClearWoods() validation")
    class CanClearWoodsTests {

        @Test
        @DisplayName("Valid clearing returns null (no error)")
        void validClearing() {
            ToHitData result = WoodsClearingAttackAction.canClearWoods(mockGame, mockEntity, targetPos, 0);
            assertNull(result, "Valid clearing should return null (no error)");
        }

        @Test
        @DisplayName("No working saw returns IMPOSSIBLE")
        void noWorkingSaw() {
            when(mockEntity.hasWorkingMisc(MiscType.F_CLUB, MiscTypeFlag.S_CHAINSAW)).thenReturn(false);
            when(mockEntity.hasWorkingMisc(MiscType.F_CLUB, MiscTypeFlag.S_DUAL_SAW)).thenReturn(false);

            ToHitData result = WoodsClearingAttackAction.canClearWoods(mockGame, mockEntity, targetPos, 0);
            assertNotNull(result);
            assertEquals(TargetRoll.IMPOSSIBLE, result.getValue());
        }

        @Test
        @DisplayName("Target hex too far returns IMPOSSIBLE")
        void targetTooFar() {
            Coords farPos = new Coords(10, 10); // More than 1 hex away
            Hex farHex = new Hex();
            farHex.addTerrain(new Terrain(Terrains.WOODS, 1));
            when(mockBoard.getHex(farPos)).thenReturn(farHex);

            ToHitData result = WoodsClearingAttackAction.canClearWoods(mockGame, mockEntity, farPos, 0);
            assertNotNull(result);
            assertEquals(TargetRoll.IMPOSSIBLE, result.getValue());
        }

        @Test
        @DisplayName("Target hex without woods returns IMPOSSIBLE")
        void targetNoWoods() {
            Hex clearHex = new Hex(); // No woods terrain
            when(mockBoard.getHex(targetPos)).thenReturn(clearHex);

            ToHitData result = WoodsClearingAttackAction.canClearWoods(mockGame, mockEntity, targetPos, 0);
            assertNotNull(result);
            assertEquals(TargetRoll.IMPOSSIBLE, result.getValue());
        }

        @Test
        @DisplayName("Target hex with jungle is valid")
        void targetWithJungle() {
            Hex jungleHex = new Hex();
            jungleHex.addTerrain(new Terrain(Terrains.JUNGLE, 1));
            when(mockBoard.getHex(targetPos)).thenReturn(jungleHex);

            ToHitData result = WoodsClearingAttackAction.canClearWoods(mockGame, mockEntity, targetPos, 0);
            assertNull(result, "Jungle hex should be a valid clearing target");
        }

        @Test
        @DisplayName("Prone entity returns IMPOSSIBLE")
        void proneEntity() {
            when(mockEntity.isProne()).thenReturn(true);

            ToHitData result = WoodsClearingAttackAction.canClearWoods(mockGame, mockEntity, targetPos, 0);
            assertNotNull(result);
            assertEquals(TargetRoll.IMPOSSIBLE, result.getValue());
        }

        @Test
        @DisplayName("Immobile entity returns IMPOSSIBLE")
        void immobileEntity() {
            when(mockEntity.isImmobile()).thenReturn(true);

            ToHitData result = WoodsClearingAttackAction.canClearWoods(mockGame, mockEntity, targetPos, 0);
            assertNotNull(result);
            assertEquals(TargetRoll.IMPOSSIBLE, result.getValue());
        }

        @Test
        @DisplayName("Entity in the target hex (distance 0) is valid")
        void entityInTargetHex() {
            // Entity is at the same position as the target
            when(mockEntity.getPosition()).thenReturn(targetPos);

            ToHitData result = WoodsClearingAttackAction.canClearWoods(mockGame, mockEntity, targetPos, 0);
            assertNull(result, "Entity in the target hex should be valid");
        }
    }
}

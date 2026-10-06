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

package megamek.ai.dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import megamek.common.Player;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Unit tests for UnitState class and its serializer.
 */
class UnitStateTest {

    @Test
    void testFromEntity() {
        Entity mockEntity = Mockito.mock(Entity.class);
        Game mockGame = Mockito.mock(Game.class);
        Player mockPlayer = Mockito.mock(Player.class);

        Mockito.when(mockEntity.getId()).thenReturn(1);
        Mockito.when(mockGame.getPhase()).thenReturn(GamePhase.MOVEMENT);
        Mockito.when(mockEntity.getOwner()).thenReturn(mockPlayer);
        Mockito.when(mockPlayer.getTeam()).thenReturn(1);
        Mockito.when(mockGame.getCurrentRound()).thenReturn(2);
        Mockito.when(mockPlayer.getId()).thenReturn(3);
        Mockito.when(mockEntity.getChassis()).thenReturn("Locust");
        Mockito.when(mockEntity.getModel()).thenReturn("LCT-1V");
        Mockito.when(mockEntity.getRole()).thenReturn(megamek.common.units.UnitRole.SCOUT);
        Mockito.when(mockEntity.getPosition()).thenReturn(new Coords(10, 20));
        Mockito.when(mockEntity.getFacing()).thenReturn(0);
        Mockito.when(mockEntity.getMpUsedLastRound()).thenReturn(5);
        Mockito.when(mockEntity.getHeat()).thenReturn(0);
        Mockito.when(mockEntity.isProne()).thenReturn(false);
        Mockito.when(mockEntity.isAirborne()).thenReturn(false);
        Mockito.when(mockEntity.isOffBoard()).thenReturn(false);
        Mockito.when(mockEntity.isCrippled()).thenReturn(false);
        Mockito.when(mockEntity.isDestroyed()).thenReturn(false);
        Mockito.when(mockEntity.isDone()).thenReturn(true);
        Mockito.when(mockEntity.getArmorRemainingPercent()).thenReturn(1.0);
        Mockito.when(mockEntity.getInternalRemainingPercent()).thenReturn(1.0);
        Mockito.when(mockEntity.getTotalArmor()).thenReturn(64);
        Mockito.when(mockEntity.getTotalInternal()).thenReturn(40);
        Mockito.when(mockEntity.getMaxWeaponRange()).thenReturn(15);
        Mockito.when(mockEntity.getInitialBV()).thenReturn(432);
        Mockito.when(mockEntity.getWeaponListWithHHW()).thenReturn(new ArrayList<>());
        Mockito.when(mockEntity.getWeaponList()).thenReturn(new ArrayList<>());
        Mockito.when(mockPlayer.isBot()).thenReturn(true);
        Mockito.when(mockEntity.hasActiveECM()).thenReturn(false);

        UnitState state = UnitState.fromEntity(mockEntity, mockGame);

        assertEquals(1, state.get(UnitState.Field.ID));
        assertEquals(GamePhase.MOVEMENT, state.get(UnitState.Field.PHASE));
        assertEquals("Locust", state.get(UnitState.Field.CHASSIS));
        assertEquals(10, state.get(UnitState.Field.X));
        assertEquals(20, state.get(UnitState.Field.Y));
        assertEquals(true, state.get(UnitState.Field.DONE));
        assertEquals("ACTIVE", state.get(UnitState.Field.REMOVAL));
    }

    /**
     * A destroyed unit's snapshot must say why it left the game. DESTROYED alone cannot: a unit that
     * fled reads as not-destroyed, and a killed unit only carries the flag briefly before it moves to
     * the graveyard - which is how a round-1 shoot-down vanished from a live game's log entirely.
     */
    @Test
    void removalConditionIsCarriedForGraveyardUnits() {
        Entity mockEntity = Mockito.mock(Entity.class);
        Game mockGame = Mockito.mock(Game.class);
        Player mockPlayer = Mockito.mock(Player.class);

        Mockito.when(mockGame.getPhase()).thenReturn(GamePhase.MOVEMENT);
        Mockito.when(mockEntity.getOwner()).thenReturn(mockPlayer);
        Mockito.when(mockEntity.getRole()).thenReturn(megamek.common.units.UnitRole.SCOUT);
        Mockito.when(mockEntity.isDestroyed()).thenReturn(true);
        Mockito.when(mockEntity.getRemovalCondition())
              .thenReturn(megamek.common.interfaces.IEntityRemovalConditions.REMOVE_SALVAGEABLE);
        Mockito.when(mockEntity.getWeaponListWithHHW()).thenReturn(new ArrayList<>());
        Mockito.when(mockEntity.getWeaponList()).thenReturn(new ArrayList<>());

        UnitState state = UnitState.fromEntity(mockEntity, mockGame);

        assertEquals("SALVAGEABLE", state.get(UnitState.Field.REMOVAL));
        assertEquals(true, state.get(UnitState.Field.DESTROYED));
        assertEquals(-1, state.get(UnitState.Field.X), "a graveyard unit has no position");
    }

    @Test
    void testSerializer() {
        UnitState state = new UnitState();
        state.put(UnitState.Field.ID, 1).put(UnitState.Field.CHASSIS, "Locust").put(UnitState.Field.DONE, true);

        UnitStateSerializer serializer = new UnitStateSerializer();
        String serialized = serializer.serialize(state);

        assertNotNull(serialized);
        assertTrue(serialized.contains("Locust"));
        assertTrue(serialized.contains("1")); // for ID or DONE (as 1)
    }
}

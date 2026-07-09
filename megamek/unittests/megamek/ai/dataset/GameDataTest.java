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
import static org.junit.jupiter.api.Assertions.assertNull;

import megamek.common.game.Game;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Unit tests for GameData class.
 */
class GameDataTest {

    @Test
    void testEmptyConstructor() {
        GameData data = new GameData();
        assertNull(data.get(GameData.Field.ROUND));
        assertEquals(GameData.Field.class, data.getFieldEnumClass());
    }

    @Test
    void testFromGameBasic() {
        Game mockGame = Mockito.mock(Game.class);
        Mockito.when(mockGame.getCurrentRound()).thenReturn(5);
        Mockito.when(mockGame.getPhase()).thenReturn(megamek.common.enums.GamePhase.MOVEMENT);
        Mockito.when(mockGame.getTurnIndex()).thenReturn(2);
        Mockito.when(mockGame.getTurn()).thenReturn(null); // Null turn player
        Mockito.when(mockGame.getInGameObjects()).thenReturn(new java.util.ArrayList<>());
        Mockito.when(mockGame.getMinedCoords())
              .thenReturn(new java.util.Vector<megamek.common.board.Coords>().elements());

        GameData data = GameData.fromGame(mockGame);

        assertEquals(5, data.get(GameData.Field.ROUND));
        assertEquals(megamek.common.enums.GamePhase.MOVEMENT, data.get(GameData.Field.PHASE));
        assertEquals(2, data.get(GameData.Field.TURN_INDEX));
        assertEquals(-1, data.get(GameData.Field.TURN_PLAYER_ID));
        assertEquals(0, data.getUnitStates().size());
        assertEquals(0, data.getMinefields().size());
    }
}

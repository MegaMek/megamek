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
 */
package megamek.common.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.moves.MoveStep;
import megamek.common.rules.core.CoreRulesMovement;
import megamek.common.rules.totalwarfare.TWRulesMovement;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.EntityMovementType;
import megamek.common.units.Mek;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RulesMovementTest {
    @Test
    void coreAndTotalWarfareRulesExerciseMovementLogic() {
        CoreRulesMovement core = new CoreRulesMovement();
        TWRulesMovement total = new TWRulesMovement();
        Game game = Mockito.mock(Game.class);
        Mek mek = Mockito.mock(Mek.class);
        MoveStep coreStep = Mockito.mock(MoveStep.class);
        MoveStep totalStep = Mockito.mock(MoveStep.class);
        Entity attacker = Mockito.mock(Entity.class);
        Entity defender = Mockito.mock(Entity.class);
        megamek.common.board.Board board = Mockito.mock(megamek.common.board.Board.class);
        megamek.common.Hex hex = Mockito.mock(megamek.common.Hex.class);

        Mockito.when(coreStep.getElevation()).thenReturn(2);
        Mockito.when(totalStep.getElevation()).thenReturn(3);
        Mockito.when(game.getEntity(1)).thenReturn(attacker);
        Mockito.when(game.getEntity(2)).thenReturn(defender);
        Mockito.when(attacker.getBoardId()).thenReturn(1);
        Mockito.when(defender.getBoardId()).thenReturn(1);
        Mockito.when(attacker.getPosition()).thenReturn(new Coords(0, 0));
        Mockito.when(defender.getPosition()).thenReturn(new Coords(1, 0));
        Mockito.when(game.getBoard(1)).thenReturn(board);
        Mockito.when(board.getHex(new Coords(0, 0))).thenReturn(hex);
        Mockito.when(board.getHex(new Coords(1, 0))).thenReturn(hex);
        Mockito.when(hex.getLevel()).thenReturn(0);
        Mockito.when(attacker.getElevation()).thenReturn(1);
        Mockito.when(defender.getElevation()).thenReturn(2);
        Mockito.when(defender.getHeight()).thenReturn(1);

        assertNotNull(core);
        assertNotNull(total);
        assertTrue(core.checkMPZeroCauseImmobile(0));
        assertFalse(total.checkMPZeroCauseImmobile(0));
        assertTrue(core.getMekRunMP(0, 6, 8, false) >= 0);
        assertTrue(total.getMekRunMP(1, 5, 7, false) >= 0);
        assertEquals(2, core.getUnderwaterMPCost());
        assertEquals(3, total.getUnderwaterMPCost());
        assertTrue(core.isMoveIntoWaterDangerous(EntityMovementType.MOVE_RUN, EntityMovementMode.BIPED));
        assertTrue(total.isMoveIntoWaterDangerous(EntityMovementType.MOVE_RUN, EntityMovementMode.BIPED));
        assertEquals(2, core.getAccidentalFallElevation(5, 3));
        assertEquals(4, total.getAccidentalFallElevation(4, 3));
        assertEquals(3, core.getDFAElevation(game, 1, 2, coreStep));
        assertEquals(3, total.getDFAElevation(game, 1, 2, totalStep));
        assertTrue(core.enableBackwardsElevationChange(true, mek));
        assertTrue(total.enableBackwardsElevationChange(true, mek));
        assertFalse(core.reduceMaxElevation(mek));
        assertFalse(total.reduceMaxElevation(mek));
    }
}

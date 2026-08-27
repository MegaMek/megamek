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
package megamek.server.totalWarfare;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Vector;

import megamek.common.board.Board;
import megamek.common.enums.GamePhase;
import megamek.common.game.Game;
import megamek.common.game.GameTurn;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the flow of the Victory Setup phase: it runs between the Exchange phase and artillery pre-sighting, but
 * only when the objectives victory option is on and the game is on a ground map, and it is skipped in any round
 * where no player has a turn in it.
 */
class VictorySetupPhaseTest {

    private Game game;
    private GameOptions gameOptions;
    private TWGameManager gameManager;
    private TWPhaseEndManager phaseEndManager;

    @BeforeEach
    void setUp() {
        game = mock(Game.class);
        gameManager = mock(TWGameManager.class);
        when(gameManager.getGame()).thenReturn(game);
        when(gameManager.getMainPhaseReport()).thenReturn(new Vector<>());
        phaseEndManager = new TWPhaseEndManager(gameManager);

        gameOptions = new GameOptions();
        when(game.getOptions()).thenReturn(gameOptions);
        Board groundBoard = mock(Board.class);
        when(groundBoard.isGround()).thenReturn(true);
        when(game.getBoard()).thenReturn(groundBoard);
        when(game.getBoards()).thenReturn(Map.of(0, groundBoard));
    }

    @Test
    void testExchangeLeadsToVictorySetupWhenObjectivesAreInPlay() {
        gameOptions.getOption(OptionsConstants.VICTORY_USE_OBJECTIVES).setValue(true);
        when(game.getPhase()).thenReturn(GamePhase.EXCHANGE);

        phaseEndManager.managePhase();

        verify(gameManager).changePhase(GamePhase.VICTORY_SETUP);
    }

    @Test
    void testExchangeSkipsVictorySetupWhenTheOptionIsOff() {
        gameOptions.getOption(OptionsConstants.VICTORY_USE_OBJECTIVES).setValue(false);
        when(game.getPhase()).thenReturn(GamePhase.EXCHANGE);

        phaseEndManager.managePhase();

        verify(gameManager).changePhase(GamePhase.SET_ARTILLERY_AUTO_HIT_HEXES);
    }

    @Test
    void testExchangeSkipsVictorySetupOnANonGroundMap() {
        gameOptions.getOption(OptionsConstants.VICTORY_USE_OBJECTIVES).setValue(true);
        Board spaceBoard = mock(Board.class);
        when(spaceBoard.isGround()).thenReturn(false);
        when(game.getBoard()).thenReturn(spaceBoard);
        when(game.getPhase()).thenReturn(GamePhase.EXCHANGE);

        phaseEndManager.managePhase();

        verify(gameManager).changePhase(GamePhase.SET_ARTILLERY_AUTO_HIT_HEXES);
    }

    @Test
    void testExchangeSkipsVictorySetupInAMultiBoardGame() {
        // the ground-object map has no board id, so objectives cannot address a multi-board game
        gameOptions.getOption(OptionsConstants.VICTORY_USE_OBJECTIVES).setValue(true);
        Board secondBoard = mock(Board.class);
        when(game.getBoards()).thenReturn(Map.of(0, mock(Board.class), 1, secondBoard));
        when(game.getPhase()).thenReturn(GamePhase.EXCHANGE);

        phaseEndManager.managePhase();

        verify(gameManager).changePhase(GamePhase.SET_ARTILLERY_AUTO_HIT_HEXES);
    }

    @Test
    void testVictorySetupLeadsToArtilleryPreSighting() {
        // the decided ordering: objectives are placed before artillery is pre-sighted and mines are laid
        when(game.getPhase()).thenReturn(GamePhase.VICTORY_SETUP);

        phaseEndManager.managePhase();

        verify(gameManager).changePhase(GamePhase.SET_ARTILLERY_AUTO_HIT_HEXES);
    }

    @Test
    void testVictorySetupIsATurnBasedPhase() {
        assertTrue(GamePhase.VICTORY_SETUP.usesTurns());
        assertTrue(GamePhase.VICTORY_SETUP.isVictorySetup());
        assertFalse(GamePhase.DEPLOY_MINEFIELDS.isVictorySetup());
    }

    @Test
    void testVictorySetupEndsWhenItsTurnsRunOut() {
        // the phase is playable while player turns remain and stops being playable once the last
        // turn is past, so the server ends it instead of hanging on a turn that never comes
        Game realGame = new Game();
        realGame.setPhase(GamePhase.VICTORY_SETUP);
        realGame.setTurnVector(List.of(new GameTurn(0)));
        realGame.resetTurnIndex();
        assertTrue(realGame.isCurrentPhasePlayable());

        realGame.changeToNextTurn();
        realGame.changeToNextTurn();
        assertFalse(realGame.isCurrentPhasePlayable());
    }
}

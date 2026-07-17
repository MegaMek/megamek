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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;

import megamek.common.Player;
import megamek.common.game.Game;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the vote on granting a player the Game Master role: who may vote, how the vote resolves, and that only one
 * player may hold the role at a time. The vote itself is judged by {@link megamek.common.voting.Poll}, which has its
 * own tests; these cover the game manager's rules around it.
 */
class TWGameManagerGameMasterVoteTest {

    private TWGameManager gameManager;
    private Player firstPlayer;
    private Player secondPlayer;

    @BeforeEach
    void setUp() {
        Game game = new Game();

        firstPlayer = new Player(0, "First");
        firstPlayer.setTeam(1);
        game.addPlayer(0, firstPlayer);

        secondPlayer = new Player(1, "Second");
        secondPlayer.setTeam(2);
        game.addPlayer(1, secondPlayer);

        gameManager = mock(TWGameManager.class);
        doCallRealMethod().when(gameManager).setGame(any());
        doCallRealMethod().when(gameManager).requestGameMaster(any());
        doCallRealMethod().when(gameManager).startGameMasterVote(any());
        doCallRealMethod().when(gameManager).castGameMasterVote(any(), anyBoolean());
        doCallRealMethod().when(gameManager).cancelGameMasterVote(any());
        doCallRealMethod().when(gameManager).getGameMasterPoll();
        doCallRealMethod().when(gameManager).getGameMaster();
        doCallRealMethod().when(gameManager).setGameMaster(any(), anyBoolean());
        gameManager.setGame(game);

        // setGame ghosts every player, as when a saved game loads and waits for them to reconnect. These
        // players are connected: a ghost is not a gamemaster voter, and a vote among ghosts resolves at once.
        firstPlayer.setGhost(false);
        secondPlayer.setGhost(false);
    }

    @Test
    void aVoteWaitsForTheOtherPlayer() {
        gameManager.startGameMasterVote(firstPlayer);

        assertNotNull(gameManager.getGameMasterPoll(), "the vote did not open");
        assertFalse(firstPlayer.getGameMaster(), "the role was granted before the vote was decided");
    }

    @Test
    void aUnanimousYesGrantsTheRole() {
        gameManager.startGameMasterVote(firstPlayer);
        gameManager.castGameMasterVote(secondPlayer, true);

        assertTrue(firstPlayer.getGameMaster());
        assertEquals(firstPlayer, gameManager.getGameMaster());
        assertNull(gameManager.getGameMasterPoll(), "the vote was not cleared after resolving");
    }

    @Test
    void aSingleNoDeniesTheRole() {
        gameManager.startGameMasterVote(firstPlayer);
        gameManager.castGameMasterVote(secondPlayer, false);

        assertFalse(firstPlayer.getGameMaster());
        assertNull(gameManager.getGameMasterPoll(), "the vote was not cleared after resolving");
    }

    @Test
    void theRequesterCanCancelTheVote() {
        gameManager.startGameMasterVote(firstPlayer);
        gameManager.cancelGameMasterVote(firstPlayer);

        assertFalse(firstPlayer.getGameMaster());
        assertNull(gameManager.getGameMasterPoll(), "the cancelled vote was not cleared");
    }

    @Test
    void onlyTheRequesterCanCancelTheVote() {
        gameManager.startGameMasterVote(firstPlayer);
        gameManager.cancelGameMasterVote(secondPlayer);

        assertNotNull(gameManager.getGameMasterPoll(), "another player cancelled the vote");
    }

    @Test
    void aSoleVoterPassesAtOnce() {
        // the second player has no team, so the requester is the only voter and their own yes decides it
        secondPlayer.setTeam(Player.TEAM_UNASSIGNED);
        gameManager.startGameMasterVote(firstPlayer);

        assertTrue(firstPlayer.getGameMaster());
        assertNull(gameManager.getGameMasterPoll());
    }

    @Test
    void aBotIsNotAVoter() {
        // the second player is a bot, so the vote does not wait on a ballot the bot can never cast
        secondPlayer.setBot(true);
        gameManager.startGameMasterVote(firstPlayer);

        assertTrue(firstPlayer.getGameMaster(), "the vote waited on a bot's ballot");
    }

    @Test
    void aPassedVoteDoesNotUnseatAGameMaster() {
        // the role was taken while the vote ran, and only one player may hold it at a time
        gameManager.startGameMasterVote(firstPlayer);
        secondPlayer.setGameMaster(true);
        gameManager.castGameMasterVote(secondPlayer, true);

        assertFalse(firstPlayer.getGameMaster(), "the vote unseated the sitting Game Master");
        assertEquals(secondPlayer, gameManager.getGameMaster());
    }

    @Test
    void thereIsNoVoteUntilOneIsCalled() {
        assertNull(gameManager.getGameMasterPoll());
    }
}

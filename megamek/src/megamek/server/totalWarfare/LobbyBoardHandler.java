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

import java.util.Objects;

import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.postprocess.TWBoardTransformer;
import megamek.common.enums.GamePhase;
import megamek.common.game.Game;
import megamek.common.loaders.MapSettings;
import megamek.common.options.OptionsConstants;
import megamek.logging.MMLogger;

/**
 * Handles building the game board while the game is still in the lounge, so that all players can see the
 * battlefield that will actually be played before the game starts. Without this, generated maps only come into
 * existence at the start of the EXCHANGE phase, and every lobby preview shows a different random roll; custom
 * boards that only exist on the server are not visible to other clients at all.
 *
 * <p>Any player may request generation (matching the map settings free-for-all in the lobby). The built board is
 * stored in the game and broadcast through the regular board packet; at game start the EXCHANGE phase reuses it
 * instead of rolling a new one. Any change that would alter the built board (map settings, map dimensions,
 * planetary conditions, board-affecting game options) discards it again, resetting all clients to the regular
 * local preview.</p>
 */
class LobbyBoardHandler extends AbstractTWRuleHandler {
    private static final MMLogger LOGGER = MMLogger.create(LobbyBoardHandler.class);

    /** Whether the board currently stored in the game was built in the lounge and is still up to date. */
    private boolean boardGeneratedInLounge = false;

    LobbyBoardHandler(TWGameManager gameManager) {
        super(gameManager);
    }

    /**
     * @return {@code true} if a board was built in the lounge and still matches the current settings, meaning the
     *       EXCHANGE phase should reuse it instead of building a new one
     */
    boolean hasBoardFromLounge() {
        return boardGeneratedInLounge;
    }

    /**
     * Handles a client's request to build the game board from the current map settings and broadcast it. Refused
     * outside the lounge phase and while a surprise board is selected (building it would reveal the surprise).
     * Requesting again re-rolls: a fresh board is built and broadcast each time.
     *
     * @param connId the connection that sent the request
     */
    void handleGenerationRequest(int connId) {
        Player player = getGame().getPlayer(connId);
        String playerName = (player == null) ? ("Connection " + connId) : player.getName();

        if (!getGame().getPhase().isLounge()) {
            LOGGER.debug("[LobbyBoard] {}: generation request ignored - phase is {}, not the lounge",
                  playerName, getGame().getPhase());
            return;
        }
        if (hasUnsetBoardSelection()) {
            LOGGER.debug("[LobbyBoard] {}: generation request refused - the board selection is not complete "
                  + "(unset board slots, usually right after a map size change)", playerName);
            return;
        }
        if (hasSurpriseBoardSelection()) {
            LOGGER.debug("[LobbyBoard] {}: generation request refused - a surprise board is selected",
                  playerName);
            gameManager.sendServerChat(connId,
                  "The battlefield cannot be built in the lobby while a Surprise board is selected.");
            return;
        }

        Board newBoard = TWBoardTransformer.instantiateBoard(getGame().getMapSettings(),
              getGame().getPlanetaryConditions(), getGame().getOptions());
        getGame().setBoard(newBoard);
        boardGeneratedInLounge = true;
        LOGGER.info("[LobbyBoard] {} built the battlefield ({}x{} hexes)",
              playerName, newBoard.getWidth(), newBoard.getHeight());
        gameManager.sendServerChat(playerName + " built the battlefield");
        gameManager.resetPlayersDone();
        gameManager.send(gameManager.getPacketHelper().createBoardsPacket());
    }

    /**
     * Discards the lounge-built board (if there is one) because a change made it stale, and broadcasts the reset
     * so all clients fall back to their regular local preview. Safe to call from any packet handler: it does
     * nothing when no lounge-built board exists or the game has left the lounge.
     *
     * @param reason what changed, for the log
     */
    void invalidate(String reason) {
        if (!boardGeneratedInLounge) {
            return;
        }
        if (!getGame().getPhase().isLounge()) {
            LOGGER.debug("[LobbyBoard] not discarding battlefield ({}) - phase is {}, not the lounge",
                  reason, getGame().getPhase());
            return;
        }
        boardGeneratedInLounge = false;
        getGame().setBoard(new Board());
        LOGGER.info("[LobbyBoard] battlefield discarded - {}", reason);
        gameManager.send(gameManager.getPacketHelper().createBoardsPacket());
    }

    /**
     * Discards the lounge-built board if a game options change altered an option that is baked into the board
     * when it is built (bridge CF, random basements).
     *
     * @param previousBridgeCF        the bridge CF option value before the options change was applied
     * @param previousRandomBasements the random basements option value before the options change was applied
     */
    void invalidateIfBoardOptionsChanged(int previousBridgeCF, boolean previousRandomBasements) {
        int currentBridgeCF = getGame().getOptions().getOption(OptionsConstants.BASE_BRIDGE_CF).intValue();
        boolean currentRandomBasements = getGame().getOptions()
              .booleanOption(OptionsConstants.BASE_RANDOM_BASEMENTS);
        if (currentBridgeCF != previousBridgeCF) {
            invalidate("bridge CF option changed");
        } else if (currentRandomBasements != previousRandomBasements) {
            invalidate("random basements option changed");
        }
    }

    /**
     * Re-derives the lounge-built board state after a saved game was loaded, since this handler's state is not
     * part of the saved game: a game that is (still) in the lounge and holds a non-empty board can only have
     * gotten it from lounge generation, so the board is reused at game start instead of being re-rolled.
     *
     * @param loadedGame the game that was just set on the game manager
     */
    void restoreFromGame(Game loadedGame) {
        GamePhase phase = loadedGame.getPhase();
        Board board = loadedGame.getBoard();
        boolean isInLounge = (phase != null) && phase.isLounge();
        boolean hasRealBoard = (board != null) && (board.getWidth() > 0) && (board.getHeight() > 0);
        boardGeneratedInLounge = isInLounge && hasRealBoard;
        if (boardGeneratedInLounge) {
            LOGGER.info("[LobbyBoard] loaded game holds a lounge-built battlefield ({}x{} hexes) - keeping it",
                  board.getWidth(), board.getHeight());
        }
    }

    /**
     * Sends the lounge-built board to a newly connected client so that a late joiner sees the same battlefield as
     * everyone else. Does nothing when no lounge-built board exists.
     *
     * @param connId the new connection to send the board to
     */
    void sendBoardToNewConnection(int connId) {
        if (boardGeneratedInLounge) {
            LOGGER.debug("[LobbyBoard] sending lounge-built battlefield to new connection {}", connId);
            gameManager.send(connId, gameManager.getPacketHelper().createBoardsPacket());
        }
    }

    /** @return {@code true} if any selected board is a surprise board, whose pick must stay hidden until game start */
    private boolean hasSurpriseBoardSelection() {
        return getGame().getMapSettings().getBoardsSelectedVector().stream()
              .anyMatch(boardName -> (boardName != null) && boardName.startsWith(MapSettings.BOARD_SURPRISE));
    }

    /**
     * @return {@code true} if any board slot is still unset ({@code null}): map size changes fill new slots with
     *       {@code null} until they are replaced, and a board cannot be built from an incomplete selection
     */
    private boolean hasUnsetBoardSelection() {
        return getGame().getMapSettings().getBoardsSelectedVector().stream().anyMatch(Objects::isNull);
    }
}

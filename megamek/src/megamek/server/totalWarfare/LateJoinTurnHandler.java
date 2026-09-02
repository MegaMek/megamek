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

import megamek.common.Player;
import megamek.common.enums.GamePhase;
import megamek.common.game.Game;
import megamek.common.game.GameTurn;
import megamek.logging.MMLogger;

/**
 * Gives a player who connects part-way through a pre-game player-turn phase the turn they would have had.
 * <p>
 * A scenario has no lobby: the game starts the instant the host launches it, and the Victory Setup,
 * artillery pre-sighting and minefield phases each build their turn order at that moment - while every
 * other human slot is still an unconnected ghost. The second player joins into a phase whose turn list was
 * fixed before they existed, never gets a turn, and the phase ends after the host alone. Deployment and
 * everything after are unaffected, because by then everyone has connected.
 * <p>
 * The fix is small: when a player connects during one of those phases, owns units, and has no turn in the
 * remaining order, one is inserted after the current turn so they act next.
 * <p>
 * It reaches a player who joins <em>during</em> the phase, not one who joins after it. A ghost seat has no
 * turn, so with only the host connected the phase ends the moment they press Done, and a player arriving
 * afterwards has missed it. Holding the phase open for unconnected seats that own units is a separate
 * decision - the game deliberately does not block on seats that may never arrive.
 */
class LateJoinTurnHandler extends AbstractTWRuleHandler {

    private static final MMLogger LOGGER = MMLogger.create(LateJoinTurnHandler.class);

    LateJoinTurnHandler(TWGameManager gameManager) {
        super(gameManager);
    }

    /**
     * Inserts a turn for the player when the phase and their situation call for one. Safe to call for every
     * connection: it does nothing outside the pre-game player-turn phases, for a player with no units, or
     * for a player who already has a turn coming.
     *
     * @param player The player who has just connected
     *
     * @return {@code true} when a turn was inserted, so the caller knows to resend the turn list
     */
    boolean giveTurnIfPhaseHasPassedThemBy(Player player) {
        Game game = getGame();
        GamePhase phase = game.getPhase();
        if (!isPreGamePlayerTurnPhase(phase)) {
            return false;
        }
        if (game.getEntitiesOwnedBy(player) == 0) {
            LOGGER.debug("[Objective] {} joined during {} but owns no units - no turn inserted",
                  player.getName(), phase);
            return false;
        }
        if (hasTurnRemaining(game, player)) {
            LOGGER.debug("[Objective] {} joined during {} and already has a turn coming", player.getName(),
                  phase);
            return false;
        }
        game.insertTurnAfter(new GameTurn(player.getId()), game.getTurnIndex());
        LOGGER.info("[Objective] {} joined during {} after its turn order was built - a turn was inserted for "
              + "them after the current one", player.getName(), phase);
        return true;
    }

    /**
     * @param phase a game phase
     *
     * @return {@code true} for the pre-game phases that give each player one turn and build that order at
     *       phase start - the ones a late-joining player is otherwise shut out of
     */
    static boolean isPreGamePlayerTurnPhase(GamePhase phase) {
        return phase.isVictorySetup() || phase.isSetArtilleryAutoHitHexes() || phase.isDeployMinefields();
    }

    private static boolean hasTurnRemaining(Game game, Player player) {
        for (int index = game.getTurnIndex(); index < game.getTurnsList().size(); index++) {
            if (game.getTurnsList().get(index).playerId() == player.getId()) {
                return true;
            }
        }
        return false;
    }
}

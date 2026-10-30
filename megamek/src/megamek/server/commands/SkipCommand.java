/*
 * Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2003-2026 The MegaMek Team. All Rights Reserved.
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

package megamek.server.commands;

import megamek.common.Player;
import megamek.logging.MMLogger;
import megamek.server.Server;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Skips the current player's turn, if possible.
 *
 * @author Ben
 * @since February 19, 2003, 12:16 PM
 */
public class SkipCommand extends ServerCommand {

    private static final MMLogger LOGGER = MMLogger.create(SkipCommand.class);

    private final TWGameManager gameManager;

    /** Creates a new instance of SkipCommand */
    public SkipCommand(Server server, TWGameManager gameManager) {
        super(server, "skip",
              "Skips the current turn, if possible.  Usage: /skip");
        this.gameManager = gameManager;
    }

    /**
     * Run this command with the arguments supplied
     */
    @Override
    public void run(int connId, String[] args) {
        if (!canRunRestrictedCommand(connId)) {
            server.sendServerChat(connId,
                  "Observers are restricted from skipping turns.");
            return;
        }

        if (canSkip(server.getPlayer(connId))) {
            server.sendServerChat(server.getPlayer(connId).getName()
                  + " has issued the skip command...");
            gameManager.skipCurrentTurn();
        } else {
            server.sendServerChat("/skip : skip failed. Only the turn of a disconnected player, or one with nothing "
                  + "to move, can be skipped, unless you are the Game Master.");
        }
    }

    /**
     * Whether this player may skip the current turn. Anybody may skip a turn that is stuck - its player has
     * disconnected or has nothing to move. The Game Master may skip any turn: the Skip Current Turn button is theirs
     * for running the game, not only for unsticking it.
     *
     * @param player The player issuing the command
     *
     * @return {@code true} when the current turn may be skipped by this player
     */
    boolean canSkip(Player player) {
        if (gameManager.isTurnSkippable()) {
            LOGGER.info("[Skip] the current turn is stuck, so it may be skipped");
            return true;
        }
        boolean isGameMaster = (player != null) && player.isGameMaster();
        boolean hasTurn = gameManager.getGame().getTurn() != null;
        LOGGER.info("[Skip] the current turn is not stuck; issuer is Game Master: {}, a turn is running: {}",
              isGameMaster, hasTurn);
        return isGameMaster && hasTurn;
    }

}

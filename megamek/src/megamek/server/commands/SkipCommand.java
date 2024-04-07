/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.server.commands;

import megamek.common.Game;
import megamek.server.gameManager.GameManager;
import megamek.server.Server;

/**
 * Skips the current player's turn, if possible.
 * 
 * @author Ben
 * @since February 19, 2003, 12:16 PM
 */
public class SkipCommand extends ServerCommand {

    private final GameManager gameManager;

    /** Creates a new instance of SkipCommand */
    public SkipCommand(Server server, GameManager gameManager) {
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

        if (gameManager.isTurnSkippable()) {
            server.sendServerChat(server.getPlayer(connId).getName()
                    + " has issued the skip command...");
            gameManager.skipCurrentTurn();
        } else {
            server.sendServerChat("/skip : skip failed.");
        }
    }

}

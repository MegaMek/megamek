/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.server.commands;

import megamek.common.options.OptionsConstants;
import megamek.server.Server;
import megamek.server.totalwarfare.TWGameManager;

/**
 * @author Luana Scoppio
 */
public class KillCommand extends ServerCommand {

    private final TWGameManager gameManager;

    /** Creates new KillCommand */
    public KillCommand(Server server, TWGameManager gameManager) {
        super(server, "kill", "Allows a GM to destroy a single unit instantly" +
                "Usage: "+
                "/kill <id> " +
                "where id is the units ID. The units ID can be found by hovering over the unit.");
        this.gameManager = gameManager;
    }

    /**
     * Run this command with the arguments supplied
     */
    @Override
    public void run(int connId, String[] args) {

        // Check to make sure gm kills are allowed!
        if (!(server.getGame().getOptions().booleanOption(OptionsConstants.ALLOWED_GM_CAN_KILL_UNITS))) {
            server.sendServerChat(connId, "Command-line kill is not enabled in this game.");
            return;
        }
        if (!server.getPlayer(connId).getGameMaster()) {
            server.sendServerChat(connId, "You are not a Game Master.");
            return;
        }
        // Check argument integrity.
        if (args.length == 2) {
            // Check command
            try {
                int unitId = Integer.parseInt(args[1]);
                // is the unit on the board?
                var unit = gameManager.getGame().getEntity(unitId);
                if (unit == null) {
                    server.sendServerChat(connId, "Specified unit is not on the board.");
                    return;
                }
                gameManager.destroyEntity(unit, "Act of God", false, false);
                server.sendServerChat(connId, unit.getDisplayName() + " has been destroyed.");
            } catch (NumberFormatException e) {
                server.sendServerChat(connId, "Kill command failed (2).  Proper format is \"/kill <id>\" where id is the units numerical ID");
            }
        } else {
            // Error out; it's not a valid call.
            server.sendServerChat(connId, "Kill command failed (1).  Proper format is \"/kill <id>\" where id is the units ID");
        }
    }
}

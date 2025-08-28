/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

import megamek.common.units.Entity;
import megamek.common.Player;
import megamek.server.Server;
import megamek.server.totalwarfare.TWGameManager;

/**
 * The Server Command "/traitor" that will switch an entity's owner to another player.
 *
 * @author Jay Lawson (Taharqa)
 */
public class TraitorCommand extends ServerCommand {

    private final TWGameManager gameManager;

    public TraitorCommand(Server server, TWGameManager gameManager) {
        super(server,
              "traitor",
              "Switches a player's entity to another player during the end phase. "
                    + "Usage: /traitor # # where the first number is the entity id and "
                    + "the second is the new player id.");
        this.gameManager = gameManager;
    }

    /**
     * Run this command with the arguments supplied
     *
     * @see megamek.server.commands.ServerCommand#run(int, java.lang.String[])
     */
    @Override
    public void run(int connId, String[] args) {
        try {
            int eid = Integer.parseInt(args[1]);
            Entity ent = gameManager.getGame().getEntity(eid);
            int pid = Integer.parseInt(args[2]);
            Player player = server.getGame().getPlayer(pid);
            if (null == ent) {
                server.sendServerChat(connId, "No such entity.");
            } else if (ent.getOwner().getId() != connId) {
                server.sendServerChat(connId, "You must own an entity to make it switch sides.");
            } else if (null == player) {
                server.sendServerChat(connId, "No such player.");
            } else if (player.getTeam() == Player.TEAM_UNASSIGNED) {
                server.sendServerChat(connId, "Player must be assigned a team.");
            } else if (pid == connId) {
                server.sendServerChat(connId, "You can't switch to the same side.");
            } else {
                server.sendServerChat(connId,
                      ent.getDisplayName()
                            + " will switch to "
                            + player.getName()
                            + "'s side at the end of this turn.");
                ent.setTraitorId(pid);
            }
        } catch (NumberFormatException ignored) {
        }
    }

}

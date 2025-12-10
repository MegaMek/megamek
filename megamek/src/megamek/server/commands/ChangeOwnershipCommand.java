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

import java.util.List;

import megamek.client.ui.Messages;
import megamek.common.Player;
import megamek.common.units.Entity;
import megamek.server.Server;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.Arguments;
import megamek.server.commands.arguments.PlayerArgument;
import megamek.server.commands.arguments.UnitArgument;
import megamek.server.totalWarfare.TWGameManager;

/**
 * The Server Command "/changeOwner" that will switch an entity's owner to another player.
 *
 * @author Luana Coppio
 */
public class ChangeOwnershipCommand extends GamemasterServerCommand {

    public static final String UNIT_ID = "unitID";
    public static final String PLAYER_ID = "playerID";

    public ChangeOwnershipCommand(Server server, TWGameManager gameManager) {
        super(server,
              gameManager,
              "changeOwner",
              Messages.getString("Gamemaster.cmd.changeOwnership.help"),
              Messages.getString("Gamemaster.cmd.changeOwnership.longName"));
    }

    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(
              new UnitArgument(UNIT_ID, Messages.getString("Gamemaster.cmd.changeOwnership.unitID")),
              new PlayerArgument(PLAYER_ID, Messages.getString("Gamemaster.cmd.changeOwnership.playerID")));
    }

    @Override
    protected void runCommand(int connId, Arguments args) {
        var unitID = (UnitArgument) args.get(UNIT_ID);
        var playerID = (PlayerArgument) args.get(PLAYER_ID);

        Entity ent = gameManager.getGame().getEntity(unitID.getValue());
        Player player = server.getGame().getPlayer(playerID.getValue());
        if (null == ent) {
            server.sendServerChat(connId, Messages.getString("Gamemaster.cmd.changeOwnership.unitNotFound"));
        } else if (null == player) {
            server.sendServerChat(connId, Messages.getString("Gamemaster.cmd.changeOwnership.playerNotFound"));
        } else if (player.getTeam() == Player.TEAM_UNASSIGNED) {
            server.sendServerChat(connId, Messages.getString("Gamemaster.cmd.changeOwnership.playerUnassigned"));
        } else {
            server.sendServerChat(connId,
                  Messages.getString("Gamemaster.cmd.changeOwnership.success", ent.getDisplayName(), player.getName()));
            ent.setTraitorId(player.getId());
        }
    }
}

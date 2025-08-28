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
import java.util.Optional;

import megamek.client.ui.Messages;
import megamek.common.Player;
import megamek.common.actions.NukeDetonatedAction;
import megamek.common.equipment.AmmoType;
import megamek.common.event.player.GamePlayerStrategicActionEvent;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.server.Server;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.Arguments;
import megamek.server.commands.arguments.CoordXArgument;
import megamek.server.commands.arguments.CoordYArgument;
import megamek.server.commands.arguments.EnumArgument;
import megamek.server.commands.arguments.OptionalIntegerArgument;
import megamek.server.totalwarfare.TWGameManager;

/**
 * @author Luana Coppio
 */
public class NuclearStrikeCommand extends ClientServerCommand {

    /** Creates new NukeCommand */
    public NuclearStrikeCommand(Server server, TWGameManager gameManager) {
        super(server, gameManager, "ns", Messages.getString("Gamemaster.cmd.nuke.help"),
              Messages.getString("Gamemaster.cmd.nuke.longName"));
    }

    public enum NukeType {
        DAVY_CROCKETT_I,
        DAVY_CROCKETT_M,
        ALAMO,
        SANTA_ANA,
        PEACEMAKER
    }

    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(
              new CoordXArgument("x", Messages.getString("Gamemaster.cmd.x")),
              new CoordYArgument("y", Messages.getString("Gamemaster.cmd.y")),
              new EnumArgument<>("type",
                    Messages.getString("Gamemaster.cmd.nuke.type"),
                    NukeType.class,
                    NukeType.DAVY_CROCKETT_M),
              new OptionalIntegerArgument("playerID", Messages.getString("Gamemaster.cmd.playerID"))
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void runCommand(int connId, Arguments args) {
        // Check to make sure nuking is allowed by game options!
        if (!isGM(connId)) {
            if (!(server.getGame().getOptions().booleanOption(OptionsConstants.ALLOWED_REALLY_ALLOW_NUKES)
                  && server.getGame().getOptions().booleanOption(OptionsConstants.ALLOWED_ALLOW_NUKES))) {
                server.sendServerChat(connId, Messages.getString("Gamemaster.cmd.nuke.error.disabled"));
                return;
            }
        }

        if (isOutsideOfBoard(connId, args)) {
            return;
        }

        var nukeType = (NukeType) args.get("type").getValue();
        int[] nuke = {
              (int) args.get("x").getValue() - 1,
              (int) args.get("y").getValue() - 1,
              nukeType.ordinal()
        };

        var playerID = (Optional<Integer>) args.get("playerID").getValue();
        var player = getGameManager().getGame().getPlayer(playerID.orElse(Player.PLAYER_NONE));
        if (player != null) {
            gameManager.getGame().processGameEvent(
                  new GamePlayerStrategicActionEvent(gameManager,
                        new NukeDetonatedAction(Entity.NONE,
                              Player.PLAYER_NONE,
                              AmmoType.Munitions.M_DAVY_CROCKETT_M)));
        }

        gameManager.addScheduledNuke(nuke);
        server.sendServerChat(connId, Messages.getString("Gamemaster.cmd.nuke.success"));

    }
}

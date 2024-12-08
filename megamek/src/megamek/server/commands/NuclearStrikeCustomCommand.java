/*
 * MegaMek - Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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

import megamek.client.ui.Messages;
import megamek.common.AmmoType;
import megamek.common.Entity;
import megamek.common.Player;
import megamek.common.actions.NukeDetonatedAction;
import megamek.common.event.GamePlayerStrategicActionEvent;
import megamek.common.options.OptionsConstants;
import megamek.server.Server;
import megamek.server.commands.arguments.*;
import megamek.server.totalwarfare.TWGameManager;

import java.util.List;
import java.util.Optional;

/**
 * @author Luana Coppio
 */
public class NuclearStrikeCustomCommand extends ClientServerCommand {

    /** Creates new NukeCommand */
    public NuclearStrikeCustomCommand(Server server, TWGameManager gameManager) {
        super(server, gameManager, "nsc", Messages.getString("Gamemaster.cmd.nukec.help"),
            Messages.getString("Gamemaster.cmd.nukec.longName"));
    }

    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(
            new CoordXArgument("x", Messages.getString("Gamemaster.cmd.x")),
            new CoordYArgument("y", Messages.getString("Gamemaster.cmd.y")),
            new IntegerArgument("dmg", Messages.getString("Gamemaster.cmd.nukec.dmg"), 0, 1_000_000),
            new IntegerArgument("deg", Messages.getString("Gamemaster.cmd.nukec.deg"), 0, 1_000_000),
            new IntegerArgument("radius", Messages.getString("Gamemaster.cmd.nukec.radius"), 1, 1000),
            new IntegerArgument("depth", Messages.getString("Gamemaster.cmd.nukec.depth"), 0, 9),
            new OptionalIntegerArgument("playerID", Messages.getString("Gamemaster.cmd.playerID"))
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void runCommand(int connId, Arguments args) {

        // The GM can ignore the rules and nuke at will
        if (!isGM(connId)) {
            // Check to make sure nuking is allowed by game options!
            if (!(server.getGame().getOptions().booleanOption(OptionsConstants.ALLOWED_REALLY_ALLOW_NUKES)
                && server.getGame().getOptions().booleanOption(OptionsConstants.ALLOWED_ALLOW_NUKES))) {
                server.sendServerChat(connId, Messages.getString("Gamemaster.cmd.nuke.error.disabled"));
                return;
            }
        }

        // is the hex on the board?
        if (isOutsideOfBoard(connId, args)) {
            return;
        }

        int[] nuke = {
            (int) args.get("x").getValue() - 1,
            (int) args.get("y").getValue() - 1,
            (int) args.get("dmg").getValue(),
            (int) args.get("deg").getValue(),
            (int) args.get("radius").getValue(),
            (int) args.get("depth").getValue()
        };

        var playerID = (Optional<Integer>) args.get("playerID").getValue();
        var player = getGameManager().getGame().getPlayer(playerID.orElse(Player.PLAYER_NONE));
        if (player != null) {
            gameManager.getGame().processGameEvent(
                new GamePlayerStrategicActionEvent(gameManager,
                    new NukeDetonatedAction(Entity.NONE, Player.PLAYER_NONE, AmmoType.Munitions.M_DAVY_CROCKETT_M)));
        }

        gameManager.addScheduledNuke(nuke);
        server.sendServerChat(connId, Messages.getString("Gamemaster.cmd.nuke.success"));
    }
}

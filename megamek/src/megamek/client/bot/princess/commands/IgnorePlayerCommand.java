/*
 * MegaMek - Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.bot.princess.commands;

import megamek.client.bot.Messages;
import megamek.client.bot.princess.Princess;
import megamek.common.Entity;
import megamek.common.GunEmplacement;
import megamek.common.InGameObject;
import megamek.common.Player;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.Arguments;
import megamek.server.commands.arguments.TeamArgument;

import java.util.List;

/**
 * Command to ignore all units from a target player.
 * @author Luana Coppio
 */
public class IgnorePlayerCommand implements ChatCommand {
    private static final String PLAYER_ID = "playerID";
    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(
            new TeamArgument(PLAYER_ID, Messages.getString("Princess.command.ignorePlayer.playerID"))
        );
    }

    @Override
    public void execute(Princess princess, Arguments arguments) {
        int playerId = arguments.getInt(PLAYER_ID);
        Player player = princess.getGame().getPlayer(playerId);
        if (player != null) {
            princess.getGame()
                  .getInGameObjects()
                  .stream()
                  .filter(e -> e instanceof Entity entity && entity.getOwner() != null)
                  .filter(e -> ((Entity) e).getOwner().getTeam() == playerId)
                  .map(InGameObject::getId)
                  .forEach(i -> princess.getBehaviorSettings().addIgnoredUnitTarget(i));
            princess.sendChat(Messages.getString("Princess.command.ignorePlayer.playerAdded", player.getName()));
        } else {
            princess.sendChat(Messages.getString("Princess.command.ignorePlayer.playerNotFound", playerId));
        }
    }
}

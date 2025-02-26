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
import megamek.common.Player;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.Arguments;
import megamek.server.commands.arguments.PlayerArgument;

import java.util.List;

/**
 * Command to set a player as a dishonored enemy of the bot.
 * @author Luana Coppio
 */
public class BloodFeudCommand implements ChatCommand {
    private static final String PLAYER_ID = "playerId";
    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(
            new PlayerArgument(PLAYER_ID, Messages.getString("Princess.command.bloodFeud.playerId"))
        );
    }

    @Override
    public void execute(Princess princess, Arguments arguments) {
        PlayerArgument playerArg = arguments.get(PLAYER_ID, PlayerArgument.class);
        Player player = princess.getGame().getPlayer(playerArg.getValue());
        if (player != null) {
            princess.getHonorUtil().setEnemyDishonored(playerArg.getValue());
            princess.sendChat(Messages.getString("Princess.command.bloodFeud.playerNotFound", player.getName()));
        } else {
            princess.sendChat(Messages.getString("Princess.command.bloodFeud.playerAdded", playerArg.getValue()));
        }
    }
}

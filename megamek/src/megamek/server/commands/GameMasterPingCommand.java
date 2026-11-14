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
package megamek.server.commands;

import megamek.client.ui.Messages;
import megamek.common.Player;
import megamek.logging.MMLogger;
import megamek.server.Server;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Lets the Game Master limit {@code /ping} to themselves, or lift that limit. The limit is held on the Game Master's
 * own player, so every client learns of it through the normal player update, and it ends when they give up the role.
 *
 * <p>Examples: {@code /gmping} switches the limit; {@code /gmping on} and {@code /gmping off} set it.</p>
 */
public class GameMasterPingCommand extends ServerCommand {

    private static final MMLogger LOGGER = MMLogger.create(GameMasterPingCommand.class);

    public static final String COMMAND_NAME = "gmping";

    /**
     * @param server the server the command runs on
     */
    public GameMasterPingCommand(Server server) {
        super(server, COMMAND_NAME, Messages.getString("GameMasterPingCommand.help"));
    }

    /**
     * Builds the chat text that sets the limit.
     *
     * @param gameMasterOnly {@code true} to limit pings to the Game Master, {@code false} to lift the limit
     *
     * @return the command, e.g. {@code /gmping on}
     */
    public static String commandText(boolean gameMasterOnly) {
        return "/" + COMMAND_NAME + (gameMasterOnly ? " on" : " off");
    }

    @Override
    public void run(int connId, String[] args) {
        Player sender = server.getGame().getPlayer(connId);
        if ((sender == null) || !sender.isGameMaster()) {
            LOGGER.info("[PlayerPing] /gmping refused from connection {}: not the Game Master", connId);
            server.sendServerChat(connId, Messages.getString("GameMasterPingCommand.notGameMaster"));
            return;
        }
        boolean gameMasterOnly;
        if (args.length < 2) {
            gameMasterOnly = !sender.isPingGameMasterOnly();
        } else if (args[1].equalsIgnoreCase("on")) {
            gameMasterOnly = true;
        } else if (args[1].equalsIgnoreCase("off")) {
            gameMasterOnly = false;
        } else {
            server.sendServerChat(connId, getHelp());
            return;
        }
        sender.setPingGameMasterOnly(gameMasterOnly);
        ((TWGameManager) server.getGameManager()).transmitAllPlayerUpdates();
        LOGGER.info("[PlayerPing] Game Master {} set pings to {}", sender.getName(),
              gameMasterOnly ? "Game Master only" : "everyone");
        server.sendServerChat(Messages.getString(gameMasterOnly ? "GameMasterPingCommand.limited"
              : "GameMasterPingCommand.lifted"));
    }
}

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

import megamek.common.util.AddBotUtil;
import megamek.server.Server;
import megamek.server.totalwarfare.TWGameManager;

/**
 * @author dirk
 */
public class AddBotCommand extends ServerCommand {

    private final TWGameManager gameManager;

    /**
     * @param server the megamek.server.Server.
     */
    public AddBotCommand(Server server, TWGameManager gameManager) {
        super(server, AddBotUtil.COMMAND, AddBotUtil.USAGE);
        this.gameManager = gameManager;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.server.commands.ServerCommand#run(int, java.lang.String[])
     */
    @Override
    public void run(int connId, String[] args) {
        String result = new AddBotUtil().addBot(args, gameManager.getGame(), server.getHost(), server.getPort());
        server.sendServerChat(connId, result);
    }
}

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
import megamek.server.Server;
import megamek.server.commands.arguments.*;
import megamek.server.totalwarfare.TWGameManager;

import java.util.List;

/**
 * @author Luana Coppio
 */
public class AerialSupportCommand extends ClientServerCommand {

    /** Creates new NukeCommand */
    public AerialSupportCommand(Server server, TWGameManager gameManager) {
        super(server, gameManager, "as", Messages.getString("Gamemaster.cmd.aerialsupport.help"),
            Messages.getString("Gamemaster.cmd.aerialsupport.longName"));
    }

    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(
            new CoordXArgument("x", Messages.getString("Gamemaster.cmd.x")),
            new CoordYArgument("y", Messages.getString("Gamemaster.cmd.y")),
            new EnumArgument<>("type", Messages.getString("Gamemaster.cmd.aerialsupport.type"), AerialSupportType.class)
        );
    }

    public enum AerialSupportType {
        LIGHT_STRIKE,
        LIGHT_BOMBING,
        HEAVY_STRIKE,
        HEAVY_BOMBING,
        STRAFING
    }

    @Override
    protected void runCommand(int connId, Arguments args) {
        // is the hex on the board?
        if (isOutsideOfBoard(connId, args)) {
            return;
        }
        // TODO Luana : implement the Aerial Support command later
        server.sendServerChat(connId, Messages.getString("Gamemaster.cmd.aerialsupport.success"));
    }
}

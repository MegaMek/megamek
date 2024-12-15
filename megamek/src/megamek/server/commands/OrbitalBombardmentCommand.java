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
import megamek.server.props.OrbitalBombardment;
import megamek.server.totalwarfare.TWGameManager;

import java.util.List;
import java.util.Map;

/**
 * @author Luana Coppio
 */
public class OrbitalBombardmentCommand extends GamemasterServerCommand {

    public static final String X = "x";
    public static final String Y = "y";
    public static final String DMG = "dmg";
    public static final String RADIUS = "radius";

    public OrbitalBombardmentCommand(Server server, TWGameManager gameManager) {
        super(server, gameManager, "ob", Messages.getString("Gamemaster.cmd.orbitalbombardment.help"),
            Messages.getString("Gamemaster.cmd.orbitalbombardment.longName"));
    }

    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(
            new CoordXArgument(X, Messages.getString("Gamemaster.cmd.x")),
            new CoordYArgument(Y, Messages.getString("Gamemaster.cmd.y")),
            new IntegerArgument(DMG, Messages.getString("Gamemaster.cmd.orbitalbombardment.dmg"), 10, 1_000_000, 100),
            new IntegerArgument(RADIUS, Messages.getString("Gamemaster.cmd.orbitalbombardment.radius"), 1, 10, 4));
    }

    /**
     * Run this command with the arguments supplied
     */
    @Override
    protected void runCommand(int connId, Arguments args) {

        var orbitalBombardmentBuilder = new OrbitalBombardment.Builder();

        orbitalBombardmentBuilder.x((int) args.get(X).getValue() - 1)
            .y((int) args.get(Y).getValue() - 1)
            .radius((int) args.get(RADIUS).getValue())
            .damage((int) args.get(DMG).getValue());
        var orbitalBombardment = orbitalBombardmentBuilder.build();

        // is the hex on the board?
        if (!gameManager.getGame().getBoard().contains(orbitalBombardment.getX(), orbitalBombardment.getY())) {
            server.sendServerChat(connId, Messages.getString("Gamemaster.cmd.orbitalbombardment.error.outofbounds"));
            return;
        }

        gameManager.addScheduledOrbitalBombardment(orbitalBombardment);
        server.sendServerChat(Messages.getString("Gamemaster.cmd.orbitalbombardment.success"));
    }

}

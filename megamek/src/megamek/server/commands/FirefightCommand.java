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
import megamek.common.Coords;
import megamek.common.Hex;
import megamek.server.Server;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.Arguments;
import megamek.server.commands.arguments.IntegerArgument;
import megamek.server.totalwarfare.TWGameManager;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The Server Command "/firefight" that will put one hex on fire.
 *
 * @author Luana Coppio
 */
public class FirefightCommand extends GamemasterServerCommand {

    private static final String FIRESTARTER = "firefight";
    private static final String X = "x";
    private static final String Y = "y";
    private static final String TYPE = "type";

    public FirefightCommand(Server server, TWGameManager gameManager) {
        super(server,
            gameManager,
            FIRESTARTER,
            Messages.getString("Gamemaster.cmd.firefight.help"),
            Messages.getString("Gamemaster.cmd.firefight.longName"));
    }

    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(
            new IntegerArgument(X, Messages.getString("Gamemaster.cmd.x")),
            new IntegerArgument(Y, Messages.getString("Gamemaster.cmd.y"))
        );
    }

    /**
     * Run this command with the arguments supplied
     *
     * @see ServerCommand#run(int, String[])
     */
    @Override
    protected void runCommand(int connId, Arguments args) {
        int xArg = (int) args.get(X).getValue() - 1;
        int yArg = (int) args.get(Y).getValue() - 1;
        firefight(new Coords(xArg, yArg));
    }

    private void firefight(Coords coords) {
        try {
            Hex hex = gameManager.getGame().getBoard().getHex(coords);
            Objects.requireNonNull(hex, "Hex not found.");
            gameManager.removeFire(coords, Messages.getString("Gamemaster.cmd.firefight.reason"));
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to ignite hex: " + e.getMessage());
        }
    }
}

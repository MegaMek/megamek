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
import megamek.server.commands.arguments.IntegerArgument;
import megamek.server.totalwarfare.TWGameManager;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The Server Command "/firestarter" that will put one hex on fire.
 *
 * @author Luana Coppio
 */
public class FirestarterCommand extends GamemasterServerCommand {

    private static final String FIRESTARTER = "firestarter";
    private static final String X = "x";
    private static final String Y = "y";
    private static final String TYPE = "type";

    public FirestarterCommand(Server server, TWGameManager gameManager) {
        super(server,
            gameManager,
            FIRESTARTER,
            Messages.getString("Gamemaster.cmd.fire.help"),
            Messages.getString("Gamemaster.cmd.firestarter.longName"));
    }

    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(
            new IntegerArgument(X, Messages.getString("Gamemaster.cmd.x")),
            new IntegerArgument(Y, Messages.getString("Gamemaster.cmd.y")),
            new IntegerArgument(TYPE, Messages.getString("Gamemaster.cmd.fire.type"), 1, 4, 1));
    }

    /**
     * Run this command with the arguments supplied
     *
     * @see ServerCommand#run(int, String[])
     */
    @Override
    protected void runAsGM(int connId, Map<String, Argument<?>> args) {
        if (getGameManager().getGame().getBoard().inSpace()) {
            server.sendServerChat(connId, "Can't start a fire in space");
            return;
        }
        int xArg = (int) args.get(X).getValue() - 1;
        int yArg = (int) args.get(Y).getValue() - 1;
        int fireType = (int) args.get(TYPE).getValue();
        igniteHex(new Coords(xArg, yArg), fireType);
    }

    private void igniteHex(Coords coords, int fireType) {
        try {
            Hex hex = gameManager.getGame().getBoard().getHex(coords);
            Objects.requireNonNull(hex, "Hex not found.");
            gameManager.ignite(coords, fireType, gameManager.getvPhaseReport());
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to ignite hex: " + e.getMessage());
        }
    }
}

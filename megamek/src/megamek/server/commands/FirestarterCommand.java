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
 * @author Luana Scoppio
 */
public class FirestarterCommand extends GamemasterServerCommand {

    public FirestarterCommand(Server server, TWGameManager gameManager) {
        super(server,
            gameManager,
            "firestarter",
            "Starts fire in one specific hex at a specific intensity. "
                + "Usage: /firestarter <x> <y> [<intensity>]  "
                + "The intensity can be 1=Norma, 2=Inferno, 3=Inferno Bomb or 4=Inferno IV, default is 1.");
    }


    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(
            new IntegerArgument("x"),
            new IntegerArgument("y"),
            new IntegerArgument("intensity", 1, 4, 1));
    }

    /**
     * Run this command with the arguments supplied
     *
     * @see ServerCommand#run(int, String[])
     */
    @Override
    protected void runAsGM(int connId, Map<String, Argument<?>> args) {
        int xArg = (int) args.get("x").getValue() - 1;
        int yArg = (int) args.get("y").getValue() -1;
        int fireType = (int) args.get("intensity").getValue();

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

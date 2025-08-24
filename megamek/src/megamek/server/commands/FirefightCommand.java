/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

import java.util.List;
import java.util.Objects;

import megamek.client.ui.Messages;
import megamek.common.board.Coords;
import megamek.common.Hex;
import megamek.server.Server;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.Arguments;
import megamek.server.commands.arguments.CoordXArgument;
import megamek.server.commands.arguments.CoordYArgument;
import megamek.server.totalwarfare.TWGameManager;

/**
 * The Server Command "/firefight" that will put one hex on fire.
 *
 * @author Luana Coppio
 */
public class FirefightCommand extends GamemasterServerCommand {

    private static final String FIRESTARTER = "firefight";
    private static final String X = "x";
    private static final String Y = "y";

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
              new CoordXArgument(X, Messages.getString("Gamemaster.cmd.x")),
              new CoordYArgument(Y, Messages.getString("Gamemaster.cmd.y"))
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

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

import megamek.client.ui.Messages;
import megamek.server.Server;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.Arguments;
import megamek.server.commands.arguments.CoordXArgument;
import megamek.server.commands.arguments.CoordYArgument;
import megamek.server.commands.arguments.IntegerArgument;
import megamek.server.props.OrbitalBombardment;
import megamek.server.totalWarfare.TWGameManager;

/**
 * @author Luana Coppio
 */
public class OrbitalBombardmentCommand extends GamemasterServerCommand {

    public static final String X = "x";
    public static final String Y = "y";
    public static final String DMG = "dmg";
    public static final String RADIUS = "radius";

    public OrbitalBombardmentCommand(Server server, TWGameManager gameManager) {
        super(server, gameManager, "ob", Messages.getString("Gamemaster.cmd.orbitalBombardment.help"),
              Messages.getString("Gamemaster.cmd.orbitalBombardment.longName"));
    }

    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(
              new CoordXArgument(X, Messages.getString("Gamemaster.cmd.x")),
              new CoordYArgument(Y, Messages.getString("Gamemaster.cmd.y")),
              new IntegerArgument(DMG, Messages.getString("Gamemaster.cmd.orbitalBombardment.dmg"), 10, 1_000_000, 100),
              new IntegerArgument(RADIUS, Messages.getString("Gamemaster.cmd.orbitalBombardment.radius"), 1, 10, 4));
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
            if (connId != Server.SERVER_CONN) {
                server.sendServerChat(connId,
                      Messages.getString("Gamemaster.cmd.orbitalBombardment.error.outOfBounds"));
            }
            return;
        }

        gameManager.addScheduledOrbitalBombardment(orbitalBombardment);
        if (connId != Server.SERVER_CONN) {
            server.sendServerChat(connId, Messages.getString("Gamemaster.cmd.orbitalBombardment.success"));
        }
    }

}

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

import java.util.List;

import megamek.client.ui.Messages;
import megamek.common.board.Coords;
import megamek.server.Server;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.Arguments;
import megamek.server.commands.arguments.CoordXArgument;
import megamek.server.commands.arguments.CoordYArgument;
import megamek.server.commands.arguments.IntegerArgument;
import megamek.server.totalWarfare.BuildingEditHandler;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Lets a Game Master set how much of the building in a hex is left standing, or bring it down.
 *
 * <p>A construction factor of zero collapses that hex of the building, which is what the rules say happens to a
 * building whose construction factor reaches zero.</p>
 *
 * @see BuildingEditHandler
 */
public class BuildingCommand extends GamemasterServerCommand {

    public static final String X = "x";
    public static final String Y = "y";
    public static final String CONSTRUCTION_FACTOR = "cf";

    /** Comfortably above the sturdiest building in the rules, so a gamemaster is not boxed in by the dialog. */
    private static final int MAX_CONSTRUCTION_FACTOR = 500;

    /** What the spinner starts on: a solid medium building, rather than a collapse nobody asked for. */
    private static final int DEFAULT_CONSTRUCTION_FACTOR = 40;

    public BuildingCommand(Server server, TWGameManager gameManager) {
        super(server,
              gameManager,
              "building",
              Messages.getString("Gamemaster.cmd.building.help"),
              Messages.getString("Gamemaster.cmd.building.longName"));
    }

    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(new CoordXArgument(X, Messages.getString("Gamemaster.cmd.x")),
              new CoordYArgument(Y, Messages.getString("Gamemaster.cmd.y")),
              new IntegerArgument(CONSTRUCTION_FACTOR,
                    Messages.getString("Gamemaster.cmd.building.cf"),
                    BuildingEditHandler.COLLAPSING_CONSTRUCTION_FACTOR,
                    MAX_CONSTRUCTION_FACTOR,
                    DEFAULT_CONSTRUCTION_FACTOR));
    }

    @Override
    protected void runCommand(int connId, Arguments args) {
        // the dialog shows hex coordinates the way the board does, counting from one, while the board itself counts
        // from zero
        Coords coords = new Coords((int) args.get(X).getValue() - 1, (int) args.get(Y).getValue() - 1);
        int constructionFactor = (int) args.get(CONSTRUCTION_FACTOR).getValue();
        String gamemasterName = gamemasterName(connId);

        String refusal = getGameManager().buildingEditHandler()
              .setConstructionFactor(coords, constructionFactor, gamemasterName);

        if (refusal != null) {
            server.sendServerChat(connId, Messages.getString("Gamemaster.cmd.building.refused", refusal));
        }
    }

    /**
     * @return the name to credit the change to in the report, falling back to the server's own name when the command
     *       did not come from a player connection
     */
    private String gamemasterName(int connId) {
        var gamemaster = server.getPlayer(connId);
        return (gamemaster != null) ? gamemaster.getName() : Messages.getString("Gamemaster.toast.serverName");
    }
}

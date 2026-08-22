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
import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.enums.BasementType;
import megamek.server.Server;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.Arguments;
import megamek.server.commands.arguments.CoordXArgument;
import megamek.server.commands.arguments.CoordYArgument;
import megamek.server.commands.arguments.IntegerArgument;
import megamek.server.commands.arguments.OptionalEnumArgument;
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
    public static final String ARMOR = "armor";
    public static final String HEIGHT = "height";
    public static final String BASEMENT = "basement";

    /**
     * What a value spinner starts on, meaning "leave this one as it is". A gamemaster usually opens this to change one
     * thing, so every value starts at leave-alone and only what they raise is written to the building.
     */
    private static final int LEAVE_UNCHANGED = -1;

    /** Comfortably above the sturdiest building in the rules, so a gamemaster is not boxed in by the dialog. */
    private static final int MAX_CONSTRUCTION_FACTOR = 500;

    /** Above the heaviest building armor in the rules. */
    private static final int MAX_ARMOR = 200;

    /** Taller than any building on a standard map. */
    private static final int MAX_HEIGHT = 20;

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
                    LEAVE_UNCHANGED,
                    MAX_CONSTRUCTION_FACTOR,
                    LEAVE_UNCHANGED),
              new IntegerArgument(ARMOR,
                    Messages.getString("Gamemaster.cmd.building.armor"),
                    LEAVE_UNCHANGED,
                    MAX_ARMOR,
                    LEAVE_UNCHANGED),
              new IntegerArgument(HEIGHT,
                    Messages.getString("Gamemaster.cmd.building.height"),
                    LEAVE_UNCHANGED,
                    MAX_HEIGHT,
                    LEAVE_UNCHANGED),
              new OptionalEnumArgument<>(BASEMENT,
                    Messages.getString("Gamemaster.cmd.building.basement"),
                    BasementType.class));
    }

    @Override
    protected void runCommand(int connId, Arguments args) {
        // the dialog shows hex coordinates the way the board does, counting from one, while the board itself counts
        // from zero
        Coords coords = new Coords((int) args.get(X).getValue() - 1, (int) args.get(Y).getValue() - 1);
        String gamemasterName = gamemasterName(connId);
        BuildingEditHandler.BuildingEdit edit = new BuildingEditHandler.BuildingEdit(
              changedValue(args, CONSTRUCTION_FACTOR),
              changedValue(args, ARMOR),
              changedValue(args, HEIGHT),
              (BasementType) args.get(BASEMENT).getValue());

        String refusal = getGameManager().buildingEditHandler().applyEdit(coords, edit, gamemasterName);

        if (refusal != null) {
            server.sendServerChat(connId, Messages.getString("Gamemaster.cmd.building.refused", refusal));
        }
    }

    /**
     * Reads one value the gamemaster may have changed.
     *
     * @return the value to write to the building, or {@code null} when it was left at leave-unchanged
     */
    private @Nullable Integer changedValue(Arguments args, String argumentName) {
        int value = (int) args.get(argumentName).getValue();
        return (value == LEAVE_UNCHANGED) ? null : value;
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

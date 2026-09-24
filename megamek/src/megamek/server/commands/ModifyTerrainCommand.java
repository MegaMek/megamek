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
import megamek.common.units.Terrains;
import megamek.server.Server;
import megamek.server.commands.arguments.Argument;
import megamek.server.commands.arguments.Arguments;
import megamek.server.commands.arguments.CoordXArgument;
import megamek.server.commands.arguments.CoordYArgument;
import megamek.server.commands.arguments.EnumArgument;
import megamek.server.commands.arguments.IntegerArgument;
import megamek.server.totalWarfare.HexEditHandler;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Lets a Game Master set how much punishment the terrain in a hex can still take, without changing what that terrain
 * is.
 *
 * <p>Woods that have been shelled for two rounds are the usual case: the hex is still light woods, and the players
 * should still see light woods, but there is far less of it left to burn or blast away. Setting the terrain factor
 * says that, where changing the woods to a different level would say something else entirely.</p>
 *
 * <p>To change what the terrain is - light woods to heavy woods, dry ground to water - a gamemaster wants Change
 * Terrain instead.</p>
 *
 * @see ChangeTerrainCommand
 */
public class ModifyTerrainCommand extends GamemasterServerCommand {

    public static final String X = "x";
    public static final String Y = "y";
    public static final String TERRAIN = "terrain";
    public static final String TERRAIN_FACTOR = "tf";

    /** Comfortably above the toughest terrain in the rules, so a gamemaster is not boxed in by the dialog. */
    private static final int MAX_TERRAIN_FACTOR = 500;

    /** What the spinner starts on, which is roughly light woods left half standing. */
    private static final int DEFAULT_TERRAIN_FACTOR = 25;

    public ModifyTerrainCommand(Server server, TWGameManager gameManager) {
        super(server,
              gameManager,
              "modifyterrain",
              Messages.getString("Gamemaster.cmd.modifyTerrain.help"),
              Messages.getString("Gamemaster.cmd.modifyTerrain.longName"));
    }

    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(new CoordXArgument(X, Messages.getString("Gamemaster.cmd.x")),
              new CoordYArgument(Y, Messages.getString("Gamemaster.cmd.y")),
              new EnumArgument<>(TERRAIN,
                    Messages.getString("Gamemaster.cmd.modifyTerrain.terrain"),
                    ModifiableTerrain.class,
                    ModifiableTerrain.WOODS),
              new IntegerArgument(TERRAIN_FACTOR,
                    Messages.getString("Gamemaster.cmd.modifyTerrain.tf"),
                    0,
                    MAX_TERRAIN_FACTOR,
                    DEFAULT_TERRAIN_FACTOR));
    }

    /**
     * The terrains that carry a terrain factor worth setting. Terrain that cannot be worn down - pavement markings,
     * water depth, ice - is left out, because a factor on it means nothing.
     */
    public enum ModifiableTerrain {
        WOODS(Terrains.WOODS),
        JUNGLE(Terrains.JUNGLE),
        ROUGH(Terrains.ROUGH),
        RUBBLE(Terrains.RUBBLE),
        PAVEMENT(Terrains.PAVEMENT),
        ROAD(Terrains.ROAD);

        private final int terrainType;

        ModifiableTerrain(int terrainType) {
            this.terrainType = terrainType;
        }

        /** @return the {@link Terrains} type this refers to */
        public int terrainType() {
            return terrainType;
        }
    }

    @Override
    protected void runCommand(int connId, Arguments args) {
        // the dialog shows hex coordinates the way the board does, counting from one, while the board itself counts
        // from zero
        Coords coords = new Coords((int) args.get(X).getValue() - 1, (int) args.get(Y).getValue() - 1);
        ModifiableTerrain terrain = (ModifiableTerrain) args.get(TERRAIN).getValue();
        int terrainFactor = (int) args.get(TERRAIN_FACTOR).getValue();
        int boardId = getGameManager().getGame().getBoard().getBoardId();
        String gamemasterName = gamemasterName(connId);

        String refusal = getGameManager().hexEditHandler()
              .setTerrainFactor(coords, boardId, terrain.terrainType(), terrainFactor, gamemasterName);

        if (refusal != null) {
            server.sendServerChat(connId, Messages.getString("Gamemaster.cmd.modifyTerrain.refused", refusal));
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

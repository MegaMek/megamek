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
 * Lets a Game Master change the terrain in one hex: flooding it, growing woods in it, dropping rubble into it, paving
 * it, or clearing it back to bare ground.
 *
 * <p>The terrains offered are the ones a gamemaster plausibly wants to place mid-game. Fire and smoke are not among
 * them because they have commands of their own, and the structural terrains - buildings, bridges, fuel tanks - are
 * left out because they need more than a level to describe.</p>
 *
 * @see HexEditHandler
 */
public class TerrainCommand extends GamemasterServerCommand {

    public static final String X = "x";
    public static final String Y = "y";
    public static final String TERRAIN = "terrain";
    public static final String LEVEL = "level";

    /** The highest level any terrain offered here accepts; the hex itself rejects a level a terrain cannot take. */
    private static final int MAX_TERRAIN_LEVEL = 6;

    /** Stands in for a terrain type on {@link EditableTerrain#CLEAR}, which sets no terrain at all. */
    private static final int NO_TERRAIN_TYPE = 0;

    /**
     * The terrains a Game Master may place, each paired with the {@link Terrains} type it sets.
     *
     * <p>{@link #CLEAR} is the odd one out: it removes everything in the hex rather than setting one terrain, and its
     * level is ignored.</p>
     */
    public enum EditableTerrain {
        CLEAR(NO_TERRAIN_TYPE),
        WATER(Terrains.WATER),
        RAPIDS(Terrains.RAPIDS),
        WOODS(Terrains.WOODS),
        JUNGLE(Terrains.JUNGLE),
        ROUGH(Terrains.ROUGH),
        RUBBLE(Terrains.RUBBLE),
        PAVEMENT(Terrains.PAVEMENT),
        ROAD(Terrains.ROAD),
        SWAMP(Terrains.SWAMP),
        MUD(Terrains.MUD),
        SNOW(Terrains.SNOW),
        ICE(Terrains.ICE),
        TUNDRA(Terrains.TUNDRA),
        SAND(Terrains.SAND),
        MAGMA(Terrains.MAGMA);

        private final int terrainType;

        EditableTerrain(int terrainType) {
            this.terrainType = terrainType;
        }

        /** @return the {@link Terrains} type this places, meaningless for {@link #CLEAR} */
        public int terrainType() {
            return terrainType;
        }
    }

    public TerrainCommand(Server server, TWGameManager gameManager) {
        super(server,
              gameManager,
              "terrain",
              Messages.getString("Gamemaster.cmd.terrain.help"),
              Messages.getString("Gamemaster.cmd.terrain.longName"));
    }

    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(new CoordXArgument(X, Messages.getString("Gamemaster.cmd.x")),
              new CoordYArgument(Y, Messages.getString("Gamemaster.cmd.y")),
              new EnumArgument<>(TERRAIN,
                    Messages.getString("Gamemaster.cmd.terrain.terrain"),
                    EditableTerrain.class,
                    EditableTerrain.WATER),
              new IntegerArgument(LEVEL,
                    Messages.getString("Gamemaster.cmd.terrain.level"),
                    HexEditHandler.REMOVE_TERRAIN_LEVEL,
                    MAX_TERRAIN_LEVEL,
                    1));
    }

    @Override
    protected void runCommand(int connId, Arguments args) {
        // the dialog shows hex coordinates the way the board does, counting from one, while the board itself counts
        // from zero
        Coords coords = new Coords((int) args.get(X).getValue() - 1, (int) args.get(Y).getValue() - 1);
        EditableTerrain terrain = (EditableTerrain) args.get(TERRAIN).getValue();
        int level = (int) args.get(LEVEL).getValue();
        int boardId = getGameManager().getGame().getBoard().getBoardId();
        String gamemasterName = gamemasterName(connId);

        String refusal = (terrain == EditableTerrain.CLEAR)
              ? getGameManager().hexEditHandler().clearHex(coords, boardId, gamemasterName)
              : getGameManager().hexEditHandler()
                    .setTerrain(coords, boardId, terrain.terrainType(), level, gamemasterName);

        if (refusal != null) {
            server.sendServerChat(connId, Messages.getString("Gamemaster.cmd.terrain.refused", refusal));
        }
    }

    /**
     * @return the name to credit the edit to in the report, falling back to the server's own name when the command did
     *       not come from a player connection
     */
    private String gamemasterName(int connId) {
        var gamemaster = server.getPlayer(connId);
        return (gamemaster != null) ? gamemaster.getName() : Messages.getString("Gamemaster.toast.serverName");
    }
}

/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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


package megamek.common.board.postprocess;

import megamek.common.Configuration;
import megamek.common.board.Board;
import megamek.common.loaders.MapSettings;
import megamek.common.options.IGameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.util.BoardUtilities;
import megamek.common.util.fileUtils.MegaMekFile;

public class TWBoardTransformer {

    private TWBoardTransformer() {
        // Utility class
    }

    /**
     * Builds the final game board from the given map settings without modifying them: surprise board picks are
     * resolved on an internal copy, so the caller's settings are left untouched. Use this for previews and
     * simulations where the settings must remain as the player configured them.
     *
     * @param sourceMapSettings    the map settings to build the board from; not modified
     * @param planetaryConditions  the planetary conditions whose weather is baked into the board's terrain
     * @param options              the game options (bridge CF, random basements) applied to the board
     *
     * @return the assembled game board
     */
    public static Board instantiateBoard(MapSettings sourceMapSettings, PlanetaryConditions planetaryConditions,
          IGameOptions options) {
        var mapSettings = MapSettings.getInstance(sourceMapSettings);
        return instantiateBoardResolvingSettings(mapSettings, planetaryConditions, options);
    }

    /**
     * Builds the final game board from the given map settings, resolving surprise board picks <i>into</i> the
     * given settings: any {@code [SURPRISE]} selection is replaced with the concrete board name that was chosen.
     * The server uses this at game start so that the settings it keeps (and later logs and saves) record the
     * board that was actually played.
     *
     * @param mapSettings          the map settings to build the board from; surprise picks are resolved in place
     * @param planetaryConditions  the planetary conditions whose weather is baked into the board's terrain
     * @param options              the game options (bridge CF, random basements) applied to the board
     *
     * @return the assembled game board
     */
    public static Board instantiateBoardResolvingSettings(MapSettings mapSettings,
          PlanetaryConditions planetaryConditions, IGameOptions options) {
        Board newBoard = setupBoardFromMapSettings(mapSettings);
        setupOptions(options, newBoard);
        setupPlanetaryConditions(planetaryConditions, newBoard);
        return newBoard;
    }

    private static Board setupBoardFromMapSettings(MapSettings mapSettings) {
        mapSettings.chooseSurpriseBoards();
        Board[] sheetBoards = new Board[mapSettings.getMapWidth() * mapSettings.getMapHeight()];
        rotateBoards(mapSettings, sheetBoards);
        return BoardUtilities.combine(mapSettings.getBoardWidth(),
              mapSettings.getBoardHeight(), mapSettings.getMapWidth(),
              mapSettings.getMapHeight(), sheetBoards,
              mapSettings.getMedium());
    }

    private static void setupPlanetaryConditions(PlanetaryConditions planetaryConditions, Board newBoard) {
        if (planetaryConditions.isTerrainAffected()) {
            BoardUtilities.addWeatherConditions(newBoard, planetaryConditions.getWeather(),
                  planetaryConditions.getWind());
        }
    }

    private static void setupOptions(IGameOptions options, Board newBoard) {
        if (options.getOption(OptionsConstants.BASE_BRIDGE_CF).intValue() > 0) {
            newBoard.setBridgeCF(options.getOption(OptionsConstants.BASE_BRIDGE_CF).intValue());
        }

        if (!options.booleanOption(OptionsConstants.BASE_RANDOM_BASEMENTS)) {
            newBoard.setRandomBasementsOff();
        }
    }

    private static void rotateBoards(MapSettings mapSettings, Board[] sheetBoards) {
        for (int i = 0; i < (mapSettings.getMapWidth() * mapSettings.getMapHeight()); i++) {
            sheetBoards[i] = new Board();
            // Need to set map type prior to loading to adjust foliage height, etc.
            sheetBoards[i].setType(mapSettings.getMedium());
            String name = mapSettings.getBoardsSelectedVector().get(i);
            boolean isRotated = false;
            if (name.startsWith(Board.BOARD_REQUEST_ROTATION)) {
                // only rotate boards with an even width
                if ((mapSettings.getBoardWidth() % 2) == 0) {
                    isRotated = true;
                }
                name = name.substring(Board.BOARD_REQUEST_ROTATION.length());
            }
            if (name.startsWith(MapSettings.BOARD_GENERATED)
                  || (mapSettings.getMedium() == MapSettings.MEDIUM_SPACE)) {
                sheetBoards[i] = BoardUtilities.generateRandom(mapSettings);
            } else {
                sheetBoards[i].load(new MegaMekFile(Configuration.boardsDir(), name + ".board").getFile());
                BoardUtilities.flip(sheetBoards[i], isRotated, isRotated);
            }
        }
    }

}

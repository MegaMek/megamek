/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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
 *
 */

package megamek.common.board.postprocess;

import megamek.common.Board;
import megamek.common.Configuration;
import megamek.common.MapSettings;
import megamek.common.options.IGameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryconditions.PlanetaryConditions;
import megamek.common.util.BoardUtilities;
import megamek.common.util.fileUtils.MegaMekFile;

import java.util.ArrayList;
import java.util.List;

public class TWBoardTransformer {

    private TWBoardTransformer() {
        // Utility class
    }

    public static Board instantiateBoard(MapSettings sourceMapSettings, PlanetaryConditions planetaryConditions, IGameOptions options) {
        var mapSettings = MapSettings.getInstance(sourceMapSettings);
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
        if (options.getOption(OptionsConstants.BASE_BRIDGECF).intValue() > 0) {
            newBoard.setBridgeCF(options.getOption(OptionsConstants.BASE_BRIDGECF).intValue());
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

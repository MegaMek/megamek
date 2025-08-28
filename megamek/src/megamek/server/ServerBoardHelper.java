/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.server;

import static megamek.client.ui.panels.phaseDisplay.lobby.LobbyUtility.extractSurpriseMaps;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import megamek.MMConstants;
import megamek.common.Configuration;
import megamek.common.Hex;
import megamek.common.board.Board;
import megamek.common.board.BoardDimensions;
import megamek.common.loaders.MapSettings;
import megamek.common.util.BoardUtilities;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.logging.MMLogger;

public class ServerBoardHelper {
    private static final MMLogger LOGGER = MMLogger.create(ServerBoardHelper.class);

    /**
     * Returns a list of path names of available boards of the size set in the given mapSettings. The path names are
     * minus the '.board' extension and relative to the boards data directory.
     */
    public static List<String> scanForBoards(MapSettings mapSettings) {
        BoardDimensions boardSize = mapSettings.getBoardSize();
        List<String> result = new ArrayList<>();

        // Scan the Megamek boards directory
        File boardDir = Configuration.boardsDir();
        scanForBoardsInDir(boardDir, "", boardSize, result);

        // Scan the userData directory
        boardDir = new File(Configuration.userdataDir(), Configuration.boardsDir().toString());
        if (boardDir.isDirectory()) {
            scanForBoardsInDir(boardDir, "", boardSize, result);
        }

        result.sort(String::compareTo);
        return result.stream().map(ServerBoardHelper::backToForwardSlash).collect(Collectors.toList());
    }

    private static String backToForwardSlash(String path) {
        return path.replace("\\", "/");
    }

    /**
     * Scans the given boardDir directory for map boards of the given size and returns them by adding them to the given
     * boards list. Removes the .board extension.
     */
    private static void scanForBoardsInDir(final File boardDir, final String basePath,
          final BoardDimensions dimensions, List<String> boards) {
        if (boardDir == null) {
            throw new IllegalArgumentException("must provide searchDir");
        } else if (basePath == null) {
            throw new IllegalArgumentException("must provide basePath");
        } else if (dimensions == null) {
            throw new IllegalArgumentException("must provide dimensions");
        } else if (boards == null) {
            throw new IllegalArgumentException("must provide boards");
        }

        String[] fileList = boardDir.list();
        if (fileList != null) {
            for (String filename : fileList) {
                File filePath = new MegaMekFile(boardDir, filename).getFile();
                if (filePath.isDirectory()) {
                    scanForBoardsInDir(filePath, basePath + File.separator + filename, dimensions, boards);
                } else {
                    if (filename.endsWith(".board")) {
                        if (Board.boardIsSize(filePath, dimensions)) {
                            boards.add(basePath + File.separator + filename.substring(0, filename.lastIndexOf(".")));
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns the game map as it is currently set in the map settings tab. When onlyFixedBoards is true, all Generated
     * and Surprise boards are replaced by empty boards, otherwise they are filled with a generated or a choice of the
     * surprise maps.
     */
    public static Board getPossibleGameBoard(MapSettings mapSettings, boolean onlyFixedBoards) {
        mapSettings.replaceBoardWithRandom(MapSettings.BOARD_SURPRISE);
        Board[] sheetBoards = new Board[mapSettings.getMapWidth() * mapSettings.getMapHeight()];

        var boardsIterator = mapSettings.getBoardsSelectedVector().iterator();
        int i = 0;
        while (boardsIterator.hasNext()) {
            String name = boardsIterator.next();

            if (name == null || name.isEmpty()) {
                continue;
            }
            sheetBoards[i] = new Board();

            if ((name.startsWith(MapSettings.BOARD_GENERATED) || name.startsWith(MapSettings.BOARD_SURPRISE))
                  && onlyFixedBoards) {
                sheetBoards[i] = Board.createEmptyBoard(mapSettings.getBoardWidth(), mapSettings.getBoardHeight());
            } else if (name.startsWith(MapSettings.BOARD_GENERATED)
                  || (mapSettings.getMedium() == MapSettings.MEDIUM_SPACE)) {
                sheetBoards[i] = BoardUtilities.generateRandom(mapSettings);
            } else {
                boolean flipBoard = false;

                if (name.startsWith(MapSettings.BOARD_SURPRISE)) {
                    List<String> boardList = extractSurpriseMaps(name);
                    int rnd = (int) (Math.random() * boardList.size());
                    name = boardList.get(rnd);
                } else if (name.startsWith(Board.BOARD_REQUEST_ROTATION)) {
                    // only rotate boards with an even width
                    if ((mapSettings.getBoardWidth() % 2) == 0) {
                        flipBoard = true;
                    }
                    name = name.substring(Board.BOARD_REQUEST_ROTATION.length());
                }

                sheetBoards[i].load(new MegaMekFile(Configuration.boardsDir(),
                      name + MMConstants.CL_KEY_FILE_EXTENSION_BOARD).getFile());
                BoardUtilities.flip(sheetBoards[i], flipBoard, flipBoard);
            }
            i++;
        }

        Board finalBoard;
        try {
            finalBoard = BoardUtilities.combine(mapSettings.getBoardWidth(), mapSettings.getBoardHeight(),
                  mapSettings.getMapWidth(), mapSettings.getMapHeight(), sheetBoards, mapSettings.getMedium());
        } catch (IllegalArgumentException ex) {
            int totalWidth = mapSettings.getMapWidth() * mapSettings.getBoardWidth();
            int totalHeight = mapSettings.getMapHeight() * mapSettings.getBoardHeight();
            // Hit a failure while trying to read a custom map; log and return a blank map for now.
            LOGGER.warn("Failed to read one or map board files; using blank Board.");
            finalBoard = new Board(totalWidth, totalHeight);
            Hex[] resultData = new Hex[totalWidth * totalHeight];
            Arrays.fill(resultData, new Hex());
            finalBoard.newData(totalWidth, totalHeight, resultData, null);
        }
        return finalBoard;
    }

    private ServerBoardHelper() {}
}

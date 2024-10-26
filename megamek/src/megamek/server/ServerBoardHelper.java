/*
 * Copyright (c) 2021-2023 - The MegaMek Team. All Rights Reserved.
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
package megamek.server;

import megamek.MMConstants;
import megamek.common.Board;
import megamek.common.BoardDimensions;
import megamek.common.Configuration;
import megamek.common.MapSettings;
import megamek.common.util.BoardUtilities;
import megamek.common.util.fileUtils.MegaMekFile;

import static megamek.client.ui.swing.lobby.LobbyUtility.extractSurpriseMaps;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ServerBoardHelper {

    /**
     * Returns a list of path names of available boards of the size set in the given
     * mapSettings. The path names are minus the '.board' extension and relative to
     * the boards data directory.
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
     * Scans the given boardDir directory for map boards of the given size and
     * returns them by adding them to the given boards list. Removes the .board extension.
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
     * Returns the game map as it is currently set in the map settings tab.
     * When onlyFixedBoards is true, all Generated and Surprise boards are
     * replaced by empty boards, otherwise they are filled with a generated or
     * a choice of the surprise maps.
     */
    public static Board getPossibleGameBoard(MapSettings mapSettings, boolean onlyFixedBoards) {
        mapSettings.replaceBoardWithRandom(MapSettings.BOARD_SURPRISE);
        Board[] sheetBoards = new Board[mapSettings.getMapWidth() * mapSettings.getMapHeight()];
        List<Boolean> rotateBoard = new ArrayList<>();
        for (int i = 0; i < (mapSettings.getMapWidth() * mapSettings.getMapHeight()); i++) {
            sheetBoards[i] = new Board();

            String name = mapSettings.getBoardsSelectedVector().get(i);
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

                sheetBoards[i].load(new MegaMekFile(Configuration.boardsDir(), name + MMConstants.CL_KEY_FILEEXTENTION_BOARD).getFile());
                BoardUtilities.flip(sheetBoards[i], flipBoard, flipBoard);
            }
        }

        return BoardUtilities.combine(mapSettings.getBoardWidth(), mapSettings.getBoardHeight(),
                mapSettings.getMapWidth(), mapSettings.getMapHeight(), sheetBoards, rotateBoard,
                mapSettings.getMedium());
    }

    private ServerBoardHelper() { }
}
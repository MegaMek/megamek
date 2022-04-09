/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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

import megamek.common.Board;
import megamek.common.BoardDimensions;
import megamek.common.Configuration;
import megamek.common.MapSettings;
import megamek.common.util.fileUtils.MegaMekFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ServerBoardHelper {
    
    /**
     * Returns a list of path names of available boards of the size set in the given
     * mapSettings. The path names are minus the '.board' extension and relative to
     * the boards data directory.
     */
    public static ArrayList<String> scanForBoards(MapSettings mapSettings) {
        BoardDimensions boardSize = mapSettings.getBoardSize();
        ArrayList<String> result = new ArrayList<>();
        
        // Scan the Megamek boards directory
        File boardDir = Configuration.boardsDir();
        scanForBoardsInDir(boardDir, "", boardSize, result);
        
        // Scan the userData directory
        boardDir = new File(Configuration.userdataDir(), Configuration.boardsDir().toString());
        if (boardDir.isDirectory()) {
            scanForBoardsInDir(boardDir, "", boardSize, result);
        }
        
        result.sort(String::compareTo);
        return result;
    }
    
    /**
     * Scans the given boardDir directory for map boards of the given size and
     * returns them by adding them to the given boards list. Removes the .board extension.
     */
    static List<String> scanForBoardsInDir(final File boardDir, final String basePath,
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
        return boards;
    }
}

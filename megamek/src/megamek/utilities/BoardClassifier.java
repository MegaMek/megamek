/*
 * Copyright (c) 2021 - The Megamek Team. All Rights Reserved.
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
package megamek.utilities;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import megamek.common.Board;
import megamek.common.BoardDimensions;
import megamek.common.Configuration;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.utilities.BoardsTagger.Tags;

/**
 * This class scans the boards directory and allows the selection of a random 
 * board based on given size criteria.
 * @author NickAragua
 */
public class BoardClassifier {
    private static BoardClassifier instance;    
    
    // boards, grouped by tag
    private Map<Tags, List<String>> boardsByTag = new HashMap<>();
    // boards, grouped by height
    private Map<Integer, List<String>> boardsByHeight = new HashMap<>();
    // boards, grouped by width
    private Map<Integer, List<String>> boardsByWidth = new HashMap<>();
    
    // function that maps full board paths to partial board paths
    private Map<String, String> boardPaths = new HashMap<>();
    
    public Map<Tags, List<String>> getBoardsByTag() {
        return boardsByTag;
    }

    public void setBoardsByTag(Map<Tags, List<String>> boardsByTag) {
        this.boardsByTag = boardsByTag;
    }

    public Map<Integer, List<String>> getBoardsByHeight() {
        return boardsByHeight;
    }

    public void setBoardsByHeight(Map<Integer, List<String>> boardsByHeight) {
        this.boardsByHeight = boardsByHeight;
    }

    public Map<Integer, List<String>> getBoardsByWidth() {
        return boardsByWidth;
    }

    public void setBoardsByWidth(Map<Integer, List<String>> boardsByWidth) {
        this.boardsByWidth = boardsByWidth;
    }

    public Map<String, String> getBoardPaths() {
        return boardPaths;
    }

    public void setBoardPaths(Map<String, String> boardPaths) {
        this.boardPaths = boardPaths;
    }

    /**
     * Get a singleton instance of the Board Classifier, initializing if necessary
     */
    public static BoardClassifier getInstance() {
        if (instance == null) {
            instance = new BoardClassifier();
            instance.scanForBoards();
        }
        
        return instance;
    }
    
    /**
     * Scans through the boards in the default and user-data directories, 
     * and populates the "boardsBy[x]" indices. WARNING: This is a very time-consuming 
     * operation, so use very sparingly.
     */
    private void scanForBoards() {
        // Scan the MegaMek boards directory
        File boardDir = Configuration.boardsDir();
        scanForBoardsInDir(boardDir, "");
        
        // Scan the userData directory
        boardDir = new File(Configuration.userdataDir(), Configuration.boardsDir().toString());
        if (boardDir.isDirectory()) {
            scanForBoardsInDir(boardDir, "");
        }
    }
    
    /**
     * Scans the given boardDir directory for map boards and populates the "boardsBy[x]" indices.
     * Potentially a very time-consuming operation.
     */
    private void scanForBoardsInDir(final File boardDir, final String basePath) {
        String[] fileList = boardDir.list();
        if (fileList != null) {
            for (String filename : fileList) {
                File filePath = new MegaMekFile(boardDir, filename).getFile();
                
                // this is a "partial" board path that omits the "data/boards" part of the path
                // and is usable 
                String partialBoardPath = basePath + File.separator + filename;
                if (filePath.isDirectory()) {
                    scanForBoardsInDir(filePath, partialBoardPath);
                } else {
                    if (filename.endsWith(".board")) {
                        BoardDimensions dimension = Board.getSize(filePath);
                        
                        if (dimension != null) {
                            getBoardsByHeight().putIfAbsent(dimension.height(), new ArrayList<>());
                            getBoardsByWidth().putIfAbsent(dimension.width(), new ArrayList<>());
                            
                            getBoardsByHeight().get(dimension.height()).add(filePath.getPath());
                            getBoardsByWidth().get(dimension.width()).add(filePath.getPath());
                        }
                        
                        for (String tagString : Board.getTags(filePath)) {
                            Tags tag = Tags.parse(tagString);
                            getBoardsByTag().putIfAbsent(tag, new ArrayList<>());
                            getBoardsByTag().get(tag).add(filePath.getPath());
                        }
                        
                        getBoardPaths().put(filePath.getPath(), partialBoardPath);
                    }
                }
            }
        }
    }
    
    /**
     * Gets a list of all the boards that are within xVariance of width and yVariance of height
     */
    public List<String> getMatchingBoards(int width, int height, int xVariance, int yVariance,
            List<Tags> tags) {
        List<String> xBoards = new ArrayList<>();
        List<String> yBoards = new ArrayList<>();
        List<String> tagBoards = new ArrayList<>();
        
        for (int widthIndex = width - xVariance; widthIndex <= width + xVariance; widthIndex++) {
            if (getBoardsByWidth().containsKey(widthIndex)) {
                xBoards.addAll(getBoardsByWidth().get(widthIndex));
            }
        }
        
        for (int heightIndex = height - yVariance; heightIndex <= height + yVariance; heightIndex++) {
            if (getBoardsByWidth().containsKey(heightIndex)) {
                yBoards.addAll(getBoardsByWidth().get(heightIndex));
            }
        }
        
        if (!boardsByTag.isEmpty()) {
            for (Tags tag : tags) {
                if (getBoardsByTag().containsKey(tag)) {
                    tagBoards.addAll(getBoardsByTag().get(tag));
                }
            }
        }
        
        if (tagBoards.isEmpty()) {
            return xBoards.stream().distinct().filter(yBoards::contains).collect(Collectors.toList());
        } else {
            return xBoards.stream().distinct()
                    .filter(yBoards::contains)
                    .filter(tagBoards::contains)
                    .collect(Collectors.toList());
        }
    }
}

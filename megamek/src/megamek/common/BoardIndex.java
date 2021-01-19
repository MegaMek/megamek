package megamek.common;

        /*
        * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
        * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
        *
        *  This program is free software; you can redistribute it and/or modify it
        *  under the terms of the GNU General Public License as published by the Free
        *  Software Foundation; either version 2 of the License, or (at your option)
        *  any later version.
        *
        *  This program is distributed in the hope that it will be useful, but
        *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
        *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
        *  for more details.
        */

import megamek.MegaMek;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

// This is to create an index of the maps available, and allow for querying
public class BoardIndex {

    private static final String SEPARATOR_COMMA = ",";
    private static final String FILE_SUFFIX_BOARD = ".board";
    
    private List<String> allDirs = new ArrayList<>();
    private List<String[]> index = new ArrayList<>();
    private String indexDir = "";
    
    // Constructor, stores the custom map directory, finds the directories in that location, and runs the indexing
    public BoardIndex(String mapDir) {
        indexDir = mapDir;
        indexDirs(mapDir);
        runIndex();
    }
    
    public static void main () {
        
    }
    
    // Returns the index array list.
    public List<String[]> getIndex() { return index; }
    
    // Returns the directory list
    public List<String> getDirs() { return allDirs; }

    // Add a directory to the directory list, after checking if it already exists in the list
    public void addDir(String dir) { 
        if (!allDirs.contains(dir)) {
            allDirs.add(dir);
        } else {
            MegaMek.getLogger().info("Did not add directory, as it already exists");
        }
    } 
    
    // Get a random board of specified width and height.
    public String getRandom(int width, int height) {
        List<Integer> randMap = new ArrayList<>();

        for (int i = 0; i < index.size(); i++) {
            String[] board = index.get(i);
            if ((Integer.parseInt(board[1]) == width) && (Integer.parseInt(board[2]) == height)) {
                randMap.add(i);
            }
        }

        String randomMap = index.get(randMap.get(Compute.randomInt(randMap.size())))[4] + FILE_SUFFIX_BOARD;
        return randomMap;
    }
    
    // Clear the directory list
    public void clearDirs() { allDirs.clear(); }
        
    // Clear and re-run the index. Useful if maps have been added or directories added
    public void reIndex() {
        index.clear();
        runIndex();
    }
    
    // Index the directories. This calls addDir to add them and check for duplicates.
    private void indexDirs(String mapDir) {
        addDir("");

        MegaMek.getLogger().info("Begin map directory index" + LocalDateTime.now());
        
        if (mapDir != null) {
            for (String customDir : mapDir.split(SEPARATOR_COMMA, -1)) {
                addDir(customDir);
            }
        }
        File dirList = new File(Configuration.boardsDir(), "");
        String[] directories = dirList.list(new FilenameFilter() {
            @Override
            public boolean accept(File current, String name) {
                return new File(current, name).isDirectory();
            }
        });

        for (int i = 0; i < directories.length; i++) {
            addDir(directories[i]);
        }

        MegaMek.getLogger().info("Directory listing complete. " + LocalDateTime.now());
    }
    
    // Create the index.
    private void runIndex() {
        
        MegaMek.getLogger().info("Begin board indexing " + LocalDateTime.now());


        for (String dir: allDirs) {
            File curDir = new File(Configuration.boardsDir(), dir);
            if (curDir.exists()) {
                for (String file : curDir.list()) {
                    if (file.toLowerCase(Locale.ROOT).endsWith(FILE_SUFFIX_BOARD)) {
                        try {
                            File readIndex = new File(curDir, file);
                            BufferedReader ir = new BufferedReader(new FileReader(readIndex));
                            String getFirstLine = ir.readLine();
                            String strVal[] = new String[3];
                            String strStore[] = new String[5];
                            strVal = getFirstLine.split(" ");
                            strStore[0] = strVal[0];
                            strStore[1] = strVal[1];
                            strStore[2] = strVal[2];
                            strStore[3] = readIndex.getPath();
                            strStore[4] = dir + "/" + file.substring(0, file.length() - FILE_SUFFIX_BOARD.length());
                            index.add(strStore);
                        } catch (IOException e) {
                            MegaMek.getLogger().info("Failed to read file " + file);
                        }
                    }
                }
            }
        }

        MegaMek.getLogger().info("All indexes read" + LocalDateTime.now());
        MegaMek.getLogger().info("Map Listing total:" + index.size());
    }
}

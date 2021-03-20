package megamek.common;

/*
 * MegaMek - Copyright (C) 2021 - The MegaMek Team
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */

import megamek.MegaMek;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * This is to create an index of the maps available, and allow for querying
 */
public class BoardIndex {
    private static final String SEPARATOR_COMMA = ",";
    private static final String FILE_SUFFIX_BOARD = ".board";
    private List<String> allDirs = new ArrayList<>();
    private String indexDir = "";
    private List<Integer> iWidth = new ArrayList<>();
    private List<Integer> iHeight = new ArrayList<>();
    private List<String> iPath = new ArrayList<>();
    private List<String> iName = new ArrayList<>();
    private List<Integer> index = new ArrayList<>();
    
    /**
     * Constructor, stores the custom map directory, 
     * finds the directories in that location, and runs the indexing
     * @param mapDir
     */
    public BoardIndex(String mapDir) {
        indexDir = mapDir;
        indexDirs(mapDir);
        runIndex();
    }

    /**
     * Blank as not needed
     */
    public static void main () {
        
    }

    /**
     * Returns the Index
     * @return
     */
    public List<Integer> getIndex() { 
        return index; 
    }

    /**
     * Returns the directory listing
     * @return
     */
    public List<String> getDirs() { 
        return allDirs; 
    }

    /**
     * Add a directory to the directory list, after checking if it already exists in the list
     * @param dir
     */
    public void addDir(String dir) {
        if (!allDirs.contains(dir)) {
            allDirs.add(dir);
            MegaMek.getLogger().info("Directory " + dir + " added. " + allDirs.size() + " directories");
        } else {
            MegaMek.getLogger().info("Did not add directory, as it already exists");
        }
    }
    
    /**
     * Get the height of a map, takes index number
     * @param i
     * @return
     */
    public int getHeight(int i) {
        return iHeight.get(i);
    }

    /**
     * Get the width of a map, takes index number
     * @param i
     * @return
     */
    public int getWidth(int i) {
        return iWidth.get(i);
    }

    /**
     * Get the name of a map, takes index number
     * @param i
     * @return
     */
    public String getName(int i) {
        return iName.get(i);
    }

    /**
     * Get the path of a map. takes index number
     * @param i
     * @return
     */
    public String getPath(int i) {
        return iPath.get(i);
    }

    /**
     * Get the index number of a value. takes index value.
     * These should always be the same, but just in case
     * @param i
     * @return
     */
    public String getIndexNum(int i) {
        return index.get(i).toString();
    }
    
    /**
     * Make a limited size index pointing to the right values
     * @param width
     * @param height
     * @return
     */
    public List<Integer> getIndexBySize(int width, int height) {
        List<Integer> retIndex = new ArrayList<>();

        MegaMek.getLogger().info("Checking for maps " + width + " " + height);
        for (int i = 0; i < index.size(); i++) {
            if (iHeight.get(i) == height && iWidth.get(i) == width) {
                retIndex.add(index.get(i));
            }
        }
        
        return retIndex;
    }
    
    /**
     * Get a random board of specified width and height.
     * @param width
     * @param height
     * @return
     */
    public String getRandom(int width, int height) {
        List<Integer> randMap = new ArrayList<>();
        randMap = getIndexBySize(width, height);
                
        String randomMap = iName.get(randMap.get(Compute.randomInt(randMap.size()))) + FILE_SUFFIX_BOARD;
        return randomMap;
    }

    /**
     * Clear the directory list
     */
    public void clearDirs() { 
        allDirs.clear(); 
    }

    /**
     * Clear and re-run the index. Useful if maps have been added or directories added
     */
    public void reIndex() {
        index.clear();
        iWidth.clear();
        iName.clear();
        iHeight.clear();
        iPath.clear();
        runIndex();
    }

    /**
     * Index the directories. This calls addDir to add them and check for duplicates.
     * @param mapDir
     */
    private void indexDirs(String mapDir) {
        addDir("");

        MegaMek.getLogger().debug("Begin map directory index");
        
        if (mapDir != null) {
            for (String customDir : mapDir.split(SEPARATOR_COMMA, -1)) {
                addDir(customDir);
            }
        }
        File dirList = new File(Configuration.boardsDir(), "");
        
        getAllSubFoldersInPath(dirList);
        
        MegaMek.getLogger().debug("Directory listing complete.");
    }

    /**
     * Find all the subdirectories
     * @param path
     */
    private void getAllSubFoldersInPath(File path) {
        File[] files = path.listFiles();
        String tempFile = "";
        
        try {
            for (File file : files) {
                if (file.isDirectory()) {
                    tempFile = file.getPath().substring(Configuration.boardsDir().getPath().length());
                    addDir(tempFile);
                    getAllSubFoldersInPath(file);
                }
            }
        }
        catch (Exception e) {
            MegaMek.getLogger().info("Could not open directories to check");
        }
    }

    /**
     * Create the index
     */
    private void runIndex() {
        MegaMek.getLogger().debug("Begin board indexing");

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
                            strVal = getFirstLine.split(" ");
                            index.add(index.size()+1);
                            iWidth.add(Integer.parseInt(strVal[1]));
                            iHeight.add(Integer.parseInt(strVal[2]));
                            iPath.add(readIndex.getPath());
                            iName.add(dir + "/" + file.substring(0, file.length() - FILE_SUFFIX_BOARD.length()));
                        } catch (IOException e) {
                            MegaMek.getLogger().info("Failed to read file " + file, e);
                        }
                    }
                }
            }
        }


        MegaMek.getLogger().debug("All indexes read");
        MegaMek.getLogger().info("Index Map Listing total: " + index.size());
    }
}

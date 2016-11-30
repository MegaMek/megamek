/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Nicholas Walczak (walczak@cs.umn.edu)
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

package megamek.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import megamek.common.Board;
import megamek.common.Configuration;

/**
 * This class provides a utility to read in all of the boards and check their validity.
 * 
 * @author arlith
 *
 */
public class BoardsValidator {
    
    int numBoardErrors = 0;
    
    public BoardsValidator() {
        
    }

    /**
     * Recursively scans the supplied File for any boards and validates them.  If the supplied File is a directory, then
     * all files in that directory are recursively scanned.
     *
     * @param file
     * @throws IOException
     */
    private void scanForBoards(File file) throws IOException {
        if (file.isDirectory()) {
            String fileList[] = file.list();
            for (String filename : fileList) {
                File filepath = new File(file, filename);
                if (filepath.isDirectory()) {
                    scanForBoards(new File(file, filename));
                } else {
                    validateBoard(filepath);
                }
            }
        } else {
            validateBoard(file);
        }
    }

    /**
     * Check whether the supplied file is a valid board file or not.  Ignores files that don't end in .board.  Any
     * errors are logged to System.out.
     * 
     * @param boardFile
     * @throws FileNotFoundException
     */
    private void validateBoard(File boardFile) throws FileNotFoundException {
        // If this isn't a board, ignore it
        if (!boardFile.toString().endsWith(".board")) {
            return;
        }
        
        java.io.InputStream is = new FileInputStream(boardFile);
        StringBuffer errBuff = new StringBuffer();
        Board b = new Board();
        b.load(is, errBuff);
        if (errBuff.length() > 0) {
            System.out.println("Found Invalid Board! Board: " + boardFile);
            System.out.println(errBuff);
            numBoardErrors++;
        }
    }

    /**
     * 
     * @param args
     */
    public static void main(String[] args) {
        try {
            File boardDir = Configuration.boardsDir();
            BoardsValidator validator = new BoardsValidator();
            
            validator.scanForBoards(boardDir);
            System.out.println("Found " + validator.numBoardErrors + " boards with errors!");
        }catch (IOException e){
            System.out.println("IOException!");
            e.printStackTrace();
        }
    }
}

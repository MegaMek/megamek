/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Nicholas Walczak (walczak@cs.umn.edu)
 *  Copyright (c) 2020 - The MegaMek team
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
package megamek.utils;

import java.io.*;
import java.util.List;

import megamek.MegaMek;
import megamek.common.Board;
import megamek.common.Configuration;
import megamek.common.IBoard;
import megamek.common.util.MegaMekFile;

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
     *
     * @param args the arguments to apply to the BoardValidator (ignored)
     */
    public static void main(String[] args) {
        BoardsValidator validator = new BoardsValidator();

        List<File> fileList = new MegaMekFile(Configuration.boardsDir().getName()).getFiles();
        for (File file : fileList) {
            validator.validateBoard(file);
        }
        if (validator.numBoardErrors > 0) {
            MegaMek.getLogger().warning(BoardsValidator.class, "main",
                    "Found " + validator.numBoardErrors + " boards with errors!");
        } else {
            MegaMek.getLogger().info(BoardsValidator.class, "main",
                    "No boards with errors found in board validation.");
        }
    }

    /**
     * Check whether the supplied file is a valid board file or not. It ignores any files that do
     * not end with
     * errors are logged to System.out.
     * 
     * @param boardFile the board file to check
     */
    private void validateBoard(File boardFile) {
        // If this isn't a board, ignore it
        if (!boardFile.toString().endsWith(IBoard.BOARD_FILE_EXTENSION)) {
            return;
        }

        try (InputStream is = new FileInputStream(boardFile)) {
            StringBuffer errBuff = new StringBuffer();
            Board board = new Board();
            board.load(is, errBuff, false);

            if (errBuff.length() > 0) {
                MegaMek.getLogger().error(BoardsValidator.class, "validateBoard",
                        "Found Invalid Board! Board: " + boardFile.toString());
                MegaMek.getLogger().error(BoardsValidator.class, "validateBoard",
                        errBuff.toString());
                numBoardErrors++;
            }
        } catch (Exception e) {
            MegaMek.getLogger().error(BoardsValidator.class, "main",
                    "Error in loading board " + boardFile);
            MegaMek.getLogger().error(BoardsValidator.class, "validateBoard", e);
        }
    }
}

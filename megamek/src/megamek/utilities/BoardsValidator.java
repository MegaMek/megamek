/*
 * MegaMek - Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Nicholas Walczak (walczak@cs.umn.edu)
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
 */
package megamek.utilities;

import megamek.common.Board;
import megamek.common.Configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * This class provides a utility to read in all the boards and check their validity.
 * 
 * @author arlith
 */
public class BoardsValidator {
    
    private int numBoardErrors = 0;
    private boolean isVerbose;
    
    /**
     * Sets a value indicating whether a full listing of errors
     * should be printed when validating a board.
     * @param b {@code true} if the specific errors for each board
     *          should be printed, otherwise {@code false} for just
     *          the file name.
     */
    public void setIsVerbose(boolean b) {
        isVerbose = b;
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
            String[] fileList = file.list();
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
     * Check whether the supplied file is a valid board file or not. Ignores files that don't end
     * in .board. Any errors are logged to System.out.
     *
     * @param boardFile the board file to check
     */
    private void validateBoard(File boardFile) throws IOException {
        // If this isn't a board, ignore it
        if (!boardFile.toString().endsWith(".board")) {
            return;
        }
        
        try (InputStream is = new FileInputStream(boardFile)) {
            List<String> errors = new ArrayList<>();
            Board b = new Board();

            try {
                b.load(is, errors, false);
            } catch (Exception e) {
                numBoardErrors++;
                System.err.println("Found invalid board: " + boardFile);
                e.printStackTrace();
                return;
            }

            if (!errors.isEmpty()) {
                numBoardErrors++;
                System.err.println("Found invalid board: " + boardFile);
                if (isVerbose) {
                    System.err.println(String.join("\n", errors));
                }
            }
        }
    }

    /**
     * 
     * @param args
     */
    public static void main(String[] args) {
        Args a = Args.parse(args);
        if (a.showHelp) {
            System.out.println("Usage: java -cp MegaMek.jar megamek.utils.BoardsValidator [OPTIONS] [paths]");
            System.out.println();
            System.out.println("    -q, --quiet       Only print invalid file names.");
            System.out.println("    -?, -h, --help    Show this message and quit.");
            System.out.println();
            System.out.println("Examples:");
            System.out.println();
            System.out.println("Validate every board in the ./data subdirectory of the");
            System.out.println("current working directory:");
            System.out.println();
            System.out.println("    > java -cp MegaMek.jar megamek.utils.BoardsValidator");
            System.out.println();
            System.out.println("Validate a given board:");
            System.out.println();
            System.out.println("    > java -cp MegaMek.jar megamek.utils.BoardsValidator SomeFile.board");
            System.out.println();
            System.out.println("Validate a directory of boards:");
            System.out.println();
            System.out.println("    > java -cp MegaMek.jar megamek.utils.BoardsValidator SomeFiles");
            System.out.println();
            System.exit(2);
            return;
        }

        BoardsValidator validator = new BoardsValidator();
        validator.setIsVerbose(!a.isQuiet);

        try {
            if (a.paths.isEmpty()) {
                File boardDir = Configuration.boardsDir();
                validator.scanForBoards(boardDir);
            } else {
                for (String path : a.paths) {
                    validator.scanForBoards(new File(path));
                }
            }

            System.out.println("Found " + validator.numBoardErrors + " boards with errors!");
            System.exit(validator.numBoardErrors > 0 ? 1 : 0);
        } catch (IOException e) {
            System.out.println("IOException!");
            e.printStackTrace();
            System.exit(64);
        }
    }

    private static class Args {
        public boolean showHelp;
        public boolean isQuiet;
        public List<String> paths = new ArrayList<>();

        private static Args parse(String[] args) {
            Args a = new Args();
            for (String arg : args) {
                if ("-?".equals(arg) || "-h".equals(arg) || "--help".equals(arg)) {
                    a.showHelp = true;
                } else if ("-q".equals(arg) || "--quiet".equals(arg)) {
                    a.isQuiet = true;
                } else {
                    a.paths.add(arg);
                }
            }
            return a;
        }
    }
}

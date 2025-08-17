/*
  Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2013 Nicholas Walczak (walczak@cs.umn.edu)
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import megamek.common.board.Board;
import megamek.common.Configuration;
import megamek.logging.MMLogger;

/**
 * This class provides a utility to read in all the boards and check their validity.
 *
 * @author arlith
 */
public class BoardsValidator {

    private int numBoardErrors = 0;
    private boolean isVerbose;

    private static final MMLogger LOGGER = MMLogger.create(BoardsValidator.class);

    /**
     * Sets a value indicating whether a full listing of errors should be printed when validating a board.
     *
     * @param verbose {@code true} if the specific errors for each board should be printed, otherwise {@code false} for
     *                just the file name.
     */
    public void setIsVerbose(boolean verbose) {
        isVerbose = verbose;
    }

    /**
     * Recursively scans the supplied File for any boards and validates them. If the supplied File is a directory, then
     * all files in that directory are recursively scanned.
     *
     * @param file File to search for.
     *
     * @throws IOException In the event the file or directory doesn't exist.
     */
    private void scanForBoards(File file) throws IOException {
        if (file.isDirectory()) {
            String[] fileList = file.list();

            if (fileList == null) {
                return;
            }

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
     * Check whether the supplied file is a valid board file or not. Ignores files that don't end in .board. Any errors
     * are logged to System.out.
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
            Board board = new Board();

            if (isVerbose) {
                String message = String.format("Checking %s", boardFile);
                LOGGER.info(message);
            }

            try {
                board.load(is, errors, false);
            } catch (Exception e) {
                numBoardErrors++;
                String errorMessage = String.format("Unable to load and parse board: %s", boardFile);
                LOGGER.error(e, errorMessage);
                return;
            }

            if (!errors.isEmpty()) {
                numBoardErrors++;
                String errorMessage = String.format("Found invalid board: %s", boardFile);
                LOGGER.error(errorMessage);

                if (isVerbose) {
                    errorMessage = String.join("\n", errors);
                    LOGGER.error(errorMessage);
                }
            }
        }
    }

    /**
     * Usage: java -cp MegaMek.jar megamek.utilities.BoardsValidator [OPTIONS] [paths]
     * <p>
     * -q, --quiet  Only print invalid file names.<br /> -?, -h, --help    Show this message and quit.
     * <p>
     * Examples:
     * <p>
     * Validate every board in the ./data subdirectory of the current working directory: <code>java -cp MegaMek.jar
     * megamek.utilities.BoardsValidator</code>
     * <p>
     * Validate a given board: <code>java -cp MegaMek.jar megamek.utilities.BoardsValidator SomeFile.board</code>
     * <p>
     * Validate a directory of boards: <code>java -cp MegaMek.jar megamek.utilities.BoardsValidator SomeFiles</code>
     *
     * @param args Arguments as detailed above and in the help text below
     */
    public static void main(String[] args) {
        BoardsValidator validator = new BoardsValidator();
        Args parsedArgs = new Args(args);

        if (parsedArgs.showHelp()) {
            String helpOutput = """
                  Usage: java -cp MegaMek.jar megamek.utilities.BoardsValidator [OPTIONS] [paths]
                  
                      -q, --quiet       Only print invalid file names.
                      -?, -h, --help    Show this message and quit.
                  
                  Examples:
                  
                  Validate every board in the ./data subdirectory of the current working directory:
                      > java -cp MegaMek.jar megamek.utilities.BoardsValidator
                  
                  Validate a given board:
                      > java -cp MegaMek.jar megamek.utilities.BoardsValidator SomeFile.board
                  
                  Validate a directory of boards:
                      > java -cp MegaMek.jar megamek.utilities.BoardsValidator SomeFiles
                  """;
            LOGGER.info(helpOutput);
            System.exit(0);
            return;
        }

        validator.setIsVerbose(!parsedArgs.isQuiet());
        List<String> paths = parsedArgs.paths();

        try {
            if (paths.isEmpty()) {
                File boardDir = Configuration.boardsDir();
                validator.scanForBoards(boardDir);
            } else {
                for (String path : paths) {
                    validator.scanForBoards(new File(path));
                }
            }

            String statusMessage = String.format("Found %d boards with errors.", validator.numBoardErrors);
            LOGGER.info(statusMessage);
            System.exit(validator.numBoardErrors > 0 ? 1 : 0);
        } catch (IOException ioException) {
            LOGGER.error(ioException, "IO Exception Occurred {}", ioException.getMessage());
            System.exit(64);
        }
    }

    private static class Args {
        private boolean showHelp = false;
        private boolean isQuiet;
        private final List<String> paths = new ArrayList<>();

        public Args(String[] args) {
            for (String arg : args) {
                if ("-?".equals(arg) || "-h".equals(arg) || "--help".equals(arg)) {
                    showHelp = true;
                } else if ("-q".equals(arg) || "--quiet".equals(arg)) {
                    isQuiet = true;
                } else {
                    paths.add(arg);
                }
            }
        }

        boolean showHelp() {
            return showHelp;
        }

        boolean isQuiet() {
            return isQuiet;
        }

        List<String> paths() {
            return paths;
        }
    }
}

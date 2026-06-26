/*
  Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2013 Nicholas Walczak (walczak@cs.umn.edu)
 * Copyright (C) 2016-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Vector;

import megamek.client.ui.tileset.TilesetManager;
import megamek.common.Configuration;
import megamek.common.util.StringUtil;
import megamek.logging.MMLogger;

/**
 * This class provides a utility to read in a HexTileSet and test to make sure all images are accessible
 *
 * @author arlith
 */
public class HexSetTest {
    private final static MMLogger LOGGER = MMLogger.create(HexSetTest.class);

    private static class StringCompCaseInsensitive implements Comparator<String> {
        @Override
        public int compare(String arg0, String arg1) {
            return arg0.compareToIgnoreCase(arg1);
        }
    }

    /**
     * Reads the *set file in the given directory and filename.  It looks at the given image file and prints a message
     * if the file cannot be opened and if the case does not match.
     *
     */
    private static void testFile(File dir, String filename, int incDepth) throws IOException {
        LOGGER.info("Checking file: {}", filename);

        // make input stream for board
        Reader bufferedReader = new BufferedReader(new FileReader(new File(dir, filename)));
        // read board, looking for "size"
        StreamTokenizer streamTokenizer = new StreamTokenizer(bufferedReader);

        streamTokenizer.eolIsSignificant(true);
        streamTokenizer.commentChar('#');
        streamTokenizer.quoteChar('"');
        streamTokenizer.wordChars('_', '_');
        while (streamTokenizer.nextToken() != StreamTokenizer.TT_EOF) {
            String terrain;
            String theme;
            String imageName;
            if ((streamTokenizer.ttype == StreamTokenizer.TT_WORD)
                  && (streamTokenizer.sval.equals("base")
                  || streamTokenizer.sval.equals("super")
                  || streamTokenizer.sval.equals("ortho"))) {

                streamTokenizer.nextToken();
                streamTokenizer.nextToken();
                terrain = streamTokenizer.sval;
                streamTokenizer.nextToken();
                theme = streamTokenizer.sval;
                streamTokenizer.nextToken();
                imageName = streamTokenizer.sval;
                Vector<String> filenames = StringUtil.splitString(imageName, ";");
                for (String entryFile : filenames) {
                    String entryName;
                    if ((theme == null) || theme.isBlank()) {
                        entryName = terrain;
                    } else {
                        entryName = terrain + " " + theme;
                    }
                    testImageName(dir, entryFile, entryName);
                }
            } else if ((streamTokenizer.ttype == StreamTokenizer.TT_WORD) && streamTokenizer.sval.equals("include")) {
                streamTokenizer.nextToken();
                incDepth++;
                if (incDepth < 100) {
                    String incFile = streamTokenizer.sval;
                    testFile(dir, incFile, incDepth);
                }
            }
        }
        bufferedReader.close();
        incDepth--;
    }

    private static void testImageName(File dir, String imageName, String entryName) throws IOException {
        File imgFile = new File(dir, imageName);

        boolean exactMatch = imgFile.exists() && imgFile.getCanonicalPath().endsWith(imgFile.getName());
        if (!exactMatch) {
            LOGGER.info("Error with {}: ", entryName);
            String[] dirFiles = imgFile.getParentFile().list();
            if (dirFiles != null) {
                Arrays.sort(dirFiles, new StringCompCaseInsensitive());
                int result = Arrays.binarySearch(dirFiles, imgFile.getName(), new StringCompCaseInsensitive());
                if (result >= 0) {
                    LOGGER.warn("Case mismatch! Entry Path: {}", imageName);
                } else {
                    LOGGER.warn("File not found! Entry Path: {}", imageName);
                }
            } else {
                LOGGER.warn("Files not found! Entry Path: {}", imageName);
            }
        }
    }


    public static void main(String[] args) {
        try {
            File hexesDir = Configuration.hexesDir();

            String[] tilesetFiles = Configuration.hexesDir()
                  .list((directory, fileName) -> fileName.endsWith(".tileset"));
            if (tilesetFiles != null) {
                Arrays.sort(tilesetFiles);
                for (String tileset : tilesetFiles) {
                    testFile(hexesDir, tileset, 0);
                }
            }
            // Create the default hex set, so we can validate it as well
            testFile(hexesDir, TilesetManager.FILENAME_DEFAULT_HEX_SET, 0);

        } catch (IOException e) {
            LOGGER.error(e, "IO Exception: {}", e.getMessage());
        }
    }
}

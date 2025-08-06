/*
  Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2013 Nicholas Walczak (walczak@cs.umn.edu)
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
import megamek.common.Terrain;
import megamek.common.util.StringUtil;

/**
 * This class provides a utility to read in a HexTileSet and test to make sure all images are accessible
 *
 * @author arlith
 */
public class HexSetTest {

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
     * @param dir
     * @param filename
     *
     * @throws IOException
     */
    private static void testFile(File dir, String filename, int incDepth)
          throws IOException {
        System.out.println("Checking file: " + filename);

        // make inpustream for board
        Reader r = new BufferedReader(new FileReader(new File(dir, filename)));
        // read board, looking for "size"
        StreamTokenizer st = new StreamTokenizer(r);

        st.eolIsSignificant(true);
        st.commentChar('#');
        st.quoteChar('"');
        st.wordChars('_', '_');
        @SuppressWarnings("unused")
        int bases, supers, orthos;
        bases = supers = orthos = 0;
        while (st.nextToken() != StreamTokenizer.TT_EOF) {
            @SuppressWarnings("unused")
            int elevation = 0;
            // int levity = 0;
            String terrain = null;
            String theme = null;
            String imageName = null;
            if ((st.ttype == StreamTokenizer.TT_WORD)
                  && (st.sval.equals("base") || st.sval.equals("super") ||
                  st.sval.equals("ortho"))) {
                boolean bas = st.sval.equals("base");
                boolean sup = st.sval.equals("super");
                boolean ort = st.sval.equals("ortho");

                if (st.nextToken() == StreamTokenizer.TT_NUMBER) {
                    elevation = (int) st.nval;
                } else {
                    elevation = Terrain.WILDCARD;
                }
                st.nextToken();
                terrain = st.sval;
                st.nextToken();
                theme = st.sval;
                st.nextToken();
                imageName = st.sval;
                // add to list
                if (bas) {
                    bases++;
                }
                if (sup) {
                    supers++;
                }
                if (ort) {
                    orthos++;
                }
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
            } else if ((st.ttype == StreamTokenizer.TT_WORD) && st.sval.equals("include")) {
                st.nextToken();
                incDepth++;
                if (incDepth < 100) {
                    String incFile = st.sval;
                    testFile(dir, incFile, incDepth);
                }
            }
        }
        r.close();
        System.out.println("\n");
        incDepth--;
    }

    private static void testImageName(File dir, String imageName,
          String entryName) throws IOException {
        File imgFile = new File(dir, imageName);

        boolean exactmatch = imgFile.exists()
              && imgFile.getCanonicalPath().endsWith(imgFile.getName());
        if (!exactmatch) {
            System.out.print("Error with " + entryName + ": ");
            String[] dirFiles = imgFile.getParentFile().list();
            if (dirFiles != null) {
                Arrays.sort(dirFiles, new StringCompCaseInsensitive());
                int result = Arrays.binarySearch(dirFiles, imgFile.getName(),
                      new StringCompCaseInsensitive());
                if (result >= 0) {
                    System.out.println("Case mismatch!  Entry Path: "
                          + imageName);
                } else {
                    System.out.println("File not found! Entry Path: "
                          + imageName);
                }
            } else {
                System.out.println("File not found! Entry Path: " + imageName);
            }
        }
    }


    public static void main(String[] args) {
        try {
            File hexesDir = Configuration.hexesDir();

            String[] tilesetFiles = Configuration.hexesDir().list(
                  (directory, fileName) -> fileName.endsWith(".tileset"));
            if (tilesetFiles != null) {
                Arrays.sort(tilesetFiles);
                for (String tileset : tilesetFiles) {
                    testFile(hexesDir, tileset, 0);
                }
            }
            // Create the default hexset, so we can validate it as well
            testFile(hexesDir, TilesetManager.FILENAME_DEFAULT_HEX_SET, 0);

        } catch (IOException e) {
            System.out.println("IOException!");
            e.printStackTrace();
        }
    }
}

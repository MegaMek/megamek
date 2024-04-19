/*
 * Copyright (c) 2022, 2024 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.swing.tileset;

import java.io.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import megamek.common.Configuration;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.common.util.fileUtils.StandardTextfileStreamTokenizer;
import org.apache.logging.log4j.LogManager;

import static megamek.client.ui.swing.tileset.MechTileset.*;

/**
 * This class provides a utility to read in the current MechSet and test to make
 * sure all images are accessible
 *
 * @author arlith
 */
public class MechSetTest {

    private MechSetTest() { }

    private static class StringCompCaseInsensitive implements Comparator<String> {
        @Override
        public int compare(String arg0, String arg1) {
            return arg0.compareToIgnoreCase(arg1);
        }
    }

    static boolean isValidLine(List<String> tokens) {
        return !tokens.contains(null)
                && (isValidContentLine(tokens) || StandardTextfileStreamTokenizer.isValidIncludeLine(tokens));
    }

    static boolean isValidContentLine(List<String> tokens) {
        return (tokens.size() == 3) && (tokens.get(0).equals(CHASSIS_KEY) || tokens.get(0).equals(MODEL_KEY));
    }

    /**
     * Reads the *set file in the given directory and filename.  It looks at the
     * given image file and prints a message if the file cannot be opened and
     * if the case does not match.
     */
    private static void testFile(File dir, String filename) throws IOException {
        System.out.println();
        System.out.println("Listing Errors for " + filename);
        try (Reader r = new BufferedReader(new FileReader(new MegaMekFile(dir, filename).getFile()))) {
            StandardTextfileStreamTokenizer tokenizer = new StandardTextfileStreamTokenizer(r);
            while (true) {
                List<String> tokens = tokenizer.getLineTokens();
                if (tokenizer.isFinished()) {
                    break;
                } else {
                    if (isValidLine(tokens)) {
                        if (tokens.get(0).equals(StandardTextfileStreamTokenizer.INCLUDE_KEY)) {
                            try {
                                testFile(dir, tokens.get(1));
                            } catch (IOException e) {
                                LogManager.getLogger().error("... failed: {}.", e.getMessage(), e);
                            }
                        } else {
                            testImageName(dir, tokens.get(2), tokens.toString());
                        }
                    } else {
                        System.out.println("Malformed line: " + tokens);
                    }
                }
            }
        }
    }

    private static void testImageName(File dir, String imageName, String line) throws IOException {
        File imgFile = new File(dir, imageName);
        boolean exactmatch = imgFile.exists() && imgFile.getCanonicalPath().endsWith(imgFile.getName());
        if (!exactmatch) {
            System.out.print("Error in [" + line + "]: ");
            String[] dirFiles = imgFile.getParentFile().list();
            if (dirFiles == null) {
                System.out.println("File is not a directory! Entry Path: " + imageName);
                return;
            }
            Arrays.sort(dirFiles, new StringCompCaseInsensitive());
            int result = Arrays.binarySearch(dirFiles, imgFile.getName(), new StringCompCaseInsensitive());
            if (result >= 0) {
                System.out.println("Case mismatch! Path: " + imageName);
            } else {
                System.out.println("File not found! Path: " + imageName);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        File mechDir = Configuration.unitImagesDir();
        testFile(mechDir, "mechset.txt");

        File wreckDir = new File(Configuration.unitImagesDir(), TilesetManager.DIR_NAME_WRECKS);
        testFile(wreckDir, "wreckset.txt");
    }
}

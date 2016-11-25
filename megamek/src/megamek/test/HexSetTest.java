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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Vector;

import megamek.client.ui.swing.TilesetManager;
import megamek.common.Configuration;
import megamek.common.ITerrain;
import megamek.common.util.StringUtil;


/**
 * This class provides a utility to read in a HexTileSet and test to make
 * sure all images are accessible
 * 
 * @author arlith
 *
 */
public class HexSetTest {

    private static class StringCompCaseInsensitive implements
            Comparator<String> {
        @Override
        public int compare(String arg0, String arg1) {
            return arg0.compareToIgnoreCase(arg1);
        }
    }
    
    /**
     * Reads the *set file in the given directory and filename.  It looks at the
     * given image file and prints a message if the file cannot be opened and
     * if the case does not match.
     * 
     * @param dir
     * @param filename
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
                        st.sval.equals("ortho"))) { //$NON-NLS-1$ //$NON-NLS-2$
                boolean bas = st.sval.equals("base"); //$NON-NLS-1$
                boolean sup = st.sval.equals("super"); //$NON-NLS-1$
                boolean ort = st.sval.equals("ortho"); //$NON-NLS-1$

                if (st.nextToken() == StreamTokenizer.TT_NUMBER) {
                    elevation = (int) st.nval;
                } else {
                    elevation = ITerrain.WILDCARD;
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
                Vector<String> filenames = StringUtil.splitString(imageName,
                        ";"); //$NON-NLS-1$
                for (String entryFile : filenames) {
                    String entryName;
                    if ((theme == null) || theme.equals("")) {
                        entryName = terrain;
                    } else {
                        entryName = terrain + " " +  theme;
                    }
                    testImageName(dir, entryFile, entryName);
                }
            } else if ((st.ttype == StreamTokenizer.TT_WORD) &&
                    st.sval.equals("include")) {
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
            String dirFiles[] = imgFile.getParentFile().list();
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
                    new FilenameFilter() {
                        public boolean accept(File directory, String fileName) {
                            return fileName.endsWith(".tileset");
                        }
                    });
            if (tilesetFiles != null) {
                Arrays.sort(tilesetFiles);
                for (String tileset : tilesetFiles) {
                    testFile(hexesDir, tileset, 0);
                }
            }
            // Create the default hexset, so we can validate it as well
            TilesetManager.createDefaultHexSet();
            testFile(hexesDir, TilesetManager.FILENAME_DEFAULT_HEX_SET, 0);

        }catch (IOException e){
            System.out.println("IOException!");
            e.printStackTrace();
        }
    }
}

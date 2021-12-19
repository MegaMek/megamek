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
import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.Arrays;
import java.util.Comparator;

import megamek.client.ui.swing.tileset.TilesetManager;
import megamek.common.Configuration;


/**
 * This class provides a utility to read in the current MechSet and test to make
 * sure all images are accessible
 * 
 * @author arlith
 *
 */
public class MechSetTest {

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
    private static void testFile(File dir, String filename) throws IOException {
        System.out.println("Listing Errors for " + filename);
        // make inpustream for board
        Reader r = new BufferedReader(new FileReader(new File(dir, filename)));
        // read board, looking for "size"
        StreamTokenizer st = new StreamTokenizer(r);
        st.eolIsSignificant(true);
        st.commentChar('#');
        st.quoteChar('"');
        st.wordChars('_', '_');
        while (st.nextToken() != StreamTokenizer.TT_EOF) {
            String name = null;
            String imageName = null;
            String entryName = null;
            if ((st.ttype == StreamTokenizer.TT_WORD)
                    && st.sval.equalsIgnoreCase("include")) {
                st.nextToken();
                name = st.sval;
                testFile(dir, name);
            } else if ((st.ttype == StreamTokenizer.TT_WORD)
                    && st.sval.equalsIgnoreCase("chassis")) {
                st.nextToken();
                name = st.sval;
                st.nextToken();
                imageName = st.sval;
                entryName = "entry: chassis " + name + " ";
                if (imageName == null) {
                    System.out.println("Error with entry " + entryName + " : no image specified!");
                } else {
                    testImageName(dir, imageName, entryName);
                }
            } else if ((st.ttype == StreamTokenizer.TT_WORD)
                    && st.sval.equalsIgnoreCase("exact")) {
                st.nextToken();
                name = st.sval;
                st.nextToken();
                imageName = st.sval;
                entryName = "entry: exact " + name + " ";
                testImageName(dir, imageName, entryName);
            }
        }
        System.out.println("\n\n");
    }
    
    private static void testImageName(File dir, String imageName,
            String entryName) throws IOException {
        File imgFile = new File(dir, imageName);
        
        boolean exactmatch = imgFile.exists()
                && imgFile.getCanonicalPath().endsWith(imgFile.getName());
        if (!exactmatch) {
            System.out.print("Error with " + entryName + ": ");
            String[] dirFiles = imgFile.getParentFile().list();
            if (dirFiles == null) {
                System.out.println("File is not a directory! Entry Path: " + imageName);
                return;
            }
            Arrays.sort(dirFiles, new StringCompCaseInsensitive());
            int result = Arrays.binarySearch(dirFiles, imgFile.getName(),
                    new StringCompCaseInsensitive());
            if (result >= 0) {
                System.out.println("Case mismatch!  Entry Path: " + imageName);
            } else {
                System.out.println("File not found! Entry Path: " + imageName);
            }
        }
    }
    
    
    public static void main(String[] args) {
        try {
            File mechDir = Configuration.unitImagesDir();
            File wreckDir = new File(Configuration.unitImagesDir(),
                    TilesetManager.DIR_NAME_WRECKS);
            String mechset = "mechset.txt";
            String wreckset = "wreckset.txt";
            
            testFile(mechDir, mechset);
            testFile(wreckDir, wreckset);
            
        } catch (IOException e) {
            System.out.println("IOException!");
            e.printStackTrace();
        }
    }
}

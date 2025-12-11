/*
 * Copyright (C) 2000-2024 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2002-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Vector;

import megamek.SuiteConstants;
import megamek.common.board.CubeCoords;
import megamek.logging.MMLogger;

/**
 * buildingBlock is based on a file format I used in an online game. The original was written in PHP, this one is more
 * robust, and written in Java.
 *
 * @author Nate Rowden
 * @since April 2, 2002, 1:57 PM
 */
public class BuildingBlock {
    private static final MMLogger logger = MMLogger.create(BuildingBlock.class);

    private Vector<String> rawData;
    private static final char comment = '#';

    /**
     * Creates new empty buildingBlock
     */
    public BuildingBlock() {
        // for holding the file we read/parse
        rawData = new Vector<>();
    }

    /**
     * Creates a new buildingBlock and fills it with the data in the String[] array.
     *
     * @param data This is most useful for storing one block file inside another...but <I>data</I> can be an array of
     *             anything...such as comments.
     */
    public BuildingBlock(String[] data) {
        rawData = new Vector<>();
        rawData = makeVector(data);
    }

    /**
     * Creates a new buildingBlock and fills it with the Vector.
     *
     * @param data The Vector can be filled with anything.
     */
    public BuildingBlock(Vector<String> data) {
        rawData = data;
    }

    public BuildingBlock(InputStream is) {
        rawData = new Vector<>();
        readInputStream(is);
    }

    public void readInputStream(InputStream is) {
        String data;
        // empty the rawData holder...
        rawData.clear();

        try (InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
              BufferedReader br = new BufferedReader(isr)) {
            // read the file till can't read anymore...
            while (br.ready()) {
                data = br.readLine();
                if (data == null) {
                    continue;
                }
                data = data.trim();

                // check for blank lines & comment lines... and don't add them to the rawData if
                // they are
                if (!data.isBlank() && !data.startsWith("" + BuildingBlock.comment)) {
                    rawData.add(data);
                }
            }
        } catch (Exception ignored) {
            logger.error("An Exception occurred while attempting to read a BuildingBlock stream.");
        }

    }

    /**
     * Finds the starting index of a block. This is used by the class to locate data, but is a public function that can
     * be useful if you know what you want to do with the <CODE>rawData</CODE> Vector.
     *
     * @param blockName The name of the data block to locate.
     *
     * @return Returns the start index of the block data. Or -1 if not found.
     */
    public int findStartIndex(String blockName) {
        String line;
        int startIndex = -1;

        // Translate the block name to a key.
        String key = '<' + blockName + '>';

        // look for the block...
        for (int lineNum = 0; lineNum < rawData.size(); lineNum++) {
            line = rawData.get(lineNum);

            // look for "<blockName>"
            try {
                if ((line.length() >= 3) && line.equalsIgnoreCase(key)) {
                    startIndex = ++lineNum;
                    break;
                }
            } catch (Exception ex) {
                logger.error(ex,
                      "Was looking for {} and caught an Exception parsing line \n\"{}\" \nat rawData index number {}",
                      key,
                      line,
                      lineNum);
            }
        }
        return startIndex;
    }

    /**
     * Finds the starting index of a block. This is used by the class to locate data, but is a public function that can
     * be useful if you know what you want to do with the <CODE>rawData</CODE> Vector.
     *
     * @param blockName The name of the data block to locate.
     *
     * @return Returns the end index of the block data. Or -1 if not found.
     */
    public int findEndIndex(String blockName) {
        String line;
        int endIndex = -1;

        // Translate the block name to a key.
        String key = "<" + '/' + blockName + '>';

        // look for the block...
        for (int lineNum = 0; lineNum < rawData.size(); lineNum++) {
            line = rawData.get(lineNum);

            // look for "</blockName>"
            try {
                if ((line.length() >= 3) && line.equalsIgnoreCase(key)) {
                    endIndex = lineNum;
                    break;
                }
            } catch (Exception ex) {
                logger.error(ex,
                      "Was looking for {} and caught an Exception parsing line \n\"{}\" \nwith rawData index number {}",
                      key,
                      line,
                      lineNum);
            }
        }
        return endIndex;
    }

    /**
     * Gets data from inside a block.
     *
     * @param blockName The name of the block to grab the data from.
     *
     * @return Returns an array of data.
     */
    public String[] getDataAsString(String blockName) {
        String[] data;
        int startIndex, endIndex;

        startIndex = findStartIndex(blockName);

        endIndex = findEndIndex(blockName);

        if ((startIndex == -1) || (endIndex == -1)) {

            data = new String[1];
            data[0] = "";
            return data;
        }

        // calculate the size of our data array by subtracting the two indexes
        // ...
        int size = endIndex - startIndex;

        if (size == 0) {
            // data = new String[size + 1]; // add one so we always have at
            // least a size 1 array...
            data = new String[1];
            data[0] = "";
            return data;
        }

        data = new String[size];

        int dataRecord = 0;

        // fill up the data array with the raw data we want...
        for (int rawRecord = startIndex; rawRecord < endIndex; rawRecord++) {
            data[dataRecord] = rawData.get(rawRecord);
            dataRecord++;
        }
        return data; // hand back the goods...
    }

    public int[] getDataAsInt(String blockName) {
        int[] data;
        int startIndex, endIndex;

        startIndex = findStartIndex(blockName);

        endIndex = findEndIndex(blockName);

        if ((startIndex == -1) || (endIndex == -1)) {
            data = new int[1];
            return data;
        }

        // calculate the size of our data array by subtracting the two indexes...

        int size = endIndex - startIndex;

        if (size == 0) {
            // data = new int[size + 1]; // add one, so we always have at least a size 1
            // array...
            data = new int[1];
            return data;
        }
        data = new int[size];

        int dataRecord = 0;

        // fill up the data array with the raw data we want...
        for (int rawRecord = startIndex; rawRecord < endIndex; rawRecord++) {
            try {
                // Bug with people placing ',' in the fuel strings like 18,000
                // Should probably change this to a method to weed out all
                // non-numeric
                // variables but this is the most common.
                String rawString = rawData.get(rawRecord);
                if (rawString.indexOf(',') >= 0) {
                    rawString = rawString.replaceAll(",", "");
                }
                data[dataRecord] = Integer.parseInt(rawString);
                dataRecord++;
            } catch (NumberFormatException ex) {
                data[0] = 0;
                logger.error(ex, "getDataAsInt(\"{}\") failed.", blockName);
            }
        }
        return data; // hand back the goods...
    }

    public float[] getDataAsFloat(String blockName) {

        float[] data;
        int startIndex, endIndex;

        startIndex = findStartIndex(blockName);

        endIndex = findEndIndex(blockName);

        if ((startIndex == -1) || (endIndex == -1)) {

            data = new float[1];
            data[0] = 0;
            return data;

        }

        // calculate the size of our data array by subtracting the two indexes
        // ...

        int size = endIndex - startIndex;

        if (size == 0) {
            // data = new float[size + 1]; // add one so we always have at least
            // a
            // size 1 array...
            data = new float[1];
            data[0] = 0;
            return data;
        }
        data = new float[size];

        int dataRecord = 0;

        // fill up the data array with the raw data we want...
        for (int rawRecord = startIndex; rawRecord < endIndex; rawRecord++) {
            try {
                data[dataRecord] = Float.parseFloat(rawData.get(rawRecord));
                dataRecord++;
            } catch (NumberFormatException ex) {
                data[0] = 0;
                logger.error("getDataAsFloat(\"{}\") failed.", blockName);
            }
        }

        return data; // hand back the goods...
    }

    public double[] getDataAsDouble(String blockName) {
        double[] data;
        int startIndex, endIndex;

        startIndex = findStartIndex(blockName);
        endIndex = findEndIndex(blockName);
        if ((startIndex == -1) || (endIndex == -1)) {
            return new double[] { 0 };
        }

        // calculate the size of our data array by subtracting the two indexes
        int size = endIndex - startIndex;

        if (size == 0) {
            return new double[] { 0 };
        }
        data = new double[size];
        int dataRecord = 0;

        // fill up the data array with the raw data we want...
        for (int rawRecord = startIndex; rawRecord < endIndex; rawRecord++) {
            try {
                data[dataRecord] = Double.parseDouble(rawData.get(rawRecord));
                dataRecord++;
            } catch (NumberFormatException ex) {
                data[0] = 0;
                logger.error("getDataAsDouble(\"{}\") failed.", blockName);
            }
        }
        return data; // hand back the goods...
    }

    public CubeCoords[] getDataAsCubeCoords(String blockName) {
        CubeCoords[] data;
        int startIndex, endIndex;

        startIndex = findStartIndex(blockName);
        endIndex = findEndIndex(blockName);
        if ((startIndex == -1) || (endIndex == -1)) {
            return new CubeCoords[] { CubeCoords.ZERO };
        }

        // calculate the size of our data array by subtracting the two indexes
        int size = endIndex - startIndex;

        if (size == 0) {
            return new CubeCoords[] { CubeCoords.ZERO };
        }
        data = new CubeCoords[size];
        int dataRecord = 0;

        // fill up the data array with the raw data we want...
        for (int rawRecord = startIndex; rawRecord < endIndex; rawRecord++) {
            try {
                String rawString = rawData.get(rawRecord);
                String[] parts = rawString.split(",");
                if (parts.length == 3) {
                    double q = Double.parseDouble(parts[0].trim());
                    double r = Double.parseDouble(parts[1].trim());
                    double s = Double.parseDouble(parts[2].trim());
                    data[dataRecord] = new CubeCoords(q, r, s);
                    dataRecord++;
                } else {
                    data[dataRecord] = CubeCoords.ZERO;
                    dataRecord++;
                    logger.error("getDataAsCubeCoords(\"{}\") failed to parse line: {}", blockName, rawString);
                }
            } catch (NumberFormatException ex) {
                data[dataRecord] = CubeCoords.ZERO;
                dataRecord++;
                logger.error(ex, "getDataAsCubeCoords(\"{}\") failed.", blockName);
            }
        }
        return data; // hand back the goods...
    }

    /**
     * Gets data from a block.
     *
     * @param blockName Name of the block to get data from.
     *
     * @return Returns the data as a Vector.
     */
    public List<String> getDataAsVector(String blockName) {
        int startIndex = findStartIndex(blockName);
        int endIndex = findEndIndex(blockName);

        if ((startIndex == -1) || (endIndex == -1)) {
            return new Vector<>();
        }

        List<String> data = new Vector<>();

        // fill up the data vector with the raw data we want...
        for (int rawRecord = startIndex; rawRecord < endIndex; rawRecord++) {
            data.add(rawData.get(rawRecord));
        }

        return data; // hand back the goods...
    }

    /**
     * Clears the <CODE>rawData</CODE> Vector and inserts a default comment and
     * <I>BlockVersion</I> information.
     */
    public void createNewBlock() {
        rawData.clear();
        writeBlockComment("Saved from version " + SuiteConstants.VERSION + " on " + LocalDate.now());
    }

    /**
     * @see #writeBlockData (String, Vector)
     */
    public boolean writeBlockData(String blockName, String blockData) {
        String[] temp = new String[1];
        temp[0] = blockData;

        return writeBlockData(blockName, makeVector(temp));
    }

    /**
     * @see #writeBlockData (String, Vector)
     */
    public boolean writeBlockData(String blockName, int blockData) {
        String[] temp = new String[1];
        temp[0] = "" + blockData;
        return writeBlockData(blockName, makeVector(temp));
    }

    /**
     * @see #writeBlockData (String, Vector)
     */
    public boolean writeBlockData(String blockName, int[] blockData) {
        String[] temp = new String[blockData.length];
        for (int c = 0; c < blockData.length; c++) {
            temp[c] = "" + blockData[c];
        }
        return writeBlockData(blockName, makeVector(temp));

    }

    /**
     * @see #writeBlockData (String, Vector)
     */
    public boolean writeBlockData(String blockName, float blockData) {
        String[] temp = new String[1];
        temp[0] = "" + blockData;
        return writeBlockData(blockName, makeVector(temp));
    }

    /**
     * @see #writeBlockData (String, Vector)
     */
    public boolean writeBlockData(String blockName, double blockData) {
        String[] temp = new String[1];
        temp[0] = "" + blockData;
        return writeBlockData(blockName, makeVector(temp));
    }

    /**
     * @see #writeBlockData (String, Vector)
     */
    public boolean writeBlockData(String blockName, float[] blockData) {
        String[] temp = new String[blockData.length];
        for (int c = 0; c < blockData.length; c++) {
            temp[c] = "" + blockData[c];
        }
        return writeBlockData(blockName, makeVector(temp));
    }

    /**
     * @see #writeBlockData (String, Vector)
     */
    public boolean writeBlockData(String blockName, CubeCoords[] blockData) {
        String[] temp = new String[blockData.length];
        for (int c = 0; c < blockData.length; c++) {
            temp[c] = blockData[c].q() + "," + blockData[c].r() + "," + blockData[c].s();
        }
        return writeBlockData(blockName, makeVector(temp));
    }

    /**
     * @see #writeBlockData (String, Vector)
     */
    public boolean writeBlockData(String blockName, String[] blockData) {
        return writeBlockData(blockName, makeVector(blockData));
    }

    /**
     * Writes a data block to the <CODE>rawData</CODE> vector.
     *
     * @param blockName Name of the block to be created.
     * @param blockData Data to be written inside the block.
     *
     * @return Returns true on success.
     */
    public boolean writeBlockData(String blockName, List<String> blockData) {
        rawData.add("<" + blockName + ">");

        for (String blockDatum : blockData) {
            rawData.add(blockDatum.trim());
        }

        rawData.add("</" + blockName + ">");
        rawData.add("");

        return true;
    }

    /**
     * Writes a comment.
     *
     * @param theComment The comment to be written.
     */
    public void writeBlockComment(String theComment) {
        rawData.add(BuildingBlock.comment + theComment);
    }

    /**
     * Writes the buildingBlock data to a file.
     *
     * @param file File to write. Overwrites existing files.
     */
    public void writeBlockFile(File file) {

        if (file.exists()) {
            if (!file.delete()) {
                logger.error("Unable to delete file with name {}", file);
                return;
            }
        }

        try (OutputStream fos = new FileOutputStream(file);
              OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
              BufferedWriter bw = new BufferedWriter(osw)) {
            for (String rawDatum : rawData) {
                bw.write(rawDatum);
                bw.newLine();
            }

            bw.flush();
        } catch (Exception e) {
            logger.error(e, "Unable to save block file {}", file.getPath());
        }

    }

    /**
     * Clears the <CODE>rawData</CODE> Vector.
     */
    public void clearData() {

        rawData.clear();

    }

    /**
     * Gets the size of the <CODE>rawData</CODE> Vector.
     *
     * @return Returns <CODE>rawData.size()</CODE>
     */
    public int dataSize() {

        return rawData.size();

    }

    /**
     * Converts a String array into a Vector.
     *
     * @param stringArray The String array to convert.
     *
     * @return Returns the Vector created by the String[]
     */
    public Vector<String> makeVector(String[] stringArray) {

        Vector<String> stringVector = new Vector<>();
        int c;

        try {

            for (c = 0; c < stringArray.length; c++) {

                // this should throw an exception when we hit the end
                stringArray[c] = stringArray[c].trim();

                stringVector.add(stringArray[c]);

            }

        } catch (ArrayIndexOutOfBoundsException e) {

            // we're done...return the vector
            return stringVector;

        }

        return stringVector; // just to make sure ; -?

    }

    /**
     * Useful if you want to copy one buildingBlock into another.
     *
     * @return Returns the <CODE>rawData</CODE> Vector.
     */
    public Vector<String> getVector() {

        return rawData;

    }

    /**
     * Gets all the data inside the <CODE>rawData</CODE> Vector.
     *
     * @return Returns the data as a String array
     */
    public String[] getAllDataAsString() {

        String[] theData = new String[rawData.size()];

        for (int c = 0; c < rawData.size(); c++) {

            theData[c] = rawData.get(c);

        }

        return theData;

    }

    /**
     * Just about the same as the <CODE>getVector()</CODE> command.
     *
     * @return Returns the <CODE>rawData</CODE> Vector.
     *
     * @see #getVector()
     */
    public Vector<String> getAllDataAsVector() {
        return rawData;

    }

    /**
     * Tells you the size of an array this thing returned by giving you the number in the [0] position.
     *
     * @param array The array to get the size of.
     *
     * @return Returns the number in the [0] position.
     */
    public int getReturnedArraySize(String[] array) {
        try {
            return Integer.parseInt(array[0]);
        } catch (Exception ignored) {
            // couldn't parse it...
            logger.error(
                  "Couldn't find array size at [0]...is this an array I returned...? Trying to find the size anyway...");
            return this.countArray(array);
        }
    }

    // for those of us who like doing things indirectly ; -?

    /**
     * @see #getReturnedArraySize (String[])
     */
    public int getReturnedArraySize(int[] array) {
        return array[0];
    }

    /**
     * @return Returns <CODE>array.size()</CODE>
     *
     * @see #getReturnedArraySize (String[])
     */
    public int getReturnedArraySize(Vector<Object> array) {
        return array.size();
    }

    /**
     * @see #getReturnedArraySize (String[])
     */
    public int getReturnedArraySize(float[] array) {
        try {
            return Integer.parseInt("" + array[0]);
        } catch (Exception ignored) {
            logger.error("Couldn't find array size at [0], is this an array I returned? Trying to find the size anyway");
            return this.countArray(array);
        }
    }

    /**
     * Counts the size of an array.
     *
     * @param array The array to count.
     *
     * @return Returns the array's size.
     */
    public int countArray(String[] array) {
        return array.length;
    }

    /**
     * @see #countArray(String[])
     */
    public int countArray(float[] array) {

        return array.length;
    }

    /**
     * @see #countArray(String[])
     */
    public int countArray(int[] array) {

        return array.length;
    }

    /**
     * Checks to see if a block exists...returns true or false
     */
    public boolean exists(String blockName) {

        if (findStartIndex(blockName) == -1) {
            return false;
        }
        return findEndIndex(blockName) != -1;
    }

    /**
     * Checks if a block exists and has data.
     */
    public boolean containsData(String blockName) {
        if (!exists(blockName)) {
            return false;
        }
        // If the end index is the next line after the start index,
        // the block is empty.
        // Otherwise, it contains data.
        int start = findStartIndex(blockName);
        int end = findEndIndex(blockName);
        return (end - start) >= 1;

    }
}

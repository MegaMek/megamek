/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package megamek.common.util;

import org.apache.logging.log4j.LogManager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Vector;

/**
 * buildingBlock is based on a file format I used in an online game. The
 * original was written in PHP, this one is more robust, and written in Java.
 * @author Nate Rowden
 * @since April 2, 2002, 1:57 PM
 */
public class BuildingBlock {

    private Vector<String> rawData;
    private static final int version = 1;
    private static final char comment = '#';

    /**
     * Creates new empty buildingBlock
     */
    public BuildingBlock() {
        // for holding the file we read/parse
        rawData = new Vector<>();
    }

    /**
     * Creates a new buildingBlock and fills it with the data in the String[]
     * array.
     *
     * @param data
     *            This is most useful for storing one block file inside
     *            another...but <I>data</I> can be an array of anything...such
     *            as comments.
     */
    public BuildingBlock(String[] data) {
        rawData = new Vector<>();
        rawData = makeVector(data);
    }

    /**
     * Creates a new buildingBlock and fills it with the Vector.
     *
     * @param data
     *            The Vector can be filled with anything.
     */
    public BuildingBlock(Vector<String> data) {
        rawData = data;
    }

    public BuildingBlock(InputStream is) {
        rawData = new Vector<>();
        readInputStream(is);
    }

    public boolean readInputStream(InputStream is) {
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
            LogManager.getLogger().error("An Exception occurred while attempting to read a BuildingBlock stream.");
            return false;
        }

        return true;
    }

    /**
     * Finds the starting index of a block. This is used by the class to locate
     * data, but is a public function that can be useful if you know what you
     * want to do with the <CODE>rawData</CODE> Vector.
     *
     * @param blockName
     *            The name of the data block to locate.
     * @return Returns the start index of the block data. Or -1 if not found.
     */
    public int findStartIndex(String blockName) {
        String line;
        int startIndex = -1;
        StringBuffer buf = new StringBuffer();

        // Translate the block name to a key.
        buf.append('<').append(blockName).append('>');
        String key = buf.toString();

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
                LogManager.getLogger().error(String.format(
                        "Was looking for %s and caught an Exception parsing line \n\"%s\" \nat rawData index number %s",
                        key, line, lineNum), ex);
            }
        }
        return startIndex;
    }

    /**
     * Finds the starting index of a block. This is used by the class to locate
     * data, but is a public function that can be useful if you know what you
     * want to do with the <CODE>rawData</CODE> Vector.
     *
     * @param blockName
     *            The name of the data block to locate.
     * @return Returns the end index of the block data. Or -1 if not found.
     */
    public int findEndIndex(String blockName) {
        String line;
        int endIndex = -1;
        StringBuffer buf = new StringBuffer();

        // Translate the block name to a key.
        buf.append('<').append('/').append(blockName).append('>');
        String key = buf.toString();

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
                LogManager.getLogger().error(String.format(
                        "Was looking for %s and caught an Exception parsing line \n\"%s\" \nwith rawData index number %s",
                        key, line, lineNum));
            }
        }
        return endIndex;
    }

    /**
     * Gets data from inside a block.
     *
     * @param blockName
     *            The name of the block to grab the data from.
     * @return Returns an array of data.
     */
    public String[] getDataAsString(String blockName) {
        String[] data;
        int startIndex = 0, endIndex = 0;

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
            data[dataRecord] = rawData.get(rawRecord).toString();
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
            // data = new int[size + 1]; // add one, so we always have at least a size 1 array...
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
                LogManager.getLogger().error("getDataAsInt(\"" + blockName + "\") failed.", ex);
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
                LogManager.getLogger().error("getDataAsFloat(\"" + blockName + "\") failed.");
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
            return new double[]{0};
        }

        // calculate the size of our data array by subtracting the two indexes
        int size = endIndex - startIndex;

        if (size == 0) {
            return new double[]{0};
        }
        data = new double[size];
        int dataRecord = 0;

        // fill up the data array with the raw data we want...
        for (int rawRecord = startIndex; rawRecord < endIndex; rawRecord++) {
            try {
                data[dataRecord] = Double.parseDouble(rawData.get(rawRecord));
                dataRecord ++;
            } catch (NumberFormatException ex) {
                data[0] = 0;
                LogManager.getLogger().error("getDataAsDouble(\"" + blockName + "\") failed.");
            }
        }
        return data; // hand back the goods...
    }

    /**
     * Gets data from a block.
     *
     * @param blockName
     *            Name of the block to get data from.
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
     *
     * @return Returns true on success.
     */
    public boolean createNewBlock() {
        rawData.clear();

        writeBlockComment("building block data file");
        this.writeBlockData("BlockVersion", "" + BuildingBlock.version);

        writeBlockComment("#Write the version number just in case...");
        this.writeBlockData("Version", "MAM0");

        return true;
    }

    /**
     * @see writeBlockData (String, Vector)
     */
    public boolean writeBlockData(String blockName, String blockData) {
        String[] temp = new String[1];
        temp[0] = blockData;

        return writeBlockData(blockName, makeVector(temp));
    }

    /**
     * @see writeBlockData (String, Vector)
     */
    public boolean writeBlockData(String blockName, int blockData) {
        String[] temp = new String[1];
        temp[0] = "" + blockData;
        return writeBlockData(blockName, makeVector(temp));
    }

    /**
     * @see writeBlockData (String, Vector)
     */
    public boolean writeBlockData(String blockName, int[] blockData) {
        String[] temp = new String[blockData.length];
        for (int c = 0; c < blockData.length; c++) {
            temp[c] = "" + blockData[c];
        }
        return writeBlockData(blockName, makeVector(temp));

    }

    /**
     * @see writeBlockData (String, Vector)
     */
    public boolean writeBlockData(String blockName, float blockData) {
        String[] temp = new String[1];
        temp[0] = "" + blockData;
        return writeBlockData(blockName, makeVector(temp));
    }

    /**
     * @see writeBlockData (String, Vector)
     */
    public boolean writeBlockData(String blockName, double blockData) {
        String[] temp = new String[1];
        temp[0] = "" + blockData;
        return writeBlockData(blockName, makeVector(temp));
    }


    /**
     * @see writeBlockData (String, Vector)
     */
    public boolean writeBlockData(String blockName, float[] blockData) {
        String[] temp = new String[blockData.length];
        for (int c = 0; c < blockData.length; c++) {
            temp[c] = "" + blockData[c];
        }
        return writeBlockData(blockName, makeVector(temp));
    }

    /**
     * @see writeBlockData (String, Vector)
     */
    public boolean writeBlockData(String blockName, String[] blockData) {
        return writeBlockData(blockName, makeVector(blockData));
    }

    /**
     * Writes a data block to the <CODE>rawData</CODE> vector.
     *
     * @param blockName
     *            Name of the block to be created.
     * @param blockData
     *            Data to be written inside the block.
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
     * @return Returns true on success.
     */
    public boolean writeBlockComment(String theComment) {
        rawData.add(BuildingBlock.comment + theComment);
        return true;
    }

    /**
     * Writes the buildingBlock data to a file.
     *
     * @param fileName File to write. Overwrites existing files.
     * @return true on success.
     */
    public boolean writeBlockFile(String fileName) {
        File file = new File(fileName);

        if (file.exists()) {
            if (!file.delete()) {
                LogManager.getLogger().error("Unable to delete file with name " + fileName);
                return false;
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
            LogManager.getLogger().error("Unable to save block file " + fileName, e);
            return false;
        }

        return true;
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
     * @param stringArray
     *            The String array to convert.
     * @return Returns the Vector created by the String[]
     */
    public Vector<String> makeVector(String[] stringArray) {

        Vector<String> newVect = new Vector<>();
        int c = 0;

        try {

            for (c = 0; c < stringArray.length; c++) {

                // this should throw an expection when we hit the end
                stringArray[c] = stringArray[c].trim();

                newVect.add(stringArray[c]);

            }

        } catch (ArrayIndexOutOfBoundsException e) {

            // we're done...return the vector
            return newVect;

        }

        return newVect; // just to make sure ; -?

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

            theData[c] = rawData.get(c).toString();

        }

        return theData;

    }

    /**
     * Just about the same as the <CODE>getVector()</CODE> command.
     *
     * @see getVector ()
     * @return Returns the <CODE>rawData</CODE> Vector.
     */
    public Vector<String> getAllDataAsVector() {

        Vector<String> theData = rawData; // can I jsut return this?

        return theData;

    }

    /**
     * Tells you the size of an array this thing returned by giving you the
     * number in the [0] position.
     *
     * @param array
     *            The array to get the size of.
     * @return Returns the number in the [0] position.
     */
    public int getReturnedArraySize(String[] array) {
        try {
            return Integer.parseInt(array[0]);
        } catch (Exception ignored) {
            // couldn't parse it...
            LogManager.getLogger().error("Couldn't find array size at [0]...is this an array I returned...? Trying to find the size anyway...");
            return this.countArray(array);
        }
    }

    // for those of us who like doing things indirectly ; -?
    /**
     * @see getReturnedArraySize (String[])
     */
    public int getReturnedArraySize(int[] array) {
        return array[0];
    }

    /**
     * @see getReturnedArraySize (String[])
     * @return Returns <CODE>array.size()</CODE>
     */
    public int getReturnedArraySize(Vector<Object> array) {
        return array.size();
    }

    /**
     * @see getReturnedArraySize (String[])
     */
    public int getReturnedArraySize(float[] array) {
        try {
            return Integer.parseInt("" + array[0]);
        } catch (Exception ignored) {
            LogManager.getLogger().error("Couldn't find array size at [0]...is this an array I returned...? Trying to find the size anyway...");
            return this.countArray(array);
        }
    }

    /**
     * Counts the size of an array.
     *
     * @param array
     *            The array to count.
     * @return Returns the array's size.
     */
    public int countArray(String[] array) {
        return array.length;
    }

    /**
     * @see countArray( String[] )
     */
    public int countArray(float[] array) {

        return array.length;
    }

    /**
     * @see countArray( String[] )
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
        if (findEndIndex(blockName) == -1) {
            return false;
        }

        return true;
    }
}

/*
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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

package megamek.common.util;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * This class represents a collection of item files present within a ZIP or JAR
 * file, categorized according to their directories. This collection will *not*
 * include all files inside of JAR and ZIP files that are located *inside* the
 * file (i.e. it ignores all nested JAR and ZIP files). <p/> Please note, from
 * this point on, the term "ZIP file" will be taken to mean "ZIP or JAR file",
 * because a JAR file is just a ZIP file with a manifest. <p/> Also note, the
 * author plans to eventually handle nested ZIP files, but the effort was deemed
 * to be unnecessary for the needs that led to the initial implementation.
 * Created on January 18, 2004
 * 
 * @author James Damour
 * @version 1
 */
public class ZippedItems implements Categorized {

    /**
     * The root category name.
     */
    private String rootName;

    /**
     * A map of the category names to maps of the item names to the items Please
     * note that this map includes the root category, if the root category
     * contains any items.
     */
    private TreeMap<String, Map<String, Object>> categories = new TreeMap<String, Map<String, Object>>(
            StringUtil.stringComparator());

    /**
     * The factory that will create <code>ItemFile</code>s for the contents
     * of the directory hierarchy.
     */
    private ItemFileFactory factory = null;

    /**
     * Identify when a name belongs to a ZIP file.
     * 
     * @param name - the <code>String</code> which may be a ZIP file name.
     *            This value must not be <code>null</code>.
     * @return <code>true</code> if the name is for a ZIP file.
     *         <code>false</code> if the name is not for a ZIP file.
     * @throws <code>IllegalArgumentException</code> if <code>name</code> is
     *             null.
     */
    public static boolean isZipName(String name) {

        // Convert the file name to upper case, and compare it to image
        // file extensions. Yeah, it's a bit of a hack, but whatever.
        String ucName = name.toUpperCase();
        return (ucName.endsWith("ZIP") || //$NON-NLS-1$
        ucName.endsWith("JAR")); //$NON-NLS-1$
    }

    /**
     * Create a categorized collection of all files within a ZIP file. Please
     * note, the name of any directories in the ZIP file will be added to the
     * root category name to create the name of the category names of the
     * directories.
     * 
     * @param zipFile - the <code>File</code> object for the ZIP file
     *            containing the image files. All files in this ZIP file, will
     *            be included in this collection, categorized by directory. This
     *            value must not be <code>null</code> and it must be a ZIP
     *            file.
     * @param categoryName - the <code>String</code> root category name for
     *            this collection. All sub-categories will include this name.
     *            This value may be <code>null</code> (it will be replaced).
     * @param itemFactory - the <code>ItemFileFactory</code> that will create
     *            <code>ItemFile</code>s for the contents of the directory.
     *            This value must not be <code>null</code>.
     * @throws <code>IllegalArgumentException</code> if <code>zipFile</code>
     *             or <code>itemFactory</code> is <code>null</code>.
     * @throws <code>ZipException</code> if there's a ZIP error opening
     *             <code>zipFile</code>.
     * @throws <code>IOException</code> if there's an IO error opening
     *             <code>zipFile</code>.
     */
    public ZippedItems(File zipFile, String categoryName,
            ItemFileFactory itemFactory) throws IllegalArgumentException,
            ZipException, IOException {

        // Declare local variables.
        Enumeration<?> entries = null;
        ZipEntry entry = null;
        String catName = null;
        String name = null;
        int index;
        Map<String, Object> category = null;

        // Validate input.
        if (null == zipFile) {
            throw new IllegalArgumentException("A null ZIP file was passed."); //$NON-NLS-1$
        }
        if (null == itemFactory) {
            throw new IllegalArgumentException(
                    "A null item factory was passed."); //$NON-NLS-1$
        }

        // Save the root category name and the item factory.
        rootName = categoryName;
        factory = itemFactory;

        // Replace a null root category name.
        if (null == rootName) {
            rootName = zipFile.getName();
        }

        // Open up the ZIP file.
        ZipFile contents = new ZipFile(zipFile);

        /***********************************************************************
         * DEBUG : uncomment this section to debug System.out.print( "Loading
         * items from "); System.out.println( zipFile.getPath() ); /* DEBUG :
         * uncomment this section to debug
         */

        // We'll need a Vector to hold the ZipEntries and a TreeMap
        // to map the directory names to the category names.
        Vector<ZipEntry> zipEntries = new Vector<ZipEntry>();
        TreeMap<String, String> names = new TreeMap<String, String>(StringUtil
                .stringComparator());

        // Walk through the contents of the ZIP file.
        entries = contents.entries();
        while (entries.hasMoreElements()) {

            // Get the next entry.
            entry = (ZipEntry) entries.nextElement();
            name = entry.getName();

            // Is this entry a sub-directory?
            if (entry.isDirectory()) {

                // Construct the category name for this sub-directory.
                StringBuffer buffer = new StringBuffer();
                buffer.append(rootName).append(" : ") //$NON-NLS-1$
                        .append(name);
                catName = buffer.toString();

                // Add the category to the map.
                categories.put(catName, new TreeMap<String, Object>(StringUtil
                        .stringComparator()));

                // Map the directory name to the category name
                names.put(name, catName);

            }

            // Is this entry a ZIP or JAR file?
            else if (ZippedItems.isZipName(name)) {
                // TODO : implement me!!!
                System.out
                        .print("... found a ZIP file **inside** a ZIP file: "); //$NON-NLS-1$
                System.out.println(name);
            }

            // Does the factory accept this entry?
            else if (factory.accept(contents, name)) {

                // Save this entry for later.
                zipEntries.addElement(entry);
            }

            /*******************************************************************
             * DEBUG : uncomment this section to debug else { System.out.print(
             * "... ignoring " ); System.out.println( name ); } /* DEBUG :
             * uncomment this section to debug
             */

        } // Handle the next ZipEntry.

        // Add a category for the base directory of the ZIP file.
        categories.put(rootName, new TreeMap<String, Object>(StringUtil
                .stringComparator()));
        names.put(rootName, rootName);

        // Walk through the ZipEntries and assign them to categories.
        entries = zipEntries.elements();
        while (entries.hasMoreElements()) {

            // Get the next entry.
            entry = (ZipEntry) entries.nextElement();

            // Get the name of the entry.
            name = entry.getName();

            // Should this entry be assigned to a sub-category?
            index = name.lastIndexOf("/"); //$NON-NLS-1$
            if (index < 0) {
                // Nope. Assign it to the root category.
                catName = rootName;
            } else {
                // Yup. Find the sub-category's name.
                index++;
                catName = name.substring(0, index);
                catName = names.get(catName);

                // We *should* have found category name.
                if (null == catName) {
                    // Assign the ZipEntry to the root category.
                    catName = rootName;
                } else {
                    // Rename the ZipEntry
                    name = name.substring(index);
                }
            }

            // Add the entry to the global map.
            category = categories.get(catName);
            category.put(name, factory.getItemFile(entry, contents));

        } // Handle the next ZipEntry

        // Remove any category without an entry.
        entries = Collections.enumeration(names.values());
        while (entries.hasMoreElements()) {

            // Get the next category name.
            catName = (String) entries.nextElement();

            // Get the named category.
            category = categories.get(catName);

            // If the category is empty, remove it.
            if (category.isEmpty()) {
                categories.remove(catName);
            }

        } // Check the next category

    }

    /**
     * Get the names of all the categories.
     * 
     * @return an <code>Enumeration</code> of <code>String</code> names.
     *         This value will not be <code>null</code>, but it may be empty.
     */
    public Iterator<String> getCategoryNames() {
        return categories.keySet().iterator();
    }

    /**
     * Get the names of all the items in one of the categories.
     * 
     * @param categoryName - the <code>String</code> name of the category
     *            whose item names are required.
     * @return an <code>Enumeration</code> of <code>String</code> names.
     *         This value will not be <code>null</code>, but it may be empty.
     */
    public Iterator<String> getItemNames(String categoryName) {

        // Get the map with the given category name.
        Map<String, Object> items = categories.get(categoryName);

        // Return the names of this category's items.
        return items.keySet().iterator();
    }

    /**
     * Get the indicated item from the correct catagory.
     * 
     * @param categoryName - the <code>String</code> name of the category
     *            whose item names are required.
     * @param itemName - the <code>String</code> name of the indicated item.
     * @return the <code>Object<code> in the correct category with the given
     *          name.  This value may be <code>null</code>.
     * @throws  <code>Exception</code> if there's any error getting the item.
     */
    public Object getItem(String categoryName, String itemName)
            throws Exception {

        // Get the map with the given category name.
        Map<String, Object> items = categories.get(categoryName);

        // Make sure the category contains an item by that name.
        if (!items.containsKey(itemName)) {
            return null;
        }

        // Find the named entry.
        ItemFile entry = (ItemFile) items.get(itemName);

        // Return the item.
        return entry.getItem();
    }

}

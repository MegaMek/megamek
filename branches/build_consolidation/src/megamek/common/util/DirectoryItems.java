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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

/**
 * This class represents a collection of files present within a directory
 * hierarchy, categorized according to their directories. This collection will
 * include all files inside of JAR and ZIP files that are located in the
 * directory hierarchy. Created on January 17, 2004
 * 
 * @author James Damour
 * @version 1
 */
public class DirectoryItems implements Categorized {

    /**
     * The root category name.
     */
    private String rootName;

    /**
     * A map of the category names to the sub-categories. Please note that this
     * map includes the root category, if the root category contains any items.
     */
    private TreeMap<String, Categorized> categories = new TreeMap<String, Categorized>(
            StringUtil.stringComparator());

    /**
     * A map of item names to the <code>ItemFile</code>s in the root
     * category.
     */
    private TreeMap<String, ItemFile> items = new TreeMap<String, ItemFile>(
            StringUtil.stringComparator());

    /**
     * The factory that will create <code>ItemFile</code>s for the contents
     * of the directory hierarchy.
     */
    private ItemFileFactory factory = null;

    /**
     * Helper function to file away new categories. It adds one entry in the map
     * for each sub-category in the passed category.
     * 
     * @param category - the <code>Categorized</code> files.
     */
    private void addCategory(Categorized category) {
        Iterator<String> names = category.getCategoryNames();
        while (names.hasNext()) {
            categories.put(names.next(), category);
        }
    }

    /**
     * Create a categorized collection of all files beneth the given directory.
     * Please note, the name of any sub-directories will be added to the root
     * category name to create the name of the sub-directories' category name.
     * 
     * @param rootDir - the <code>File</code> object for the root directory of
     *            the image files. All files in this root, or in any sub-
     *            directory of this root will be included in this collection.
     *            This value must not be <code>null</code> and it must be a
     *            directory.
     * @param categoryName - the <code>String</code> root category name for
     *            this collection. All sub-categories will include this name.
     *            This value may be <code>null</code> (it will be replaced).
     * @param itemFactory - the <code>ItemFileFactory</code> that will create
     *            <code>ItemFile</code>s for the contents of the directory.
     *            This value must not be <code>null</code>.
     * @throws <code>IllegalArgumentException</code> if <code>rootDir</code>
     *             is null or if it is not a directory, or if a
     *             <code>null</code> is passed for <code>itemFactory</code>.
     */
    public DirectoryItems(File rootDir, String categoryName,
            ItemFileFactory itemFactory) throws IllegalArgumentException {

        // Validate input.
        if (null == rootDir) {
            throw new IllegalArgumentException(
                    "A null root directory was passed."); //$NON-NLS-1$
        } else if (!rootDir.isDirectory()) {
            throw new IllegalArgumentException(
                    "The passed file is not a directory."); //$NON-NLS-1$
        }
        if (null == itemFactory) {
            throw new IllegalArgumentException(
                    "A null item factory was passed."); //$NON-NLS-1$
        }

        // Save the root category name and the item factory.
        rootName = categoryName;
        factory = itemFactory;

        // Replace a null name with an empty name.
        if (null == rootName) {
            rootName = ""; //$NON-NLS-1$
        }

        /***********************************************************************
         * DEBUG : uncomment this section to debug System.out.print( "Loading
         * items from "); System.out.println( rootDir.getPath() ); /* DEBUG :
         * uncomment this section to debug
         */

        // Walk through the contents of the root directory.
        String[] contents = rootDir.list();
        for (int entry = 0; entry < contents.length; entry++) {

            // Get the entry's file.
            File file = new File(rootDir, contents[entry]);

            // Is this entry a sub-directory?
            if (file.isDirectory()) {

                // Construct the category name for this sub-directory.
                StringBuffer name = new StringBuffer();
                name.append(rootName).append(contents[entry]).append("/"); //$NON-NLS-1$

                // Parse the sub-directory, and add it to the map.
                this.addCategory(new DirectoryItems(file, name.toString(),
                        factory));

            }

            // Is this entry a ZIP or JAR file?
            else if (ZippedItems.isZipName(contents[entry])) {

                // Construct the category name for this ZIP file.
                StringBuffer name = new StringBuffer();
                name.append(rootName).append(contents[entry]);

                // Try to parse the ZIP file, and add it to the map.
                try {
                    this.addCategory(new ZippedItems(file, name.toString(),
                            factory));
                } catch (Exception err) {
                    // Print diagnostics and keep going.
                    System.err.print("Could not parse "); //$NON-NLS-1$
                    System.err.println(contents[entry]);
                    err.printStackTrace();
                }
            }

            // Does the factory accept this entry?
            else if (factory.accept(rootDir, contents[entry])) {

                // Save the ItemFile for this entry.
                items.put(contents[entry], factory.getItemFile(file));
            }

            /*******************************************************************
             * DEBUG : uncomment this section to debug else { System.out.print(
             * "... ignoring " ); System.out.println( contents[entry] ); } /*
             * DEBUG : uncomment this section to debug
             */

        } // Get the next entry in the root directory.

        // If the root directory has any item files, add it to the map.
        if (!items.isEmpty()) {
            categories.put(rootName, this);
        }
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

        // Get the category with the given name.
        Categorized category = categories.get(categoryName);

        // Return an empty Enumeration if we couldn't find the category.
        if (null == category) {
            return new ArrayList<String>().iterator();
        }

        // Is this a subcategory?
        if (this != category) {

            // Yup. Pass the request on.
            return category.getItemNames(categoryName);
        }

        // Return the names of this directory's items.
        return items.keySet().iterator();
    }

    /**
     * Get the indicated item from the correct catagory.
     * 
     * @param categoryName - the <code>String</code> name of the category
     *            whose item names are required. This value may be
     *            <code>null</code>.
     * @param itemName - the <code>String</code> name of the indicated item.
     * @return the <code>Object<code> in the given category with the given
     *          name.  This value may be <code>null</code>.
     * @throws  <code>Exception</code> if there's any error getting the item.
     */
    public Object getItem(String categoryName, String itemName)
            throws Exception {

        // Validate input.
        if (null == categoryName) {
            throw new IllegalArgumentException(
                    "A null category name was passed."); //$NON-NLS-1$
        }
        if (null == itemName) {
            throw new IllegalArgumentException("A null item name was passed."); //$NON-NLS-1$
        }

        // Make sure we have this category?
        if (!categories.containsKey(categoryName)) {
            return null;
        }

        // Get the category with the given name.
        Categorized category = categories.get(categoryName);

        // Is this a subcategory?
        if (this != category) {

            // Yup. Pass the request on.
            return category.getItem(categoryName, itemName);
        }

        // Make sure we have an item by that name.
        if (!items.containsKey(itemName)) {
            return null;
        }

        // Find the named entry.
        ItemFile entry = items.get(itemName);

        // Return the item.
        return entry.getItem();
    }

}

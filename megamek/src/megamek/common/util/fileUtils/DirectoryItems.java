/*
 * Copyright (C) 2004 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.util.fileUtils;

import megamek.common.util.ItemFileFactory;

import java.io.File;
import java.util.Objects;

/**
 * This class represents a collection of files present within a directory
 * hierarchy, categorized according to their directories. This collection will
 * include all files inside of JAR and ZIP files that are located in the
 * directory hierarchy.
 *
 * @author James Damour (original)
 * @author Justin "Windchild" Bowen
 * @version 2
 */
public class DirectoryItems extends AbstractDirectory {
    /**
     * Create a categorized collection of all files beneath the given directory.
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
     * @throws AssertionError if <code>rootDir</code> is null or if it is not a directory, or if a
     *             <code>null</code> is passed for <code>itemFactory</code>.
     */
    public DirectoryItems(File rootDir, String categoryName, ItemFileFactory itemFactory)
            throws AssertionError {
        super(rootDir, categoryName, itemFactory);
        assert rootDir.isDirectory() : "The passed file is not a directory.";

        // Walk through the contents of the root directory. It will NPE if the folder in question has
        // any illegal paths noted by File::list
        for (String content : Objects.requireNonNull(rootDir.list())) {
            // Get the entry's file.
            File file = new File(rootDir, content);

            if (file.isDirectory()) { // Is this entry a sub-directory?
                // Construct the category name for this sub-directory, and add it to the map
                addCategory(new DirectoryItems(file,
                        getRootName() + content + "/", itemFactory));
            } else if (isZipName(content)) { // Is this entry a ZIP or JAR file?
                // Try to parse the ZIP file, and add it to the map.
                try {
                    addCategory(new ZippedItems(file, getRootName() + content, itemFactory));
                } catch (Exception e) {
                    logger.error(getClass(), "DirectoryItems",
                            "Could not parse " + content, e);
                }
            } else if (itemFactory.accept(rootDir, content)) { // Does the factory accept this entry?
                // Save the ItemFile for this entry.
                items.put(content, itemFactory.getItemFile(file));
            }
        } // Get the next entry in the root directory.

        // If the root directory has any item files, add it to the map.
        if (!items.isEmpty()) {
            categories.put(getRootName(), this);
        }
    }
}
/*
 * Copyright (C) 2004 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2020-2021 - The MegaMek Team. All Rights Reserved.
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

import org.apache.logging.log4j.LogManager;

import java.io.File;

/**
 * This class represents a collection of files present within a directory hierarchy, categorized
 * according to their directories.
 *
 * @author James Damour (original)
 * @author Justin "Windchild" Bowen
 */
public class DirectoryItems extends AbstractDirectory {
    public DirectoryItems(final File root, final ItemFileFactory itemFactory)
            throws IllegalArgumentException, NullPointerException {
        this(root, "", "", itemFactory);
    }

    /**
     * Create a categorized collection of all files beneath the given directory.
     * Please note, the name of any sub-directories will be added to the root category name to
     * create the name of the sub-directories' category path.
     *
     * @param root the <code>File</code> object for the root directory of the image files. All files
     *             in this root will be included in this collection. This value must not be
     *             <code>null</code> and it must be a directory.
     * @param categoryName the <code>String</code> root category name for this directory
     * @param categoryPath the <code>String</code> root category path for this collection. All
     *                     sub-categories will include this as part of their path.
     * @param itemFactory the <code>ItemFileFactory</code> that will create <code>ItemFile</code>s
     *                    for the contents of the directory. This value must not be <code>null</code>.
     * @throws AssertionError if <code>root</code> is null or if it is not a directory, or if a
     * <code>null</code> is passed for <code>itemFactory</code>.
     */
    private DirectoryItems(final File root, final String categoryName, final String categoryPath,
                           final ItemFileFactory itemFactory)
            throws IllegalArgumentException, NullPointerException {
        super(root, categoryName, categoryPath, itemFactory);

        if (!root.isDirectory()) {
            throw new IllegalArgumentException("The passed file is not a directory.");
        }

        final String[] children = root.list();
        if (children == null) {
            LogManager.getLogger().error("Failed to parse the " + categoryName + " directory, getting null children calling root.list()");
            return;
        }

        for (final String content : children) {
            final File file = new File(root, content);

            if (file.isDirectory()) {
                // Construct the category name for this sub-directory, and add it to the map
                getCategories().put(content,
                        new DirectoryItems(file, content, getRootPath() + content + "/", itemFactory));
            } else if (itemFactory.accept(root, content)) {
                // Save the ItemFile for this entry.
                getItems().put(content, itemFactory.getItemFile(file));
            }
        }
    }
}

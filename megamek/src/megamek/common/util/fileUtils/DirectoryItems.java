/*
 * Copyright (C) 2004 - Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.util.fileUtils;

import java.io.File;

import megamek.logging.MMLogger;

/**
 * This class represents a collection of files present within a directory hierarchy, categorized according to their
 * directories.
 *
 * @author James Damour (original)
 * @author Justin "Windchild" Bowen
 */
public class DirectoryItems extends AbstractDirectory {
    private static final MMLogger logger = MMLogger.create(DirectoryItems.class);

    public DirectoryItems(final File root, final ItemFileFactory itemFactory)
          throws IllegalArgumentException, NullPointerException {
        this(root, "", "", itemFactory);
    }

    /**
     * Create a categorized collection of all files beneath the given directory. Please note, the name of any
     * sub-directories will be added to the root category name to create the name of the sub-directories' category
     * path.
     *
     * @param root         the <code>File</code> object for the root directory of the image files. All files in this
     *                     root will be included in this collection. This value must not be
     *                     <code>null</code> and it must be a directory.
     * @param categoryName the <code>String</code> root category name for this directory
     * @param categoryPath the <code>String</code> root category path for this collection. All sub-categories will
     *                     include this as part of their path.
     * @param itemFactory  the <code>ItemFileFactory</code> that will create
     *                     <code>ItemFile</code>s
     *                     for the contents of the directory. This value must not be
     *                     <code>null</code>.
     *
     * @throws AssertionError if <code>root</code> is null or if it is not a directory, or if a
     *                        <code>null</code> is passed for
     *                        <code>itemFactory</code>.
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
            logger.error(
                  "Failed to parse the " + categoryName + " directory, getting null children calling root.list()");
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

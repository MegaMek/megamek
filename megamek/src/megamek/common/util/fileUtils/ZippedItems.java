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
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * First, please note that the term "ZIP file" will be taken to mean "ZIP or JAR file",
 * because a JAR file is just a ZIP file with a manifest, from this point onwards.
 * This class represents a collection of item files present within a ZIP file,
 * categorized according to their directories. This collection does not currently handle
 * nested ZIP files, although that is the end goal.
 *
 * @author James Damour (original)
 * @author Justin "Windchild" Bowen
 * @version 2
 */
public class ZippedItems extends AbstractDirectory {
    /**
     * Create a categorized collection of all files within a ZIP file. Please
     * note, the name of any directories in the ZIP file will be added to the
     * root category name to create the name of the category names of the
     * directories.
     *
     * @param zipFile - the <code>File</code> object for the ZIP file
     *            containing the image files. All files in this ZIP file, will
     *            be included in this collection, categorized by directory. This
     *            value must not be <code>null</code> and it must be a ZIP file.
     * @param categoryName - the <code>String</code> root category name for
     *            this collection. All sub-categories will include this name.
     *            This value may be <code>null</code> (it will be replaced).
     * @param itemFactory - the <code>ItemFileFactory</code> that will create
     *            <code>ItemFile</code>s for the contents of the directory.
     *            This value must not be <code>null</code>.
     * @throws AssertionError if <code>zipFile</code>
     *             or <code>itemFactory</code> is <code>null</code>.
     * @throws IOException if there's an IO error opening <code>zipFile</code>.
     */
    public ZippedItems(File zipFile, String categoryName, ItemFileFactory itemFactory)
            throws AssertionError, IOException {
        super(zipFile, categoryName, itemFactory);

        // Fix a null root category name.
        if (categoryName == null) {
            setRootName(zipFile.getName());
        }

        // Open up the ZIP file.
        ZipFile contents = new ZipFile(zipFile);

        // Walk through the contents of the ZIP file.
        Enumeration<?> entries = contents.entries();
        while (entries.hasMoreElements()) {
            // Get the next entry.
            ZipEntry entry = (ZipEntry) entries.nextElement();

            String name = entry.getName();

            if (entry.isDirectory()) { // Is this entry a sub-directory?
                addCategory(new DefaultDirectory(zipFile, getRootName() + name + "/", itemFactory));
            } else if (isZipName(name)) { // Is this entry a ZIP or JAR file?
                addCategory(new ZippedItems(zipFile, name, itemFactory));
            } else if (itemFactory.accept(contents, name)) { // Does the factory accept this entry?
                items.put(name, itemFactory.getItemFile(entry, contents));
            }
        }

        // If the root directory has any item files, add it to the map.
        if (!items.isEmpty()) {
            categories.put(getRootName(), this);
        }
    }
}

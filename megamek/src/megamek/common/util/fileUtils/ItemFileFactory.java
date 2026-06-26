/*
 * Copyright (C) 2004 - Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
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
import java.io.FilenameFilter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * This interface represents a factory that can produce items from categorizable files. This interface extends the
 * <code>FilenameFilter</code>, so the factory is aware of the files that it can handle as input.
 *
 * @author James Damour
 * @since January 18, 2004
 */
public interface ItemFileFactory extends FilenameFilter {
    /**
     * Get the <code>ItemFile</code> for the given <code>File</code>.
     *
     * @param file The input <code>File</code> object that will be read to produce the item. This value must not be
     *             <code>null</code>.
     *
     * @return an <code>ItemFile</code> for the given file.
     */
    ItemFile getItemFile(File file);

    /**
     * Get the <code>ItemFile</code> for the given <code>ZipEntry</code> in the <code>ZipFile</code>.
     *
     * @param zipEntry The <code>ZipEntry</code> that will be read to produce the item. This value must not be
     *                 <code>null</code>.
     * @param zipFile  The <code>ZipFile</code> object that contains the <code>ZipEntry</code> that will produce the
     *                 item. This value must not be <code>null</code>.
     *
     * @return an <code>ItemFile</code> for the given zip file entry.
     */
    ItemFile getItemFile(ZipEntry zipEntry, ZipFile zipFile);

    /**
     * The method that must be implemented by any object that filters filenames (i.e., selects a subset of filenames
     * from a list of filenames).
     * <p>
     * This definition is copied from {@link java.io.FilenameFilter} for completeness.
     *
     * @param dir  The <code>File</code> object of the directory containing the named file.
     * @param name The <code>String</code> name of the file.
     */
    @Override
    boolean accept(File dir, String name);

    /**
     * The method that must be implemented by any object that filters filenames within a
     * <code>ZipFile</code> (i.e., selects a subset of filenames from a list of filenames in a ZIP archive).
     *
     * @param zipFile The <code>ZipFile</code> object that contains the named file's entry.
     * @param name    The <code>String</code> name of the file.
     */
    boolean accept(ZipFile zipFile, String name);
}

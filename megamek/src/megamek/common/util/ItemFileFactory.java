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
import java.io.FilenameFilter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * This interface represents a factory that can produce items from cate-
 * gorizable files. This interface extends the <code>FilenameFilter</code>,
 * so the factory is aware of the files that it can handle as input. Created on
 * January 18, 2004
 * 
 * @author James Damour
 * @version 1
 */
public interface ItemFileFactory extends FilenameFilter {

    /**
     * Get the <code>ItemFile</code> for the given <code>File</code>.
     * 
     * @param file - the input <code>File</code> object that will be read to
     *            produce the item. This value must not be <code>null</code>.
     * @return an <code>ItemFile</code> for the given file.
     * @throws <code>IllegalArgumentException</code> if the <code>file</code>
     *             is <code>null</code>.
     */
    public ItemFile getItemFile(File file) throws IllegalArgumentException;

    /**
     * Get the <code>ItemFile</code> for the given <code>ZipEntry</code> in
     * the <code>ZipFile</code>.
     * 
     * @param zipEntry - the <code>ZipEntry</code> that will be read to
     *            produce the item. This value must not be <code>null</code>.
     * @param zipFile - the <code>ZipFile</code> object that contains the
     *            <code>ZipEntry</code> that will produce the item. This value
     *            must not be <code>null</code>.
     * @return an <code>ItemFile</code> for the given zip file entry.
     * @throws <code>IllegalArgumentException</code> if the <code>file</code>
     *             is <code>null</code>.
     */
    public ItemFile getItemFile(ZipEntry entry, ZipFile zipFile)
            throws IllegalArgumentException;

    /**
     * The method that must be implemented by any object that filters filenames
     * (i.e., selects a subset of filenames from a list of filenames). <p/> This
     * definition is copied from <code>java.io.FilenameFilter</code> for
     * completeness.
     * 
     * @param dir - the <code>File</code> object of the directory containing
     *            the named file.
     * @param name - the <code>String</code> name of the file.
     */
    public boolean accept(File dir, String name);

    /**
     * The method that must be implemented by any object that filters filenames
     * within a <code>ZipFile</code> (i.e., selects a subset of filenames from
     * a list of filenames in a ZIP archive).
     * 
     * @param zipFile - the <code>ZipFile</code> object that contains the
     *            named file's entry.
     * @param name - the <code>String</code> name of the file.
     */
    public boolean accept(ZipFile zipFile, String name);

}

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

package megamek.client.ui.swing.util;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import megamek.common.util.ItemFile;
import megamek.common.util.ItemFileFactory;

/**
 * This class will produce <code>Image</code> objects from files. If an image
 * file is inside of JAR and ZIP file, then it must save the contents to a
 * temporary file. <p/> <p/> Created on January 18, 2004
 * 
 * @author James Damour
 * @version 1
 */
public class ImageFileFactory implements ItemFileFactory {

    /**
     * Accepted image file extentions
     */
    private final static String JPG = "JPG", JPEG = "JPEG", GIF = "GIF", PNG = "PNG"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

    /**
     * Implement the Singleton pattern.
     */
    private ImageFileFactory() {
    }

    private static ImageFileFactory singleton = null;

    /**
     * Get the Singleton <code>ImageFileFactory</code>.
     * 
     * @return the Singleton <code>ImageFileFactory</code>.
     */
    public static ImageFileFactory getInstance() {
        if (null == singleton)
            singleton = new ImageFileFactory();
        return singleton;
    }

    /**
     * Get the <code>ItemFile</code> for the given <code>File</code>.
     * 
     * @param file - the input <code>File</code> object that will be read to
     *            produce the item. This value must not be <code>null</code>.
     * @return an <code>ItemFile</code> for the given file.
     * @throws <code>IllegalArgumentException</code> if the <code>file</code>
     *             is <code>null</code>.
     */
    public ItemFile getItemFile(final File file)
            throws IllegalArgumentException {

        // Validate the input.
        if (null == file) {
            throw new IllegalArgumentException("A null image file was passed."); //$NON-NLS-1$
        }

        // Construct an anonymous class that gets an Image for the file.
        return new ItemFile() {

            private File itemFile = file; // copy the file entry
            private Image image = null; // cache the Image

            public Object getItem() throws Exception {
                // Cache the image on first use.
                if (null == image) {
                    String name = itemFile.getAbsolutePath();
                    image = Toolkit.getDefaultToolkit().getImage(name);
                }
                // Return a copy of the image.
                return image.getScaledInstance(84, 72, Image.SCALE_FAST);
            } // End getItem()
        };

    }

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
     * @throws <code>IllegalArgumentException</code> if either the
     *             <code>zipEntry</code> or the <code>zipFile</code> is
     *             <code>null</code>.
     */
    public ItemFile getItemFile(final ZipEntry zipEntry, final ZipFile zipFile)
            throws IllegalArgumentException {

        // Validate the input.
        if (null == zipEntry) {
            throw new IllegalArgumentException("A null ZIP entry was passed."); //$NON-NLS-1$
        }
        if (null == zipFile) {
            throw new IllegalArgumentException("A null ZIP file was passed."); //$NON-NLS-1$
        }

        // Construct an anonymous class that gets an Image for the file.
        return new ItemFile() {

            private ZipEntry itemEntry = zipEntry; // copy the ZipEntry
            private Image image = null; // cache the Image

            public Object getItem() throws Exception {

                // Cache the image on first use.
                if (null == image) {

                    // Get ready to read from the item.
                    InputStream in = new BufferedInputStream(zipFile
                            .getInputStream(itemEntry), (int) itemEntry
                            .getSize());

                    // Make a buffer big enough to hold the item,
                    // read from the ZIP file, and write it to temp.
                    byte[] buffer = new byte[(int) itemEntry.getSize()];
                    in.read(buffer);

                    // Check the last 10 bytes. I've been having
                    // some problems with incomplete image files,
                    // and I want to detect it early and give advice
                    // to players for dealing with the problem.
                    int index = (int) itemEntry.getSize() - 10;
                    while (itemEntry.getSize() > index) {
                        if (0 != buffer[index]) {
                            break;
                        }
                        index++;
                    }
                    if (itemEntry.getSize() <= index) {
                        throw new IOException(
                                "Error reading " + itemEntry.getName() + //$NON-NLS-1$
                                        "\nYou may want to unzip " + //$NON-NLS-1$
                                        zipFile.getName());
                    }

                    // Create the image from the buffer.
                    image = Toolkit.getDefaultToolkit().createImage(buffer);

                } // End get-image

                // Return a copy of the image.
                return image.getScaledInstance(84, 72, Image.SCALE_FAST);

            } // End getItem()
        };

    }

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
    public boolean accept(File dir, String name) {

        // Convert the file name to upper case, and compare it to image
        // file extensions. Yeah, it's a bit of a hack, but whatever.
        String ucName = name.toUpperCase(Locale.ROOT);
        return (ucName.endsWith(JPG) || ucName.endsWith(JPEG)
                || ucName.endsWith(GIF) || ucName.endsWith(PNG));
    }

    /**
     * The method that must be implemented by any object that filters filenames
     * within a <code>ZipFile</code> (i.e., selects a subset of filenames from
     * a list of filenames in a ZIP archive).
     * 
     * @param zipFile - the <code>ZipFile</code> object that contains the
     *            named file's entry.
     * @param name - the <code>String</code> name of the file.
     */
    public boolean accept(ZipFile zipFile, String name) {

        // Convert the file name to upper case, and compare it to image
        // file extensions. Yeah, it's a bit of a hack, but whatever.
        String ucName = name.toUpperCase(Locale.ROOT);
        return (ucName.endsWith(JPG) || ucName.endsWith(JPEG)
                || ucName.endsWith(GIF) || ucName.endsWith(PNG));
    }

}

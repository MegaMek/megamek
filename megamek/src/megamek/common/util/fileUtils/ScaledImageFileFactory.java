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

import java.awt.Image;
import java.awt.Toolkit;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import megamek.common.util.ImageUtil;

/**
 * This class will produce <code>Image</code> objects from files. If an image
 * file is inside of JAR and ZIP file, then it must save the contents to a
 * temporary file. <p/> <p/> Created on January 18, 2004
 * 
 * @author James Damour
 * @version 1
 */
public class ScaledImageFileFactory implements ItemFileFactory {
    /**
     * Accepted image file extensions
     */
    private static final String JPG = "JPG", JPEG = "JPEG", GIF = "GIF", PNG = "PNG";

    /**
     * Implement the Singleton pattern.
     */
    private ScaledImageFileFactory() { }

    private static ScaledImageFileFactory singleton = null;

    /**
     * Get the Singleton <code>ImageFileFactory</code>.
     *
     * @return the Singleton <code>ImageFileFactory</code>.
     */
    public static ScaledImageFileFactory getInstance() {
        if (singleton == null) {
            singleton = new ScaledImageFileFactory();
        }
        return singleton;
    }

    /**
     * Get the <code>ItemFile</code> for the given <code>File</code>.
     *
     * @param file The input <code>File</code> object that will be read to produce the item.
     *             This value must not be <code>null</code>.
     * @return an <code>ItemFile</code> for the given file.
     * @throws IllegalArgumentException if the <code>file</code> is <code>null</code>.
     */
    @Override
    public ItemFile getItemFile(final File file) throws IllegalArgumentException {
        // Validate the input.
        Objects.requireNonNull(file, "A null image file was passed.");

        // Construct an anonymous class that gets an Image for the file.
        return new ItemFile() {
            private File itemFile = file; // copy the file entry
            private Image image = null; // cache the Image

            @Override
            public Object getItem() {
                // Cache the image on first use.
                if (image == null) {
                    String name = itemFile.getAbsolutePath();
                    image = ImageUtil.loadImageFromFile(name);
                }
                // Return a copy of the image.
                return ImageUtil.getScaledImage(image, 84, 72);
            }
        };
    }

    /**
     * Get the <code>ItemFile</code> for the given <code>ZipEntry</code> in the <code>ZipFile</code>.
     *
     * @param zipEntry The <code>ZipEntry</code> that will be read to produce the item. This value
     *                 must not be <code>null</code>.
     * @param zipFile The <code>ZipFile</code> object that contains the <code>ZipEntry</code>
     *                that will produce the item. This value must not be <code>null</code>.
     * @return an <code>ItemFile</code> for the given zip file entry.
     * @throws IllegalArgumentException if the <code>file</code> is <code>null</code>.
     */
    @Override
    public ItemFile getItemFile(final ZipEntry zipEntry, final ZipFile zipFile)
            throws IllegalArgumentException {
        // Validate the input.
        Objects.requireNonNull(zipEntry, "A null ZIP entry was passed.");
        Objects.requireNonNull(zipFile, "A null ZIP file was passed.");

        // Construct an anonymous class that gets an Image for the file.
        return new ItemFile() {
            private ZipEntry itemEntry = zipEntry; // copy the ZipEntry
            private Image image = null; // cache the Image

            @Override
            public Object getItem() throws Exception {
                // Cache the image on first use.
                if (image == null) {

                    // Get ready to read from the item.
                    try (InputStream in = new BufferedInputStream(zipFile.getInputStream(itemEntry),
                            (int) itemEntry.getSize())) {

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
                            if (buffer[index] == 0) {
                                index++;
                            } else {
                                break;
                            }
                        }
                        if (itemEntry.getSize() <= index) {
                            throw new IOException("Error reading " + itemEntry.getName()
                                    + "\nYou may want to unzip " + zipFile.getName());
                        }
                        // Create the image from the buffer.
                        image = Toolkit.getDefaultToolkit().createImage(buffer);
                    }
                }
                // Return a copy of the image.
                return ImageUtil.getScaledImage(image, 84, 72);
            }
        };

    }

    /**
     * The method that must be implemented by any object that filters filenames
     * (i.e., selects a subset of filenames from a list of filenames).
     *
     * This definition is copied from {@link java.io.FilenameFilter} for completeness.
     *
     * @param dir The <code>File</code> object of the directory containing the named file.
     * @param name The <code>String</code> name of the file.
     */
    @Override
    public boolean accept(File dir, String name) {
        // Convert the file name to upper case, and compare it to image
        // file extensions. Yeah, it's a bit of a hack, but whatever.
        String ucName = name.toUpperCase(Locale.ROOT);
        return (ucName.endsWith(JPG) || ucName.endsWith(JPEG) || ucName.endsWith(GIF) || ucName.endsWith(PNG));
    }

    /**
     * The method that must be implemented by any object that filters filenames within a
     * <code>ZipFile</code> (i.e., selects a subset of filenames from a list of filenames in a ZIP archive).
     *
     * @param zipFile The <code>ZipFile</code> object that contains the named file's entry.
     * @param name The <code>String</code> name of the file.
     */
    @Override
    public boolean accept(ZipFile zipFile, String name) {
        // Convert the file name to upper case, and compare it to image
        // file extensions. Yeah, it's a bit of a hack, but whatever.
        String ucName = name.toUpperCase(Locale.ROOT);
        return (ucName.endsWith(JPG) || ucName.endsWith(JPEG) || ucName.endsWith(GIF) || ucName.endsWith(PNG));
    }
}

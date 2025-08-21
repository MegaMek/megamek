/*
 * Copyright (C) 2004 - Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2006-2025 The MegaMek Team. All Rights Reserved.
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

import java.awt.Image;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Locale;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;

import megamek.common.util.ImageUtil;

/**
 * A FilenameFilter that produces image files (PNG, JPG/JPEG, GIF). (The images are not scaled. To produce scaled images
 * use ScaledImageFileFactory.)
 */
public class ImageFileFactory implements ItemFileFactory {
    //region Variable Declarations
    /** Accepted image file extensions */
    protected static final String JPG = "JPG";
    protected static final String JPEG = "JPEG";
    protected static final String GIF = "GIF";
    protected static final String PNG = "PNG";
    //endregion Variable Declarations

    //region Constructors
    public ImageFileFactory() {

    }
    //endregion Constructors

    /**
     * Get the <code>ItemFile</code> for the given <code>File</code>.
     *
     * @param file The input <code>File</code> object that will be read to produce the item. This value must not be
     *             <code>null</code>.
     *
     * @return an <code>ItemFile</code> for the given file.
     */
    @Override
    public ItemFile getItemFile(final File file) {
        // Validate the input.
        Objects.requireNonNull(file, "A null image file was passed");

        // Construct an anonymous class that gets an Image for the file.
        return new ItemFile() {
            @Override
            public Object getItem() {
                // Cache the image on first use.
                if (isNullOrEmpty()) {
                    item = ImageUtil.loadImageFromFile(file.getAbsolutePath());
                    if (!isNullOrEmpty()) {
                        item = ImageUtil.createAcceleratedImage((Image) item);
                    }
                }
                return item;
            }
        };
    }

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
    @Override
    public ItemFile getItemFile(final ZipEntry zipEntry, final ZipFile zipFile) {
        // Validate the input.
        Objects.requireNonNull(zipEntry, "A null ZIP entry was passed.");
        Objects.requireNonNull(zipFile, "A null ZIP file was passed.");

        // Construct an anonymous class that gets an Image for the file.
        return new ItemFile() {
            @Override
            public Object getItem() throws Exception {
                // Cache the image on first use.
                if (isNullOrEmpty()) {
                    item = createZippedImage(zipEntry, zipFile);
                    if (!isNullOrEmpty()) {
                        item = ImageUtil.createAcceleratedImage((Image) item);
                    }
                }
                return item;
            }
        };
    }

    /**
     * This creates an image from a zipped image
     *
     * @param zipEntry The <code>ZipEntry</code> that will be read to produce the item. This value must not be
     *                 <code>null</code>.
     * @param zipFile  The <code>ZipFile</code> object that contains the <code>ZipEntry</code> that will produce the
     *                 item. This value must not be <code>null</code>.
     *
     * @return the image created from a zipped image
     *
     * @throws Exception if there is an error reading the file
     */
    protected Image createZippedImage(final ZipEntry zipEntry, final ZipFile zipFile) throws Exception {
        // Get ready to read from the item.
        try (InputStream is = zipFile.getInputStream(zipEntry);
              BufferedInputStream bis = new BufferedInputStream(is, (int) zipEntry.getSize())) {
            return ImageIO.read(bis);
        }
    }

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
    public boolean accept(File dir, String name) {
        // Convert the file name to upper case, and compare it to image file extensions.
        String ucName = name.toUpperCase(Locale.ROOT);
        return (ucName.endsWith(JPG) || ucName.endsWith(JPEG) || ucName.endsWith(GIF) || ucName.endsWith(PNG));
    }

    /**
     * The method that must be implemented by any object that filters filenames within a
     * <code>ZipFile</code> (i.e., selects a subset of filenames from a list of filenames in a ZIP archive).
     *
     * @param zipFile The <code>ZipFile</code> object that contains the named file's entry.
     * @param name    The <code>String</code> name of the file.
     */
    @Override
    public boolean accept(ZipFile zipFile, String name) {
        // Convert the file name to upper case, and compare it to image file extensions.
        String ucName = name.toUpperCase(Locale.ROOT);
        return (ucName.endsWith(JPG) || ucName.endsWith(JPEG) || ucName.endsWith(GIF) || ucName.endsWith(PNG));
    }
}

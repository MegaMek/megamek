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
import java.io.File;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import megamek.common.util.ImageUtil;

/**
 * This class will produce <code>Image</code> objects from files. If an image
 * file is inside of JAR and ZIP file, then it must save the contents to a
 * temporary file.
 * 
 * @author James Damour
 * @since January 18, 2004
 */
public class ScaledImageFileFactory extends ImageFileFactory {
    //region Variable Declarations
    protected int width;
    protected int height;
    //endregion Variable Declarations

    //region Constructors
    public ScaledImageFileFactory() {
        this(84, 72);
    }

    public ScaledImageFileFactory(int width, int height) {
        super();
        setWidth(width);
        setHeight(height);
    }
    //endregion Constructors

    //region Getters/Setters
    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }
    //endregion Getters/Setters

    /**
     * Get the <code>ItemFile</code> for the given <code>File</code>.
     *
     * @param file The input <code>File</code> object that will be read to produce the item.
     *             This value must not be <code>null</code>.
     * @return an <code>ItemFile</code> for the given file.
     */
    @Override
    public ItemFile getItemFile(final File file) {
        // Validate the input.
        Objects.requireNonNull(file, "A null image file was passed.");

        // Construct an anonymous class that gets an Image for the file.
        return new ItemFile() {
            @Override
            public Object getItem() {
                // Cache the image on first use.
                if (isNullOrEmpty()) {
                    String name = file.getAbsolutePath();
                    item = ImageUtil.loadImageFromFile(name);
                    // Only if we load a non-null image can we scale it
                    if (!isNullOrEmpty()) {
                        item = ImageUtil.getScaledImage((Image) item, getWidth(), getHeight());
                    }
                }
                return item;
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
                    // Only if we load a non-null image can we scale it
                    if (!isNullOrEmpty()) {
                        item = ImageUtil.getScaledImage((Image) item, getWidth(), getHeight());
                    }
                }
                return item;
            }
        };
    }
}

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

import java.awt.Image;
import java.io.File;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import megamek.common.util.ImageUtil;

/**
 * This class will produce <code>Image</code> objects from files. If an image file is inside of JAR and ZIP file, then
 * it must save the contents to a temporary file.
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
     * @param file The input <code>File</code> object that will be read to produce the item. This value must not be
     *             <code>null</code>.
     *
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

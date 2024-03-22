/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.icons;

import megamek.common.annotations.Nullable;
import megamek.common.util.ImageUtil;

import java.awt.*;
import java.io.File;
import java.util.Objects;

/**
 * FileCamouflage is an implementation of Camouflage that does not take camos from the parsed directories
 * of MegaMek but is constructed directly from a local image file, e.g. a custom camo in a scenario that
 * is not part of MM's usual camos.
 */
public class FileCamouflage extends Camouflage {

    private final File file;
    private transient Image image;

    /**
     * Constructs a new camo with the given local file. This should be used to construct a camo from a local
     * file that is not within the parsed directories for camos, for example for a scenario that uses a custom
     * camo image.
     *
     * @param file The File, such as a file of "CustomCamo.jpg"
     */
    public FileCamouflage(File file) {
        this.file = file;
    }

    public boolean isColourCamouflage() {
        return false;
    }

    @Override
    public boolean hasDefaultCategory() {
        return false;
    }

    @Override
    public @Nullable Image getBaseImage() {
        if (image == null) {
            image = ImageUtil.loadImageFromFile(file.toString());
        }
        return image;
    }

    @Override
    public FileCamouflage clone() {
        FileCamouflage newCamo = new FileCamouflage(file);
        newCamo.setRotationAngle(rotationAngle);
        newCamo.setScale(scale);
        return newCamo;
    }

    @Override
    public boolean equals(Object other) {
        if (super.equals(other) && other instanceof FileCamouflage) {
            FileCamouflage otherCamo = (FileCamouflage) other;
            return otherCamo.file.equals(file);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(file, rotationAngle, scale);
    }
}
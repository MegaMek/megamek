/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.icons;

import java.awt.Image;
import java.io.File;
import java.util.Objects;

import megamek.common.annotations.Nullable;
import megamek.common.util.ImageUtil;

/**
 * FileCamouflage is an implementation of Camouflage that does not take camos from the parsed directories of MegaMek but
 * is constructed directly from a local image file, e.g. a custom camo in a scenario that is not part of MM's usual
 * camos.
 */
public class FileCamouflage extends Camouflage {

    private final File file;
    private transient Image image;

    /**
     * Constructs a new camo with the given local file. This should be used to construct a camo from a local file that
     * is not within the parsed directories for camos, for example for a scenario that uses a custom camo image.
     *
     * @param file The File, such as a file of "CustomCamo.jpg"
     */
    public FileCamouflage(File file) {
        this.file = file;
    }

    @Override
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

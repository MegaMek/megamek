/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.boardeditor;

import megamek.client.ui.util.UIUtil;
import megamek.common.annotations.Nullable;
import megamek.common.util.ImageUtil;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * A specialized JButton that only shows an icon but scales that icon according to the current GUI scaling when its
 * rescale() method is called.
 */
class ScalingIconButton extends JButton {

    private final Image baseImage;
    private Image baseRolloverImage;
    private Image baseDisabledImage;
    private final int baseWidth;

    ScalingIconButton(Image image, int width) {
        super();
        Objects.requireNonNull(image);
        baseImage = image;
        baseWidth = width;
        rescale();
    }

    /** Adapts all images of this button to the current gui scale. */
    void rescale() {
        int realWidth = UIUtil.scaleForGUI(baseWidth);
        int realHeight = baseImage.getHeight(null) * realWidth / baseImage.getWidth(null);
        setIcon(new ImageIcon(ImageUtil.getScaledImage(baseImage, realWidth, realHeight)));

        if (baseRolloverImage != null) {
            realHeight = baseRolloverImage.getHeight(null) * realWidth / baseRolloverImage.getWidth(null);
            setRolloverIcon(new ImageIcon(ImageUtil.getScaledImage(baseRolloverImage, realWidth, realHeight)));
        } else {
            setRolloverIcon(null);
        }

        if (baseDisabledImage != null) {
            realHeight = baseDisabledImage.getHeight(null) * realWidth / baseDisabledImage.getWidth(null);
            setDisabledIcon(new ImageIcon(ImageUtil.getScaledImage(baseDisabledImage, realWidth, realHeight)));
        } else {
            setDisabledIcon(null);
        }
    }

    /**
     * Sets the unscaled base image to use as a mouse hover image for the button. image may be null. Passing null
     * disables the hover image.
     */
    void setRolloverImage(@Nullable Image image) {
        baseRolloverImage = image;
    }

    /**
     * Sets the unscaled base image to use as a button disabled image for the button. image may be null. Passing null
     * disables the button disabled image.
     */
    void setDisabledImage(@Nullable Image image) {
        baseDisabledImage = image;
    }
}

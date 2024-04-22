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
package megamek.client.ui;

import megamek.common.annotations.Nullable;
import megamek.common.util.ImageUtil;
import org.apache.logging.log4j.LogManager;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Base64;
import java.util.Objects;

/**
 * This is a form of an Image that is based on a base64-encoded form, e.g. loaded from file. The base64 is retained
 * and the actual image only created when {@link #getImage()} is called. This class is serializable;
 * the internal Image object is transient and the base64 String is used for serialization.
 * Decoding the base64 image uses {@link ImageIO#read(InputStream)}.
 *
 * @implNote Threadsafe; Immutable. The displayable Image is created only when needed and uses synchronized access
 */
public class Base64Image implements Serializable {

    /** The base64 representation of the image. */
    private final String base64encodedImage;

    /** The displayable image. Created lazily by {@link #getImage()}. */
    private transient Image image;

    /** Creates an empty Base64Image. */
    public Base64Image() {
        this("");
    }

    /** Creates a Base64Image with the given base64-encoded image. */
    public Base64Image(String base64) {
        this.base64encodedImage = Objects.requireNonNullElse(base64, "");
    }

    /** @return True when there is no image, i.e. the base64 encoded form is blank. */
    public boolean isEmpty() {
        return base64encodedImage.isBlank();
    }

    /** @return The base64 representation of the image.  */
    public String getBase64String() {
        return base64encodedImage;
    }

    /**
     * Returns the image in displayable form. When first called, the base64 representation is converted to an Image.
     * When conversion fails, returns {@link ImageUtil#failStandardImage()}. When the base64 representation is
     * blank, returns null.
     *
     * @return The image in displayable form
     */
    public @Nullable Image getImage() {
        if (isEmpty()) {
            return null;
        } else {
            synchronized (this) {
                if (image == null) {
                    byte[] imageBytes = Base64.getDecoder().decode(base64encodedImage);
                    try (ByteArrayInputStream inStreambj = new ByteArrayInputStream(imageBytes)) {
                        image = ImageIO.read(inStreambj);
                    } catch (IOException ex) {
                        LogManager.getLogger().warn("Could not convert base64 to image", ex);
                        image = ImageUtil.failStandardImage();
                    }
                }
                return image;
            }
        }
    }
}
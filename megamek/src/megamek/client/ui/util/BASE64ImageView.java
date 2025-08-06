/*
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
package megamek.client.ui.util;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.Base64;
import java.util.Dictionary;
import java.util.Hashtable;
import javax.imageio.ImageIO;
import javax.swing.text.Element;
import javax.swing.text.html.HTML;
import javax.swing.text.html.ImageView;

import megamek.logging.MMLogger;

public class BASE64ImageView extends ImageView {
    private static final MMLogger logger = MMLogger.create(BASE64ImageView.class);

    private URL url;

    /**
     * Returns a unique url for the image. It's created by getting the code location and adding the element to it. This
     * doesn't strictly need to be an actual url, it just needs to be unique and properly formatted.
     *
     * @param elmnt the html element containing the base64 src
     */
    public BASE64ImageView(Element elmnt) {
        super(elmnt);
        populateImage();
    }

    // Creates a cache of images for each <img> src,
    @SuppressWarnings("unchecked")
    private void populateImage() {
        Dictionary<URL, Image> cache = (Dictionary<URL, Image>) getDocument()
              .getProperty("imageCache");
        if (cache == null) {
            cache = new Hashtable<>();
            getDocument().putProperty("imageCache", cache);
        }
        URL src = getImageURL();
        Image image = loadImage();
        if (image != null) {
            cache.put(src, image);
        }
    }

    // decodes the Base64 string into an image and returns it
    private Image loadImage() {
        String b64 = getBASE64Image();
        if (b64 != null) {
            BufferedImage newImage = null;
            try (ByteArrayInputStream bais = new ByteArrayInputStream(
                  Base64.getDecoder().decode(b64.getBytes()))) {
                newImage = ImageIO.read(bais);
            } catch (Exception ex) {
                logger.error("", ex);
            }
            return newImage;
        } else {
            return null;
        }
    }

    /**
     * Returns a unique url for the image. It's created by getting the code location and adding the element to it. This
     * doesn't strictly need to be an actual url, it just needs to be unique and properly formatted.
     *
     * @return the generated url for the image
     */
    @Override
    public URL getImageURL() {
        String src = (String) getElement().getAttributes()
              .getAttribute(HTML.Attribute.SRC);
        if (isBase64Encoded(src)) {

            try {
                this.url = new URL("file:/" + this.getElement().toString());
            } catch (Exception e) {
                logger.error("", e);
            }

            return this.url;
        }
        return super.getImageURL();
    }

    // checks if the given src is encoded
    private boolean isBase64Encoded(String src) {
        return src != null && src.contains("base64,");
    }

    // returns the string without the base64 text
    private String getBASE64Image() {
        String src = (String) getElement().getAttributes()
              .getAttribute(HTML.Attribute.SRC);
        if (!isBase64Encoded(src)) {
            return null;
        }
        return src.substring(src.indexOf("base64,") + 7);
    }

}

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
package megamek.client.ui.swing.widget;

import java.awt.Graphics;
import java.awt.Image;
import java.io.File;

import javax.swing.JPanel;

import megamek.common.Configuration;
import megamek.common.util.ImageUtil;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.logging.MMLogger;

public class SkinnedJPanel extends JPanel {
    private static final MMLogger logger = MMLogger.create(SkinnedJPanel.class);

    private final Image backgroundIcon;

    public SkinnedJPanel(SkinSpecification.UIComponents skinComponent, int backgroundIndex) {
        this(skinComponent.getComp(), backgroundIndex);
    }

    public SkinnedJPanel(String skinComponent, int backgroundIndex) {
        SkinSpecification skinSpec = SkinXMLHandler.getSkin(skinComponent, true);

        if (skinSpec.hasBackgrounds() && (skinSpec.backgrounds.size() > backgroundIndex)) {
            File file = new MegaMekFile(Configuration.widgetsDir(), skinSpec.backgrounds.get(backgroundIndex))
                    .getFile();
            if (file.exists()) {
                backgroundIcon = ImageUtil.loadImageFromFile(file.toString());
            } else {
                logger.error("Background icon doesn't exist: " + file.getPath());
                backgroundIcon = null;
            }
        } else {
            backgroundIcon = null;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (backgroundIcon == null) {
            super.paintComponent(g);
            return;
        }
        int w = getWidth();
        int h = getHeight();
        int iW = backgroundIcon.getWidth(this);
        int iH = backgroundIcon.getHeight(this);
        // If the image isn't loaded, prevent an infinite loop
        if ((iW < 1) || (iH < 1)) {
            return;
        }
        for (int x = 0; x < w; x += iW) {
            for (int y = 0; y < h; y += iH) {
                g.drawImage(backgroundIcon, x, y, null);
            }
        }
    }
}

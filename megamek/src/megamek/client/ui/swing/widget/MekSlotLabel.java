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

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;

public class MekSlotLabel extends PicMap {

    /**
     *
     */
    private static final long serialVersionUID = 5601930871313914270L;

    // Margins - used to draw 3D box
    private static final int MARGIN_WIDTH = 2;

    private BackGroundDrawer bgd = new BackGroundDrawer(null);

    public MekSlotLabel(String s, FontMetrics fm, Image im, Color textColor,
            Color bgColor) {
        super();
        PMPicArea pa = new PMPicArea(im);
        pa.setCursor(Cursor.getDefaultCursor());
        addElement(pa);
        PMSimpleLabel l = new PMSimpleLabel(s, fm, textColor);
        addElement(l);
        l.moveTo(pa.getBounds().width + 5, (pa.getBounds().height - l
                .getBounds().height)
                / 2 + l.getSize().height - l.getDescent());
        setContentMargins(MARGIN_WIDTH, MARGIN_WIDTH, MARGIN_WIDTH,
                MARGIN_WIDTH);
        setBackground(bgColor);
        addBgDrawer(bgd);
        drawBGImage();
    }

    private void drawBGImage() {
        Dimension d = getSize();
        int w = d.width;
        int h = d.height;
        Image BGImage = createImage(w, h);
        if (BGImage == null) {
            return;
        }
        Graphics g = BGImage.getGraphics();
        g.setColor(Color.green.darker().darker());
        g.fillRect(0, 0, w, h);
        g.setColor(Color.green.darker());
        g.fillRect(w - 2, 0, 2, h);
        g.fillRect(0, h - 2, w, 2);
        g.setColor(Color.green.darker().darker().darker());
        g.fillRect(0, 0, w, 2);
        g.fillRect(0, 0, 2, h);
        g.dispose();
        bgd.setImage(BGImage);
    }

    @Override
    public void onResize() {
        drawBGImage();
    }
}

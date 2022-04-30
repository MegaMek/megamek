/*
 * Copyright (c) 2003 - Ben Mazur (bmazur@sev.org).
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.client.ui.swing.widget;

import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * Simple polygonal area for PicMap component. Is represented by a set of
 * "Active"/"Idle" images but active area will be anyway defined by polygon.
 */
public class PMPicPolygonalArea extends PMGenericHotArea {
    private Image idleImage;
    private Image activeImage;
    private boolean highlight = true;
    private Polygon areaShape;
    private boolean selected = false;
    private boolean visible = true;

    public PMPicPolygonalArea(Polygon p, Image idle, Image active) {
        this.areaShape = p;
        this.idleImage = idle;
        this.activeImage = active;
    }

    public PMPicPolygonalArea(Polygon p, Image im) {
        this(p, im, null);
        highlight = false;
    }

    public PMPicPolygonalArea(Polygon p, Image im, boolean highlight) {
        this(p, im, null);
        this.highlight = highlight;
    }

    public void setIdleImage(Image im) {
        this.idleImage = im;
    }

    public Image getIdleImage() {
        return idleImage;
    }

    public Image getActiveImage() {
        return activeImage;
    }

    public void setActiveImage(Image im) {
        this.activeImage = im;
        highlight = activeImage != null;
    }

    // PMElement interface methods
    @Override
    public void translate(int x, int y) {
        areaShape.translate(x, y);
    }

    @Override
    public Rectangle getBounds() {
        return areaShape.getBounds();
    }

    @Override
    public void drawInto(Graphics g) {
        if ((g == null) || (!visible)) {
            return;
        }
        Rectangle r = getBounds();
        if (selected) {
            g.drawImage(activeImage, r.x, r.y, null);
        } else {
            g.drawImage(idleImage, r.x, r.y, null);
        }
    }

    @Override
    public void setVisible(boolean v) {
        visible = v;
    }

    // PMHotArea interface methods
    @Override
    public Shape getAreaShape() {
        return this.areaShape;
    }

    @Override
    public void onMouseOver(MouseEvent e) {
        if (highlight) {
            selected = true;
        }
        super.onMouseOver(e);

    }

    @Override
    public void onMouseExit(MouseEvent e) {
        if (highlight) {
            selected = false;
        }
        super.onMouseExit(e);
    }
}

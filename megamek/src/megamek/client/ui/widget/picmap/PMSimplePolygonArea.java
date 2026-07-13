/*
 * Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2003-2026 The MegaMek Team. All Rights Reserved.
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


package megamek.client.ui.widget.picmap;

import java.awt.AWTEventMulticaster;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import megamek.common.annotations.Nullable;

/**
 * Class for drawing a simple polygon, used to display the polygon areas for different locations on an Entity.
 */
public class PMSimplePolygonArea implements PMHotArea {

    /**
     * Told which location the user picked when this area is double-clicked; {@code null} when the diagram is
     * display-only.
     */
    private final LocationSelectListener locationSelectListener;

    /**
     * The location of systems corresponding to this polygon area
     */
    private final int loc;

    private ActionListener actionListener = null;

    public Color backColor = Color.lightGray;
    public Color normalBorderColor = Color.black;
    public Color highlightBorderColor = Color.red;
    private final boolean highlight;
    private final Polygon areaShape;
    private boolean selected = false;
    private boolean visible = true;

    private Cursor cursor = new Cursor(Cursor.HAND_CURSOR);

    public PMSimplePolygonArea(Polygon p, Color backColor, Color brdColor,
          Color hiBrdColor, boolean highlight, @Nullable LocationSelectListener locationSelectListener,
          int loc) {
        this.areaShape = p;
        if (backColor != null) {
            this.backColor = backColor;
        }

        if (brdColor != null) {
            this.normalBorderColor = brdColor;
        }

        if (hiBrdColor != null) {
            this.highlightBorderColor = hiBrdColor;
        }
        this.highlight = highlight;
        this.locationSelectListener = locationSelectListener;
        this.loc = loc;
    }

    public PMSimplePolygonArea(Polygon p, Color backColor, Color brdColor,
          @Nullable LocationSelectListener locationSelectListener, int loc) {
        this(p, backColor, brdColor, null, false, locationSelectListener, loc);
    }

    public PMSimplePolygonArea(Polygon p, @Nullable LocationSelectListener locationSelectListener, int loc) {
        this(p, null, null, null, true, locationSelectListener, loc);

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
        Color oldColor = g.getColor();
        g.setColor(this.backColor);
        g.fillPolygon(areaShape);
        if (selected && highlight) {
            g.setColor(highlightBorderColor);
        } else {
            g.setColor(this.normalBorderColor);
        }
        g.drawPolygon(this.areaShape);
        g.setColor(oldColor);
    }

    @Override
    public void setVisible(boolean v) {
        visible = v;
    }

    public synchronized void addActionListener(ActionListener l) {
        actionListener = AWTEventMulticaster.add(actionListener, l);
    }

    public synchronized void removeActionListener(ActionListener l) {
        actionListener = AWTEventMulticaster.remove(actionListener, l);
    }

    // PMHotArea interface methods
    @Override
    public Shape getAreaShape() {
        return this.areaShape;
    }

    @Override
    public Cursor getCursor() {
        return cursor;
    }

    @Override
    public void setCursor(Cursor c) {
        cursor = c;
    }

    @Override
    public void onMouseClick(MouseEvent e) {
        if ((e.getClickCount() == 2) && (locationSelectListener != null)) {
            locationSelectListener.locationSelected(loc);
        }
    }

    @Override
    public void onMouseOver(MouseEvent e) {
        if (highlight) {
            selected = true;
        }
    }

    @Override
    public void onMouseExit(MouseEvent e) {
        if (highlight) {
            selected = false;
        }
    }

    @Override
    public void onMouseDown(MouseEvent e) {

    }

    @Override
    public void onMouseUp(MouseEvent e) {

    }
}

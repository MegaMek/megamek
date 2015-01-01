/**
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * 
 *  This program is free software; you can redistribute it and/or modify it 
 *  under the terms of the GNU General Public License as published by the Free 
 *  Software Foundation; either version 2 of the License, or (at your option) 
 *  any later version.
 * 
 *  This program is distributed in the hope that it will be useful, but 
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 *  for more details.
 */

package megamek.client.ui.swing.widget;

import java.awt.AWTEventMulticaster;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

public class PMSimplePolygonArea implements PMHotArea {

    private ActionListener actionListener = null;

    public Color backColor = Color.lightGray;
    public Color normalBorderColor = Color.black;
    public Color highlightBorderColor = Color.red;
    private boolean highlight = true;
    private Polygon areaShape;
    private boolean selected = false;
    private boolean visible = true;

    private Cursor cursor = new Cursor(Cursor.HAND_CURSOR);

    public PMSimplePolygonArea(Polygon p, Color backColor, Color brdColor,
            Color hiBrdColor, boolean highlight) {
        this.areaShape = p;
        if (backColor != null)
            this.backColor = backColor;
        if (brdColor != null)
            this.normalBorderColor = brdColor;
        if (hiBrdColor != null)
            this.highlightBorderColor = hiBrdColor;
        this.highlight = highlight;
    }

    public PMSimplePolygonArea(Polygon p, Color backColor, Color brdColor) {
        this(p, backColor, brdColor, null, false);
    }

    public PMSimplePolygonArea(Polygon p) {
        this(p, null, null, null, true);
    }

    // PMElement interface methods
    public void translate(int x, int y) {
        areaShape.translate(x, y);
    }

    public Rectangle getBounds() {
        return areaShape.getBounds();
    }

    public void drawInto(Graphics g) {
        if ((g == null) || (!visible))
            return;
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
    public Shape getAreaShape() {
        return this.areaShape;
    }

    public Cursor getCursor() {
        return cursor;
    }

    public void setCursor(Cursor c) {
        cursor = c;
    }

    public void onMouseClick(MouseEvent e) {
        // !!!!!!code here
    }

    public void onMouseOver(MouseEvent e) {
        if (highlight)
            selected = true;
    }

    public void onMouseExit(MouseEvent e) {
        if (highlight)
            selected = false;
    }

    public void onMouseDown(MouseEvent e) {
    }

    public void onMouseUp(MouseEvent e) {
    }
}
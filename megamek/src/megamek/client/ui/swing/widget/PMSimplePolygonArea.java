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

import megamek.client.ui.swing.unitDisplay.UnitDisplay;

/**
 * Class for drawing a simple polygon, used to display the polgyon areas for 
 * different locations on an Entity.
 *
 */
public class PMSimplePolygonArea implements PMHotArea {

    /**
     * References to the UnitDisplay for call-back purposes
     */
    private UnitDisplay unitDisplay;
    
    /**
     * The location of systems corresponding to this polygon area
     */
    private int loc;
    
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
            Color hiBrdColor, boolean highlight, UnitDisplay unitDisplay,
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
        this.unitDisplay = unitDisplay;
        this.loc = loc;
    }

    public PMSimplePolygonArea(Polygon p, Color backColor, Color brdColor,
            UnitDisplay unitDisplay, int loc) {
        this(p, backColor, brdColor, null, false, unitDisplay, loc);
    }

    public PMSimplePolygonArea(Polygon p, UnitDisplay unitDisplay, int loc) {
        this(p, null, null, null, true, unitDisplay, loc);
        
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
        if (e.getClickCount() == 2) {
            unitDisplay.showSpecificSystem(loc);
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

/*
 * Copyright (c) 2000-2002 - Ben Mazur (bmazur@sev.org)
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

import java.awt.AWTEventMulticaster;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

/**
 * Simple rectangle hot are for PicMap component. Show single image when idle
 * and "hoghlite" image when mouse is over this area.
 */
public class PMPicArea implements PMHotArea {
    private int x = 0;
    private int y = 0;
    private Rectangle areaShape;
    private ActionListener actionListener = null;
    private Image idleImage;
    private Image activeImage;
    private boolean highlight = true;
    private boolean selected = false;
    private boolean visible = true;
    private Cursor cursor = new Cursor(Cursor.HAND_CURSOR);

    public PMPicArea(Image idle, Image active) {
        this.idleImage = idle;
        this.activeImage = active;
        areaShape = new Rectangle(x, y, idle.getWidth(null), idle.getHeight(null));
    }

    public PMPicArea(Image im) {
        this(im, null);
        highlight = false;
    }

    // PMElement interface methods
    @Override
    public void translate(int x, int y) {
        areaShape.translate(x, y);
        this.x = this.x + x;
        this.y = this.y + y;
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

        if (selected) {
            g.drawImage(activeImage, x, y, null);
        } else {
            g.drawImage(idleImage, x, y, null);
        }
    }

    public void setIdleImage(Image idle) {
        this.idleImage = idle;
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

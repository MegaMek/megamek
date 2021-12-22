/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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

import java.awt.Cursor;
import java.awt.Shape;
import java.awt.event.MouseEvent;

/**
 * Generic Hot area of PicMap component
 */
public interface PMHotArea extends PMElement {
    String MOUSE_CLICK_LEFT = "mouse_click_left";
    String MOUSE_CLICK_RIGHT = "mouse_click_right";
    String MOUSE_DOUBLE_CLICK = "mouse_double_click";
    String MOUSE_OVER = "mouse_over";
    String MOUSE_EXIT = "mouse_exit";
    String MOUSE_UP = "mouse_up";
    String MOUSE_DOWN = "mouse_down";

    Cursor getCursor();

    void setCursor(Cursor c);

    Shape getAreaShape();

    void onMouseClick(MouseEvent e);

    void onMouseOver(MouseEvent e);

    void onMouseExit(MouseEvent e);

    void onMouseDown(MouseEvent e);

    void onMouseUp(MouseEvent e);
}
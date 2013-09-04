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
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

/**
 * Abstract class which defines common functionality for all hot areas such as
 * event handling and dispatching.
 */

public abstract class PMGenericHotArea implements PMHotArea {

    private ActionListener actionListener = null;
    private Cursor cursor = new Cursor(Cursor.HAND_CURSOR);

    public Cursor getCursor() {
        return cursor;
    }

    public void setCursor(Cursor c) {
        cursor = c;
    }

    public synchronized void addActionListener(ActionListener l) {
        actionListener = AWTEventMulticaster.add(actionListener, l);
    }

    public synchronized void removeActionListener(ActionListener l) {
        actionListener = AWTEventMulticaster.remove(actionListener, l);
    }

    public void onMouseClick(MouseEvent e) {
        int modifiers = e.getModifiers();
        String command = ""; //$NON-NLS-1$

        if (0 != (modifiers & InputEvent.BUTTON1_MASK)) {
            command = PMHotArea.MOUSE_CLICK_LEFT;
        } else if (0 != (modifiers & InputEvent.BUTTON2_MASK)) {
            command = PMHotArea.MOUSE_CLICK_RIGHT;
        }

        if (e.getClickCount() > 1)
            command = PMHotArea.MOUSE_DOUBLE_CLICK;

        ActionEvent ae = new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
                command, modifiers);
        dispatchEvent(ae);
    }

    public void onMouseOver(MouseEvent e) {
        int modifiers = e.getModifiers();
        String command = PMHotArea.MOUSE_OVER;
        ActionEvent ae = new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
                command, modifiers);
        dispatchEvent(ae);

    }

    public void onMouseExit(MouseEvent e) {
        int modifiers = e.getModifiers();
        String command = PMHotArea.MOUSE_EXIT;
        ActionEvent ae = new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
                command, modifiers);
        dispatchEvent(ae);
    }

    public void onMouseDown(MouseEvent e) {
        int modifiers = e.getModifiers();
        String command = PMHotArea.MOUSE_DOWN;
        ActionEvent ae = new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
                command, modifiers);
        dispatchEvent(ae);
    }

    public void onMouseUp(MouseEvent e) {
        int modifiers = e.getModifiers();
        String command = PMHotArea.MOUSE_UP;
        ActionEvent ae = new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
                command, modifiers);
        dispatchEvent(ae);
    }

    private void dispatchEvent(ActionEvent ae) {
        if (actionListener != null) {
            actionListener.actionPerformed(ae);
        }
    }

}
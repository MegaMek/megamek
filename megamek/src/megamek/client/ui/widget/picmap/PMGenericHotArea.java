/*
 * Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2003-2025 The MegaMek Team. All Rights Reserved.
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
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

/**
 * Abstract class which defines common functionality for all hot areas such as event handling and dispatching.
 */

public abstract class PMGenericHotArea implements PMHotArea {

    private ActionListener actionListener = null;
    private Cursor cursor = new Cursor(Cursor.HAND_CURSOR);

    @Override
    public Cursor getCursor() {
        return cursor;
    }

    @Override
    public void setCursor(Cursor c) {
        cursor = c;
    }

    public synchronized void addActionListener(ActionListener l) {
        actionListener = AWTEventMulticaster.add(actionListener, l);
    }

    public synchronized void removeActionListener(ActionListener l) {
        actionListener = AWTEventMulticaster.remove(actionListener, l);
    }

    @Override
    public void onMouseClick(MouseEvent e) {
        int modifiers = e.getModifiersEx();
        String command = "";

        if (0 != (modifiers & InputEvent.BUTTON1_DOWN_MASK)) {
            command = MOUSE_CLICK_LEFT;
        } else if (0 != (modifiers & InputEvent.BUTTON2_DOWN_MASK)) {
            command = MOUSE_CLICK_RIGHT;
        }

        if (e.getClickCount() > 1) {
            command = MOUSE_DOUBLE_CLICK;
        }

        ActionEvent ae = new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
              command, modifiers);
        dispatchEvent(ae);
    }

    @Override
    public void onMouseOver(MouseEvent e) {
        int modifiers = e.getModifiersEx();
        ActionEvent ae = new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
              MOUSE_OVER, modifiers);
        dispatchEvent(ae);

    }

    @Override
    public void onMouseExit(MouseEvent e) {
        int modifiers = e.getModifiersEx();
        ActionEvent ae = new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
              MOUSE_EXIT, modifiers);
        dispatchEvent(ae);
    }

    @Override
    public void onMouseDown(MouseEvent e) {
        int modifiers = e.getModifiersEx();
        ActionEvent ae = new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
              MOUSE_DOWN, modifiers);
        dispatchEvent(ae);
    }

    @Override
    public void onMouseUp(MouseEvent e) {
        int modifiers = e.getModifiersEx();
        ActionEvent ae = new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
              MOUSE_UP, modifiers);
        dispatchEvent(ae);
    }

    private void dispatchEvent(ActionEvent ae) {
        if (actionListener != null) {
            actionListener.actionPerformed(ae);
        }
    }

}

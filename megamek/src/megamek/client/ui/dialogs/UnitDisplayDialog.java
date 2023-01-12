/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.unitDisplay.UnitDisplay;
import megamek.client.ui.swing.util.UIUtil;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class UnitDisplayDialog extends JDialog {
    //region Variable Declarations
    private final ClientGUI clientGUI;
    private static final String MSG_TITLE = Messages.getString("ClientGUI.MechDisplay");
    private static final GUIPreferences GUIP = GUIPreferences.getInstance();
    //endregion Variable Declarations

    //region Constructors
    public UnitDisplayDialog(final JFrame frame, final ClientGUI clientGUI) {
        super(frame, "", false);
        this.setTitle(MSG_TITLE);

        if (GUIP.getUnitDisplayStartTabbed()) {
            this.setLocation(GUIP.getUnitDisplayPosX(), GUIP.getUnitDisplayPosY());
            this.setSize(GUIP.getUnitDisplaySizeWidth(), GUIP.getUnitDisplaySizeHeight());
        }
        else {
            this.setLocation(GUIP.getUnitDisplayNontabbedPosX(), GUIP.getUnitDisplayNontabbedPosY());
            this.setSize(GUIP.getUnitDisplayNonTabbedSizeWidth(), GUIP.getUnitDisplayNonTabbedSizeHeight());
        }

        UIUtil.updateWindowBounds(this);
        this.setResizable(true);
        this.setFocusable(false);
        this.setFocusableWindowState(false);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evt) {
                GUIP.setUnitDisplayEnabled(false);
            }
        });

        this.clientGUI = clientGUI;
    }
    //endregion Constructors

    @Override
    protected void processWindowEvent(WindowEvent e) {
        super.processWindowEvent(e);
        if ((e.getID() == WindowEvent.WINDOW_DEACTIVATED) || (e.getID() == WindowEvent.WINDOW_CLOSING)) {
            if ((getSize().width * getSize().height) > 0) {
                if (GUIP.getUnitDisplayStartTabbed()) {
                    GUIP.setUnitDisplayPosX(getLocation().x);
                    GUIP.setUnitDisplayPosY(getLocation().y);
                    GUIP.setUnitDisplaySizeWidth(getSize().width);
                    GUIP.setUnitDisplaySizeHeight(getSize().height);
                } else {
                    GUIP.setUnitDisplayNontabbedPosX(getLocation().x);
                    GUIP.setUnitDisplayNontabbedPosY(getLocation().y);
                    GUIP.setUnitDisplayNonTabbedSizeWidth(getSize().width);
                    GUIP.setUnitDisplayNonTabbedSizeHeight(getSize().height);
                    clientGUI.getUnitDisplay().saveSplitterLoc();
                }
            }
        }
    }

    /**
     * In addition to the default Dialog processKeyEvent, this method
     * dispatches a KeyEvent to the client gui.
     * This enables all the gui hotkeys.
     */
    @Override
    protected void processKeyEvent(KeyEvent evt) {
        evt.setSource(clientGUI);
        clientGUI.getMenuBar().dispatchEvent(evt);
        // Make the source be the ClientGUI and not the dialog
        // This prevents a ClassCastException in ToolTipManager
        clientGUI.getCurrentPanel().dispatchEvent(evt);
        if (!evt.isConsumed()) {
            super.processKeyEvent(evt);
        }
    }
}

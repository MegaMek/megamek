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
package megamek.client.ui.swing.forceDisplay;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.UIUtil;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class ForceDisplayDialog extends JDialog {
    //region Variable Declarations
    private final ClientGUI clientGUI;
    private static final GUIPreferences GUIP = GUIPreferences.getInstance();
    //endregion Variable Declarations

    //region Constructors
    public ForceDisplayDialog(final JFrame frame, final ClientGUI clientGUI) {
        super(frame, "", false);
        this.setTitle(Messages.getString("ClientGUI.ForceDisplay"));

        this.clientGUI = clientGUI;

        this.setLocation(GUIP.getForceDisplayPosX(), GUIP.getForceDisplayPosY());
        this.setSize(GUIP.getForceDisplaySizeWidth(), GUIP.getForceDisplaySizeHeight());

        UIUtil.updateWindowBounds(this);
        this.setResizable(true);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evt) {
                GUIP.setUnitDisplayEnabled(false);
            }
        });
    }
    //endregion Constructors

    public void saveSettings() {
        if ((getSize().width * getSize().height) > 0) {
            GUIP.setForceDisplayPosX(getLocation().x);
            GUIP.setForceDisplayPosY(getLocation().y);
            GUIP.setForceDisplaySizeWidth(getSize().width);
            GUIP.setForceDisplaySizeHeight(getSize().height);
        }
    }

    @Override
    protected void processWindowEvent(WindowEvent e) {
        super.processWindowEvent(e);
        if ((e.getID() == WindowEvent.WINDOW_DEACTIVATED) || (e.getID() == WindowEvent.WINDOW_CLOSING)) {
            saveSettings();
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

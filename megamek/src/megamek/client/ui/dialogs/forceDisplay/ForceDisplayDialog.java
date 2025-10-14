/*
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs.forceDisplay;

import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JDialog;
import javax.swing.JFrame;

import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.util.UIUtil;

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
                GUIP.setForceDisplayEnabled(false);
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
     * In addition to the default Dialog processKeyEvent, this method dispatches a KeyEvent to the client gui. This
     * enables all the gui hotkeys.
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

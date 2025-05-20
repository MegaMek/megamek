/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs;

import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JDialog;
import javax.swing.JFrame;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.Entity;

/**
 * A dialog for displaying detailed unit information in MegaMek.
 * <p>
 *  This dialog serves as a container for the UnitDisplay component, which provides detailed information about game
 *  entities (meks, vehicles, infantry, etc.).
 *  The dialog can operate in two modes:
 * </p>
 * <ul>
 *   <li>As a persistent, reusable dialog that is hidden when closed</li>
 *   <li>As a disposable dialog that is destroyed when closed</li>
 * </ul>
 * <p>
 *  The dialog manages its position and size through GUIPreferences, storing separate settings for tabbed and
 *  non-tabbed display modes.
 * </p>
 */
public class UnitDisplayDialog extends JDialog {

    private final ClientGUI clientGUI;
    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    /**
     * Shows an entity in a UnitDisplayDialog, either reusing the existing dialog instance
     * or creating a new one.
     *
     * @param frame the parent frame for the dialog
     * @param entity the entity to display
     * @param newInstance if true, creates a new dialog that will be disposed on close;
     *                    if false, reuses or creates a persistent dialog instance
     */
    public static void showEntity(final JFrame frame, Entity entity, final boolean newInstance) {
        UnitDisplayContainer displayContainer = getDisplayContainer(frame, newInstance);
        displayContainer.getUnitDisplay().displayEntity(entity);
        displayContainer.getDialog().setVisible(true);
    }

    /**
     * Factory method to get the appropriate UnitDisplayContainer based on whether
     * a new instance is requested.
     *
     * @param frame the parent frame for the dialog
     * @param newInstance if true, returns a DisposableDisplayContainer;
     *                    if false, returns the shared container instance
     * @return a UnitDisplayContainer with the appropriate lifecycle management
     */
    private static UnitDisplayContainer getDisplayContainer(JFrame frame, boolean newInstance) {
        return newInstance ?
                     new DisposableDisplayContainer(frame) :
                     SharedDisplayContainer.getInstance(frame);
    }

    /**
     * Creates a new UnitDisplayDialog.
     *
     * @param frame the parent frame for this dialog
     * @param clientGUI the ClientGUI reference, which can be null for standalone usage
     */
    public UnitDisplayDialog(final JFrame frame, final ClientGUI clientGUI) {
        super(frame, "", false);
        this.clientGUI = clientGUI;
        this.setTitle(Messages.getString("ClientGUI.MekDisplay"));

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

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evt) {
                GUIP.setUnitDisplayEnabled(false);
            }
        });

    }
    //endregion Constructors

    /**
     * Saves the current size and position of the dialog to preferences.
     * Different settings are stored based on whether the display is in
     * tabbed or non-tabbed mode.
     */
    public void saveSettings() {
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
                if (clientGUI != null) {
                    clientGUI.getUnitDisplay().saveSplitterLoc();
                }
            }
        }
    }

    /**
     * Overrides the default window event processing to save settings when
     * the window is deactivated or closing.
     *
     * @param e the window event
     */
    @Override
    protected void processWindowEvent(WindowEvent e) {
        super.processWindowEvent(e);
        if ((e.getID() == WindowEvent.WINDOW_DEACTIVATED) || (e.getID() == WindowEvent.WINDOW_CLOSING)) {
            saveSettings();
        }
    }

    /**
     * Processes key events and forwards them to the client GUI to enable
     * hotkey functionality throughout the application.
     *
     * @param evt the key event to process
     */
    @Override
    protected void processKeyEvent(KeyEvent evt) {
        if (clientGUI != null) {
            evt.setSource(clientGUI);
            clientGUI.getMenuBar().dispatchEvent(evt);
            // Make the source be the ClientGUI and not the dialog
            // This prevents a ClassCastException in ToolTipManager
            clientGUI.getCurrentPanel().dispatchEvent(evt);
        }
        if (!evt.isConsumed()) {
            super.processKeyEvent(evt);
        }
    }
}

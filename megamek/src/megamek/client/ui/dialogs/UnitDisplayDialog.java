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

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class UnitDisplayDialog extends JDialog {
    //region Variable Declarations
    private UnitDisplay unitDisplay;

    private final ClientGUI clientGUI;
    private static final GUIPreferences GUIP = GUIPreferences.getInstance();
    //endregion Variable Declarations

    //region Constructors
    public UnitDisplayDialog(final JFrame frame, final UnitDisplay unitDisplay,
                             final ClientGUI clientGUI) {
        super(frame, Messages.getString("ClientGUI.MechDisplay"), false);
        setUnitDisplay(unitDisplay);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evt) {
                GUIP.setUnitDisplayEnabled(false);
            }
        });

        this.clientGUI = clientGUI;
    }
    //endregion Constructors

    //region Getters/Setters
    public UnitDisplay getUnitDisplay() {
        return unitDisplay;
    }

    public void setUnitDisplay(final UnitDisplay unitDisplay) {
        this.unitDisplay = unitDisplay;
    }
    //endregion Getters/Setters

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

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

import megamek.MegaMek;
import megamek.client.ui.baseComponents.AbstractDialog;
import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.unitDisplay.UnitDisplay;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

public class UnitDisplayDialog extends AbstractDialog {
    //region Variable Declarations
    private final ClientGUI clientGUI;
    private UnitDisplay unitDisplay;
    //endregion Variable Declarations

    //region Constructors
    public UnitDisplayDialog(final JFrame frame, final ClientGUI clientGUI) {
        super(frame, "UnitDisplayDialog", "UnitDisplayDialog.title");
        this.clientGUI = clientGUI;
        initialize();
    }
    //endregion Constructors

    //region Getters/Setters
    public ClientGUI getClientGUI() {
        return clientGUI;
    }

    public UnitDisplay getUnitDisplay() {
        return unitDisplay;
    }

    public void setUnitDisplay(final UnitDisplay unitDisplay) {
        this.unitDisplay = unitDisplay;
    }
    //endregion Getters/Setters

    //region Initialization
    @Override
    protected Container createCenterPane() {
        setUnitDisplay(new UnitDisplay(getClientGUI(), getClientGUI().getController()));
        getUnitDisplay().addMechDisplayListener(getClientGUI().getBoardView());
        return getUnitDisplay();
    }
    //endregion Initialization

    @Override
    protected void cancelAction() {
        MegaMek.getMMOptions().setShowUnitDisplay(false);
    }

    /**
     * In addition to the default Dialog processKeyEvent, this method
     * dispatches a KeyEvent to the client gui.
     * This enables all the gui hotkeys.
     */
    @Override
    protected void processKeyEvent(KeyEvent evt) {
        evt.setSource(getClientGUI());
        getClientGUI().getMenuBar().dispatchEvent(evt);
        // Make the source be the ClientGUI and not the dialog
        // This prevents a ClassCastException in ToolTipManager
        getClientGUI().getCurrentPanel().dispatchEvent(evt);
        if (!evt.isConsumed()) {
            super.processKeyEvent(evt);
        }
    }
}

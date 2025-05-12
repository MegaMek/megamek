/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.swing.phaseDisplay;

import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.phaseDisplay.dialog.SimpleNagNotice;

import javax.swing.JOptionPane;

public abstract class SimpleConfirmDialog extends SimpleNagNotice {

    private boolean isOKSelected;

    protected SimpleConfirmDialog(ClientGUI clientGui) {
        super(clientGui);
    }

    public boolean isOKSelected() {
        return isOKSelected;
    }

    public boolean isCanceled() {
        return !isOKSelected;
    }

    @Override
    final protected String preferenceKey() {
        return "";
    }

    /**
     * Shows this confirm dialog. Note that while the dialog is shown, BoardView tooltips are suspended so they don't
     * overlap the dialog.
     */
    @Override
    public final void show() {
        if (!initialized) {
            initialized = true;
            initialize();
        }

        clientGui.suspendBoardTooltips();

        isOKSelected = JOptionPane.showConfirmDialog(clientGui.getFrame(), contentPanel, title(),
              JOptionPane.OK_CANCEL_OPTION)
              == JOptionPane.OK_OPTION;

        clientGui.activateBoardTooltips();
    }
}

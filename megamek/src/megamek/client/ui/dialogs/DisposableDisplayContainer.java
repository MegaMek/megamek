/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

import megamek.client.ui.swing.unitDisplay.UnitDisplay;

import javax.swing.JDialog;
import javax.swing.JFrame;
import java.awt.BorderLayout;

/**
 * A UnitDisplayContainer implementation that creates a one-time use dialog and display.
 * This container creates a new UnitDisplayDialog and UnitDisplay instance with each instantiation.
 * The dialog will be automatically disposed when closed by the user.
 * @author Luana Coppio
 */
public class DisposableDisplayContainer implements UnitDisplayContainer {
    private final UnitDisplayDialog dialog;
    private final UnitDisplay unitDisplay;

    /**
     * Creates a new disposable container with a fresh dialog and display
     *
     * @param frame the parent frame for the dialog
     */
    DisposableDisplayContainer(JFrame frame) {
        dialog = new UnitDisplayDialog(frame, null);
        unitDisplay = new UnitDisplay(null, null);
        dialog.add(unitDisplay, BorderLayout.CENTER);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }

    /**
     * Returns the shared UnitDisplay instance
     *
     * @return the UnitDisplay component
     */
    @Override
    public UnitDisplay getUnitDisplay() {
        return unitDisplay;
    }

    /**
     * Returns the shared dialog window
     *
     * @return the JDialog instance
     */
    @Override
    public JDialog getDialog() {
        return dialog;
    }
}

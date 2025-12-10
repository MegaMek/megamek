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
package megamek.client.ui.dialogs.unitSelectorDialogs;

import javax.swing.JFrame;
import javax.swing.JPanel;

import megamek.client.ui.dialogs.UnitLoadingDialog;
import megamek.common.TechConstants;

/**
 * This Dialog is used as a unit browser for MegaMek's main menu. It doesn't allow selecting units. Since there are no
 * game options in the main menu, the options are fixed to show units without restriction.
 */
public class MainMenuUnitBrowserDialog extends AbstractUnitSelectorDialog {

    public MainMenuUnitBrowserDialog(JFrame parent, UnitLoadingDialog unitLoadingDialog) {
        super(parent, unitLoadingDialog);
        enableYearLimits = false;
        allowedYear = ALLOWED_YEAR_ANY;
        canonOnly = false;
        allowInvalid = true;
        gameTechLevel = TechConstants.T_SIMPLE_UNOFFICIAL;
        initialize();
    }

    @Override
    public void updateOptionValues() {}

    @Override
    protected JPanel createButtonsPanel() {
        return new JPanel();
    }

    @Override
    protected void select(boolean close) {}
}

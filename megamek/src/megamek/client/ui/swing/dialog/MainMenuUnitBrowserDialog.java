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
package megamek.client.ui.swing.dialog;

import megamek.client.ui.swing.UnitLoadingDialog;
import megamek.common.TechConstants;

import javax.swing.*;

/**
 * This Dialog is used as a unit browser for MegaMek's main menu. It doesn't allow selecting units.
 * Since there are no game options in the main menu, the options are fixed to show units without
 * restriction.
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
    public void updateOptionValues() { }

    @Override
    protected JPanel createButtonsPanel() {
        return new JPanel();
    }

    @Override
    protected void select(boolean close) { }
}

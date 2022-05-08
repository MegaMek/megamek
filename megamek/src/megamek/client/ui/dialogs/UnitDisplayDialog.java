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

import megamek.client.ui.baseComponents.AbstractDialog;
import megamek.client.ui.swing.unitDisplay.UnitDisplay;

import javax.swing.*;
import java.awt.*;

public class UnitDisplayDialog extends AbstractDialog {
    //region Variable Declarations
    private UnitDisplay unitDisplay;
    //endregion Variable Declarations

    //region Constructors
    public UnitDisplayDialog(final JFrame frame, final UnitDisplay unitDisplay) {
        super(frame, true, "UnitDisplayDialog", "UnitDisplayDialog.title");
        setUnitDisplay(unitDisplay);
        initialize();
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

    //region Initialization
    @Override
    protected Container createCenterPane() {
        return getUnitDisplay();
    }
    //endregion Initialization
}

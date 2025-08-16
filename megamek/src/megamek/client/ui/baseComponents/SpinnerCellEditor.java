/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.baseComponents;

import java.awt.Component;
import java.io.Serial;
import javax.swing.AbstractSpinnerModel;
import javax.swing.DefaultCellEditor;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;

/**
 * The SpinnerCellEditor is a Cell Editor for a cell that is edited through the provided spinner model.
 */
public class SpinnerCellEditor extends DefaultCellEditor {
    //region Variable Declarations
    @Serial
    private static final long serialVersionUID = 7956499745127048276L;
    private final JSpinner spinner;
    //endregion Variable Declarations

    //region Constructors
    public SpinnerCellEditor(final AbstractSpinnerModel spinnerModel, final boolean editable) {
        super(new JTextField());
        this.spinner = new JSpinner(spinnerModel);
        ((JSpinner.DefaultEditor) getSpinner().getEditor()).getTextField().setEditable(editable);
    }
    //endregion Constructors

    //region Getters
    public JSpinner getSpinner() {
        return spinner;
    }
    //endregion Getters

    @Override
    public Object getCellEditorValue() {
        return getSpinner().getValue();
    }

    @Override
    public Component getTableCellEditorComponent(final JTable table, final Object value,
          final boolean isSelected, final int row,
          final int column) {
        getSpinner().setValue(value);
        return getSpinner();
    }
}

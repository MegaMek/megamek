/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.baseComponents;

import javax.swing.*;
import java.awt.*;

/**
 * The SpinnerCellEditor is a Cell Editor for a cell that is edited through the provided spinner
 * model.
 */
public class SpinnerCellEditor extends DefaultCellEditor {
    //region Variable Declarations
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

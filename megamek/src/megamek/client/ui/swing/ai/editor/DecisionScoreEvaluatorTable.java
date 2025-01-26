/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 */

package megamek.client.ui.swing.ai.editor;

import megamek.ai.utility.Decision;
import megamek.ai.utility.DecisionScoreEvaluator;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.util.ArrayList;
import java.util.List;

public class DecisionScoreEvaluatorTable<DECISION extends Decision<?,?>> extends JTable {

    public DecisionScoreEvaluatorTable(
        DecisionTableModel<DECISION> model) {
        super(model);
    }

    public void createUIComponents() {
        //
    }

    @Override
    @SuppressWarnings("unchecked")
    public DecisionTableModel<DECISION> getModel() {
        return (DecisionTableModel<DECISION>) super.getModel();
    }

    @Override
    public TableCellEditor getCellEditor(int row, int column) {
        // Decision is column 1, Evaluator is column 2
        if (column == 1) {
            return new DefaultCellEditor(new JTextField());
        } else if (column == 2) {
            return new SpinnerCellEditor(1d, 0d, 5d, 0.1d);
        }
        return super.getCellEditor(row, column);
    }

}

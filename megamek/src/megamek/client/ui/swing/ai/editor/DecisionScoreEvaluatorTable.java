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

import megamek.ai.utility.Action;
import megamek.ai.utility.Decision;
import megamek.ai.utility.DecisionScoreEvaluator;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.util.List;

public class DecisionScoreEvaluatorTable<DECISION extends Decision<?,?>, DSE extends DecisionScoreEvaluator<?,?>> extends JTable {

    private final Action[] actionList;
    private final List<DSE> dse;

    public DecisionScoreEvaluatorTable(
        DecisionScoreEvaluatorTableModel<DECISION> model, Action[] actionList, List<DSE> dse) {
        super(model);
        this.actionList = actionList;
        this.dse = dse;
    }

    public void createUIComponents() {
        //
    }

    @Override
    public DecisionScoreEvaluatorTableModel<DECISION> getModel() {
        return (DecisionScoreEvaluatorTableModel<DECISION>) super.getModel();
    }

    @Override
    public TableCellEditor getCellEditor(int row, int column) {
        // Decision is column 1, Evaluator is column 2
        if (column == 1) {
            // Create a combo box for Decisions
            JComboBox<Action> cb = new JComboBox<>(
                actionList
            );
            return new DefaultCellEditor(cb);
        } else if (column == 2) {
            // Create a combo box for Evaluators
            var cb = new JComboBox<>(
                dse.toArray(new DecisionScoreEvaluator[0])
            );
            return new DefaultCellEditor(cb);
        }
        return super.getCellEditor(row, column);
    }

}

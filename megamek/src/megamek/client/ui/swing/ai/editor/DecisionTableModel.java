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

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;


public  class DecisionTableModel<DECISION extends Decision<?,?>> extends AbstractTableModel {

    private final List<DECISION> rows;
    private final String[] columnNames = { "ID", "Decision", "Evaluator" };

    public DecisionTableModel(List<DECISION> initialRows) {
        this.rows = new ArrayList<>(initialRows);
    }

    public void addRow(DECISION dse) {
        rows.add(dse);
        fireTableRowsInserted(rows.size() - 1, rows.size() - 1);
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return columnNames[columnIndex];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        DECISION dse = rows.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> rowIndex;  // or some ID from dse
            case 1 -> dse.getAction().getActionName();
            case 2 -> dse.getDecisionScoreEvaluator().getName();
            default -> null;
        };
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        // Let's keep ID read-only, but allow editing for Decision & Evaluator
        return columnIndex == 1 || columnIndex == 2;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        var dse = rows.get(rowIndex);

        switch (columnIndex) {
            case 1:
                if (aValue instanceof Action action) {
                    dse.setAction(action);
                }
                break;
            case 2:
                if (aValue instanceof DecisionScoreEvaluator decisionScoreEvaluator) {
                    dse.setDecisionScoreEvaluator(decisionScoreEvaluator);
                }
                break;
            default:
                // no-op
        }
        rows.set(rowIndex, dse);
        fireTableCellUpdated(rowIndex, columnIndex);
    }

    /** Helper to fetch the updated data */
    public List<DECISION> getDecisions() {
        return rows;
    }
}

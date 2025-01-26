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

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class DecisionTableModel<DECISION extends Decision<?,?>> extends AbstractTableModel {

    private final List<DECISION> rows;
    private final String[] columnNames = { "ID", "Decision", "Weight", "Evaluator" };
    private final Set<Integer> editableColumns = Set.of(1, 2, 3);

    public DecisionTableModel() {
        this.rows = new ArrayList<>();
    }

    public DecisionTableModel(List<DECISION> initialRows) {
        this.rows = new ArrayList<>(initialRows);
    }

    public void addRow(DECISION dse) {
        rows.add(dse);
        fireTableRowsInserted(rows.size() - 1, rows.size() - 1);
    }

    public void deleteRow(int row) {
        rows.remove(row);
        fireTableRowsDeleted(row, row);
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
            case 1 -> dse.getName();
            case 2 -> dse.getWeight();
            case 3 -> dse.getDecisionScoreEvaluator().getName();
            default -> null;
        };
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return editableColumns.contains(columnIndex);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        var dse = rows.get(rowIndex);

        switch (columnIndex) {
            case 1:
                if (aValue instanceof String name) {
                    dse.setName(name);
                }
                break;
            case 2:
                if (aValue instanceof Number weight) {
                    dse.setWeight((Double) weight);
                }
            case 3:
                // noinspection rawtypes
                if (aValue instanceof DecisionScoreEvaluator decisionScoreEvaluator) {
                    // noinspection unchecked
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

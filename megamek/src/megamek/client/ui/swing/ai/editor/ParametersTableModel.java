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

import megamek.ai.utility.Consideration;
import megamek.ai.utility.ParameterTitleTooltip;
import megamek.logging.MMLogger;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParametersTableModel extends AbstractTableModel {
    private static final MMLogger logger = MMLogger.create(ParametersTableModel.class);
    private final List<Row> rowValues = new ArrayList<>();
    private final String[] columnNames = { "Name", "Value" };
    private record Row(String name, Object value, Class<?> clazz, ParameterTitleTooltip parameterTitleTooltip) {}

    public ParametersTableModel() {
        fireTableDataChanged();
    }

    public ParametersTableModel(Consideration<?,?> consideration) {
        setParameters(consideration);
    }

    public void setParameters(Consideration<?,?> consideration) {
        setParameters(consideration.getParameters(), consideration.getParameterTypes(), consideration.getParameterTooltips());
    }

    public void setEmptyParameters() {
        rowValues.clear();
        fireTableDataChanged();
    }

    private void setParameters(Map<String, Object> parameters,
                               Map<String, Class<?>> parameterTypes,
                               Map<String, ParameterTitleTooltip> parameterTitleTooltipMap) {
        rowValues.clear();
        parameters.forEach((k, v) -> rowValues.add(new Row(k, v, parameterTypes.get(k), parameterTitleTooltipMap.get(k))));
        fireTableDataChanged();
    }

    public Map<String, Object> getParameters() {
        Map<String, Object> hashRows = new HashMap<>();
        rowValues.forEach(row -> hashRows.put(row.name, row.value));
        return hashRows;
    }

    @Override
    public int getRowCount() {
        return rowValues.size();
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
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 0) {
            return String.class;
        } else {
            return Object.class;
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 1;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        var row = rowValues.get(rowIndex);
        if (columnIndex == 0) {
            return row.name;
        }
        return row.value;
    }

    public Class<?> getParameterValueClassAt(int rowIndex) {
        return rowValues.get(rowIndex).clazz;
    }

    public String getTooltipAt(int rowIndex) {
        return rowValues.get(rowIndex).parameterTitleTooltip.getTooltip();
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (rowIndex > rowValues.size()) {
            return;
        }
        var row = rowValues.get(rowIndex);
        if (columnIndex == 1) {
            if (aValue.getClass().equals(row.clazz)) {
                rowValues.set(rowIndex, new Row(row.name, aValue, row.clazz, row.parameterTitleTooltip));
            } else {
                logger.error("Invalid value type: " + aValue.getClass() + " for " + row.clazz, "Invalid value type");
            }
        }
    }
}

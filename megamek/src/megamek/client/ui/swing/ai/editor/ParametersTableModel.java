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

import megamek.logging.MMLogger;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParametersTableModel extends AbstractTableModel {
    private static final MMLogger logger = MMLogger.create(ParametersTableModel.class);
    private final Map<String, Object> hashRows = new HashMap<>();
    private final List<Row> rowValues = new ArrayList<>();
    private final String[] columnNames = { "Name", "Value" };
    private final Class<?>[] columnClasses = { String.class, Object.class };
    private record Row(String name, Object value) {}

    public ParametersTableModel() {
    }

    public ParametersTableModel(Map<String, Object> parameters) {
        this.hashRows.putAll(parameters);
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            rowValues.add(new Row(entry.getKey(), entry.getValue()));
        }
    }

    public void setParameters(Map<String, Object> parameters) {
        hashRows.clear();
        rowValues.clear();
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            hashRows.put(entry.getKey(), entry.getValue());
            rowValues.add(new Row(entry.getKey(), entry.getValue()));
        }
        fireTableDataChanged();
    }

    public void addRow(String parameterName, Object value) {
        if (hashRows.containsKey(parameterName)) {
            logger.formattedErrorDialog("Parameter already exists",
                "Could not add parameter {}, another parameters with the same name is already present.", parameterName);
            return;
        }
        hashRows.put(parameterName, value);
        rowValues.add(new Row(parameterName, value));
        fireTableRowsInserted(rowValues.size() - 1, rowValues.size() - 1);
    }

    public String newParameterName() {
        int i = 0;
        while (hashRows.containsKey("Parameter " + i)) {
            i++;
        }
        return "Parameter " + i;
    }

    public Map<String, Object> getParameters() {
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
        return columnClasses[columnIndex];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        var row = rowValues.get(rowIndex);
        if (columnIndex == 0) {
            return row.name;
        }
        return row.value;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        var row = rowValues.get(rowIndex);
        if (columnIndex == 1) {
            rowValues.set(rowIndex, new Row(row.name, aValue));
            hashRows.put(row.name, aValue);
        } else {
            if (hashRows.containsKey((String) aValue)) {
                logger.formattedErrorDialog("Parameter already exists",
                    "Could not rename parameter %s, another parameters with the same name is already present.", aValue);
                return;
            }
            rowValues.set(rowIndex, new Row((String) aValue, row.value));
            hashRows.remove(row.name);
            hashRows.put((String) aValue, row.value);
        }
    }
}

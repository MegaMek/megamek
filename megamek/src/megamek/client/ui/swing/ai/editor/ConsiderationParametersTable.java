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

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;

public class ConsiderationParametersTable extends JTable {

    public ConsiderationParametersTable(
        ParametersTableModel model) {
        super(model);
    }

    public void createUIComponents() {
        //
    }

    @Override
    @SuppressWarnings("unchecked")
    public ParametersTableModel getModel() {
        return (ParametersTableModel) super.getModel();
    }

    @Override
    public TableCellEditor getCellEditor(int row, int column) {
        if (column == 1) {
            var clazz = getModel().getParameterValueClassAt(row);
            var value = getModel().getValueAt(row, column);
            var tooltip = getModel().getTooltipAt(row);
            if (clazz == null) {
                // Should actually throw an error here...
                return super.getCellEditor(row, column);
            }
            if (clazz.equals(Boolean.class)) {
                return new DefaultCellEditor(new JCheckBox());
            } else if (clazz.equals(Double.class)) {
                return new SpinnerCellEditor(
                    (double) (value == null ? 0d : value),
                    Double.MIN_VALUE,
                    Double.MAX_VALUE,
                    0.1d,
                    tooltip
                );
            } else if (clazz.equals(Integer.class)) {
                return new SpinnerCellEditor(
                    (int) (value == null ? 0 : value),
                    Integer.MIN_VALUE,
                    Integer.MAX_VALUE,
                    1,
                    tooltip
                );
            } else if (clazz.equals(String.class)) {
                return new DefaultCellEditor(new JTextField());
            } else if (clazz.isEnum()) {
                var cb = new JComboBox<>(
                    clazz.getEnumConstants()
                );
                cb.setToolTipText(tooltip);
                return new DefaultCellEditor(cb);
            }
        }
        return super.getCellEditor(row, column);
    }

    public static class SpinnerCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final JSpinner spinner;

        public SpinnerCellEditor(double defaultValue, double min, double max, double step, String tooltip) {
            spinner = new JSpinner(new SpinnerNumberModel(min, min, max, step));
            spinner.setValue(defaultValue);
            spinner.setToolTipText(tooltip);
            JComponent editor = spinner.getEditor();
            if (editor instanceof JSpinner.DefaultEditor) {
                JFormattedTextField textField = ((JSpinner.DefaultEditor) editor).getTextField();
                textField.setHorizontalAlignment(JFormattedTextField.LEFT);
            }
        }

        public SpinnerCellEditor(int defaultValue, int min, int max, int step, String tooltip) {
            spinner = new JSpinner(new SpinnerNumberModel(min, min, max, step));
            spinner.setValue(defaultValue);
            spinner.setToolTipText(tooltip);
            JComponent editor = spinner.getEditor();
            if (editor instanceof JSpinner.DefaultEditor) {
                JFormattedTextField textField = ((JSpinner.DefaultEditor) editor).getTextField();
                textField.setHorizontalAlignment(JFormattedTextField.LEFT);
            }
        }

        @Override
        public Object getCellEditorValue() {
            return spinner.getValue();
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            spinner.setValue(value);
            return spinner;
        }
    }

}

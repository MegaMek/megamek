/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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

package megamek.client.ui.swing;

import java.awt.FlowLayout;
import java.awt.event.*;
import java.util.StringTokenizer;

import javax.swing.*;
import megamek.client.ui.swing.util.UIUtil.FixedYPanel;
import megamek.common.options.*;

/** @author Cord Awtry */
public class DialogOptionComponent extends FixedYPanel implements ItemListener, Comparable<DialogOptionComponent> {

    private static final long serialVersionUID = -4190538980884459746L;

    IOption option;

    private JCheckBox checkbox;
    private JComboBox<String> choice;
    private JTextField textField;
    private final DialogOptionListener dialogOptionListener;
    
    /** Value used to force a change */
    private boolean hasOptionChanged = false;

    public DialogOptionComponent(DialogOptionListener parent, IOption option) {
        this(parent, option, true);
    }

    public DialogOptionComponent(DialogOptionListener parent, IOption option,
            boolean editable) {
        this(parent, option, editable, false);
    }

    public DialogOptionComponent(DialogOptionListener parent, IOption option,
            boolean editable, boolean choiceLabelFirst) {
        dialogOptionListener = parent;
        this.option = option;

        setLayout(new FlowLayout(FlowLayout.LEFT, 5, 2));
        switch (option.getType()) {
            case IOption.BOOLEAN:
                checkbox = new JCheckBox("", option.booleanValue());
                checkbox.addItemListener(this);
                checkbox.setToolTipText(convertToHtml(option.getDescription()));
                checkbox.setEnabled(editable);
                JLabel label = new JLabel(option.getDisplayableName());
                label.setLabelFor(checkbox);
                label.setToolTipText(convertToHtml(option.getDescription()));
                label.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent evt) {
                        if (checkbox.isEnabled()) {
                            checkbox.setSelected(!checkbox.isSelected());
                        }
                    }
                });
                add(Box.createHorizontalStrut(10));
                add(checkbox);
                add(label);
                break;
            case IOption.CHOICE:
                choice = new JComboBox<>();
                label = new JLabel(option.getDisplayableName());
                label.setLabelFor(choice);
                label.setToolTipText(convertToHtml(option.getDescription()));
                choice.setEnabled(editable);
                if (choiceLabelFirst) {
                    add(choice);
                    add(label);
                } else {
                    add(label);
                    add(choice);
                }
                break;
            default:
                textField = new JTextField(option.stringValue(), option.getTextFieldLength());
                textField.setHorizontalAlignment(JTextField.CENTER);
                label = new JLabel(option.getDisplayableName());
                label.setToolTipText(convertToHtml(option.getDescription()));
                label.setLabelFor(textField);
                label.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent evt) {
                        if (textField.isEnabled()) {
                            textField.requestFocus();
                            textField.selectAll();
                        }
                    }
                });
                textField.setEnabled(editable);
                if (!option.isLabelBeforeTextField()) {
                    add(Box.createHorizontalStrut(2));
                }
                add(textField);
                add(label);
                break;
        }
    }

    public static String convertToHtml(String source) {
        StringBuilder result = new StringBuilder();
        result.append("<html><div width=500>");
        StringTokenizer tok = new StringTokenizer(source, "\n");
        while (tok.hasMoreTokens()) {
            result.append(tok.nextToken());
            result.append("<br>");
        }
        result.append("</DIV></html>");
        return result.toString();
    }
    
    public boolean hasChanged() {
        return !option.getValue().equals(getValue()) || hasOptionChanged;
    }
    
    public void setOptionChanged(boolean v) {
        hasOptionChanged = v;
    }

    public Object getValue() {
        switch (option.getType()) {
            case IOption.BOOLEAN:
                return checkbox.isSelected();
            case IOption.INTEGER:
                return textField.getText().isBlank() ? 0 : Integer.parseInt(textField.getText());
            case IOption.FLOAT:
                return textField.getText().isBlank() ? 0 : Float.parseFloat(textField.getText());
            case IOption.STRING:
                return textField.getText();
            case IOption.CHOICE:
                return choice.getSelectedItem();
            default:
                return null;
        }
    }

    public void setValue(Object v) {
        switch (option.getType()) {
            case IOption.BOOLEAN:
                checkbox.setSelected((Boolean) v);
                break;
            case IOption.INTEGER:
            case IOption.FLOAT:
            case IOption.STRING:
                textField.setText((String) v);
                break;
            case IOption.CHOICE:
                choice.setSelectedItem(v);
            default:
        }
    }

    public IOption getOption() {
        return option;
    }

    /**
     * Update the option component so that it is editable or view-only.
     *
     * @param editable - <code>true</code> if the contents of the component
     *            are editable, <code>false</code> if they are view-only.
     */
    public void setEditable(boolean editable) {

        // Update the correct control.
        switch (option.getType()) {
            case IOption.BOOLEAN:
                checkbox.setEnabled(editable);
                break;
            case IOption.CHOICE:
                choice.setEnabled(editable);
                break;
            default:
                textField.setEnabled(editable);
                break;
        }
    }

    public boolean getEditable() {
        switch (option.getType()) {
            case IOption.BOOLEAN:
                return checkbox.isEnabled();
            case IOption.CHOICE:
                return choice.isEnabled();
            default:
                return textField.isEnabled();
        }
    }

    public void setSelected(boolean state) {
        checkbox.setSelected(state);
    }

    public void setSelected(String value) {
        choice.setSelectedItem(value);
    }

    public void addValue(String value) {
        choice.addItem(value);
    }

    public boolean isDefaultValue() {
        switch (option.getType()) {
            case IOption.BOOLEAN:
                return checkbox.isSelected() == (boolean) option.getDefault();
            case IOption.CHOICE:
                // Assume first choice is always default
                return choice.getSelectedIndex() == 0;
            default:
                return textField.getText().equals(String.valueOf(option.getDefault()));
        }        
    }
    
    public void resetToDefault() {
        switch (option.getType()) {
            case IOption.BOOLEAN:
                checkbox.setSelected((boolean) option.getDefault());
                break;
            case IOption.CHOICE:
                choice.setSelectedIndex(0); // Assume first choice is always default
                break;
            default:
                textField.setText(String.valueOf(option.getDefault()));
                break;
        }
    }

    /** Returns a new option, representing the option in it's changed state. */
    public IBasicOption changedOption() {
        return new BasicOption(option.getName(), getValue());
    }

    @Override
    public void itemStateChanged(ItemEvent itemEvent) {
        dialogOptionListener.optionClicked(this, option, checkbox.isSelected());
    }

    @Override
    public int compareTo(DialogOptionComponent doc) {
        return option.getDisplayableName().compareTo(doc.option.getDisplayableName());
    }

    @Override
    public String toString() {
        return option.getDisplayableName();
    }
}

/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

/**
 * DialogOptionComponent.java
 *
 * @author Cord Awtry
 */

package megamek.client.ui.swing;

import java.awt.BorderLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.Serializable;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import megamek.common.options.IBasicOption;
import megamek.common.options.IOption;

public class DialogOptionComponent extends JPanel implements MouseListener,
        ItemListener {
    /**
     * 
     */
    private static final long serialVersionUID = -4190538980884459746L;

    IOption option;

    private JCheckBox checkbox;
    private JComboBox choice;
    private JTextField textField;
    private JLabel label;
    private DialogOptionListener dialogOptionListener;

    public DialogOptionComponent(DialogOptionListener parent, IOption option) {
        this(parent, option, true);
    }

    public DialogOptionComponent(DialogOptionListener parent, IOption option,
            boolean editable) {
        dialogOptionListener = parent;
        this.option = option;

        addMouseListener(this);

        setLayout(new BorderLayout());
        switch (option.getType()) {
            case IOption.BOOLEAN:
                checkbox = new JCheckBox(option.getDisplayableName(), option
                        .booleanValue());
                checkbox.addMouseListener(this);
                checkbox.addItemListener(this);
                add(checkbox, BorderLayout.CENTER);

                if (!editable)
                    checkbox.setEnabled(false);

                break;
            case IOption.CHOICE:
                choice = new JComboBox();

                choice.addMouseListener(this);
                label = new JLabel(option.getDisplayableName());
                label.addMouseListener(this);
                add(label, BorderLayout.WEST);
                add(choice, BorderLayout.CENTER);

                if (!editable)
                    choice.setEnabled(false);

                break;
            default:
                textField = new JTextField(option.stringValue(), option
                        .getTextFieldLength());
                textField.addMouseListener(this);
                label = new JLabel(option.getDisplayableName());
                label.addMouseListener(this);

                if (option.isLabelBeforeTextField()) {
                    add(label, BorderLayout.CENTER);
                    add(textField, BorderLayout.WEST);
                } else {
                    add(textField, BorderLayout.WEST);
                    add(label, BorderLayout.CENTER);
                }

                if (!editable)
                    textField.setEnabled(false);

                break;
        }

    }

    public boolean hasChanged() {
        return !option.getValue().equals(getValue());
    }

    public Object getValue() {
        String text = "";
        switch (option.getType()) {
            case IOption.BOOLEAN:
                return Boolean.valueOf(checkbox.isSelected());
            case IOption.INTEGER:
                text = textField.getText();
                if (text.trim().equals("")) {
                    text = "0";
                }
                return Integer.valueOf(textField.getText());
            case IOption.FLOAT:
                text = textField.getText();
                if (text.trim().equals("")) {
                    text = "0";
                }
                return Float.valueOf(textField.getText());
            case IOption.STRING:
                return textField.getText();
            case IOption.CHOICE:
                return choice.getSelectedItem();
            default:
                return null;
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

    public void setSelected(boolean state) {
        checkbox.setSelected(state);
    }

    public void setSelected(String value) {
        choice.setSelectedItem(value);
    }

    public void addValue(String value) {
        choice.addItem(value);
    }

    public void resetToDefault() {
        switch (option.getType()) {
            case IOption.BOOLEAN:
                checkbox.setSelected(((Boolean) option.getDefault())
                        .booleanValue());
                break;
            case IOption.CHOICE:
                choice.setSelectedIndex(0); // Assume first choice is always
                                            // default
                break;
            default:
                textField.setText(String.valueOf(option.getDefault()));
                break;
        }
    }

    /**
     * Returns a new option, representing the option in it's changed state.
     */
    public IBasicOption changedOption() {
        return new BasicOption(option.getName(), getValue());
    }

    public void mousePressed(MouseEvent mouseEvent) {
    }

    public void mouseEntered(MouseEvent mouseEvent) {
        dialogOptionListener.showDescFor(option);
    }

    public void mouseReleased(MouseEvent mouseEvent) {
    }

    public void mouseClicked(MouseEvent mouseEvent) {
    }

    public void mouseExited(MouseEvent mouseEvent) {
    }

    public void itemStateChanged(ItemEvent itemEvent) {
        dialogOptionListener.optionClicked(this, option, checkbox.isSelected());
    }

    private static class BasicOption implements IBasicOption, Serializable {
        /**
         * 
         */
        private static final long serialVersionUID = -1888895390718831758L;
        private String name;
        private Object value;

        BasicOption(String name, Object value) {
            this.name = name;
            this.value = value;
        }

        /*
         * (non-Javadoc)
         * 
         * @see megamek.common.options.IBasicOption#getName()
         */
        public String getName() {
            return name;
        }

        /*
         * (non-Javadoc)
         * 
         * @see megamek.common.options.IBasicOption#getValue()
         */
        public Object getValue() {
            return value;
        }
    }

}

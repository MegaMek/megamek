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

package megamek.client.ui.AWT;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ItemListener;
import java.awt.event.MouseListener;
import java.io.Serializable;

import megamek.common.options.IBasicOption;
import megamek.common.options.IOption;

public class DialogOptionComponent extends Panel implements MouseListener,
        ItemListener {
    /**
     * 
     */
    private static final long serialVersionUID = 4062277493444205351L;

    IOption option;

    private Checkbox checkbox;
    private Choice choice;
    private TextField textField;
    private Label label;
    private DialogOptionListener parent;

    public DialogOptionComponent(DialogOptionListener parent, IOption option) {
        this(parent, option, true);
    }

    public DialogOptionComponent(DialogOptionListener parent, IOption option,
            boolean editable) {
        this.parent = parent;
        this.option = option;

        addMouseListener(this);

        setLayout(new BorderLayout());
        switch (option.getType()) {
            case IOption.BOOLEAN:
                checkbox = new Checkbox(option.getDisplayableName(), option
                        .booleanValue());
                checkbox.addMouseListener(this);
                checkbox.addItemListener(this);
                add(checkbox, BorderLayout.CENTER);

                if (!editable)
                    checkbox.setEnabled(false);

                break;
            case IOption.CHOICE:
                choice = new Choice();

                choice.addMouseListener(this);
                label = new Label(option.getDisplayableName());
                label.addMouseListener(this);
                add(label, BorderLayout.WEST);
                add(choice, BorderLayout.CENTER);

                if (!editable)
                    choice.setEnabled(false);

                break;
            default:
                textField = new TextField(option.stringValue(), option
                        .getTextFieldLength());
                textField.addMouseListener(this);
                label = new Label(option.getDisplayableName());
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
                return new Boolean(checkbox.getState());
            case IOption.INTEGER:
                text = textField.getText();
                if (text.trim().equals("")) {
                    text = "0";
                }
                return Integer.valueOf(text);
            case IOption.FLOAT:
                text = textField.getText();
                if (text.trim().equals("")) {
                    text = "0";
                }
                return Float.valueOf(text);
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

    public void setState(boolean state) {
        checkbox.setState(state);
    }

    public void setSelected(String value) {
        choice.select(value);
    }

    public void addValue(String value) {
        choice.add(value);
    }

    public void resetToDefault() {
        switch (option.getType()) {
            case IOption.BOOLEAN:
                checkbox.setState(((Boolean) option.getDefault())
                        .booleanValue());
                break;
            case IOption.CHOICE:
                choice.select(0); // Assume first choice is always default
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

    public void mousePressed(java.awt.event.MouseEvent mouseEvent) {
    }

    public void mouseEntered(java.awt.event.MouseEvent mouseEvent) {
        parent.showDescFor(option);
    }

    public void mouseReleased(java.awt.event.MouseEvent mouseEvent) {
    }

    public void mouseClicked(java.awt.event.MouseEvent mouseEvent) {
    }

    public void mouseExited(java.awt.event.MouseEvent mouseEvent) {
    }

    public void itemStateChanged(java.awt.event.ItemEvent itemEvent) {
        parent.optionClicked(this, option, checkbox.getState());
    }

    private static class BasicOption implements IBasicOption, Serializable {
        /**
         * 
         */
        private static final long serialVersionUID = -8128677074350169153L;
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

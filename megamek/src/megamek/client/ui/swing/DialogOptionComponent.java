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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.StringTokenizer;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import megamek.common.options.BasicOption;
import megamek.common.options.IBasicOption;
import megamek.common.options.IOption;

public class DialogOptionComponent extends JPanel implements
        ItemListener, Comparable<DialogOptionComponent> {
    /**
     *
     */
    private static final long serialVersionUID = -4190538980884459746L;

    IOption option;

    private JCheckBox checkbox;
    private JComboBox<String> choice;
    private JTextField textField;
    private JLabel label;
    private DialogOptionListener dialogOptionListener;
    
    /**
     * Value used to force a change
     */
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

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        switch (option.getType()) {
            case IOption.BOOLEAN:
                checkbox = new JCheckBox("", option.booleanValue());
                checkbox.addItemListener(this);
                checkbox.setToolTipText(convertToHtml(option.getDescription()));
                if (!editable) {
                    checkbox.setEnabled(false);
                }

                label = new JLabel(option.getDisplayableName());
                label.setToolTipText(convertToHtml(option.getDescription()));
                label.addMouseListener(new MouseListener(){
                    public void mouseClicked(MouseEvent evt) {
                        if (checkbox.isEnabled()) {
                            checkbox.setSelected(!checkbox.isSelected());
                        }
                    }
                    public void mouseEntered(MouseEvent evt) {}
                    public void mouseExited(MouseEvent evt) {}
                    public void mousePressed(MouseEvent evt) {}
                    public void mouseReleased(MouseEvent evt) {}                    
                });
                
                gbc.gridx = gbc.gridy = 0;
                gbc.anchor = GridBagConstraints.CENTER;
                gbc.insets = new Insets(0,10,0,10);
                add(checkbox, gbc);
                gbc.gridx++;
                gbc.weightx = 1.0;
                gbc.anchor = GridBagConstraints.WEST;
                gbc.insets = new Insets(0,0,0,0);
                add(label, gbc);
                break;
            case IOption.CHOICE:
                choice = new JComboBox<String>();
                label = new JLabel(option.getDisplayableName());
                label.setToolTipText(convertToHtml(option.getDescription()));
                if (!editable) {
                    choice.setEnabled(false);
                }

                if (choiceLabelFirst) {
                    gbc.gridx = gbc.gridy = 0;
                    add(choice, gbc);
                    gbc.gridx++;
                    gbc.weightx = 1.0;
                    add(label, gbc);
                } else {
                    gbc.gridx = gbc.gridy = 0;
                    add(label, gbc);
                    gbc.gridx++;
                    gbc.weightx = 1.0;
                    add(choice, gbc);
                }
                break;
            default:
                textField = new JTextField(option.stringValue(), option
                        .getTextFieldLength());
                textField.setHorizontalAlignment(JTextField.CENTER);
                label = new JLabel(option.getDisplayableName());
                label.setToolTipText(convertToHtml(option.getDescription()));
                label.addMouseListener(new MouseListener(){
                    public void mouseClicked(MouseEvent evt) {
                        if (textField.isEnabled()) {
                            textField.requestFocus();
                            textField.selectAll();
                        }
                    }
                    public void mouseEntered(MouseEvent evt) {}
                    public void mouseExited(MouseEvent evt) {}
                    public void mousePressed(MouseEvent evt) {}
                    public void mouseReleased(MouseEvent evt) {}                    
                });
                
                if (!editable) {
                    textField.setEnabled(false);
                }
                
                if (option.isLabelBeforeTextField()) {
                    gbc.gridx = gbc.gridy = 0;
                    add(label, gbc);
                    gbc.gridx++;
                    gbc.weightx = 1.0;
                    add(textField, gbc);
                } else {
                    gbc.gridx = gbc.gridy = 0;
                    gbc.insets = new Insets(0,2,0,2);
                    add(textField, gbc);
                    gbc.gridx++;
                    gbc.insets = new Insets(0,0,0,0);
                    gbc.weightx = 1.0;
                    add(label, gbc);
                }
                break;
        }
    }

    public static String convertToHtml(String source) {
        StringBuffer sb = new StringBuffer();
        sb.append("<html>");
        StringTokenizer tok =new StringTokenizer(source,"\n");
        while ( tok.hasMoreTokens() ) {
            sb.append(tok.nextToken());
            sb.append("<br>");
        }
        sb.append("</html>");
        return sb.toString();
    }
    
    public boolean hasChanged() {
        return !option.getValue().equals(getValue()) || hasOptionChanged;
    }
    
    public void setOptionChanged(boolean v) {
        hasOptionChanged = v;
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

    public void setValue(Object v) {
        switch (option.getType()) {
            case IOption.BOOLEAN:
                checkbox.setSelected((Boolean)v);
                break;
            case IOption.INTEGER:
            case IOption.FLOAT:
            case IOption.STRING:
                textField.setText((String)v);
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
                return checkbox.isSelected() == ((Boolean) option.getDefault())
                        .booleanValue();
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

    public void itemStateChanged(ItemEvent itemEvent) {
        dialogOptionListener.optionClicked(this, option, checkbox.isSelected());
    }

    @Override
    public int compareTo(DialogOptionComponent doc) {
        return option.getDisplayableName().compareTo(doc.option.getDisplayableName());
    }

    public String toString() {
        return option.getDisplayableName();
    }

}

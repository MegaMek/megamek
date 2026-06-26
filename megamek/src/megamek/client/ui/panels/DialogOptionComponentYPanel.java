/*
 * Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2003-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.client.ui.panels;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.StringTokenizer;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

import megamek.client.ui.clientGUI.DialogOptionListener;
import megamek.client.ui.util.UIUtil;
import megamek.client.ui.util.UIUtil.FixedYPanel;
import megamek.codeUtilities.MathUtility;
import megamek.common.options.BasicOption;
import megamek.common.options.IBasicOption;
import megamek.common.options.IOption;

/** @author Cord Awtry */
public class DialogOptionComponentYPanel extends FixedYPanel
      implements ItemListener, FocusListener, ActionListener, Comparable<DialogOptionComponentYPanel> {

    @Serial
    private static final long serialVersionUID = -4190538980884459746L;

    IOption option;

    private JCheckBox checkbox;
    private JComboBox<String> choice;
    private JTextField textField;
    private final DialogOptionListener dialogOptionListener;

    /** Value used to force a change */
    private boolean hasOptionChanged = false;

    public DialogOptionComponentYPanel(DialogOptionListener parent, IOption option, boolean editable) {
        this(parent, option, editable, false);
    }

    public DialogOptionComponentYPanel(DialogOptionListener parent, IOption option, boolean editable,
          boolean choiceLabelFirst) {
        dialogOptionListener = parent;
        this.option = option;
        JLabel label;

        setLayout(new FlowLayout(FlowLayout.LEFT, 5, 2));
        switch (option.getType()) {
            case IOption.BOOLEAN:
                checkbox = new JCheckBox("", option.booleanValue());
                checkbox.addItemListener(this);
                checkbox.setToolTipText(convertToHtml(option.getDescription()));
                checkbox.setEnabled(editable);
                label = new JLabel(option.getDisplayableName());
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
                add(Box.createHorizontalStrut(UIUtil.scaleForGUI(10)));
                add(checkbox);
                add(label);
                break;
            case IOption.CHOICE:
                choice = new JComboBox<>();
                label = new JLabel(option.getDisplayableName());
                label.setLabelFor(choice);
                label.setToolTipText(convertToHtml(option.getDescription()));
                choice.setEnabled(editable);
                choice.addActionListener(this);
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
                textField.addFocusListener(this);
                textField.setEnabled(editable);
                if (option.isLabelBeforeTextField()) {
                    add(Box.createHorizontalStrut(UIUtil.scaleForGUI(10)));
                    add(label);
                    add(textField);
                } else {
                    add(Box.createHorizontalStrut(UIUtil.scaleForGUI(2)));
                    add(textField);
                    add(label);
                }
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
        return switch (option.getType()) {
            case IOption.BOOLEAN -> checkbox.isSelected();
            case IOption.INTEGER -> textField.getText().isBlank() ? 0 : MathUtility.parseInt(textField.getText(), 0);
            case IOption.FLOAT -> textField.getText().isBlank() ? 0 : MathUtility.parseFloat(textField.getText(), 0);
            case IOption.STRING -> textField.getText();
            case IOption.CHOICE -> choice.getSelectedItem();
            default -> null;
        };
    }

    public void setValue(Object v) {
        switch (option.getType()) {
            case IOption.BOOLEAN:
                checkbox.setSelected((Boolean) v);
                break;
            case IOption.INTEGER:
            case IOption.FLOAT:
                textField.setText(v + "");
                break;
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
     * @param editable - <code>true</code> if the contents of the component are editable, <code>false</code> if they are
     *                 view-only.
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
        return switch (option.getType()) {
            case IOption.BOOLEAN -> checkbox.isEnabled();
            case IOption.CHOICE -> choice.isEnabled();
            default -> textField.isEnabled();
        };
    }

    public void setSelected(boolean state) {
        checkbox.setSelected(state);
    }

    public void setSelected(String value) {
        choice.setSelectedItem(value);
    }

    public void addValue(String value) {
        //turn off listener when adding the item
        choice.removeActionListener(this);
        choice.addItem(value);
        choice.addActionListener(this);
    }

    public boolean isDefaultValue() {
        return switch (option.getType()) {
            case IOption.BOOLEAN -> checkbox.isSelected() == (boolean) option.getDefault();
            case IOption.CHOICE ->
                // Assume first choice is always default
                  choice.getSelectedIndex() == 0;
            default -> textField.getText().equals(String.valueOf(option.getDefault()));
        };
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
    public void actionPerformed(ActionEvent actionEvent) {
        dialogOptionListener.optionSwitched(this, option, choice.getSelectedIndex());
    }

    @Override
    public int compareTo(DialogOptionComponentYPanel doc) {
        return option.getDisplayableName().compareTo(doc.option.getDisplayableName());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DialogOptionComponentYPanel dialogOptionComponentYPanel) {
            return dialogOptionComponentYPanel.option.getDisplayableName().equals(option.getDisplayableName());
        }

        return false;
    }

    @Override
    public int hashCode() {
        return option.getDisplayableName().hashCode();
    }

    @Override
    public String toString() {
        return option.getDisplayableName();
    }

    @Override
    public void focusGained(FocusEvent e) {
    }

    @Override
    public void focusLost(FocusEvent e) {
        dialogOptionListener.optionClicked(this, option, true);
    }
}

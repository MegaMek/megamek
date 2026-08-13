/*
 * Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2003-2026 The MegaMek Team. All Rights Reserved.
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

import java.awt.Component;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import org.apache.commons.text.StringEscapeUtils;

import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.DialogOptionListener;
import megamek.client.ui.settings.SettingsBadge;
import megamek.client.ui.settings.SettingsHelpProvider;
import megamek.client.ui.settings.SettingsSpinner;
import megamek.client.ui.util.UIUtil;
import megamek.client.ui.util.UIUtil.FixedYPanel;
import megamek.codeUtilities.MathUtility;
import megamek.common.annotations.Nullable;
import megamek.common.options.BasicOption;
import megamek.common.options.IBasicOption;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;

/** @author Cord Awtry */
public class DialogOptionComponentYPanel extends FixedYPanel
      implements ItemListener, FocusListener, ActionListener, Comparable<DialogOptionComponentYPanel>,
      SettingsHelpProvider {

    @Serial
    private static final long serialVersionUID = -4190538980884459746L;
    IOption option;

    private JCheckBox checkbox;
    private JComboBox<String> choice;
    private JComboBox<String> integerChoice;
    private List<Integer> integerChoiceValues = List.of();
    private JSpinner integerSpinner;
    private JTextField textField;
    /** The label showing the option's displayable name and any presentation-specific markers. */
    private JLabel optionLabel;
    private String optionDisplayName;
    private String settingsBadgeHtml = "";
    private String settingsHelpText;
    private int optionLabelWrapWidth;
    private boolean settingsCheckBoxPresentation;
    private boolean settingsComponentsDetached;
    private boolean settingsCheckBoxInitialized;
    private boolean settingsLabelInitialized;
    private boolean settingsControlInitialized;
    private boolean requestedEditable;
    private boolean dependencyEditable = true;
    /** Short marker appended to the displayable name (e.g. " (P)" for partially implemented SPAs). */
    private String nameSuffix = "";
    private final DialogOptionListener dialogOptionListener;

    /** Value used to force a change */
    private boolean hasOptionChanged = false;

    /** True when this component renders the Directional Torso Mount torso multi-select (BMM p.83). */
    private boolean torsoMultiSelect = false;
    private JPanel torsoControlPanel;
    /** Torso location abbreviation -&gt; its checkbox, for the Directional Torso Mount multi-select. */
    private final Map<String, JCheckBox> torsoCheckboxes = new LinkedHashMap<>();
    /**
     * The torso location codes offered by the Directional Torso Mount multi-select. These are the serialized tokens of
     * the quirk's location string (e.g. {@code "LT RT"}), which {@code Mounted} parses back - not display text. The
     * user-facing location names come from the {@code DialogOptionComponentYPanel.torsoMount.*} message keys.
     */
    private static final String[] TORSO_MOUNT_LOCATION_CODES = { "H", "LT", "RT", "CT" };

    public DialogOptionComponentYPanel(DialogOptionListener parent, IOption option, boolean editable) {
        this(parent, option, editable, false);
    }

    public DialogOptionComponentYPanel(DialogOptionListener parent, IOption option, boolean editable,
          boolean choiceLabelFirst) {
        dialogOptionListener = parent;
        this.option = option;
        requestedEditable = editable;
        optionDisplayName = option.getDisplayableName();

        setLayout(new FlowLayout(FlowLayout.LEFT, 5, 2));
        if (isTorsoMountQuirk(option)) {
            buildTorsoMultiSelect(editable);
            return;
        }
        switch (option.getType()) {
            case IOption.BOOLEAN:
                checkbox = new JCheckBox("", option.booleanValue());
                checkbox.addItemListener(this);
                checkbox.setToolTipText(convertToHtml(option.getDescription()));
                checkbox.setEnabled(editable);
                optionLabel = new JLabel(option.getDisplayableName());
                optionLabel.setLabelFor(checkbox);
                optionLabel.setToolTipText(convertToHtml(option.getDescription()));
                optionLabel.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent evt) {
                        if (checkbox.isEnabled()) {
                            checkbox.setSelected(!checkbox.isSelected());
                        }
                    }
                });
                add(Box.createHorizontalStrut(UIUtil.scaleForGUI(10)));
                add(checkbox);
                add(optionLabel);
                break;
            case IOption.CHOICE:
                choice = new JComboBox<>();
                optionLabel = new JLabel(option.getDisplayableName());
                optionLabel.setLabelFor(choice);
                optionLabel.setToolTipText(convertToHtml(option.getDescription()));
                choice.setToolTipText(convertToHtml(option.getDescription()));
                choice.setEnabled(editable);
                choice.addActionListener(this);
                if (choiceLabelFirst) {
                    add(choice);
                    add(optionLabel);
                } else {
                    add(optionLabel);
                    add(choice);
                }

                break;
            default:
                textField = new JTextField(option.stringValue(), option.getTextFieldLength());
                textField.setHorizontalAlignment(JTextField.CENTER);
                optionLabel = new JLabel(option.getDisplayableName());
                optionLabel.setToolTipText(convertToHtml(option.getDescription()));
                optionLabel.setLabelFor(textField);
                textField.setToolTipText(convertToHtml(option.getDescription()));
                optionLabel.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent evt) {
                        JComponent control = valueControl();
                        if (control.isEnabled()) {
                            control.requestFocus();
                            if (control instanceof JTextField field) {
                                field.selectAll();
                            }
                        }
                    }
                });
                textField.addFocusListener(this);
                textField.setEnabled(editable);
                if (option.isLabelBeforeTextField()) {
                    add(Box.createHorizontalStrut(UIUtil.scaleForGUI(10)));
                    add(optionLabel);
                    add(textField);
                } else {
                    add(Box.createHorizontalStrut(UIUtil.scaleForGUI(2)));
                    add(textField);
                    add(optionLabel);
                }
                break;
        }
    }

    /**
     * Applies page-specific display text and metadata markers to this option's visible label.
     *
     * @param displayName concise text to show in this presentation
     * @param badges      markers that apply specifically to this option
     */
    void setSettingsPresentation(String displayName, Collection<SettingsBadge> badges) {
        optionDisplayName = displayName;
        settingsBadgeHtml = SettingsBadge.formatHtml(badges);
        if (!settingsBadgeHtml.isBlank()) {
            settingsBadgeHtml += "&nbsp;";
        }
        updateOptionLabelText();
    }

    void setSettingsHelpText(String helpText) {
        settingsHelpText = helpText;
    }

    void setEditableWhenSelected(DialogOptionComponentYPanel controllingComponent) {
        if (controllingComponent.option.getType() != IOption.BOOLEAN) {
            throw new IllegalArgumentException("Editability dependencies require a boolean option");
        }
        dependencyEditable = controllingComponent.checkbox.isSelected();
        controllingComponent.checkbox.addItemListener(event -> {
            dependencyEditable = controllingComponent.checkbox.isSelected();
            applyEditable();
        });
        applyEditable();
    }

    void setEditableWhenAnySelected(DialogOptionComponentYPanel... controllingComponents) {
        if (controllingComponents.length == 0) {
            throw new IllegalArgumentException("Editability dependencies require at least one controlling option");
        }
        for (DialogOptionComponentYPanel controllingComponent : controllingComponents) {
            if (controllingComponent.option.getType() != IOption.BOOLEAN) {
                throw new IllegalArgumentException("Editability dependencies require boolean options");
            }
            controllingComponent.checkbox.addItemListener(event -> {
                dependencyEditable = anySelected(controllingComponents);
                applyEditable();
            });
        }
        dependencyEditable = anySelected(controllingComponents);
        applyEditable();
    }

    private static boolean anySelected(DialogOptionComponentYPanel... components) {
        for (DialogOptionComponentYPanel component : components) {
            if (component.checkbox.isSelected()) {
                return true;
            }
        }
        return false;
    }

    JCheckBox settingsCheckBox() {
        if (checkbox == null) {
            throw new IllegalStateException("Settings checkboxes require a boolean option");
        }
        settingsCheckBoxPresentation = true;
        settingsComponentsDetached = true;
        checkbox.setText(optionLabel.getText());
        if (!settingsCheckBoxInitialized) {
            checkbox.setToolTipText(getSettingsHelpText());
            settingsCheckBoxInitialized = true;
        }
        return checkbox;
    }

    JLabel settingsLabel() {
        if (checkbox != null) {
            throw new IllegalStateException("Boolean settings use their checkbox text as the label");
        }
        settingsComponentsDetached = true;
        if (!settingsLabelInitialized) {
            optionLabel.setToolTipText(getSettingsHelpText());
            settingsLabelInitialized = true;
        }
        return optionLabel;
    }

    JComponent settingsControl() {
        JComponent control = valueControl();
        if (control == null) {
            throw new IllegalStateException("Boolean settings do not have a separate control");
        }
        settingsComponentsDetached = true;
        if (!settingsControlInitialized) {
            control.setToolTipText(getSettingsHelpText());
            settingsControlInitialized = true;
        }
        return control;
    }

    /** Replaces this integer option's text field with a spinner constrained to the given minimum. */
    void useIntegerSpinner(int minimum) {
        configureIntegerSpinner(minimum, null);
    }

    /** Replaces this integer option's text field with a spinner constrained to the given range. */
    void useIntegerSpinner(int minimum, int maximum) {
        if (maximum < minimum) {
            throw new IllegalArgumentException("Integer spinner maximum cannot be below its minimum");
        }
        configureIntegerSpinner(minimum, maximum);
    }

    private void configureIntegerSpinner(int minimum, @Nullable Integer maximum) {
        if (option.getType() != IOption.INTEGER) {
            throw new IllegalStateException("Integer spinners require an integer option");
        }
        if (integerSpinner != null) {
            return;
        }
        int componentIndex = getComponentZOrder(textField);
        boolean editable = textField.isEnabled();
        remove(textField);
        int currentValue = option.intValue();
        int effectiveMinimum = Math.min(minimum, currentValue);
        Integer effectiveMaximum = maximum == null ? null : Math.max(maximum, currentValue);
        integerSpinner = new JSpinner(new SpinnerNumberModel(
              (Number) currentValue, effectiveMinimum, effectiveMaximum, 1));
        SettingsSpinner.configureEditor(integerSpinner);
        integerSpinner.setEnabled(editable);
        integerSpinner.setToolTipText(convertToHtml(option.getDescription()));
        integerSpinner.addChangeListener(event -> dialogOptionListener.optionClicked(this, option, true));
        optionLabel.setLabelFor(integerSpinner);
        add(integerSpinner, componentIndex);
    }

    /** Replaces this integer option's text field with a dropdown backed by explicit integer values. */
    void useIntegerChoice(Map<Integer, String> choices) {
        if (option.getType() != IOption.INTEGER) {
            throw new IllegalStateException("Integer dropdowns require an integer option");
        }
        if (integerChoice != null) {
            return;
        }
        int componentIndex = getComponentZOrder(textField);
        boolean editable = textField.isEnabled();
        remove(textField);
        List<Integer> values = new ArrayList<>(choices.keySet());
        integerChoice = new JComboBox<>(choices.values().toArray(String[]::new));
        int selectedIndex = values.indexOf(option.intValue());
        if (selectedIndex < 0) {
            values.add(option.intValue());
            integerChoice.addItem(Integer.toString(option.intValue()));
            selectedIndex = values.size() - 1;
        }
        integerChoiceValues = List.copyOf(values);
        integerChoice.setSelectedIndex(selectedIndex);
        integerChoice.setEnabled(editable);
        integerChoice.setToolTipText(convertToHtml(option.getDescription()));
        integerChoice.addActionListener(event -> dialogOptionListener.optionClicked(this, option, true));
        optionLabel.setLabelFor(integerChoice);
        add(integerChoice, componentIndex);
    }

    private JComponent valueControl() {
        if (torsoMultiSelect) {
            return torsoControlPanel;
        }
        if (option.getType() == IOption.CHOICE) {
            return choice;
        }
        if (integerChoice != null) {
            return integerChoice;
        }
        return integerSpinner == null ? textField : integerSpinner;
    }

    private void updateOptionLabelText() {
        String plainName = optionDisplayName + nameSuffix;
        String displayName = StringEscapeUtils.escapeHtml4(plainName);
        if (optionLabelWrapWidth > 0) {
            optionLabel.setText("<html><div width=" + optionLabelWrapWidth + '>'
                  + displayName + settingsBadgeHtml + "</div></html>");
        } else if (!settingsBadgeHtml.isBlank()) {
            optionLabel.setText("<html><nobr>" + displayName + settingsBadgeHtml + "</nobr></html>");
        } else {
            optionLabel.setText(plainName);
        }
        if (settingsCheckBoxPresentation) {
            checkbox.setText(optionLabel.getText());
        }
    }

    @Override
    public String getSettingsHelpText() {
        return settingsHelpText == null ? option.getDescription() : settingsHelpText;
    }

    /**
     * @param option the quirk option being rendered
     *
     * @return {@code true} if this option is a Directional Torso Mount chassis quirk (BMM p.83), which is rendered as a
     *       multi-select of torso locations rather than the default text field
     */
    private static boolean isTorsoMountQuirk(IOption option) {
        String optionName = option.getName();
        return optionName.equals(OptionsConstants.QUIRK_POS_DIRECTIONAL_TORSO_MOUNT)
              || optionName.equals(OptionsConstants.QUIRK_POS_DIRECTIONAL_TORSO_MOUNT_360);
    }

    /**
     * Builds the Directional Torso Mount torso multi-select: the quirk's display name followed by a checkbox per torso
     * location (head, left/right/center torso). The checkboxes are initialized from, and serialized back to, the
     * quirk's space-separated location string (e.g. {@code "LT RT"}).
     *
     * @param editable whether the checkboxes may be changed
     */
    private void buildTorsoMultiSelect(boolean editable) {
        torsoMultiSelect = true;
        optionLabel = new JLabel(option.getDisplayableName());
        optionLabel.setToolTipText(convertToHtml(option.getDescription()));
        torsoControlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, UIUtil.scaleForGUI(5), 0));
        torsoControlPanel.setOpaque(false);
        torsoControlPanel.setToolTipText(convertToHtml(option.getDescription()));
        add(Box.createHorizontalStrut(UIUtil.scaleForGUI(10)));
        add(optionLabel);
        Set<String> selected = parseTorsoValue(option.stringValue());
        for (String locationCode : TORSO_MOUNT_LOCATION_CODES) {
            JCheckBox box = new JCheckBox(locationCode, selected.contains(locationCode));
            box.setToolTipText(Messages.getString("DialogOptionComponentYPanel.torsoMount." + locationCode));
            box.setEnabled(editable);
            box.addItemListener(this);
            torsoCheckboxes.put(locationCode, box);
            torsoControlPanel.add(box);
        }
        add(torsoControlPanel);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (!settingsComponentsDetached) {
            return;
        }
        if (settingsCheckBoxPresentation && checkbox != null) {
            checkbox.setVisible(visible);
            return;
        }
        if (optionLabel != null) {
            optionLabel.setVisible(visible);
        }
        JComponent control = valueControl();
        if (control != null) {
            control.setVisible(visible);
        }
    }

    /**
     * @param value a Directional Torso Mount quirk value (space- or comma-separated location abbreviations), or
     *              {@code null}
     *
     * @return the set of upper-case location abbreviations present in the value
     */
    private static Set<String> parseTorsoValue(String value) {
        Set<String> selected = new HashSet<>();
        if (value != null) {
            for (String token : value.split("[ ,]+")) {
                if (!token.isBlank()) {
                    selected.add(token.trim().toUpperCase());
                }
            }
        }
        return selected;
    }

    /**
     * @return the current torso multi-select as a space-separated location string (e.g. {@code "LT RT"})
     */
    private String torsoSelectionValue() {
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, JCheckBox> entry : torsoCheckboxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                if (!result.isEmpty()) {
                    result.append(' ');
                }
                result.append(entry.getKey());
            }
        }
        return result.toString();
    }

    /**
     * Appends a short marker to the option's displayable name, kept through {@link #setNameLabelWrapWidth(int)}
     * re-wraps (e.g. " (P)" for partially implemented SPAs).
     *
     * @param suffix the marker text, or {@code null} to clear it
     */
    public void setNameSuffix(@Nullable String suffix) {
        nameSuffix = (suffix == null) ? "" : suffix;
        if (optionLabel != null) {
            updateOptionLabelText();
        }
    }

    /**
     * Makes the option's name label wrap when the whole row is wider than the given width, by switching the label
     * to width-constrained HTML; a name that fits reverts to plain text. The label's share is the row width minus
     * this row's other visible components (checkbox, combo box, inline controls), so a row with a wide control
     * (e.g. the Sandblaster weapon choice) narrows its label instead of pushing the control onto a clipped second
     * line. Multi-column containers call this when they lay out.
     *
     * @param availableRowWidth the width in pixels the whole row may use, or 0 or less to always use plain text
     */
    public void setNameLabelWrapWidth(int availableRowWidth) {
        if (optionLabel == null) {
            return;
        }
        String plainName = optionDisplayName + nameSuffix;
        int availableLabelWidth = availableRowWidth;
        if (availableRowWidth > 0) {
            FlowLayout flowLayout = (FlowLayout) getLayout();
            availableLabelWidth -= flowLayout.getHgap();
            for (Component child : getComponents()) {
                if ((child != optionLabel) && child.isVisible()) {
                    availableLabelWidth -= child.getPreferredSize().width + flowLayout.getHgap();
                }
            }
        }
        // When the controls alone exceed the row (availableLabelWidth <= 0) wrapping cannot help; keep the plain
        // text and let the row clip at its right edge, which stays usable.
        boolean fits = (availableRowWidth <= 0) || (availableLabelWidth <= 0)
              || (optionLabel.getFontMetrics(optionLabel.getFont()).stringWidth(plainName) <= availableLabelWidth);
        optionLabelWrapWidth = fits ? 0 : availableLabelWidth;
        updateOptionLabelText();
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
        if (torsoMultiSelect) {
            return torsoSelectionValue();
        }
        return switch (option.getType()) {
            case IOption.BOOLEAN -> checkbox.isSelected();
            case IOption.INTEGER -> integerValue();
            case IOption.FLOAT -> textField.getText().isBlank() ? 0 : MathUtility.parseFloat(textField.getText(), 0);
            case IOption.STRING -> textField.getText();
            case IOption.CHOICE -> choice.getSelectedItem();
            default -> null;
        };
    }

    private int integerValue() {
        if (integerChoice != null) {
            return integerChoiceValues.get(integerChoice.getSelectedIndex());
        }
        if (integerSpinner != null) {
            return ((Number) integerSpinner.getValue()).intValue();
        }
        return textField.getText().isBlank() ? 0 : MathUtility.parseInt(textField.getText(), 0);
    }

    public void setValue(Object v) {
        if (torsoMultiSelect) {
            Set<String> selected = parseTorsoValue((String) v);
            torsoCheckboxes.forEach((abbreviation, box) -> box.setSelected(selected.contains(abbreviation)));
            return;
        }
        switch (option.getType()) {
            case IOption.BOOLEAN:
                checkbox.setSelected((Boolean) v);
                break;
            case IOption.INTEGER:
                if (integerChoice != null) {
                    int selectedIndex = integerChoiceValues.indexOf(((Number) v).intValue());
                    if (selectedIndex >= 0) {
                        integerChoice.setSelectedIndex(selectedIndex);
                    }
                    break;
                }
                if (integerSpinner != null) {
                    SpinnerNumberModel model = (SpinnerNumberModel) integerSpinner.getModel();
                    int value = ((Number) v).intValue();
                    if (value < ((Number) model.getMinimum()).intValue()) {
                        model.setMinimum(value);
                    }
                    if (model.getMaximum() != null && value > ((Number) model.getMaximum()).intValue()) {
                        model.setMaximum(value);
                    }
                    integerSpinner.setValue(value);
                    break;
                }
                textField.setText(v + "");
                break;
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
        requestedEditable = editable;
        applyEditable();
    }

    private void applyEditable() {
        boolean editable = requestedEditable && dependencyEditable;
        if (torsoMultiSelect) {
            torsoCheckboxes.values().forEach(box -> box.setEnabled(editable));
            return;
        }
        // Update the correct control.
        switch (option.getType()) {
            case IOption.BOOLEAN:
                checkbox.setEnabled(editable);
                optionLabel.setEnabled(editable);
                break;
            case IOption.CHOICE:
                choice.setEnabled(editable);
                optionLabel.setEnabled(editable);
                break;
            case IOption.INTEGER:
                valueControl().setEnabled(editable);
                break;
            default:
                textField.setEnabled(editable);
                break;
        }
    }

    public boolean getEditable() {
        if (torsoMultiSelect) {
            return !torsoCheckboxes.isEmpty() && torsoCheckboxes.values().iterator().next().isEnabled();
        }
        return switch (option.getType()) {
            case IOption.BOOLEAN -> checkbox.isEnabled();
            case IOption.CHOICE -> choice.isEnabled();
            case IOption.INTEGER -> valueControl().isEnabled();
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
        if (torsoMultiSelect) {
            return torsoSelectionValue().equals(String.valueOf(option.getDefault()));
        }
        return switch (option.getType()) {
            case IOption.BOOLEAN -> checkbox.isSelected() == (boolean) option.getDefault();
            case IOption.CHOICE ->
                // Assume first choice is always default
                choice.getSelectedIndex() == 0;
            case IOption.INTEGER -> getValue().equals(option.getDefault());
            default -> textField.getText().equals(String.valueOf(option.getDefault()));
        };
    }

    public void resetToDefault() {
        if (torsoMultiSelect) {
            torsoCheckboxes.values().forEach(box -> box.setSelected(false));
            return;
        }
        switch (option.getType()) {
            case IOption.BOOLEAN:
                checkbox.setSelected((boolean) option.getDefault());
                break;
            case IOption.CHOICE:
                choice.setSelectedIndex(0); // Assume first choice is always default
                break;
            case IOption.INTEGER:
                setValue(option.getDefault());
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
        if (torsoMultiSelect) {
            // Non-boolean option: the listener re-reads the value via getValue(); the flag is unused.
            dialogOptionListener.optionClicked(this, option, true);
            return;
        }
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

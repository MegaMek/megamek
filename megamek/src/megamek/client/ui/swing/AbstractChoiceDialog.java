/*
 * MegaMek - Copyright (C) 2023 - The MegaMek Team
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package megamek.client.ui.swing;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import megamek.client.ui.baseComponents.AbstractButtonDialog;
import megamek.client.ui.enums.DialogResult;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.annotations.Nullable;

/**
 * A modal dialog for presenting options as buttons. Can do single or
 * multi-selection. Sub-classes should define methods for setting the text
 * labels for the buttons as a summary or details of the item. Text label can be
 * HTML or any other JButton legal content.
 *
 * @param <T> Any object type
 */
public abstract class AbstractChoiceDialog<T> extends AbstractButtonDialog {

    private static final int BASE_BUTTON_GAP = 5;

    private final String message;
    private final transient List<T> targets;
    private final List<T> chosen = new ArrayList<>();
    private final boolean isMultiSelect;
    private JToggleButton detailsCheckBox;
    private boolean showDetails = false;
    private JToggleButton[] buttons;
    private int columns = 2;

    /**
     * This creates a modal AbstractChoiceDialog using the default resource bundle
     * as a Modal dialog. concrete classes must call initialize() at the end of the
     * constructor
     *
     * @param frame         parent @JFrame that owns this dialog
     * @param title         Resource key string only, plain text will result in NPE
     *                      from Resources
     * @param message       HTML or plain text message show at top of dialog
     * @param targets       things to chose from
     * @param isMultiSelect if true, allows user to select multiple items. if false
     *                      first, item chosen will close the window
     */
    protected AbstractChoiceDialog(JFrame frame, String title, String message,
            @Nullable List<T> targets, boolean isMultiSelect) {
        super(frame, true, title, title);
        this.message = message;
        this.targets = targets;
        this.isMultiSelect = isMultiSelect;
        // in concrete class, initialize() must be called after all member variables set
    }

    /**
     * @param useDetailed Enables or disables the useDetailed flag.
     */
    public void setUseDetailed(boolean useDetailed) {
        if (!useDetailed) {
            showDetails = false;
        }

        detailsCheckBox.setVisible(useDetailed);
    }

    /**
     * @param columns Number of columns to use. Min: 1
     */
    public void setColumns(int columns) {
        if (columns < 1) {
            throw new IllegalArgumentException("Cannot use less than one column.");
        } else {
            this.columns = columns;
        }
    }

    @Override
    protected Container createCenterPane() {
        JPanel result = new JPanel();
        result.setLayout(new BorderLayout());

        int buttonGap = UIUtil.scaleForGUI(BASE_BUTTON_GAP);
        int padding = 5 * buttonGap;

        JPanel ops = new JPanel();
        ops.setLayout(new FlowLayout());
        result.add(ops, BorderLayout.PAGE_START);
        var msgLabel = new JLabel(message, SwingConstants.CENTER);
        msgLabel.setBorder(new EmptyBorder(0, padding, 0, padding));
        ops.add(msgLabel);

        detailsCheckBox = new JToggleButton("Show details", showDetails);
        detailsCheckBox.addActionListener(e -> toggleDetails());
        ops.add(detailsCheckBox);

        JPanel choicesPanel = new JPanel();
        JScrollPane listScroller = new JScrollPane(choicesPanel);
        listScroller.setBorder(null);
        result.add(listScroller, BorderLayout.CENTER);

        // button per-option
        choicesPanel.setLayout(new GridLayout(0, columns, buttonGap, buttonGap));
        choicesPanel.setBorder(new EmptyBorder(buttonGap, padding, padding, padding));
        buttons = new JToggleButton[targets.size()];
        for (int i = 0; i < buttons.length; i++) {
            buttons[i] = new JToggleButton();
            final T target = targets.get(i);
            buttons[i].addActionListener(e -> choose(target));
            choicesPanel.add(buttons[i]);
        }

        updateChoices();
        return result;
    }

    @Override
    public void setVisible(boolean value) {
        super.setVisible(value);
        updateChoices();
    }

    private void updateChoices() {
        if (targets == null) {
            return;
        }

        for (int i = 0; i < buttons.length; i++) {
            T target = targets.get(i);
            buttons[i].setSelected(chosen.contains(target));
            if (showDetails) {
                detailLabel(buttons[i], target);
            } else {
                summaryLabel(buttons[i], target);
            }
        }

        // resize window to nicely contain updated buttons
        pack();
        fit();
    }

    /**
     * Override to set button text and/or icon with details about this choice.
     * Usually this is called then the show details button is depressed
     *
     * @param button A Toggle Button
     * @param target Target to store value?
     */
    protected abstract void detailLabel(JToggleButton button, T target);

    /**
     * Override to set button text and/or icon with summary info about this choice.
     * This is called then the show details button is not depressed
     *
     * @param button A Toggle Button
     * @param target Target to store value?
     */
    protected abstract void summaryLabel(JToggleButton button, T target);

    private void toggleDetails() {
        showDetails = !showDetails;
        updateChoices();
    }

    // called when item chosen by user
    private void choose(@Nullable T choice) {
        if (!chosen.contains(choice)) {
            chosen.add(choice);
        }
        // exit immediate if only one need to be selected
        if (!isMultiSelect) {
            okAction();
            setResult(DialogResult.CONFIRMED);
            setVisible(false);
        }
    }

    /**
     * @return first chosen item, or null if nothing chosen
     */
    public @Nullable T getFirstChoice() {
        return (chosen.isEmpty()) ? null : chosen.get(0);
    }

    /**
     * @return list of items picked by user. List will be empty if nothing picked
     */
    public List<T> getChosen() {
        return chosen;
    }
}

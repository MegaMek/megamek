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

import megamek.client.ui.baseComponents.AbstractButtonDialog;
import megamek.client.ui.baseComponents.AbstractDialog;
import megamek.client.ui.enums.DialogResult;
import megamek.common.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * A modal dialog for presenting options as buttons. Can do single or multi-selection.
 * Sublclasses should define mehtods for setting the text labels for the buttons as a summary or
 * details of the item. Text label can be HTML or any other JButton legal content.
 *
 * @param <T> Any object type
 */
public abstract class AbstractChoiceDialog<T> extends AbstractButtonDialog {
    private static boolean showDetails = false;
    final private String message;
    final List<T> targets;
    final private List<T> chosen = new ArrayList<T>();
    final private boolean isMultiSelect;
    private JPanel choicesPanel;
    private JToggleButton[] buttons;

    /**
     * This creates a modal AbstractChoiceDialog using the default resource bundle as a Modal dialog.
     * concrete classes must call initialize() at the end of the constructor
     *
     * @param frame         parent @JFrame that owns this dialog
     * @param title         Resource key string only, plain text will result in NPE from Resources
     * @param message       HTML or plain text message show at top of dialog
     * @param targets       things to chose from
     * @param isMultiSelect if true, allows user to select multiple items. if false first,
     *                      item chosen will close the window
     */
    protected AbstractChoiceDialog(JFrame frame, String title, String message, @Nullable List<T> targets, boolean isMultiSelect) {
        super(frame, true, title, title);
        this.message = message;
        this.targets = targets;
        this.isMultiSelect = isMultiSelect;
        // in concrete class, initialize() must be called after all member variables set
    }

    @Override
    protected Container createCenterPane() {
        JPanel result = new JPanel();
        result.setLayout(new BorderLayout());

        JPanel ops = new JPanel();
        ops.setLayout(new FlowLayout());
        result.add(ops, BorderLayout.PAGE_START);
        var msgLabel = new JLabel(message, JLabel.CENTER);
        ops.add(msgLabel);

        JToggleButton detailsCheckBox = new JToggleButton("Show details", showDetails);
        detailsCheckBox.addActionListener(e -> toggleDetails());
        ops.add(detailsCheckBox);

        choicesPanel = new JPanel();
        JScrollPane listScroller = new JScrollPane(choicesPanel);
        result.add(listScroller, BorderLayout.CENTER);

        // button per-option
        choicesPanel.setLayout(new GridLayout(0, 2));
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

        //resize window to nicely contain updated buttons
        pack();
        fit();
    }

    /**
     * @Override to set button text and/or icon with details about this choice. Usually this is
     * This is called then the show details button is depressed
     */
    abstract protected void detailLabel(JToggleButton button, T target);

    /**
     * @Override to set button text and/or icon with summary info about this choice.
     * This is called then the show details button is not depressed
     */
    abstract protected void summaryLabel(JToggleButton button, T target);

    private void toggleDetails() {
        showDetails = !showDetails;
        updateChoices();
    }

    // called when item chosen by user
    private void choose(@Nullable T choice) {
        if (!chosen.contains(choice)) {
            chosen.add(choice);
        }
        //exit immediate if only one need to be selected
        if (!isMultiSelect) {
            okAction();
            setResult(DialogResult.CONFIRMED);
            setVisible(false);
        }
    }

    /**
     * @return first chosen item, or @null if nothing chosen
     */
    public @Nullable T getFirstChoice() {
        return (chosen.size() == 0) ? null : chosen.get(0);
    }

    /**
     * @return list of items picked by user. List will be empty if nothing picked
     */
    public List<T> getChosen() {
        return chosen;
    }
}

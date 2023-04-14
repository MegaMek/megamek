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
 * @param <T> Any object type
 */
public abstract class AbstractChoiceDialog<T> extends ClientDialog {
    private static boolean showDetails = false;
    final List<T> targets;
    final private List<T> chosen = new ArrayList<T>();
    final private boolean isMultiSelect;
    final private JPanel choicesPanel;
    private boolean userResponse;
    private JToggleButton [] buttons;

    /**  */
    protected AbstractChoiceDialog(JFrame frame, String message, String title,
                                   @Nullable List<T> targets, boolean isMultiSelect) {
        super(frame, title, true, true);
        this.targets = targets;
        this.isMultiSelect = isMultiSelect;
        this.setLayout(new BorderLayout());

        JPanel ops = new JPanel();
        ops.setLayout(new FlowLayout());
        this.add(ops, BorderLayout.PAGE_START);
        var msgLabel = new JLabel(message, JLabel.CENTER );
        ops.add(msgLabel);

        JToggleButton detailsCheckBox = new JToggleButton("Show details", showDetails);
        detailsCheckBox.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleDetails();
            }
        });
        ops.add(detailsCheckBox);

        choicesPanel = new JPanel();
        JScrollPane listScroller = new JScrollPane(choicesPanel);
        this.add(listScroller, BorderLayout.CENTER);

        JPanel doneCancel = new JPanel();
        doneCancel.setLayout(new FlowLayout());
        this.add(doneCancel, BorderLayout.PAGE_END);

        if (this.isMultiSelect) {
            JButton doneButton = new JButton("OK");
            doneButton.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    doOK();
                }
            });
            doneCancel.add(doneButton);
        }

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doCancel();
            }
        });

        doneCancel.add(cancelButton);
        initChoices();
    }

    public boolean showDialog() {
        userResponse = false;
        updateChoices();
        setVisible(true);
        return userResponse;
    }

    private void initChoices() {
        choicesPanel.setLayout(new GridLayout(0,2));
        buttons = new JToggleButton[targets.size()];
        for (int i = 0; i < buttons.length; i++) {
            buttons[i] = new JToggleButton();
            T target = targets.get(i);
            choicesPanel.add(buttons[i]);
            buttons[i].addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    choose(target);
                }
            });

        }
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
    }

    /** @Override to set button text with details about this option */
    abstract protected void detailLabel(JToggleButton button, T target);

    /** @Override to set button text with a summary of this option */
    abstract  protected void summaryLabel(JToggleButton button, T target);

    private void toggleDetails() {
        showDetails = !showDetails;
        updateChoices();
        // force a resize to fit change components
        this.setVisible(true);
    }

    private void choose(@Nullable T choice) {
        if (!chosen.contains(choice)) {
            chosen.add(choice);
        }
        if (!isMultiSelect) {
            doOK();
        }
    }

    // use
    private void doOK() {
        this.userResponse = true;
        this.setVisible(false);
    }

    private void doCancel() {
        this.userResponse = false;
        this.setVisible(false);
    }

    /** @return first chosen item, or @null if nothing chosen */
    public @Nullable T getFirstChoice() {
        return (chosen.size() == 0) ? null : chosen.get(0);
    }

    /** @return list of items picked by user. List will be empty if nothing picked */
    public List<T> getChosen() {
        return chosen;
    }
}

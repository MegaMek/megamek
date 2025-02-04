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

import megamek.client.bot.caspar.ai.utility.tw.considerations.TWConsideration;
import megamek.client.bot.caspar.ai.utility.tw.decision.TWDecisionScoreEvaluator;
import megamek.client.ui.Messages;
import megamek.logging.MMLogger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;

public class DecisionScoreEvaluatorPane extends JPanel {
    private static final MMLogger logger = MMLogger.create(DecisionScoreEvaluatorPane.class);

    private JTextField nameField;
    private JTextField descriptionField;
    private JTextField notesField;
    private JPanel decisionScoreEvaluatorPane;
    private JPanel considerationsPane;
    private JToolBar considerationsToolbar;
    private JScrollPane scrollablePane;
    private final HoverStateModel hoverStateModel;
    private final List<ConsiderationPane> considerationPaneList = new ArrayList<>();
    private AtomicReference<TWDecisionScoreEvaluator> editedDecisionScoreEvaluator;
    private static final JSeparator SEPARATOR = new JSeparator(SwingConstants.HORIZONTAL);

    // Add a MouseWheelListener to forward the event to the parent JScrollPane
    private final MouseWheelListener mouseScrollListener = e -> {
        if (getParent() instanceof JViewport viewport) {
            if (viewport.getParent() instanceof JScrollPane scrollPane) {
                scrollPane.dispatchEvent(SwingUtilities.convertMouseEvent(e.getComponent(), e, scrollPane));
            }
        }
    };

    private final AtomicReference<TWConsideration> considerationParametersForCopyAndPaste = new AtomicReference<>();

    public DecisionScoreEvaluatorPane() {
        $$$setupUI$$$();
        add(decisionScoreEvaluatorPane, BorderLayout.WEST);
        hoverStateModel = new HoverStateModel();

        // Considerations Toolbar
        var newConsiderationBtn = new JButton(Messages.getString("aiEditor.new.consideration"));
        newConsiderationBtn.addActionListener(l -> addEmptyConsideration());
        considerationsToolbar.add(newConsiderationBtn);
        SEPARATOR.addMouseWheelListener(mouseScrollListener);
    }

    public TWDecisionScoreEvaluator updateInPlaceTheDSE() {
        var editedDse = editedDecisionScoreEvaluator.get();
        if (editedDse != null) {
            editedDse.setName(nameField.getText());
            editedDse.setDescription(descriptionField.getText());
            editedDse.setNotes(notesField.getText());
            editedDse.getConsiderations().clear();
            for (var considerationPane : considerationPaneList) {
                editedDse.addConsideration(considerationPane.getConsideration());
            }
            return editedDse;
        } else {
            logger.error(Messages.getString("aiEditor.edit.decisionScoreEvaluator.update.error"),
                Messages.getString("aiEditor.edit.decisionScoreEvaluator.update.error.title"));
        }
        return null;
    }

    public TWDecisionScoreEvaluator getDecisionScoreEvaluator() {
        var dse = new TWDecisionScoreEvaluator();
        dse.setName(nameField.getText());
        dse.setDescription(descriptionField.getText());
        dse.setNotes(notesField.getText());
        for (var considerationPane : considerationPaneList) {
            dse.addConsideration(considerationPane.getConsideration());
        }
        return dse;
    }

    private JPopupMenu createContextMenu(TWConsideration consideration, int index) {
        // Create a popup menu
        JPopupMenu contextMenu = new JPopupMenu();
        var newConsideration = new JMenuItem(Messages.getString("aiEditor.new.consideration"));
        var copyConsideration = new JMenuItem(Messages.getString("aiEditor.copy.consideration"));
        var pasteConsideration = new JMenuItem(Messages.getString("aiEditor.paste.consideration"));
        if (considerationParametersForCopyAndPaste.get() == null) {
            pasteConsideration.setEnabled(false);
        }
        var deleteConsideration = new JMenuItem(Messages.getString("aiEditor.delete.consideration"));
        newConsideration.addActionListener(evt -> {
            addEmptyConsideration();
        });
        copyConsideration.addActionListener(evt -> {
            considerationParametersForCopyAndPaste.set(consideration.copy());
            pasteConsideration.setEnabled(true);
        });
        pasteConsideration.addActionListener(evt -> {
            // consideration // do something so it makes sense...
            var considerationPane = new ConsiderationPane();
            considerationPane.setConsideration(considerationParametersForCopyAndPaste.get().copy());
            considerationPane.setHoverStateModel(hoverStateModel);
            considerationsPane.add(considerationPane, index);
            considerationsPane.remove(index+1);
            considerationPaneList.add(index/2, considerationPane);
            considerationPaneList.remove(index/2 + 1);
        });
        deleteConsideration.addActionListener(evt -> {
            considerationsPane.remove(index);
        });

        contextMenu.add(newConsideration);
        contextMenu.add(copyConsideration);
        contextMenu.add(pasteConsideration);
        contextMenu.add(deleteConsideration);

        JMenuItem menuItemOther = new JMenuItem(Messages.getString("aiEditor.contextualMenu.delete"));
        menuItemOther.addActionListener(evt -> {
            // Another action
            int deletePrompt = JOptionPane.showConfirmDialog(null,
                Messages.getString("aiEditor.deleteNodePrompt"),
                Messages.getString("aiEditor.deleteNode.title"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
            if (deletePrompt == JOptionPane.YES_OPTION) {
                removeConsiderationEntry(index);
            }
        });
        contextMenu.add(menuItemOther);

        return contextMenu;
    }

    private void removeConsiderationEntry(int index) {
        considerationsPane.remove(index);
        considerationPaneList.remove(index/2);
        this.updateUI();
    }

    public void addEmptyConsideration() {
        considerationsPane.removeAll();
        considerationsPane.setLayout(new GridBagLayout());
        var gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;

        var emptyConsideration = new ConsiderationPane();
        emptyConsideration.setEmptyConsideration();
        emptyConsideration.setHoverStateModel(hoverStateModel);
        emptyConsideration.addMouseWheelListener(mouseScrollListener);
        emptyConsideration.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON2 || e.getButton() == MouseEvent.BUTTON3) {
                    var t = emptyConsideration.getConsideration();
                    var contextMenu = createContextMenu(t, 0);
                    contextMenu.show(emptyConsideration, e.getX(), e.getY());
                }
            }
        });
        // add new consideration at the top of the screen
        considerationsPane.add(emptyConsideration, gbc);
        gbc.gridy++;
        considerationsPane.add(SEPARATOR, gbc);
        gbc.gridy++;
        int i = 2;
        // reinsert old considerations to the screen
        for (var c : considerationPaneList) {
            considerationsPane.add(c, gbc);
            c.addMouseWheelListener(mouseScrollListener);
            int finalI = i;
            c.addMouseListener(new MouseAdapter() {
                private final int index = finalI;
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON2 || e.getButton() == MouseEvent.BUTTON3) {
                        var t = c.getConsideration();
                        var contextMenu = createContextMenu(t, index);
                        contextMenu.show(c, e.getX(), e.getY());
                    }
                }
            });
            i++;
            gbc.gridy++;
            considerationsPane.add(new JSeparator(), gbc);
            i++;
            gbc.gridy++;
        }
        // add new consideration at the top of the list
        considerationPaneList.add(0, emptyConsideration);
        this.updateUI();
    }

    public void reset() {
        nameField.setText("");
        descriptionField.setText("");
        notesField.setText("");
        considerationsPane.removeAll();
        considerationPaneList.clear();
    }

    public void setDecisionScoreEvaluator(AtomicReference<TWDecisionScoreEvaluator> dse) {
        editedDecisionScoreEvaluator = dse;
        setDecisionScoreEvaluator(dse.get());
    }

    public void setDecisionScoreEvaluator(TWDecisionScoreEvaluator dse) {
        nameField.setText(dse.getName());
        descriptionField.setText(dse.getDescription());
        notesField.setText(dse.getNotes());
        considerationsPane.removeAll();

        var considerations = dse.getConsiderations();
        considerationsPane.setLayout(new GridBagLayout());
        considerationPaneList.clear();
        var gbc = new GridBagConstraints();
        int i = 0;
        for (var consideration : considerations) {
            var c = new ConsiderationPane();
            c.setConsideration(consideration);
            c.addMouseWheelListener(mouseScrollListener);
            c.setHoverStateModel(hoverStateModel);
            considerationPaneList.add(c);
            considerationsPane.add(c, gbc);
            int finalI = i;
            c.addMouseListener(new MouseAdapter() {
                private final int index = finalI;
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON2 || e.getButton() == MouseEvent.BUTTON3) {
                        var t = c.getConsideration();
                        var contextMenu = createContextMenu(t, index);
                        contextMenu.show(c, e.getX(), e.getY());
                    }
                }
            });
            gbc.gridy++;
            i++;
            considerationsPane.add(SEPARATOR, gbc);
            gbc.gridy++;
            i++;
        }
        this.updateUI();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        decisionScoreEvaluatorPane = new JPanel();
        decisionScoreEvaluatorPane.setLayout(new GridBagLayout());
        nameField = new JTextField();
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        decisionScoreEvaluatorPane.add(nameField, gbc);
        descriptionField = new JTextField();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        decisionScoreEvaluatorPane.add(descriptionField, gbc);
        notesField = new JTextField();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        decisionScoreEvaluatorPane.add(notesField, gbc);
        scrollablePane = new JScrollPane();
        scrollablePane.setHorizontalScrollBarPolicy(31);
        scrollablePane.setMaximumSize(new Dimension(-1, Integer.MAX_VALUE));
        scrollablePane.setMinimumSize(new Dimension(400, 400));
        scrollablePane.setWheelScrollingEnabled(true);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.gridheight = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        decisionScoreEvaluatorPane.add(scrollablePane, gbc);
        considerationsPane = new JPanel();
        considerationsPane.setLayout(new GridBagLayout());
        considerationsPane.setMaximumSize(new Dimension(-1, Integer.MAX_VALUE));
        considerationsPane.setMinimumSize(new Dimension(400, 400));
        scrollablePane.setViewportView(considerationsPane);
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, this.$$$getMessageFromBundle$$$("megamek/common/options/messages", "aiEditor.considerations"));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        decisionScoreEvaluatorPane.add(label1, gbc);
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2, this.$$$getMessageFromBundle$$$("megamek/common/options/messages", "aiEditor.notes"));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        decisionScoreEvaluatorPane.add(label2, gbc);
        final JLabel label3 = new JLabel();
        this.$$$loadLabelText$$$(label3, this.$$$getMessageFromBundle$$$("megamek/common/options/messages", "aiEditor.description"));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        decisionScoreEvaluatorPane.add(label3, gbc);
        final JLabel label4 = new JLabel();
        this.$$$loadLabelText$$$(label4, this.$$$getMessageFromBundle$$$("megamek/common/options/messages", "aiEditor.name"));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        decisionScoreEvaluatorPane.add(label4, gbc);
        considerationsToolbar = new JToolBar();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        decisionScoreEvaluatorPane.add(considerationsToolbar, gbc);
        label1.setLabelFor(scrollablePane);
        label2.setLabelFor(notesField);
        label3.setLabelFor(descriptionField);
        label4.setLabelFor(nameField);
    }

    private String $$$getMessageFromBundle$$$(String path, String key) {
        ResourceBundle bundle = ResourceBundle.getBundle(path);
        return bundle.getString(key);
    }


    private void $$$loadLabelText$$$(JLabel component, String text) {
        StringBuffer result = new StringBuffer();
        boolean haveMnemonic = false;
        char mnemonic = '\0';
        int mnemonicIndex = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&') {
                i++;
                if (i == text.length()) break;
                if (!haveMnemonic && text.charAt(i) != '&') {
                    haveMnemonic = true;
                    mnemonic = text.charAt(i);
                    mnemonicIndex = result.length();
                }
            }
            result.append(text.charAt(i));
        }
        component.setText(result.toString());
        if (haveMnemonic) {
            component.setDisplayedMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
    }


    public JComponent $$$getRootComponent$$$() {
        return decisionScoreEvaluatorPane;
    }

}

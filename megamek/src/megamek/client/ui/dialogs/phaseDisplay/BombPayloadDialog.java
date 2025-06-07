/*
 * MegaMek -
 * Copyright (C) 2002-2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2018 The MegaMek Team
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
package megamek.client.ui.dialogs.phaseDisplay;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Serial;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import megamek.client.ui.Messages;
import megamek.common.BombLoadout;
import megamek.common.BombType;
import megamek.common.BombType.BombTypeEnum;

/**
 * A dialog to determine bomb payload.
 *
 * @author suvarov454@sourceforge.net
 */
public class BombPayloadDialog extends JDialog implements ActionListener, ItemListener {
    @Serial
    private static final long serialVersionUID = -4629867982571421459L;

    private boolean confirm = false;
    private int limit;
    private int internalBombLimit = 6;
    private int internalBombCount = 0;
    private BombLoadout availableBombs;

    private final JPanel panButtons = new JPanel();
    private final JButton butOK = new JButton(Messages.getString("Okay"));
    private final JButton butCancel = new JButton(Messages.getString("Cancel"));


    private EnumMap<BombTypeEnum, JComboBox<String>> b_choices = new EnumMap<>(BombTypeEnum.class);
    private EnumMap<BombTypeEnum, JLabel> b_labels = new EnumMap<>(BombTypeEnum.class);
    private JLabel description;

    /**
     * Keeps track of the number of fighters in the squadron, 0 implies a single fighter not in a squadron squadron.
     */
    private double numFighters;

    /**
     * Create and initialize the dialog.
     *
     * @param parent      - the <code>Frame</code> that is locked by this dialog.
     * @param title       - the title <code>String</code> for this dialog.
     * @param availableBombs The bomb choice list
     * @param spaceBomb   Flag for whether this is space bombing
     * @param bombDump
     * @param lim
     * @param numFighters The number of fighters in a squadron, 0 implies a single fighter not in a squadron.
     */
    @SuppressWarnings("unchecked")
    private void initialize(JFrame parent, String title, BombLoadout availableBombs, boolean spaceBomb, boolean bombDump, int lim,
                            int numFighters) {
        // super.setResizable(false);

        this.numFighters = numFighters;
        this.availableBombs = new BombLoadout(availableBombs);
        this.limit = lim;

        GridBagLayout gridbag = new GridBagLayout();
        setLayout(gridbag);

        GridBagConstraints c = new GridBagConstraints();

        c.gridwidth = 4;
        c.gridheight = 1;
        c.gridx = 0;
        //c.gridy = 0;
        c.insets = new Insets(5, 5, 5, 5);

        description = new JLabel();
        if (numFighters != 0) {
            description.setText(Messages.getString("BombPayloadDialog.SquadronBombDesc"));
        } else {
            description.setText(Messages.getString("BombPayloadDialog.FighterBombDesc"));
        }
        add(description, c);

        c.gridwidth = 1;
        c.gridheight = 1;
        c.gridx = 1;
        c.gridy = 1;

        //initialize the bomb choices
        int currentRow = 1;
        for (BombTypeEnum bombType : BombTypeEnum.values()) {
            if (bombType == BombTypeEnum.NONE) continue;

            int availableCount = availableBombs.getCount(bombType);
            if (availableCount == 0) continue;
            if (spaceBomb && !bombType.canSpaceBomb()) continue;
            if (!spaceBomb && !bombDump && !bombType.canGroundBomb()) continue;

            JComboBox<String> comboBox = new JComboBox<>();
            JLabel label = new JLabel(bombType.getDisplayName());
            
            b_choices.put(bombType, comboBox);
            b_labels.put(bombType, label);

            int maxSelectable = availableCount;
            if ((limit > -1) && (maxSelectable > limit)) {
                maxSelectable = limit;
            }

            if (numFighters != 0) {
                // Squadrons give the salvo size, and the whole salvo must be
                //  fired

                // Add 0 bombs
                comboBox.addItem("0");
                int maxNumSalvos = (int) Math.ceil(availableCount / this.numFighters);
                // Add the full-squadron salvos
                for (int j = 1; j < maxNumSalvos; j++) {
                    int numBombs = (int) (j * numFighters);
                    comboBox.addItem(j + " (" + numBombs + ")");
                }
                // Add the maximum number of salvos
                comboBox.addItem(maxNumSalvos + " (" + availableCount + ")");
            } else {
                for (int x = 0; x <= maxSelectable; x++) {
                    comboBox.addItem(Integer.toString(x));
                }
            }
            comboBox.setSelectedIndex(0);
            comboBox.addItemListener(this);

            c.gridx = 1;
            c.gridy = currentRow + 1;
            c.anchor = GridBagConstraints.EAST;
            add(label, c);
            gridbag.setConstraints(label, c);
            c.gridx = 2;
            c.gridy = currentRow + 1;
            c.anchor = GridBagConstraints.WEST;
            add(comboBox, c);
            gridbag.setConstraints(comboBox, c);
            currentRow++;
        }

        // Allow the player to confirm or abort the choice.
        setupButtons();
        c.gridy++;
        c.insets = new Insets(5, 5, 5, 5);
        add(panButtons, c);
        butOK.requestFocus();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                setVisible(false);
            }
        });

        pack();
        Dimension size = getSize();
        setLocation((parent.getLocation().x + (parent.getSize().width / 2)) - (size.width / 2),
              (parent.getLocation().y + (parent.getSize().height / 2)) - (size.height / 2));
    }

    private void setupButtons() {
        butOK.addActionListener(this);
        butCancel.addActionListener(this);

        // layout
        GridBagLayout gridBag = new GridBagLayout();
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        panButtons.setLayout(gridBag);

        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(10, 5, 5, 5);
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = GridBagConstraints.VERTICAL;
        gridBagConstraints.ipadx = 20;
        gridBagConstraints.ipady = 5;

        gridBagConstraints.gridwidth = 1;
        gridBag.setConstraints(butOK, gridBagConstraints);
        panButtons.add(butOK);

        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBag.setConstraints(butCancel, gridBagConstraints);
        panButtons.add(butCancel);
    }

    /**
     * Create a choice dialog. The player can choose any or all of the choices. If no choices are passed in, this will
     * be a very boring dialog, but it will not suffer an exception.
     *
     * @param parent      - the <code>Frame</code> that is locked by this dialog.
     * @param title       - the title <code>String</code> for this dialog.
     * @param bombs       - an array of <code>String</code>s the number of bombs of each type
     * @param spaceBomb
     * @param bombDump
     * @param limit
     * @param numFighters
     */
    public BombPayloadDialog(JFrame parent, String title, BombLoadout bombs, boolean spaceBomb, boolean bombDump, int limit,
                             int numFighters) {
        super(parent, title, true);
        initialize(parent, title, bombs, spaceBomb, bombDump, limit, numFighters);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == butOK) {
            confirm = true;
            setVisible(false);
        } else {
            confirm = false;
            setVisible(false);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void itemStateChanged(ItemEvent ie) {
        if (limit < 0) {
            return;
        }

        BombLoadout currentSelections = new BombLoadout();
        for (Map.Entry<BombTypeEnum, JComboBox<String>> entry : b_choices.entrySet()) {
            BombTypeEnum bombType = entry.getKey();
            JComboBox<String> comboBox = entry.getValue();
            
            int selectedCount = getSelectedCount(comboBox);
            if (selectedCount > 0) {
                currentSelections.put(bombType, selectedCount);
            }
        }
        
        // Calculate remaining capacity for each bomb type
        // don't factor in your own choice when determining how much is left
        int totalSelected = currentSelections.getTotalBombs();
        for (Map.Entry<BombTypeEnum, JComboBox<String>> entry : b_choices.entrySet()) {
            BombTypeEnum bombType = entry.getKey();
            JComboBox<String> comboBox = entry.getValue();
            
            comboBox.removeItemListener(this);
            comboBox.removeAllItems();
            
            int currentSelected = currentSelections.getCount(bombType);
            int remainingCapacity = limit - (totalSelected - currentSelected);
            int available = availableBombs.getCount(bombType);
            int maxSelectable = Math.min(available, remainingCapacity);
            
            if (numFighters != 0) {
                // Squadron logic
                comboBox.addItem("0");
                int maxNumSalvos = (int) Math.ceil(maxSelectable / this.numFighters);
                
                for (int j = 1; j <= maxNumSalvos; j++) {
                    int numBombs = (int) Math.min(j * numFighters, available);
                    if (numBombs <= maxSelectable) {
                        comboBox.addItem(j + " (" + numBombs + ")");
                    }
                }
            } else {
                for (int x = 0; x <= maxSelectable; x++) {
                    comboBox.addItem(Integer.toString(x));
                }
            }
            
            // Restore selection if possible
            if (currentSelected <= maxSelectable) {
                setSelectedCount(comboBox, currentSelected);
            } else {
                comboBox.setSelectedIndex(0);
            }
            
            comboBox.addItemListener(this);
        }
    }

    private int getSelectedCount(JComboBox<String> comboBox) {
        String selected = (String) comboBox.getSelectedItem();
        if (selected == null || "0".equals(selected)) {
            return 0;
        }
        // Squadrons have to parse values differently
        if (numFighters != 0) {
            // Parse squadron format: "# (#)"
            StringTokenizer toks = new StringTokenizer(selected, "() ");
            toks.nextToken(); // Skip salvo count
            if (toks.hasMoreTokens()) {
                try {
                    return Integer.parseInt(toks.nextToken());
                } catch (NumberFormatException ignored) {
                    // Will return 0
                }
            }
            return 0;
        } else {
            try {
                return Integer.parseInt(selected);
            } catch (NumberFormatException ignored) {
                return 0; // If parsing fails, return 0
            }
        }
    }

    private void setSelectedCount(JComboBox<String> comboBox, int count) {
        if (count == 0) {
            comboBox.setSelectedIndex(0);
            return;
        }
        
        for (int i = 0; i < comboBox.getItemCount(); i++) {
            String item = comboBox.getItemAt(i);
            if (numFighters != 0) {
                // Squadron format
                if (item.contains("(") && item.contains(")")) {
                    StringTokenizer toks = new StringTokenizer(item, "() ");
                    toks.nextToken(); // Skip salvo count
                    try {
                        if (toks.hasMoreTokens() && (Integer.parseInt(toks.nextToken()) == count)) {
                            comboBox.setSelectedIndex(i);
                            return;
                        }
                    } catch (NumberFormatException ignored) {
                        // If parsing fails, continue to next item
                    }
                }
            } else {
                if (Integer.toString(count).equals(item)) {
                    comboBox.setSelectedIndex(i);
                    return;
                }
            }
        }
    }

    /**
     * See if the player confirmed a choice.
     *
     * @return <code>true</code> if the player has confirmed a choice.
     *       <code>false</code> if the player canceled, if the player did not
     *       select a choice, or if no choices were available.
     */
    public boolean getAnswer() {
        return confirm && (getChoices() != null);
    }

    /**
     * Which choices did the player select?
     *
     * @return If no choices were available, if the player canceled, if the player did not select a choice, or if the
     *       player canceled the choice, a <code>null</code> value is returned, otherwise an array of the
     *       <code>int</code> indexes from the input array that match the selected choices is returned.
     */
    public BombLoadout getChoices() {
        if (!confirm) {
            return null;
        }

        BombLoadout choices = new BombLoadout();
        for (Map.Entry<BombTypeEnum, JComboBox<String>> entry : b_choices.entrySet()) {
            BombTypeEnum bombType = entry.getKey();
            JComboBox<String> comboBox = entry.getValue();
            
            int selectedCount = getSelectedCount(comboBox);
            if (selectedCount > 0) {
                choices.put(bombType, selectedCount);
            }
        }
        
        return choices.isEmpty() ? null : choices;
    }

    public int getInternalBombLimit() {
        return internalBombLimit;
    }

    public void setInternalBombLimit(int internalBombLimit) {
        this.internalBombLimit = internalBombLimit;
    }

    public int getInternalBombCount() {
        return internalBombCount;
    }

    public void setInternalBombCount(int internalBombCount) {
        this.internalBombCount = internalBombCount;
    }
}

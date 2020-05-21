/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * MegaMek - Copyright (C) 2020 - The MegaMek Team
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

package megamek.client.ui.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import megamek.client.ui.Messages;

/** 
 * Allows the player to select the type of entity in the hexes used
 * by the LOS tool.
 */
public class LOSDialog extends ClientDialog implements ActionListener {

    private static final long serialVersionUID = 5633160028901713806L;

    JButton butOK = new JButton(new OkayAction(this)); 

    /**
     * The checkboxes for available choices.
     */
    private JCheckBox[] checkboxes1 = new JCheckBox[2];
    private JCheckBox[] checkboxes2 = new JCheckBox[2];

    public LOSDialog(JFrame parent, boolean mechInFirst, boolean mechInSecond) {
        super(parent, Messages.getString("LOSDialog.title"), true); //$NON-NLS-1$
        super.setResizable(false);

        // The panel with the options
        JPanel midPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        JLabel labMessage = new JLabel(Messages
                .getString("LOSDialog.inFirstHex"), SwingConstants.LEFT); //$NON-NLS-1$
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        midPanel.add(labMessage, c);

        checkboxes1[0] = new JCheckBox(
                Messages.getString("LOSDialog.Mech"), mechInFirst); //$NON-NLS-1$
        c.gridwidth = 1;
        midPanel.add(checkboxes1[0], c);

        checkboxes1[1] = new JCheckBox(
                Messages.getString("LOSDialog.NonMech"), !mechInFirst); //$NON-NLS-1$
        c.gridwidth = GridBagConstraints.REMAINDER;
        midPanel.add(checkboxes1[1], c);
        
        addSpacerRow(midPanel, c, 20);
        
        labMessage = new JLabel(
                Messages.getString("LOSDialog.InSecondHex"), SwingConstants.LEFT); //$NON-NLS-1$
        c.gridwidth = GridBagConstraints.REMAINDER;
        midPanel.add(labMessage, c);

        checkboxes2[0] = new JCheckBox(
                Messages.getString("LOSDialog.Mech"), mechInSecond); //$NON-NLS-1$
        c.gridwidth = 1;
        midPanel.add(checkboxes2[0], c);

        checkboxes2[1] = new JCheckBox(
                Messages.getString("LOSDialog.NonMech"), !mechInSecond); //$NON-NLS-1$
        c.gridwidth = GridBagConstraints.REMAINDER;
        midPanel.add(checkboxes2[1], c);

        addSpacerRow(midPanel, c, 20);
        
        // group the checkboxes
        ButtonGroup radioGroup1 = new ButtonGroup();
        radioGroup1.add(checkboxes1[0]);
        radioGroup1.add(checkboxes1[1]);
        ButtonGroup radioGroup2 = new ButtonGroup();
        radioGroup2.add(checkboxes2[0]);
        radioGroup2.add(checkboxes2[1]);
        
        // A bit of spacing
        add(Box.createHorizontalStrut(20), BorderLayout.LINE_START);
        add(Box.createVerticalStrut(10), BorderLayout.PAGE_START);
        
        // Assemble the dialog panel
        add(midPanel, BorderLayout.CENTER);
        add(butOK, BorderLayout.PAGE_END);
        
        setMinimumSize(new Dimension(300, 140));
        pack();
        center();

        butOK.requestFocusInWindow();
    }

    public void actionPerformed(ActionEvent e) {
        setVisible(false);
    }

    public boolean getMechInFirst() {
        return checkboxes1[0].isSelected();
    }

    public boolean getMechInSecond() {
        return checkboxes2[0].isSelected();
    }
}

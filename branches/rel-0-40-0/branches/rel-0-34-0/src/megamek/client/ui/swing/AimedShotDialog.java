/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.widget.IndexedCheckbox;

public class AimedShotDialog extends JDialog {
    /**
     * 
     */
    private static final long serialVersionUID = 6527374019085650613L;

    private JButton butNoAim = new JButton(Messages
            .getString("AimedShotDialog.dontAim")); //$NON-NLS-1$

    /**
     * The checkboxes for available choices.
     */
    private IndexedCheckbox[] checkboxes;
    private boolean[] boxEnabled;

    public AimedShotDialog(JFrame parent, String title, String message,
            String[] choices, boolean[] enabled, int selectedIndex,
            ItemListener il, ActionListener al) {
        super(parent, title, false);
        super.setResizable(false);

        boxEnabled = enabled;

        GridBagLayout gridbag = new GridBagLayout();
        getContentPane().setLayout(gridbag);

        GridBagConstraints c = new GridBagConstraints();

        JLabel labMessage = new JLabel(message, SwingConstants.LEFT);
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridwidth = 0;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(labMessage, c);
        getContentPane().add(labMessage);

        ButtonGroup radioGroup = new ButtonGroup();
        checkboxes = new IndexedCheckbox[choices.length];

        for (int i = 0; i < choices.length; i++) {
            boolean even = (i & 1) == 0;
            checkboxes[i] = new IndexedCheckbox(choices[i], i == selectedIndex,
                    radioGroup, i);
            checkboxes[i].addItemListener(il);
            checkboxes[i].setEnabled(enabled[i]);
            c.gridwidth = even ? 1 : GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.WEST;
            gridbag.setConstraints(checkboxes[i], c);
            add(checkboxes[i]);
        }

        butNoAim.addActionListener(al);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(5, 0, 5, 0);
        c.anchor = GridBagConstraints.CENTER;
        gridbag.setConstraints(butNoAim, c);
        add(butNoAim);

        butNoAim.requestFocus();

        pack();
        setLocation(parent.getLocation().x + parent.getSize().width / 2
                - getSize().width / 2, parent.getLocation().y
                + parent.getSize().height / 2 - getSize().height / 2);
    }

    public void setEnableAll(boolean enableAll) {
        for (int i = 0; i < checkboxes.length; i++) {
            if (enableAll) {
                checkboxes[i].setEnabled(boxEnabled[i]);
            } else {
                checkboxes[i].setEnabled(false);
            }
        }
    }

}
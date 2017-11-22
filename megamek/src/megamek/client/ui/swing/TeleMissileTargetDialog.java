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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.widget.IndexedRadioButton;


public class TeleMissileTargetDialog extends JDialog implements ActionListener {
    /**
     *
     */
    private static final long serialVersionUID = 6527373019065650613L;

    private JButton butOk = new JButton(Messages.getString("Okay")); //$NON-NLS-1$
    private int target;

    /**
     * The checkboxes for available choices.
     */
    private IndexedRadioButton[] checkboxes;
    private boolean[] boxEnabled;

    public TeleMissileTargetDialog(JFrame parent, String title, String message,
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
        checkboxes = new IndexedRadioButton[choices.length];

        for (int i = 0; i < choices.length; i++) {
            boolean even = (i & 1) == 0;
            checkboxes[i] = new IndexedRadioButton(choices[i], i == selectedIndex,
                    radioGroup, i);
            checkboxes[i].addItemListener(il);
            checkboxes[i].setEnabled(enabled[i]);
            c.gridwidth = even ? 1 : GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.WEST;
            gridbag.setConstraints(checkboxes[i], c);
            add(checkboxes[i]);
        }

        butOk.addActionListener(al);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(5, 0, 5, 0);
        c.anchor = GridBagConstraints.CENTER;
        gridbag.setConstraints(butOk, c);
        add(butOk);

        butOk.requestFocus();

        pack();
        setLocation((parent.getLocation().x + (parent.getSize().width / 2))
                - (getSize().width / 2), (parent.getLocation().y
                + (parent.getSize().height / 2)) - (getSize().height / 2));
    }
    
    public int getTarget() {
        return target;
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
    
    /**
     * ActionListener, listens to the button in the dialog.
     */
    public void actionPerformed(ActionEvent actionEvent) {
        if (actionEvent.getSource().equals(butOk)) {
            closeDialog();
        }
    }
    
    /**
     * ItemListener, listens to the radiobuttons in the dialog.
     */
    public void itemStateChanged(ItemEvent ev) {
        IndexedRadioButton icb = (IndexedRadioButton) ev.getSource();
        target = icb.getIndex();
    }
    
    public void closeDialog() {
        if (this != null) {
            target = 0;
            this.dispose();
        }
    }

}
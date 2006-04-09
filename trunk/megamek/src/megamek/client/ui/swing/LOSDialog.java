/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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

import javax.swing.JButton;
import javax.swing.JCheckBox;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JLabel;
import javax.swing.ButtonGroup;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

// Allows the player to select the type of entity in the hexes used
// by the LOS tool.
public class LOSDialog
    extends Dialog implements ActionListener
{
    private JButton butOK = new JButton(Messages.getString("Okay")); //$NON-NLS-1$

    /**
     * The checkboxes for available choices.
     */
    private JCheckBox[] checkboxes1 = null;
    private JCheckBox[] checkboxes2 = null;

    public LOSDialog(Frame parent, boolean mechInFirst, boolean mechInSecond) {
        super(parent, Messages.getString("LOSDialog.title"), true); //$NON-NLS-1$
        super.setResizable(false);

        // closing the window is the same as hitting butOK
        addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    actionPerformed(new ActionEvent(butOK,ActionEvent.ACTION_PERFORMED,butOK.getText()));
                }
        });

        GridBagLayout gridbag = new GridBagLayout();
        setLayout(gridbag);

        GridBagConstraints c = new GridBagConstraints();

        JLabel labMessage = new JLabel(Messages.getString("LOSDialog.inFirstHex"), JLabel.LEFT); //$NON-NLS-1$
        c.weightx = 1.0;    c.weighty = 1.0;
        c.gridwidth = 0;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(labMessage, c);
        add(labMessage);
        
        ButtonGroup radioGroup1 = new ButtonGroup();
        checkboxes1 = new JCheckBox[2];
        
        checkboxes1[0] = new JCheckBox(Messages.getString("LOSDialog.Mech"), mechInFirst); //$NON-NLS-1$
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(checkboxes1[0], c);
        radioGroup1.add(checkboxes1[0]);
        add(checkboxes1[0]);

        checkboxes1[1] = new JCheckBox(Messages.getString("LOSDialog.NonMech"), !mechInFirst); //$NON-NLS-1$
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(checkboxes1[1], c);
        radioGroup1.add(checkboxes1[1]);
        add(checkboxes1[1]);

        labMessage = new JLabel(Messages.getString("LOSDialog.InSecondHex"), JLabel.LEFT); //$NON-NLS-1$
        c.weightx = 1.0;    c.weighty = 1.0;
        c.gridwidth = 0;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(labMessage, c);
        add(labMessage);
        
        ButtonGroup radioGroup2 = new ButtonGroup();
        checkboxes2 = new JCheckBox[2];
        
        checkboxes2[0] = new JCheckBox(Messages.getString("LOSDialog.Mech"), mechInSecond); //$NON-NLS-1$
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(checkboxes2[0], c);
        radioGroup2.add(checkboxes2[0]);
        add(checkboxes2[0]);

        checkboxes2[1] = new JCheckBox(Messages.getString("LOSDialog.NonMech"), !mechInSecond); //$NON-NLS-1$
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(checkboxes2[1], c);
        radioGroup2.add(checkboxes2[1]);
        add(checkboxes2[1]);

        butOK.addActionListener(this);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(5, 0, 5, 0);
        c.anchor = GridBagConstraints.CENTER;
        gridbag.setConstraints(butOK, c);
        add(butOK);

        pack();

        setLocation(parent.getLocation().x + parent.getSize().width/2 - getSize().width/2,
                    parent.getLocation().y + parent.getSize().height/2 - getSize().height/2);

        // we'd like the OK button to have focus, but that can only be done on displayed
        // dialogs in Windows. So, this rather elaborate setup: as soon as the first focusable
        // component receives the focus, it shunts the focus to the OK button, and then
        // removes the FocusListener to prevent this happening again
        checkboxes1[0].addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {
                butOK.requestFocus();
            }
            public void focusLost(FocusEvent e) {
                checkboxes1[0].removeFocusListener(this); // refers to listener
            }
        });
    }

    public void actionPerformed(ActionEvent e) {
        this.setVisible(false);
    }

    public boolean getMechInFirst() {
        return this.checkboxes1[0].isSelected() == true;
    }

    public boolean getMechInSecond() {
        return this.checkboxes2[0].isSelected() == true;
    }
} 

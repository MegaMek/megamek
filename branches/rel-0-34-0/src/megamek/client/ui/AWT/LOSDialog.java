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

package megamek.client.ui.AWT;

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import megamek.client.ui.Messages;

// Allows the player to select the type of entity in the hexes used
// by the LOS tool.
public class LOSDialog extends Dialog implements ActionListener {
    /**
     * 
     */
    private static final long serialVersionUID = 2709304305904897160L;

    private Button butOK = new Button(Messages.getString("Okay")); //$NON-NLS-1$

    /**
     * The checkboxes for available choices.
     */
    private Checkbox[] checkboxes1 = null;
    private Checkbox[] checkboxes2 = null;

    public LOSDialog(Frame parent, boolean mechInFirst, boolean mechInSecond) {
        super(parent, Messages.getString("LOSDialog.title"), true); //$NON-NLS-1$
        super.setResizable(false);

        // closing the window is the same as hitting butOK
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                actionPerformed(new ActionEvent(butOK,
                        ActionEvent.ACTION_PERFORMED, butOK.getLabel()));
            }
        });

        GridBagLayout gridbag = new GridBagLayout();
        setLayout(gridbag);

        GridBagConstraints c = new GridBagConstraints();

        Label labMessage = new Label(
                Messages.getString("LOSDialog.inFirstHex"), Label.LEFT); //$NON-NLS-1$
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridwidth = 0;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(labMessage, c);
        add(labMessage);

        CheckboxGroup radioGroup1 = new CheckboxGroup();
        checkboxes1 = new Checkbox[2];

        checkboxes1[0] = new Checkbox(
                Messages.getString("LOSDialog.Mech"), mechInFirst, radioGroup1); //$NON-NLS-1$
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(checkboxes1[0], c);
        add(checkboxes1[0]);

        checkboxes1[1] = new Checkbox(
                Messages.getString("LOSDialog.NonMech"), !mechInFirst, radioGroup1); //$NON-NLS-1$
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(checkboxes1[1], c);
        add(checkboxes1[1]);

        labMessage = new Label(
                Messages.getString("LOSDialog.InSecondHex"), Label.LEFT); //$NON-NLS-1$
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridwidth = 0;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(labMessage, c);
        add(labMessage);

        CheckboxGroup radioGroup2 = new CheckboxGroup();
        checkboxes2 = new Checkbox[2];

        checkboxes2[0] = new Checkbox(
                Messages.getString("LOSDialog.Mech"), mechInSecond, radioGroup2); //$NON-NLS-1$
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(checkboxes2[0], c);
        add(checkboxes2[0]);

        checkboxes2[1] = new Checkbox(
                Messages.getString("LOSDialog.NonMech"), !mechInSecond, radioGroup2); //$NON-NLS-1$
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(checkboxes2[1], c);
        add(checkboxes2[1]);

        butOK.addActionListener(this);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(5, 0, 5, 0);
        c.anchor = GridBagConstraints.CENTER;
        gridbag.setConstraints(butOK, c);
        add(butOK);

        pack();

        setLocation(parent.getLocation().x + parent.getSize().width / 2
                - getSize().width / 2, parent.getLocation().y
                + parent.getSize().height / 2 - getSize().height / 2);

        // we'd like the OK button to have focus, but that can only be done on
        // displayed
        // dialogs in Windows. So, this rather elaborate setup: as soon as the
        // first focusable
        // component receives the focus, it shunts the focus to the OK button,
        // and then
        // removes the FocusListener to prevent this happening again
        checkboxes1[0].addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {
                butOK.requestFocus();
            }

            public void focusLost(FocusEvent e) {
                checkboxes1[0].removeFocusListener(this); // refers to
                                                            // listener
            }
        });
    }

    public void actionPerformed(ActionEvent e) {
        this.setVisible(false);
    }

    public boolean getMechInFirst() {
        return this.checkboxes1[0].getState() == true;
    }

    public boolean getMechInSecond() {
        return this.checkboxes2[0].getState() == true;
    }
}

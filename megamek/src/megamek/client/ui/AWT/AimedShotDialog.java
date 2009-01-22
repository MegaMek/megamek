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

package megamek.client.ui.AWT;

import java.awt.Button;
import java.awt.CheckboxGroup;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;

import megamek.client.ui.Messages;
import megamek.client.ui.AWT.widget.IndexedCheckbox;

public class AimedShotDialog extends Dialog {
    /**
     * 
     */
    private static final long serialVersionUID = 3780327164169237560L;

    private Button butNoAim = new Button(Messages
            .getString("AimedShotDialog.dontAim")); //$NON-NLS-1$

    /**
     * The checkboxes for available choices.
     */
    private IndexedCheckbox[] checkboxes = null;
    private boolean[] enabled = null;

    public AimedShotDialog(Frame parent, String title, String message,
            String[] choices, boolean[] enabled, int selectedIndex,
            ItemListener il, ActionListener al) {
        super(parent, title, false);
        super.setResizable(false);

        this.enabled = enabled;

        GridBagLayout gridbag = new GridBagLayout();
        setLayout(gridbag);

        GridBagConstraints c = new GridBagConstraints();

        Label labMessage = new Label(message, Label.LEFT);
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridwidth = 0;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(labMessage, c);
        add(labMessage);

        CheckboxGroup radioGroup = new CheckboxGroup();
        checkboxes = new IndexedCheckbox[choices.length];

        for (int i = 0; i < choices.length; i++) {
            boolean even = (i & 1) == 0;
            checkboxes[i] = new IndexedCheckbox(choices[i],
                    (i == selectedIndex), radioGroup, i);
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
                checkboxes[i].setEnabled(enabled[i]);
            } else {
                checkboxes[i].setEnabled(false);
            }
        }
    }

}
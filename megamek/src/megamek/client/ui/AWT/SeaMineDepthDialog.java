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
import java.awt.Choice;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import megamek.client.ui.Messages;

/**
 * Ask for the density of all mines.
 */
public class SeaMineDepthDialog extends Dialog implements ActionListener {

    /**
     * 
     */
    private static final long serialVersionUID = 1432135839873581337L;
    private GridBagLayout gridbag = new GridBagLayout();
    private GridBagConstraints c = new GridBagConstraints();

    private Button butOk = new Button(Messages.getString("Okay")); //$NON-NLS-1$
    private Label labDepth = new Label(Messages
            .getString("SeaMineDepthDialog.labDepth"), Label.RIGHT); //$NON-NLS-1$
    private Choice choDepth = new Choice();

    private int depth = 5;
    //private Frame frame = null;

    public SeaMineDepthDialog(Frame p, int totalDepth) {
        super(p, Messages.getString("SeaMineDepthDialog.title"), true); //$NON-NLS-1$
        super.setResizable(false);
        //frame = p;

        butOk.addActionListener(this);

        choDepth.removeAll();
        for(int i =0; i < (totalDepth + 1); i++) {
            choDepth.add(Integer.toString(i));
        }
        choDepth.select(0);

        setLayout(gridbag);

        c.fill = GridBagConstraints.VERTICAL;
        c.insets = new Insets(1, 1, 1, 1);
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(labDepth, c);
        add(labDepth);

        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 0.0;
        c.weighty = 0.0;
        gridbag.setConstraints(choDepth, c);
        add(choDepth);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 0.0;
        c.weighty = 0.0;
        gridbag.setConstraints(butOk, c);
        add(butOk);

        pack();
        setLocation(p.getLocation().x + p.getSize().width / 2 - getSize().width
                / 2, p.getLocation().y + p.getSize().height / 2
                - getSize().height / 2);
    }

    public int getDepth() {
        return depth;
    }

    public void actionPerformed(ActionEvent actionEvent) {
        if (actionEvent.getSource() == butOk) {
            depth = Integer.parseInt(choDepth.getSelectedItem());
        }
        this.setVisible(false);
    }
}

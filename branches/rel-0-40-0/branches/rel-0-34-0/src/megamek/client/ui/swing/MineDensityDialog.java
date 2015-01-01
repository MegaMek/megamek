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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import megamek.client.ui.Messages;

/**
 * Ask for the setting for a vibrabomb.
 */
public class MineDensityDialog extends JDialog implements ActionListener {
    /**
     * 
     */
    private static final long serialVersionUID = -7642956136536119067L;
    private GridBagLayout gridbag = new GridBagLayout();
    private GridBagConstraints c = new GridBagConstraints();
    private JButton butOk = new JButton(Messages.getString("Okay")); //$NON-NLS-1$
    private JLabel labDensity = new JLabel(Messages
            .getString("MineDensityDialog.labDensity"), SwingConstants.RIGHT); //$NON-NLS-1$
    private JComboBox choDensity = new JComboBox();
    private int density;
    //private JFrame frame;

    public MineDensityDialog(JFrame p) {
        super(p, Messages.getString("MineDensityDialog.title"), true); //$NON-NLS-1$
        super.setResizable(false);
        //frame = p;
        butOk.addActionListener(this);
        
        choDensity.removeAllItems();
        for(int i =5; i < 35; i = i + 5) {
            choDensity.addItem(Integer.toString(i));
        }
        choDensity.setSelectedIndex(0);
        
        getContentPane().setLayout(gridbag);
        c.fill = GridBagConstraints.VERTICAL;
        c.insets = new Insets(1, 1, 1, 1);
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(labDensity, c);
        getContentPane().add(labDensity);
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 0.0;
        c.weighty = 0.0;
        gridbag.setConstraints(choDensity, c);
        getContentPane().add(choDensity);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 0.0;
        c.weighty = 0.0;
        gridbag.setConstraints(butOk, c);
        getContentPane().add(butOk);
        pack();
        setLocation(p.getLocation().x + p.getSize().width / 2 - getSize().width
                / 2, p.getLocation().y + p.getSize().height / 2
                - getSize().height / 2);
    }

    public int getDensity() {
        return density;
    }

    public void actionPerformed(ActionEvent actionEvent) {
        if (actionEvent.getSource() == butOk) {
            density = Integer.parseInt((String)choDensity.getSelectedItem());
        }
        this.setVisible(false);
    }
}

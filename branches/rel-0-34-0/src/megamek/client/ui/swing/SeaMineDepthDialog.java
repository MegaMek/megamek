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
public class SeaMineDepthDialog extends JDialog implements ActionListener {
    /**
     * 
     */
    private static final long serialVersionUID = -7642956136536119067L;
    private GridBagLayout gridbag = new GridBagLayout();
    private GridBagConstraints c = new GridBagConstraints();
    private JButton butOk = new JButton(Messages.getString("Okay")); //$NON-NLS-1$
    private JLabel labDepth = new JLabel(Messages
            .getString("SeaMineDepthDialog.labDepth"), SwingConstants.RIGHT); //$NON-NLS-1$
    private JComboBox choDepth = new JComboBox();
    private int depth;
    //private JFrame frame;

    public SeaMineDepthDialog(JFrame p, int totalDepth) {
        super(p, Messages.getString("MineDensityDialog.title"), true); //$NON-NLS-1$
        super.setResizable(false);
        //frame = p;
        butOk.addActionListener(this);
        
        
        choDepth.removeAllItems();
        for(int i =0; i < (totalDepth + 1); i++) {
            choDepth.addItem(Integer.toString(i));
        }
        choDepth.setSelectedIndex(0);
        
        getContentPane().setLayout(gridbag);
        c.fill = GridBagConstraints.VERTICAL;
        c.insets = new Insets(1, 1, 1, 1);
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(labDepth, c);
        getContentPane().add(labDepth);
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 0.0;
        c.weighty = 0.0;
        gridbag.setConstraints(choDepth, c);
        getContentPane().add(choDepth);
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

    public int getDepth() {
        return depth;
    }

    public void actionPerformed(ActionEvent actionEvent) {
        if (actionEvent.getSource() == butOk) {
            depth = Integer.parseInt((String)choDepth.getSelectedItem());
        }
        this.setVisible(false);
    }
}

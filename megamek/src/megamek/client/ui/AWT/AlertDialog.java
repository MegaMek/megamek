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

package megamek.client;

import java.awt.*;
import java.awt.event.*;

/**
 * A (somewhat primitive) dialog with a message and an okay button that makes
 * the dialog go away.
 */
public class AlertDialog
    extends Dialog implements ActionListener
{
    private Button butOkay = new Button("Okay");
    private Label labMessage;
    
    public AlertDialog(Frame parent, String title, String message) {
        super(parent, title, true);
        
        labMessage = new Label(message, Label.CENTER);
        butOkay.addActionListener(this);
        
        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridbag);
            
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(10, 10, 10, 10);
        c.weightx = 1.0;    c.weighty = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(labMessage, c);
        add(labMessage);
            
        c.weightx = 1.0;    c.weighty = 1.0;
        c.fill = GridBagConstraints.VERTICAL;
        c.ipadx = 20;    c.ipady = 5;
        gridbag.setConstraints(butOkay, c);
        add(butOkay);
        
        addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent e) { setVisible(false); }
	});
        
        pack();
        setLocation(parent.getLocation().x + parent.getSize().width/2 - getSize().width/2,
                    parent.getLocation().y + parent.getSize().height/2 - getSize().height/2);
    }
    
    public void actionPerformed(ActionEvent e) {
        this.setVisible(false);
    }
}

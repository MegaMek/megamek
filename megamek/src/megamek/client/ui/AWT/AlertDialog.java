/**
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
        
        butOkay.addActionListener(this);
        labMessage = new Label(message, Label.CENTER);
        
        setLayout(new BorderLayout());
        
        add(labMessage, BorderLayout.CENTER);
        add(butOkay, BorderLayout.SOUTH);
        
        pack();
        setLocation(parent.getLocation().x + 150 , parent.getLocation().y + 150);
    }
    
	public void actionPerformed(ActionEvent e) {
        this.setVisible(false);
    }
}

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

import java.awt.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import megamek.client.Client;

public abstract class StatusBarPhaseDisplay extends AbstractPhaseDisplay implements ActionListener
{
    
  // displays
    private Label labStatus;
    protected Panel panStatus;

    /**
     * Sets up the status bar with toggle buttons for the mek display and map.
     */
    protected void setupStatusBar(String defStatus) {
      panStatus = new Panel();

      labStatus = new Label(defStatus, Label.CENTER);
      
      // layout
      GridBagLayout gridbag = new GridBagLayout();
      GridBagConstraints c = new GridBagConstraints();
      panStatus.setLayout(gridbag);
          
      c.insets = new Insets(0, 1, 0, 1);
      c.fill = GridBagConstraints.HORIZONTAL;

      c.gridwidth = GridBagConstraints.REMAINDER;
      c.weightx = 1.0;    c.weighty = 0.0;
      gridbag.setConstraints(labStatus, c);
      panStatus.add(labStatus);
    }
    
    protected void setStatusBarText(String text) {
      labStatus.setText(text);
    }
    
    protected boolean statusBarActionPerformed(ActionEvent ev, Client client) {
      return false;
    }
}


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

package megamek.client;

import java.awt.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public abstract class StatusBarPhaseDisplay extends AbstractPhaseDisplay implements ActionListener
{
	
  // displays
    private Label labStatus;
    protected Panel panStatus;
    private Button butDisplay;
    private Button butMap;
    private Button butLOS;

    /**
     * Sets up the status bar with toggle buttons for the mek display and map.
     */
    protected void setupStatusBar(String defStatus) {
      panStatus = new Panel();

      labStatus = new Label(defStatus, Label.CENTER);
      
      butDisplay = new Button("D");
      butDisplay.addActionListener(this);
      butDisplay.setActionCommand(VIEW_MEK_DISPLAY);
      
      butMap = new Button("M");
      butMap.addActionListener(this);
      butMap.setActionCommand(VIEW_MINI_MAP);
      
      butLOS = new Button("L");
      butLOS.addActionListener(this);
      butLOS.setActionCommand(VIEW_LOS_SETTING);

      // layout
      GridBagLayout gridbag = new GridBagLayout();
      GridBagConstraints c = new GridBagConstraints();
      panStatus.setLayout(gridbag);
          
      c.insets = new Insets(0, 1, 0, 1);
      c.fill = GridBagConstraints.HORIZONTAL;
      c.weightx = 1.0;    c.weighty = 0.0;
      gridbag.setConstraints(labStatus, c);
      panStatus.add(labStatus);
      
      c.weightx = 0.0;    c.weighty = 0.0;
      gridbag.setConstraints(butDisplay, c);
      panStatus.add(butDisplay);

	  panStatus.add(butMap);

      c.gridwidth = GridBagConstraints.REMAINDER;
      panStatus.add(butLOS);
    }
    
    protected void setStatusBarText(String text) {
      labStatus.setText(text);
    }
    
    protected void setDisplayButtonEnabled(boolean enabled) {
      butDisplay.setEnabled(enabled);
    }

    protected boolean statusBarActionPerformed(ActionEvent ev, Client client) {
      boolean handled = false;

      if (ev.getActionCommand().equals(VIEW_MEK_DISPLAY)) {
        client.toggleDisplay();
        handled = true;
      } else if (ev.getActionCommand().equals(VIEW_MINI_MAP)) {
        client.toggleMap();
        handled = true;
      } else if (ev.getActionCommand().equals(VIEW_LOS_SETTING)) {
        client.showLOSSettingDialog();
        handled = true;
      }
      
      return handled;
    }
}


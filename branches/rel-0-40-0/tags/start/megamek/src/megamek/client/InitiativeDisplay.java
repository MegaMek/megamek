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
import java.util.*;

import megamek.common.*;

public class InitiativeDisplay 
	extends Panel
	implements ActionListener, KeyListener
{
	// parent game
	private Client client;

	// buttons & such
  private Label			labInitiative;
	private Panel			panInitiative;
	private Panel			panMain;
	
	private Label			labStatus;

	private Button			butReady;
	
	public InitiativeDisplay(Client client) {
		this.client = client;
		
		// init AWT components
		labInitiative = new Label("Initiative Phase", Label.CENTER);

    panInitiative = new Panel();
    
    panMain = new Panel();
    panMain.setLayout(new GridBagLayout());
    panMain.add(panInitiative);
		
    labStatus = new Label("", Label.CENTER);
    
		butReady = new Button("Ready");
		butReady.setActionCommand("ready");
		butReady.addActionListener(this);
		
		
		addKeyListener(this);

		// layout screen
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		setLayout(gridbag);
		
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;	c.weighty = 1.0;
		c.insets = new Insets(1, 1, 1, 1);
		c.gridwidth = GridBagConstraints.REMAINDER;
		addBag(panMain, gridbag, c);

		c.weightx = 1.0;	c.weighty = 0.0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		addBag(labStatus, gridbag, c);

		c.gridwidth = 1;
		c.weightx = 1.0;	c.weighty = 0.0;
		addBag(client.cb.getComponent(), gridbag, c);

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 0.0;	c.weighty = 0.0;
		addBag(butReady, gridbag, c);

		fillInitPanel();
	}

	private void addBag(Component comp, GridBagLayout gridbag, GridBagConstraints c) {
		gridbag.setConstraints(comp, c);
		add(comp);
		comp.addKeyListener(this);
	}
	
	/**
	 * Shows initiative numbers and enables the ready button.
	 */
	public void go() {
		fillInitPanel();
		butReady.setEnabled(true);
	}
	
	/**
	 * Fills the initiative panel with data.
	 */
	public void fillInitPanel() {
		int nop = client.getNoOfPlayers();
		panInitiative.removeAll();
		panInitiative.setLayout(new GridLayout(2, 4));
		
		for(Enumeration e = client.getPlayers(); e.hasMoreElements();) {
			Player p = (Player)e.nextElement();
			if(p != null) {
				panInitiative.add(new Label(p.getName(), Label.CENTER));
			}
		}
		for(Enumeration e = client.getPlayers(); e.hasMoreElements();) {
			Player p = (Player)e.nextElement();
			if(p != null) {
				panInitiative.add(new Label(p.getInitiative() + "", Label.CENTER));
			}
		}
		
		panInitiative.validate();
		validate();
	}

	/**
	 * Sets you as ready and disables the ready button.
	 */
	public void ready() {
		client.sendReady(true);
		butReady.setEnabled(false);
	}
	
	//
	// KeyListener
	//
	public void keyPressed(KeyEvent ke) {
		if(ke.getKeyCode() == ke.VK_ENTER || ke.getKeyCode() == ke.VK_SPACE) {
			ready();
		}
	}
	public void keyReleased(KeyEvent ke) {
		;
	}
	public void keyTyped(KeyEvent ke) {
		;
	}

	//
	// ActionListener
	//
	public void actionPerformed(ActionEvent ev) {
		if(ev.getActionCommand().equalsIgnoreCase("ready")) {
			ready();
		}
	}
}

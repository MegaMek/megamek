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
import java.util.*;

/**
 * A (somewhat primitive) dialog that asks a question and lets the user confirm or cancel.
 * The question string is tokenised on "\n".
 * Based on AlertDialog and CustomMechDialog
 * @author  Dima.Nemchenko@lunarlollipop.co.uk
 * @version 1
 */
public class ConfirmDialog
	extends Dialog implements ActionListener
{
	private boolean confirm = false;

	private Panel panButtons = new Panel();
	private Button butOK = new Button("OK");
	private Button butCancel = new Button("Cancel");

	public ConfirmDialog(Frame parent, String title, String question) {
		super(parent, title, true);
		super.setResizable(false);

		GridBagLayout gridbag = new GridBagLayout();
		setLayout(gridbag);

		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = 1;
		c.gridheight = 1;
		c.gridx = 0;

		TextArea message = new TextArea(question, 5, 50, TextArea.SCROLLBARS_VERTICAL_ONLY);
		message.setEditable(false);

		c.gridy = 0;
		c.insets = new Insets(0, 5, 0, 5);
		add(message,c);

		setupButtons();
		c.gridy = 1;
		c.insets = new Insets(5, 5, 5, 5);
		add(panButtons,c);
        
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) { setVisible(false); }
		});
        
		pack();
		setLocation(parent.getLocation().x + parent.getSize().width/2 - getSize().width/2,
			parent.getLocation().y + parent.getSize().height/2 - getSize().height/2);
	};

	private void setupButtons() {
		butOK.addActionListener(this);
		butCancel.addActionListener(this);

		// layout
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		panButtons.setLayout(gridbag);

		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(10, 5, 5, 5);
		c.weightx = 1.0;    c.weighty = 1.0;
		c.fill = GridBagConstraints.VERTICAL;
		c.ipadx = 20;    c.ipady = 5;

		c.gridwidth = 1;
		gridbag.setConstraints(butOK, c);
		panButtons.add(butOK);

		c.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(butCancel, c);
		panButtons.add(butCancel);
	};

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == butOK) {
			confirm = true;
		} else {
			confirm = false;
		};
		this.setVisible(false);
	};

	public boolean getAnswer() {
		return confirm;
	};
}

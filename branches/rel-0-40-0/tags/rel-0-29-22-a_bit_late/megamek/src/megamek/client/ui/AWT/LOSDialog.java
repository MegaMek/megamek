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

import megamek.client.util.*;
import megamek.common.*;
import java.awt.*;
import java.awt.event.*;

// Allows the player to select the type of entity in the hexes used
// by the LOS tool.
public class LOSDialog
    extends Dialog implements ActionListener
{
    private Button butOK = new Button("OK");

    /**
     * The checkboxes for available choices.
     */
    private Checkbox[] checkboxes1 = null;
    private Checkbox[] checkboxes2 = null;

    public LOSDialog(Frame parent, boolean mechInFirst, boolean mechInSecond) {
        super(parent, "LOS tool settings", false);
        super.setResizable(false);

        // closing the window is the same as hitting butOK
        addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    actionPerformed(new ActionEvent(butOK,ActionEvent.ACTION_PERFORMED,butOK.getLabel()));
                };
        });

        GridBagLayout gridbag = new GridBagLayout();
        setLayout(gridbag);

        GridBagConstraints c = new GridBagConstraints();

        Label labMessage = new Label("In first hex:", Label.LEFT);
        c.weightx = 1.0;    c.weighty = 1.0;
        c.gridwidth = 0;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(labMessage, c);
        add(labMessage);
        
        CheckboxGroup radioGroup1 = new CheckboxGroup();
        checkboxes1 = new Checkbox[2];
        
		checkboxes1[0] = new Checkbox("Mech", mechInFirst, radioGroup1);
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.WEST;
		gridbag.setConstraints(checkboxes1[0], c);
		add(checkboxes1[0]);

		checkboxes1[1] = new Checkbox("Non-mech", !mechInFirst, radioGroup1);
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.anchor = GridBagConstraints.WEST;
		gridbag.setConstraints(checkboxes1[1], c);
		add(checkboxes1[1]);

        labMessage = new Label("In second hex:", Label.LEFT);
        c.weightx = 1.0;    c.weighty = 1.0;
        c.gridwidth = 0;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(labMessage, c);
        add(labMessage);
        
        CheckboxGroup radioGroup2 = new CheckboxGroup();
        checkboxes2 = new Checkbox[2];
        
		checkboxes2[0] = new Checkbox("Mech", mechInSecond, radioGroup2);
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.WEST;
		gridbag.setConstraints(checkboxes2[0], c);
		add(checkboxes2[0]);

		checkboxes2[1] = new Checkbox("Non-mech", !mechInSecond, radioGroup2);
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.anchor = GridBagConstraints.WEST;
		gridbag.setConstraints(checkboxes2[1], c);
		add(checkboxes2[1]);

        butOK.addActionListener(this);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(5, 0, 5, 0);
        c.anchor = GridBagConstraints.CENTER;
        gridbag.setConstraints(butOK, c);
        add(butOK);

        pack();

        setLocation(parent.getLocation().x + parent.getSize().width/2 - getSize().width/2,
                    parent.getLocation().y + parent.getSize().height/2 - getSize().height/2);

    }
    
    public void actionPerformed(ActionEvent e) {
        this.setVisible(false);
    }

    public boolean getMechInFirst() {
		return this.checkboxes1[0].getState() == true;
    }

    public boolean getMechInSecond() {
		return this.checkboxes2[0].getState() == true;
    }

    /** Shows the dialog, and allows focus to be set on the OK button, even though we're modal
    */
    public void show() {
        // quickly show the dialog non-modal, set the focus, and hide
        super.show();
        butOK.requestFocus();
        hide();

        // make the dialog modal and re-show (focus will still be set)
        setModal(true);
        super.show();
    }
} 
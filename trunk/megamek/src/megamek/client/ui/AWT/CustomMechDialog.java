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

/*
 * CustomMechDialog.java
 *
 * Created on March 18, 2002, 2:56 PM
 */

package megamek.client;

import java.awt.*;
import java.awt.event.*;

import megamek.common.*;

/**
 * A dialog that a player can use to customize his mech before battle.  
 * Currently, only changing pilots is supported.
 *
 * @author  Ben
 * @version 
 */
public class CustomMechDialog 
extends Dialog implements ActionListener {
    
    private Label labInstructions = new Label("Edit your pilot's stats here.");
    
    private Label labName = new Label("Name: ", Label.RIGHT);
    private TextField fldName = new TextField(20);
    private Label labGunnery = new Label("Gunnery: ", Label.RIGHT);;
    private TextField fldGunnery = new TextField(3);
    private Label labPiloting = new Label("Piloting: ", Label.RIGHT);;
    private TextField fldPiloting = new TextField(3);
    
    private Panel panButtons = new Panel();
    private Button butOkay = new Button("Okay");
    private Button butCancel = new Button("Cancel");
    
    private Entity entity;
    private boolean okay = false;
    private Frame frame;

    /** Creates new CustomMechDialog */
    public CustomMechDialog(Frame frame, Entity entity, boolean editable) {
        super(frame, "Customize pilot stats...", true);
        
        this.entity = entity;
        this.frame = frame;
        
        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridbag);
            
        c.fill = GridBagConstraints.VERTICAL;
        c.insets = new Insets(5, 5, 5, 5);
//        c.weightx = 1.0;    c.weighty = 0.0;
//        c.gridwidth = GridBagConstraints.REMAINDER;
//        gridbag.setConstraints(labInstructions, c);
//        add(labInstructions);
            
        c.weightx = 1.0;    c.weighty = 1.0;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(labName, c);
        add(labName);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(fldName, c);
        add(fldName);
        
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(labGunnery, c);
        add(labGunnery);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(fldGunnery, c);
        add(fldGunnery);
        
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(labPiloting, c);
        add(labPiloting);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(fldPiloting, c);
        add(fldPiloting);
        
        setupButtons();
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.CENTER;
        gridbag.setConstraints(panButtons, c);
        add(panButtons);
        
        fldName.setText(entity.getCrew().getName());
        fldGunnery.setText(new Integer(entity.getCrew().getGunnery()).toString());
        fldPiloting.setText(new Integer(entity.getCrew().getPiloting()).toString());
        
        if (!editable) {
            fldName.setEnabled(false);
            fldGunnery.setEnabled(false);
            fldPiloting.setEnabled(false);
        }
        
        addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent e) { setVisible(false); }
	});

        pack();
        setResizable(false);
        setLocation(frame.getLocation().x + frame.getSize().width/2 - getSize().width/2,
                    frame.getLocation().y + frame.getSize().height/2 - getSize().height/2);
    }
    
    private void setupButtons() {
        butOkay.addActionListener(this);
        butCancel.addActionListener(this);
        
        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panButtons.setLayout(gridbag);
            
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(5, 5, 0, 0);
        c.weightx = 1.0;    c.weighty = 1.0;
        c.fill = GridBagConstraints.VERTICAL;
        c.ipadx = 20;    c.ipady = 5;
        c.gridwidth = 1;
        gridbag.setConstraints(butOkay, c);
        panButtons.add(butOkay);
            
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(butCancel, c);
        panButtons.add(butCancel);
    }
    
    public boolean isOkay() {
        return okay;
    }

    public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
        if (actionEvent.getSource() == butOkay) {
            // get values
            String name = fldName.getText();
            int gunnery;
            int piloting;
            try {
                gunnery = Integer.parseInt(fldGunnery.getText());
                piloting =  Integer.parseInt(fldPiloting.getText());
            } catch (NumberFormatException e) {
                new AlertDialog(frame, "Number Format Error", "Please enter valid numbers for the skill values.").show();
                return;
            }
            
            // keep these reasonable, please
            if (gunnery < 0 || gunnery > 7 || piloting < 0 || piloting > 7) {
                new AlertDialog(frame, "Number Format Error", "Please enter values between 0 and 7 for the skill values.").show();
                return;
            }
            
            // change entity
            entity.crew = new Pilot(name, gunnery, piloting);
            okay = true;
        }
        
        this.setVisible(false);
    }
    
}

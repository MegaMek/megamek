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
import java.io.*;
import java.util.*;

import megamek.common.*;

public class ReportDisplay 
    extends AbstractPhaseDisplay
    implements ActionListener, KeyListener
{
    // parent game
    public Client client;
    
    // chatterbox keeps track of chatting and other messages
    private ChatterBox        cb;
    
    // displays
    private TextArea        rta;
    private Label            statusL;
    
    private Window            mechw;
    private MechDisplay        mechd;
    private    boolean            mechdOn;
    
    // buttons
    private Button            readyB;
    
    // let's keep track of what we're moving, too
    private int                cen;    // current entity number
    private MovementData    md;        // movement data
    private MovementData    cmd;    // considering movement data
    
    /**
     * Creates and lays out a new movement phase display 
     * for the specified client.
     */
    public ReportDisplay(Client client) {
        this.client = client;
        
        cb = client.cb;
        
        rta = new TextArea(client.eotr, 40, 25, TextArea.SCROLLBARS_VERTICAL_ONLY);
        rta.setEditable(false);
        
        statusL = new Label("", Label.CENTER);
        
        readyB = new Button("Done");
        readyB.setActionCommand("ready");
        readyB.addActionListener(this);
        
        // layout screen
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridbag);
        
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;    c.weighty = 1.0;
        c.insets = new Insets(1, 1, 1, 1);
        c.gridwidth = GridBagConstraints.REMAINDER;
        addBag(rta, gridbag, c);

        c.weightx = 1.0;    c.weighty = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        addBag(statusL, gridbag, c);

        c.gridwidth = 1;
        c.weightx = 1.0;    c.weighty = 0.0;
        addBag(cb.getComponent(), gridbag, c);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 0.0;    c.weighty = 0.0;
        addBag(readyB, gridbag, c);

    addKeyListener(this);
        
    }
    
    private void addBag(Component comp, GridBagLayout gridbag, GridBagConstraints c) {
        gridbag.setConstraints(comp, c);
        add(comp);
        comp.addKeyListener(this);
    }
    
    /**
     * Sets you as ready and disables the ready button.
     */
    public void ready() {
        readyB.setEnabled(false);
        client.sendDone(true);
    }
    
    /**
     * Refreshes the report
     */
    public void refresh() {
        rta.setText(client.eotr);
    }
    
    //
    // ActionListener
    //
    public void actionPerformed(ActionEvent ev) {
        if(ev.getActionCommand().equalsIgnoreCase("ready")) {
            ready();
        }
    }
    

    //
    // KeyListener
    //
    public void keyPressed(KeyEvent ev) {
        if(ev.getKeyCode() == ev.VK_ESCAPE) {
        }
        if(ev.getKeyCode() == ev.VK_ENTER && ev.isControlDown()) {
            ready();
        }
    }
    public void keyReleased(KeyEvent ev) {
        ;
    }
    public void keyTyped(KeyEvent ev) {
        ;
    }
    

}

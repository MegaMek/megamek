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

package megamek.client.ui.swing;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

import megamek.client.Client;
import megamek.client.ui.swing.widget.MegamekButton;

/**
 * This is a parent class for the button display for each phase.  Every phase 
 * has a panel of control buttons along with a done button.  This class formats
 * the button panel, the done button, and a status display area. 
 * 
 * Control buttons are grouped and the groups can be cycled through.
 *
 */
public abstract class StatusBarPhaseDisplay extends AbstractPhaseDisplay
        implements ActionListener, KeyListener {

    private static final long serialVersionUID = 639696875125581395L;
    
    protected static final int TRANSPARENT = 0xFFFF00FF;
    
    // displays
    private JLabel labStatus;
    protected JPanel panStatus;
    protected JPanel panButtons;  
    
    // Variables that determine the layout of the button panel
    // Keeps track of which group of buttons we are displaying
    protected int currentButtonGroup = 0;
    // How many different button groups there are, needs to be computed in a 
    //   child class
    protected int numButtonGroups;
    // How any buttons are in each group
    protected int buttonsPerGroup = 10;
    protected int buttonsPerRow = 5;
    

    protected StatusBarPhaseDisplay() {
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
        		KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0), "clearButton");

        getActionMap().put("clearButton", new AbstractAction() {
            private static final long serialVersionUID = -7781405756822535409L;

            public void actionPerformed(ActionEvent e) {
                if (isIgnoringEvents()) {
                    return;
                }
                if (clientgui.cb2.hasFocus){
                	clientgui.cb2.hasFocus = false;
                	clientgui.cb2.clearMessage();
                } else if (clientgui.getClient().isMyTurn()) {
                    clear();
                }
            }
        });
        
        panButtons = new JPanel();      
        panButtons.setOpaque(false);        
        panButtons.setLayout(new GridBagLayout());
    }
    
    
    /**
     * This method will return the list of buttons that should be displayed.
     * @return
     */
    protected abstract ArrayList<MegamekButton> getButtonList();

    /**
     * Adds the buttons and status bar to the panel.    
     */
    protected void layoutScreen(){
    	GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridbag);
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(1, 1, 1, 1);
        c.weightx = 1.0;
        c.weighty = 0.0;
        
        c.gridy = 0;
        addBag(panButtons, gridbag, c);
        c.gridy = 1;
        addBag(panStatus, gridbag, c);    	
    }
    
    /**
     * Adds buttons to the button panel.  The buttons to be added are retrieved
     * with the <code>getButtonList()<code> method.  The number of buttons to
     * display is defined in <code>buttonsPerGroup</code> and which group of
     * buttons will be displayed is set by <code>currentButtonGroup</code>.
     */
    protected void setupButtonPanel() {
        panButtons.removeAll();
        panButtons.setLayout(new GridBagLayout());
        
        int numRows = buttonsPerGroup/buttonsPerRow;
        
        JPanel subPanel = new JPanel();
        subPanel.setOpaque(false);
        subPanel.setLayout(new GridLayout(numRows,buttonsPerRow));
        ArrayList<MegamekButton> buttonList = getButtonList();
                
        // We may skip the current button group if all of its buttons are 
        //  disabled
        boolean ok = false;
        while (!ok && (currentButtonGroup != 0)) {
            for (int i = currentButtonGroup * buttonsPerGroup; 
            		(i < ((currentButtonGroup + 1) * buttonsPerGroup))
                    	&& (i < buttonList.size()); i++) {
                if (buttonList.get(i) != null && buttonList.get(i).isEnabled()){
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                // skip as nothing was enabled
            	currentButtonGroup++;
                if ((currentButtonGroup * buttonsPerGroup) >= 
                		buttonList.size()) {
                	currentButtonGroup = 0;
                }
            }
        }
        int i = 0;
        for (i = currentButtonGroup * buttonsPerGroup; 
        		(i < ((currentButtonGroup + 1) * buttonsPerGroup))
                	&& (i < buttonList.size()); i++) {        
            if (buttonList.get(i) != null){
            	subPanel.add(buttonList.get(i));              
            } else {
            	subPanel.add(Box.createHorizontalGlue());
            }
        }           
        while ( i < ((currentButtonGroup + 1) * buttonsPerGroup)){
        	subPanel.add(Box.createHorizontalGlue());
        	i++;
        }
           
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.WEST;
        
        c.gridx = c.gridy = 0;
        c.weightx = 1;        
        panButtons.add(subPanel,c);
        
        c.anchor = GridBagConstraints.EAST;
        c.gridx = 1;
        c.weightx = 0;
        c.gridx = 1;
        panButtons.add(butDone,c);        
        butDone.setSize(DONE_BUTTON_WIDTH,butDone.getHeight());
    	butDone.setPreferredSize(butDone.getSize());
    	butDone.setMinimumSize(butDone.getSize());
    	
        panButtons.validate();
        panButtons.repaint();   
    }
    
    protected void addBag(JComponent comp, GridBagLayout gridbag,
            GridBagConstraints c) {
        gridbag.setConstraints(comp, c);
        add(comp);
        comp.addKeyListener(this);
    }

    
    /**
     * clears the actions of this phase
     */
    public abstract void clear();

    
    /**
     * Sets up the status bar with toggle buttons for the mek display and map.
     */
    protected void setupStatusBar(String defStatus) {
        panStatus = new JPanel();
        panStatus.setOpaque(false);
        labStatus = new JLabel(defStatus, SwingConstants.CENTER);
        labStatus.setForeground(Color.yellow);
        labStatus.setOpaque(false);

        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panStatus.setLayout(gridbag);

        c.insets = new Insets(0, 1, 0, 1);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1.0;
        c.weighty = 0.0;
        gridbag.setConstraints(labStatus, c);
        panStatus.add(labStatus);
    }

    protected void setStatusBarText(String text) {
        labStatus.setText(text);
    }

    protected boolean statusBarActionPerformed(ActionEvent ev, Client client) {
        return false;
    }
    
	@Override
	public void keyPressed(KeyEvent evt) {	
	}

	@Override
	public void keyReleased(KeyEvent evt) {
	}

	@Override
	public void keyTyped(KeyEvent evt) {			
	}
}

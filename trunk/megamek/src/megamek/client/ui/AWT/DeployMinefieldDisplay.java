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
import java.io.*;
import java.util.*;

import megamek.common.*;

public class DeployMinefieldDisplay 
    extends StatusBarPhaseDisplay
    implements BoardListener,  ActionListener,
    KeyListener, GameListener
{    
	// Action command names
	public static final String DEPLOY_MINE_CONV        = "deployMineConv";
	public static final String DEPLOY_MINE_COM         = "deployMineCom";
	public static final String DEPLOY_MINE_VIBRA       = "deployMineVibra";

    // parent game
    public Client client;
    
    // buttons
    private Panel             panButtons;
    
    private Button            butC;
    private Button            butM;
    private Button            butSpace;
    private Button              butV;
    private Button              butUnload;
    private Button            butDone;

    private int                cen;    // current entity number

    // is the shift key held?
    private boolean            turnMode = false;
    private boolean				deployM = false;
    private boolean				deployC = false;
    private boolean				deployV = false;

	private Player p;
	private Vector deployedMinefields = new Vector();

    /**
     * Creates and lays out a new deployment phase display 
     * for the specified client.
     */
    public DeployMinefieldDisplay(Client client) {
        this.client = client;
        client.addGameListener(this);

        client.game.board.addBoardListener(this);

        setupStatusBar("Waiting to begin Deploy minefield phase...");

		p = client.getLocalPlayer();
		
        butM = new Button("Minefield(" + p.getNbrMFConventional() + ")");
        butM.addActionListener(this);
        butM.setActionCommand(DEPLOY_MINE_CONV);
        butM.setEnabled(false);
                        
        butSpace = new Button(".");
        butSpace.setEnabled(false);

        butC = new Button("Command(" + p.getNbrMFCommand() + ")");
        butC.addActionListener(this);
        butC.setActionCommand(DEPLOY_MINE_COM);
        butC.setEnabled(false);

        butUnload = new Button(".");
        butUnload.addActionListener(this);
        butUnload.setEnabled(false);

        butV = new Button("Vibrabomb(" + p.getNbrMFVibra() + ")");
        butV.addActionListener(this);
        butV.setActionCommand(DEPLOY_MINE_VIBRA);
        butV.setEnabled(false);

        butDone = new Button("Done");
        butDone.addActionListener(this);
        butDone.setEnabled(false);

        // layout button grid
        panButtons = new Panel();
        panButtons.setLayout(new GridLayout(1, 4));
        panButtons.add(butM);
        panButtons.add(butC);
        panButtons.add(butV);
        panButtons.add(butDone);

        // layout screen
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridbag);

        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;    c.weighty = 1.0;
        c.insets = new Insets(1, 1, 1, 1);
        c.gridwidth = GridBagConstraints.REMAINDER;
        addBag(client.bv, gridbag, c);

        c.weightx = 1.0;    c.weighty = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        addBag(panStatus, gridbag, c);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 0.0;    c.weighty = 0.0;
        c.ipady = 20;
        addBag(panButtons, gridbag, c);

        addKeyListener(this);
    }
    
    private void addBag(Component comp, GridBagLayout gridbag, GridBagConstraints c) {
        gridbag.setConstraints(comp, c);
        add(comp);
        comp.addKeyListener(this);
    }

    /**
     * Enables relevant buttons and sets up for your turn.
     */
    private void beginMyTurn() {
		setConventionalEnabled(p.getNbrMFConventional());
		setCommandEnabled(p.getNbrMFCommand());
		setVibrabombEnabled(p.getNbrMFVibra());
    }

    /**
     * Clears out old deployment data and disables relevant buttons.
     */
    private void endMyTurn() {
        // end my turn, then.
        disableButtons();
        client.game.board.select(null);
        client.game.board.highlight(null);
        client.game.board.cursor(null);

    }

    /**
     * Disables all buttons in the interface
     */
    private void disableButtons() {
		setConventionalEnabled(0);
		setCommandEnabled(0);
		setVibrabombEnabled(0);

        butDone.setEnabled(false);
        butUnload.setEnabled(false);
    }

    private void deployMinefield(Coords coords) {
    	if (!client.game.board.contains(coords)) {
    		return;
    	}
    	
    	if (client.game.containsMinefield(coords)) {
    		Minefield mf = (Minefield) client.game.getMinefields(coords).elementAt(0);
    		if (mf.getPlayerId() == client.getLocalPlayer().getId()) {
		        butDone.setEnabled(false);
	    		client.game.removeMinefield(mf);
	    		deployedMinefields.removeElement(mf);
	    		switch (mf.getType()) {
	    			case (Minefield.TYPE_CONVENTIONAL) :
		        	deployM = true;
		        	deployC = false;
		        	deployV = false;
	    			p.setNbrMFConventional(p.getNbrMFConventional() + 1);
	    			break;
	    			case (Minefield.TYPE_COMMAND_DETONATED) :
		        	deployM = false;
		        	deployC = true;
		        	deployV = false;
	    			p.setNbrMFCommand(p.getNbrMFCommand() + 1);
	    			break;
	    			case (Minefield.TYPE_VIBRABOMB) :
		        	deployM = false;
		        	deployC = false;
		        	deployV = true;
	    			p.setNbrMFVibra(p.getNbrMFVibra() + 1);
	    			break;
	    		}
	    	}
    	} else {
    		Minefield mf;
    		
    		if (deployM) {
    			mf = Minefield.createConventionalMF(coords, p.getId());
    			p.setNbrMFConventional(p.getNbrMFConventional() - 1);
    		} else if (deployC) {
    			mf = Minefield.createCommandDetonatedMF(coords, p.getId());
    			p.setNbrMFCommand(p.getNbrMFCommand() - 1);
    		} else if (deployV) {
    			VibrabombSettingDialog vsd  = new VibrabombSettingDialog(client.frame);
    			vsd.show();

				// Hack warning...    			
    			client.bv.stopScrolling();
    			mf = Minefield.createVibrabombMF(coords, p.getId(), vsd.getSetting());
    			p.setNbrMFVibra(p.getNbrMFVibra() - 1);
    		} else {
    			return;
    		}
    		client.game.addMinefield(mf);
    		deployedMinefields.addElement(mf);
    		client.bv.update(client.bv.getGraphics());
    	}
    	
    	if (p.getNbrMFConventional() == 0 &&
    		p.getNbrMFCommand() == 0 &&
    		p.getNbrMFVibra() == 0) {
    		butDone.setEnabled(true);
    	}

		setConventionalEnabled(p.getNbrMFConventional());
		setCommandEnabled(p.getNbrMFCommand());
		setVibrabombEnabled(p.getNbrMFVibra());

    	if (p.getNbrMFConventional() == 0) {
    		deployM = false;
    	}
    	if (p.getNbrMFCommand() == 0) {
    		deployC = false;
	     }
    	if (p.getNbrMFVibra() == 0) {
    		deployV = false;
        }

    }


    //
    // BoardListener
    //
    public void boardHexMoused(BoardEvent b) {
        if (b.getType() != BoardEvent.BOARD_HEX_DRAGGED) {
            return;
        }
        
        // ignore buttons other than 1
        if (!client.isMyTurn() || (b.getModifiers() & MouseEvent.BUTTON1_MASK) == 0) {
            return;
        }

        // check for shifty goodness
        boolean shiftheld = (b.getModifiers() & MouseEvent.SHIFT_MASK) != 0;
        
        // check for a deployment
        client.game.board.select(b.getCoords());
        deployMinefield(b.getCoords());
    }

    //
    // GameListener
    //
    public void gameTurnChange(GameEvent ev) {
        endMyTurn();

        if (client.isMyTurn()) {
            beginMyTurn();
            setStatusBarText("It's your turn to deploy minefields.");
        } else {
            setStatusBarText("It's " + ev.getPlayer().getName() + 
                    "'s turn to deploy minefields.");
        }
    }

    public void gamePhaseChange(GameEvent ev) {
        if (client.game.getPhase() != Game.PHASE_DEPLOY_MINEFIELDS) {
            if (client.isMyTurn()) {
                endMyTurn();
            }
            client.removeGameListener(this);
            client.game.board.removeBoardListener(this);
            client.bv.removeKeyListener(this);
            client.cb.getComponent().removeKeyListener(this);
        }
    }

    //
    // ActionListener
    //
    public void actionPerformed(ActionEvent ev) {
        if ( statusBarActionPerformed(ev, client) )
          return;
          
        if (!client.isMyTurn()) {
            // odd...
            return;
        }
        
        if (ev.getSource().equals(butDone)) {
        	endMyTurn();
        	client.sendDeployMinefields(deployedMinefields);
			client.sendPlayerInfo();
        }
        if (ev.getActionCommand().equals(DEPLOY_MINE_CONV)) {
        	deployM = true;
        	deployC = false;
        	deployV = false;
        }
        if (ev.getActionCommand().equals(DEPLOY_MINE_COM)) {
        	deployM = false;
        	deployC = true;
        	deployV = false;
        }
        if (ev.getActionCommand().equals(DEPLOY_MINE_VIBRA)) {
        	deployM = false;
        	deployC = false;
        	deployV = true;
        }
    } // End public void actionPerformed(ActionEvent ev)
    

    //
    // KeyListener
    //
    public void keyPressed(KeyEvent ev) {
    }

    public void keyReleased(KeyEvent ev) {
    }

    public void keyTyped(KeyEvent ev) {
        ;
    }

	private void setConventionalEnabled(int nbr) {
        butM.setLabel("Minefield(" + nbr + ")");
       	butM.setEnabled(nbr > 0);
        client.getMenuBar().setDeployConventionalEnabled(nbr);
	}
	private void setCommandEnabled(int nbr) {
        butC.setLabel("Command(" + nbr + ")");
       	butC.setEnabled(nbr > 0);
        client.getMenuBar().setDeployCommandEnabled(nbr);
	}
	private void setVibrabombEnabled(int nbr) {
        butV.setLabel("Vibrabomb(" + nbr + ")");
       	butV.setEnabled(nbr > 0);
        client.getMenuBar().setDeployVibrabombEnabled(nbr);
	}
}

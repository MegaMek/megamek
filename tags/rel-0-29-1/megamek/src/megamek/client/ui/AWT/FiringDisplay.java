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
import megamek.common.actions.*;

public class FiringDisplay 
    extends AbstractPhaseDisplay
    implements BoardListener, GameListener, ActionListener,
    KeyListener, ItemListener
{
    private static final int    NUM_BUTTON_LAYOUTS = 2;
    
    // parent game
    public Client client;
    
    // displays
    private Label             labStatus;
    private Panel             panStatus;
    private Button            butDisplay;
    private Button            butMap;
    
    // buttons
    private Container        panButtons;
    
    private Button            butFire;
    private Button            butTwist;
    private Button            butSkip;
    
    private Button            butAim;
    private Button            butFindClub;
    private Button            butNextTarg;
    private Button            butFlipArms;
    
    private Button            butReport;
    private Button            butSpace;
    private Button            butFireMode; // Fire Mode - Add a Fire Mode Button - Rasia
    
    private Button            butNext;
    private Button            butDone;
    private Button            butMore;
    
    private int               buttonLayout;

    // let's keep track of what we're shooting and at what, too
    private int                cen;        // current entity number
    private Targetable         target;        // target 
  
    // shots we have so far.
    private Vector attacks;  
  
    // is the shift key held?
    private boolean            shiftheld;
    private boolean            twisting;

    private Entity[]            visibleTargets  = null;
    private int                 lastTargetID    = -1;

    /**
     * Creates and lays out a new firing phase display 
     * for the specified client.
     */
    public FiringDisplay(Client client) {
        this.client = client;
        client.addGameListener(this);
        
        client.game.board.addBoardListener(this);
        
        shiftheld = false;
    
        // fire
        attacks = new Vector();

        setupStatusBar();
        
        butFire = new Button("Fire");
        butFire.addActionListener(this);
        butFire.setEnabled(false);
        
        butSkip = new Button("Skip");
        butSkip.addActionListener(this);
        butSkip.setEnabled(false);
        
        butTwist = new Button("Twist");
        butTwist.addActionListener(this);
        butTwist.setEnabled(false);
        

        butFindClub = new Button("Find Club");
        butFindClub.addActionListener(this);
        butFindClub.setEnabled(false);
        
        butAim = new Button("Aim");
        butAim.addActionListener(this);
        butAim.setEnabled(false);
        
        butNextTarg = new Button("Next Target");
        butNextTarg.addActionListener(this);
        butNextTarg.setEnabled(false);
        
        butFlipArms = new Button("Flip Arms");
        butFlipArms.addActionListener(this);
        butFlipArms.setEnabled(false);
        
        butReport = new Button("Report..");
        butReport.addActionListener(this);
        butReport.setEnabled(true);
        
        butSpace = new Button(".");
        butSpace.setEnabled(false);

        // Fire Mode - Adding a Fire Mode Button to the 2nd Menu - Rasia
        butFireMode = new Button("Mode");
        butFireMode.addActionListener(this);
        butFireMode.setEnabled(false);

        butDone = new Button("Done");
        butDone.addActionListener(this);
        butDone.setEnabled(false);
        
        butNext = new Button(" Next Unit ");
        butNext.addActionListener(this);
        butNext.setEnabled(false);
        
        butMore = new Button("More...");
        butMore.addActionListener(this);
        butMore.setEnabled(false);
        
        // layout button grid
        panButtons = new Panel();
        buttonLayout = 0;
        setupButtonPanel();
        
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

        c.gridwidth = 1;
        c.weightx = 1.0;    c.weighty = 0.0;
        addBag(client.cb.getComponent(), gridbag, c);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 0.0;    c.weighty = 0.0;
        addBag(panButtons, gridbag, c);
        
        addKeyListener(this);
        
        // mech display.
        client.mechD.wPan.weaponList.addItemListener(this);
        client.mechD.wPan.weaponList.addKeyListener(this);
    
    }
    
    private void addBag(Component comp, GridBagLayout gridbag, GridBagConstraints c) {
        gridbag.setConstraints(comp, c);
        add(comp);
        comp.addKeyListener(this);
    }
    
    /**
     * Sets up the status bar with toggle buttons for the mek display and map.
     * TODO: remove copy/pastiness with deploy, move, fire & phys panels
     */
    private void setupStatusBar() {
        panStatus = new Panel();

        labStatus = new Label("Waiting to begin Firing phase...", Label.CENTER);
        
        butDisplay = new Button("D");
        butDisplay.addActionListener(this);
        
        butMap = new Button("M");
        butMap.addActionListener(this);
        
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
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        panStatus.add(butMap);
    }

    private void setupButtonPanel() {
        panButtons.removeAll();
        panButtons.setLayout(new GridLayout(2, 4));
        
        switch (buttonLayout) {
        case 0 :
            panButtons.add(butFire);
            panButtons.add(butSkip);
            panButtons.add(butNextTarg);
            panButtons.add(butNext);
            panButtons.add(butTwist);
            panButtons.add(butReport);
            panButtons.add(butMore);
            panButtons.add(butDone);
            break;
        case 1 :
            panButtons.add(butFire);
            panButtons.add(butFindClub);
            panButtons.add(butFlipArms);
            panButtons.add(butNext);
            panButtons.add(butAim);
            /* 2003-03-30 Suvarov454: I'd rather see the "Fire"
            **            button on this panel than a spaceholder.
            panButtons.add(butSpace);
            */
            panButtons.add(butFireMode);
            panButtons.add(butMore);
            panButtons.add(butDone);
            break;
        }
        
        validate();
    }
    /**
     * Selects an entity, by number, for movement.
     */
    public void selectEntity(int en) {
        // clear any previously considered attacks
        if (en != cen) {
            clearAttacks();
            refreshAll();
        }

        if (client.game.getEntity(en) != null) {
            this.cen = en;

            // If the selected entity is not on the board, use the next one.
            // ASSUMPTION: there will always be *at least one* entity on map.
            if ( null == ce().getPosition() ) {

		// Walk through the list of entities for this player.
		for ( int nextId = client.getNextEntityNum(en);
		      nextId != en;
		      nextId = client.getNextEntityNum(nextId) ) {

		    if (null != client.game.getEntity(nextId).getPosition()) {
			this.cen = nextId;
			break;
		    }

                } // Check the player's next entity.

                // We were *supposed* to have found an on-board entity.
                if ( null == ce().getPosition() ) {
                    System.err.println
                        ("FiringDisplay: could not find an on-board entity: " +
                         en);
                    return;
                }

            } // End ce()-not-on-board

            target(null);
            client.game.board.highlight(ce().getPosition());
            client.game.board.select(null);
            client.game.board.cursor(null);

            refreshAll();
            cacheVisibleTargets();

            client.bv.centerOnHex(ce().getPosition());
            
            butTwist.setEnabled(ce().canChangeSecondaryFacing());
            butFindClub.setEnabled(Compute.canMechFindClub(client.game, en));
            butFlipArms.setEnabled(ce().canFlipArms());
        } else {
            System.err.println("FiringDisplay: tried to select non-existant entity: " + en);
        }
    }
    
    /**
     * Does turn start stuff
     */
    private void beginMyTurn() {
        target = null;
        butNext.setEnabled(true);
        butDone.setEnabled(true);
        butMore.setEnabled(true);
        butFireMode.setEnabled(true); // Fire Mode - Setting Fire Mode to true, currently doesn't detect if weapon has a special Fire Mode or not- Rasia        client.setDisplayVisible(true);
        client.setDisplayVisible(true);
        client.game.board.select(null);
        client.game.board.highlight(null);
        selectEntity(client.getFirstEntityNum());
    }
    
    /**
     * Does end turn stuff.
     */
    private void endMyTurn() {
        // end my turn, then.
        cen = Entity.NONE;
        target(null);
        client.game.board.select(null);
        client.game.board.highlight(null);
        client.game.board.cursor(null);
        client.setDisplayVisible(false);
        client.bv.clearMovementData();
        disableButtons();
        
        clearVisibleTargets();
    }
    
    /**
     * Disables all buttons in the interface
     */
    private void disableButtons() {
        butFire.setEnabled(false);
        butSkip.setEnabled(false);
        butTwist.setEnabled(false);
        butAim.setEnabled(false);
        butFindClub.setEnabled(false);
        butMore.setEnabled(false);
        butNext.setEnabled(false);
        butDone.setEnabled(false);
        butNextTarg.setEnabled(false);
        butFlipArms.setEnabled(false);
        butFireMode.setEnabled(false); // Fire Mode - Handlng of Fire Mode Button - Rasia
    }
    
   /**
    * Fire Mode - Adds a Fire Mode Change to the current Attack Action
    */
    private void changeMode() {
        int wn = client.mechD.wPan.getSelectedWeaponNum();
        Mounted m = ce().getEquipment(wn);
        int nMode = m.switchMode();
        
        // send change to the server
        client.sendModeChange(ce().getId(), wn, nMode);
        
        // notify the player
        if (m.getType().hasInstantModeSwitch()) {
            client.cb.systemMessage("Switched " + m.getName() + " to " + m.curMode());
        }
        else {
            client.cb.systemMessage(m.getName() + " will switch to " + m.pendingMode() + 
                    " at end of turn.");
        }

        this.updateTarget();
        client.mechD.wPan.displayMech(ce());
        client.mechD.wPan.selectWeapon(wn);
    }

   /**
     * Cache the list of visible targets. This is used for the 'next target' button.
     *
     * We'll sort it by range to us.
     */
      private void cacheVisibleTargets() {
        clearVisibleTargets();
        
        Vector vec = client.game.getValidTargets( ce() );
        com.sun.java.util.collections.Comparator sortComp = new com.sun.java.util.collections.Comparator() {
          public int compare(java.lang.Object x, java.lang.Object y) {
            Entity entX = (Entity)x;
            Entity entY = (Entity)y;
        
            int rangeToX = ce().getPosition().distance(entX.getPosition());
            int rangeToY = ce().getPosition().distance(entY.getPosition());
        
            if ( rangeToX == rangeToY ) return ((entX.getId() < entY.getId()) ? -1 : 1);
          
            return ((rangeToX < rangeToY) ? -1 : 1);
          }
        };
          
        com.sun.java.util.collections.TreeSet tree = new com.sun.java.util.collections.TreeSet(sortComp);
        visibleTargets = new Entity[vec.size()];
              
        for ( int i = 0; i < vec.size(); i++ ) {
          tree.add((Entity)vec.elementAt(i));
          }
        
        com.sun.java.util.collections.Iterator it = tree.iterator();
        int count = 0;
        while ( it.hasNext() ) {
          visibleTargets[count++] = (Entity)it.next();
        }

        butNextTarg.setEnabled(visibleTargets.length > 0);
      }

      private void clearVisibleTargets() {
        visibleTargets = null;
        lastTargetID = -1;
        butNextTarg.setEnabled(false);
      }
      
    /**
     * Get the next target. Return null if we don't have any targets.
     */
      private Entity getNextTarget() {
        if ( null == visibleTargets )
          return null;
        
        lastTargetID++;
        
        if ( lastTargetID >= visibleTargets.length )
          lastTargetID = 0;
        
        return (Entity)visibleTargets[lastTargetID];
      }
    
    /**
     * Jump to our next target. If there isn't one, well, don't do anything.
     */
      private void jumpToNextTarget() {
        Entity targ = getNextTarget();
        
        if ( null == targ )
          return;

        client.bv.centerOnHex(targ.getPosition());
        client.game.board.select(targ.getPosition());
        target(targ);
    }
    
    /**
     * Called when the current entity is done firing.  Send out our attack
     * queue to the server.
     */
    private void ready() {
        // stop further input (hopefully)
        disableButtons();

        // remove temporary attacks from game & board
        removeTempAttacks();
        
        // send out attacks
        client.sendAttackData(cen, attacks);
        
        // clear queue
        attacks.removeAllElements();
    }
    
    /**
     * Adds a weapon attack with the currently selected weapon to the attack
     * queue.
     */
    private void fire() {
        // get the sepected weaponnum
        int weaponNum = client.mechD.wPan.getSelectedWeaponNum();
        Mounted mounted = ce().getEquipment(weaponNum);
        
        // validate
        if (ce() == null || target == null || mounted == null 
        || !(mounted.getType() instanceof WeaponType)) {
            throw new IllegalArgumentException("current fire parameters are invalid");
        }
        
        WeaponAttackAction waa = new WeaponAttackAction(cen, target.getTargetType(), 
                target.getTargetId(), weaponNum);
        
        if (((WeaponType)mounted.getType()).getAmmoType() != AmmoType.T_NA) {
            waa.setAmmoId(ce().getEquipmentNum(mounted.getLinked()));
        }
    
        // add the attack to our temporary queue
        attacks.addElement(waa);
        
        // and add it into the game, temporarily
        client.game.addAction(waa);
        client.bv.addAttack(waa);
        client.bv.repaint(100);
        
        // set the weapon as used
        mounted.setUsedThisRound(true);
        
        // find the next available weapon 
        int nextWeapon = ce().getNextWeapon(weaponNum);
        
        // check; if there are no ready weapons, you're done.
        if (nextWeapon == -1 && Settings.autoEndFiring) {
            ready();
            return;
        }
        
        // otherwise, display firing info for the next weapon
        client.mechD.wPan.displayMech(ce());
        client.mechD.wPan.selectWeapon(nextWeapon);
        updateTarget();
        
        butNext.setEnabled(false);
    }
    
    /**
     * Skips to the next weapon
     */
    private void nextWeapon() {
        int nextWeapon = ce().getNextWeapon(client.mechD.wPan.getSelectedWeaponNum());
        // if there's no next weapon, forget about it
        if(nextWeapon == -1) {
            return;
        }
        client.mechD.wPan.displayMech(ce());
        client.mechD.wPan.selectWeapon(nextWeapon);
        updateTarget();
    }
    
    /**
     * The entity spends the rest of its turn finding a club
     */
    private void findClub() {
        if (ce() == null) {
            return;
        }
        
        attacks.removeAllElements();
        attacks.addElement(new FindClubAction(cen));
        
        ready();
    }
  
    /**
     * Removes all current fire
     */
    private void clearAttacks() {
        // We may not have an entity selected yet (race condition).
        if (ce() == null) {
            return;
        }
        
        // remove attacks, set weapons available again
        Enumeration i = attacks.elements();
        while ( i.hasMoreElements() ) {
            Object o = i.nextElement();
            if (o instanceof WeaponAttackAction) {
                WeaponAttackAction waa = (WeaponAttackAction)o;
                ce().getEquipment(waa.getWeaponId()).setUsedThisRound(false);
            }
        }
        attacks.removeAllElements();
        
        // remove temporary attacks from game & board
        removeTempAttacks();
        
        // restore any other movement to default
        ce().setSecondaryFacing(ce().getFacing());
        ce().setArmsFlipped(false);
    }
    
    /**
     * Removes temp attacks from the game and board
     */
    private void removeTempAttacks() {
        // remove temporary attacks from game & board
        client.game.removeActionsFor(cen);
        client.bv.removeAttacksFor(cen);
        client.bv.repaint(100);
        
    }
    
    /**
     * Refeshes all displays.
     */
    private void refreshAll() {
        if (ce() == null) {
            return;
        }
        client.bv.redrawEntity(ce());
        client.mechD.displayEntity(ce());
        client.mechD.showPanel("weapons");
        client.mechD.wPan.selectWeapon(ce().getFirstWeapon());
        updateTarget();
    }
    
    /**
     * Targets something
     */
    void target(Targetable t) {
        this.target = t;
        updateTarget();
    }
    
    /**
     * Targets something
     */
    protected void updateTarget() {
        butFire.setEnabled(false);
        
        // make sure we're showing the current entity in the mech display
        if (ce() != null && !ce().equals(client.mechD.getCurrentEntity())) {
            client.mechD.displayEntity(ce());
        }
        
        // update target panel
        final int weaponId = client.mechD.wPan.getSelectedWeaponNum();
        if (target != null && weaponId != -1) {
            ToHitData toHit = Compute.toHitWeapon(client.game, cen, target, weaponId);
            client.mechD.wPan.wTargetR.setText(target.getDisplayName());
            client.mechD.wPan.wRangeR.setText("" + ce().getPosition().distance(target.getPosition()));
            Mounted m = ce().getEquipment(weaponId);
            if (m.isUsedThisRound()) {
                client.mechD.wPan.wToHitR.setText("Already fired");
                butFire.setEnabled(false);
            } else if (m.getType().hasFlag(WeaponType.F_AUTO_TARGET)) {
                client.mechD.wPan.wToHitR.setText("Auto-firing weapon");
                butFire.setEnabled(false);
            } else if (toHit.getValue() == ToHitData.IMPOSSIBLE) {
                client.mechD.wPan.wToHitR.setText(toHit.getValueAsString());
                butFire.setEnabled(false);
            } else if (toHit.getValue() == ToHitData.AUTOMATIC_FAIL) {
                client.mechD.wPan.wToHitR.setText(toHit.getValueAsString());
                butFire.setEnabled(true);
            } else {
                client.mechD.wPan.wToHitR.setText(toHit.getValueAsString() + " (" + Compute.oddsAbove(toHit.getValue()) + "%)");
                butFire.setEnabled(true);
            }
            client.mechD.wPan.toHitText.setText(toHit.getDesc());
            butSkip.setEnabled(true);
        } else {
            client.mechD.wPan.wTargetR.setText("---");
            client.mechD.wPan.wRangeR.setText("---");
            client.mechD.wPan.wToHitR.setText("---");
            client.mechD.wPan.toHitText.setText("");
        }
    }
  
    /**
     * Torso twist in the proper direction.
     */
    private void torsoTwist(Coords target) {
        int direction = ce().getFacing();
        
        if ( null != target )
          direction = ce().clipSecondaryFacing(ce().getPosition().direction(target));
          
        if (direction != ce().getSecondaryFacing()) {
            clearAttacks();
            attacks.addElement(new TorsoTwistAction(cen, direction));
            ce().setSecondaryFacing(direction);
            refreshAll();
        }
    }
    
    /**
     * Returns the current entity.
     */
    private Entity ce() {
        return client.game.getEntity(cen);
    }
        
    //
    // BoardListener
    //
    public void boardHexMoused(BoardEvent b) {
        // ignore buttons other than 1
        if (!client.isMyTurn() || (b.getModifiers() & MouseEvent.BUTTON1_MASK) == 0) {
            return;
        }
        // check for shifty goodness
        if (shiftheld != ((b.getModifiers() & MouseEvent.SHIFT_MASK) != 0)) {
            shiftheld = (b.getModifiers() & MouseEvent.SHIFT_MASK) != 0;
        }
        
        if (b.getType() == b.BOARD_HEX_DRAGGED) {
            if (shiftheld || twisting) {
                updateFlipArms(false);
                torsoTwist(b.getCoords());
            }
            client.game.board.cursor(b.getCoords());
        } else if (b.getType() == b.BOARD_HEX_CLICKED) {
            twisting = false;
            client.game.board.select(b.getCoords());
        }
    }
    
    public void boardHexSelected(BoardEvent b) {
        if (client.isMyTurn() && b.getCoords() != null && ce() != null && !b.getCoords().equals(ce().getPosition())) {
            if (shiftheld) {
                updateFlipArms(false);
                torsoTwist(b.getCoords());
            } else if (client.game.getFirstEntity(b.getCoords()) != null) {
                target(client.game.getFirstEntity(b.getCoords()));
            }
        }
    }
    
    //
    // GameListener
    //
    public void gameTurnChange(GameEvent ev) {
        if(client.game.phase == Game.PHASE_FIRING) {
            endMyTurn();
            
            if(client.isMyTurn()) {
                beginMyTurn();
                labStatus.setText("It's your turn to fire.");
            } else {
                labStatus.setText("It's " + ev.getPlayer().getName() + "'s turn to fire.");
            }
        }
    }
    public void gamePhaseChange(GameEvent ev) {
        if(client.isMyTurn() && client.game.phase != Game.PHASE_FIRING) {
            endMyTurn();
        }
        // if we're ending the firing phase, unregister stuff.
        if(client.game.phase !=  Game.PHASE_FIRING) {
            client.removeGameListener(this);
            client.game.board.removeBoardListener(this);
            client.mechD.wPan.weaponList.removeItemListener(this);
            client.mechD.wPan.weaponList.removeKeyListener(this);
            client.bv.removeKeyListener(this);
            client.cb.getComponent().removeKeyListener(this);

        }
    }

    //
    // ActionListener
    //
    public void actionPerformed(ActionEvent ev) {
        if (ev.getSource() == butDisplay) {
            client.toggleDisplay();
        }
        else if (ev.getSource() == butMap) {
            client.toggleMap();
        }
        
        if (!client.isMyTurn()) {
            return;
        }
        
        if (ev.getSource() == butDone) {
            ready();
        } else if (ev.getSource() == butReport) {
            new MiniReportDisplay(client.frame, client.eotr).show();
            return;
        } else if (ev.getSource() == butFire) {
            fire();
        } else if (ev.getSource() == butSkip) {
            nextWeapon();
        } else if (ev.getSource() == butTwist) {
            twisting = true;
        } else if (ev.getSource() == butNext) {
            selectEntity(client.getNextEntityNum(cen));
        } else if (ev.getSource() == butMore) {
            buttonLayout++;
            buttonLayout %= NUM_BUTTON_LAYOUTS;
            setupButtonPanel();
        } else if (ev.getSource() == butFindClub) {
            findClub();
        } else if (ev.getSource() == butNextTarg) {
          jumpToNextTarget();
        } else if (ev.getSource() == butFlipArms) {
          updateFlipArms(!ce().getArmsFlipped());
        // Fire Mode - More Fire Mode button handling - Rasia
        } else if (ev.getSource() == butFireMode) {
            changeMode();
        }
    }
    
    private void updateFlipArms(boolean armsFlipped) {
        if (armsFlipped == ce().getArmsFlipped()) {
            return;
        }
        
      twisting = false;

      torsoTwist(null);
      
      clearAttacks();
      ce().setArmsFlipped(armsFlipped);
      attacks.addElement(new FlipArmsAction(cen, armsFlipped));
      updateTarget();
      refreshAll();
    }
    
    //
    // KeyListener
    //
    public void keyPressed(KeyEvent ev) {
        if (ev.getKeyCode() == ev.VK_ESCAPE) {
            clearAttacks();
            client.game.board.select(null);
            client.game.board.cursor(null);
            refreshAll();
        }
        if (ev.getKeyCode() == ev.VK_ENTER && ev.isControlDown()) {
            if (client.isMyTurn()) {
                //
            }
        }
        if (ev.getKeyCode() == KeyEvent.VK_SHIFT && !shiftheld) {
            shiftheld = true;
            if (client.isMyTurn() && client.game.board.lastCursor != null) {
                updateFlipArms(false);
                torsoTwist(client.game.board.lastCursor);
            }
        }
        if (ev.getKeyCode() == ev.VK_M) {
            changeMode(); 
        }
    }
    public void keyReleased(KeyEvent ev) {
        if (ev.getKeyCode() == KeyEvent.VK_SHIFT && shiftheld) {
            shiftheld = false;
        }
    }
    public void keyTyped(KeyEvent ev) {
        ;
    }
    
    //
    // ItemListener
    //
    public void itemStateChanged(ItemEvent ev) {
        if(ev.getItemSelectable() == client.mechD.wPan.weaponList) {
            // update target data in weapon display
            updateTarget();
        }
    }
}


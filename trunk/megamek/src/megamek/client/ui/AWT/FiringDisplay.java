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
import megamek.common.actions.*;

public class FiringDisplay 
    extends AbstractPhaseDisplay
    implements BoardListener, GameListener, ActionListener,
    KeyListener, ComponentListener, MouseListener,
    ItemListener
{
    // parent game
    public Client client;
    
    // displays
    private Label              labStatus;
    
    // buttons
    private Container        panButtons;
    private Button            butFire;
    private Button            butReady;
    private Button            butNext;
    private Button            butMenu;
    
    // let's keep track of what we're shooting and at what, too
    private int                cen;        // current entity number
    private int                ten;        // target entity number
    private int       selectedWeapon;
  
    // shots we have so far.
    private Vector attacks;  
  
    // is the shift key held?
    private boolean            shiftheld;

    
    /**
     * Creates and lays out a new movement phase display 
     * for the specified client.
     */
    public FiringDisplay(Client client) {
        this.client = client;
        client.addGameListener(this);
        
        client.game.board.addBoardListener(this);
        
        shiftheld = false;
    
        // fire
        attacks = new Vector();

        labStatus = new Label("Waiting to begin Weapon Attack phase...", Label.CENTER);
        
        butFire = new Button("Fire");
        butFire.setActionCommand("fire");
        butFire.addActionListener(this);
        butFire.setEnabled(false);
        
        butReady = new Button("Done");
        butReady.setActionCommand("ready");
        butReady.addActionListener(this);
        
        butNext = new Button("Next Unit");
        butNext.setActionCommand("next");
        butNext.addActionListener(this);
        butNext.setEnabled(false);
        
        butMenu = new Button("?");
        butMenu.setActionCommand("menu");
        butMenu.addActionListener(this);
        butMenu.setEnabled(false);
        
        // layout button grid
        panButtons = new Panel();
        panButtons.setLayout(new GridLayout(2, 2));
        panButtons.add(butFire);
        panButtons.add(butNext);
        panButtons.add(butMenu);
        panButtons.add(butReady);
    
        
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
        addBag(labStatus, gridbag, c);

        c.gridwidth = 1;
        c.weightx = 1.0;    c.weighty = 0.0;
        addBag(client.cb.getComponent(), gridbag, c);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 0.0;    c.weighty = 0.0;
        addBag(panButtons, gridbag, c);
        
        addKeyListener(this);
        
        // mech display.
        client.mechD.addMouseListener(this);
        client.mechD.wPan.weaponList.addItemListener(this);
        client.frame.addComponentListener(this);
    
    }
    
    private void addBag(Component comp, GridBagLayout gridbag, GridBagConstraints c) {
        gridbag.setConstraints(comp, c);
        add(comp);
        comp.addKeyListener(this);
    }
    
    /**
     * Selects an entity, by number, for movement.
     */
    public void selectEntity(int en) {
        if (client.game.getEntity(en) != null) {
            this.cen = en;
            target(Entity.NONE);
            client.game.board.highlight(ce().getPosition());
            client.game.board.select(null);
            client.game.board.cursor(null);

            client.mechD.displayMech(ce());
            client.mechD.showPanel("weapons");
            selectedWeapon = ce().getFirstWeapon();
            client.mechD.wPan.selectWeapon(selectedWeapon);

            client.bv.centerOnHex(ce().getPosition());
        } else {
            System.err.println("FiringDisplay: tried to select non-existant entity: " + en);
            System.err.println("FiringDisplay: sending ready signal...");
            client.sendReady(true);
        }
    }
    
    /**
     * Does turn start stuff
     */
    private void beginMyTurn() {
        ten = Entity.NONE;
        butNext.setEnabled(true);
        butReady.setEnabled(true);
        client.mechW.setVisible(true);
        moveMechDisplay();
        client.game.board.select(null);
        client.game.board.highlight(null);
        selectEntity(client.game.getFirstEntityNum(client.getLocalPlayer()));
    }
    
    /**
     * Does end turn stuff.
     */
    private void endMyTurn() {
        // end my turn, then.
        cen = Entity.NONE;
        ten = Entity.NONE;
        target(Entity.NONE);
        client.game.board.select(null);
        client.game.board.highlight(null);
        client.game.board.cursor(null);
        client.mechW.setVisible(false);
        client.bv.clearMovementData();
        disableButtons();
    }
    
    /**
     * Disables all buttons in the interface
     */
    private void disableButtons() {
        butFire.setEnabled(false);
        butReady.setEnabled(false);
        butNext.setEnabled(false);
    }
    
    /**
     * Called when the current entity is done firing.
     */
    private void ready() {
        disableButtons();
        client.sendAttackData(cen, attacks);
        attacks.removeAllElements();
        client.sendEntityReady(cen);
        client.sendReady(true);
    }
    
    /**
     * Sends a data packet indicating the chosen weapon and 
     * target.
     */
    private void fire() {
        int wn = client.mechD.wPan.weaponList.getSelectedIndex();
        if(ce() == null || te() == null || ce().getWeapon(wn) == null) {
            throw new IllegalArgumentException("current fire parameters are invalid");
        }
    
        attacks.addElement(new WeaponAttackAction(cen, ten, wn));
    
        ce().getWeapon(wn).setReady(false);
        selectedWeapon = ce().getNextWeapon(wn);
        // check; if there are no ready weapons, you're done.
        if(selectedWeapon == -1) {
            ready();
            return;
        }
        client.mechD.wPan.displayMech(ce());
        client.mechD.wPan.selectWeapon(selectedWeapon);
        updateTarget();
        butNext.setEnabled(false);
    }
  
    /**
     * Removes all current fire
     */
    private void clearAttacks() {
        if (attacks.size() > 0) {
            for (Enumeration i = attacks.elements(); i.hasMoreElements();) {
                Object o = i.nextElement();
                if (o instanceof WeaponAttackAction) {
                    WeaponAttackAction waa = (WeaponAttackAction)o;
                    ce().getWeapon(waa.getWeaponId()).setReady(true);
                }
            }
            attacks.removeAllElements();
        }
        client.mechD.wPan.displayMech(ce());
        selectedWeapon = ce().getFirstWeapon();
        client.mechD.wPan.selectWeapon(selectedWeapon);
        updateTarget();
    }
    
    /**
     * Targets an entity
     */
    private void target(int en) {
        this.ten = en;
        updateTarget();
    }
    
    /**
     * Targets an entity
     */
    private void updateTarget() {
        // update target panel
        final int weaponId = client.mechD.wPan.weaponList.getSelectedIndex();
        if (ten != Entity.NONE && weaponId != -1) {
            ToHitData toHit = Compute.toHitWeapon(client.game, cen, ten, weaponId, attacks);
            client.mechD.wPan.wTargetR.setText(te().getDisplayName());
            client.mechD.wPan.wRangeR.setText("" + ce().getPosition().distance(te().getPosition()));
            if (toHit.getValue() <= 12) {
                client.mechD.wPan.wToHitR.setText(toHit.getValue() + " (" + Compute.oddsAbove(toHit.getValue()) + "%)");
                butFire.setEnabled(true);
            } else {
                client.mechD.wPan.wToHitR.setText("Impossible");
                butFire.setEnabled(false);
            }
            client.mechD.wPan.toHitText.setText(toHit.getDesc());
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
    private void doTorsoTwist(Coords target) {
        int direction = ce().clipSecondaryFacing(ce().getPosition().direction(target));
        //System.out.println("firingDisplay: removed all pending fire due to torso twist");
        if (direction != ce().getSecondaryFacing()) {
            clearAttacks();
            attacks.addElement(new TorsoTwistAction(cen, direction));
            ce().setSecondaryFacing(direction);
            client.bv.redrawEntity(ce());
        }
    }
    
    /**
     * Returns the current entity.
     */
    private Entity ce() {
        return client.game.getEntity(cen);
    }
    
    /**
     * Returns the target entity.
     */
    private Entity te() {
        return client.game.getEntity(ten);
    }
    
    /**
     * Moves the mech display window to the proper position.
     */
    private void moveMechDisplay() {
        if(client.bv.isShowing()) {
            client.mechW.setLocation(client.bv.getLocationOnScreen().x 
                                     + client.bv.getSize().width 
                                     - client.mechD.getSize().width - 20, 
                                     client.bv.getLocationOnScreen().y + 20);
        }
    }
    
    //
    // BoardListener
    //
    public void boardHexMoused(BoardEvent b) {
        if (client.isMyTurn()
            && (b.getModifiers() & MouseEvent.BUTTON1_MASK) != 0) {
            if (b.getType() == b.BOARD_HEX_DRAGGED) {
                if (!b.getCoords().equals(client.game.board.lastCursor)) {
                    if (shiftheld) {
                        // consider torso twist towards selected hex
                        doTorsoTwist(b.getCoords());
                    }
                    client.game.board.cursor(b.getCoords());
                }
            } else if (b.getType() == b.BOARD_HEX_CLICKED) {
                client.game.board.select(b.getCoords());
            }
        }
    }
    public void boardHexSelected(BoardEvent b) {
        if (client.isMyTurn() && b.getCoords() != null && ce() != null
            && !b.getCoords().equals(ce().getPosition())) {
          if (shiftheld) {
            // commit torso twist towards selected hex
            doTorsoTwist(b.getCoords());
          } else if (client.game.getEntity(b.getCoords()) != null 
                     && client.game.getEntity(b.getCoords()).isTargetable()) {
                  target(client.game.getEntity(b.getCoords()).getId());
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
            client.bv.clearAllAttacks();
            
            client.removeGameListener(this);
            client.game.board.removeBoardListener(this);
            client.mechD.removeMouseListener(this);
            client.mechD.wPan.weaponList.removeItemListener(this);
            client.frame.removeComponentListener(this);
            client.bv.removeKeyListener(this);
            client.cb.getComponent().removeKeyListener(this);
        }
    }

    //
    // ActionListener
    //
    public void actionPerformed(ActionEvent ev) {
        if(ev.getActionCommand().equalsIgnoreCase("ready") && client.isMyTurn()) {
            ready();
        }
        if(ev.getActionCommand().equalsIgnoreCase("fire") && client.isMyTurn()) {
            fire();
        }
        if(ev.getActionCommand().equalsIgnoreCase("next") && client.isMyTurn()) {
            selectEntity(client.game.getNextEntityNum(client.getLocalPlayer(), cen));
        }
    }
    

    //
    // KeyListener
    //
    public void keyPressed(KeyEvent ev) {
        if (ev.getKeyCode() == ev.VK_ESCAPE) {
            clearAttacks();
        }
        if (ev.getKeyCode() == ev.VK_ENTER && ev.isControlDown()) {
            if (client.isMyTurn()) {
                //
            }
        }
        if (ev.getKeyCode() == KeyEvent.VK_SHIFT && !shiftheld) {
            shiftheld = true;
            if (client.isMyTurn() && client.game.board.lastCursor != null) {
                // torso twist towards cursor
                doTorsoTwist(client.game.board.lastCursor);
            }
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
    // ComponentListener
    //
    public void componentHidden(ComponentEvent ev) {
        client.mechW.setVisible(false);
    }
    public void componentMoved(ComponentEvent ev) {
        moveMechDisplay();
    }
    public void componentResized(ComponentEvent ev) {
        moveMechDisplay();
    }
    public void componentShown(ComponentEvent ev) {
        client.mechW.setVisible(false);
        moveMechDisplay();
    }
    
    //
    // MouseListener
    //
    public void mouseEntered(MouseEvent ev) {
        ;
    }
    public void mouseExited(MouseEvent ev) {
        ;
    }
    public void mousePressed(MouseEvent ev) {
        ;
    }
    public void mouseReleased(MouseEvent ev) {
        ;
    }
    public void mouseClicked(MouseEvent ev) {
        ;
    }
    
    //
    // ItemListener
    //
    public void itemStateChanged(ItemEvent ev) {
        if(ev.getItemSelectable() == client.mechD.wPan.weaponList) {
            butFire.setEnabled(false);
            // update target data in weapon display
            updateTarget();
            // also, allow firing only if weapon is ready
            if (ce() != null) {
                butFire.setEnabled(ce().getWeapon(client.mechD.wPan.weaponList.getSelectedIndex()).isReady());
            } else {
                butFire.setEnabled(false);
            }
        }
    }
}

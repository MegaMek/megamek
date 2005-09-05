/*
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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

package megamek.client.ui.AWT;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import megamek.client.Client;
import megamek.client.event.BoardViewEvent;
import megamek.client.event.BoardViewListener;
import megamek.common.*;
import megamek.common.actions.*;
import megamek.common.event.GameListener;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GameTurnChangeEvent;
import megamek.common.util.Distractable;
import megamek.common.util.DistractableAdapter;
/*Targeting Phase Display.  Breaks naming convention because
TargetingDisplay is too easy to confuse with something else*/
public class TargetingPhaseDisplay
    extends StatusBarPhaseDisplay
    implements GameListener, ActionListener, DoneButtoned,
               KeyListener, ItemListener, BoardViewListener, Distractable
{
    // Distraction implementation.
    private DistractableAdapter distracted = new DistractableAdapter();

    // Action command names
    public static final String FIRE_FIRE       = "fireFire"; //$NON-NLS-1$
    public static final String FIRE_MODE       = "fireMode"; //$NON-NLS-1$
    public static final String FIRE_FLIP_ARMS  = "fireFlipArms"; //$NON-NLS-1$
    public static final String FIRE_NEXT       = "fireNext"; //$NON-NLS-1$
    public static final String FIRE_NEXT_TARG  = "fireNextTarg"; //$NON-NLS-1$
    public static final String FIRE_SKIP       = "fireSkip"; //$NON-NLS-1$
    public static final String FIRE_TWIST      = "fireTwist"; //$NON-NLS-1$
    public static final String FIRE_CANCEL     = "fireCancel"; //$NON-NLS-1$
    public static final String FIRE_SEARCHLIGHT= "fireSearchlight"; //$NON-NLS-1$

    // parent game
    public ClientGUI clientgui;
    private Client client;

    // buttons
    private Container        panButtons;

    private Button            butFire;
    private Button            butTwist;
    private Button            butSkip;
    private Button            butFlipArms;
    private Button            butFireMode;
    private Button            butSpace;
    private Button            butNext;
    private Button            butNextTarg;
    private Button            butDone;
    private Button            butSearchlight;

    private int               buttonLayout;

    // let's keep track of what we're shooting and at what, too
    private int                cen = Entity.NONE;        // current entity number
    private Targetable         target;        // target

    // shots we have so far.
    private Vector attacks;

    // is the shift key held?
    private boolean            shiftheld;
    private boolean            twisting;

    private final int           phase;

    private Entity[]            visibleTargets  = null;
    private int                 lastTargetID    = -1;
    private boolean            showTargetChoice = true;

    /**
     * Creates and lays out a new targeting phase display
     * for the specified client.
     */
    public TargetingPhaseDisplay(ClientGUI clientgui, boolean offboard) {
        this.clientgui = clientgui;
        this.client = clientgui.getClient();
        this.phase = offboard?IGame.PHASE_OFFBOARD:IGame.PHASE_TARGETING;
        shiftheld = false;

        // fire
        attacks = new Vector();

        setupStatusBar(Messages.getString("TargetingPhaseDisplay.waitingForTargetingPhase")); //$NON-NLS-1$

        butFire = new Button(Messages.getString("TargetingPhaseDisplay.Fire")); //$NON-NLS-1$
        butFire.addActionListener(this);
        butFire.setActionCommand(FIRE_FIRE);
        butFire.setEnabled(false);

        butSkip = new Button(Messages.getString("TargetingPhaseDisplay.Skip")); //$NON-NLS-1$
        butSkip.addActionListener(this);
        butSkip.setActionCommand(FIRE_SKIP);
        butSkip.setEnabled(false);

        butTwist = new Button(Messages.getString("TargetingPhaseDisplay.Twist")); //$NON-NLS-1$
        butTwist.addActionListener(this);
        butTwist.setActionCommand(FIRE_TWIST);
        butTwist.setEnabled(false);

        butFlipArms = new Button(Messages.getString("TargetingPhaseDisplay.FlipArms")); //$NON-NLS-1$
        butFlipArms.addActionListener(this);
        butFlipArms.setActionCommand(FIRE_FLIP_ARMS);
        butFlipArms.setEnabled(false);

        butFireMode = new Button(Messages.getString("TargetingPhaseDisplay.Mode")); //$NON-NLS-1$
        butFireMode.addActionListener(this);
        butFireMode.setActionCommand(FIRE_MODE);
        butFireMode.setEnabled(false);

        butNextTarg = new Button(Messages.getString("FiringDisplay.NextTarget")); //$NON-NLS-1$
        butNextTarg.addActionListener(this);
        butNextTarg.addKeyListener(this);
        butNextTarg.setActionCommand(FIRE_NEXT_TARG);
        butNextTarg.setEnabled(false);

        butSearchlight = new Button(Messages.getString("FiringDisplay.Searchlight")); //$NON-NLS-1$
        butSearchlight.addActionListener(this);
        butSearchlight.addKeyListener(this);
        butSearchlight.setActionCommand(FIRE_SEARCHLIGHT);
        butSearchlight.setEnabled(false);

        butSpace = new Button("."); //$NON-NLS-1$
        butSpace.setEnabled(false);

        butDone = new Button(Messages.getString("TargetingPhaseDisplay.Done")); //$NON-NLS-1$
        butDone.addActionListener(this);
        butDone.setEnabled(false);

        butNext = new Button(Messages.getString("TargetingPhaseDisplay.NextUnit")); //$NON-NLS-1$
        butNext.addActionListener(this);
        butNext.setActionCommand(FIRE_NEXT);
        butNext.setEnabled(false);

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
//         c.gridwidth = GridBagConstraints.REMAINDER;
//         addBag(clientgui.bv, gridbag, c);

//         c.weightx = 1.0;    c.weighty = 0;
//         c.gridwidth = 1;
//         addBag(client.cb.getComponent(), gridbag, c);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 0.0;    c.weighty = 0.0;
        addBag(panButtons, gridbag, c);

        c.weightx = 1.0;    c.weighty = 0.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        addBag(panStatus, gridbag, c);

    }

    /**
     * Have the panel register itself as a listener wherever it's needed.
     * <p/>
     * According to http://www-106.ibm.com/developerworks/java/library/j-jtp0618.html
     * it is a major bad no-no to perform these registrations before the
     * constructor finishes, so this function has to be called after the
     * panel is created.  Please note, this restriction only applies to
     * listeners for objects that aren't on the panel itself.
     */
    public void initializeListeners() {

        client.game.addGameListener(this);
        clientgui.getBoardView().addBoardViewListener(this);

        this.clientgui.bv.addKeyListener( this );
        addKeyListener(this);

        // mech display.
        this.clientgui.mechD.wPan.weaponList.addItemListener(this);
        this.clientgui.mechD.wPan.weaponList.addKeyListener(this);
    }

    private void addBag(Component comp, GridBagLayout gridbag, GridBagConstraints c) {
        gridbag.setConstraints(comp, c);
        add(comp);
        comp.addKeyListener(this);
    }

    private void setupButtonPanel() {
        panButtons.removeAll();
        panButtons.setLayout(new GridLayout(0, 8));

        switch (buttonLayout) {
        case 0 :
            panButtons.add(butNext);
            panButtons.add(butFire);
            panButtons.add(butSkip);
            panButtons.add(butNextTarg);
            panButtons.add(butFlipArms);
            panButtons.add(butTwist);
            panButtons.add(butFireMode);
            panButtons.add(butSearchlight);
         // panButtons.add(butDone);
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
            clientgui.setSelectedEntityNum(en);
            
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
                    ("FiringDisplay: could not find an on-board entity: " + //$NON-NLS-1$
                            en);
                    return;
                }
                
            } // End ce()-not-on-board
            
            target(null);
            clientgui.getBoardView().highlight(ce().getPosition());
            clientgui.getBoardView().select(null);
            clientgui.getBoardView().cursor(null);
            
            refreshAll();
            cacheVisibleTargets();
            
            if (!clientgui.bv.isMovingUnits()) {
                clientgui.bv.centerOnHex(ce().getPosition());
            }
            
            // Update the menu bar.
            clientgui.getMenuBar().setEntity( ce() );
            
            // 2003-12-29, nemchenk -- only twist if crew conscious
            setTwistEnabled(ce().canChangeSecondaryFacing() && ce().getCrew().isActive());
            setFlipArmsEnabled(ce().canFlipArms());
            updateSearchlight();

            setFireModeEnabled(true);
        } else {
            System.err.println("FiringDisplay: tried to select non-existant entity: " + en); //$NON-NLS-1$
        }
    }

    /**
     * Does turn start stuff
     */
    private void beginMyTurn() {
        target = null;

        selectEntity(client.getFirstEntityNum());

        if (!clientgui.bv.isMovingUnits()) {
            clientgui.setDisplayVisible(true);
        }

        // There's special processing for triggering AP Pods.
        if ( client.game.getTurn() instanceof GameTurn.TriggerAPPodTurn &&
             null != ce() ) {
            disableButtons();
            TriggerAPPodDialog dialog = new TriggerAPPodDialog
                ( clientgui.getFrame(), ce() );
            dialog.show();
            attacks.removeAllElements();
            Enumeration actions = dialog.getActions();
            while ( actions.hasMoreElements() ) {
                attacks.addElement( actions.nextElement() );
            }
            ready();
        } else {
            setNextEnabled(true);
            butDone.setEnabled(true);
            clientgui.getBoardView().select(null);
        }
    }

    /**
     * Does end turn stuff.
     */
    private void endMyTurn() {
        // end my turn, then.
        Entity next = client.game.getNextEntity( client.game.getTurnIndex() );
        if ( phase == client.game.getPhase()
             && null != next
             && null != ce()
             && next.getOwnerId() != ce().getOwnerId() ) {
            clientgui.setDisplayVisible(false);
        }
        cen = Entity.NONE;
        target(null);
        clientgui.getBoardView().select(null);
        clientgui.getBoardView().highlight(null);
        clientgui.getBoardView().cursor(null);
        clientgui.bv.clearMovementData();
        disableButtons();

    }

    /**
     * Disables all buttons in the interface
     */
    private void disableButtons() {
        setFireEnabled(false);
        setSkipEnabled(false);
        setTwistEnabled(false);
        setNextEnabled(false);
        butDone.setEnabled(false);
        setFlipArmsEnabled(false);
        setFireModeEnabled(false);
        setNextTargetEnabled(false);
    }

   /**
    * Fire Mode - Adds a Fire Mode Change to the current Attack Action
    */
    private void changeMode() {
        int wn = clientgui.mechD.wPan.getSelectedWeaponNum();

        // Do nothing we have no unit selected.
        if ( null == ce() ) {
            return;
        }

        // If the weapon does not have modes, just exit.
        Mounted m = ce().getEquipment(wn);
        if ( m == null || !m.getType().hasModes() ) {
            return;
        }
        
        // send change to the server
        int nMode = m.switchMode();
        client.sendModeChange(cen, wn, nMode);
        
        // notify the player
        if (m.getType().hasInstantModeSwitch()) {
            clientgui.systemMessage(Messages.getString("FiringDisplay.switched", new Object[]{m.getName(), m.curMode().getDisplayableName()})); //$NON-NLS-1$
        }
        else {
            clientgui.systemMessage(Messages.getString("FiringDisplay.willSwitch", new Object[]{m.getName(), m.pendingMode().getDisplayableName()})); //$NON-NLS-1$
        }

        this.updateTarget();
        clientgui.mechD.wPan.displayMech(ce());
        clientgui.mechD.wPan.selectWeapon(wn);
    }

    /**
     * Called when the current entity is done firing.  Send out our attack
     * queue to the server.
     */
    private void ready() {
        if (attacks.isEmpty() && GUIPreferences.getInstance().getNagForNoAction()) {
            // comfirm this action
            String title = Messages.getString("TargetingPhaseDisplay.DontFireDialog.title"); //$NON-NLS-1$
            String body = Messages.getString("TargetingPhaseDisplay.DontFireDialog.message"); //$NON-NLS-1$
            ConfirmDialog response = clientgui.doYesNoBotherDialog(title, body);
            if ( !response.getShowAgain() ) {
                GUIPreferences.getInstance().setNagForNoAction(false);
            }
            if ( !response.getAnswer() ) {
                return;
            }
        }

        // stop further input (hopefully)
        disableButtons();

        // remove temporary attacks from game & board
        removeTempAttacks();

        // send out attacks
        client.sendAttackData(cen, attacks);

        // clear queue
        attacks.removeAllElements();

        // Clear the menu bar.
        clientgui.getMenuBar().setEntity( null );

        // close aimed shot display, if any

    }

    private void doSearchlight() {
        // validate
        if (ce() == null || target == null) {
            throw new IllegalArgumentException("current searchlight parameters are invalid"); //$NON-NLS-1$
        }

        if(!SearchlightAttackAction.isPossible(client.game,cen,target,null))
            return;

        //create and queue a searchlight action
        SearchlightAttackAction saa = new SearchlightAttackAction(cen, target.getTargetType(), target.getTargetId());
        attacks.addElement(saa);

        // and add it into the game, temporarily
        client.game.addAction(saa);
        clientgui.bv.addAttack(saa);
        clientgui.bv.repaint(100);
        clientgui.minimap.drawMap();

        //refresh weapon panel, as bth will have changed
        updateTarget();
    }

    /**
     * Adds a weapon attack with the currently selected weapon to the attack
     * queue.
     */
    private void fire() {
        // get the selected weaponnum
        int weaponNum = clientgui.mechD.wPan.getSelectedWeaponNum();
        Mounted mounted = ce().getEquipment(weaponNum);

        // validate
        if (ce() == null || target == null || mounted == null
        || !(mounted.getType() instanceof WeaponType)) {
            throw new IllegalArgumentException("current fire parameters are invalid"); //$NON-NLS-1$
        }

        WeaponAttackAction waa = new WeaponAttackAction(cen, target.getTargetType(),
                target.getTargetId(), weaponNum);
        if ( null != mounted.getLinked() && 
                ((WeaponType)mounted.getType()).getAmmoType() != AmmoType.T_NA ) {
               Mounted ammoMount = mounted.getLinked();
               waa.setAmmoId(ce().getEquipmentNum(ammoMount));
               if (((AmmoType)(ammoMount.getType())).getMunitionType() == AmmoType.M_VIBRABOMB_IV) {
                   VibrabombSettingDialog vsd = new VibrabombSettingDialog(clientgui.frame);
                   vsd.show();
                   waa.setOtherAttackInfo(vsd.getSetting());
               }
        }
        
        // add the attack to our temporary queue
        attacks.addElement(waa);

        // and add it into the game, temporarily
        client.game.addAction(waa);
        clientgui.bv.addAttack(waa);
        clientgui.bv.repaint(100);
        clientgui.minimap.drawMap();

        // set the weapon as used
        mounted.setUsedThisRound(true);

        // find the next available weapon
        int nextWeapon = ce().getNextWeapon(weaponNum);

        // check; if there are no ready weapons, you're done.
        if (nextWeapon == -1 && GUIPreferences.getInstance().getAutoEndFiring()) {
            ready();
            return;
        }

        // otherwise, display firing info for the next weapon
        clientgui.mechD.wPan.displayMech(ce());
        clientgui.mechD.wPan.selectWeapon(nextWeapon);
        updateTarget();

    }

    /**
     * Skips to the next weapon
     */
    private void nextWeapon() {
        int nextWeapon = ce().getNextWeapon(clientgui.mechD.wPan.getSelectedWeaponNum());
        // if there's no next weapon, forget about it
        if(nextWeapon == -1) {
            return;
        }
        clientgui.mechD.wPan.displayMech(ce());
        clientgui.mechD.wPan.selectWeapon(nextWeapon);
        updateTarget();
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
        clientgui.bv.removeAttacksFor(cen);
        clientgui.bv.repaint(100);

    }

    /**
     * Refeshes all displays.
     */
    private void refreshAll() {
        if (ce() == null) {
            return;
        }
        clientgui.bv.redrawEntity(ce());
        clientgui.mechD.displayEntity(ce());
        clientgui.mechD.showPanel("weapons"); //$NON-NLS-1$
        clientgui.mechD.wPan.selectWeapon(ce().getFirstWeapon());
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
        setFireEnabled(false);

        // make sure we're showing the current entity in the mech display
        if (ce() != null && !ce().equals(clientgui.mechD.getCurrentEntity())) {
            clientgui.mechD.displayEntity(ce());
        }

        // update target panel
        final int weaponId = clientgui.mechD.wPan.getSelectedWeaponNum();
        if (target != null && weaponId != -1) {
            ToHitData toHit;

              toHit = WeaponAttackAction.toHit(client.game, cen, target, weaponId, Mech.LOC_NONE, 0);
              clientgui.mechD.wPan.wTargetR.setText(target.getDisplayName());

            clientgui.mechD.wPan.wRangeR.setText("" + ce().getPosition().distance(target.getPosition())); //$NON-NLS-1$
            Mounted m = ce().getEquipment(weaponId);
            if (m.isUsedThisRound()) {
                clientgui.mechD.wPan.wToHitR.setText(Messages.getString("TargetingPhaseDisplay.alreadyFired")); //$NON-NLS-1$
                setFireEnabled(false);
            } else if (m.getType().hasFlag(WeaponType.F_AUTO_TARGET)) {
                clientgui.mechD.wPan.wToHitR.setText(Messages.getString("TargetingPhaseDisplay.autoFiringWeapon")); //$NON-NLS-1$
                setFireEnabled(false);
            } else if (toHit.getValue() == ToHitData.IMPOSSIBLE) {
                clientgui.mechD.wPan.wToHitR.setText(toHit.getValueAsString());
                setFireEnabled(false);
            } else if (toHit.getValue() == ToHitData.AUTOMATIC_FAIL) {
                clientgui.mechD.wPan.wToHitR.setText(toHit.getValueAsString());
                setFireEnabled(true);
            } else {
                clientgui.mechD.wPan.wToHitR.setText(toHit.getValueAsString() + " (" + Compute.oddsAbove(toHit.getValue()) + "%)"); //$NON-NLS-1$ //$NON-NLS-2$
                setFireEnabled(true);
            }
            clientgui.mechD.wPan.toHitText.setText(toHit.getDesc());
            setSkipEnabled(true);
        } else {
            clientgui.mechD.wPan.wTargetR.setText("---"); //$NON-NLS-1$
            clientgui.mechD.wPan.wRangeR.setText("---"); //$NON-NLS-1$
            clientgui.mechD.wPan.wToHitR.setText("---"); //$NON-NLS-1$
            clientgui.mechD.wPan.toHitText.setText(""); //$NON-NLS-1$
        }
        updateSearchlight();
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
     * Torso twist to the left or right
     * @param target An <code>int</code> specifying wether we're twisting left or right,
     *               0 if we're twisting to the left, 1 if to the right.
     */
    
    private void torsoTwist(int target) {
        int direction = ce().getSecondaryFacing();
        if (target == 0) {
            clearAttacks();
            direction = ce().clipSecondaryFacing((direction+5)%6);
            attacks.addElement(new TorsoTwistAction(cen, direction));
            ce().setSecondaryFacing(direction);
            refreshAll();
        } else if (target == 1) {
            clearAttacks();
            direction = ce().clipSecondaryFacing((direction+7)%6);
            attacks.addElement(new TorsoTwistAction(cen, direction));
            ce().setSecondaryFacing(direction);
            refreshAll();
        }        
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
            tree.add(vec.elementAt(i));
        }
        
        com.sun.java.util.collections.Iterator it = tree.iterator();
        int count = 0;
        while ( it.hasNext() ) {
            visibleTargets[count++] = (Entity)it.next();
        }
        
        setNextTargetEnabled(visibleTargets.length > 0);
    }

    private void clearVisibleTargets() {
        visibleTargets = null;
        lastTargetID = -1;
        setNextTargetEnabled(false);
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
        
        return visibleTargets[lastTargetID];
    }
    
    /**
     * Jump to our next target. If there isn't one, well, don't do anything.
     */
    private void jumpToNextTarget() {
        Entity targ = getNextTarget();
        
        if ( null == targ )
            return;
        
        // HACK : don't show the choice dialog.
        this.showTargetChoice = false;
        
        clientgui.bv.centerOnHex(targ.getPosition());
        clientgui.getBoardView().select(targ.getPosition());
        
        // HACK : show the choice dialog again.
        this.showTargetChoice = true;
        target(targ);
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
    public void hexMoused(BoardViewEvent b) {

        // Are we ignoring events?
        if ( this.isIgnoringEvents() ) {
            return;
        }

        // ignore buttons other than 1
        if (!client.isMyTurn() || (b.getModifiers() & MouseEvent.BUTTON1_MASK) == 0) {
            return;
        }
    // control pressed means a line of sight check.
    // added ALT_MASK by kenn
    if ((b.getModifiers() & InputEvent.CTRL_MASK) != 0 || (b.getModifiers() & InputEvent.ALT_MASK) != 0) {
      return;
    }
        // check for shifty goodness
        if (shiftheld != ((b.getModifiers() & MouseEvent.SHIFT_MASK) != 0)) {
            shiftheld = (b.getModifiers() & MouseEvent.SHIFT_MASK) != 0;
        }

        if (b.getType() == BoardViewEvent.BOARD_HEX_DRAGGED) {
            if (shiftheld || twisting) {
                updateFlipArms(false);
                torsoTwist(b.getCoords());
            }
            clientgui.getBoardView().cursor(b.getCoords());
        } else if (b.getType() == BoardViewEvent.BOARD_HEX_CLICKED) {
            twisting = false;
            clientgui.getBoardView().select(b.getCoords());
        }
    }

    public void hexSelected(BoardViewEvent b) {

        // Are we ignoring events?
        if ( this.isIgnoringEvents() ) {
            return;
        }

        if (client.isMyTurn() && b.getCoords() != null && ce() != null && !b.getCoords().equals(ce().getPosition())) {
            boolean friendlyFire = client.game.getOptions().booleanOption("friendly_fire"); //$NON-NLS-1$
            if (shiftheld) {
                updateFlipArms(false);
                torsoTwist(b.getCoords());
            } else if (friendlyFire && client.game.getFirstEntity(b.getCoords()) != null) {
                target(client.game.getFirstEntity(b.getCoords()));
            } else if ( client.game.getFirstEnemyEntity(b.getCoords(), ce()) != null) {
                target(client.game.getFirstEnemyEntity(b.getCoords(), ce()));
            }
        }
    }

    //
    // GameListener
    //
    public void gameTurnChange(GameTurnChangeEvent e) {

        // Are we ignoring events?
        if ( this.isIgnoringEvents() ) {
            return;
        }

        if(client.game.getPhase() == phase) {
            endMyTurn();

            if(client.isMyTurn()) {
                beginMyTurn();
                setStatusBarText(Messages.getString("TargetingPhaseDisplay.its_your_turn")); //$NON-NLS-1$
            } else {               
                setStatusBarText(Messages.getString("TargetingPhaseDisplay.its_others_turn", new Object[]{e.getPlayer().getName()})); //$NON-NLS-1$
            }
        }
    }
    public void gamePhaseChange(GamePhaseChangeEvent e) {

        // Are we ignoring events?
        if ( this.isIgnoringEvents() ) {
            return;
        }

        if(client.isMyTurn() && client.game.getPhase() != phase) {
            endMyTurn();
        }
        // if we're ending the firing phase, unregister stuff.
        if(client.game.getPhase() ==  phase) {
            setStatusBarText(Messages.getString("TargetingPhaseDisplay.waitingForFiringPhase")); //$NON-NLS-1$
        }
    }

    //
    // ActionListener
    //
    public void actionPerformed(ActionEvent ev) {

        // Are we ignoring events?
        if ( this.isIgnoringEvents() ) {
            return;
        }

        if ( statusBarActionPerformed(ev, client) )
          return;

        if (!client.isMyTurn()) {
            return;
        }

        if (ev.getSource() == butDone) {
            ready();
        } else if (ev.getActionCommand().equalsIgnoreCase("viewGameOptions")) { //$NON-NLS-1$
            // Make sure the game options dialog is not editable.
            if ( clientgui.getGameOptionsDialog().isEditable() ) {
                clientgui.getGameOptionsDialog().setEditable( false );
            }
            // Display the game options dialog.
            clientgui.getGameOptionsDialog().update(client.game.getOptions());
            clientgui.getGameOptionsDialog().show();
        } else if (ev.getActionCommand().equals(FIRE_FIRE)) {
            fire();
        } else if (ev.getActionCommand().equals(FIRE_SKIP)) {
            nextWeapon();
        } else if (ev.getActionCommand().equals(FIRE_TWIST)) {
            twisting = true;
        } else if (ev.getActionCommand().equals(FIRE_NEXT)) {
            selectEntity(client.getNextEntityNum(cen));
        } else if (ev.getActionCommand().equals(FIRE_NEXT_TARG)) {
            jumpToNextTarget();
        } else if (ev.getActionCommand().equals(FIRE_FLIP_ARMS)) {
            updateFlipArms(!ce().getArmsFlipped());
        } else if (ev.getActionCommand().equals(FIRE_MODE)) {
            changeMode();
        } else if (ev.getActionCommand().equals(FIRE_CANCEL)) {
            clearAttacks();
            clientgui.getBoardView().select(null);
            clientgui.getBoardView().cursor(null);
            refreshAll();
        } else if (ev.getActionCommand().equals(FIRE_SEARCHLIGHT)) {
            doSearchlight();
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

    private void updateSearchlight() {
        setSearchlightEnabled(ce() != null
                && target != null
                && ce().isUsingSpotlight() 
                && ce().getCrew().isActive()
                && SearchlightAttackAction.isPossible(client.game, cen, target,null));
    }

        private void setFireEnabled(boolean enabled) {
                butFire.setEnabled(enabled);
        clientgui.getMenuBar().setFireFireEnabled(enabled);
        }
        private void setTwistEnabled(boolean enabled) {
                butTwist.setEnabled(enabled);
        clientgui.getMenuBar().setFireTwistEnabled(enabled);
        }
        private void setSkipEnabled(boolean enabled) {
                butSkip.setEnabled(enabled);
        clientgui.getMenuBar().setFireSkipEnabled(enabled);
        }
        private void setFlipArmsEnabled(boolean enabled) {
                butFlipArms.setEnabled(enabled);
        clientgui.getMenuBar().setFireFlipArmsEnabled(enabled);
        }
        private void setNextEnabled(boolean enabled) {
                butNext.setEnabled(enabled);
        clientgui.getMenuBar().setFireNextEnabled(enabled);
        }
    private void setSearchlightEnabled(boolean enabled) {
        butSearchlight.setEnabled(enabled);
        clientgui.getMenuBar().setFireSearchlightEnabled(enabled);
    }
    private void setFireModeEnabled(boolean enabled) {
        butFireMode.setEnabled(enabled);
        clientgui.getMenuBar().setFireModeEnabled(enabled);
    }
    private void setNextTargetEnabled(boolean enabled) {
        butNextTarg.setEnabled(enabled);
        clientgui.getMenuBar().setFireNextTargetEnabled(enabled);
    }

    //
    // KeyListener
    //
    public void keyPressed(KeyEvent ev) {

        // Are we ignoring events?
        if ( this.isIgnoringEvents() ) {
            return;
        }

        if (ev.getKeyCode() == KeyEvent.VK_ESCAPE) {
            clearAttacks();
            clientgui.getBoardView().select(null);
            clientgui.getBoardView().cursor(null);
            refreshAll();
        }
        if (ev.getKeyCode() == KeyEvent.VK_ENTER && ev.isControlDown()) {
            if (client.isMyTurn()) {
                //
            }
        }
        if (ev.getKeyCode() == KeyEvent.VK_SHIFT && !shiftheld) {
            shiftheld = true;
            if (client.isMyTurn() && clientgui.getBoardView().getLastCursor() != null) {
                updateFlipArms(false);
                torsoTwist(clientgui.getBoardView().getLastCursor());
            }
        }
        if (ev.getKeyCode() == KeyEvent.VK_LEFT && shiftheld) {
            updateFlipArms(false);
            torsoTwist(0);
        }
        if (ev.getKeyCode() == KeyEvent.VK_RIGHT && shiftheld) {
            updateFlipArms(false);
            torsoTwist(1);
        }
/*        if (ev.getKeyCode() == KeyEvent.VK_M) {
            changeMode();
        }
*/  }
    public void keyReleased(KeyEvent ev) {

        // Are we ignoring events?
        if ( this.isIgnoringEvents() ) {
            return;
        }

        if (ev.getKeyCode() == KeyEvent.VK_SHIFT && shiftheld) {
            shiftheld = false;
        }
    }
    public void keyTyped(KeyEvent ev) {
    }

    //
    // ItemListener
    //
    public void itemStateChanged(ItemEvent ev) {

        // Are we ignoring events?
        if ( this.isIgnoringEvents() ) {
            return;
        }

        if(ev.getItemSelectable() == clientgui.mechD.wPan.weaponList) {
            // update target data in weapon display
            updateTarget();
        }
    }

    // board view listener
    public void finishedMovingUnits(BoardViewEvent b) {

        // Are we ignoring events?
        if ( this.isIgnoringEvents() ) {
            return;
        }

        if (client.isMyTurn() && ce() != null) {
            clientgui.setDisplayVisible(true);
            clientgui.bv.centerOnHex(ce().getPosition());
        }
    }

    public void unitSelected(BoardViewEvent b) {

        // Are we ignoring events?
        if ( this.isIgnoringEvents() ) {
            return;
        }

            Entity e = client.game.getEntity(b.getEntityId());
            if (client.isMyTurn()) {
            if ( client.game.getTurn().isValidEntity(e,client.game) ) {
                selectEntity(e.getId());
            }
            } else {
            clientgui.setDisplayVisible(true);
            clientgui.mechD.displayEntity(e);
            if (e.isDeployed()) {
                    clientgui.bv.centerOnHex(e.getPosition());
            }
            }
    }


    /**
     * Determine if the listener is currently distracted.
     *
     * @return  <code>true</code> if the listener is ignoring events.
     */
    public boolean isIgnoringEvents() {
        return this.distracted.isIgnoringEvents();
    }

    /**
     * Specify if the listener should be distracted.
     *
     * @param   distract <code>true</code> if the listener should ignore events
     *          <code>false</code> if the listener should pay attention again.
     *          Events that occured while the listener was distracted NOT
     *          going to be processed.
     */
    public void setIgnoringEvents( boolean distracted ) {
        this.distracted.setIgnoringEvents( distracted );
    }

    /**
     * Retrieve the "Done" button of this object.
     *
     * @return  the <code>java.awt.Button</code> that activates this
     *          object's "Done" action.
     */
    public Button getDoneButton() {
        return butDone;
    }

    /**
     * Stop just ignoring events and actually stop listening to them.
     */
    public void removeAllListeners() {
        client.game.removeGameListener(this);
        clientgui.getBoardView().removeBoardViewListener(this);
        clientgui.mechD.wPan.weaponList.removeItemListener(this);
    }

}

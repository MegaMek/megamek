/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005,2006 Ben Mazur (bmazur@sev.org)
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
import megamek.client.ui.AWT.widget.IndexedCheckbox;
import megamek.common.*;
import megamek.common.actions.*;
import megamek.common.event.GameListener;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GameTurnChangeEvent;
import megamek.common.util.Distractable;
import megamek.common.util.DistractableAdapter;
import megamek.common.LosEffects;

public class FiringDisplay 
    extends StatusBarPhaseDisplay
    implements BoardViewListener, GameListener, ActionListener, DoneButtoned,
               KeyListener, ItemListener, Distractable
{
    static final long serialVersionUID = 6891920084457830181L;
    // Distraction implementation.
    private DistractableAdapter distracted = new DistractableAdapter();

    private static final int    NUM_BUTTON_LAYOUTS = 2;
    
    // Action command names
    public static final String FIRE_AIM        = "fireAim"; //$NON-NLS-1$
    public static final String FIRE_FIND_CLUB  = "fireFindClub"; //$NON-NLS-1$
    public static final String FIRE_FIRE       = "fireFire"; //$NON-NLS-1$
    public static final String FIRE_MODE       = "fireMode"; //$NON-NLS-1$
    public static final String FIRE_FLIP_ARMS  = "fireFlipArms"; //$NON-NLS-1$
    public static final String FIRE_MORE       = "fireMore"; //$NON-NLS-1$
    public static final String FIRE_NEXT       = "fireNext"; //$NON-NLS-1$
    public static final String FIRE_NEXT_TARG  = "fireNextTarg"; //$NON-NLS-1$
    public static final String FIRE_SKIP       = "fireSkip"; //$NON-NLS-1$
    public static final String FIRE_SPOT       = "fireSpot"; //$NON-NLS-1$
    public static final String FIRE_TWIST      = "fireTwist"; //$NON-NLS-1$
    public static final String FIRE_CANCEL     = "fireCancel"; //$NON-NLS-1$
    public static final String FIRE_SEARCHLIGHT= "fireSearchlight"; //$NON-NLS-1$

    // parent game
    public Client client;
    private ClientGUI clientgui;
    // buttons
    private Container        panButtons;
    
    private Button            butFire;
    private Button            butTwist;
    private Button            butSkip;
    
    private Button            butFindClub;
    private Button            butNextTarg;
    private Button            butFlipArms;
    private Button            butSpot;

    private Button            butSearchlight;
    
//    private Button            butReport;
    private Button            butSpace;
    private Button            butFireMode; // Fire Mode - Add a Fire Mode Button - Rasia
    
    private Button            butNext;
    private Button            butDone;
    private Button            butMore;
    
    private int               buttonLayout;

    // let's keep track of what we're shooting and at what, too
    private int                cen = Entity.NONE;        // current entity number
    private Targetable         target;        // target 

    // HACK : track when we wan to show the target choice dialog.
    private boolean            showTargetChoice = true;
  
    // shots we have so far.
    private Vector attacks;  
  
    // is the shift key held?
    private boolean            shiftheld;
    private boolean            twisting;

    private Entity[]            visibleTargets  = null;
    private int                 lastTargetID    = -1;
    
    private AimedShotHandler   ash;

    /**
     * Creates and lays out a new firing phase display 
     * for the specified client.
     */
    public FiringDisplay(ClientGUI clientgui) {
        this.client = clientgui.getClient();
        this.clientgui = clientgui;
        client.game.addGameListener(this);
        
        clientgui.getBoardView().addBoardViewListener(this);
        
        shiftheld = false;
    
        // fire
        attacks = new Vector();

        setupStatusBar(Messages.getString("FiringDisplay.waitingForFiringPhase")); //$NON-NLS-1$
        
        butFire = new Button(Messages.getString("FiringDisplay.Fire")); //$NON-NLS-1$
        butFire.addActionListener(this);
        butFire.addKeyListener(this);
        butFire.setActionCommand(FIRE_FIRE);
        butFire.setEnabled(false);
        
        butSkip = new Button(Messages.getString("FiringDisplay.Skip")); //$NON-NLS-1$
        butSkip.addActionListener(this);
        butSkip.addKeyListener(this);
        butSkip.setActionCommand(FIRE_SKIP);
        butSkip.setEnabled(false);
        
        butTwist = new Button(Messages.getString("FiringDisplay.Twist")); //$NON-NLS-1$
        butTwist.addActionListener(this);
        butTwist.addKeyListener(this);
        butTwist.setActionCommand(FIRE_TWIST);
        butTwist.setEnabled(false);
        

        butFindClub = new Button(Messages.getString("FiringDisplay.FindClub")); //$NON-NLS-1$
        butFindClub.addActionListener(this);
        butFindClub.addKeyListener(this);
        butFindClub.setActionCommand(FIRE_FIND_CLUB);
        butFindClub.setEnabled(false);
        
        butNextTarg = new Button(Messages.getString("FiringDisplay.NextTarget")); //$NON-NLS-1$
        butNextTarg.addActionListener(this);
        butNextTarg.addKeyListener(this);
        butNextTarg.setActionCommand(FIRE_NEXT_TARG);
        butNextTarg.setEnabled(false);
        
        butFlipArms = new Button(Messages.getString("FiringDisplay.FlipArms")); //$NON-NLS-1$
        butFlipArms.addActionListener(this);
        butFlipArms.addKeyListener(this);
        butFlipArms.setActionCommand(FIRE_FLIP_ARMS);
        butFlipArms.setEnabled(false);
        
        butSpot = new Button(Messages.getString("FiringDisplay.Spot")); //$NON-NLS-1$
        butSpot.addActionListener(this);
        butSpot.addKeyListener(this);
        butSpot.setActionCommand(FIRE_SPOT);
        butSpot.setEnabled(false);
        
        butSearchlight = new Button(Messages.getString("FiringDisplay.Searchlight")); //$NON-NLS-1$
        butSearchlight.addActionListener(this);
        butSearchlight.addKeyListener(this);
        butSearchlight.setActionCommand(FIRE_SEARCHLIGHT);
        butSearchlight.setEnabled(false);

        butSpace = new Button("."); //$NON-NLS-1$
        butSpace.setEnabled(false);

        // Fire Mode - Adding a Fire Mode Button to the 2nd Menu - Rasia
        butFireMode = new Button(Messages.getString("FiringDisplay.Mode")); //$NON-NLS-1$
        butFireMode.addActionListener(this);
        butFireMode.addKeyListener(this);
        butFireMode.setActionCommand(FIRE_MODE);
        butFireMode.setEnabled(false);

        butDone = new Button(Messages.getString("FiringDisplay.Done")); //$NON-NLS-1$
        butDone.addActionListener(this);
        butDone.addKeyListener(this);
        butDone.setEnabled(false);
        
        butNext = new Button(Messages.getString("FiringDisplay.NextUnit")); //$NON-NLS-1$
        butNext.addActionListener(this);
        butNext.addKeyListener(this);
        butNext.setActionCommand(FIRE_NEXT);
        butNext.setEnabled(false);
        
        butMore = new Button(Messages.getString("FiringDisplay.More")); //$NON-NLS-1$
        butMore.addActionListener(this);
        butMore.addKeyListener(this);
        butMore.setActionCommand(FIRE_MORE);
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
        
        clientgui.bv.addKeyListener( this );
        addKeyListener(this);
        
        // mech display.
        clientgui.mechD.wPan.weaponList.addItemListener(this);
        clientgui.mechD.wPan.weaponList.addKeyListener(this);

        ash = new AimedShotHandler();
    
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
            panButtons.add(butTwist);
            panButtons.add(butFireMode);
            panButtons.add(butMore);
//             panButtons.add(butDone);
            break;
        case 1 :
            panButtons.add(butNext);
            panButtons.add(butFire);
            panButtons.add(butFlipArms);
            panButtons.add(butFindClub);
            panButtons.add(butSpot);
            panButtons.add(butSearchlight);
            panButtons.add(butMore);
//             panButtons.add(butDone);
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

            setFindClubEnabled(FindClubAction.canMechFindClub(client.game, en));
            setSpotEnabled(ce().canSpot()
              && client.game.getOptions().booleanOption("indirect_fire")); //$NON-NLS-1$
            setFlipArmsEnabled(ce().canFlipArms());
            updateSearchlight();
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
            butMore.setEnabled(true);
            setFireModeEnabled(true); // Fire Mode - Setting Fire Mode to true, currently doesn't detect if weapon has a special Fire Mode or not- Rasia        client.setDisplayVisible(true);
            clientgui.getBoardView().select(null);
        }
    }
    
    /**
     * Does end turn stuff.
     */
    private void endMyTurn() {
        // end my turn, then.
        Entity next = client.game.getNextEntity( client.game.getTurnIndex() );
        if ( IGame.PHASE_FIRING == client.game.getPhase()
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
        
        clearVisibleTargets();
    }
    
    /**
     * Disables all buttons in the interface
     */
    private void disableButtons() {
        setFireEnabled(false);
        setSkipEnabled(false);
        setTwistEnabled(false);
        setSpotEnabled(false);
        setFindClubEnabled(false);
        butMore.setEnabled(false);
        setNextEnabled(false);
        butDone.setEnabled(false);
        setNextTargetEnabled(false);
        setFlipArmsEnabled(false);
        setFireModeEnabled(false); // Fire Mode - Handlng of Fire Mode Button - Rasia
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
     * Called when the current entity is done firing.  Send out our attack
     * queue to the server.
     */
    private void ready() {
        if (attacks.isEmpty() && GUIPreferences.getInstance().getNagForNoAction()) {
            // comfirm this action
            String title = Messages.getString("FiringDisplay.DontFireDialog.title"); //$NON-NLS-1$
            String body = Messages.getString("FiringDisplay.DontFireDialog.message"); //$NON-NLS-1$
            ConfirmDialog response = clientgui.doYesNoBotherDialog(title, body);
            if ( !response.getShowAgain() ) {
                GUIPreferences.getInstance().setNagForNoAction(false);
            }
            if ( !response.getAnswer() ) {
                return;
            }
        }
        // auto spot if we can and the option is set
        if (attacks.isEmpty() && 
            client.game.getOptions().booleanOption("auto_spot") && //$NON-NLS-1$
                client.game.getPhase() == IGame.PHASE_FIRING) {
            if (!ce().isINarcedWith( INarcPod.HAYWIRE)) {
                // if we might do physicals, ask for confirmation
                if (ce().isEligibleForPhysical()) {
                    doSpot();
                // else, spot without asking
                } else {
                    attacks.addElement(new SpotAction(cen));
                }
            }
        }

        // stop further input (hopefully)
        disableButtons();

        // remove temporary attacks from game & board
        removeTempAttacks();
        
        // For bug 1002223
        // Re-compute the to-hit numbers by adding in correct order.
        Vector newAttacks = new Vector();
        for (Enumeration e=attacks.elements(); e.hasMoreElements();) {
            Object o = e.nextElement();
            if (o instanceof WeaponAttackAction) {
                WeaponAttackAction waa = (WeaponAttackAction)o;
                Entity attacker = waa.getEntity(client.game);
                Targetable target = waa.getTarget(client.game);
                boolean curInFrontArc = Compute.isInArc(attacker.getPosition(), attacker.getSecondaryFacing(), target.getPosition(), Compute.ARC_FORWARD);
                if (curInFrontArc) {
                    WeaponAttackAction waa2 = new WeaponAttackAction(waa.getEntityId(), waa.getTargetType(), waa.getTargetId(), waa.getWeaponId());
                    waa2.setAimedLocation(waa.getAimedLocation());
                    waa2.setAimingMode(waa.getAimingMode());
                    waa2.setOtherAttackInfo(waa.getOtherAttackInfo());
                    newAttacks.addElement(waa2);
                }
            } else {
                newAttacks.addElement(o);
            }
        }
        for (Enumeration e=attacks.elements(); e.hasMoreElements();) {
            Object o = e.nextElement();
            if (o instanceof WeaponAttackAction) {
                WeaponAttackAction waa = (WeaponAttackAction) o;
                Entity attacker = waa.getEntity(client.game);
                Targetable target = waa.getTarget(client.game);
                boolean curInFrontArc = Compute.isInArc(attacker.getPosition(), attacker.getSecondaryFacing(), target.getPosition(), Compute.ARC_FORWARD);
                if (!curInFrontArc) {
                    WeaponAttackAction waa2 = new WeaponAttackAction(waa.getEntityId(), waa.getTargetType(), waa.getTargetId(), waa.getWeaponId() );
                    waa2.setAimedLocation(waa.getAimedLocation());
                    waa2.setAimingMode(waa.getAimingMode());
                    waa2.setOtherAttackInfo(waa.getOtherAttackInfo());
                    newAttacks.addElement(waa2);
                }
            }
        }
        
        // send out attacks
        client.sendAttackData(cen, newAttacks);
        
        // clear queue
        attacks.removeAllElements();

        // Clear the menu bar.
        clientgui.getMenuBar().setEntity( null );

        // close aimed shot display, if any
        ash.closeDialog();
        ash.lockLocation(false);
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
            AmmoType ammoType = (AmmoType)ammoMount.getType();
            waa.setAmmoId(ce().getEquipmentNum(ammoMount));
            if ( ((ammoType.getMunitionType() == AmmoType.M_THUNDER_VIBRABOMB)
                && (ammoType.getAmmoType() == AmmoType.T_LRM))
                 || ammoType.getMunitionType() == AmmoType.M_VIBRABOMB_IV)
                 
            {
                VibrabombSettingDialog vsd  =
                    new VibrabombSettingDialog( clientgui.frame );
                vsd.show();
                waa.setOtherAttackInfo(vsd.getSetting());
            }
        }

        if (ash.allowAimedShotWith(mounted) &&
            ash.inAimingMode() && 
            ash.isAimingAtLocation()) {
            waa.setAimedLocation(ash.getAimingAt());
            waa.setAimingMode(ash.getAimingMode());
            if (ash.getAimingMode() == IAimingModes.AIM_MODE_TARG_COMP) {
                ash.lockLocation(true);
            }
        } else {
            waa.setAimedLocation(Mech.LOC_NONE);
            waa.setAimingMode(IAimingModes.AIM_MODE_NONE);
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
   * The entity spends the rest of its turn finding a club
   */
  private void findClub() {
    if (ce() == null) {
      return;
    }
        
    // comfirm this action
    String title = Messages.getString("FiringDisplay.FindClubDialog.title"); //$NON-NLS-1$
    String body = Messages.getString("FiringDisplay.FindClubDialog.message"); //$NON-NLS-1$
    if (!clientgui.doYesNoDialog(title, body)) {
      return;
        }
    
        attacks.removeAllElements();
        attacks.addElement(new FindClubAction(cen));
        
        ready();
  }
  
    /**
     * The entity spends the rest of its turn spotting
     */
    private void doSpot() {
        if (ce() == null) {
            return;
        }
        
        if (ce().isINarcedWith( INarcPod.HAYWIRE )) {
            String title = Messages.getString("FiringDisplay.CantSpotDialog.title"); //$NON-NLS-1$
            String body = Messages.getString("FiringDisplay.CantSpotDialog.message"); //$NON-NLS-1$
            clientgui.doAlertDialog(title, body);
            return;
        }
    
        // comfirm this action
        String title = Messages.getString("FiringDisplay.SpotForInderectDialog.title"); //$NON-NLS-1$
        String body = Messages.getString("FiringDisplay.SpotForInderectDialog.message"); //$NON-NLS-1$
        if (!clientgui.doYesNoDialog(title, body)) {
            return;
        }

        attacks.removeAllElements();
        attacks.addElement(new SpotAction(cen));

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
        ash.lockLocation(false);
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
     * removes the last action
     */
    private void removeLastFiring() {
        Object o = attacks.lastElement();
        if (o instanceof WeaponAttackAction) {
            WeaponAttackAction waa = (WeaponAttackAction)o;
            ce().getEquipment(waa.getWeaponId()).setUsedThisRound(false);
            attacks.removeElement(o);
            clientgui.mechD.wPan.displayMech(ce());
            client.game.removeAction(o);
            clientgui.bv.refreshAttacks();
            clientgui.bv.repaint(100);
            clientgui.minimap.drawMap();
        }
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
        ash.setAimingMode();
        updateTarget();
        ash.showDialog();
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
        if (target != null && weaponId != -1 && ce() != null && !ce().usedTag()) {
            ToHitData toHit;
            if (ash.inAimingMode()) {
            Mounted weapon = ce().getEquipment(weaponId);
              boolean aiming = ash.isAimingAtLocation() && 
                      ash.allowAimedShotWith(weapon);
              ash.setEnableAll(aiming);
              if (aiming) {
                toHit = WeaponAttackAction.toHit(client.game, cen, target, weaponId, ash.getAimingAt(), ash.getAimingMode());
                clientgui.mechD.wPan.wTargetR.setText(target.getDisplayName() + " (" + ash.getAimingLocation() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
              } else {
                toHit = WeaponAttackAction.toHit(client.game, cen, target, weaponId, Mech.LOC_NONE, IAimingModes.AIM_MODE_NONE);
                clientgui.mechD.wPan.wTargetR.setText(target.getDisplayName());
              }
              ash.setPartialCover(toHit.getCover());
            } else {
              toHit = WeaponAttackAction.toHit(client.game, cen, target, weaponId, Mech.LOC_NONE, IAimingModes.AIM_MODE_NONE);
              clientgui.mechD.wPan.wTargetR.setText(target.getDisplayName());
            }
            clientgui.mechD.wPan.wRangeR.setText("" + ce().getPosition().distance(target.getPosition())); //$NON-NLS-1$
            Mounted m = ce().getEquipment(weaponId);
            if (m.isUsedThisRound()) {
                clientgui.mechD.wPan.wToHitR.setText(Messages.getString("FiringDisplay.alreadyFired")); //$NON-NLS-1$
                setFireEnabled(false);
            } else if (m.getType().hasFlag(WeaponType.F_AUTO_TARGET)) {
                clientgui.mechD.wPan.wToHitR.setText(Messages.getString("FiringDisplay.autoFiringWeapon")); //$NON-NLS-1$
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
            // HACK : sometimes we don't show the target choice window
            Targetable targ = null;
            if (this.showTargetChoice)
                targ = this.chooseTarget( b.getCoords() );
            if (shiftheld) {
                updateFlipArms(false);
                torsoTwist(b.getCoords());
            } else if ( targ != null ) {
                target( targ );
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

        if(client.game.getPhase() == IGame.PHASE_FIRING) {
            endMyTurn();
            
            if(client.isMyTurn()) {
                beginMyTurn();
                setStatusBarText(Messages.getString("FiringDisplay.its_your_turn")); //$NON-NLS-1$
            } else {                
                setStatusBarText(Messages.getString("FiringDisplay.its_others_turn", new Object[]{e.getPlayer().getName()})); //$NON-NLS-1$
            }
        }
    }
    public void gamePhaseChange(GamePhaseChangeEvent e) {

        // Are we ignoring events?
        if ( this.isIgnoringEvents() ) {
            return;
        }

        if(client.isMyTurn() && client.game.getPhase() != IGame.PHASE_FIRING) {
            endMyTurn();
        }
        // if we're ending the firing phase, unregister stuff.
        if(client.game.getPhase() ==  IGame.PHASE_FIRING) {
            setStatusBarText(Messages.getString("FiringDisplay.waitingForFiringPhase")); //$NON-NLS-1$
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
        } else if (ev.getActionCommand().equals(FIRE_MORE)) {
            buttonLayout++;
            buttonLayout %= NUM_BUTTON_LAYOUTS;
            setupButtonPanel();
        } else if (ev.getActionCommand().equals(FIRE_FIND_CLUB)) {
            findClub();
        } else if (ev.getActionCommand().equals(FIRE_SPOT)) {
            doSpot();
        } else if (ev.getActionCommand().equals(FIRE_NEXT_TARG)) {
            jumpToNextTarget();
        } else if (ev.getActionCommand().equals(FIRE_FLIP_ARMS)) {
            updateFlipArms(!ce().getArmsFlipped());
        // Fire Mode - More Fire Mode button handling - Rasia
        } else if (ev.getActionCommand().equals(FIRE_MODE)) {
            changeMode();
        } else if ((ev.getActionCommand().equalsIgnoreCase("changeSinks"))
                || (ev.getActionCommand().equals(FIRE_CANCEL))) {
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
    private void setFindClubEnabled(boolean enabled) {
        butFindClub.setEnabled(enabled);
        clientgui.getMenuBar().setFireFindClubEnabled(enabled);
    }
    private void setNextTargetEnabled(boolean enabled) {
        butNextTarg.setEnabled(enabled);
        clientgui.getMenuBar().setFireNextTargetEnabled(enabled);
    }
    private void setFlipArmsEnabled(boolean enabled) {
        butFlipArms.setEnabled(enabled);
        clientgui.getMenuBar().setFireFlipArmsEnabled(enabled);
    }
    private void setSpotEnabled(boolean enabled) {
        butSpot.setEnabled(enabled);
        clientgui.getMenuBar().setFireSpotEnabled(enabled);
    }
    private void setSearchlightEnabled(boolean enabled) {
        butSearchlight.setEnabled(enabled);
        clientgui.getMenuBar().setFireSearchlightEnabled(enabled);
    }
    private void setFireModeEnabled(boolean enabled) {
        butFireMode.setEnabled(enabled);
        clientgui.getMenuBar().setFireModeEnabled(enabled);
    }
    private void setNextEnabled(boolean enabled) {
        butNext.setEnabled(enabled);
        clientgui.getMenuBar().setFireNextEnabled(enabled);
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
        if (ev.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
            if (client.isMyTurn()) {
                removeLastFiring();
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

  private class AimedShotHandler implements ActionListener, ItemListener {  
    private int aimingAt = Mech.LOC_NONE;
    private int aimingMode = IAimingModes.AIM_MODE_NONE;
    private int partialCover = LosEffects.COVER_NONE;

    private boolean lockedLocation = false;
    private int lockedLoc = Mech.LOC_NONE;
    private Targetable lockedTarget = null;

    private AimedShotDialog asd;

    public AimedShotHandler() {
    }

    public void showDialog() {
      if (asd != null) {
        int oldAimingMode = aimingMode;
        closeDialog();
        aimingMode = oldAimingMode;
      }

      if (inAimingMode()) {
        String[] options;

        if (target instanceof BipedMech) {
          options = BipedMech.LOCATION_NAMES;
        } else {
          options = QuadMech.LOCATION_NAMES;
        }
        boolean[] enabled = createEnabledMask(options.length);

        if (aimingMode == IAimingModes.AIM_MODE_IMMOBILE) {
          aimingAt = Mech.LOC_HEAD;
        } else if (aimingMode == IAimingModes.AIM_MODE_TARG_COMP) {
          aimingAt = Mech.LOC_CT;
        }
        if (lockedLocation) {
          aimingAt = lockedLoc;
        }
        asd = new AimedShotDialog(clientgui.frame,
                      Messages.getString("FiringDisplay.AimedShotDialog.title"), //$NON-NLS-1$
                      Messages.getString("FiringDisplay.AimedShotDialog.message"), //$NON-NLS-1$
                      options,
                      enabled,
                      aimingAt,
                      lockedLocation,
                      this,
                      this);

        asd.show();
        updateTarget();
      }
    }

    private boolean[] createEnabledMask(int length) {
      boolean[] mask = new boolean[length];

      for (int i = 0; i < length; i++) {
        mask[i] = true;
      }

      int side = Compute.targetSideTable(ce(), target);

      // remove locations hidden by partial cover
      if(side == ToHitData.SIDE_FRONT) {
        if ((partialCover & LosEffects.COVER_LOWLEFT) != 0) mask[Mech.LOC_RLEG] = false;
        if ((partialCover & LosEffects.COVER_LOWRIGHT) != 0) mask[Mech.LOC_LLEG] = false;
        if ((partialCover & LosEffects.COVER_LEFT) != 0) {
            mask[Mech.LOC_RARM] = false;
            mask[Mech.LOC_RT] = false;
        }
        if ((partialCover & LosEffects.COVER_RIGHT) != 0) {
            mask[Mech.LOC_LARM] = false;
            mask[Mech.LOC_LT] = false;
        }
      } else {
        if ((partialCover & LosEffects.COVER_LOWLEFT) != 0) mask[Mech.LOC_LLEG] = false;
        if ((partialCover & LosEffects.COVER_LOWRIGHT) != 0) mask[Mech.LOC_RLEG] = false;
        if ((partialCover & LosEffects.COVER_LEFT) != 0) {
            mask[Mech.LOC_LARM] = false;
            mask[Mech.LOC_LT] = false;
        }
        if ((partialCover & LosEffects.COVER_RIGHT) != 0) {
            mask[Mech.LOC_RARM] = false;
            mask[Mech.LOC_RT] = false;
        }
      }

      if (aimingMode == IAimingModes.AIM_MODE_TARG_COMP) {
        // Can't target head with Clan targeting computer.
        mask[Mech.LOC_HEAD] = false;

        switch (side) {
          case (ToHitData.SIDE_RIGHT) :
          // Can't target left side when on the right
          // with Clan targeting computer.
          mask[Mech.LOC_LARM] = false;
          mask[Mech.LOC_LT] = false;
          mask[Mech.LOC_LLEG] = false;
          break;
          case (ToHitData.SIDE_LEFT) :
          // Can't target right side when on the left
          // with Clan targeting computer.
          mask[Mech.LOC_RARM] = false;
          mask[Mech.LOC_RT] = false;
          mask[Mech.LOC_RLEG] = false;
          break;
        }
      }
      return mask;
    }

    public void closeDialog() {
      if (asd != null) {
        aimingAt = Mech.LOC_NONE;
        aimingMode = IAimingModes.AIM_MODE_NONE;
        asd.hide();
        asd = null;
        updateTarget();
      }
    }

    // Enables the radiobuttons in the dialog.    
    public void setEnableAll(boolean enableAll) {
      if (asd != null && !lockedLocation) {
        asd.setEnableAll(enableAll);
      }
    }

    // All aimed shots with a targeting computer
    // must be at the same location.
    public void lockLocation(boolean lock) {
      if (lock) {
        lockedTarget = target;
        lockedLoc = aimingAt;
        setEnableAll(false);
        lockedLocation = true;
      } else {
        lockedTarget = null;
        lockedLoc = Mech.LOC_NONE;
        lockedLocation = false;
        setEnableAll(true);
      }
    }

    public void setPartialCover(int partialCover) {
      this.partialCover = partialCover;
    }

    public int getAimingAt() {
      return aimingAt;
    }

    public int getAimingMode() {
      return aimingMode;
    }

    // Returns the name of aimed location.
    public String getAimingLocation() {
      if ((aimingAt != Mech.LOC_NONE) && (aimingMode != IAimingModes.AIM_MODE_NONE)) {
        if (target != null && target instanceof BipedMech) {
          return BipedMech.LOCATION_NAMES[aimingAt];
        } else if (target != null && target instanceof BipedMech){
          return QuadMech.LOCATION_NAMES[aimingAt];
        }
      }
      return null;
    }

    // Sets the aiming mode, depending on the target and
    // the attacker. Against immobile mechs, targeting
    // computer aiming mode will be used if turned on.  
    // (This is a hack, but it's the resolution suggested
    // by the bug submitter, and I don't think it's half 
    // bad.
    public void setAimingMode() {
      boolean allowAim;
      
      allowAim = ((target != null) && ce().hasAimModeTargComp() && target instanceof Mech);
      if (allowAim) {
        if (lockedLocation) {
          allowAim = ((Entity)target).equals(lockedTarget);
          if (allowAim) {
            aimingMode = IAimingModes.AIM_MODE_TARG_COMP;
            return;
          }
        } else {
          aimingMode = IAimingModes.AIM_MODE_TARG_COMP;
          return;
        }
      }
      allowAim = ((target != null) && target.isImmobile() && target instanceof Mech);
      if (allowAim) {
        aimingMode = IAimingModes.AIM_MODE_IMMOBILE;
        return;
      }
      aimingMode = IAimingModes.AIM_MODE_NONE;
    }

    // If in aiming mode.
    public boolean inAimingMode() {
      return aimingMode != IAimingModes.AIM_MODE_NONE;
    }

    // If a hit location is currently selected.
    public boolean isAimingAtLocation() {
      return aimingAt != Mech.LOC_NONE;
    }

    // Determines if a certain weapon may aimed at a specific
    // hit location.
    public boolean allowAimedShotWith(Mounted weapon) {
      WeaponType wtype = (WeaponType)weapon.getType();
      boolean isWeaponInfantry = wtype.hasFlag(WeaponType.F_INFANTRY);
      boolean usesAmmo = wtype.getAmmoType() != AmmoType.T_NA &&
        wtype.getAmmoType() != AmmoType.T_BA_MG &&
        wtype.getAmmoType() != AmmoType.T_BA_SMALL_LASER &&
      !isWeaponInfantry;
      Mounted ammo = usesAmmo ? weapon.getLinked() : null;
      AmmoType atype = ammo == null ? null : (AmmoType)ammo.getType();

      // Leg and swarm attacks can't be aimed.
      if (wtype.getInternalName() == Infantry.LEG_ATTACK ||
          wtype.getInternalName() == Infantry.SWARM_MEK) {
          return false;
      }
      switch (aimingMode) {
        case (IAimingModes.AIM_MODE_NONE) :
        return false;
        case (IAimingModes.AIM_MODE_IMMOBILE) :
        if (atype != null) {
            switch (atype.getAmmoType()) {
              case (AmmoType.T_SRM_STREAK) :
              case (AmmoType.T_LRM_STREAK) :
              case (AmmoType.T_LRM) :
              case (AmmoType.T_LRM_TORPEDO) :
              case (AmmoType.T_SRM) :
              case (AmmoType.T_SRM_TORPEDO) :
              case (AmmoType.T_MRM) :
              case (AmmoType.T_NARC) :
              case (AmmoType.T_AMS) :
              case (AmmoType.T_ARROW_IV) :
              case (AmmoType.T_LONG_TOM) :
              case (AmmoType.T_SNIPER) :
              case (AmmoType.T_THUMPER) :
              case (AmmoType.T_SRM_ADVANCED) :
              case (AmmoType.T_BA_INFERNO) :
              case (AmmoType.T_LRM_TORPEDO_COMBO) :
              case (AmmoType.T_ATM) :
              return false;
            }
            if ((atype.getAmmoType() == AmmoType.T_AC_LBX)
                    && (atype.getMunitionType() == AmmoType.M_CLUSTER))
                return false;
          }
          break;
          case (IAimingModes.AIM_MODE_TARG_COMP) :
          if (!wtype.hasFlag(WeaponType.F_DIRECT_FIRE)) {
            return false;
          }
          if ((atype != null)
                && (atype.getAmmoType() == AmmoType.T_AC_LBX)
                && (atype.getMunitionType() == AmmoType.M_CLUSTER)) {
            return false;
          }
          break;
        }
        return true;
      }

    // ActionListener, listens to the button in the dialog.
    public void actionPerformed(ActionEvent ev) {
        closeDialog();
    }

    // ItemListener, listens to the radiobuttons in the dialog.
    public void itemStateChanged(ItemEvent ev) {
      IndexedCheckbox icb = (IndexedCheckbox) ev.getSource();
      aimingAt = icb.getIndex();
      updateTarget();
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
    
    /**
     * Have the player select a target from the entities at the given coords.
     *
     * @param   pos - the <code>Coords</code> containing targets.
     */
    private Targetable chooseTarget( Coords pos ) {
          
        boolean friendlyFire = client.game.getOptions().booleanOption("friendly_fire"); //$NON-NLS-1$
        // Assume that we have *no* choice.
        Targetable choice = null;
        Enumeration choices = null;
        
        // Get the available choices, depending on friendly fire
        if (friendlyFire) {
            choices = client.game.getEntities( pos );
        } else choices = client.game.getEnemyEntities(pos, ce() );
        
        // Convert the choices into a List of targets.
        Vector targets = new Vector();
        while ( choices.hasMoreElements() ) {
            choice = (Targetable) choices.nextElement();
            if ( !ce().equals( choice ) ) {
                targets.addElement( choice );
            }
        }

        // Is there a building in the hex?
        Building bldg = client.game.getBoard().getBuildingAt( pos );
        if ( bldg != null ) {
            targets.addElement( new BuildingTarget(pos, client.game.getBoard(), false) );
        }

        // Do we have a single choice?
        if ( targets.size() == 1 ) {

            // Return  that choice.
            choice = (Targetable) targets.elementAt( 0 );

        }

        // If we have multiple choices, display a selection dialog.
        else if ( targets.size() > 1 ) {
            String[] names = new String[ targets.size() ];
            for ( int loop = 0; loop < names.length; loop++ ) {
                names[loop] = ( (Targetable)targets.elementAt(loop) )
                    .getDisplayName();
            }
            SingleChoiceDialog choiceDialog =
                new SingleChoiceDialog( clientgui.frame,
                                        Messages.getString("FiringDisplay.ChooseTargetDialog.title"), //$NON-NLS-1$
                                        Messages.getString("FiringDisplay.ChooseTargetDialog.message", new Object[]{pos.getBoardNum()}), //$NON-NLS-1$ 
                                        names );
            choiceDialog.show();
            if ( choiceDialog.getAnswer() == true ) {
                choice = (Targetable) targets.elementAt
                    ( choiceDialog.getChoice() );
            }
        } // End have-choices

        // Return the chosen unit.
        return choice;

    } // End private Targetable chooseTarget( Coords )

}

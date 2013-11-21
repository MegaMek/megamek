/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import megamek.client.event.BoardViewEvent;
import megamek.client.ui.Messages;
import megamek.client.ui.SharedUtility;
import megamek.client.ui.swing.widget.IndexedRadioButton;
import megamek.client.ui.swing.widget.MegamekButton;
import megamek.common.Aero;
import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.BombType;
import megamek.common.Building;
import megamek.common.BuildingTarget;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.FighterSquadron;
import megamek.common.GameTurn;
import megamek.common.GunEmplacement;
import megamek.common.IAimingModes;
import megamek.common.IGame;
import megamek.common.IGame.Phase;
import megamek.common.INarcPod;
import megamek.common.LargeSupportTank;
import megamek.common.LosEffects;
import megamek.common.Mech;
import megamek.common.Mounted;
import megamek.common.Protomech;
import megamek.common.SuperHeavyTank;
import megamek.common.Tank;
import megamek.common.TargetRoll;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.actions.AbstractEntityAction;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.EntityAction;
import megamek.common.actions.FindClubAction;
import megamek.common.actions.FlipArmsAction;
import megamek.common.actions.RepairWeaponMalfunctionAction;
import megamek.common.actions.SearchlightAttackAction;
import megamek.common.actions.SpotAction;
import megamek.common.actions.TorsoTwistAction;
import megamek.common.actions.TriggerAPPodAction;
import megamek.common.actions.TriggerBPodAction;
import megamek.common.actions.UnjamTurretAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GameTurnChangeEvent;

public class FiringDisplay extends StatusBarPhaseDisplay implements
        KeyListener, ItemListener, ListSelectionListener {
    /**
     *
     */
    private static final long serialVersionUID = -5586388490027013723L;

    /**
     * This enumeration lists all of the possible ActionCommands that can be
     * carried out during the firing phase.  Each command has a string for the
     * command plus a flag that determines what unit type it is appropriate for.
     * @author walczak
     *
     */
    public static enum Command {
    	FIRE_NEXT("fireNext"),
    	FIRE_TWIST("fireTwist"),
    	FIRE_FIRE("fireFire"),
    	FIRE_SKIP("fireSkip"),    	
    	FIRE_NEXT_TARG("fireNextTarg"),    	
    	FIRE_MODE("fireMode"),
    	FIRE_SPOT("fireSpot"),
    	FIRE_FLIP_ARMS("fireFlipArms"),    	
    	FIRE_FIND_CLUB("fireFindClub"),    	
    	FIRE_SEARCHLIGHT("fireSearchlight"),
    	FIRE_CLEAR_TURRET("fireClearTurret"),
    	FIRE_CLEAR_WEAPON("fireClearWeaponJam"),
    	FIRE_CALLED("fireCalled"),
    	FIRE_CANCEL("fireCancel"),
    	FIRE_MORE("fireMore");    	
    
	    String cmd;
	    private Command(String c){
	    	cmd = c;
	    }
	    
	    public String getCmd(){
	    	return cmd;
	    }
	    
	    public String toString(){
	    	return cmd;
	    }
    }

    // buttons
    protected Hashtable<Command,MegamekButton> buttons;

    // let's keep track of what we're shooting and at what, too
    private int cen = Entity.NONE; // current entity number

    Targetable target; // target

    // HACK : track when we wan to show the target choice dialog.
    private boolean showTargetChoice = true;

    // shots we have so far.
    private Vector<AbstractEntityAction> attacks;

    // is the shift key held?
    private boolean shiftheld;

    private boolean twisting;

    private Entity[] visibleTargets = null;

    private int lastTargetID = -1;

    private AimedShotHandler ash;

    /**
     * Creates and lays out a new firing phase display for the specified
     * clientgui.getClient().
     */
    public FiringDisplay(ClientGUI clientgui) {
        this.clientgui = clientgui;
        clientgui.getClient().getGame().addGameListener(this);

        clientgui.getBoardView().addBoardViewListener(this);

        shiftheld = false;

        // fire
        attacks = new Vector<AbstractEntityAction>();

        setupStatusBar(Messages
                .getString("FiringDisplay.waitingForFiringPhase")); //$NON-NLS-1$

        buttons = new Hashtable<Command, MegamekButton>(
				(int) (Command.values().length * 1.25 + 0.5));
		for (Command cmd : Command.values()) {
			String title = Messages.getString("FiringDisplay."
					+ cmd.getCmd());
			MegamekButton newButton = new MegamekButton(title,"PhaseDisplayButton");
			newButton.addActionListener(this);
			newButton.setActionCommand(cmd.getCmd());
			newButton.setEnabled(false);
			buttons.put(cmd, newButton);
		}  		
		numButtonGroups = 
        		(int)Math.ceil((buttons.size()+0.0) / buttonsPerGroup);
        
		butDone.setText("<html><b>"+Messages.getString(
        		"FiringDisplay.Done")+"</b></html>"); //$NON-NLS-1$
        butDone.setEnabled(false);
		
        layoutScreen();
        
        setupButtonPanel();

        clientgui.bv.addKeyListener(this);
        addKeyListener(this);

        // mech display.
        clientgui.mechD.wPan.weaponList.addListSelectionListener(this);
        clientgui.mechD.wPan.weaponList.addKeyListener(this);

        ash = new AimedShotHandler();
    }

    protected ArrayList<MegamekButton> getButtonList(){                
        ArrayList<MegamekButton> buttonList = new ArrayList<MegamekButton>(); 
        int i = 0;
        for (Command cmd : Command.values()){
        	if (cmd == Command.FIRE_NEXT || cmd == Command.FIRE_MORE || 
        			cmd == Command.FIRE_CANCEL){
        		continue;
        	}
        	if (i % buttonsPerGroup == 0){
        		buttonList.add(buttons.get(Command.FIRE_NEXT));
        		i++;
        	}
        	
            buttonList.add(buttons.get(cmd));
            i++;
            
            if ((i+1) % buttonsPerGroup == 0){
        		buttonList.add(buttons.get(Command.FIRE_MORE));
        		i++;
        	}
        }
        if (!buttonList.get(i-1).getActionCommand().
        		equals(Command.FIRE_MORE.getCmd())){
	        while ((i+1) % buttonsPerGroup != 0){
	        	buttonList.add(null);
	        	i++;	        	
	        }
	        buttonList.add(buttons.get(Command.FIRE_MORE));
        }
        return buttonList;
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

        if (clientgui.getClient().getGame().getEntity(en) != null) {

            cen = en;
            clientgui.setSelectedEntityNum(en);
            clientgui.mechD.displayEntity(ce());

            // If the selected entity is not on the board, use the next one.
            // ASSUMPTION: there will always be *at least one* entity on map.
            if (ce().getPosition() == null) {

                // Walk through the list of entities for this player.
                for (int nextId = clientgui.getClient().getNextEntityNum(en); nextId != en; nextId = clientgui
                        .getClient().getNextEntityNum(nextId)) {

                    if (clientgui.getClient().getGame().getEntity(nextId)
                            .getPosition() != null) {
                        cen = nextId;
                        break;
                    }

                } // Check the player's next entity.

                // We were *supposed* to have found an on-board entity.
                if (ce().getPosition() == null) {
                    System.err
                            .println("FiringDisplay: could not find an on-board entity: " + //$NON-NLS-1$
                                    en);
                    return;
                }

            } // End ce()-not-on-board

            int lastTarget = ce().getLastTarget();
            if (ce() instanceof Mech) {
                int grapple = ((Mech) ce()).getGrappled();
                if (grapple != Entity.NONE) {
                    lastTarget = grapple;
                }
            }
            Entity t = clientgui.getClient().getGame().getEntity(lastTarget);
            target(t);

            if (!ce().isOffBoard()) {
                clientgui.getBoardView().highlight(ce().getPosition());
            }
            clientgui.getBoardView().select(null);
            clientgui.getBoardView().cursor(null);

            refreshAll();
            cacheVisibleTargets();

            if (!clientgui.bv.isMovingUnits() && !ce().isOffBoard()) {
                clientgui.bv.centerOnHex(ce().getPosition());
            }

            // Update the menu bar.
            clientgui.getMenuBar().setEntity(ce());

            // only twist if crew conscious
            setTwistEnabled(ce().canChangeSecondaryFacing()
                    && ce().getCrew().isActive());

            setFindClubEnabled(FindClubAction.canMechFindClub(
                    clientgui.getClient().getGame(), en));
            setFlipArmsEnabled(ce().canFlipArms());
            updateSearchlight();
            updateClearTurret();
            updateClearWeaponJam();
        } else {
            System.err
                    .println("FiringDisplay: tried to select non-existant entity: " + en); //$NON-NLS-1$
        }

        setFiringSolutions();
    }

    public void setFiringSolutions() {
        IGame game = clientgui.getClient().getGame();
        if (!GUIPreferences.getInstance().getFiringSolutions()){
        	return;
        }
        Hashtable<Integer,ToHitData> fs = new Hashtable<Integer,ToHitData>(); 
        for (Entity target : game.getEntitiesVector()){
            int ownerId = ce().getOwnerId();
            boolean friendlyFire = game.getOptions().booleanOption(
                    "friendly_fire"); //$NON-NLS-1$
            if ((target.getId() != cen)
                    && (friendlyFire || (target.getOwnerId() != ownerId))
                    && target.isTargetable()) {
                ToHitData thd = WeaponAttackAction.toHit(game, cen, target);
                thd.setLocation(target.getPosition());
                thd.setRange(ce().getPosition().distance(target.getPosition()));
                fs.put(target.getId(), thd);
            }
        }
        clientgui.getBoardView().setFiringSolutions(ce(), fs);
    }

    /**
     * Does turn start stuff
     */
    private void beginMyTurn() {
        target = null;

        if (!clientgui.bv.isMovingUnits()) {
            clientgui.setDisplayVisible(true);
        }

        selectEntity(clientgui.getClient().getFirstEntityNum());

        GameTurn turn = clientgui.getClient().getMyTurn();
        // There's special processing for triggering AP Pods.
        if ((turn instanceof GameTurn.TriggerAPPodTurn) && (ce() != null)) {
            disableButtons();
            TriggerAPPodDialog dialog = new TriggerAPPodDialog(
                    clientgui.getFrame(), ce());
            dialog.setVisible(true);
            attacks.removeAllElements();
            Enumeration<TriggerAPPodAction> actions = dialog.getActions();
            while (actions.hasMoreElements()) {
                attacks.addElement(actions.nextElement());
            }
            ready();
        } else if ((turn instanceof GameTurn.TriggerBPodTurn) && (null != ce())) {
            disableButtons();
            TriggerBPodDialog dialog = new TriggerBPodDialog(clientgui, ce(),
                    ((GameTurn.TriggerBPodTurn) turn).getAttackType());
            dialog.setVisible(true);
            attacks.removeAllElements();
            Enumeration<TriggerBPodAction> actions = dialog.getActions();
            while (actions.hasMoreElements()) {
                attacks.addElement(actions.nextElement());
            }
            ready();
        } else {
            setNextEnabled(true);
            butDone.setEnabled(true);
            buttons.get(Command.FIRE_MORE).setEnabled(true);
            setFireCalledEnabled(clientgui.getClient().getGame().getOptions()
                    .booleanOption("tacops_called_shots"));
            clientgui.getBoardView().select(null);
        }
    }

    /**
     * Does end turn stuff.
     */
    private void endMyTurn() {
        // end my turn, then.
        Entity next = clientgui.getClient().getGame().getNextEntity(clientgui
                .getClient().getGame().getTurnIndex());
        if ((clientgui.getClient().getGame().getPhase() == IGame.Phase.PHASE_FIRING)
                && (next != null) && (ce() != null)
                && (next.getOwnerId() != ce().getOwnerId())) {
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
        buttons.get(Command.FIRE_MORE).setEnabled(false);
        setNextEnabled(false);
        butDone.setEnabled(false);
        setNextTargetEnabled(false);
        setFlipArmsEnabled(false);
        setFireModeEnabled(false);
        setFireCalledEnabled(false);
        setFireClearTurretEnabled(false);
        setFireClearWeaponJamEnabled(false);
    }

    /**
     * Fire Mode - Adds a Fire Mode Change to the current Attack Action
     */
    private void changeMode() {
        int wn = clientgui.mechD.wPan.getSelectedWeaponNum();

        // Do nothing we have no unit selected.
        if (ce() == null) {
            return;
        }

        // If the weapon does not have modes, just exit.
        Mounted m = ce().getEquipment(wn);
        if ((m == null) || !m.getType().hasModes()) {
            return;
        }

        // Aeros cannot switch modes under standard rules
        /*
         * if (ce() instanceof Aero) { return; }
         */

        // send change to the server
        int nMode = m.switchMode();
        clientgui.getClient().sendModeChange(cen, wn, nMode);

        // notify the player
        if (m.canInstantSwitch(nMode)) {
            clientgui
                    .systemMessage(Messages
                            .getString(
                                    "FiringDisplay.switched", new Object[] { m.getName(), m.curMode().getDisplayableName(true) })); //$NON-NLS-1$
        } else {
            clientgui
                    .systemMessage(Messages
                            .getString(
                                    "FiringDisplay.willSwitch", new Object[] { m.getName(), m.pendingMode().getDisplayableName(true) })); //$NON-NLS-1$
        }

        updateTarget();
        clientgui.mechD.wPan.displayMech(ce());
        clientgui.mechD.wPan.selectWeapon(wn);
    }

    /**
     * Called Shots - changes the current called shots selection
     */
    private void changeCalled() {
        int wn = clientgui.mechD.wPan.getSelectedWeaponNum();

        // Do nothing we have no unit selected.
        if (ce() == null) {
            return;
        }

        Mounted m = ce().getEquipment(wn);
        if ((m == null)) {
            return;
        }

        // send change to the server
        m.getCalledShot().switchCalledShot();
        clientgui.getClient().sendCalledShotChange(cen, wn);

        updateTarget();
        clientgui.mechD.wPan.displayMech(ce());
        clientgui.mechD.wPan.selectWeapon(wn);
    }

    /**
     * Cache the list of visible targets. This is used for the 'next target'
     * button.
     * <p/>
     * We'll sort it by range to us.
     */
    private void cacheVisibleTargets() {
        clearVisibleTargets();

        Vector<Entity> vec = clientgui.getClient().getGame().getValidTargets(ce());
        Comparator<Entity> sortComp = new Comparator<Entity>() {
            public int compare(Entity entX, Entity entY) {
                int rangeToX = ce().getPosition().distance(entX.getPosition());
                int rangeToY = ce().getPosition().distance(entY.getPosition());

                if (rangeToX == rangeToY) {
                    return ((entX.getId() < entY.getId()) ? -1 : 1);
                }

                return ((rangeToX < rangeToY) ? -1 : 1);
            }
        };

        // put the vector in the TreeSet first to sort it.
        TreeSet<Entity> tree = new TreeSet<Entity>(sortComp);
        visibleTargets = new Entity[vec.size()];

        for (int i = 0; i < vec.size(); i++) {
            tree.add(vec.elementAt(i));
        }

        // not go through the sorted Set to cache the targets.
        Iterator<Entity> it = tree.iterator();
        int count = 0;
        while (it.hasNext()) {
            visibleTargets[count++] = it.next();
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
        if (visibleTargets == null) {
            return null;
        }

        lastTargetID++;

        if (lastTargetID >= visibleTargets.length) {
            lastTargetID = 0;
        }

        return visibleTargets[lastTargetID];
    }

    /**
     * Jump to our next target. If there isn't one, well, don't do anything.
     */
    private void jumpToNextTarget() {
        Entity targ = getNextTarget();

        if (targ == null) {
            return;
        }

        // HACK : don't show the choice dialog.
        showTargetChoice = false;

        clientgui.bv.centerOnHex(targ.getPosition());
        clientgui.getBoardView().select(targ.getPosition());

        // HACK : show the choice dialog again.
        showTargetChoice = true;
        target(targ);
    }

    /**
     * Called when the current entity is done firing. Send out our attack queue
     * to the server.
     */
    @Override
    public void ready() {
        if (attacks.isEmpty()
                && GUIPreferences.getInstance().getNagForNoAction()) {
            // comfirm this action
            String title = Messages
                    .getString("FiringDisplay.DontFireDialog.title"); //$NON-NLS-1$
            String body = Messages
                    .getString("FiringDisplay.DontFireDialog.message"); //$NON-NLS-1$
            ConfirmDialog response = clientgui.doYesNoBotherDialog(title, body);
            if (!response.getShowAgain()) {
                GUIPreferences.getInstance().setNagForNoAction(false);
            }
            if (!response.getAnswer()) {
                return;
            }
        }

        // We need to nag for overheat on capital fighters
        if ((ce() != null) && ce().isCapitalFighter()
                && GUIPreferences.getInstance().getNagForOverheat()) {
            int totalheat = 0;
            for (EntityAction action : attacks) {
                if (action instanceof WeaponAttackAction) {
                    Mounted weapon = ce().getEquipment(
                            ((WeaponAttackAction) action).getWeaponId());
                    totalheat += weapon.getCurrentHeat();
                }
            }
            if (totalheat > ce().getHeatCapacity()) {
                // comfirm this action
                String title = Messages
                        .getString("FiringDisplay.OverheatNag.title"); //$NON-NLS-1$
                String body = Messages
                        .getString("FiringDisplay.OverheatNag.message"); //$NON-NLS-1$
                ConfirmDialog response = clientgui.doYesNoBotherDialog(title,
                        body);
                if (!response.getShowAgain()) {
                    GUIPreferences.getInstance().setNagForOverheat(false);
                }
                if (!response.getAnswer()) {
                    return;
                }
            }
        }

        // stop further input (hopefully)
        disableButtons();

        // remove temporary attacks from game & board
        removeTempAttacks();

        // For bug 1002223
        // Re-compute the to-hit numbers by adding in correct order.
        Vector<EntityAction> newAttacks = new Vector<EntityAction>();
        for (EntityAction o : attacks) {
            if (o instanceof ArtilleryAttackAction) {
                newAttacks.addElement(o);
            } else if (o instanceof WeaponAttackAction) {
                WeaponAttackAction waa = (WeaponAttackAction) o;
                Entity attacker = waa.getEntity(clientgui.getClient().getGame());
                Targetable target1 = waa.getTarget(clientgui.getClient().getGame());
                boolean curInFrontArc = Compute.isInArc(attacker.getPosition(),
                        attacker.getSecondaryFacing(), target1,
                        attacker.getForwardArc());
                if (curInFrontArc) {
                    WeaponAttackAction waa2 = new WeaponAttackAction(
                            waa.getEntityId(), waa.getTargetType(),
                            waa.getTargetId(), waa.getWeaponId());
                    waa2.setAimedLocation(waa.getAimedLocation());
                    waa2.setAimingMode(waa.getAimingMode());
                    waa2.setOtherAttackInfo(waa.getOtherAttackInfo());
                    waa2.setAmmoId(waa.getAmmoId());
                    waa2.setBombPayload(waa.getBombPayload());
                    newAttacks.addElement(waa2);
                }
            } else {
                newAttacks.addElement(o);
            }
        }
        // now add the attacks in rear/arm arcs
        for (EntityAction o : attacks) {
            if (o instanceof ArtilleryAttackAction) {
                // newAttacks.addElement(o);
                continue;
            } else if (o instanceof WeaponAttackAction) {
                WeaponAttackAction waa = (WeaponAttackAction) o;
                Entity attacker = waa.getEntity(clientgui.getClient().getGame());
                Targetable target1 = waa.getTarget(clientgui.getClient().getGame());
                boolean curInFrontArc = Compute.isInArc(attacker.getPosition(),
                        attacker.getSecondaryFacing(), target1,
                        attacker.getForwardArc());
                if (!curInFrontArc) {
                    WeaponAttackAction waa2 = new WeaponAttackAction(
                            waa.getEntityId(), waa.getTargetType(),
                            waa.getTargetId(), waa.getWeaponId());
                    waa2.setAimedLocation(waa.getAimedLocation());
                    waa2.setAimingMode(waa.getAimingMode());
                    waa2.setOtherAttackInfo(waa.getOtherAttackInfo());
                    waa2.setAmmoId(waa.getAmmoId());
                    waa2.setBombPayload(waa.getBombPayload());
                    newAttacks.addElement(waa2);
                }
            }
        }

        // send out attacks
        clientgui.getClient().sendAttackData(cen, newAttacks);

        // clear queue
        attacks.removeAllElements();

        // Clear the menu bar.
        clientgui.getMenuBar().setEntity(null);

        // close aimed shot display, if any
        ash.closeDialog();

        endMyTurn();
    }

    /**
     * clear turret
     */
    private void doClearTurret() {
        String title = Messages.getString("FiringDisplay.ClearTurret.title"); //$NON-NLS-1$
        String body = Messages.getString("FiringDisplay.ClearTurret.message"); //$NON-NLS-1$
        if (!clientgui.doYesNoDialog(title, body)) {
            return;
        }
        if ((((attacks.size() == 0) && (ce() instanceof Tank) && (((Tank) ce())
                .isTurretJammed(((Tank) ce()).getLocTurret()))) || ((Tank) ce())
                .isTurretJammed(((Tank) ce()).getLocTurret2()))) {
            UnjamTurretAction uta = new UnjamTurretAction(ce().getId());
            attacks.add(uta);
            ready();
        }
    }

    /**
     * clear weapon jam
     */
    private void doClearWeaponJam() {
        ArrayList<Mounted> weapons = ((Tank) ce()).getJammedWeapons();
        String[] names = new String[weapons.size()];
        for (int loop = 0; loop < names.length; loop++) {
            names[loop] = weapons.get(loop).getDesc();
        }
        String input = (String) JOptionPane.showInputDialog(clientgui,
                Messages.getString("FiringDisplay.ClearWeaponJam.question"), //$NON-NLS-1$
                Messages.getString("FiringDisplay.ClearWeaponJam.title"), //$NON-NLS-1$
                JOptionPane.QUESTION_MESSAGE, null, names, null);

        if (input != null) {
            for (int loop = 0; loop < names.length; loop++) {
                if (input.equals(names[loop])) {
                    RepairWeaponMalfunctionAction rwma = new RepairWeaponMalfunctionAction(
                            ce().getId(), ce().getEquipmentNum(
                                    weapons.get(loop)));
                    attacks.add(rwma);
                    ready();
                }
            }
        }
    }

    /**
     * fire searchlight
     */
    private void doSearchlight() {
        // validate
        if ((ce() == null) || (target == null)) {
            throw new IllegalArgumentException(
                    "current searchlight parameters are invalid"); //$NON-NLS-1$
        }

        if (!SearchlightAttackAction.isPossible(clientgui.getClient().getGame(),
                cen, target, null)) {
            return;
        }

        // create and queue a searchlight action
        SearchlightAttackAction saa = new SearchlightAttackAction(cen,
                target.getTargetType(), target.getTargetId());
        attacks.addElement(saa);

        // and add it into the game, temporarily
        clientgui.getClient().getGame().addAction(saa);
        clientgui.bv.addAttack(saa);
        clientgui.minimap.drawMap();

        // refresh weapon panel, as bth will have changed
        updateTarget();
    }

    private int[] getBombPayload(boolean isSpace, int limit) {
        int[] payload = new int[BombType.B_NUM];
        if (!(ce() instanceof Aero)) {
            return payload;
        }
        int[] loadout = ce().getBombLoadout();
        // this part is ugly, but we need to find any other bombing attacks by
        // this
        // entity in the attack list and subtract those payloads from the
        // loadout
        for (EntityAction o : attacks) {
            if (o instanceof WeaponAttackAction) {
                WeaponAttackAction waa = (WeaponAttackAction) o;
                if (waa.getEntityId() == ce().getId()) {
                    int[] priorLoad = waa.getBombPayload();
                    for (int i = 0; i < priorLoad.length; i++) {
                        loadout[i] = loadout[i] - priorLoad[i];
                    }
                }
            }
        }

        int numFighters = 0;
        if (ce() instanceof FighterSquadron) {
            numFighters = ((FighterSquadron) ce()).getNFighters();
        }
        BombPayloadDialog bombsDialog = new BombPayloadDialog(clientgui.frame,
                Messages.getString("FiringDisplay.BombNumberDialog.title"), //$NON-NLS-1$
                loadout, isSpace, false, limit, numFighters);
        bombsDialog.setVisible(true);
        if (bombsDialog.getAnswer()) {
            payload = bombsDialog.getChoices();
        }
        return payload;
    }

    /**
     * Adds a weapon attack with the currently selected weapon to the attack
     * queue.
     */
    void fire() {
        // get the selected weaponnum
        int weaponNum = clientgui.mechD.wPan.getSelectedWeaponNum();
        Mounted mounted = ce().getEquipment(weaponNum);

        // validate
        if ((ce() == null) || (target == null) || (mounted == null)
                || !(mounted.getType() instanceof WeaponType)) {
            throw new IllegalArgumentException(
                    "current fire parameters are invalid"); //$NON-NLS-1$
        }
        // check if we now shoot at a target in the front arc and previously
        // shot a target in side/rear arc that then was primary target
        // if so, ask and tell the user that to-hits will change
        if ((ce() instanceof Mech) || (ce() instanceof Tank)
                || (ce() instanceof Protomech)) {
            EntityAction lastAction = null;
            try {
                lastAction = attacks.lastElement();
            } catch (NoSuchElementException ex) {
                // ignore
            }
            if ((lastAction != null)
                    && (lastAction instanceof WeaponAttackAction)) {
                WeaponAttackAction oldWaa = (WeaponAttackAction) lastAction;
                Targetable oldTarget = oldWaa
                        .getTarget(clientgui.getClient().getGame());
                if (!oldTarget.equals(target)) {
                    boolean oldInFront = Compute.isInArc(ce().getPosition(),
                            ce().getSecondaryFacing(), oldTarget, ce()
                                    .getForwardArc());
                    boolean curInFront = Compute.isInArc(ce().getPosition(),
                            ce().getSecondaryFacing(), target, ce()
                                    .getForwardArc());
                    if (!oldInFront && curInFront) {
                        String title = Messages
                                .getString("FiringDisplay.SecondaryTargetToHitChange.title"); //$NON-NLS-1$
                        String body = Messages
                                .getString("FiringDisplay.SecondaryTargetToHitChange.message"); //$NON-NLS-1$
                        if (!clientgui.doYesNoDialog(title, body)) {
                            return;
                        }
                    }
                }
            }
        }

        // declare searchlight, if possible
        if (GUIPreferences.getInstance().getAutoDeclareSearchlight()
                && ce().isUsingSpotlight()) {
            doSearchlight();
        }

        WeaponAttackAction waa;
        if (!mounted.getType().hasFlag(WeaponType.F_ARTILLERY)) {
            waa = new WeaponAttackAction(cen, target.getTargetType(),
                    target.getTargetId(), weaponNum);
        } else {
            waa = new ArtilleryAttackAction(cen, target.getTargetType(),
                    target.getTargetId(), weaponNum, clientgui.getClient().getGame());
        }

        // check for a bomb payload dialog
        if (mounted.getType().hasFlag(WeaponType.F_SPACE_BOMB)) {
            int[] payload = getBombPayload(true, -1);
            waa.setBombPayload(payload);
        } else if (mounted.getType().hasFlag(WeaponType.F_DIVE_BOMB)) {
            int[] payload = getBombPayload(false, -1);
            waa.setBombPayload(payload);
        } else if (mounted.getType().hasFlag(WeaponType.F_ALT_BOMB)) {
            // if the user cancels, then return
            int[] payload = getBombPayload(false, 2);
            waa.setBombPayload(payload);
        }

        if ((mounted.getLinked() != null)
                && (((WeaponType) mounted.getType()).getAmmoType() != AmmoType.T_NA)) {
            Mounted ammoMount = mounted.getLinked();
            AmmoType ammoType = (AmmoType) ammoMount.getType();
            waa.setAmmoId(ce().getEquipmentNum(ammoMount));
            if (((ammoType.getMunitionType() == AmmoType.M_THUNDER_VIBRABOMB) && ((ammoType
                    .getAmmoType() == AmmoType.T_LRM) || (ammoType
                    .getAmmoType() == AmmoType.T_MML)))
                    || (ammoType.getMunitionType() == AmmoType.M_VIBRABOMB_IV)) {
                VibrabombSettingDialog vsd = new VibrabombSettingDialog(
                        clientgui.frame);
                vsd.setVisible(true);
                waa.setOtherAttackInfo(vsd.getSetting());
            }
        }

        if (ash.allowAimedShotWith(mounted) && ash.inAimingMode()
                && ash.isAimingAtLocation()) {
            waa.setAimedLocation(ash.getAimingAt());
            waa.setAimingMode(ash.getAimingMode());
        } else {
            waa.setAimedLocation(Entity.LOC_NONE);
            waa.setAimingMode(IAimingModes.AIM_MODE_NONE);
        }

        // add the attack to our temporary queue
        attacks.addElement(waa);

        // and add it into the game, temporarily
        clientgui.getClient().getGame().addAction(waa);
        clientgui.minimap.drawMap();

        // set the weapon as used
        mounted.setUsedThisRound(true);

        // find the next available weapon
        int nextWeapon = ce().getNextWeapon(weaponNum);

        // we fired a weapon, can't clear turret jams or weapon jams anymore
        updateClearTurret();
        updateClearWeaponJam();

        // check; if there are no ready weapons, you're done.
        if ((nextWeapon == -1)
                && GUIPreferences.getInstance().getAutoEndFiring()) {
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
    void nextWeapon() {
        if (ce() == null) {
            return;
        }
        int nextWeapon = ce().getNextWeapon(
                clientgui.mechD.wPan.getSelectedWeaponNum());
        // if there's no next weapon, forget about it
        if (nextWeapon == -1) {
            return;
        }
        clientgui.mechD.wPan.displayMech(ce());
        clientgui.mechD.wPan.selectWeapon(nextWeapon);
        final int weaponId = clientgui.mechD.wPan.getSelectedWeaponNum();
        if (weaponId == -1) {
            setFireModeEnabled(false);
        } else {
            Mounted m = ce().getEquipment(weaponId);
            setFireModeEnabled(m.isModeSwitchable());
        }
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
        String body = Messages
                .getString("FiringDisplay.FindClubDialog.message"); //$NON-NLS-1$
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
        if ((ce() == null) || (target == null)) {
            return;
        }
        if (ce().isINarcedWith(INarcPod.HAYWIRE)) {
            String title = Messages
                    .getString("FiringDisplay.CantSpotDialog.title"); //$NON-NLS-1$
            String body = Messages
                    .getString("FiringDisplay.CantSpotDialog.message"); //$NON-NLS-1$
            clientgui.doAlertDialog(title, body);
            return;
        }
        // comfirm this action
        String title = Messages
                .getString("FiringDisplay.SpotForInderectDialog.title"); //$NON-NLS-1$
        String body = Messages
                .getString("FiringDisplay.SpotForInderectDialog.message"); //$NON-NLS-1$
        if (!clientgui.doYesNoDialog(title, body)) {
            return;
        }
        attacks.addElement(new SpotAction(cen, target.getTargetId()));

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
        Enumeration<AbstractEntityAction> i = attacks.elements();
        while (i.hasMoreElements()) {
            Object o = i.nextElement();
            if (o instanceof WeaponAttackAction) {
                WeaponAttackAction waa = (WeaponAttackAction) o;
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
        clientgui.getClient().getGame().removeActionsFor(cen);
        clientgui.bv.removeAttacksFor(ce());
    }

    /**
     * removes the last action
     */
    private void removeLastFiring() {
        if (!attacks.isEmpty()) {
            Object o = attacks.lastElement();
            if (o instanceof WeaponAttackAction) {
                WeaponAttackAction waa = (WeaponAttackAction) o;
                ce().getEquipment(waa.getWeaponId()).setUsedThisRound(false);
                attacks.removeElement(o);
                clientgui.mechD.wPan.displayMech(ce());
                clientgui.getClient().getGame().removeAction(o);
                clientgui.bv.refreshAttacks();
                clientgui.minimap.drawMap();
            }
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
        target = t;
        ash.setAimingMode();
        updateTarget();
        ash.showDialog();
    }

    /**
     * Targets something
     */
    protected void updateTarget() {
        setFireEnabled(false);

        // allow spotting
        if ((ce() != null)
                && ce().canSpot()
                && (target != null)
                && clientgui.getClient().getGame().getOptions().booleanOption(
                        "indirect_fire")) { //$NON-NLS-1$)
            setSpotEnabled(true);
        }

        // update target panel
        final int weaponId = clientgui.mechD.wPan.getSelectedWeaponNum();
        if ((target != null) && (target.getPosition() != null)
                && (weaponId != -1) && (ce() != null)) {
            ToHitData toHit;
            if (ash.inAimingMode()) {
                Mounted weapon = ce().getEquipment(weaponId);
                boolean aiming = ash.isAimingAtLocation()
                        && ash.allowAimedShotWith(weapon);
                ash.setEnableAll(aiming);
                if (aiming) {
                    toHit = WeaponAttackAction.toHit(
                            clientgui.getClient().getGame(), cen, target, weaponId,
                            ash.getAimingAt(), ash.getAimingMode());
                    clientgui.mechD.wPan.wTargetR.setText(target
                            .getDisplayName()
                            + " (" + ash.getAimingLocation() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
                } else {
                    toHit = WeaponAttackAction.toHit(
                            clientgui.getClient().getGame(), cen, target, weaponId,
                            Entity.LOC_NONE, IAimingModes.AIM_MODE_NONE);
                    clientgui.mechD.wPan.wTargetR.setText(target
                            .getDisplayName());
                }
                ash.setPartialCover(toHit.getCover());
            } else {
                toHit = WeaponAttackAction.toHit(clientgui.getClient().getGame(),
                        cen, target, weaponId, Entity.LOC_NONE,
                        IAimingModes.AIM_MODE_NONE);
                clientgui.mechD.wPan.wTargetR.setText(target.getDisplayName());
            }
            clientgui.mechD.wPan.wRangeR
                    .setText("" + Compute.effectiveDistance(clientgui.getClient().getGame(), ce(), target)); //$NON-NLS-1$
            Mounted m = ce().getEquipment(weaponId);
            if (m.isUsedThisRound()) {
                clientgui.mechD.wPan.wToHitR.setText(Messages
                        .getString("FiringDisplay.alreadyFired")); //$NON-NLS-1$
                setFireEnabled(false);
            } else if (m.getType().hasFlag(WeaponType.F_AUTO_TARGET)) {
                clientgui.mechD.wPan.wToHitR.setText(Messages
                        .getString("FiringDisplay.autoFiringWeapon")); //$NON-NLS-1$
                setFireEnabled(false);
            } else if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
                clientgui.mechD.wPan.wToHitR.setText(toHit.getValueAsString());
                setFireEnabled(false);
            } else if (toHit.getValue() == TargetRoll.AUTOMATIC_FAIL) {
                clientgui.mechD.wPan.wToHitR.setText(toHit.getValueAsString());
                setFireEnabled(true);
            } else {
                clientgui.mechD.wPan.wToHitR.setText(toHit.getValueAsString()
                        + " (" + Compute.oddsAbove(toHit.getValue()) + "%)"); //$NON-NLS-1$ //$NON-NLS-2$
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

        if ((weaponId != -1) && (ce() != null)) {
            Mounted m = ce().getEquipment(weaponId);
            setFireModeEnabled(m.isModeSwitchable());
        }

        updateSearchlight();
    }

    /**
     * Torso twist in the proper direction.
     */
    void torsoTwist(Coords twistTarget) {
        int direction = ce().getFacing();

        if (twistTarget != null) {
            direction = ce().clipSecondaryFacing(
                    ce().getPosition().direction(twistTarget));
        }

        if (direction != ce().getSecondaryFacing()) {
            clearAttacks();
            attacks.addElement(new TorsoTwistAction(cen, direction));
            ce().setSecondaryFacing(direction);
            refreshAll();
        }
    }

    /**
     * Torso twist to the left or right
     *
     * @param twistDir
     *            An <code>int</code> specifying wether we're twisting left or
     *            right, 0 if we're twisting to the left, 1 if to the right.
     */

    void torsoTwist(int twistDir) {
        int direction = ce().getSecondaryFacing();
        if (twistDir == 0) {
            clearAttacks();
            direction = ce().clipSecondaryFacing((direction + 5) % 6);
            attacks.addElement(new TorsoTwistAction(cen, direction));
            ce().setSecondaryFacing(direction);
            refreshAll();
        } else if (twistDir == 1) {
            clearAttacks();
            direction = ce().clipSecondaryFacing((direction + 7) % 6);
            attacks.addElement(new TorsoTwistAction(cen, direction));
            ce().setSecondaryFacing(direction);
            refreshAll();
        }
    }

    /**
     * Returns the current entity.
     */
    Entity ce() {
        return clientgui.getClient().getGame().getEntity(cen);
    }

    //
    // BoardListener
    //
    @Override
    public void hexMoused(BoardViewEvent b) {

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        // ignore buttons other than 1
        if (!clientgui.getClient().isMyTurn()
                || ((b.getModifiers() & InputEvent.BUTTON1_MASK) == 0)) {
            return;
        }
        // control pressed means a line of sight check.
        // added ALT_MASK by kenn
        if (((b.getModifiers() & InputEvent.CTRL_MASK) != 0)
                || ((b.getModifiers() & InputEvent.ALT_MASK) != 0)) {
            return;
        }
        // check for shifty goodness
        if (shiftheld != ((b.getModifiers() & InputEvent.SHIFT_MASK) != 0)) {
            shiftheld = (b.getModifiers() & InputEvent.SHIFT_MASK) != 0;
        }

        if (b.getType() == BoardViewEvent.BOARD_HEX_DRAGGED) {
            if (shiftheld || twisting) {
                updateFlipArms(false);
                torsoTwist(b.getCoords());
            }
            clientgui.getBoardView().cursor(b.getCoords());
        } else if (b.getType() == BoardViewEvent.BOARD_HEX_CLICKED) {
            twisting = false;
            if (!shiftheld) {
                clientgui.getBoardView().select(b.getCoords());
            }
        }
    }

    @Override
    public void hexSelected(BoardViewEvent b) {

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (clientgui.getClient().isMyTurn() && (b.getCoords() != null)
                && (ce() != null) && !b.getCoords().equals(ce().getPosition())) {
            // HACK : sometimes we don't show the target choice window
            Targetable targ = null;
            if (showTargetChoice) {
                targ = chooseTarget(b.getCoords());
            }
            if (shiftheld) {
                updateFlipArms(false);
                torsoTwist(b.getCoords());
            } else if (targ != null) {
                target(targ);
            }
        }
    }

    //
    // GameListener
    //
    @Override
    public void gameTurnChange(GameTurnChangeEvent e) {

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (clientgui.getClient().getGame().getPhase() == IGame.Phase.PHASE_FIRING) {

            if (clientgui.getClient().isMyTurn()) {
                if (cen == Entity.NONE) {
                    beginMyTurn();
                }
                setStatusBarText(Messages
                        .getString("FiringDisplay.its_your_turn")); //$NON-NLS-1$
            } else {
                endMyTurn();
                setStatusBarText(Messages
                        .getString(
                                "FiringDisplay.its_others_turn", new Object[] {e.getPlayer().getName() })); //$NON-NLS-1$
            }
        }
    }

    @Override
    public void gamePhaseChange(GamePhaseChangeEvent e) {

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (clientgui.getClient().isMyTurn()
                && (clientgui.getClient().getGame().getPhase() != IGame.Phase.PHASE_FIRING)) {
            endMyTurn();
        }
        // if we're ending the firing phase, unregister stuff.
        if (clientgui.getClient().getGame().getPhase() == IGame.Phase.PHASE_FIRING) {
            setStatusBarText(Messages
                    .getString("FiringDisplay.waitingForFiringPhase")); //$NON-NLS-1$
        }
    }

    //
    // ActionListener
    //
    public void actionPerformed(ActionEvent ev) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (statusBarActionPerformed(ev, clientgui.getClient())) {
            return;
        }

        if (!clientgui.getClient().isMyTurn()) {
            return;
        }

        if ("viewGameOptions".equalsIgnoreCase(ev.getActionCommand())) { //$NON-NLS-1$
            // Make sure the game options dialog is not editable.
            if (clientgui.getGameOptionsDialog().isEditable()) {
                clientgui.getGameOptionsDialog().setEditable(false);
            }
            // Display the game options dialog.
            clientgui.getGameOptionsDialog().update(
                    clientgui.getClient().getGame().getOptions());
            clientgui.getGameOptionsDialog().setVisible(true);
        } else if (ev.getActionCommand().equals(Command.FIRE_FIRE.getCmd())) {
            fire();
        } else if (ev.getActionCommand().equals(Command.FIRE_SKIP.getCmd())) {
            nextWeapon();
        } else if (ev.getActionCommand().equals(Command.FIRE_TWIST.getCmd())) {
            twisting = true;
        } else if (ev.getActionCommand().equals(Command.FIRE_NEXT.getCmd())) {
            selectEntity(clientgui.getClient().getNextEntityNum(cen));
        } else if (ev.getActionCommand().equals(Command.FIRE_MORE.getCmd())) {
        	currentButtonGroup++;
        	currentButtonGroup %= numButtonGroups;
            setupButtonPanel();
        } else if (ev.getActionCommand().equals(Command.FIRE_FIND_CLUB.getCmd())) {
            findClub();
        } else if (ev.getActionCommand().equals(Command.FIRE_SPOT.getCmd())) {
            doSpot();
        } else if (ev.getActionCommand().equals(Command.FIRE_NEXT_TARG.getCmd())) {
            jumpToNextTarget();
        } else if (ev.getActionCommand().equals(Command.FIRE_FLIP_ARMS.getCmd())) {
            updateFlipArms(!ce().getArmsFlipped());
            // Fire Mode - More Fire Mode button handling - Rasia
        } else if (ev.getActionCommand().equals(Command.FIRE_MODE.getCmd())) {
            changeMode();
        } else if (ev.getActionCommand().equals(Command.FIRE_CALLED.getCmd())) {
            changeCalled();
        } else if (("changeSinks".equalsIgnoreCase(ev.getActionCommand()))
                || (ev.getActionCommand().equals(Command.FIRE_CANCEL.getCmd()))) {
            clear();
        } else if (ev.getActionCommand().equals(Command.FIRE_SEARCHLIGHT.getCmd())) {
            doSearchlight();
        } else if (ev.getActionCommand().equals(Command.FIRE_CLEAR_TURRET.getCmd())) {
            doClearTurret();
        } else if (ev.getActionCommand().equals(Command.FIRE_CLEAR_WEAPON.getCmd())) {
            doClearWeaponJam();
        }
    }

    /**
     * update for change of arms-flipping status
     *
     * @param armsFlipped
     */
    void updateFlipArms(boolean armsFlipped) {
        if (ce() == null) {
            return;
        }
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
        setSearchlightEnabled((ce() != null)
                && (target != null)
                && ce().isUsingSpotlight()
                && ce().getCrew().isActive()
                && SearchlightAttackAction.isPossible(
                        clientgui.getClient().getGame(), cen, target, null)
                && !((ce() instanceof Tank) && (((Tank) ce()).getStunnedTurns() > 0)));
    }

    private void updateClearTurret() {
        setFireClearTurretEnabled((ce() instanceof Tank)
                && (((Tank) ce()).isTurretJammed(((Tank) ce()).getLocTurret()) || ((Tank) ce())
                        .isTurretJammed(((Tank) ce()).getLocTurret2()))
                && (attacks.size() == 0)
                && !(((Tank) ce()).getStunnedTurns() > 0));
    }

    private void updateClearWeaponJam() {
        setFireClearWeaponJamEnabled((ce() instanceof Tank)
                && (((Tank) ce()).getJammedWeapons().size() != 0)
                && (attacks.size() == 0)
                && !(((Tank) ce()).getStunnedTurns() > 0));
    }

    private void setFireEnabled(boolean enabled) {
    	buttons.get(Command.FIRE_FIRE).setEnabled(enabled);
        clientgui.getMenuBar().setFireFireEnabled(enabled);
    }

    private void setTwistEnabled(boolean enabled) {
    	buttons.get(Command.FIRE_TWIST).setEnabled(enabled);
        clientgui.getMenuBar().setFireTwistEnabled(enabled);
    }

    private void setSkipEnabled(boolean enabled) {
    	buttons.get(Command.FIRE_SKIP).setEnabled(enabled);
        clientgui.getMenuBar().setFireSkipEnabled(enabled);
    }

    private void setFindClubEnabled(boolean enabled) {
    	buttons.get(Command.FIRE_FIND_CLUB).setEnabled(enabled);
        clientgui.getMenuBar().setFireFindClubEnabled(enabled);
    }

    private void setNextTargetEnabled(boolean enabled) {
    	buttons.get(Command.FIRE_NEXT_TARG).setEnabled(enabled);
        clientgui.getMenuBar().setFireNextTargetEnabled(enabled);
    }

    private void setFlipArmsEnabled(boolean enabled) {
    	buttons.get(Command.FIRE_FLIP_ARMS).setEnabled(enabled);
        clientgui.getMenuBar().setFireFlipArmsEnabled(enabled);
    }

    private void setSpotEnabled(boolean enabled) {
    	buttons.get(Command.FIRE_SPOT).setEnabled(enabled);
        clientgui.getMenuBar().setFireSpotEnabled(enabled);
    }

    private void setSearchlightEnabled(boolean enabled) {
    	buttons.get(Command.FIRE_SEARCHLIGHT).setEnabled(enabled);
        clientgui.getMenuBar().setFireSearchlightEnabled(enabled);
    }

    private void setFireModeEnabled(boolean enabled) {
    	buttons.get(Command.FIRE_MODE).setEnabled(enabled);
        clientgui.getMenuBar().setFireModeEnabled(enabled);
    }

    private void setFireCalledEnabled(boolean enabled) {
    	buttons.get(Command.FIRE_CALLED).setEnabled(enabled);
        clientgui.getMenuBar().setFireCalledEnabled(enabled);
    }

    private void setFireClearTurretEnabled(boolean enabled) {
    	buttons.get(Command.FIRE_CLEAR_TURRET).setEnabled(enabled);
        clientgui.getMenuBar().setFireClearTurretEnabled(enabled);
    }

    private void setFireClearWeaponJamEnabled(boolean enabled) {
    	buttons.get(Command.FIRE_CLEAR_WEAPON).setEnabled(enabled);
        clientgui.getMenuBar().setFireClearWeaponJamEnabled(enabled);
    }

    private void setNextEnabled(boolean enabled) {
    	buttons.get(Command.FIRE_NEXT).setEnabled(enabled);
        clientgui.getMenuBar().setFireNextEnabled(enabled);
    }

    //
    // KeyListener
    //
    public void keyPressed(KeyEvent ev) {

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (ev.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
            if (clientgui.getClient().isMyTurn()) {
                removeLastFiring();
            }
        }
        if ((ev.getKeyCode() == KeyEvent.VK_SHIFT) && !shiftheld) {
            shiftheld = true;
            if (clientgui.getClient().isMyTurn()
                    && (clientgui.getBoardView().getLastCursor() != null)) {
                updateFlipArms(false);
                torsoTwist(clientgui.getBoardView().getLastCursor());
            }
        }
        if ((ev.getKeyCode() == KeyEvent.VK_LEFT) && shiftheld) {
            updateFlipArms(false);
            torsoTwist(0);
        }
        if ((ev.getKeyCode() == KeyEvent.VK_RIGHT) && shiftheld) {
            updateFlipArms(false);
            torsoTwist(1);
        }
    }

    @Override
    public void clear() {
        clearAttacks();
        clientgui.getBoardView().select(null);
        clientgui.getBoardView().cursor(null);
        refreshAll();
    }

    public void keyReleased(KeyEvent ev) {

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if ((ev.getKeyCode() == KeyEvent.VK_SHIFT) && shiftheld) {
            shiftheld = false;
        }
    }

    public void keyTyped(KeyEvent ev) {
        // ignore
    }

    //
    // ItemListener
    //
    public void itemStateChanged(ItemEvent ev) {

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }
    }

    // board view listener
    @Override
    public void finishedMovingUnits(BoardViewEvent b) {

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (clientgui.getClient().isMyTurn() && (ce() != null)) {
            clientgui.setDisplayVisible(true);
            clientgui.bv.centerOnHex(ce().getPosition());
        }
    }

    @Override
    public void unitSelected(BoardViewEvent b) {

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        Entity e = clientgui.getClient().getGame().getEntity(b.getEntityId());
        if (clientgui.getClient().isMyTurn()) {
            if (clientgui.getClient().getMyTurn()
                    .isValidEntity(e, clientgui.getClient().getGame())) {
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

    public void valueChanged(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }
        if (event.getSource().equals(clientgui.mechD.wPan.weaponList)
                && (clientgui.getClient().getGame().getPhase() == Phase.PHASE_FIRING)) {
            // If we aren't in the firing phase, there's no guarantee that cen
            // is set properly, hence we can't update

            // update target data in weapon display
            updateTarget();
        }
    }

    private class AimedShotHandler implements ActionListener, ItemListener {
        private int aimingAt = Entity.LOC_NONE;

        private int aimingMode = IAimingModes.AIM_MODE_NONE;

        private int partialCover = LosEffects.COVER_NONE;

        private AimedShotDialog asd;

        public AimedShotHandler() {
            // ignore
        }

        public void showDialog() {
            if (asd != null) {
                int oldAimingMode = aimingMode;
                closeDialog();
                aimingMode = oldAimingMode;
            }

            if (inAimingMode()) {
                String[] options;
                boolean[] enabled;

                if (target instanceof GunEmplacement) {
                    return;
                }
                if (target instanceof Entity) {
                    options = ((Entity) target).getLocationNames();
                    enabled = createEnabledMask(options.length);
                } else {
                    return;
                }
                if (target instanceof Mech) {
                    if (aimingMode == IAimingModes.AIM_MODE_IMMOBILE) {
                        aimingAt = Mech.LOC_HEAD;
                    } else if (aimingMode == IAimingModes.AIM_MODE_TARG_COMP) {
                        aimingAt = Mech.LOC_CT;
                    }
                } else if (target instanceof Tank) {
                    int side = Compute.targetSideTable(ce(), target);
                    if (target instanceof LargeSupportTank) {
                        if (side == ToHitData.SIDE_FRONTLEFT) {
                            aimingAt = LargeSupportTank.LOC_FRONTLEFT;
                        } else if (side == ToHitData.SIDE_FRONTRIGHT) {
                            aimingAt = LargeSupportTank.LOC_FRONTRIGHT;
                        } else if (side == ToHitData.SIDE_REARRIGHT) {
                            aimingAt = LargeSupportTank.LOC_REARRIGHT;
                        } else if (side == ToHitData.SIDE_REARLEFT) {
                            aimingAt = LargeSupportTank.LOC_REARLEFT;
                        }
                    }
                    if (side == ToHitData.SIDE_LEFT) {
                        aimingAt = Tank.LOC_LEFT;
                    }
                    if (side == ToHitData.SIDE_RIGHT) {
                        aimingAt = Tank.LOC_RIGHT;
                    }
                    if (side == ToHitData.SIDE_REAR) {
                        aimingAt = (target instanceof LargeSupportTank) ? LargeSupportTank.LOC_REAR
                                : target instanceof SuperHeavyTank ? SuperHeavyTank.LOC_REAR
                                        : Tank.LOC_REAR;
                    }
                    if (side == ToHitData.SIDE_FRONT) {
                        aimingAt = Tank.LOC_FRONT;
                    }
                } else if (target instanceof Protomech) {
                    aimingAt = Protomech.LOC_TORSO;
                } else if (target instanceof BattleArmor) {
                    aimingAt = BattleArmor.LOC_TROOPER_1;
                } else {
                    // no aiming allowed for MechWarrior or BattleArmor
                    return;
                }

                asd = new AimedShotDialog(
                        clientgui.frame,
                        Messages.getString("FiringDisplay.AimedShotDialog.title"), //$NON-NLS-1$
                        Messages.getString("FiringDisplay.AimedShotDialog.message"), //$NON-NLS-1$
                        options, enabled, aimingAt, this, this);

                asd.setVisible(true);
                updateTarget();
            }
        }

        private boolean[] createEnabledMask(int length) {
            boolean[] mask = new boolean[length];

            for (int i = 0; i < length; i++) {
                mask[i] = true;
            }

            int side = Compute.targetSideTable(ce(), target);

            // on a tank, remove turret if its missing
            // also, remove body
            if (target instanceof Tank) {
                mask[Tank.LOC_BODY] = false;
                Tank tank = (Tank) target;
                if (tank.hasNoTurret()) {
                    int turretLoc = tank.getLocTurret();
                    mask[turretLoc] = false;
                }
                // remove non-visible sides
                if (target instanceof LargeSupportTank) {
                    if (side == ToHitData.SIDE_FRONT) {
                        mask[LargeSupportTank.LOC_FRONTLEFT] = false;
                        mask[LargeSupportTank.LOC_REARLEFT] = false;
                        mask[LargeSupportTank.LOC_REARRIGHT] = false;
                        mask[LargeSupportTank.LOC_REAR] = false;
                    }
                    if (side == ToHitData.SIDE_FRONTLEFT) {
                        mask[LargeSupportTank.LOC_FRONTRIGHT] = false;
                        mask[LargeSupportTank.LOC_REARLEFT] = false;
                        mask[LargeSupportTank.LOC_REARRIGHT] = false;
                        mask[LargeSupportTank.LOC_REAR] = false;
                    }
                    if (side == ToHitData.SIDE_FRONTRIGHT) {
                        mask[LargeSupportTank.LOC_FRONTLEFT] = false;
                        mask[LargeSupportTank.LOC_REARLEFT] = false;
                        mask[LargeSupportTank.LOC_REARRIGHT] = false;
                        mask[LargeSupportTank.LOC_REAR] = false;
                    }
                    if (side == ToHitData.SIDE_REARRIGHT) {
                        mask[Tank.LOC_FRONT] = false;
                        mask[LargeSupportTank.LOC_FRONTLEFT] = false;
                        mask[LargeSupportTank.LOC_FRONTRIGHT] = false;
                        mask[LargeSupportTank.LOC_REARLEFT] = false;
                    }
                    if (side == ToHitData.SIDE_REARLEFT) {
                        mask[Tank.LOC_FRONT] = false;
                        mask[LargeSupportTank.LOC_FRONTLEFT] = false;
                        mask[LargeSupportTank.LOC_FRONTRIGHT] = false;
                        mask[LargeSupportTank.LOC_REARRIGHT] = false;
                    }
                    if (side == ToHitData.SIDE_REAR) {
                        mask[Tank.LOC_FRONT] = false;
                        mask[LargeSupportTank.LOC_FRONTLEFT] = false;
                        mask[LargeSupportTank.LOC_FRONTRIGHT] = false;
                        mask[LargeSupportTank.LOC_REARRIGHT] = false;
                    }
                } else if (target instanceof SuperHeavyTank) {
                    if (side == ToHitData.SIDE_FRONT) {
                        mask[SuperHeavyTank.LOC_FRONTLEFT] = false;
                        mask[SuperHeavyTank.LOC_REARLEFT] = false;
                        mask[SuperHeavyTank.LOC_REARRIGHT] = false;
                        mask[SuperHeavyTank.LOC_REAR] = false;
                    }
                    if (side == ToHitData.SIDE_FRONTLEFT) {
                        mask[SuperHeavyTank.LOC_FRONTRIGHT] = false;
                        mask[SuperHeavyTank.LOC_REARLEFT] = false;
                        mask[SuperHeavyTank.LOC_REARRIGHT] = false;
                        mask[SuperHeavyTank.LOC_REAR] = false;
                    }
                    if (side == ToHitData.SIDE_FRONTRIGHT) {
                        mask[SuperHeavyTank.LOC_FRONTLEFT] = false;
                        mask[SuperHeavyTank.LOC_REARLEFT] = false;
                        mask[SuperHeavyTank.LOC_REARRIGHT] = false;
                        mask[SuperHeavyTank.LOC_REAR] = false;
                    }
                    if (side == ToHitData.SIDE_REARRIGHT) {
                        mask[Tank.LOC_FRONT] = false;
                        mask[SuperHeavyTank.LOC_FRONTLEFT] = false;
                        mask[SuperHeavyTank.LOC_FRONTRIGHT] = false;
                        mask[SuperHeavyTank.LOC_REARLEFT] = false;
                    }
                    if (side == ToHitData.SIDE_REARLEFT) {
                        mask[Tank.LOC_FRONT] = false;
                        mask[SuperHeavyTank.LOC_FRONTLEFT] = false;
                        mask[SuperHeavyTank.LOC_FRONTRIGHT] = false;
                        mask[SuperHeavyTank.LOC_REARRIGHT] = false;
                    }
                    if (side == ToHitData.SIDE_REAR) {
                        mask[Tank.LOC_FRONT] = false;
                        mask[SuperHeavyTank.LOC_FRONTLEFT] = false;
                        mask[SuperHeavyTank.LOC_FRONTRIGHT] = false;
                        mask[SuperHeavyTank.LOC_REARRIGHT] = false;
                    }
                } else {
                    if (side == ToHitData.SIDE_LEFT) {
                        mask[Tank.LOC_RIGHT] = false;
                    }
                    if (side == ToHitData.SIDE_RIGHT) {
                        mask[Tank.LOC_LEFT] = false;
                    }
                    if (side == ToHitData.SIDE_REAR) {
                        mask[Tank.LOC_FRONT] = false;
                    }
                    if (side == ToHitData.SIDE_FRONT) {
                        mask[Tank.LOC_REAR] = false;
                    }
                }
            }

            // remove main gun on protos that don't have one
            if (target instanceof Protomech) {
                if (!((Protomech) target).hasMainGun()) {
                    mask[Protomech.LOC_MAINGUN] = false;
                }
            }

            // remove squad location on BAs
            // also remove dead troopers
            if (target instanceof BattleArmor) {
                mask[BattleArmor.LOC_SQUAD] = false;
            }

            // remove locations hidden by partial cover
            if ((partialCover & LosEffects.COVER_HORIZONTAL) != 0) {
                mask[Mech.LOC_LLEG] = false;
                mask[Mech.LOC_RLEG] = false;
            }
            if (side == ToHitData.SIDE_FRONT) {
                if ((partialCover & LosEffects.COVER_LOWLEFT) != 0) {
                    mask[Mech.LOC_RLEG] = false;
                }
                if ((partialCover & LosEffects.COVER_LOWRIGHT) != 0) {
                    mask[Mech.LOC_LLEG] = false;
                }
                if ((partialCover & LosEffects.COVER_LEFT) != 0) {
                    mask[Mech.LOC_RARM] = false;
                    mask[Mech.LOC_RT] = false;
                }
                if ((partialCover & LosEffects.COVER_RIGHT) != 0) {
                    mask[Mech.LOC_LARM] = false;
                    mask[Mech.LOC_LT] = false;
                }
            } else {
                if ((partialCover & LosEffects.COVER_LOWLEFT) != 0) {
                    mask[Mech.LOC_LLEG] = false;
                }
                if ((partialCover & LosEffects.COVER_LOWRIGHT) != 0) {
                    mask[Mech.LOC_RLEG] = false;
                }
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
                // Can't target head with targeting computer.
                mask[Mech.LOC_HEAD] = false;
            }
            return mask;
        }

        public void closeDialog() {
            if (asd != null) {
                aimingAt = Entity.LOC_NONE;
                aimingMode = IAimingModes.AIM_MODE_NONE;
                asd.dispose();
                asd = null;
                updateTarget();
            }
        }

        /**
         * Enables the radiobuttons in the dialog.
         */
        public void setEnableAll(boolean enableAll) {
            if (asd != null) {
                asd.setEnableAll(enableAll);
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

        /**
         * Returns the name of aimed location.
         */
        public String getAimingLocation() {
            if ((target != null) && (aimingAt != Entity.LOC_NONE)
                    && (aimingMode != IAimingModes.AIM_MODE_NONE)) {
                if (target instanceof GunEmplacement) {
                    return GunEmplacement.HIT_LOCATION_NAMES[aimingAt];
                } else if (target instanceof Entity) {
                    return ((Entity) target).getLocationAbbrs()[aimingAt];
                }
            }
            return null;
        }

        /**
         * Sets the aiming mode, depending on the target and the attacker.
         * Against immobile mechs, targeting computer aiming mode will be used
         * if turned on. (This is a hack, but it's the resolution suggested by
         * the bug submitter, and I don't think it's half bad.
         */

        public void setAimingMode() {
            boolean allowAim;

            // TC against a mech
            allowAim = ((target != null) && (ce() != null)
                    && ce().hasAimModeTargComp() && ((target instanceof Mech)
                    || (target instanceof Tank)
                    || (target instanceof BattleArmor) || (target instanceof Protomech)));
            if (allowAim) {
                aimingMode = IAimingModes.AIM_MODE_TARG_COMP;
                return;
            }
            // immobile mech or gun emplacement
            allowAim = ((target != null) && ((target.isImmobile() && ((target instanceof Mech) || (target instanceof Tank))) || (target instanceof GunEmplacement)));
            if (allowAim) {
                aimingMode = IAimingModes.AIM_MODE_IMMOBILE;
                return;
            }

            aimingMode = IAimingModes.AIM_MODE_NONE;
        }

        /**
         * @return if are we in aiming mode
         */
        public boolean inAimingMode() {
            return aimingMode != IAimingModes.AIM_MODE_NONE;
        }

        /**
         * @return if a hit location currently selected.
         */
        public boolean isAimingAtLocation() {
            return aimingAt != Entity.LOC_NONE;
        }

        /**
         * should aimned shoots be allowed with the passed weapon
         *
         * @param weapon
         * @return
         */
        public boolean allowAimedShotWith(Mounted weapon) {
            return Compute.allowAimedShotWith(weapon, aimingMode);
        }

        /**
         * ActionListener, listens to the button in the dialog.
         */
        public void actionPerformed(ActionEvent ev) {
            closeDialog();
        }

        /**
         * ItemListener, listens to the radiobuttons in the dialog.
         */
        public void itemStateChanged(ItemEvent ev) {
            IndexedRadioButton icb = (IndexedRadioButton) ev.getSource();
            aimingAt = icb.getIndex();
            updateTarget();
        }
    }

    /**
     * Stop just ignoring events and actually stop listening to them.
     */
    public void removeAllListeners() {
        clientgui.getClient().getGame().removeGameListener(this);
        clientgui.getBoardView().removeBoardViewListener(this);
        clientgui.mechD.wPan.weaponList.removeListSelectionListener(this);
    }

    /**
     * Have the player select a target from the entities at the given coords.
     *
     * @param pos
     *            - the <code>Coords</code> containing targets.
     */
    private Targetable chooseTarget(Coords pos) {

        boolean friendlyFire = clientgui.getClient().getGame().getOptions()
                .booleanOption("friendly_fire"); //$NON-NLS-1$
        // Assume that we have *no* choice.
        Targetable choice = null;
        Enumeration<Entity> choices;

        // Get the available choices, depending on friendly fire
        if (friendlyFire) {
            choices = clientgui.getClient().getGame().getEntities(pos);
        } else {
            choices = clientgui.getClient().getGame().getEnemyEntities(pos, ce());
        }

        // Convert the choices into a List of targets.
        List<Targetable> targets = new ArrayList<Targetable>();
        while (choices.hasMoreElements()) {
            choice = choices.nextElement();
            if (!ce().equals(choice)) {
                targets.add(choice);
            }
        }

        // Is there a building in the hex?
        Building bldg = clientgui.getClient().getGame().getBoard()
                .getBuildingAt(pos);
        if (bldg != null) {
            targets.add(new BuildingTarget(pos, clientgui.getClient().getGame()
                    .getBoard(), false));
        }

        // Do we have a single choice?
        if (targets.size() == 1) {

            // Return that choice.
            choice = targets.get(0);

        }

        // If we have multiple choices, display a selection dialog.
        else if (targets.size() > 1) {
            String input = (String) JOptionPane
                    .showInputDialog(
                            clientgui,
                            Messages.getString(
                                    "FiringDisplay.ChooseTargetDialog.message", new Object[] { pos.getBoardNum() }), //$NON-NLS-1$                            JOptionPane.QUESTION_MESSAGE, null,
                            Messages.getString("FiringDisplay.ChooseTargetDialog.title"), //$NON-NLS-1$
                            JOptionPane.QUESTION_MESSAGE, null, SharedUtility
                                    .getDisplayArray(targets), null);
            choice = SharedUtility.getTargetPicked(targets, input);
        } // End have-choices

        // Return the chosen unit.
        return choice;

    } // End private Targetable chooseTarget( Coords )

}

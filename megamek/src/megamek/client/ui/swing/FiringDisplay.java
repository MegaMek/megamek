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
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import megamek.client.event.BoardViewEvent;
import megamek.client.ui.Messages;
import megamek.client.ui.SharedUtility;
import megamek.client.ui.swing.util.CommandAction;
import megamek.client.ui.swing.util.KeyCommandBind;
import megamek.client.ui.swing.util.MegaMekController;
import megamek.client.ui.swing.widget.MegamekButton;
import megamek.client.ui.swing.widget.SkinSpecification;
import megamek.common.Aero;
import megamek.common.AmmoType;
import megamek.common.BombType;
import megamek.common.Building;
import megamek.common.BuildingTarget;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.FighterSquadron;
import megamek.common.GameTurn;
import megamek.common.HexTarget;
import megamek.common.IAimingModes;
import megamek.common.IBoard;
import megamek.common.IGame;
import megamek.common.IGame.Phase;
import megamek.common.IHex;
import megamek.common.INarcPod;
import megamek.common.IPlayer;
import megamek.common.IdealHex;
import megamek.common.Infantry;
import megamek.common.LosEffects;
import megamek.common.Mech;
import megamek.common.Mounted;
import megamek.common.Protomech;
import megamek.common.Tank;
import megamek.common.TargetRoll;
import megamek.common.Targetable;
import megamek.common.Terrains;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.actions.AbstractEntityAction;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.AttackAction;
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
import megamek.common.options.OptionsConstants;

public class FiringDisplay extends StatusBarPhaseDisplay implements
        ItemListener, ListSelectionListener {
    /**
     *
     */
    private static final long serialVersionUID = -5586388490027013723L;

    /**
     * This enumeration lists all of the possible ActionCommands that can be
     * carried out during the firing phase.  Each command has a string for the
     * command plus a flag that determines what unit type it is appropriate for.
     *
     * @author arlith
     */
    public static enum FiringCommand implements PhaseCommand {
        FIRE_NEXT("fireNext"),
        FIRE_TWIST("fireTwist"),
        FIRE_FIRE("fireFire"),
        FIRE_SKIP("fireSkip"),
        FIRE_NEXT_TARG("fireNextTarg"),
        FIRE_MODE("fireMode"),
        FIRE_SPOT("fireSpot"),
        FIRE_FLIP_ARMS("fireFlipArms"),
        FIRE_FIND_CLUB("fireFindClub"),
        FIRE_STRAFE("fireStrafe"),
        FIRE_SEARCHLIGHT("fireSearchlight"),
        FIRE_CLEAR_TURRET("fireClearTurret"),
        FIRE_CLEAR_WEAPON("fireClearWeaponJam"),
        FIRE_CALLED("fireCalled"),
        FIRE_CANCEL("fireCancel"),
        FIRE_MORE("fireMore");

        String cmd;

        /**
         * Priority that determines this buttons order
         */
        public int priority;

        private FiringCommand(String c) {
            cmd = c;
        }

        public String getCmd() {
            return cmd;
        }

        public int getPriority() {
            return priority;
        }

        public void setPriority(int p) {
            priority = p;
        }

        public String toString() {
            return Messages.getString("FiringDisplay." + getCmd());
        }
    }

    // buttons
    private Map<FiringCommand, MegamekButton> buttons;

    // let's keep track of what we're shooting and at what, too
    protected int cen = Entity.NONE; // current entity number

    Targetable target; // target

    // HACK : track when we wan to show the target choice dialog.
    protected boolean showTargetChoice = true;

    // shots we have so far.
    protected Vector<AbstractEntityAction> attacks;

    // is the shift key held?
    protected boolean shiftheld;

    protected boolean twisting;

    protected Entity[] visibleTargets = null;

    protected int lastTargetID = -1;

    protected AimedShotHandler ash;
    
    protected boolean isStrafing = false;
    
    /**
     * Keeps track of the Coords that are in a strafing run.
     */
    private ArrayList<Coords> strafingCoords = new ArrayList<Coords>(5);

    /**
     * Creates and lays out a new firing phase display for the specified
     * clientgui.getClient().
     */
    public FiringDisplay(final ClientGUI clientgui) {
        super(clientgui);
        clientgui.getClient().getGame().addGameListener(this);

        clientgui.getBoardView().addBoardViewListener(this);

        shiftheld = false;

        // fire
        attacks = new Vector<AbstractEntityAction>();

        setupStatusBar(Messages
                .getString("FiringDisplay.waitingForFiringPhase")); //$NON-NLS-1$

        buttons = new HashMap<FiringCommand, MegamekButton>(
                (int) (FiringCommand.values().length * 1.25 + 0.5));
        for (FiringCommand cmd : FiringCommand.values()) {
            String title = Messages.getString("FiringDisplay." //$NON-NLS-1$
                    + cmd.getCmd());
            MegamekButton newButton = new MegamekButton(title,
                    SkinSpecification.UIComponents.PhaseDisplayButton.getComp());
            newButton.addActionListener(this);
            newButton.setActionCommand(cmd.getCmd());
            newButton.setEnabled(false);
            buttons.put(cmd, newButton);
        }
        numButtonGroups =
                (int) Math.ceil((buttons.size() + 0.0) / buttonsPerGroup);

        butDone.setText("<html><b>" + Messages.getString( //$NON-NLS-1$
                "FiringDisplay.Done") + "</b></html>"); //$NON-NLS-1$ //$NON-NLS-2$
        butDone.setEnabled(false);

        layoutScreen();

        setupButtonPanel();

        clientgui.bv.addKeyListener(this);

        // mech display.
        clientgui.mechD.wPan.weaponList.addListSelectionListener(this);
        clientgui.mechD.wPan.weaponList.addKeyListener(this);

        ash = new AimedShotHandler(this);

        registerKeyCommands();
    }

    /**
     * Register all of the <code>CommandAction</code>s for this panel display.
     */
    protected void registerKeyCommands() {
        MegaMekController controller = clientgui.controller;
        final StatusBarPhaseDisplay display = this;
        // Register the action for UNDO
        controller.registerCommandAction(KeyCommandBind.UNDO.cmd,
                new CommandAction() {

                    @Override
                    public boolean shouldPerformAction() {
                        if (!clientgui.getClient().isMyTurn()
                                || clientgui.bv.getChatterBoxActive()
                                || display.isIgnoringEvents()
                                || !display.isVisible()) {
                            return false;
                        } else {
                            return true;
                        }
                    }

                    @Override
                    public void performAction() {
                        removeLastFiring();
                    }
                });

        // Register the action for TWIST_LEFT
        controller.registerCommandAction(KeyCommandBind.TWIST_LEFT.cmd,
                new CommandAction() {

                    @Override
                    public boolean shouldPerformAction() {
                        if (!clientgui.getClient().isMyTurn()
                                || clientgui.bv.getChatterBoxActive()
                                || !display.isVisible()
                                || display.isIgnoringEvents()) {
                            return false;
                        } else {
                            return true;
                        }
                    }

                    @Override
                    public void performAction() {
                        updateFlipArms(false);
                        torsoTwist(0);
                    }
                });

        // Register the action for TWIST_RIGHT
        controller.registerCommandAction(KeyCommandBind.TWIST_RIGHT.cmd,
                new CommandAction() {

                    @Override
                    public boolean shouldPerformAction() {
                        if (!clientgui.getClient().isMyTurn()
                                || clientgui.bv.getChatterBoxActive()
                                || !display.isVisible()
                                || display.isIgnoringEvents()) {
                            return false;
                        } else {
                            return true;
                        }
                    }

                    @Override
                    public void performAction() {
                        updateFlipArms(false);
                        torsoTwist(1);
                    }
                });

        // Register the action for FIRE
        controller.registerCommandAction(KeyCommandBind.FIRE.cmd,
                new CommandAction() {

                    @Override
                    public boolean shouldPerformAction() {
                        if (!clientgui.getClient().isMyTurn()
                                || clientgui.bv.getChatterBoxActive()
                                || !display.isVisible()
                                || display.isIgnoringEvents()
                                || !buttons.get(FiringCommand.FIRE_FIRE)
                                        .isEnabled()) {
                            return false;
                        } else {
                            return true;
                        }
                    }

                    @Override
                    public void performAction() {
                        fire();
                    }
                });

        // Register the action for NEXT_WEAPON
        controller.registerCommandAction(KeyCommandBind.NEXT_WEAPON.cmd,
                new CommandAction() {

                    @Override
                    public boolean shouldPerformAction() {
                        if (!clientgui.getClient().isMyTurn()
                                || clientgui.bv.getChatterBoxActive()
                                || !display.isVisible()
                                || display.isIgnoringEvents()) {
                            return false;
                        } else {
                            return true;
                        }
                    }

                    @Override
                    public void performAction() {
                        nextWeapon();
                    }
                });

        // Register the action for PREV_WEAPON
        controller.registerCommandAction(KeyCommandBind.PREV_WEAPON.cmd,
                new CommandAction() {

                    @Override
                    public boolean shouldPerformAction() {
                        if (!clientgui.getClient().isMyTurn()
                                || clientgui.bv.getChatterBoxActive()
                                || !display.isVisible()
                                || display.isIgnoringEvents()) {
                            return false;
                        } else {
                            return true;
                        }
                    }

                    @Override
                    public void performAction() {
                        prevWeapon();
                    }
                });

        // Register the action for NEXT_UNIT
        controller.registerCommandAction(KeyCommandBind.NEXT_UNIT.cmd,
                new CommandAction() {

                    @Override
                    public boolean shouldPerformAction() {
                        if (!clientgui.getClient().isMyTurn()
                                || clientgui.bv.getChatterBoxActive()
                                || !display.isVisible()
                                || display.isIgnoringEvents()) {
                            return false;
                        } else {
                            return true;
                        }
                    }

                    @Override
                    public void performAction() {
                        selectEntity(clientgui.getClient()
                                .getNextEntityNum(cen));
                    }
                });

        // Register the action for PREV_UNIT
        controller.registerCommandAction(KeyCommandBind.PREV_UNIT.cmd,
                new CommandAction() {

                    @Override
                    public boolean shouldPerformAction() {
                        if (!clientgui.getClient().isMyTurn()
                                || clientgui.bv.getChatterBoxActive()
                                || !display.isVisible()
                                || display.isIgnoringEvents()) {
                            return false;
                        } else {
                            return true;
                        }
                    }

                    @Override
                    public void performAction() {
                        selectEntity(clientgui.getClient()
                                .getPrevEntityNum(cen));
                    }
                });

        // Register the action for NEXT_TARGET
        controller.registerCommandAction(KeyCommandBind.NEXT_TARGET.cmd,
                new CommandAction() {

                    @Override
                    public boolean shouldPerformAction() {
                        if (!clientgui.getClient().isMyTurn()
                                || clientgui.bv.getChatterBoxActive()
                                || !display.isVisible()
                                || display.isIgnoringEvents()) {
                            return false;
                        } else {
                            return true;
                        }
                    }

                    @Override
                    public void performAction() {
                        jumpToNextTarget();
                    }
                });

        // Register the action for PREV_TARGET
        controller.registerCommandAction(KeyCommandBind.PREV_TARGET.cmd,
                new CommandAction() {

                    @Override
                    public boolean shouldPerformAction() {
                        if (!clientgui.getClient().isMyTurn()
                                || clientgui.bv.getChatterBoxActive()
                                || !display.isVisible()
                                || display.isIgnoringEvents()) {
                            return false;
                        } else {
                            return true;
                        }
                    }

                    @Override
                    public void performAction() {
                        jumpToPrevTarget();
                    }
                });

        // Register the action for CLEAR
        controller.registerCommandAction(KeyCommandBind.CANCEL.cmd,
                new CommandAction() {

                    @Override
                    public boolean shouldPerformAction() {
                        if (clientgui.bv.getChatterBoxActive()
                                || clientgui.bv.getChatterBoxActive()
                                || !display.isVisible()
                                || display.isIgnoringEvents()) {
                            return false;
                        } else {
                            return true;
                        }
                    }

                    @Override
                    public void performAction() {
                        clear();
                    }
                });

    }

    protected ArrayList<MegamekButton> getButtonList() {
        ArrayList<MegamekButton> buttonList = new ArrayList<MegamekButton>();
        int i = 0;
        FiringCommand commands[] = FiringCommand.values();
        CommandComparator comparator = new CommandComparator();
        Arrays.sort(commands, comparator);
        for (FiringCommand cmd : commands) {
            if (cmd == FiringCommand.FIRE_NEXT
                    || cmd == FiringCommand.FIRE_MORE
                    || cmd == FiringCommand.FIRE_CANCEL) {
                continue;
            }
            if (i % buttonsPerGroup == 0) {
                buttonList.add(buttons.get(FiringCommand.FIRE_NEXT));
                i++;
            }

            buttonList.add(buttons.get(cmd));
            i++;

            if ((i + 1) % buttonsPerGroup == 0) {
                buttonList.add(buttons.get(FiringCommand.FIRE_MORE));
                i++;
            }
        }
        if (!buttonList.get(i - 1).getActionCommand()
                .equals(FiringCommand.FIRE_MORE.getCmd())) {
            while ((i + 1) % buttonsPerGroup != 0) {
                buttonList.add(null);
                i++;
            }
            buttonList.add(buttons.get(FiringCommand.FIRE_MORE));
        }
        return buttonList;
    }


    /**
     * Selects an entity, by number, for firing.
     */
    public void selectEntity(int en) {
        // clear any previously considered attacks
        if (en != cen) {
            target(null);
            clearAttacks();
            refreshAll();
        }
        
        if ((ce() != null) && ce().isWeapOrderChanged()) {
            clientgui.getClient().sendEntityWeaponOrderUpdate(ce());
        }
        
        if (clientgui.getClient().isMyTurn()) {
            setStatusBarText(Messages
                    .getString("FiringDisplay.its_your_turn")); //$NON-NLS-1$
        }

        if (clientgui.getClient().getGame().getEntity(en) != null) {

            cen = en;
            clientgui.setSelectedEntityNum(en);
            clientgui.mechD.displayEntity(ce());

            // If the selected entity is not on the board, use the next one.
            // ASSUMPTION: there will always be *at least one* entity on map.
            if (ce().getPosition() == null) {

                // Walk through the list of entities for this player.
                for (int nextId = clientgui.getClient().getNextEntityNum(en);
                     nextId != en; nextId = clientgui
                        .getClient().getNextEntityNum(nextId)) {

                    if (clientgui.getClient().getGame().getEntity(nextId)
                                 .getPosition() != null) {
                        cen = nextId;
                        break;
                    }

                } // Check the player's next entity.

                // We were *supposed* to have found an on-board entity.
                if (ce().getPosition() == null) {
                    System.err.println("FiringDisplay: could " + //$NON-NLS-1$
                            "not find an on-board entity: " + en); //$NON-NLS-1$
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

            if (!ce().isOffBoard()) {
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
            updateStrafe();
        } else {
            System.err.println("FiringDisplay: tried to " + //$NON-NLS-1$
                    "select non-existant entity: " + en); //$NON-NLS-1$
        }
        

        if (GUIPreferences.getInstance().getBoolean("FiringSolutions")) {
            setFiringSolutions();
        } else {
            clientgui.getBoardView().clearFiringSolutionData();
        }
    }

    public void setFiringSolutions() {
        // If no Entity is selected, exit
        if (cen == Entity.NONE) {
            return;
        }

        IGame game = clientgui.getClient().getGame();
        IPlayer localPlayer = clientgui.getClient().getLocalPlayer();
        if (!GUIPreferences.getInstance().getFiringSolutions()) {
            return;
        }
        Map<Integer, ToHitData> fs = new HashMap<Integer, ToHitData>();
        for (Entity target : game.getEntitiesVector()) {
            boolean friendlyFire = game.getOptions().booleanOption(
                    "friendly_fire"); //$NON-NLS-1$
            boolean enemyTarget = target.getOwner().isEnemyOf(ce().getOwner());
            if ((target.getId() != cen)
                && (friendlyFire || enemyTarget)
                && (!enemyTarget || target.hasSeenEntity(localPlayer)
                    || target.hasDetectedEntity(localPlayer))
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
    protected void beginMyTurn() {
        target = null;

        if (!clientgui.bv.isMovingUnits()) {
            clientgui.setDisplayVisible(true);
        }
        clientgui.bv.clearFieldofF();

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
            if (numButtonGroups > 1)
                buttons.get(FiringCommand.FIRE_MORE).setEnabled(true);
            setFireCalledEnabled(clientgui.getClient().getGame().getOptions()
                    .booleanOption("tacops_called_shots"));
            clientgui.getBoardView().select(null);
        }
    }

    /**
     * Does end turn stuff.
     */
    protected void endMyTurn() {
        // end my turn, then.
        IGame game = clientgui.getClient().getGame();
        Entity next = game.getNextEntity(game.getTurnIndex());
        if ((game.getPhase() == IGame.Phase.PHASE_FIRING)
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
        clientgui.bv.clearFiringSolutionData();
        clientgui.bv.clearStrafingCoords();
        clientgui.bv.clearFieldofF();
        clientgui.setSelectedEntityNum(Entity.NONE);
        disableButtons();

        clearVisibleTargets();
    }

    /**
     * Disables all buttons in the interface
     */
    protected void disableButtons() {
        setFireEnabled(false);
        setSkipEnabled(false);
        setTwistEnabled(false);
        setSpotEnabled(false);
        setFindClubEnabled(false);
        buttons.get(FiringCommand.FIRE_MORE).setEnabled(false);
        setNextEnabled(false);
        butDone.setEnabled(false);
        setNextTargetEnabled(false);
        setFlipArmsEnabled(false);
        setFireModeEnabled(false);
        setFireCalledEnabled(false);
        setFireClearTurretEnabled(false);
        setFireClearWeaponJamEnabled(false);
        setStrafeEnabled(false);
    }

    /**
     * Fire Mode - Adds a Fire Mode Change to the current Attack Action
     */
    protected void changeMode() {
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
            clientgui.systemMessage(Messages.getString(
                    "FiringDisplay.switched", new Object[] { m.getName(),
                            m.curMode().getDisplayableName(true) })); //$NON-NLS-1$
        } else {
            clientgui.systemMessage(Messages.getString(
                    "FiringDisplay.willSwitch", new Object[] { m.getName(), //$NON-NLS-1$
                            m.pendingMode().getDisplayableName(true) }));
        }

        updateTarget();
        clientgui.mechD.wPan.displayMech(ce());
        clientgui.mechD.wPan.selectWeapon(wn);
    }

    /**
     * Called Shots - changes the current called shots selection
     */
    protected void changeCalled() {
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

        List<Entity> vec = clientgui.getClient().getGame()
                .getValidTargets(ce());
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
            tree.add(vec.get(i));
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
     * Get the next target. Return null if we don't have any targets.
     */
    private Entity getPrevTarget() {
        if (visibleTargets == null) {
            return null;
        }

        lastTargetID--;

        if (lastTargetID < 0) {
            lastTargetID = visibleTargets.length - 1;
        }

        return visibleTargets[lastTargetID];
    }

    /**
     * Jump to our next target. If there isn't one, well, don't do anything.
     */
    private void jumpToPrevTarget() {
        Entity targ = getPrevTarget();

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
                Entity attacker = waa
                        .getEntity(clientgui.getClient().getGame());
                Targetable target1 = waa.getTarget(clientgui.getClient()
                        .getGame());
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
                    waa2.setStrafing(waa.isStrafing());
                    waa2.setStrafingFirstShot(waa.isStrafingFirstShot());
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
                Entity attacker = waa
                        .getEntity(clientgui.getClient().getGame());
                Targetable target1 = waa.getTarget(clientgui.getClient()
                        .getGame());
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
                    waa2.setStrafing(waa.isStrafing());
                    waa2.setStrafingFirstShot(waa.isStrafingFirstShot());
                    newAttacks.addElement(waa2);
                }
            }
        }
        
        // If the user picked a hex along the flight path, server needs to know
        if ((target instanceof Entity) && Compute.isGroundToAir(ce(), target)) {
            Coords targetPos = ((Entity)target).getPlayerPickedPassThrough(cen);
            if (targetPos != null) {
                clientgui.getClient().sendPlayerPickedPassThrough(
                        ((Entity) target).getId(), cen, targetPos);
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

        if ((ce() != null) && ce().isWeapOrderChanged()) {
            clientgui.getClient().sendEntityWeaponOrderUpdate(ce());
        }
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
        String input = (String) JOptionPane.showInputDialog(clientgui, Messages
                .getString("FiringDisplay.ClearWeaponJam" + ".question"), //$NON-NLS-1$ //$NON-NLS-2$
                Messages.getString("FiringDisplay.ClearWeaponJam.title"), //$NON-NLS-1$
                //$NON-NLS-1$
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
    protected void doSearchlight() {
        // validate
        if ((ce() == null) || (target == null)) {
            throw new IllegalArgumentException(
                    "current searchlight parameters are invalid"); //$NON-NLS-1$
        }

        if (!SearchlightAttackAction.isPossible(
                clientgui.getClient().getGame(), cen, target, null)) {
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
    
    private void doStrafe() {
        target(null);
        clearAttacks();        
        isStrafing = true;
        setStatusBarText(Messages
                .getString("FiringDisplay.Strafing.StatusLabel"));
        refreshAll();
    }
    
    private void updateStrafingTargets() {
        final IGame game = clientgui.getClient().getGame();
        final int weaponId = clientgui.mechD.wPan.getSelectedWeaponNum();
        final Mounted m = ce().getEquipment(weaponId);
        ToHitData toHit;
        StringBuffer toHitBuff = new StringBuffer();
        setFireEnabled(true);
        for (Coords c : strafingCoords) {
            for (Entity t : game.getEntitiesVector(c)) {
                // Airborne units cannot be strafed
                if (t.isAirborne()) {
                    continue;
                }
                // Can't shoot at infantry in the building
                // Instead, strafe will hit the building, which could damage Inf
                if (Compute.isInBuilding(game, t) && (t instanceof Infantry)) {
                    continue;
                }
                
                toHit = WeaponAttackAction.toHit(game, cen, t, weaponId,
                        Entity.LOC_NONE, IAimingModes.AIM_MODE_NONE, true);
                toHitBuff.append(t.getShortName() + ": ");
                toHitBuff.append(toHit.getDesc());
                toHitBuff.append("\n");
                if (m.getType().hasFlag(WeaponType.F_AUTO_TARGET)
                        || (toHit.getValue() == TargetRoll.IMPOSSIBLE)) {
                    setFireEnabled(false);
                }
            }
            Building bldg = game.getBoard().getBuildingAt(c); 
            if (bldg != null) {
                Targetable t = new BuildingTarget(c, game.getBoard(), false);
                toHit = WeaponAttackAction.toHit(game, cen, t, weaponId,
                        Entity.LOC_NONE, IAimingModes.AIM_MODE_NONE, true);
                toHitBuff.append(t.getDisplayName() + ": ");
                toHitBuff.append(toHit.getDesc());
                toHitBuff.append("\n");
            }
            Targetable hexTarget = new HexTarget(c, game.getBoard(),
                    HexTarget.TYPE_HEX_CLEAR);
            toHit = WeaponAttackAction.toHit(game, cen, hexTarget, weaponId,
                    Entity.LOC_NONE, IAimingModes.AIM_MODE_NONE, true);
            if (m.getType().hasFlag(WeaponType.F_AUTO_TARGET)
                    || (toHit.getValue() == TargetRoll.IMPOSSIBLE)) {
                setFireEnabled(false);
                if (toHitBuff.length() < 1) {
                    toHitBuff.append(toHit.getDesc());
                }
            }
            // Could check legality on buildings, but I don't believe there are
            // any weapons that are still legal that aren't legal on buildings            
        }
        clientgui.mechD.wPan.toHitText.setText(toHitBuff.toString());
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
        BombPayloadDialog bombsDialog = new BombPayloadDialog(
                clientgui.frame,
                Messages.getString("FiringDisplay.BombNumberDialog" + ".title"), //$NON-NLS-1$
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
        final IGame game = clientgui.getClient().getGame();
        // get the selected weaponnum
        final int weaponNum = clientgui.mechD.wPan.getSelectedWeaponNum();
        Mounted mounted = ce().getEquipment(weaponNum);

        // validate
        if ((ce() == null)
                || (target == null && (!isStrafing || strafingCoords.size() == 0))
                || (mounted == null)
                || !(mounted.getType() instanceof WeaponType)) {
            throw new IllegalArgumentException(
                    "current fire parameters are invalid"); //$NON-NLS-1$
        }
        // check if we now shoot at a target in the front arc and previously
        // shot a target in side/rear arc that then was primary target
        // if so, ask and tell the user that to-hits will change
        if (!game.getOptions().booleanOption("no_forced_primary_targets")
                && (ce() instanceof Mech) || (ce() instanceof Tank)
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
                Targetable oldTarget = oldWaa.getTarget(game);
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

        ArrayList<Targetable> targets = new ArrayList<Targetable>();
        if (isStrafing) {
            for (Coords c : strafingCoords) {
                targets.add(new HexTarget(c, game.getBoard(),
                        Targetable.TYPE_HEX_CLEAR));
                Building bldg = game.getBoard().getBuildingAt(c); 
                if (bldg != null) {
                    targets.add(new BuildingTarget(c, game.getBoard(), false));
                }
                // Target all ground units (non-airborne, VTOLs still count)
                for (Entity t : game.getEntitiesVector(c)) {
                    boolean infInBuilding = Compute.isInBuilding(game, t)
                            && (t instanceof Infantry);
                    if (!t.isAirborne() && !infInBuilding) {
                        targets.add(t);
                    }
                }
            }
        } else {
            targets.add(target);
        }
        
        boolean firstShot = true;
        for (Targetable t : targets) {
        
            WeaponAttackAction waa;
            if (!mounted.getType().hasFlag(WeaponType.F_ARTILLERY)) {
                waa = new WeaponAttackAction(cen, t.getTargetType(),
                        t.getTargetId(), weaponNum);
            } else {
                waa = new ArtilleryAttackAction(cen, t.getTargetType(),
                        t.getTargetId(), weaponNum, game);
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
                    && (((WeaponType) mounted.getType()).getAmmoType() != AmmoType.T_NA)
                    && (mounted.getLinked().getType() instanceof AmmoType)) {
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
            waa.setStrafing(isStrafing);
            waa.setStrafingFirstShot(firstShot);
            firstShot = false;

            // add the attack to our temporary queue
            attacks.addElement(waa);

            // and add it into the game, temporarily
            game.addAction(waa);
        
        }
        clientgui.minimap.drawMap();

        // set the weapon as used
        mounted.setUsedThisRound(true);

        // find the next available weapon
        int nextWeapon = clientgui.mechD.wPan.getNextWeaponNum();

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
        Mounted nextMounted = ce().getEquipment(nextWeapon);
        if (!mounted.getType().hasFlag(WeaponType.F_VGL)
                && (nextMounted != null)
                && nextMounted.getType().hasFlag(WeaponType.F_VGL)) {
            clientgui.mechD.wPan.setPrevTarget(target);
        }
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
        int weaponId = clientgui.mechD.wPan.selectNextWeapon();

        if (ce().getId() != clientgui.mechD.wPan.getSelectedEntityId()) {
            clientgui.mechD.wPan.displayMech(ce());
        }

        if (weaponId == -1) {
            setFireModeEnabled(false);
        } else {
            Mounted m = ce().getEquipment(weaponId);
            setFireModeEnabled(m.isModeSwitchable());
        }
        updateTarget();
    }

    /**
     * Skips to the previous weapon
     */
    void prevWeapon() {
        if (ce() == null) {
            return;
        }
        int weaponId = clientgui.mechD.wPan.selectPrevWeapon();

        if (ce().getId() != clientgui.mechD.wPan.getSelectedEntityId()) {
            clientgui.mechD.wPan.displayMech(ce());
        }

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
    protected void doSpot() {
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
    protected void clearAttacks() {
        isStrafing = false;
        strafingCoords.clear();
        clientgui.bv.clearStrafingCoords();
        
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
    protected void removeTempAttacks() {
        // remove temporary attacks from game & board
        clientgui.getClient().getGame().removeActionsFor(cen);
        clientgui.bv.removeAttacksFor(ce());
    }

    /**
     * removes the last action
     */
    protected void removeLastFiring() {
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
     * Refreshes all displays.
     */
    protected void refreshAll() {
        if (ce() == null) {
            return;
        }
        clientgui.bv.redrawEntity(ce());
        clientgui.mechD.displayEntity(ce());
        clientgui.mechD.showPanel("weapons"); //$NON-NLS-1$
        clientgui.mechD.wPan.selectFirstWeapon();
        updateTarget();
    }

    /**
     * Targets something
     */
    public void target(Targetable t) {
        if (ce() == null) {
            return;
        }
        final int weaponId = clientgui.mechD.wPan.getSelectedWeaponNum();
        Mounted weapon = ce().getEquipment(weaponId); 
        // Some weapons pick an automatic target
        if ((weapon != null) && weapon.getType().hasFlag(WeaponType.F_VGL)) {
            int facing;
            if (ce().isSecondaryArcWeapon(weaponId)) {
                facing = ce().getSecondaryFacing();
            } else {
                facing = ce().getFacing();
            }
            facing = (facing + weapon.getFacing()) % 6;
            Coords c = ce().getPosition().translated(facing);
            IBoard board = clientgui.getClient().getGame().getBoard();
            Targetable hexTarget = 
                    new HexTarget(c, board, Targetable.TYPE_HEX_CLEAR);
            // Ignore events that will be generated by the select/cursor calls
            setIgnoringEvents(true);
            clientgui.getBoardView().select(c);
            setIgnoringEvents(false);
            target = hexTarget;
        } else {
            target = t;
        }
        if ((target instanceof Entity) && Compute.isGroundToAir(ce(), target)) {
            Coords targetPos = Compute.getClosestFlightPath(cen, ce()
                    .getPosition(), (Entity) target);
            clientgui.getBoardView().cursor(targetPos);
        }
        ash.setAimingMode();
        updateTarget();
        ash.showDialog();
    }

    /**
     * Targets something
     */
    public void updateTarget() {
        setFireEnabled(false);
        IGame game = clientgui.getClient().getGame();
        // allow spotting
        if ((ce() != null) && ce().canSpot() && (target != null)
                && game.getOptions().booleanOption("indirect_fire")) { //$NON-NLS-1$)
            boolean hasLos = LosEffects.calculateLos(game, cen, target)
                    .canSee();
            // In double blind, we need to "spot" the target as well as LoS
            if (game.getOptions().booleanOption("double_blind")
                    && !Compute.inVisualRange(game, ce(), target)
                    && !Compute.inSensorRange(game, ce(), target, null)) {
                hasLos = false;
            }
            setSpotEnabled(hasLos);
        } else {
            setSpotEnabled(false);
        }

        // update target panel
        final int weaponId = clientgui.mechD.wPan.getSelectedWeaponNum();
        if (isStrafing && weaponId != -1) {
            clientgui.mechD.wPan.wTargetR.setText(Messages
                    .getString("FiringDisplay.Strafing.TargetLabel"));
            updateStrafingTargets();
        } else if ((target != null) && (target.getPosition() != null)
            && (weaponId != -1) && (ce() != null)) {
            ToHitData toHit;
            if (ash.inAimingMode()) {
                Mounted weapon = ce().getEquipment(weaponId);
                boolean aiming = ash.isAimingAtLocation()
                        && ash.allowAimedShotWith(weapon);
                ash.setEnableAll(aiming);
                if (aiming) {
                    toHit = WeaponAttackAction.toHit(game, cen, target,
                            weaponId, ash.getAimingAt(), ash.getAimingMode(),
                            false);
                    clientgui.mechD.wPan.wTargetR.setText(target
                            .getDisplayName()
                            + " (" + ash.getAimingLocation() + ")"); //$NON-NLS-1$ // $NON-NLS-2$
                } else {
                    toHit = WeaponAttackAction.toHit(game, cen, target,
                            weaponId, Entity.LOC_NONE,
                            IAimingModes.AIM_MODE_NONE, false);
                    clientgui.mechD.wPan.wTargetR.setText(target
                            .getDisplayName());
                }
                ash.setPartialCover(toHit.getCover());
            } else {
                toHit = WeaponAttackAction.toHit(game, cen, target, weaponId,
                        Entity.LOC_NONE, IAimingModes.AIM_MODE_NONE, false);
                clientgui.mechD.wPan.wTargetR.setText(target.getDisplayName());
            }
            int effectiveDistance = Compute.effectiveDistance(
                    game, ce(), target);
            clientgui.mechD.wPan.wRangeR.setText("" + effectiveDistance); //$NON-NLS-1$
            Mounted m = ce().getEquipment(weaponId);
            // If we have a Centurion Weapon System selected, we may need to
            //  update ranges.
            if (m.getType().hasFlag(WeaponType.F_CWS)) {
                clientgui.mechD.wPan.selectWeapon(weaponId);
            }
            if (m.isUsedThisRound()) {
                clientgui.mechD.wPan.wToHitR.setText(Messages
                        .getString("FiringDisplay.alreadyFired")); //$NON-NLS-1$
                setFireEnabled(false);
            } else if (m.getType().hasFlag(WeaponType.F_AUTO_TARGET)) {
                clientgui.mechD.wPan.wToHitR.setText(Messages
                        .getString("FiringDisplay.autoFiringWeapon"));
                //$NON-NLS-1$
                setFireEnabled(false);
            } else if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
                clientgui.mechD.wPan.wToHitR.setText(toHit.getValueAsString());
                setFireEnabled(false);
            } else if (toHit.getValue() == TargetRoll.AUTOMATIC_FAIL) {
                clientgui.mechD.wPan.wToHitR.setText(toHit.getValueAsString());
                setFireEnabled(true);
            } else {
                boolean natAptGunnery = ce().getCrew().getOptions()
                        .booleanOption(OptionsConstants.PILOT_APTITUDE_GUNNERY);
                clientgui.mechD.wPan.wToHitR.setText(toHit.getValueAsString()
                        + " ("
                        + Compute.oddsAbove(toHit.getValue(), natAptGunnery)
                        + "%)"); //$NON-NLS-1$
                // $NON-NLS-2$
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

        if ((weaponId != -1) && (ce() != null) && !isStrafing) {
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
     * @param twistDir An <code>int</code> specifying wether we're twisting left or
     *                 right, 0 if we're twisting to the left, 1 if to the right.
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

        Coords evtCoords = b.getCoords();
        if (clientgui.getClient().isMyTurn() && (evtCoords != null)
            && (ce() != null)) {
            if (isStrafing) {
                if (validStrafingCoord(evtCoords)) {
                    strafingCoords.add(evtCoords);
                    clientgui.bv.addStrafingCoords(evtCoords);
                    updateStrafingTargets();
                }
            } else if (!evtCoords.equals(ce().getPosition())){
                // HACK : sometimes we don't show the target choice window
                Targetable targ = null;
                if (showTargetChoice) {
                    targ = chooseTarget(evtCoords);
                }
                if (shiftheld) {
                    updateFlipArms(false);
                    torsoTwist(b.getCoords());
                } else if (targ != null) {
                    if ((targ instanceof Entity) 
                            && Compute.isGroundToAir(ce(), targ)) {
                        Entity entTarg = (Entity)targ;
                        boolean alreadyShotAt = false;
                        List<EntityAction> actions = clientgui.getClient()
                                .getGame().getActionsVector();
                        for (EntityAction action : actions) {
                            if (!(action instanceof AttackAction)) {
                                continue;
                            }
                            AttackAction aa = (AttackAction)action;
                            if ((action.getEntityId() == cen) 
                                    && (aa.getTargetId() == entTarg.getId())) {
                                alreadyShotAt = true;
                            }
                        }
                        if (!alreadyShotAt) {
                            entTarg.setPlayerPickedPassThrough(cen, evtCoords);
                        }
                    }
                    target(targ);
                }
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
                String playerName;
                if (e.getPlayer() != null) {
                    playerName = e.getPlayer().getName();
                } else {
                    playerName = "Unknown";
                }
                setStatusBarText(Messages.getString(
                        "FiringDisplay.its_others_turn", //$NON-NLS-1$
                        new Object[] { playerName }));
            }
        }
    }

    @Override
    public void gamePhaseChange(GamePhaseChangeEvent e) {

        // In case of a /reset command, ensure the state gets reset
        if (clientgui.getClient().getGame().getPhase() 
                == IGame.Phase.PHASE_LOUNGE) {
            endMyTurn();
        }

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (clientgui.getClient().isMyTurn()
                && (clientgui.getClient().getGame().getPhase() 
                        != IGame.Phase.PHASE_FIRING)) {
            endMyTurn();
        }
        // if we're ending the firing phase, unregister stuff.
        if (clientgui.getClient().getGame().getPhase() 
                == IGame.Phase.PHASE_FIRING) {
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

        if (ev.getActionCommand().equals(FiringCommand.FIRE_FIRE.getCmd())) {
            fire();
        } else if (ev.getActionCommand().equals(FiringCommand.FIRE_SKIP.getCmd())) {
            nextWeapon();
        } else if (ev.getActionCommand().equals(FiringCommand.FIRE_TWIST.getCmd())) {
            twisting = true;
        } else if (ev.getActionCommand().equals(FiringCommand.FIRE_NEXT.getCmd())) {
            selectEntity(clientgui.getClient().getNextEntityNum(cen));
        } else if (ev.getActionCommand().equals(FiringCommand.FIRE_MORE.getCmd())) {
            currentButtonGroup++;
            currentButtonGroup %= numButtonGroups;
            setupButtonPanel();
        } else if (ev.getActionCommand().equals(FiringCommand.FIRE_FIND_CLUB.getCmd())) {
            findClub();
        } else if (ev.getActionCommand().equals(FiringCommand.FIRE_SPOT.getCmd())) {
            doSpot();
        } else if (ev.getActionCommand().equals(FiringCommand.FIRE_NEXT_TARG.getCmd())) {
            jumpToNextTarget();
        } else if (ev.getActionCommand().equals(FiringCommand.FIRE_FLIP_ARMS.getCmd())) {
            updateFlipArms(!ce().getArmsFlipped());
            // Fire Mode - More Fire Mode button handling - Rasia
        } else if (ev.getActionCommand().equals(FiringCommand.FIRE_MODE.getCmd())) {
            changeMode();
        } else if (ev.getActionCommand().equals(FiringCommand.FIRE_CALLED.getCmd())) {
            changeCalled();
        } else if (("changeSinks".equalsIgnoreCase(ev.getActionCommand()))
                   || (ev.getActionCommand().equals(FiringCommand.FIRE_CANCEL.getCmd()))) {
            clear();
        } else if (ev.getActionCommand().equals(FiringCommand.FIRE_SEARCHLIGHT.getCmd())) {
            doSearchlight();
        } else if (ev.getActionCommand().equals(FiringCommand.FIRE_CLEAR_TURRET.getCmd())) {
            doClearTurret();
        } else if (ev.getActionCommand().equals(FiringCommand.FIRE_CLEAR_WEAPON.getCmd())) {
            doClearWeaponJam();
        } else if (ev.getActionCommand().equals(FiringCommand.FIRE_STRAFE.getCmd())) {
            doStrafe();
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

    protected void updateSearchlight() {
        setSearchlightEnabled((ce() != null)
                && (target != null)
                && ce().isUsingSpotlight()
                && ce().getCrew().isActive()
                && SearchlightAttackAction.isPossible(clientgui.getClient()
                        .getGame(), cen, target, null)
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

    private void updateStrafe() {
        if (ce() instanceof Aero) {
            Aero a = (Aero) ce();
            setStrafeEnabled(a.getAltitude() <= 3 && !a.isSpheroid());
        } else {
            setStrafeEnabled(false);
        }
    }

    protected void setFireEnabled(boolean enabled) {
        buttons.get(FiringCommand.FIRE_FIRE).setEnabled(enabled);
        clientgui.getMenuBar().setFireFireEnabled(enabled);
    }

    protected void setTwistEnabled(boolean enabled) {
        buttons.get(FiringCommand.FIRE_TWIST).setEnabled(enabled);
        clientgui.getMenuBar().setFireTwistEnabled(enabled);
    }

    protected void setSkipEnabled(boolean enabled) {
        buttons.get(FiringCommand.FIRE_SKIP).setEnabled(enabled);
        clientgui.getMenuBar().setFireSkipEnabled(enabled);
    }

    protected void setFindClubEnabled(boolean enabled) {
        buttons.get(FiringCommand.FIRE_FIND_CLUB).setEnabled(enabled);
        clientgui.getMenuBar().setFireFindClubEnabled(enabled);
    }

    protected void setNextTargetEnabled(boolean enabled) {
        buttons.get(FiringCommand.FIRE_NEXT_TARG).setEnabled(enabled);
        clientgui.getMenuBar().setFireNextTargetEnabled(enabled);
    }

    protected void setFlipArmsEnabled(boolean enabled) {
        buttons.get(FiringCommand.FIRE_FLIP_ARMS).setEnabled(enabled);
        clientgui.getMenuBar().setFireFlipArmsEnabled(enabled);
    }

    protected void setSpotEnabled(boolean enabled) {
        buttons.get(FiringCommand.FIRE_SPOT).setEnabled(enabled);
        clientgui.getMenuBar().setFireSpotEnabled(enabled);
    }

    protected void setSearchlightEnabled(boolean enabled) {
        buttons.get(FiringCommand.FIRE_SEARCHLIGHT).setEnabled(enabled);
        clientgui.getMenuBar().setFireSearchlightEnabled(enabled);
    }

    protected void setFireModeEnabled(boolean enabled) {
        buttons.get(FiringCommand.FIRE_MODE).setEnabled(enabled);
        clientgui.getMenuBar().setFireModeEnabled(enabled);
    }

    protected void setFireCalledEnabled(boolean enabled) {
        buttons.get(FiringCommand.FIRE_CALLED).setEnabled(enabled);
        clientgui.getMenuBar().setFireCalledEnabled(enabled);
    }

    protected void setFireClearTurretEnabled(boolean enabled) {
        buttons.get(FiringCommand.FIRE_CLEAR_TURRET).setEnabled(enabled);
        clientgui.getMenuBar().setFireClearTurretEnabled(enabled);
    }

    protected void setFireClearWeaponJamEnabled(boolean enabled) {
        buttons.get(FiringCommand.FIRE_CLEAR_WEAPON).setEnabled(enabled);
        clientgui.getMenuBar().setFireClearWeaponJamEnabled(enabled);
    }

    protected void setStrafeEnabled(boolean enabled) {
        buttons.get(FiringCommand.FIRE_STRAFE).setEnabled(enabled);
        clientgui.getMenuBar().setFireClearWeaponJamEnabled(enabled);
    }    
    

    protected void setNextEnabled(boolean enabled) {
        buttons.get(FiringCommand.FIRE_NEXT).setEnabled(enabled);
        clientgui.getMenuBar().setFireNextEnabled(enabled);
    }

    @Override
    public void clear() {
        if (clientgui.getClient().isMyTurn()) {
            setStatusBarText(Messages
                    .getString("FiringDisplay.its_your_turn")); //$NON-NLS-1$
        }
        if ((target instanceof Entity) 
                && Compute.isGroundToAir(ce(), target)) {
            ((Entity)target).setPlayerPickedPassThrough(cen, null);
        }
        target(null);
        clearAttacks();        
        clientgui.getBoardView().select(null);
        clientgui.getBoardView().cursor(null);
        refreshAll();
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
     * @param pos - the <code>Coords</code> containing targets.
     */
    private Targetable chooseTarget(Coords pos) {
        final IGame game = clientgui.getClient().getGame();
        boolean friendlyFire = game.getOptions().booleanOption("friendly_fire"); //$NON-NLS-1$
        // Assume that we have *no* choice.
        Targetable choice = null;
        Iterator<Entity> choices;

        int wn = clientgui.mechD.wPan.getSelectedWeaponNum();
        Mounted weap = ce().getEquipment(wn);
        
        // Check for weapon/ammo types that should automatically target hexes
        if ((weap != null) && (weap.getLinked() != null) 
                && (weap.getLinked().getType() instanceof AmmoType)) {
            AmmoType aType = (AmmoType)weap.getLinked().getType();
            long munitionType = aType.getMunitionType();
            // Mek mortar flares should default to deliver flare
            if ((aType.getAmmoType() == AmmoType.T_MEK_MORTAR) 
                    && (munitionType == AmmoType.M_FLARE)) {
                return new HexTarget(pos, game.getBoard(),
                        Targetable.TYPE_FLARE_DELIVER);
            // Certain mek mortar types should target hexes
            } else if ((aType.getAmmoType() == AmmoType.T_MEK_MORTAR)
                    && ((munitionType == AmmoType.M_AIRBURST) 
                            || (munitionType == AmmoType.M_SMOKE_WARHEAD))) {
                return new HexTarget(pos, game.getBoard(),
                        Targetable.TYPE_HEX_CLEAR);
            } else if (munitionType == AmmoType.M_MINE_CLEARANCE) {
                return new HexTarget(pos, game.getBoard(),
                        Targetable.TYPE_HEX_CLEAR);
            }
        }
        // Get the available choices, depending on friendly fire
        if (friendlyFire) {
            choices = game.getEntities(pos);
        } else {
            choices = game.getEnemyEntities(pos, ce());
        }

        // Convert the choices into a List of targets.
        List<Targetable> targets = new ArrayList<Targetable>();
        final IPlayer localPlayer = clientgui.getClient().getLocalPlayer();
        while (choices.hasNext()) {
            Targetable t = choices.next();
            boolean isSensorReturn = false;
            boolean isVisible = true;
            if (t instanceof Entity) {
                isSensorReturn = ((Entity) t).isSensorReturn(localPlayer);
                isVisible = ((Entity) t).hasSeenEntity(localPlayer);
            }
            if (!ce().equals(t) && !isSensorReturn && isVisible) {
                targets.add(t);
            }
        }
        
        // If there aren't other targets, check for targets flying over pos
        if (targets.size() == 0) {
            List<Entity> flyovers = 
                    clientgui.getBoardView().getEntitiesFlyingOver(pos);
            for (Entity e : flyovers) {
                if (!targets.contains(e)) {
                    targets.add(e);
                }
            }
        }

        // Is there a building in the hex?
        Building bldg = clientgui.getClient().getGame().getBoard()
                .getBuildingAt(pos);
        if (bldg != null) {
            targets.add(new BuildingTarget(pos, clientgui.getClient().getGame()
                    .getBoard(), false));
        }
        
        // If we clicked on a wooded hex with no other targets, clear woods
        if (targets.size() == 0) {
            IHex hex = game.getBoard().getHex(pos);
            if (hex.containsTerrain(Terrains.WOODS)
                    || hex.containsTerrain(Terrains.JUNGLE)) {
                targets.add(new HexTarget(pos, game.getBoard(),
                        Targetable.TYPE_HEX_CLEAR));
            }
        }

        // Do we have a single choice?
        if (targets.size() == 1) {

            // Return that choice.
            choice = targets.get(0);

        }

        // If we have multiple choices, display a selection dialog.
        else if (targets.size() > 1) {
            String input = (String) JOptionPane
                    .showInputDialog(clientgui,
                            Messages.getString(
                                    "FiringDisplay.ChooseTargetDialog.message",
                                    new Object[] { pos.getBoardNum() }),
                            //$NON-NLS-1$                            JOptionPane.QUESTION_MESSAGE, null,
                            Messages.getString("FiringDisplay.ChooseTargetDialog.title"), //$NON-NLS-1$
                            JOptionPane.QUESTION_MESSAGE, null, SharedUtility
                                    .getDisplayArray(targets), null);
            choice = SharedUtility.getTargetPicked(targets, input);
        } // End have-choices

        // Return the chosen unit.
        return choice;

    } // End private Targetable chooseTarget( Coords )

    public Targetable getTarget() {
        return target;
    }
    
    private boolean validStrafingCoord(Coords newCoord) {
        // Only Aeros can strafe...
        if (ce() == null || !(ce() instanceof Aero)) {
            return false;
        }
        Aero a = (Aero)ce();
        
        // Can't update strafe hexes after weapons are fired, otherwise we'd
        // have to have a way to update the attacks vector
        if (attacks.size() > 0) {
            return false;
        }
        
        // Can only strafe hexes that were flown over
        if (!a.passedThrough(newCoord)) {
            return false;
        }
        
        // No more limitations if it's the first hex
        if (strafingCoords.size() == 0) {
            return true;
        }
        
        // We can only select at most 5 hexes
        if (strafingCoords.size() >= 5) {
            return false;
        }
        
        // Can't strafe the same hex twice
        if (strafingCoords.contains(newCoord)) {
            return false;
        }
        
        boolean isConsecutive = false;
        for (Coords c : strafingCoords) {
            isConsecutive |= (c.distance(newCoord) == 1);
        }
        
        boolean isInaLine = true;
        // If there is only one other coord, then they're linear
        if (strafingCoords.size() > 1) {
            IdealHex newHex = new IdealHex(newCoord);
            IdealHex start = new IdealHex(strafingCoords.get(0));
            // Define the vector formed by the new coords and the first coords
            for (int i = 1; i < strafingCoords.size(); i++) {
                IdealHex iHex = new IdealHex(strafingCoords.get(i));
                isInaLine &= iHex.isIntersectedBy(start.cx, start.cy, newHex.cx,
                        newHex.cy);
            }
        }
        return isConsecutive && isInaLine;
    }
    
    public void FieldofFire(Entity unit, int[][] ranges, int arc, int loc, int facing) {
        // do nothing here outside the movement phase
        if (!(clientgui.getClient().getGame().getPhase() == Phase.PHASE_FIRING)) return;
        
        clientgui.bv.fieldofFireUnit = unit;
        clientgui.bv.fieldofFireRanges = ranges;
        clientgui.bv.fieldofFireWpArc = arc;
        clientgui.bv.fieldofFireWpLoc = loc;
        
        clientgui.bv.setWeaponFieldofFire(facing, unit.getPosition());
    }
    
}

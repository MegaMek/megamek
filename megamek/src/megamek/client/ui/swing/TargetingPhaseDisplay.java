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

package megamek.client.ui.swing;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import megamek.client.Client;
import megamek.client.event.BoardViewEvent;
import megamek.client.ui.Messages;
import megamek.client.ui.SharedUtility;
import megamek.client.ui.swing.util.CommandAction;
import megamek.client.ui.swing.util.KeyCommandBind;
import megamek.client.ui.swing.util.MegaMekController;
import megamek.client.ui.swing.widget.MegamekButton;
import megamek.client.ui.swing.widget.SkinSpecification;
import megamek.common.AmmoType;
import megamek.common.Building;
import megamek.common.BuildingTarget;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.GameTurn;
import megamek.common.IPlayer;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GameTurnChangeEvent;
import megamek.common.HexTarget;
import megamek.common.IGame;
import megamek.common.Mounted;
import megamek.common.TargetRoll;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.IGame.Phase;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.EntityAction;
import megamek.common.actions.FlipArmsAction;
import megamek.common.actions.SearchlightAttackAction;
import megamek.common.actions.TorsoTwistAction;
import megamek.common.actions.TriggerAPPodAction;
import megamek.common.actions.TriggerBPodAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.options.OptionsConstants;
import megamek.common.util.FiringSolution;
import megamek.common.weapons.ArtilleryWeapon;

/*
 * Targeting Phase Display. Breaks naming convention because TargetingDisplay is too easy to confuse
 * with something else
 */

public class TargetingPhaseDisplay extends StatusBarPhaseDisplay implements
        KeyListener, ItemListener, ListSelectionListener {
    /**
     *
     */
    private static final long serialVersionUID = 3441669419807288865L;

    /**
     * This enumeration lists all of the possible ActionCommands that can be
     * carried out during the deploy minefield phase.  Each command has a string
     * for the command plus a flag that determines what unit type it is
     * appropriate for.
     *
     * @author arlith
     */
    public static enum TargetingCommand implements PhaseCommand {
        FIRE_NEXT("fireNext"),
        FIRE_TWIST("fireTwist"),
        FIRE_FIRE("fireFire"),
        FIRE_SKIP("fireSkip"),
        FIRE_NEXT_TARG("fireNextTarg"),
        FIRE_MODE("fireMode"),
        FIRE_FLIP_ARMS("fireFlipArms"),
        FIRE_SEARCHLIGHT("fireSearchlight"),
        FIRE_CANCEL("fireCancel");

        String cmd;

        /**
         * Priority that determines this buttons order
         */
        public int priority;

        private TargetingCommand(String c) {
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
            return Messages.getString("TargetingPhaseDisplay." + getCmd());
        }
    }

    // buttons
    protected Map<TargetingCommand, MegamekButton> buttons;

    // let's keep track of what we're shooting and at what, too
    private int cen = Entity.NONE; // current entity number

    private Targetable target; // target

    // shots we have so far.
    private Vector<EntityAction> attacks;

    // is the shift key held?
    private boolean shiftheld;

    private boolean twisting;

    private final IGame.Phase phase;

    private Entity[] visibleTargets;

    private int lastTargetID = -1;

    /**
     * Creates and lays out a new targeting phase display for the specified
     * clientgui.getClient().
     */
    public TargetingPhaseDisplay(final ClientGUI clientgui, boolean offboard) {
        super(clientgui);
        phase = offboard ? IGame.Phase.PHASE_OFFBOARD
                : IGame.Phase.PHASE_TARGETING;
        shiftheld = false;

        // fire
        attacks = new Vector<EntityAction>();

        setupStatusBar(Messages
                .getString("TargetingPhaseDisplay.waitingForTargetingPhase")); //$NON-NLS-1$

        buttons = new HashMap<TargetingCommand, MegamekButton>(
                (int) (TargetingCommand.values().length * 1.25 + 0.5));
        for (TargetingCommand cmd : TargetingCommand.values()) {
            String title = Messages.getString("TargetingPhaseDisplay."
                    + cmd.getCmd());
            MegamekButton newButton = new MegamekButton(title,
                    SkinSpecification.UIComponents.PhaseDisplayButton.getComp());
            String ttKey = "TargetingPhaseDisplay." + cmd.getCmd() + ".tooltip";
            if (Messages.keyExists(ttKey)) {
                newButton.setToolTipText(Messages.getString(ttKey));
            }
            newButton.addActionListener(this);
            newButton.setActionCommand(cmd.getCmd());
            newButton.setEnabled(false);
            buttons.put(cmd, newButton);
        }
        numButtonGroups = (int) Math.ceil((buttons.size() + 0.0)
                / buttonsPerGroup);

        butDone.setText(Messages.getString("TargetingPhaseDisplay.Done")); //$NON-NLS-1$
        butDone.setEnabled(false);

        layoutScreen();

        setupButtonPanel();
        
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
                                || !buttons.get(TargetingCommand.FIRE_FIRE)
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

        // Register the action for NEXT_MODE
        controller.registerCommandAction(KeyCommandBind.NEXT_MODE.cmd,
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
                        changeMode(true);
                    }
                });

        // Register the action for PREV_MODE
        controller.registerCommandAction(KeyCommandBind.PREV_MODE.cmd,
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
                        changeMode(false);
                    }
                });

        // Register the action for CLEAR
        controller.registerCommandAction(KeyCommandBind.CANCEL.cmd,
                new CommandAction() {

                    @Override
                    public boolean shouldPerformAction() {
                        if (clientgui.bv.getChatterBoxActive()
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

    /**
     * Have the panel register itself as a listener wherever it's needed.
     * <p/>
     * According to
     * http://www-106.ibm.com/developerworks/java/library/j-jtp0618.html it is a
     * major bad no-no to perform these registrations before the constructor
     * finishes, so this function has to be called after the panel is created.
     * Please note, this restriction only applies to listeners for objects that
     * aren't on the panel itself.
     */
    public void initializeListeners() {

        clientgui.getClient().getGame().addGameListener(this);
        clientgui.getBoardView().addBoardViewListener(this);

        clientgui.bv.addKeyListener(this);

        // mech display.
        clientgui.mechD.wPan.weaponList.addListSelectionListener(this);
        clientgui.mechD.wPan.weaponList.addKeyListener(this);
    }

    protected ArrayList<MegamekButton> getButtonList() {
        ArrayList<MegamekButton> buttonList = new ArrayList<MegamekButton>();
        TargetingCommand commands[] = TargetingCommand.values();
        CommandComparator comparator = new CommandComparator();
        Arrays.sort(commands, comparator);
        for (TargetingCommand cmd : commands) {
            if (cmd == TargetingCommand.FIRE_CANCEL) {
                continue;
            }
            buttonList.add(buttons.get(cmd));
        }
        return buttonList;
    }

    /**
     * Selects an entity, by number, for targeting.
     */
    private void selectEntity(int en) {
        // clear any previously considered attacks
        if (en != cen) {
            clearAttacks();
            refreshAll();
        }
        Client client = clientgui.getClient();
        if ((ce() != null) &&ce().isWeapOrderChanged()) {
            client.sendEntityWeaponOrderUpdate(ce());
        }

        if (client.getGame().getEntity(en) != null) {

            cen = en;
            clientgui.setSelectedEntityNum(en);

            // If the selected entity is not on the board, use the next one.
            // ASSUMPTION: there will always be *at least one* entity on map.
            if (null == ce().getPosition()) {

                // Walk through the list of entities for this player.
                for (int nextId = client.getNextEntityNum(en); nextId != en; 
                        nextId = client.getNextEntityNum(nextId)) {

                    if (null != clientgui.getClient().getGame()
                            .getEntity(nextId).getPosition()) {
                        cen = nextId;
                        break;
                    }

                } // Check the player's next entity.

                // We were *supposed* to have found an on-board entity.
                if (null == ce().getPosition()) {
                    System.err.println("FiringDisplay: could not find " //$NON-NLS-1$
                            + "an on-board entity: " + //$NON-NLS-1$
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

            if (!clientgui.bv.isMovingUnits() && !ce().isOffBoard()) {
                clientgui.bv.centerOnHex(ce().getPosition());
            }

            // Update the menu bar.
            clientgui.getMenuBar().setEntity(ce());

            // 2003-12-29, nemchenk -- only twist if crew conscious
            setTwistEnabled(ce().canChangeSecondaryFacing()
                            && ce().getCrew().isActive());
            setFlipArmsEnabled(ce().canFlipArms());
            updateSearchlight();

            setFireModeEnabled(true);

            if (GUIPreferences.getInstance().getBoolean("FiringSolutions")
                    && !ce().isOffBoard()) {
                setFiringSolutions();
            } else {
                clientgui.getBoardView().clearFiringSolutionData();
            }
        } else {
            System.err.println("TargetingPhaseDisplay: " //$NON-NLS-1$
                    + "tried to select non-existant entity: " + en); //$NON-NLS-1$
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

        // Determine which entities are spotted
        Set<Integer> spottedEntities = new HashSet<>();
        for (Entity target : game.getEntitiesVector()) {
            if (!target.isEnemyOf(ce()) && target.isSpotting()) {
                spottedEntities.add(target.getSpotTargetId());
            }
        }

        // Calculate firing solutions
        Map<Integer, FiringSolution> fs = new HashMap<>();
        for (Entity target : game.getEntitiesVector()) {
            boolean friendlyFire = game.getOptions().booleanOption(
                    OptionsConstants.BASE_FRIENDLY_FIRE); //$NON-NLS-1$
            boolean enemyTarget = target.getOwner().isEnemyOf(ce().getOwner());
            if ((target.getId() != cen)
                && (friendlyFire || enemyTarget)
                && (!enemyTarget || target.hasSeenEntity(localPlayer)
                    || target.hasDetectedEntity(localPlayer))
                && target.isTargetable()) {
                ToHitData thd = WeaponAttackAction.toHit(game, cen, target);
                thd.setLocation(target.getPosition());
                thd.setRange(ce().getPosition().distance(target.getPosition()));
                fs.put(target.getId(), new FiringSolution(thd, spottedEntities.contains(target.getId())));
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
        clientgui.bv.clearFieldofF();

        selectEntity(clientgui.getClient().getFirstEntityNum());

        GameTurn turn = clientgui.getClient().getMyTurn();
        // There's special processing for triggering AP Pods.
        if ((turn instanceof GameTurn.TriggerAPPodTurn) && (null != ce())) {
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
            clientgui.getBoardView().select(null);
        }
        setupButtonPanel();
    }

    /**
     * Does end turn stuff.
     */
    private void endMyTurn() {
        // end my turn, then.
        Entity next = clientgui.getClient().getGame()
                .getNextEntity(clientgui.getClient().getGame().getTurnIndex());
        if ((phase == clientgui.getClient().getGame().getPhase())
                && (null != next) && (null != ce())
                && (next.getOwnerId() != ce().getOwnerId())) {
            clientgui.setDisplayVisible(false);
        }
        cen = Entity.NONE;
        target(null);
        clientgui.getBoardView().select(null);
        clientgui.getBoardView().highlight(null);
        clientgui.getBoardView().cursor(null);
        clientgui.bv.clearFiringSolutionData();
        clientgui.bv.clearMovementData();
        clientgui.bv.clearFieldofF();
        clientgui.setSelectedEntityNum(Entity.NONE);
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
    private void changeMode(boolean forward) {
        int wn = clientgui.mechD.wPan.getSelectedWeaponNum();

        // Do nothing we have no unit selected.
        if (null == ce()) {
            return;
        }

        // If the weapon does not have modes, just exit.
        Mounted m = ce().getEquipment(wn);
        if ((m == null) || !m.getType().hasModes()) {
            return;
        }

        // Dropship Artillery cannot be switched to "Direct" Fire
        final WeaponType wtype = (WeaponType) m.getType();
        if ((ce() instanceof Dropship) && (wtype instanceof ArtilleryWeapon)) {
            return;
        }

        // send change to the server
        int nMode = m.switchMode(forward);
        clientgui.getClient().sendModeChange(cen, wn, nMode);

        // notify the player
        if (m.canInstantSwitch(nMode)) {
            clientgui.systemMessage(Messages.getString(
                    "FiringDisplay.switched", new Object[] { m.getName(),
                            m.curMode().getDisplayableName() }));
            //$NON-NLS-1$
        } else {
            clientgui.systemMessage(Messages.getString(
                    "FiringDisplay.willSwitch", new Object[] { m.getName(),
                            m.pendingMode().getDisplayableName() })); //$NON-NLS-1$
        }

        updateTarget();
        clientgui.mechD.wPan.displayMech(ce());
        clientgui.mechD.wPan.selectWeapon(wn);
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
                    .getString("TargetingPhaseDisplay.DontFireDialog.title"); //$NON-NLS-1$
            String body = Messages
                    .getString("TargetingPhaseDisplay.DontFireDialog.message"); //$NON-NLS-1$
            ConfirmDialog response = clientgui.doYesNoBotherDialog(title, body);
            if (!response.getShowAgain()) {
                GUIPreferences.getInstance().setNagForNoAction(false);
            }
            if (!response.getAnswer()) {
                return;
            }
        }

        // stop further input (hopefully)
        disableButtons();

        // remove temporary attacks from game & board
        removeTempAttacks();

        // send out attacks
        clientgui.getClient().sendAttackData(cen, attacks);

        // clear queue
        attacks.removeAllElements();

        // Clear the menu bar.
        clientgui.getMenuBar().setEntity(null);
        if ((ce() != null) && ce().isWeapOrderChanged()) {
            clientgui.getClient().sendEntityWeaponOrderUpdate(ce());
        }
        endMyTurn();
    }

    private void doSearchlight() {
        // validate
        if ((ce() == null) || (target == null)) {
            throw new IllegalArgumentException(
                    "current searchlight parameters are invalid"); //$NON-NLS-1$
        }

        if (!SearchlightAttackAction.isPossible(clientgui.getClient().getGame(), cen, target, null)) {
            return;
        }

        // create and queue a searchlight action
        SearchlightAttackAction saa = new SearchlightAttackAction(cen, target
                .getTargetType(), target.getTargetId());
        attacks.addElement(saa);

        // and add it into the game, temporarily
        clientgui.getClient().getGame().addAction(saa);
        clientgui.bv.addAttack(saa);
        clientgui.minimap.drawMap();

        // refresh weapon panel, as bth will have changed
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
        if ((ce() == null) || (target == null) || (mounted == null)
                || !(mounted.getType() instanceof WeaponType)) {
            throw new IllegalArgumentException(
                    "current fire parameters are invalid"); //$NON-NLS-1$
        }

        // declare searchlight, if possible
        if (GUIPreferences.getInstance().getAutoDeclareSearchlight()) {
            doSearchlight();
        }

        WeaponAttackAction waa = new WeaponAttackAction(cen,
                target.getTargetType(), target.getTargetId(), weaponNum);
        if (mounted.getType().hasFlag(WeaponType.F_ARTILLERY)) {
            waa = new ArtilleryAttackAction(cen, target.getTargetType(),
                    target.getTargetId(), weaponNum, clientgui.getClient()
                            .getGame());
        }
        if ((null != mounted.getLinked())
                && (((WeaponType) mounted.getType()).getAmmoType() != AmmoType.T_NA)) {
            Mounted ammoMount = mounted.getLinked();
            waa.setAmmoId(ce().getEquipmentNum(ammoMount));
            if (((AmmoType) ammoMount.getType()).getMunitionType() == AmmoType.M_VIBRABOMB_IV) {
                VibrabombSettingDialog vsd = new VibrabombSettingDialog(
                        clientgui.frame);
                vsd.setVisible(true);
                waa.setOtherAttackInfo(vsd.getSetting());
            }
        }

        // add the attack to our temporary queue
        attacks.addElement(waa);

        // and add it into the game, temporarily
        clientgui.getClient().getGame().addAction(waa);
        clientgui.minimap.drawMap();

        // set the weapon as used
        mounted.setUsedThisRound(true);

        // find the next available weapon
        int nextWeapon = clientgui.mechD.wPan.selectNextWeapon();

        // check; if there are no ready weapons, you're done.
        if ((nextWeapon == -1) && GUIPreferences.getInstance().getAutoEndFiring()) {
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
     * Removes all current fire
     */
    private void clearAttacks() {
        // We may not have an entity selected yet (race condition).
        if (ce() == null) {
            return;
        }

        // remove attacks, set weapons available again
        Enumeration<EntityAction> i = attacks.elements();
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
        clientgui.mechD.wPan.selectFirstWeapon();
        updateTarget();
    }

    /**
     * Targets something
     */
    void target(Targetable t) {
        target = t;
        updateTarget();

    }

    /**
     * Targets something
     */
    public void updateTarget() {
        setFireEnabled(false);

        // update target panel
        final int weaponId = clientgui.mechD.wPan.getSelectedWeaponNum();
        if ((target != null) && (weaponId != -1)) {
            ToHitData toHit;

            toHit = WeaponAttackAction.toHit(clientgui.getClient().getGame(),
                    cen, target, weaponId, Entity.LOC_NONE, 0, false);
            clientgui.mechD.wPan.wTargetR.setText(target.getDisplayName());

            clientgui.mechD.wPan.wRangeR
                    .setText("" + ce().getPosition().distance(target.getPosition())); //$NON-NLS-1$
            Mounted m = ce().getEquipment(weaponId);
            if (m.isUsedThisRound()) {
                clientgui.mechD.wPan.wToHitR.setText(Messages
                        .getString("TargetingPhaseDisplay.alreadyFired"));
                //$NON-NLS-1$
                setFireEnabled(false);
            } else if (m.getType().hasFlag(WeaponType.F_AUTO_TARGET)) {
                clientgui.mechD.wPan.wToHitR.setText(Messages
                        .getString("TargetingPhaseDisplay.autoFiringWeapon"));
                //$NON-NLS-1$
                setFireEnabled(false);
            } else if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
                clientgui.mechD.wPan.wToHitR.setText(toHit.getValueAsString());
                setFireEnabled(false);
            } else if (toHit.getValue() == TargetRoll.AUTOMATIC_FAIL) {
                clientgui.mechD.wPan.wToHitR.setText(toHit.getValueAsString());
                setFireEnabled(true);
            } else {
                clientgui.mechD.wPan.wToHitR
                        .setText(toHit.getValueAsString()
                                + " ("
                                + Compute.oddsAbove(
                                        toHit.getValue(),
                                        ce().getCrew()
                                                .getOptions()
                                                .booleanOption(
                                                        OptionsConstants.PILOT_APTITUDE_GUNNERY))
                                + "%)"); //$NON-NLS-1$ //$NON-NLS-2$
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
    void torsoTwist(Coords cTarget) {
        int direction = ce().getFacing();

        if (null != cTarget) {
            direction = ce().clipSecondaryFacing(
                    ce().getPosition().direction(cTarget));
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
     * @param twistDirection An <code>int</code> specifying wether we're twisting left or
     *                       right, 0 if we're twisting to the left, 1 if to the right.
     */

    void torsoTwist(int twistDirection) {
        int direction = ce().getSecondaryFacing();
        if (twistDirection == 0) {
            clearAttacks();
            direction = ce().clipSecondaryFacing((direction + 5) % 6);
            attacks.addElement(new TorsoTwistAction(cen, direction));
            ce().setSecondaryFacing(direction);
            refreshAll();
        } else if (twistDirection == 1) {
            clearAttacks();
            direction = ce().clipSecondaryFacing((direction + 7) % 6);
            attacks.addElement(new TorsoTwistAction(cen, direction));
            ce().setSecondaryFacing(direction);
            refreshAll();
        }
    }

    /**
     * Cache the list of visible targets. This is used for the 'next target'
     * button.
     * <p/>
     * We'll sort it by range to us.
     */
    private void cacheVisibleTargets() {
        clearVisibleTargets();

        List<Entity> vec = clientgui.getClient().getGame().getValidTargets(ce());
        Comparator<Entity> sortComp = new Comparator<Entity>() {
            public int compare(Entity x, Entity y) {

                int rangeToX = ce().getPosition().distance(x.getPosition());
                int rangeToY = ce().getPosition().distance(y.getPosition());

                if (rangeToX == rangeToY) {
                    return x.getId() < y.getId() ? -1 : 1;
                }

                return rangeToX < rangeToY ? -1 : 1;
            }
        };

        TreeSet<Entity> tree = new TreeSet<Entity>(sortComp);
        visibleTargets = new Entity[vec.size()];

        for (int i = 0; i < vec.size(); i++) {
            tree.add(vec.get(i));
        }

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
        if (null == visibleTargets) {
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

        if (null == targ) {
            return;
        }

        clientgui.bv.centerOnHex(targ.getPosition());
        clientgui.getBoardView().select(targ.getPosition());

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

        clientgui.bv.centerOnHex(targ.getPosition());
        clientgui.getBoardView().select(targ.getPosition());

        target(targ);
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
            clientgui.getBoardView().select(b.getCoords());
        }
    }

    @Override
    public void hexSelected(BoardViewEvent b) {

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }
        final Client client = clientgui.getClient();

        if (client.isMyTurn() && (b.getCoords() != null)
                && (ce() != null) && !b.getCoords().equals(ce().getPosition())) {
            if (shiftheld) {
                updateFlipArms(false);
                torsoTwist(b.getCoords());
            } else if (phase == IGame.Phase.PHASE_TARGETING) {
                target(new HexTarget(b.getCoords(), ce().getGame().getBoard(),
                        Targetable.TYPE_HEX_ARTILLERY));
            } else {
                target(chooseTarget(b.getCoords()));
            }
        }
    }

    /**
     * Have the player select a target from the entities at the given coords.
     *
     * @param pos - the <code>Coords</code> containing targets.
     */
    private Targetable chooseTarget(Coords pos) {

        boolean friendlyFire = clientgui.getClient().getGame().getOptions()
                .booleanOption(OptionsConstants.BASE_FRIENDLY_FIRE); //$NON-NLS-1$
        // Assume that we have *no* choice.
        Targetable choice = null;
        Iterator<Entity> choices;

        // Get the available choices, depending on friendly fire
        if (friendlyFire) {
            choices = clientgui.getClient().getGame().getEntities(pos);
        } else {
            choices = clientgui.getClient().getGame()
                    .getEnemyEntities(pos, ce());
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

        // Is there a building in the hex?
        Building bldg = clientgui.getClient().getGame().getBoard()
                .getBuildingAt(pos);
        if (bldg != null) {
            targets.add(new BuildingTarget(pos, clientgui.getClient().getGame()
                    .getBoard(), Targetable.TYPE_BLDG_TAG));
        }

        targets.add(new HexTarget(pos, clientgui.getClient().getGame()
                .getBoard(), Targetable.TYPE_HEX_TAG));

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
                                    "FiringDisplay.ChooseTargetDialog.message", //$NON-NLS-1$ 
                                    new Object[] { pos.getBoardNum() }),
                            Messages.getString("FiringDisplay.ChooseTargetDialog.title"), //$NON-NLS-1$
                            JOptionPane.QUESTION_MESSAGE, null, SharedUtility
                                    .getDisplayArray(targets), null);
            choice = SharedUtility.getTargetPicked(targets, input);
        } // End have-choices

        // Return the chosen unit.
        return choice;

    } // End private Targetable chooseTarget( Coords )

    //
    // GameListener
    //
    @Override
    public void gameTurnChange(GameTurnChangeEvent e) {

        // In case of a /reset command, ensure the state gets reset
        if (clientgui.getClient().getGame().getPhase() == IGame.Phase.PHASE_LOUNGE) {
            endMyTurn();
        }

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (clientgui.getClient().getGame().getPhase() == phase) {

            if (clientgui.getClient().isMyTurn()) {
                if (cen == Entity.NONE) {
                    beginMyTurn();
                }
                setStatusBarText(Messages
                        .getString("TargetingPhaseDisplay.its_your_turn")); //$NON-NLS-1$
            } else {
                endMyTurn();
                if (e.getPlayer() != null) {
                    setStatusBarText(Messages.getString(
                            "TargetingPhaseDisplay.its_others_turn", //$NON-NLS-1$
                            new Object[] { e.getPlayer().getName() }));
                }

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
                && (clientgui.getClient().getGame().getPhase() != phase)) {
            endMyTurn();
        }
        // if we're ending the firing phase, unregister stuff.
        if (clientgui.getClient().getGame().getPhase() == phase) {
            setStatusBarText(Messages
                    .getString("TargetingPhaseDisplay.waitingForFiringPhase")); //$NON-NLS-1$
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

        if (ev.getActionCommand().equals(TargetingCommand.FIRE_FIRE.getCmd())) {
            fire();
        } else if (ev.getActionCommand().equals(TargetingCommand.FIRE_SKIP.getCmd())) {
            nextWeapon();
        } else if (ev.getActionCommand().equals(TargetingCommand.FIRE_TWIST.getCmd())) {
            twisting = true;
        } else if (ev.getActionCommand().equals(TargetingCommand.FIRE_NEXT.getCmd())) {
            selectEntity(clientgui.getClient().getNextEntityNum(cen));
        } else if (ev.getActionCommand().equals(TargetingCommand.FIRE_NEXT_TARG.getCmd())) {
            jumpToNextTarget();
        } else if (ev.getActionCommand().equals(TargetingCommand.FIRE_FLIP_ARMS.getCmd())) {
            updateFlipArms(!ce().getArmsFlipped());
        } else if (ev.getActionCommand().equals(TargetingCommand.FIRE_MODE.getCmd())) {
            changeMode(true);
        } else if (ev.getActionCommand().equals(TargetingCommand.FIRE_CANCEL.getCmd())) {
            clear();
        } else if (ev.getActionCommand().equals(TargetingCommand.FIRE_SEARCHLIGHT.getCmd())) {
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
        setSearchlightEnabled((ce() != null)
                && (target != null)
                && ce().isUsingSpotlight()
                && ce().getCrew().isActive()
                && SearchlightAttackAction.isPossible(clientgui.getClient()
                        .getGame(), cen, target, null));
    }

    private void setFireEnabled(boolean enabled) {
        buttons.get(TargetingCommand.FIRE_FIRE).setEnabled(enabled);
        clientgui.getMenuBar().setFireFireEnabled(enabled);
    }

    private void setTwistEnabled(boolean enabled) {
        buttons.get(TargetingCommand.FIRE_TWIST).setEnabled(enabled);
        clientgui.getMenuBar().setFireTwistEnabled(enabled);
    }

    private void setSkipEnabled(boolean enabled) {
        buttons.get(TargetingCommand.FIRE_SKIP).setEnabled(enabled);
        clientgui.getMenuBar().setFireSkipEnabled(enabled);
    }

    private void setFlipArmsEnabled(boolean enabled) {
        buttons.get(TargetingCommand.FIRE_FLIP_ARMS).setEnabled(enabled);
        clientgui.getMenuBar().setFireFlipArmsEnabled(enabled);
    }

    private void setNextEnabled(boolean enabled) {
        buttons.get(TargetingCommand.FIRE_NEXT).setEnabled(enabled);
        clientgui.getMenuBar().setFireNextEnabled(enabled);
    }

    private void setSearchlightEnabled(boolean enabled) {
        buttons.get(TargetingCommand.FIRE_SEARCHLIGHT).setEnabled(enabled);
        clientgui.getMenuBar().setFireSearchlightEnabled(enabled);
    }

    private void setFireModeEnabled(boolean enabled) {
        buttons.get(TargetingCommand.FIRE_MODE).setEnabled(enabled);
        clientgui.getMenuBar().setFireModeEnabled(enabled);
    }

    private void setNextTargetEnabled(boolean enabled) {
        buttons.get(TargetingCommand.FIRE_NEXT_TARG).setEnabled(enabled);
        clientgui.getMenuBar().setFireNextTargetEnabled(enabled);
    }

    @Override
    public void clear() {
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

    /**
     * Stop just ignoring events and actually stop listening to them.
     */
    public void removeAllListeners() {
        clientgui.getClient().getGame().removeGameListener(this);
        clientgui.getBoardView().removeBoardViewListener(this);
        clientgui.mechD.wPan.weaponList.removeListSelectionListener(this);
    }

    public void valueChanged(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }
        if (event.getSource().equals(clientgui.mechD.wPan.weaponList)) {
            // update target data in weapon display
            updateTarget();
        }
    }
    
    public void FieldofFire(Entity unit, int[][] ranges, int arc, int loc, int facing) {
        // do nothing here outside the arty targeting phase
        if (!(clientgui.getClient().getGame().getPhase() == Phase.PHASE_TARGETING)) return;
        
        clientgui.bv.fieldofFireUnit = unit;
        clientgui.bv.fieldofFireRanges = ranges;
        clientgui.bv.fieldofFireWpArc = arc;
        clientgui.bv.fieldofFireWpLoc = loc;
        
        clientgui.bv.setWeaponFieldofFire(facing, unit.getPosition());
    }
    
    
}

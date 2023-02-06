/*
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.client.ui.swing;

import megamek.client.Client;
import megamek.client.event.BoardViewEvent;
import megamek.client.ui.Messages;
import megamek.client.ui.SharedUtility;
import megamek.client.ui.swing.FiringDisplay.FiringCommand;
import megamek.client.ui.swing.unitDisplay.WeaponPanel;
import megamek.client.ui.swing.util.CommandAction;
import megamek.client.ui.swing.util.KeyCommandBind;
import megamek.client.ui.swing.util.MegaMekController;
import megamek.client.ui.swing.widget.MegamekButton;
import megamek.common.*;
import megamek.common.actions.*;
import megamek.common.enums.AimingMode;
import megamek.common.enums.GamePhase;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GameTurnChangeEvent;
import megamek.common.options.OptionsConstants;
import megamek.common.util.FiringSolution;
import megamek.common.weapons.Weapon;
import megamek.common.weapons.artillery.ArtilleryWeapon;
import megamek.common.weapons.bayweapons.TeleOperatedMissileBayWeapon;
import megamek.common.weapons.capitalweapons.CapitalMissileWeapon;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.*;
import java.util.*;

import static megamek.client.ui.swing.util.UIUtil.guiScaledFontHTML;
import static megamek.client.ui.swing.util.UIUtil.uiLightViolet;

/**
 * Targeting Phase Display. Breaks naming convention because TargetingDisplay is too easy to confuse
 * with something else
 */
public class TargetingPhaseDisplay extends StatusBarPhaseDisplay implements
        KeyListener, ItemListener, ListSelectionListener {
    private static final long serialVersionUID = 3441669419807288865L;

    /**
     * This enumeration lists all of the possible ActionCommands that can be
     * carried out during the deploy minefield phase. Each command has a string
     * for the command plus a flag that determines what unit type it is
     * appropriate for.
     *
     * @author arlith
     */
    public enum TargetingCommand implements PhaseCommand {
        FIRE_NEXT("fireNext"),
        FIRE_FIRE("fireFire"),
        FIRE_SKIP("fireSkip"),
        FIRE_NEXT_TARG("fireNextTarg"),
        FIRE_MODE("fireMode"),
        FIRE_FLIP_ARMS("fireFlipArms"),
        FIRE_SEARCHLIGHT("fireSearchlight"),
        FIRE_CANCEL("fireCancel"),
        FIRE_DISENGAGE("fireDisengage");

        String cmd;

        /**
         * Priority that determines this buttons order
         */
        public int priority;

        TargetingCommand(String c) {
            cmd = c;
        }

        @Override
        public String getCmd() {
            return cmd;
        }

        @Override
        public int getPriority() {
            return priority;
        }

        @Override
        public void setPriority(int p) {
            priority = p;
        }

        @Override
        public String toString() {
            return Messages.getString("TargetingPhaseDisplay." + getCmd());
        }

        public String getHotKeyDesc() {
            String result = "";

            String msg_next= Messages.getString("Next");
            String msg_previous = Messages.getString("Previous");
            String msg_valid = Messages.getString("TargetingPhaseDisplay.FireNextTarget.tooltip.Valid");
            String msg_noallies = Messages.getString("TargetingPhaseDisplay.FireNextTarget.tooltip.NoAllies");

            switch (this) {
                case FIRE_NEXT:
                    result = "<BR>";
                    result += "&nbsp;&nbsp;" + msg_next + ": " + KeyCommandBind.getDesc(KeyCommandBind.NEXT_UNIT);
                    result += "&nbsp;&nbsp;" + msg_previous + ": " + KeyCommandBind.getDesc(KeyCommandBind.PREV_UNIT);
                    break;
                case FIRE_FIRE:
                    result = "<BR>";
                    result += "&nbsp;&nbsp;" + KeyCommandBind.getDesc(KeyCommandBind.FIRE);
                    break;
                case FIRE_NEXT_TARG:
                    result = "<BR>";
                    result += "&nbsp;&nbsp;" + msg_next + ": " + KeyCommandBind.getDesc(KeyCommandBind.NEXT_TARGET);
                    result += "&nbsp;&nbsp;" + msg_previous + ": " + KeyCommandBind.getDesc(KeyCommandBind.PREV_TARGET);
                    result += "<BR>";
                    result += "&nbsp;&nbsp;" + msg_valid + " " + msg_next + ": " + KeyCommandBind.getDesc(KeyCommandBind.NEXT_TARGET_VALID);
                    result += "&nbsp;&nbsp;" + msg_previous + ": " + KeyCommandBind.getDesc(KeyCommandBind.PREV_TARGET_VALID);
                    result += "<BR>";
                    result += "&nbsp;&nbsp;" + msg_noallies + " " + msg_next + ": " + KeyCommandBind.getDesc(KeyCommandBind.NEXT_TARGET_NOALLIES);
                    result += "&nbsp;&nbsp;" + msg_previous + ": " + KeyCommandBind.getDesc(KeyCommandBind.PREV_TARGET_NOALLIES);
                    result += "<BR>";
                    result += "&nbsp;&nbsp;" + msg_valid + " (" + msg_noallies + ") " + msg_next + ": " + KeyCommandBind.getDesc(KeyCommandBind.NEXT_TARGET_VALID_NO_ALLIES);
                    result += "&nbsp;&nbsp;" + msg_previous + ": " + KeyCommandBind.getDesc(KeyCommandBind.PREV_TARGET_VALID_NO_ALLIES);
                    break;
                case FIRE_SKIP:
                    result = "<BR>";
                    result +=  "&nbsp;&nbsp;" + msg_next + ": " + KeyCommandBind.getDesc(KeyCommandBind.NEXT_WEAPON);
                    result += "&nbsp;&nbsp;" + msg_previous + ": " + KeyCommandBind.getDesc(KeyCommandBind.PREV_WEAPON);
                    break;
                case FIRE_MODE:
                    result = "<BR>";
                    result += "&nbsp;&nbsp;" + msg_next + ": " + KeyCommandBind.getDesc(KeyCommandBind.NEXT_MODE);
                    result += "&nbsp;&nbsp;" + msg_previous + ": " + KeyCommandBind.getDesc(KeyCommandBind.PREV_MODE);
                    break;
                case FIRE_CANCEL:
                    result = "<BR>";
                    result += "&nbsp;&nbsp;" + KeyCommandBind.getDesc(KeyCommandBind.CANCEL);
                    break;
                default:
                    break;
            }

            return result;
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

    private final GamePhase phase;

    private Entity[] visibleTargets;

    private int lastTargetID = -1;

    /**
     * Creates and lays out a new targeting phase display for the specified
     * clientgui.getClient().
     */
    public TargetingPhaseDisplay(final ClientGUI clientgui, boolean offboard) {
        super(clientgui);
        phase = offboard ? GamePhase.OFFBOARD : GamePhase.TARGETING;
        shiftheld = false;

        // fire
        attacks = new Vector<>();

        setupStatusBar(Messages.getString("TargetingPhaseDisplay.waitingForTargetingPhase"));

        setButtons();
        setButtonsTooltips();

        butDone.setText(Messages.getString("TargetingPhaseDisplay.Done"));
        String f = guiScaledFontHTML(uiLightViolet()) +  KeyCommandBind.getDesc(KeyCommandBind.DONE)+ "</FONT>";
        butDone.setToolTipText("<html><body>" + f + "</body></html>");
        butDone.setEnabled(false);

        setupButtonPanel();

        registerKeyCommands();
    }

    @Override
    protected void setButtons() {
        buttons = new HashMap<>(
                (int) (TargetingCommand.values().length * 1.25 + 0.5));
        for (TargetingCommand cmd : TargetingCommand.values()) {
            buttons.put(cmd, createButton(cmd.getCmd(), "TargetingPhaseDisplay." ));
        }
        numButtonGroups = (int) Math.ceil((buttons.size() + 0.0) / buttonsPerGroup);
    }

    @Override
    protected void setButtonsTooltips() {
        for (TargetingCommand cmd : TargetingCommand.values()) {
            String tt = createToolTip(cmd.getCmd(), "TargetingPhaseDisplay.", cmd.getHotKeyDesc());
            buttons.get(cmd).setToolTipText(tt);
        }
    }

    /**
     * Register all of the <code>CommandAction</code>s for this panel display.
     */
    protected void registerKeyCommands() {
        MegaMekController controller = clientgui.controller;
        final StatusBarPhaseDisplay display = this;
        // Register the action for UNDO
        controller.registerCommandAction(KeyCommandBind.UNDO_LAST_STEP.cmd,
                new CommandAction() {

                    @Override
                    public boolean shouldPerformAction() {
                        if (!clientgui.getClient().isMyTurn()
                                || clientgui.getBoardView().getChatterBoxActive()
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

        // Register the action for FIRE
        controller.registerCommandAction(KeyCommandBind.FIRE.cmd,
                new CommandAction() {

                    @Override
                    public boolean shouldPerformAction() {
                        if (!clientgui.getClient().isMyTurn()
                                || clientgui.getBoardView().getChatterBoxActive()
                                || !display.isVisible()
                                || display.isIgnoringEvents()
                                || !buttons.get(TargetingCommand.FIRE_FIRE).isEnabled()) {
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
                                || clientgui.getBoardView().getChatterBoxActive()
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
                                || clientgui.getBoardView().getChatterBoxActive()
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
                                || clientgui.getBoardView().getChatterBoxActive()
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
                                || clientgui.getBoardView().getChatterBoxActive()
                                || !display.isVisible()
                                || display.isIgnoringEvents()) {
                            return false;
                        } else {
                            return true;
                        }
                    }

                    @Override
                    public void performAction() {
                        selectEntity(clientgui.getClient().getPrevEntityNum(cen));
                    }
                });

        // Register the action for NEXT_TARGET
        controller.registerCommandAction(KeyCommandBind.NEXT_TARGET.cmd,
                new CommandAction() {

                    @Override
                    public boolean shouldPerformAction() {
                        if (!clientgui.getClient().isMyTurn()
                                || clientgui.getBoardView().getChatterBoxActive()
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
                                || clientgui.getBoardView().getChatterBoxActive()
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
                                || clientgui.getBoardView().getChatterBoxActive()
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
                                || clientgui.getBoardView().getChatterBoxActive()
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
                        if (clientgui.getBoardView().getChatterBoxActive()
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
     * <p>
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

        clientgui.getBoardView().addKeyListener(this);

        // mech display.
        clientgui.getUnitDisplay().wPan.weaponList.addListSelectionListener(this);
        clientgui.getUnitDisplay().wPan.weaponList.addKeyListener(this);
    }

    @Override
    protected ArrayList<MegamekButton> getButtonList() {
        ArrayList<MegamekButton> buttonList = new ArrayList<>();
        TargetingCommand[] commands = TargetingCommand.values();
        CommandComparator comparator = new CommandComparator();
        Arrays.sort(commands, comparator);
        for (TargetingCommand cmd : commands) {
            if (cmd == TargetingCommand.FIRE_CANCEL) {
                continue;
            }
            if ((cmd == TargetingCommand.FIRE_DISENGAGE) && ((ce() == null) || !ce().isOffBoard())) {
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
                    LogManager.getLogger().error("Could not find an on-board entity: " + en);
                    return;
                }
            }

            target(null);
            clientgui.getBoardView().highlight(ce().getPosition());
            clientgui.getBoardView().select(null);
            clientgui.getBoardView().cursor(null);

            refreshAll();
            cacheVisibleTargets();

            if (!clientgui.getBoardView().isMovingUnits() && !ce().isOffBoard()) {
                clientgui.getBoardView().centerOnHex(ce().getPosition());
            }

            setFlipArmsEnabled(ce().canFlipArms() && ce().getCrew().isActive());
            updateSearchlight();

            setFireModeEnabled(true);

            if (GUIPreferences.getInstance().getBoolean("FiringSolutions")
                    && !ce().isOffBoard()) {
                setFiringSolutions();
            } else {
                clientgui.getBoardView().clearFiringSolutionData();
            }
        } else {
            LogManager.getLogger().error("Tried to select non-existent entity: " + en);
        }
    }

    public void setFiringSolutions() {
        // If no Entity is selected, exit
        if (cen == Entity.NONE) {
            return;
        }

        Game game = clientgui.getClient().getGame();
        Player localPlayer = clientgui.getClient().getLocalPlayer();
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
            boolean friendlyFire = game.getOptions().booleanOption(OptionsConstants.BASE_FRIENDLY_FIRE);
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

        if (!clientgui.getBoardView().isMovingUnits()) {
            clientgui.maybeShowUnitDisplay();
        }
        clientgui.getBoardView().clearFieldofF();

        selectEntity(clientgui.getClient().getFirstEntityNum());
        setDisengageEnabled((ce() != null) && attacks.isEmpty() && ce().canFlee());

        GameTurn turn = clientgui.getClient().getMyTurn();
        // There's special processing for triggering AP Pods.
        if ((turn instanceof GameTurn.TriggerAPPodTurn) && (null != ce())) {
            disableButtons();
            TriggerAPPodDialog dialog = new TriggerAPPodDialog(clientgui.getFrame(), ce());
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
            clientgui.maybeShowUnitDisplay();
        }
        cen = Entity.NONE;
        target(null);
        clientgui.getBoardView().select(null);
        clientgui.getBoardView().highlight(null);
        clientgui.getBoardView().cursor(null);
        clientgui.getBoardView().clearFiringSolutionData();
        clientgui.getBoardView().clearMovementData();
        clientgui.getBoardView().clearFieldofF();
        clientgui.setSelectedEntityNum(Entity.NONE);
        disableButtons();
    }

    /**
     * Disables all buttons in the interface
     */
    private void disableButtons() {
        setFireEnabled(false);
        setSkipEnabled(false);
        setNextEnabled(false);
        butDone.setEnabled(false);
        setFlipArmsEnabled(false);
        setFireModeEnabled(false);
        setNextTargetEnabled(false);
        setDisengageEnabled(false);
    }

    /**
     * Fire Mode - Adds a Fire Mode Change to the current Attack Action
     */
    private void changeMode(boolean forward) {
        int wn = clientgui.getUnitDisplay().wPan.getSelectedWeaponNum();

        // Do nothing we have no unit selected.
        if (null == ce()) {
            return;
        }

        // If the weapon does not have modes, just exit.
        Mounted m = ce().getEquipment(wn);
        if ((m == null) || !m.getType().hasModes()) {
            return;
        }

        // DropShip Artillery cannot be switched to "Direct" Fire
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
        } else {
            clientgui.systemMessage(Messages.getString(
                    "FiringDisplay.willSwitch", new Object[] { m.getName(),
                            m.pendingMode().getDisplayableName() }));
        }

        updateTarget();
        clientgui.getUnitDisplay().wPan.displayMech(ce());
        clientgui.getUnitDisplay().wPan.selectWeapon(wn);
    }

    /**
     * Called when the current entity is done firing. Send out our attack queue
     * to the server.
     */
    @Override
    public void ready() {
        if (attacks.isEmpty() && GUIPreferences.getInstance().getNagForNoAction()) {
            // confirm this action
            String title = Messages.getString("TargetingPhaseDisplay.DontFireDialog.title");
            String body = Messages.getString("TargetingPhaseDisplay.DontFireDialog.message");
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

        if ((ce() != null) && ce().isWeapOrderChanged()) {
            clientgui.getClient().sendEntityWeaponOrderUpdate(ce());
        }
        endMyTurn();
    }

    private void doSearchlight() {
        // validate
        if ((ce() == null) || (target == null)) {
            throw new IllegalArgumentException("current searchlight parameters are invalid");
        }

        if (!SearchlightAttackAction.isPossible(clientgui.getClient().getGame(), cen, target, null)) {
            return;
        }

        // create and queue a searchlight action
        SearchlightAttackAction saa = new SearchlightAttackAction(cen, target.getTargetType(), target.getId());
        attacks.addElement(saa);

        // and add it into the game, temporarily
        clientgui.getClient().getGame().addAction(saa);
        clientgui.getBoardView().addAttack(saa);

        // refresh weapon panel, as bth will have changed
        updateTarget();
    }

    /**
     * Adds a weapon attack with the currently selected weapon to the attack
     * queue.
     */
    private void fire() {
        // get the selected weaponnum
        int weaponNum = clientgui.getUnitDisplay().wPan.getSelectedWeaponNum();
        Mounted mounted = ce().getEquipment(weaponNum);

        // validate
        if ((ce() == null) || (target == null) || (mounted == null)
                || !(mounted.getType() instanceof WeaponType)) {
            throw new IllegalArgumentException("current fire parameters are invalid");
        }

        // declare searchlight, if possible
        if (GUIPreferences.getInstance().getAutoDeclareSearchlight()) {
            doSearchlight();
        }

        WeaponAttackAction waa = new WeaponAttackAction(cen, target.getTargetType(),
                target.getId(), weaponNum);
        Game game = clientgui.getClient().getGame();
        int distance = Compute.effectiveDistance(game, waa.getEntity(game), waa.getTarget(game));
        if ((mounted.getType().hasFlag(WeaponType.F_ARTILLERY))
                || (mounted.isInBearingsOnlyMode()
                        && distance >= RangeType.RANGE_BEARINGS_ONLY_MINIMUM)
                || (mounted.getType() instanceof CapitalMissileWeapon
                        && Compute.isGroundToGround(ce(), target))) {
            waa = new ArtilleryAttackAction(cen, target.getTargetType(),
                    target.getId(), weaponNum, clientgui.getClient().getGame());
            // Get the launch velocity for bearings-only telemissiles
            if (mounted.getType() instanceof TeleOperatedMissileBayWeapon) {                
                TeleMissileSettingDialog tsd = new TeleMissileSettingDialog(clientgui.frame, clientgui.getClient().getGame());
                tsd.setVisible(true);
                waa.setLaunchVelocity(tsd.getSetting());
                waa.updateTurnsTilHit(clientgui.getClient().getGame());
            } 
        }
        
        updateDisplayForPendingAttack(mounted, waa);
    }
    
    /**
     * Worker function that handles setting associated ammo and other bookkeeping/UI updates
     * for a pending weapon attack action.
     */
    public void updateDisplayForPendingAttack(Mounted mounted, WeaponAttackAction waa) {
        // put this and the rest of the method into a separate function for access externally.
        if ((null != mounted.getLinked())
                && (((WeaponType) mounted.getType()).getAmmoType() != AmmoType.T_NA)) {
            Mounted ammoMount = mounted.getLinked();
            waa.setAmmoId(ammoMount.getEntity().getEquipmentNum(ammoMount));
            long ammoMunitionType = ((AmmoType) ammoMount.getType()).getMunitionType();
            waa.setAmmoMunitionType(ammoMunitionType);
            waa.setAmmoCarrier(ammoMount.getEntity().getId());
            if (ammoMunitionType == AmmoType.M_VIBRABOMB_IV) {
                VibrabombSettingDialog vsd = new VibrabombSettingDialog(clientgui.frame);
                vsd.setVisible(true);
                waa.setOtherAttackInfo(vsd.getSetting());
            }
        }

        // add the attack to our temporary queue
        attacks.addElement(waa);

        // and add it into the game, temporarily
        clientgui.getClient().getGame().addAction(waa);

        // set the weapon as used
        mounted.setUsedThisRound(true);

        // find the next available weapon
        int nextWeapon = clientgui.getUnitDisplay().wPan.selectNextWeapon();

        // check; if there are no ready weapons, you're done.
        if ((nextWeapon == -1) && GUIPreferences.getInstance().getAutoEndFiring()) {
            ready();
            return;
        }

        // otherwise, display firing info for the next weapon
        clientgui.getUnitDisplay().wPan.displayMech(ce());
        clientgui.getUnitDisplay().wPan.selectWeapon(nextWeapon);
        updateTarget();
        setDisengageEnabled(false);
    }

    /**
     * Skips to the next weapon
     */
    private void nextWeapon() {
        if (ce() == null) {
            return;
        }
        int weaponId = clientgui.getUnitDisplay().wPan.selectNextWeapon();
        if (ce().getId() != clientgui.getUnitDisplay().wPan.getSelectedEntityId()) {
            clientgui.getUnitDisplay().wPan.displayMech(ce());
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
        int weaponId = clientgui.getUnitDisplay().wPan.selectPrevWeapon();
        if (ce().getId() != clientgui.getUnitDisplay().wPan.getSelectedEntityId()) {
            clientgui.getUnitDisplay().wPan.displayMech(ce());
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
        setDisengageEnabled(ce().isOffBoard() && ce().canFlee());
    }

    /**
     * Removes temp attacks from the game and board
     */
    private void removeTempAttacks() {
        // remove temporary attacks from game & board
        clientgui.getClient().getGame().removeActionsFor(cen);
        clientgui.getBoardView().removeAttacksFor(ce());
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
                setDisengageEnabled(attacks.isEmpty() && ce().isOffBoard() && ce().canFlee());
                clientgui.getUnitDisplay().wPan.displayMech(ce());
                clientgui.getClient().getGame().removeAction(o);
                clientgui.getBoardView().refreshAttacks();
            }
        }
    }    

    /**
     * Refreshes all displays.
     */
    private void refreshAll() {
        if (ce() == null) {
            return;
        }
        clientgui.getBoardView().redrawEntity(ce());
        clientgui.getUnitDisplay().displayEntity(ce());
        clientgui.getUnitDisplay().showPanel("weapons");
        clientgui.getUnitDisplay().wPan.selectFirstWeapon();
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
        final int weaponId = clientgui.getUnitDisplay().wPan.getSelectedWeaponNum();
        if ((target != null) && (weaponId != -1)) {
            ToHitData toHit;
            Mounted m = ce().getEquipment(weaponId);

            int targetDistance = ce().getPosition().distance(target.getPosition()); 
            boolean isArtilleryAttack = m.getType().hasFlag(WeaponType.F_ARTILLERY)
                    // For other weapons that can make artillery attacks
                    || target.getTargetType() == Targetable.TYPE_HEX_ARTILLERY;            

            toHit = WeaponAttackAction.toHit(clientgui.getClient().getGame(),
                    cen, target, weaponId, Entity.LOC_NONE, AimingMode.NONE, false);
            
            String flightTimeText = ""; 
            if (isArtilleryAttack) {
                ArtilleryAttackAction aaa = new ArtilleryAttackAction(ce().getId(), target.getTargetType(),
                        target.getId(), weaponId, clientgui.getClient().getGame());
                flightTimeText = String.format("(%d turns)", aaa.getTurnsTilHit());
            }

            String t =  String.format("<html><div WIDTH=%d>%s</div></html>", WeaponPanel.TARGET_DISPLAY_WIDTH, target.getDisplayName());
            clientgui.getUnitDisplay().wPan.wTargetR.setText(t);
            clientgui.getUnitDisplay().wPan.wRangeR.setText(String.format("%d %s", targetDistance, flightTimeText));
            
            Game game = clientgui.getClient().getGame();
            int distance = Compute.effectiveDistance(game, ce(), target);
            if (m.isUsedThisRound()) {
                clientgui.getUnitDisplay().wPan.wToHitR.setText(
                        Messages.getString("TargetingPhaseDisplay.alreadyFired"));
                setFireEnabled(false);
            } else if (m.isInBearingsOnlyMode() && distance < RangeType.RANGE_BEARINGS_ONLY_MINIMUM) {
                clientgui.getUnitDisplay().wPan.wToHitR.setText(
                        Messages.getString("TargetingPhaseDisplay.bearingsOnlyMinRange"));
                setFireEnabled(false);
            } else if ((m.getType().hasFlag(WeaponType.F_AUTO_TARGET)
                    && !m.curMode().equals(Weapon.MODE_AMS_MANUAL))) {
                clientgui.getUnitDisplay().wPan.wToHitR.setText(
                        Messages.getString("TargetingPhaseDisplay.autoFiringWeapon"));
                setFireEnabled(false);
            } else if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
                clientgui.getUnitDisplay().wPan.wToHitR.setText(toHit.getValueAsString());
                setFireEnabled(false);
            } else if (toHit.getValue() == TargetRoll.AUTOMATIC_FAIL) {
                clientgui.getUnitDisplay().wPan.wToHitR.setText(toHit.getValueAsString());
                setFireEnabled(true);
            } else {
                clientgui.getUnitDisplay().wPan.wToHitR.setText(toHit.getValueAsString() + " ("
                        + Compute.oddsAbove(toHit.getValue(), ce().hasAbility(OptionsConstants.PILOT_APTITUDE_GUNNERY))
                        + "%)");
                setFireEnabled(true);
            }
            clientgui.getUnitDisplay().wPan.toHitText.setText(toHit.getDesc());
            setSkipEnabled(true);
        } else {
            clientgui.getUnitDisplay().wPan.wTargetR.setText("---");
            clientgui.getUnitDisplay().wPan.wRangeR.setText("---");
            clientgui.getUnitDisplay().wPan.wToHitR.setText("---");
            clientgui.getUnitDisplay().wPan.toHitText.setText("");
        }
        updateSearchlight();
    }

    /**
     * Cache the list of visible targets. This is used for the 'next target'
     * button.
     * <p>
     * We'll sort it by range to us.
     */
    private void cacheVisibleTargets() {
        clearVisibleTargets();

        List<Entity> vec = clientgui.getClient().getGame().getValidTargets(ce());
        Comparator<Entity> sortComp = (x, y) -> {
            int rangeToX = ce().getPosition().distance(x.getPosition());
            int rangeToY = ce().getPosition().distance(y.getPosition());
            if (rangeToX == rangeToY) {
                return x.getId() < y.getId() ? -1 : 1;
            }
            return rangeToX < rangeToY ? -1 : 1;
        };

        TreeSet<Entity> tree = new TreeSet<>(sortComp);
        visibleTargets = new Entity[vec.size()];

        tree.addAll(vec);

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

        clientgui.getBoardView().centerOnHex(targ.getPosition());
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

        clientgui.getBoardView().centerOnHex(targ.getPosition());
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
            || ((b.getButton() != MouseEvent.BUTTON1))) {
            return;
        }
        // control pressed means a line of sight check.
        // added ALT_MASK by kenn
        if (((b.getModifiers() & InputEvent.CTRL_DOWN_MASK) != 0)
            || ((b.getModifiers() & InputEvent.ALT_DOWN_MASK) != 0)) {
            return;
        }
        // check for shifty goodness
        if (shiftheld != ((b.getModifiers() & InputEvent.SHIFT_DOWN_MASK) != 0)) {
            shiftheld = (b.getModifiers() & InputEvent.SHIFT_DOWN_MASK) != 0;
        }

        if (b.getType() == BoardViewEvent.BOARD_HEX_DRAGGED) {
            if (shiftheld) {
                updateFlipArms(false);
            }
            clientgui.getBoardView().cursor(b.getCoords());
        } else if (b.getType() == BoardViewEvent.BOARD_HEX_CLICKED) {
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
            } else if (phase.isTargeting()) {
                target(new HexTarget(b.getCoords(), Targetable.TYPE_HEX_ARTILLERY));
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
                .booleanOption(OptionsConstants.BASE_FRIENDLY_FIRE);
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
        List<Targetable> targets = new ArrayList<>();
        final Player localPlayer = clientgui.getClient().getLocalPlayer();
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

        targets.add(new HexTarget(pos, Targetable.TYPE_HEX_TAG));

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
                                    "FiringDisplay.ChooseTargetDialog.message",
                                    new Object[] { pos.getBoardNum() }),
                            Messages.getString("FiringDisplay.ChooseTargetDialog.title"),
                            JOptionPane.QUESTION_MESSAGE, null, SharedUtility
                                    .getDisplayArray(targets), null);
            choice = SharedUtility.getTargetPicked(targets, input);
        }

        // Return the chosen unit.
        return choice;
    }

    //
    // GameListener
    //
    @Override
    public void gameTurnChange(GameTurnChangeEvent e) {
        // In case of a /reset command, ensure the state gets reset
        if (clientgui.getClient().getGame().getPhase().isLounge()) {
            endMyTurn();
        }
        // On simultaneous phases, each player ending their turn will generate a turn change
        // We want to ignore turns from other players and only listen to events we generated
        // Except on the first turn
        if (clientgui.getClient().getGame().getPhase().isSimultaneous(clientgui.getClient().getGame())
                && (e.getPreviousPlayerId() != clientgui.getClient().getLocalPlayerNumber())
                && (clientgui.getClient().getGame().getTurnIndex() != 0)) {
            return;
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
                setStatusBarText(Messages.getString("TargetingPhaseDisplay.its_your_turn"));
            } else {
                endMyTurn();
                if (e.getPlayer() != null) {
                    setStatusBarText(Messages.getString("TargetingPhaseDisplay.its_others_turn",
                            e.getPlayer().getName()));
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
            setStatusBarText(Messages.getString("TargetingPhaseDisplay.waitingForFiringPhase"));
        }
    }

    //
    // ActionListener
    //
    @Override
    public void actionPerformed(ActionEvent ev) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (!clientgui.getClient().isMyTurn()) {
            return;
        }

        if (ev.getActionCommand().equals(TargetingCommand.FIRE_FIRE.getCmd())) {
            fire();
        } else if (ev.getActionCommand().equals(TargetingCommand.FIRE_SKIP.getCmd())) {
            nextWeapon();
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
        } else if (ev.getActionCommand().equals(TargetingCommand.FIRE_DISENGAGE.getCmd())
                && clientgui.doYesNoDialog(Messages.getString("MovementDisplay.EscapeDialog.title"),
                        Messages.getString("MovementDisplay.EscapeDialog.message"))) {
            clear();
            attacks.add(new DisengageAction(cen));
            ready();
        }
    }

    private void updateFlipArms(boolean armsFlipped) {
        if (armsFlipped == ce().getArmsFlipped()) {
            return;
        }

        clearAttacks();
        ce().setArmsFlipped(armsFlipped);
        attacks.addElement(new FlipArmsAction(cen, armsFlipped));
        updateTarget();
        refreshAll();
    }

    private void updateSearchlight() {
        setSearchlightEnabled((ce() != null)
                && (target != null)
                && ce().isUsingSearchlight()
                && ce().getCrew().isActive()
                && SearchlightAttackAction.isPossible(clientgui.getClient().getGame(), cen, target, null));
    }

    private void setFireEnabled(boolean enabled) {
        buttons.get(TargetingCommand.FIRE_FIRE).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(FiringCommand.FIRE_FIRE.getCmd(), enabled);
    }

    private void setSkipEnabled(boolean enabled) {
        buttons.get(TargetingCommand.FIRE_SKIP).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(FiringCommand.FIRE_SKIP.getCmd(), enabled);
    }

    private void setFlipArmsEnabled(boolean enabled) {
        buttons.get(TargetingCommand.FIRE_FLIP_ARMS).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(FiringCommand.FIRE_FLIP_ARMS.getCmd(), enabled);
    }

    private void setNextEnabled(boolean enabled) {
        buttons.get(TargetingCommand.FIRE_NEXT).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(FiringCommand.FIRE_NEXT.getCmd(), enabled);
    }

    private void setSearchlightEnabled(boolean enabled) {
        buttons.get(TargetingCommand.FIRE_SEARCHLIGHT).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(FiringCommand.FIRE_SEARCHLIGHT.getCmd(), enabled);
    }

    private void setFireModeEnabled(boolean enabled) {
        buttons.get(TargetingCommand.FIRE_MODE).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(FiringCommand.FIRE_MODE.getCmd(), enabled);
    }

    private void setNextTargetEnabled(boolean enabled) {
        buttons.get(TargetingCommand.FIRE_NEXT_TARG).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(FiringCommand.FIRE_NEXT_TARG.getCmd(), enabled);
    }

    private void setDisengageEnabled(boolean enabled) {
        if (buttons.containsKey(TargetingCommand.FIRE_DISENGAGE)) {
            buttons.get(TargetingCommand.FIRE_DISENGAGE).setEnabled(enabled);
        }
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
    @Override
    public void itemStateChanged(ItemEvent evt) {

    }

    // board view listener
    @Override
    public void finishedMovingUnits(BoardViewEvent b) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (clientgui.getClient().isMyTurn() && (ce() != null)) {
            clientgui.maybeShowUnitDisplay();
            clientgui.getBoardView().centerOnHex(ce().getPosition());
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
            clientgui.maybeShowUnitDisplay();
            clientgui.getUnitDisplay().displayEntity(e);
            if (e.isDeployed()) {
                clientgui.getBoardView().centerOnHex(e.getPosition());
            }
        }
    }

    /**
     * Stop just ignoring events and actually stop listening to them.
     */
    @Override
    public void removeAllListeners() {
        clientgui.getClient().getGame().removeGameListener(this);
        clientgui.getBoardView().removeBoardViewListener(this);
        clientgui.getUnitDisplay().wPan.weaponList.removeListSelectionListener(this);
    }

    @Override
    public void valueChanged(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }

        if ((clientgui.getClient().getGame().getPhase().isTargeting()) &&
                (event.getSource().equals(clientgui.getUnitDisplay().wPan.weaponList))) {
            // update target data in weapon display
            updateTarget();
        }
    }
    
    public void FieldofFire(Entity unit, int[][] ranges, int arc, int loc, int facing) {
        // do nothing here outside the arty targeting phase
        if (!clientgui.getClient().getGame().getPhase().isTargeting() &&
                !clientgui.getClient().getGame().getPhase().isOffboard()) {
            return;
        }
        
        clientgui.getBoardView().fieldofFireUnit = unit;
        clientgui.getBoardView().fieldofFireRanges = ranges;
        clientgui.getBoardView().fieldofFireWpArc = arc;
        clientgui.getBoardView().fieldofFireWpLoc = loc;
        
        clientgui.getBoardView().setWeaponFieldofFire(facing, unit.getPosition());
    }
}

/*
 * MegaMek -
 * Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
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

import megamek.client.event.BoardViewEvent;
import megamek.client.ui.Messages;
import megamek.client.ui.SharedUtility;
import megamek.client.ui.swing.unitDisplay.WeaponPanel;
import megamek.client.ui.swing.util.CommandAction;
import megamek.client.ui.swing.util.KeyCommandBind;
import megamek.client.ui.swing.util.MegaMekController;
import megamek.client.ui.swing.widget.MegamekButton;
import megamek.client.ui.swing.widget.SkinSpecification;
import megamek.common.*;
import megamek.common.actions.*;
import megamek.common.enums.AimingMode;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GameTurnChangeEvent;
import megamek.common.options.OptionsConstants;
import megamek.common.util.FiringSolution;
import megamek.common.weapons.Weapon;
import megamek.common.weapons.capitalweapons.CapitalMissileWeapon;
import megamek.common.weapons.mortars.VehicularGrenadeLauncherWeapon;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.*;
import java.util.*;

public class FiringDisplay extends StatusBarPhaseDisplay implements ItemListener, ListSelectionListener {
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
        FIRE_ACTIVATE_SPA("fireActivateSPA"),
        FIRE_MORE("fireMore");

        String cmd;

        /**
         * Priority that determines this buttons order
         */
        public int priority;

        private FiringCommand(String c) {
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
    private ArrayList<Coords> strafingCoords = new ArrayList<>(5);

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
        attacks = new Vector<>();

        setupStatusBar(Messages.getString("FiringDisplay.waitingForFiringPhase"));

        buttons = new HashMap<>((int) (FiringCommand.values().length * 1.25 + 0.5));
        for (FiringCommand cmd : FiringCommand.values()) {
            String title = Messages.getString("FiringDisplay." + cmd.getCmd());
            MegamekButton newButton = new MegamekButton(title,
                    SkinSpecification.UIComponents.PhaseDisplayButton.getComp());
            String ttKey = "FiringDisplay." + cmd.getCmd() + ".tooltip";
            if (Messages.keyExists(ttKey)) {
                newButton.setToolTipText(Messages.getString(ttKey));
            }
            newButton.addActionListener(this);
            newButton.setActionCommand(cmd.getCmd());
            newButton.setEnabled(false);
            buttons.put(cmd, newButton);
        }
        numButtonGroups = (int) Math.ceil((buttons.size() + 0.0) / buttonsPerGroup);

        butDone.setText("<html><b>" + Messages.getString("FiringDisplay.Done") + "</b></html>");
        butDone.setEnabled(false);

        setupButtonPanel();

        clientgui.getBoardView().addKeyListener(this);

        // mech display.
        clientgui.getUnitDisplay().wPan.weaponList.addListSelectionListener(this);
        clientgui.getUnitDisplay().wPan.weaponList.addKeyListener(this);

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

        // Register the action for TWIST_LEFT
        controller.registerCommandAction(KeyCommandBind.TWIST_LEFT.cmd,
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
                                || clientgui.getBoardView().getChatterBoxActive()
                                || !display.isVisible()
                                || display.isIgnoringEvents()
                                || !buttons.get(FiringCommand.FIRE_FIRE).isEnabled()) {
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
                        jumpToTarget(true, false, false);
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
                        jumpToTarget(false, false, false);
                    }
                });

        // Register the action for NEXT_TARGET
        controller.registerCommandAction(KeyCommandBind.NEXT_TARGET_VALID.cmd,
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
                        jumpToTarget(true, true, false);
                    }
                });

        // Register the action for PREV_TARGET
        controller.registerCommandAction(KeyCommandBind.PREV_TARGET_VALID.cmd,
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
                        jumpToTarget(false, true, false);
                    }
                });

        // Register the action for NEXT_TARGET
        controller.registerCommandAction(KeyCommandBind.NEXT_TARGET_NOALLIES.cmd,
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
                        jumpToTarget(true, false, true);
                    }
                });

        // Register the action for PREV_TARGET
        controller.registerCommandAction(KeyCommandBind.PREV_TARGET_NOALLIES.cmd,
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
                        jumpToTarget(false, false, true);
                    }
                });

        // Register the action for NEXT_TARGET
        controller.registerCommandAction(KeyCommandBind.NEXT_TARGET_VALID_NO_ALLIES.cmd,
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
                        jumpToTarget(true, true, true);
                    }
                });

        // Register the action for PREV_TARGET
        controller.registerCommandAction(KeyCommandBind.PREV_TARGET_VALID_NO_ALLIES.cmd,
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
                        jumpToTarget(false, true, true);
                    }
                });

        // Register the action for VIEW_ACTING_UNIT
        controller.registerCommandAction(KeyCommandBind.VIEW_ACTING_UNIT.cmd,
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
                        if (!Objects.equals(ce(), clientgui.getUnitDisplay().getCurrentEntity())
                                && clientgui.getUnitDisplay().isVisible()) {
                            Entity en_Target = clientgui.getUnitDisplay().getCurrentEntity();
                            // Avoided using selectEntity(), to avoid centering on active unit
                            clientgui.getUnitDisplay().displayEntity(ce());
                            clientgui.getUnitDisplay().showPanel("weapons");
                            clientgui.getUnitDisplay().wPan.selectFirstWeapon();
                            target(en_Target);
                        }
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

    @Override
    protected ArrayList<MegamekButton> getButtonList() {
        ArrayList<MegamekButton> buttonList = new ArrayList<>();
        int i = 0;
        FiringCommand[] commands = FiringCommand.values();
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
            setStatusBarText(Messages.getString("FiringDisplay.its_your_turn"));
        }

        if (clientgui.getClient().getGame().getEntity(en) != null) {
            cen = en;
            clientgui.setSelectedEntityNum(en);
            clientgui.getUnitDisplay().displayEntity(ce());

            // If the selected entity is not on the board, use the next one.
            // ASSUMPTION: there will always be *at least one* entity on map.
            if (ce().getPosition() == null) {

                // Walk through the list of entities for this player.
                for (int nextId = clientgui.getClient().getNextEntityNum(en); nextId != en;
                     nextId = clientgui.getClient().getNextEntityNum(nextId)) {

                    if (clientgui.getClient().getGame().getEntity(nextId).getPosition() != null) {
                        cen = nextId;
                        break;
                    }
                } // Check the player's next entity.

                // We were *supposed* to have found an on-board entity.
                if (ce().getPosition() == null) {
                    LogManager.getLogger().error("Could not find an on-board entity " + en);
                    return;
                }
            }

            if (ce().isMakingVTOLGroundAttack()) {
                this.updateVTOLGroundTarget();
            } else {
                // Need to clear attacks again in case previous en was making VTOL ground attack
                clearAttacks();
                int lastTarget = ce().getLastTarget();
                if (ce() instanceof Mech) {
                    int grapple = ce().getGrappled();
                    if (grapple != Entity.NONE) {
                        lastTarget = grapple;
                    }
                }
                Entity t = clientgui.getClient().getGame().getEntity(lastTarget);
                target(t);
            }

            if (!ce().isOffBoard()) {
                clientgui.getBoardView().highlight(ce().getPosition());
            }
            clientgui.getBoardView().select(null);
            clientgui.getBoardView().cursor(null);

            refreshAll();
            cacheVisibleTargets();

            if (!ce().isOffBoard()) {
                clientgui.getBoardView().centerOnHex(ce().getPosition());
            }

            // only twist if crew conscious
            setTwistEnabled(ce().canChangeSecondaryFacing() && ce().getCrew().isActive());

            setFindClubEnabled(FindClubAction.canMechFindClub(clientgui.getClient().getGame(), en));
            setFlipArmsEnabled(ce().canFlipArms());
            updateSearchlight();
            updateClearTurret();
            updateClearWeaponJam();
            updateStrafe();

            // Hidden units can only spot
            if ((ce() != null) && ce().isHidden()) {
                setFireEnabled(false);
                setTwistEnabled(false);
                setFindClubEnabled(false);
                setFlipArmsEnabled(false);
                setStrafeEnabled(false);
                clientgui.getUnitDisplay().wPan.toHitText.setText("Hidden units are only allowed to spot!");
            }
        } else {
            LogManager.getLogger().error("Tried to select non-existent entity " + en);
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
                    && (!enemyTarget || EntityVisibilityUtils.detectedOrHasVisual(localPlayer, game, target))
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
    protected void beginMyTurn() {
        target = null;

        if (!clientgui.getBoardView().isMovingUnits()) {
            clientgui.maybeShowUnitDisplay();
        }
        clientgui.getBoardView().clearFieldofF();

        selectEntity(clientgui.getClient().getFirstEntityNum());

        GameTurn turn = clientgui.getClient().getMyTurn();
        // There's special processing for triggering AP Pods.
        if ((turn instanceof GameTurn.TriggerAPPodTurn) && (ce() != null)) {
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
            if (numButtonGroups > 1) {
                buttons.get(FiringCommand.FIRE_MORE).setEnabled(true);
            }
            setFireCalledEnabled(clientgui.getClient().getGame().getOptions()
                    .booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_CALLED_SHOTS));
            clientgui.getBoardView().select(null);
        }

        startTimer();
    }

    /**
     * Does end turn stuff.
     */
    protected void endMyTurn() {
        stopTimer();

        // end my turn, then.
        Game game = clientgui.getClient().getGame();
        Entity next = game.getNextEntity(game.getTurnIndex());
        if (game.getPhase().isFiring() && (next != null) && (ce() != null)
                && (next.getOwnerId() != ce().getOwnerId())) {
            clientgui.setUnitDisplayVisible(false);
        }
        cen = Entity.NONE;
        target(null);
        clientgui.getBoardView().select(null);
        clientgui.getBoardView().highlight(null);
        clientgui.getBoardView().cursor(null);
        clientgui.getBoardView().clearMovementData();
        clientgui.getBoardView().clearFiringSolutionData();
        clientgui.getBoardView().clearStrafingCoords();
        clientgui.getBoardView().clearFieldofF();
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
    protected void changeMode(boolean forward) {
        int wn = clientgui.getUnitDisplay().wPan.getSelectedWeaponNum();

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
        int nMode = m.switchMode(forward);
        // BattleArmor can fire popup-mine launchers individually. The mode determines
        // how many will be fired, but we don't want to set the mode higher than the
        // number of troopers in the squad.
        if ((ce() instanceof BattleArmor)
                && (m.getType() instanceof WeaponType)
                && m.getType().hasFlag(WeaponType.F_BA_INDIVIDUAL)
                && (m.curMode().getName().contains("-shot"))
                && (Integer.parseInt(m.curMode().getName().replace("-shot", "")) > ce().getTotalInternal())) {
            m.setMode(0);
        }
        clientgui.getClient().sendModeChange(cen, wn, nMode);

        // notify the player
        if (m.canInstantSwitch(nMode)) {
            clientgui.systemMessage(Messages.getString("FiringDisplay.switched", m.getName(),
                    m.curMode().getDisplayableName(true)));
        } else {
            clientgui.systemMessage(Messages.getString("FiringDisplay.willSwitch", m.getName(),
                    m.pendingMode().getDisplayableName(true)));
        }

        updateTarget();
        clientgui.getUnitDisplay().wPan.displayMech(ce());
        clientgui.getUnitDisplay().wPan.selectWeapon(wn);
    }

    /**
     * Called Shots - changes the current called shots selection
     */
    protected void changeCalled() {
        int wn = clientgui.getUnitDisplay().wPan.getSelectedWeaponNum();

        // Do nothing we have no unit selected.
        if (ce() == null) {
            return;
        }

        Mounted m = ce().getEquipment(wn);
        if (m == null) {
            return;
        }

        // send change to the server
        m.getCalledShot().switchCalledShot();
        clientgui.getClient().sendCalledShotChange(cen, wn);

        updateTarget();
        clientgui.getUnitDisplay().wPan.displayMech(ce());
        clientgui.getUnitDisplay().wPan.selectWeapon(wn);
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
        Comparator<Entity> sortComp = (entX, entY) -> {
            int rangeToX = ce().getPosition().distance(entX.getPosition());
            int rangeToY = ce().getPosition().distance(entY.getPosition());

            if (rangeToX == rangeToY) {
                return ((entX.getId() < entY.getId()) ? -1 : 1);
            }

            return ((rangeToX < rangeToY) ? -1 : 1);
        };

        // put the vector in the TreeSet first to sort it.
        TreeSet<Entity> tree = new TreeSet<>(sortComp);
        visibleTargets = new Entity[vec.size()];

        tree.addAll(vec);

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
    private Entity getNextTarget(boolean nextOrPrev, boolean onlyValid,
            boolean ignoreAllies) {
        if (visibleTargets == null) {
            return null;
        }

        Entity result = null;
        boolean done = false;
        int count = 0;
        // Loop until we hit an exit criteria
        //  Default is one iteration, but may need to skip invalid or allies
        while (!done) {
            // Increment or decrement target index
            if (nextOrPrev) {
                lastTargetID++;
            } else {
                lastTargetID--;
            }
            // Check bounds
            if (lastTargetID < 0) {
                lastTargetID = visibleTargets.length - 1;
            } else if (lastTargetID >= visibleTargets.length) {
                lastTargetID = 0;
            }
            //If we've cycled through all visible targets without finding a valid one, stop looping
            count++;
            if (count > visibleTargets.length) {
                return null;
            }
            // Store target
            result = visibleTargets[lastTargetID];
            done = true;
            // Check done
            if (onlyValid) {
                ToHitData toHit = WeaponAttackAction.toHit(
                        clientgui.getClient().getGame(), ce().getId(), result,
                        clientgui.getUnitDisplay().wPan.getSelectedWeaponNum(),
                        isStrafing);
                done &= toHit.getValue() != TargetRoll.AUTOMATIC_FAIL
                        && toHit.getValue() != TargetRoll.IMPOSSIBLE
                        && toHit.getValue() <= 12;
            }
            if (ignoreAllies) {
                done &= result.isEnemyOf(ce());
            }
        }
        return result;
    }

    /**
     * Jump to our next target. If there isn't one, well, don't do anything.
     */
    private void jumpToTarget(boolean nextTarg, boolean onlyValid, boolean ignoreAllies) {
        Entity targ = getNextTarget(nextTarg, onlyValid, ignoreAllies);
        if (targ == null) {
            return;
        }

        // HACK : don't show the choice dialog.
        showTargetChoice = false;

        clientgui.getBoardView().centerOnHex(targ.getPosition());
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
        if (attacks.isEmpty() && GUIPreferences.getInstance().getNagForNoAction()) {
            // confirm this action
            String title = Messages.getString("FiringDisplay.DontFireDialog.title");
            String body = Messages.getString("FiringDisplay.DontFireDialog.message");
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
                    Mounted weapon = ce().getEquipment(((WeaponAttackAction) action).getWeaponId());
                    totalheat += weapon.getCurrentHeat();
                }
            }

            if (totalheat > ce().getHeatCapacity()) {
                // confirm this action
                String title = Messages.getString("FiringDisplay.OverheatNag.title");
                String body = Messages.getString("FiringDisplay.OverheatNag.message");
                ConfirmDialog response = clientgui.doYesNoBotherDialog(title, body);
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
        Vector<EntityAction> newAttacks = new Vector<>();
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
                    waa2.setAmmoCarrier(waa.getAmmoCarrier());
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
                    waa2.setAmmoCarrier(waa.getAmmoCarrier());
                    waa2.setBombPayload(waa.getBombPayload());
                    waa2.setStrafing(waa.isStrafing());
                    waa2.setStrafingFirstShot(waa.isStrafingFirstShot());
                    newAttacks.addElement(waa2);
                }
            }
        }

        // If the user picked a hex along the flight path, server needs to know
        if ((target instanceof Entity) && Compute.isGroundToAir(ce(), target)) {
            Coords targetPos = ((Entity) target).getPlayerPickedPassThrough(cen);
            if (targetPos != null) {
                clientgui.getClient().sendPlayerPickedPassThrough(
                        ((Entity) target).getId(), cen, targetPos);
            }
        }

        // send out attacks
        clientgui.getClient().sendAttackData(cen, newAttacks);

        // clear queue
        attacks.removeAllElements();

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
        String title = Messages.getString("FiringDisplay.ClearTurret.title");
        String body = Messages.getString("FiringDisplay.ClearTurret.message");
        if (!clientgui.doYesNoDialog(title, body)) {
            return;
        }

        if ((attacks.isEmpty() && (ce() instanceof Tank)
                && (((Tank) ce()).isTurretJammed(((Tank) ce()).getLocTurret())))
                || ((Tank) ce()).isTurretJammed(((Tank) ce()).getLocTurret2())) {
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
                Messages.getString("FiringDisplay.ClearWeaponJam.question"),
                Messages.getString("FiringDisplay.ClearWeaponJam.title"),
                JOptionPane.QUESTION_MESSAGE, null, names, null);

        if (input != null) {
            for (int loop = 0; loop < names.length; loop++) {
                if (input.equals(names[loop])) {
                    RepairWeaponMalfunctionAction rwma = new RepairWeaponMalfunctionAction(
                            ce().getId(), ce().getEquipmentNum(weapons.get(loop)));
                    attacks.add(rwma);
                    ready();
                }
            }
        }
    }

    /**
     * This pops up a menu allowing the user to choose one of several 
     * SPAs, and then performs the appropriate steps.
     */
    private void doActivateSpecialAbility() {
        Map<String, String> skillNames = new HashMap<>();

        if (canActivateBloodStalker() && (target != null)) {
            skillNames.put("Blood Stalker", OptionsConstants.GUNNERY_BLOOD_STALKER);
        }

        String targetString = (target != null) ?
                String.format("\nTarget: %s", target.getDisplayName()) : "";

        String input = (String) JOptionPane.showInputDialog(clientgui,
                String.format("Pick a Special Pilot Ability to activate.%s", targetString),
                "Activate Special Pilot Ability",
                JOptionPane.QUESTION_MESSAGE, null, skillNames.keySet().toArray(), null);

        // unsafe, but since we're generating it right here, it should be fine.
        switch (skillNames.get(input)) {
            case OptionsConstants.GUNNERY_BLOOD_STALKER:
                // figure out when to clear Blood Stalker (when unit destroyed or flees or fly off no return)
                ActivateBloodStalkerAction bloodStalkerAction = new ActivateBloodStalkerAction(ce().getId(), target.getTargetId());
                attacks.add(0, bloodStalkerAction);
                ce().setBloodStalkerTarget(target.getTargetId());
                break;
        }

        updateActivateSPA();
    }

    /**
     * Worker function that determines if we can activate the "blood stalker" ability
     */
    private boolean canActivateBloodStalker() {
        // can be activated if the entity can do it and haven't done it already 
        // and the target is something that can be blood-stalked
        return (ce() != null) && ce().canActivateBloodStalker() &&
                (target != null) && (target.getTargetType() == Targetable.TYPE_ENTITY) &&
                attacks.stream().noneMatch(item -> item instanceof ActivateBloodStalkerAction);
    }

    /**
     * fire searchlight
     */
    protected void doSearchlight() {
        // validate
        if ((ce() == null) || (target == null)) {
            throw new IllegalArgumentException("current searchlight parameters are invalid");
        }

        if (!SearchlightAttackAction.isPossible(clientgui.getClient().getGame(), cen, target, null)) {
            return;
        }

        // create and queue a searchlight action
        SearchlightAttackAction saa = new SearchlightAttackAction(cen,
                target.getTargetType(), target.getTargetId());
        attacks.addElement(saa);

        // and add it into the game, temporarily
        clientgui.getClient().getGame().addAction(saa);
        clientgui.getBoardView().addAttack(saa);

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
        final Game game = clientgui.getClient().getGame();
        final int weaponId = clientgui.getUnitDisplay().wPan.getSelectedWeaponNum();
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
                        Entity.LOC_NONE, AimingMode.NONE, true);
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
                        Entity.LOC_NONE, AimingMode.NONE, true);
                toHitBuff.append(t.getDisplayName() + ": ");
                toHitBuff.append(toHit.getDesc());
                toHitBuff.append("\n");
            }
            Targetable hexTarget = new HexTarget(c, HexTarget.TYPE_HEX_CLEAR);
            toHit = WeaponAttackAction.toHit(game, cen, hexTarget, weaponId,
                    Entity.LOC_NONE, AimingMode.NONE, true);
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
        clientgui.getUnitDisplay().wPan.toHitText.setText(toHitBuff.toString());
    }

    private int[] getBombPayload(boolean isSpace, int limit) {
        int[] payload = new int[BombType.B_NUM];
        if (!ce().isBomber()) {
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

        int numFighters = ce().getActiveSubEntities().size();
        BombPayloadDialog bombsDialog = new BombPayloadDialog(
                clientgui.frame,
                Messages.getString("FiringDisplay.BombNumberDialog" + ".title"),
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
        final Game game = clientgui.getClient().getGame();
        // get the selected weaponnum
        final int weaponNum = clientgui.getUnitDisplay().wPan.getSelectedWeaponNum();
        Mounted mounted = ce().getEquipment(weaponNum);

        // validate
        if ((ce() == null)
                || (target == null && (!isStrafing || strafingCoords.isEmpty()))
                || (mounted == null)
                || !(mounted.getType() instanceof WeaponType)) {
            throw new IllegalArgumentException("current fire parameters are invalid");
        }
        // check if we now shoot at a target in the front arc and previously
        // shot a target in side/rear arc that then was primary target
        // if so, ask and tell the user that to-hits will change
        if (!game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_NO_FORCED_PRIMARY_TARGETS)
                && (ce() instanceof Mech) || (ce() instanceof Tank)
                || (ce() instanceof Protomech)) {
            EntityAction lastAction = null;
            try {
                lastAction = attacks.lastElement();
            } catch (NoSuchElementException ex) {
                // ignore
            }

            if (lastAction instanceof WeaponAttackAction) {
                WeaponAttackAction oldWaa = (WeaponAttackAction) lastAction;
                Targetable oldTarget = oldWaa.getTarget(game);
                if (!oldTarget.equals(target)) {
                    boolean oldInFront = Compute.isInArc(ce().getPosition(),
                            ce().getSecondaryFacing(), oldTarget, ce().getForwardArc());
                    boolean curInFront = Compute.isInArc(ce().getPosition(),
                            ce().getSecondaryFacing(), target, ce().getForwardArc());
                    if (!oldInFront && curInFront) {
                        String title = Messages.getString("FiringDisplay.SecondaryTargetToHitChange.title");
                        String body = Messages.getString("FiringDisplay.SecondaryTargetToHitChange.message");
                        if (!clientgui.doYesNoDialog(title, body)) {
                            return;
                        }
                    }
                }
            }
        }

        // declare searchlight, if possible
        if (GUIPreferences.getInstance().getAutoDeclareSearchlight() && ce().isUsingSearchlight()) {
            doSearchlight();
        }

        ArrayList<Targetable> targets = new ArrayList<>();
        if (isStrafing) {
            for (Coords c : strafingCoords) {
                targets.add(new HexTarget(c, Targetable.TYPE_HEX_CLEAR));
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
            if (!(mounted.getType().hasFlag(WeaponType.F_ARTILLERY)
                    || (mounted.getType() instanceof CapitalMissileWeapon
                            && Compute.isGroundToGround(ce(), t)))) {
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
                waa.setAmmoId(ammoMount.getEntity().getEquipmentNum(ammoMount));
                waa.setAmmoCarrier(ammoMount.getEntity().getId());
                if (((ammoType.getMunitionType() == AmmoType.M_THUNDER_VIBRABOMB) &&
                        ((ammoType.getAmmoType() == AmmoType.T_LRM)
                        || (ammoType.getAmmoType() == AmmoType.T_LRM_IMP)
                        || (ammoType.getAmmoType() == AmmoType.T_MML)))
                        || (ammoType.getMunitionType() == AmmoType.M_VIBRABOMB_IV)) {
                    VibrabombSettingDialog vsd = new VibrabombSettingDialog(
                            clientgui.frame);
                    vsd.setVisible(true);
                    waa.setOtherAttackInfo(vsd.getSetting());
                }
            }

            if (ash.allowAimedShotWith(mounted) && !ash.getAimingMode().isNone() && ash.isAimingAtLocation()) {
                waa.setAimedLocation(ash.getAimingAt());
                waa.setAimingMode(ash.getAimingMode());
            } else {
                waa.setAimedLocation(Entity.LOC_NONE);
                waa.setAimingMode(AimingMode.NONE);
            }
            waa.setStrafing(isStrafing);
            waa.setStrafingFirstShot(firstShot);
            firstShot = false;

            // add the attack to our temporary queue
            attacks.addElement(waa);

            // and add it into the game, temporarily
            game.addAction(waa);

        }
        // set the weapon as used
        mounted.setUsedThisRound(true);

        // find the next available weapon
        int nextWeapon = clientgui.getUnitDisplay().wPan.getNextWeaponNum();

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
        clientgui.getUnitDisplay().wPan.displayMech(ce());
        Mounted nextMounted = ce().getEquipment(nextWeapon);
        if (!mounted.getType().hasFlag(WeaponType.F_VGL) && (nextMounted != null)
                && nextMounted.getType().hasFlag(WeaponType.F_VGL)) {
            clientgui.getUnitDisplay().wPan.setPrevTarget(target);
        }
        clientgui.getUnitDisplay().wPan.selectWeapon(nextWeapon);
        updateTarget();

    }

    /**
     * Skips to the next weapon
     */
    void nextWeapon() {
        if (ce() == null) {
            return;
        }
        clientgui.getUnitDisplay().wPan.selectNextWeapon();

        if (ce().getId() != clientgui.getUnitDisplay().wPan.getSelectedEntityId()) {
            clientgui.getUnitDisplay().wPan.displayMech(ce());
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

        clientgui.getUnitDisplay().wPan.selectPrevWeapon();

        if (ce().getId() != clientgui.getUnitDisplay().wPan.getSelectedEntityId()) {
            clientgui.getUnitDisplay().wPan.displayMech(ce());
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

        // confirm this action
        String title = Messages.getString("FiringDisplay.FindClubDialog.title");
        String body = Messages.getString("FiringDisplay.FindClubDialog.message");
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
            String title = Messages.getString("FiringDisplay.CantSpotDialog.title");
            String body = Messages.getString("FiringDisplay.CantSpotDialog.message");
            clientgui.doAlertDialog(title, body);
            return;
        }
        // confirm this action
        String title = Messages.getString("FiringDisplay.SpotForInderectDialog.title");
        String body = Messages.getString("FiringDisplay.SpotForInderectDialog.message");
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
        clientgui.getBoardView().clearStrafingCoords();

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
        clientgui.getBoardView().removeAttacksFor(ce());
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
                clientgui.getUnitDisplay().wPan.displayMech(ce());
                clientgui.getClient().getGame().removeAction(o);
                clientgui.getBoardView().refreshAttacks();
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
        clientgui.getBoardView().redrawEntity(ce());
        clientgui.getUnitDisplay().displayEntity(ce());
        clientgui.getUnitDisplay().showPanel("weapons");
        clientgui.getUnitDisplay().wPan.selectFirstWeapon();
        if (ce().isMakingVTOLGroundAttack()) {
            this.updateVTOLGroundTarget();
        }
        updateTarget();
    }

    /**
     * Targets something
     */
    public void target(Targetable t) {
        if (ce() == null) {
            return;
        }
        final int weaponId = clientgui.getUnitDisplay().wPan.getSelectedWeaponNum();
        Mounted weapon = ce().getEquipment(weaponId);
        // Some weapons pick an automatic target
        if ((weapon != null) && weapon.getType().hasFlag(WeaponType.F_VGL)) {
            Targetable hexTarget = VehicularGrenadeLauncherWeapon.getTargetHex(weapon, weaponId);
            // Ignore events that will be generated by the select/cursor calls
            setIgnoringEvents(true);
            clientgui.getBoardView().select(hexTarget.getPosition());
            setIgnoringEvents(false);
            target = hexTarget;
        } else {
            target = t;
            if ((visibleTargets != null) && (target != null)) {
                // Set last target ID, so next/prev target behaves correctly
                for (int i = 0; i < visibleTargets.length; i++) {
                    if (visibleTargets[i].getId() == target.getTargetId()) {
                        lastTargetID = i;
                        break;
                    }
                }
            }
        }
        if ((target instanceof Entity) && Compute.isGroundToAir(ce(), target)) {
            Coords targetPos = Compute.getClosestFlightPath(cen, ce().getPosition(), (Entity) target);
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
        Game game = clientgui.getClient().getGame();
        // allow spotting
        if ((ce() != null) && !ce().isSpotting() && ce().canSpot() && (target != null)
                && game.getOptions().booleanOption(OptionsConstants.BASE_INDIRECT_FIRE)) {
            boolean hasLos = LosEffects.calculateLOS(game, ce(), target).canSee();
            // In double blind, we need to "spot" the target as well as LoS
            if (hasLos
                    && game.getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND)
                    && !Compute.inVisualRange(game, ce(), target)
                    && !Compute.inSensorRange(game, ce(), target, null)) {
                hasLos = false;
            }
            setSpotEnabled(hasLos);
        } else {
            setSpotEnabled(false);
        }

        // update target panel
        final int weaponId = clientgui.getUnitDisplay().wPan.getSelectedWeaponNum();
        if (isStrafing && weaponId != -1) {
            clientgui.getUnitDisplay().wPan.wTargetR.setText(Messages
                    .getString("FiringDisplay.Strafing.TargetLabel"));
            updateStrafingTargets();
        } else if ((target != null) && (target.getPosition() != null)
            && (weaponId != -1) && (ce() != null)) {
            ToHitData toHit;
            if (!ash.getAimingMode().isNone()) {
                Mounted weapon = ce().getEquipment(weaponId);
                boolean aiming = ash.isAimingAtLocation() && ash.allowAimedShotWith(weapon);
                ash.setEnableAll(aiming);
                if (aiming) {
                    toHit = WeaponAttackAction.toHit(game, cen, target,
                            weaponId, ash.getAimingAt(), ash.getAimingMode(),
                            false);

                    String t =  String.format("<html><div WIDTH=%d>%s</div></html>", WeaponPanel.TARGET_DISPLAY_WIDTH, target.getDisplayName() + " (" + ash.getAimingLocation() + ")");
                    clientgui.getUnitDisplay().wPan.wTargetR.setText(t);
                } else {
                    toHit = WeaponAttackAction.toHit(game, cen, target, weaponId, Entity.LOC_NONE,
                            AimingMode.NONE, false);
                    String t =  String.format("<html><div WIDTH=%d>%s</div></html>", WeaponPanel.TARGET_DISPLAY_WIDTH, target.getDisplayName());
                    clientgui.getUnitDisplay().wPan.wTargetR.setText(t);
                }
                ash.setPartialCover(toHit.getCover());
            } else {
                toHit = WeaponAttackAction.toHit(game, cen, target, weaponId,
                        Entity.LOC_NONE, AimingMode.NONE, false);
                String t =  String.format("<html><div WIDTH=%d>%s</div></html>", WeaponPanel.TARGET_DISPLAY_WIDTH, target.getDisplayName());
                clientgui.getUnitDisplay().wPan.wTargetR.setText(t);
            }
            int effectiveDistance = Compute.effectiveDistance(game, ce(), target);
            clientgui.getUnitDisplay().wPan.wRangeR.setText("" + effectiveDistance);
            Mounted m = ce().getEquipment(weaponId);
            // If we have a Centurion Weapon System selected, we may need to
            //  update ranges.
            if (m.getType().hasFlag(WeaponType.F_CWS)) {
                clientgui.getUnitDisplay().wPan.selectWeapon(weaponId);
            }
            if (m.isUsedThisRound()) {
                clientgui.getUnitDisplay().wPan.wToHitR.setText(
                        Messages.getString("FiringDisplay.alreadyFired"));
                setFireEnabled(false);
            } else if ((m.getType().hasFlag(WeaponType.F_AUTO_TARGET)
                    && !m.curMode().equals(Weapon.MODE_AMS_MANUAL))
                    || (m.getType().hasModes() && m.curMode().equals("Point Defense"))) {
                clientgui.getUnitDisplay().wPan.wToHitR.setText(
                        Messages.getString("FiringDisplay.autoFiringWeapon"));
                setFireEnabled(false);
            } else if (m.isInBearingsOnlyMode()) {
                clientgui.getUnitDisplay().wPan.wToHitR.setText(
                        Messages.getString("FiringDisplay.bearingsOnlyWrongPhase"));
                setFireEnabled(false);
            } else if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
                clientgui.getUnitDisplay().wPan.wToHitR.setText(toHit.getValueAsString());
                setFireEnabled(false);
            } else if (toHit.getValue() == TargetRoll.AUTOMATIC_FAIL) {
                clientgui.getUnitDisplay().wPan.wToHitR.setText(toHit.getValueAsString());
                setFireEnabled(true);
            } else {
                boolean natAptGunnery = ce().hasAbility(OptionsConstants.PILOT_APTITUDE_GUNNERY);
                clientgui.getUnitDisplay().wPan.wToHitR.setText(toHit.getValueAsString()
                        + " (" + Compute.oddsAbove(toHit.getValue(), natAptGunnery) + "%)");
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

        if ((weaponId != -1) && (ce() != null) && !isStrafing) {
            adaptFireModeEnabled(ce().getEquipment(weaponId));
        } else {
            setFireModeEnabled(false);
        }

        updateSearchlight();
        updateActivateSPA();
        updateClearWeaponJam();
        updateClearTurret();

        // Hidden units can only spot
        if ((ce() != null) && ce().isHidden()) {
            setFireEnabled(false);
            setTwistEnabled(false);
            setFindClubEnabled(false);
            setFlipArmsEnabled(false);
            setStrafeEnabled(false);
            clientgui.getUnitDisplay().wPan.toHitText.setText("Hidden units are only allowed to spot!");
        }
    }

    /**
     * A VTOL or LAM in airmech mode making a bombing or strafing attack already has the target set
     * during the movement phase. 
     */
    void updateVTOLGroundTarget() {
        clientgui.getBoardView().clearStrafingCoords();
        target(null);
        isStrafing = false;
        strafingCoords.clear();
        if (ce().isBomber() && ((IBomber) ce()).isVTOLBombing()) {
            target(((IBomber) ce()).getVTOLBombTarget());
            clientgui.getBoardView().addStrafingCoords(target.getPosition());
        } else if ((ce() instanceof VTOL) && !((VTOL) ce()).getStrafingCoords().isEmpty()) {
            strafingCoords.addAll(((VTOL) ce()).getStrafingCoords());
            strafingCoords.forEach(c -> clientgui.getBoardView().addStrafingCoords(c));
            isStrafing = true;
        }
    }

    /**
     * Torso twist in the proper direction.
     */
    void torsoTwist(Coords twistTarget) {
        int direction = ce().getFacing();

        if (twistTarget != null) {
            direction = ce().clipSecondaryFacing(ce().getPosition().direction(twistTarget));
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
                    clientgui.getBoardView().addStrafingCoords(evtCoords);
                    updateStrafingTargets();
                }
            } else if (!evtCoords.equals(ce().getPosition())) {
                // HACK : sometimes we don't show the target choice window
                Targetable targ = null;
                if (showTargetChoice) {
                    targ = chooseTarget(evtCoords);
                }
                if (shiftheld) {
                    updateFlipArms(false);
                    torsoTwist(b.getCoords());
                } else if (targ != null && !ce().isMakingVTOLGroundAttack()) {
                    if ((targ instanceof Entity) && Compute.isGroundToAir(ce(), targ)) {
                        Entity entTarg = (Entity) targ;
                        boolean alreadyShotAt = false;
                        List<EntityAction> actions = clientgui.getClient()
                                .getGame().getActionsVector();
                        for (EntityAction action : actions) {
                            if (!(action instanceof AttackAction)) {
                                continue;
                            }
                            AttackAction aa = (AttackAction) action;
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
        // On simultaneous phases, each player ending their turn will generate a turn change
        // We want to ignore turns from other players and only listen to events we generated
        // Except on the first turn
        if (clientgui.getClient().getGame().getPhase().isSimultaneous(clientgui.getClient().getGame())
                && (e.getPreviousPlayerId() != clientgui.getClient().getLocalPlayerNumber())
                && (clientgui.getClient().getGame().getTurnIndex() != 0)) {
            return;
        }

        if (clientgui.getClient().getGame().getPhase().isFiring()) {
            if (clientgui.getClient().isMyTurn()) {
                if (cen == Entity.NONE) {
                    beginMyTurn();
                }
                setStatusBarText(Messages.getString("FiringDisplay.its_your_turn"));
            } else {
                endMyTurn();
                String playerName;
                if (e.getPlayer() != null) {
                    playerName = e.getPlayer().getName();
                } else {
                    playerName = "Unknown";
                }
                setStatusBarText(Messages.getString("FiringDisplay.its_others_turn", playerName));
            }
        }
    }

    @Override
    public void gamePhaseChange(GamePhaseChangeEvent e) {
        // In case of a /reset command, ensure the state gets reset
        if (clientgui.getClient().getGame().getPhase().isLounge()) {
            endMyTurn();
        }

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (clientgui.getClient().isMyTurn() && !clientgui.getClient().getGame().getPhase().isFiring()) {
            endMyTurn();
        }
        // if we're ending the firing phase, unregister stuff.
        if (clientgui.getClient().getGame().getPhase().isFiring()) {
            setStatusBarText(Messages.getString("FiringDisplay.waitingForFiringPhase"));
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
        } else if (!clientgui.getClient().isMyTurn()) {
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
            boolean onlyValidTargets = (ev.getModifiers() & ActionEvent.SHIFT_MASK) > 0;
            boolean ignoreAllies = (ev.getModifiers() & ActionEvent.CTRL_MASK) > 0;
            jumpToTarget(true, onlyValidTargets, ignoreAllies);
        } else if (ev.getActionCommand().equals(FiringCommand.FIRE_FLIP_ARMS.getCmd())) {
            updateFlipArms(!ce().getArmsFlipped());
            // Fire Mode - More Fire Mode button handling - Rasia
        } else if (ev.getActionCommand().equals(FiringCommand.FIRE_MODE.getCmd())) {
            changeMode(true);
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
        } else if (ev.getActionCommand().equals(FiringCommand.FIRE_ACTIVATE_SPA.getCmd())) {
            doActivateSpecialAbility();
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
        } else if (armsFlipped == ce().getArmsFlipped()) {
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
                && ce().isUsingSearchlight()
                && ce().getCrew().isActive()
                && !ce().isHidden()
                && SearchlightAttackAction.isPossible(clientgui.getClient().getGame(), cen, target, null)
                && !((ce() instanceof Tank) && (((Tank) ce()).getStunnedTurns() > 0)));
    }

    private void updateClearTurret() {
        setFireClearTurretEnabled((ce() instanceof Tank) && ((Tank) ce()).canClearTurret()
                && attacks.isEmpty());
    }

    private void updateClearWeaponJam() {
        setFireClearWeaponJamEnabled((ce() instanceof Tank) && ((Tank) ce()).canUnjamWeapon()
                && attacks.isEmpty());
    }

    private void updateStrafe() {
        if (ce().isAero()) {
            setStrafeEnabled(ce().getAltitude() <= 3 && !((IAero) ce()).isSpheroid());
        } else {
            setStrafeEnabled(false);
        }
    }

    private void updateActivateSPA() {
        setActivateSPAEnabled(canActivateBloodStalker());
    }

    protected void setFireEnabled(boolean enabled) {
        buttons.get(FiringCommand.FIRE_FIRE).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(FiringCommand.FIRE_FIRE.getCmd(), enabled);
    }

    protected void setTwistEnabled(boolean enabled) {
        buttons.get(FiringCommand.FIRE_TWIST).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(FiringCommand.FIRE_TWIST.getCmd(), enabled);
    }

    protected void setSkipEnabled(boolean enabled) {
        buttons.get(FiringCommand.FIRE_SKIP).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(FiringCommand.FIRE_SKIP.getCmd(), enabled);
    }

    protected void setFindClubEnabled(boolean enabled) {
        buttons.get(FiringCommand.FIRE_FIND_CLUB).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(FiringCommand.FIRE_FIND_CLUB.getCmd(), enabled);
    }

    protected void setNextTargetEnabled(boolean enabled) {
        buttons.get(FiringCommand.FIRE_NEXT_TARG).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(FiringCommand.FIRE_NEXT_TARG.getCmd(), enabled);
    }

    protected void setFlipArmsEnabled(boolean enabled) {
        buttons.get(FiringCommand.FIRE_FLIP_ARMS).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(FiringCommand.FIRE_FLIP_ARMS.getCmd(), enabled);
    }

    protected void setSpotEnabled(boolean enabled) {
        buttons.get(FiringCommand.FIRE_SPOT).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(FiringCommand.FIRE_SPOT.getCmd(), enabled);
    }

    protected void setSearchlightEnabled(boolean enabled) {
        buttons.get(FiringCommand.FIRE_SEARCHLIGHT).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(FiringCommand.FIRE_SEARCHLIGHT.getCmd(), enabled);
    }

    protected void setFireModeEnabled(boolean enabled) {
        buttons.get(FiringCommand.FIRE_MODE).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(FiringCommand.FIRE_MODE.getCmd(), enabled);
    }

    /**
     * Enables the mode button when mode switching is allowed
     * (always true except for LAMs with certain weapons) and
     * the weapon has modes. Disables otherwise.
     *
     * @param m The active weapon
     */
    protected void adaptFireModeEnabled(Mounted m) {
        setFireModeEnabled(m.isModeSwitchable() & m.getType().hasModes());
    }

    protected void setFireCalledEnabled(boolean enabled) {
        buttons.get(FiringCommand.FIRE_CALLED).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(FiringCommand.FIRE_CALLED.getCmd(), enabled);
    }

    protected void setFireClearTurretEnabled(boolean enabled) {
        buttons.get(FiringCommand.FIRE_CLEAR_TURRET).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(FiringCommand.FIRE_CLEAR_TURRET.getCmd(), enabled);
    }

    protected void setFireClearWeaponJamEnabled(boolean enabled) {
        buttons.get(FiringCommand.FIRE_CLEAR_WEAPON).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(FiringCommand.FIRE_CLEAR_WEAPON.getCmd(), enabled);
    }

    protected void setStrafeEnabled(boolean enabled) {
        buttons.get(FiringCommand.FIRE_STRAFE).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(FiringCommand.FIRE_STRAFE.getCmd(), enabled);
    }

    protected void setNextEnabled(boolean enabled) {
        buttons.get(FiringCommand.FIRE_NEXT).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(FiringCommand.FIRE_NEXT.getCmd(), enabled);
    }

    protected void setActivateSPAEnabled(boolean enabled) {
        buttons.get(FiringCommand.FIRE_ACTIVATE_SPA).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(FiringCommand.FIRE_ACTIVATE_SPA.getCmd(), enabled);
    }

    @Override
    public void clear() {
        if (clientgui.getClient().isMyTurn()) {
            setStatusBarText(Messages.getString("FiringDisplay.its_your_turn"));
        }
        if ((target instanceof Entity) && Compute.isGroundToAir(ce(), target)) {
            ((Entity) target).setPlayerPickedPassThrough(cen, null);
        }
        if ((ce() != null) && !ce().isMakingVTOLGroundAttack()) {
            target(null);
        }
        // if we're clearing a "blood stalker" activation from the queue, 
        // clear the local entity's blood stalker 
        if ((ce() != null) && attacks.stream().anyMatch(item -> item instanceof ActivateBloodStalkerAction)) {
            ce().setBloodStalkerTarget(Entity.NONE);
        }
        clearAttacks();
        clientgui.getBoardView().select(null);
        clientgui.getBoardView().cursor(null);
        refreshAll();
    }

    //
    // ItemListener
    //
    @Override
    public void itemStateChanged(ItemEvent ev) {

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
            if (clientgui.getClient().getMyTurn().isValidEntity(e, clientgui.getClient().getGame())) {
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

    @Override
    public void valueChanged(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }
        if (event.getSource().equals(clientgui.getUnitDisplay().wPan.weaponList)
                && clientgui.getClient().getGame().getPhase().isFiring()) {
            // If we aren't in the firing phase, there's no guarantee that cen
            // is set properly, hence we can't update

            // update target data in weapon display
            updateTarget();
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

    /**
     * Have the player select a target from the entities at the given coords.
     *
     * @param pos - the <code>Coords</code> containing targets.
     */
    private Targetable chooseTarget(Coords pos) {
        final Game game = clientgui.getClient().getGame();
        boolean friendlyFire = game.getOptions().booleanOption(OptionsConstants.BASE_FRIENDLY_FIRE);
        // Assume that we have *no* choice.
        Targetable choice = null;
        Iterator<Entity> choices;

        int wn = clientgui.getUnitDisplay().wPan.getSelectedWeaponNum();
        Mounted weap = ce().getEquipment(wn);

        // Check for weapon/ammo types that should automatically target hexes
        if ((weap != null) && (weap.getLinked() != null)
                && (weap.getLinked().getType() instanceof AmmoType)) {
            AmmoType aType = (AmmoType) weap.getLinked().getType();
            long munitionType = aType.getMunitionType();
            // Mek mortar flares should default to deliver flare
            if ((aType.getAmmoType() == AmmoType.T_MEK_MORTAR)
                    && (munitionType == AmmoType.M_FLARE)) {
                return new HexTarget(pos, Targetable.TYPE_FLARE_DELIVER);
            // Certain mek mortar types and LRMs should target hexes
            } else if (((aType.getAmmoType() == AmmoType.T_MEK_MORTAR)
                    || (aType.getAmmoType() == AmmoType.T_LRM)
                    || (aType.getAmmoType() == AmmoType.T_LRM_IMP))
                    && ((munitionType == AmmoType.M_AIRBURST)
                            || (munitionType == AmmoType.M_SMOKE_WARHEAD))) {
                return new HexTarget(pos, Targetable.TYPE_HEX_CLEAR);
            } else if (munitionType == AmmoType.M_MINE_CLEARANCE) {
                return new HexTarget(pos, Targetable.TYPE_HEX_CLEAR);
            }
        }
        // Get the available choices, depending on friendly fire
        if (friendlyFire) {
            choices = game.getEntities(pos);
        } else {
            choices = game.getEnemyEntities(pos, ce());
        }

        // Convert the choices into a List of targets.
        List<Targetable> targets = new ArrayList<>();
        final Player localPlayer = clientgui.getClient().getLocalPlayer();
        while (choices.hasNext()) {
            Entity t = choices.next();
            boolean isSensorReturn = false;
            boolean isVisible = true;
            boolean isHidden = false;
            if (t != null) {
                isSensorReturn = t.isSensorReturn(localPlayer);
                isVisible = t.hasSeenEntity(localPlayer);
                isHidden = t.isHidden();
            }

            if (!ce().equals(t) && !isSensorReturn && isVisible && !isHidden) {
                targets.add(t);
            }
        }

        // If there aren't other targets, check for targets flying over pos
        if (targets.isEmpty()) {
            List<Entity> flyovers = clientgui.getBoardView().getEntitiesFlyingOver(pos);
            for (Entity e : flyovers) {
                if (!targets.contains(e)) {
                    targets.add(e);
                }
            }
        }

        // Is there a building in the hex?
        Building bldg = clientgui.getClient().getGame().getBoard().getBuildingAt(pos);
        if (bldg != null) {
            targets.add(new BuildingTarget(pos, clientgui.getClient().getGame().getBoard(), false));
        }

        // If we clicked on a wooded hex with no other targets, clear woods
        if (targets.isEmpty()) {
            Hex hex = game.getBoard().getHex(pos);
            if (hex.containsTerrain(Terrains.WOODS)
                    || hex.containsTerrain(Terrains.JUNGLE)) {
                targets.add(new HexTarget(pos, Targetable.TYPE_HEX_CLEAR));
            }
        }

        // Do we have a single choice?
        if (targets.size() == 1) {
            // Return that choice.
            choice = targets.get(0);
        } else if (targets.size() > 1) {
            // If we have multiple choices, display a selection dialog.
            String input = (String) JOptionPane.showInputDialog(clientgui,
                    Messages.getString("FiringDisplay.ChooseTargetDialog.message", pos.getBoardNum()),
                    Messages.getString("FiringDisplay.ChooseTargetDialog.title"),
                    JOptionPane.QUESTION_MESSAGE, null,
                    SharedUtility.getDisplayArray(targets), null);
            choice = SharedUtility.getTargetPicked(targets, input);
        }

        // Return the chosen unit.
        return choice;
    }

    public Targetable getTarget() {
        return target;
    }

    private boolean validStrafingCoord(Coords newCoord) {
        // Only Aeros can strafe...
        if (ce() == null || !ce().isAero()) {
            return false;
        }

        // Can't update strafe hexes after weapons are fired, otherwise we'd
        // have to have a way to update the attacks vector
        if (!attacks.isEmpty()) {
            return false;
        }

        // Can only strafe hexes that were flown over
        if (!ce().passedThrough(newCoord)) {
            return false;
        }

        // No more limitations if it's the first hex
        if (strafingCoords.isEmpty()) {
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
                isInaLine &= iHex.isIntersectedBy(start.cx, start.cy, newHex.cx, newHex.cy);
            }
        }
        return isConsecutive && isInaLine;
    }

    public void FieldofFire(Entity unit, int[][] ranges, int arc, int loc, int facing) {
        // do nothing here outside the movement phase
        if (!clientgui.getClient().getGame().getPhase().isFiring()) {
            return;
        }

        clientgui.getBoardView().fieldofFireUnit = unit;
        clientgui.getBoardView().fieldofFireRanges = ranges;
        clientgui.getBoardView().fieldofFireWpArc = arc;
        clientgui.getBoardView().fieldofFireWpLoc = loc;

        clientgui.getBoardView().setWeaponFieldofFire(facing, unit.getPosition());
    }
}

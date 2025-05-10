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
package megamek.client.ui.swing.phaseDisplay;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.*;

import javax.swing.JOptionPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import megamek.client.event.BoardViewEvent;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.boardview.BoardView;
import megamek.client.ui.swing.boardview.IBoardView;
import megamek.client.ui.swing.phaseDisplay.dialog.BombPayloadDialog;
import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.phaseDisplay.dialog.TargetChoiceDialog;
import megamek.client.ui.swing.phaseDisplay.dialog.TriggerAPPodDialog;
import megamek.client.ui.swing.phaseDisplay.dialog.TriggerBPodDialog;
import megamek.client.ui.swing.phaseDisplay.dialog.VibrabombSettingDialog;
import megamek.client.ui.swing.util.KeyCommandBind;
import megamek.client.ui.swing.util.MegaMekController;
import megamek.client.ui.swing.widget.MegaMekButton;
import megamek.client.ui.swing.widget.MekPanelTabStrip;
import megamek.common.*;
import megamek.common.actions.*;
import megamek.common.annotations.Nullable;
import megamek.common.enums.AimingMode;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GameTurnChangeEvent;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.Weapon;
import megamek.common.weapons.capitalweapons.CapitalMissileWeapon;
import megamek.common.weapons.mortars.VehicularGrenadeLauncherWeapon;
import megamek.logging.MMLogger;

public class FiringDisplay extends AttackPhaseDisplay implements ListSelectionListener {
    private final static MMLogger logger = MMLogger.create(FiringDisplay.class);

    private static final long serialVersionUID = -5586388490027013723L;

    /**
     * This enumeration lists all of the possible ActionCommands that can be
     * carried out during the firing phase. Each command has a string for the
     * command plus a flag that determines what unit type it is appropriate for.
     *
     * @author arlith
     */
    public enum FiringCommand implements PhaseCommand {
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

        public String getHotKeyDesc() {
            String result = "";

            String msg_left = Messages.getString("Left");
            String msg_right = Messages.getString("Right");
            String msg_next = Messages.getString("Next");
            String msg_previous = Messages.getString("Previous");
            String msg_valid = Messages.getString("FiringDisplay.FireNextTarget.tooltip.Valid");
            String msg_noallies = Messages.getString("FiringDisplay.FireNextTarget.tooltip.NoAllies");

            switch (this) {
                case FIRE_NEXT:
                    result = "<BR>";
                    result += "&nbsp;&nbsp;" + msg_next + ": " + KeyCommandBind.getDesc(KeyCommandBind.NEXT_UNIT);
                    result += "&nbsp;&nbsp;" + msg_previous + ": " + KeyCommandBind.getDesc(KeyCommandBind.PREV_UNIT);
                    break;
                case FIRE_TWIST:
                    result = "<BR>";
                    result += "&nbsp;&nbsp;" + msg_left + ": " + KeyCommandBind.getDesc(KeyCommandBind.TWIST_LEFT);
                    result += "&nbsp;&nbsp;" + msg_right + ": " + KeyCommandBind.getDesc(KeyCommandBind.TWIST_RIGHT);
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
                    result += "&nbsp;&nbsp;" + msg_valid + " " + msg_next + ": "
                            + KeyCommandBind.getDesc(KeyCommandBind.NEXT_TARGET_VALID);
                    result += "&nbsp;&nbsp;" + msg_previous + ": "
                            + KeyCommandBind.getDesc(KeyCommandBind.PREV_TARGET_VALID);
                    result += "<BR>";
                    result += "&nbsp;&nbsp;" + msg_noallies + " " + msg_next + ": "
                            + KeyCommandBind.getDesc(KeyCommandBind.NEXT_TARGET_NOALLIES);
                    result += "&nbsp;&nbsp;" + msg_previous + ": "
                            + KeyCommandBind.getDesc(KeyCommandBind.PREV_TARGET_NOALLIES);
                    result += "<BR>";
                    result += "&nbsp;&nbsp;" + msg_valid + " (" + msg_noallies + ") " + msg_next + ": "
                            + KeyCommandBind.getDesc(KeyCommandBind.NEXT_TARGET_VALID_NO_ALLIES);
                    result += "&nbsp;&nbsp;" + msg_previous + ": "
                            + KeyCommandBind.getDesc(KeyCommandBind.PREV_TARGET_VALID_NO_ALLIES);
                    break;
                case FIRE_SKIP:
                    result = "<BR>";
                    result += "&nbsp;&nbsp;" + msg_next + ": " + KeyCommandBind.getDesc(KeyCommandBind.NEXT_WEAPON);
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
    private Map<FiringCommand, MegaMekButton> buttons;

    Targetable target; // target

    // HACK : track when we want to show the target choice dialog.
    protected boolean showTargetChoice = true;

    protected boolean twisting;

    protected Entity[] visibleTargets = null;

    protected int lastTargetID = -1;

    protected AimedShotHandler ash;

    protected boolean isStrafing = false;

    protected int phaseInternalBombs = 0;

    /**
     * Keeps track of the Coords that are in a strafing run.
     */
    private final List<Coords> strafingCoords = new ArrayList<>(5);

    /**
     * Creates and lays out a new firing phase display for the specified
     * clientgui.getClient().
     */
    public FiringDisplay(final ClientGUI clientgui) {
        super(clientgui);
        game.addGameListener(this);
        setupStatusBar(Messages.getString("FiringDisplay.waitingForFiringPhase"));
        setButtons();
        setButtonsTooltips();
        setupButtonPanel();
        clientgui.getUnitDisplay().wPan.weaponList.addListSelectionListener(this);
        ash = new AimedShotHandler(this);
        registerKeyCommands();
    }

    @Override
    protected String getDoneButtonLabel() {
        return Messages.getString("FiringDisplay.DoneTurn");
    }

    @Override
    protected String getSkipTurnButtonLabel() {
        return Messages.getString("FiringDisplay.SkipTurn");
    }

    @Override
    protected void setButtons() {
        buttons = new HashMap<>((int) (FiringCommand.values().length * 1.25 + 0.5));
        for (FiringCommand cmd : FiringCommand.values()) {
            buttons.put(cmd, createButton(cmd.getCmd(), "FiringDisplay."));
        }
        numButtonGroups = (int) Math.ceil((buttons.size() + 0.0) / buttonsPerGroup);
    }

    @Override
    protected void setButtonsTooltips() {
        for (FiringCommand cmd : FiringCommand.values()) {
            String tt = createToolTip(cmd.getCmd(), "FiringDisplay.", cmd.getHotKeyDesc());
            buttons.get(cmd).setToolTipText(tt);
        }
    }

    private boolean shouldPerformFireKeyCommand() {
        return shouldReceiveKeyCommands() && buttons.get(FiringCommand.FIRE_FIRE).isEnabled();
    }

    protected void twistLeft() {
        updateFlipArms(false);
        torsoTwist(0);
    }

    protected void twistRight() {
        updateFlipArms(false);
        torsoTwist(1);
    }

    private void viewActingUnit() {
        if (!Objects.equals(ce(), clientgui.getUnitDisplay().getCurrentEntity())
                && clientgui.getUnitDisplay().isVisible()) {
            Entity en_Target = clientgui.getUnitDisplay().getCurrentEntity();
            // Avoided using selectEntity(), to avoid centering on active unit
            clientgui.getUnitDisplay().displayEntity(ce());
            if (GUIP.getFireDisplayTabDuringFiringPhases()) {
                clientgui.getUnitDisplay().showPanel(MekPanelTabStrip.WEAPONS);
            }
            clientgui.getUnitDisplay().wPan.selectFirstWeapon();
            target(en_Target);
        }
    }

    protected boolean shouldPerformClearKeyCommand() {
        return !clientgui.isChatBoxActive() && !isIgnoringEvents() && isVisible();
    }

    /**
     * Register all of the <code>CommandAction</code>s for this panel display.
     */
    protected void registerKeyCommands() {
        MegaMekController controller = clientgui.controller;
        controller.registerCommandAction(KeyCommandBind.UNDO_LAST_STEP, this, this::removeLastFiring);
        controller.registerCommandAction(KeyCommandBind.TWIST_LEFT, this, this::twistLeft);
        controller.registerCommandAction(KeyCommandBind.TWIST_RIGHT, this, this::twistRight);
        controller.registerCommandAction(KeyCommandBind.FIRE, this::shouldPerformFireKeyCommand, this::fire);
        controller.registerCommandAction(KeyCommandBind.NEXT_WEAPON, this, this::nextWeapon);
        controller.registerCommandAction(KeyCommandBind.PREV_WEAPON, this, this::prevWeapon);

        controller.registerCommandAction(KeyCommandBind.NEXT_UNIT, this,
                () -> selectEntity(clientgui.getClient().getNextEntityNum(currentEntity)));
        controller.registerCommandAction(KeyCommandBind.PREV_UNIT, this,
                () -> selectEntity(clientgui.getClient().getPrevEntityNum(currentEntity)));

        controller.registerCommandAction(KeyCommandBind.NEXT_TARGET, this,
                () -> jumpToTarget(true, false, false));
        controller.registerCommandAction(KeyCommandBind.PREV_TARGET, this,
                () -> jumpToTarget(false, false, false));

        controller.registerCommandAction(KeyCommandBind.NEXT_TARGET_VALID, this,
                () -> jumpToTarget(true, true, false));
        controller.registerCommandAction(KeyCommandBind.PREV_TARGET_VALID, this,
                () -> jumpToTarget(false, true, false));

        controller.registerCommandAction(KeyCommandBind.NEXT_TARGET_NOALLIES, this,
                () -> jumpToTarget(true, false, true));
        controller.registerCommandAction(KeyCommandBind.PREV_TARGET_NOALLIES, this,
                () -> jumpToTarget(false, false, true));

        controller.registerCommandAction(KeyCommandBind.NEXT_TARGET_VALID_NO_ALLIES, this,
                () -> jumpToTarget(true, true, true));
        controller.registerCommandAction(KeyCommandBind.PREV_TARGET_VALID_NO_ALLIES, this,
                () -> jumpToTarget(false, true, true));

        controller.registerCommandAction(KeyCommandBind.VIEW_ACTING_UNIT, this, this::viewActingUnit);
        controller.registerCommandAction(KeyCommandBind.NEXT_MODE, this, () -> changeMode(true));
        controller.registerCommandAction(KeyCommandBind.PREV_MODE, this, () -> changeMode(false));
        controller.registerCommandAction(KeyCommandBind.CANCEL, this::shouldPerformClearKeyCommand, this::clear);
    }

    @Override
    protected ArrayList<MegaMekButton> getButtonList() {
        ArrayList<MegaMekButton> buttonList = new ArrayList<>();
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
        if (en != currentEntity) {
            target(null);
            clearAttacks();
            refreshAll();
        }

        if ((ce() != null) && ce().isWeapOrderChanged()) {
            clientgui.getClient().sendEntityWeaponOrderUpdate(ce());
        }

        if (game.getEntity(en) != null) {
            currentEntity = en;
            clientgui.setSelectedEntityNum(en);
            clientgui.getUnitDisplay().displayEntity(ce());

            // If the selected entity is not on the board, use the next one.
            // ASSUMPTION: there will always be *at least one* entity on map.
            if (ce().getPosition() == null) {

                // Walk through the list of entities for this player.
                for (int nextId = clientgui.getClient().getNextEntityNum(en); nextId != en; nextId = clientgui
                        .getClient().getNextEntityNum(nextId)) {

                    if (game.getEntity(nextId).getPosition() != null) {
                        currentEntity = nextId;
                        break;
                    }
                } // Check the player's next entity.

                // We were *supposed* to have found an on-board entity.
                if (ce().getPosition() == null) {
                    logger.error("Could not find an on-board entity " + en);
                    return;
                }
            }

            if (ce().isMakingVTOLGroundAttack()) {
                updateVTOLGroundTarget();
            } else {
                // Need to clear attacks again in case previous en was making VTOL ground attack
                clearAttacks();
                int lastTarget = ce().getLastTarget();
                if (ce() instanceof Mek) {
                    int grapple = ce().getGrappled();
                    if (grapple != Entity.NONE) {
                        lastTarget = grapple;
                    }
                }
                Entity t = game.getEntity(lastTarget);
                target(t);
            }

            clientgui.boardViews().forEach(IBoardView::clearMarkedHexes);
            if (clientgui.getBoardView(ce()) != null) {
                clientgui.getBoardView(ce()).highlight(ce().getPosition());
            }

            refreshAll();
            cacheVisibleTargets();

            if (!ce().isOffBoard()) {
                clientgui.centerOnUnit(ce());
            }

            // only twist if crew conscious
            setTwistEnabled(!ce().getAlreadyTwisted() && ce().canChangeSecondaryFacing() && ce().getCrew().isActive());

            setFindClubEnabled(FindClubAction.canMekFindClub(game, en));
            setFlipArmsEnabled(!ce().getAlreadyTwisted() && ce().canFlipArms());
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
                clientgui.getUnitDisplay().wPan.setToHit("Hidden units are only allowed to spot!");
            }
        } else {
            logger.error("Tried to select non-existent entity " + en);
        }

        clientgui.showFiringSolutions(ce());
    }

    /**
     * Does turn start stuff
     */
    protected void beginMyTurn() {
        target = null;

        if (!clientgui.isCurrentBoardViewShowingAnimation()) {
            clientgui.maybeShowUnitDisplay();
        }
        clientgui.clearFieldOfFire();
        clientgui.clearTemporarySprites();

        if (GUIP.getAutoSelectNextUnit()) {
            selectEntity(clientgui.getClient().getFirstEntityNum());
        }

        GameTurn turn = clientgui.getClient().getMyTurn();
        // There's special processing for triggering AP Pods.
        if ((turn instanceof TriggerAPPodTurn) && (ce() != null)) {
            disableButtons();
            TriggerAPPodDialog dialog = new TriggerAPPodDialog(clientgui.getFrame(), ce());
            dialog.setVisible(true);
            removeAllAttacks();
            Enumeration<TriggerAPPodAction> actions = dialog.getActions();
            while (actions.hasMoreElements()) {
                addAttack(actions.nextElement());
            }
            ready();
        } else if ((turn instanceof TriggerBPodTurn) && (null != ce())) {
            disableButtons();
            TriggerBPodDialog dialog = new TriggerBPodDialog(clientgui, ce(),
                    ((TriggerBPodTurn) turn).getAttackType());
            dialog.setVisible(true);
            removeAllAttacks();
            Enumeration<TriggerBPodAction> actions = dialog.getActions();
            while (actions.hasMoreElements()) {
                addAttack(actions.nextElement());
            }
            ready();
        } else {
            setNextEnabled(true);
            if (numButtonGroups > 1) {
                buttons.get(FiringCommand.FIRE_MORE).setEnabled(true);
            }
            setFireCalledEnabled(game.getOptions()
                    .booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_CALLED_SHOTS));
            clientgui.boardViews().forEach(bv -> bv.select(null));
            initDonePanelForNewTurn();
        }

        startTimer();
    }

    /**
     * Does end turn stuff.
     */
    protected void endMyTurn() {
        stopTimer();

        // end my turn, then.
        Entity next = game.getNextEntity(game.getTurnIndex());
        if (game.getPhase().isFiring() && (next != null) && (ce() != null)
                && (next.getOwnerId() != ce().getOwnerId())) {
            clientgui.maybeShowUnitDisplay();

        }
        currentEntity = Entity.NONE;
        target(null);
        clearMarkedHexes();
        clearMovementSprites();
        clientgui.onAllBoardViews(BoardView::clearStrafingCoords);
        clientgui.clearFieldOfFire();
        clientgui.clearTemporarySprites();
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
        butSkipTurn.setEnabled(false);
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
        Mounted<?> m = ce().getEquipment(wn);
        if ((m == null) || !m.hasModes()) {
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
        clientgui.getClient().sendModeChange(currentEntity, wn, nMode);

        // notify the player
        if (m.canInstantSwitch(nMode)) {
            clientgui.systemMessage(Messages.getString("FiringDisplay.switched", m.getName(),
                    m.curMode().getDisplayableName(true)));
        } else {
            clientgui.systemMessage(Messages.getString("FiringDisplay.willSwitch", m.getName(),
                    m.pendingMode().getDisplayableName(true)));
        }

        updateTarget();
        clientgui.getUnitDisplay().wPan.displayMek(ce());
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

        Mounted<?> m = ce().getEquipment(wn);
        if (m == null) {
            return;
        }

        // send change to the server
        m.getCalledShot().switchCalledShot();
        clientgui.getClient().sendCalledShotChange(currentEntity, wn);

        updateTarget();
        clientgui.getUnitDisplay().wPan.displayMek(ce());
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

        List<Entity> vec = game.getValidTargets(ce());
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
        // Default is one iteration, but may need to skip invalid or allies
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
            // If we've cycled through all visible targets without finding a valid one, stop
            // looping
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
                        game, ce().getId(), result,
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

        clientgui.centerOnUnit(targ);
        if (clientgui.getBoardView(targ) != null) {
            clientgui.getBoardView(targ).select(targ.getPosition());
        }
        // HACK : show the choice dialog again.
        showTargetChoice = true;
        target(targ);
    }

    private boolean checkNags() {
        if (needNagForNoAction()) {
            if (attacks.isEmpty()) {
                // confirm this action
                String title = Messages.getString("FiringDisplay.DontFireDialog.title");
                String body = Messages.getString("FiringDisplay.DontFireDialog.message");
                if (checkNagForNoAction(title, body)) {
                    return true;
                }
            }
        }

        if (needNagForOverheat()) {
            // We need to nag for overheat on capital fighters
            if ((ce() != null)
                    && ce().isCapitalFighter()) {
                int totalheat = 0;
                for (EntityAction action : attacks) {
                    if (action instanceof WeaponAttackAction) {
                        Mounted<?> weapon = ce().getEquipment(((WeaponAttackAction) action).getWeaponId());
                        totalheat += weapon.getCurrentHeat();
                    }
                }

                if (totalheat > ce().getHeatCapacity()) {
                    // confirm this action
                    String title = Messages.getString("FiringDisplay.OverheatNag.title");
                    String body = Messages.getString("FiringDisplay.OverheatNag.message");
                    if (checkNagForOverheat(title, body)) {
                        return true;
                    }
                }
            }
        }

        if (ce() == null) {
            return true;
        }

        return false;
    }

    @Override
    public void ready() {
        if (checkNags()) {
            return;
        }

        // stop further input (hopefully)
        disableButtons();

        // Handle some entity bookkeeping
        if (ce() != null) {
            // Add internal bombs used this phase to all internal bombs used this round
            if (ce().isBomber()) {
                if (phaseInternalBombs > 0) {
                    ((IBomber) ce()).increaseUsedInternalBombs(phaseInternalBombs);
                }

            }
        }

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
                Entity attacker = waa.getEntity(game);
                Targetable target1 = waa.getTarget(game);
                boolean curInFrontArc = ComputeArc.isInArc(attacker.getPosition(),
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
                    waa2.setAmmoMunitionType(waa.getAmmoMunitionType());
                    waa2.setAmmoCarrier(waa.getAmmoCarrier());
                    waa2.setBombPayloads(waa.getBombPayloads());
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
                Entity attacker = waa.getEntity(game);
                Targetable target1 = waa.getTarget(game);
                boolean curInFrontArc = ComputeArc.isInArc(attacker.getPosition(),
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
                    waa2.setAmmoMunitionType(waa.getAmmoMunitionType());
                    waa2.setAmmoCarrier(waa.getAmmoCarrier());
                    waa2.setBombPayloads(waa.getBombPayloads());
                    waa2.setStrafing(waa.isStrafing());
                    waa2.setStrafingFirstShot(waa.isStrafingFirstShot());
                    newAttacks.addElement(waa2);
                }
            }
        }

        // If the user picked a hex along the flight path, server needs to know
        if ((target instanceof Entity) && Compute.isGroundToAir(ce(), target)) {
            Coords targetPos = ((Entity) target).getPlayerPickedPassThrough(currentEntity);
            if (targetPos != null) {
                clientgui.getClient().sendPlayerPickedPassThrough(
                        ((Entity) target).getId(), currentEntity, targetPos);
            }
        }

        // send out attacks
        clientgui.getClient().sendAttackData(currentEntity, newAttacks);

        // clear queue
        removeAllAttacks();

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
            addAttack(uta);
            ready();
        }
    }

    /**
     * clear weapon jam
     */
    private void doClearWeaponJam() {
        ArrayList<Mounted<?>> weapons = ((Tank) ce()).getJammedWeapons();
        String[] names = new String[weapons.size()];
        for (int loop = 0; loop < names.length; loop++) {
            names[loop] = weapons.get(loop).getDesc();
        }
        String input = (String) JOptionPane.showInputDialog(clientgui.getFrame(),
                Messages.getString("FiringDisplay.ClearWeaponJam.question"),
                Messages.getString("FiringDisplay.ClearWeaponJam.title"),
                JOptionPane.QUESTION_MESSAGE, null, names, null);

        if (input != null) {
            for (int loop = 0; loop < names.length; loop++) {
                if (input.equals(names[loop])) {
                    RepairWeaponMalfunctionAction rwma = new RepairWeaponMalfunctionAction(
                            ce().getId(), ce().getEquipmentNum(weapons.get(loop)));
                    addAttack(rwma);
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

        String targetString = (target != null) ? String.format("\nTarget: %s", target.getDisplayName()) : "";

        String input = (String) JOptionPane.showInputDialog(clientgui.getFrame(),
                String.format("Pick a Special Pilot Ability to activate.%s", targetString),
                "Activate Special Pilot Ability",
                JOptionPane.QUESTION_MESSAGE, null, skillNames.keySet().toArray(), null);

        // unsafe, but since we're generating it right here, it should be fine.
        switch (skillNames.get(input)) {
            case OptionsConstants.GUNNERY_BLOOD_STALKER:
                // figure out when to clear Blood Stalker (when unit destroyed or flees or fly
                // off no return)
                ActivateBloodStalkerAction bloodStalkerAction = new ActivateBloodStalkerAction(ce().getId(),
                        target.getId());
                addAttack(0, bloodStalkerAction);
                ce().setBloodStalkerTarget(target.getId());
                break;
        }

        updateActivateSPA();
    }

    /**
     * Worker function that determines if we can activate the "blood stalker"
     * ability
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

        if (!SearchlightAttackAction.isPossible(game, currentEntity, target, null)) {
            return;
        }

        // create and queue a searchlight action
        SearchlightAttackAction saa = new SearchlightAttackAction(currentEntity,
                target.getTargetType(), target.getId());
        addAttack(saa);

        // and add it into the game, temporarily
        game.addAction(saa);
        clientgui.getBoardView(target).addAttack(saa);

        // refresh weapon panel, as bth will have changed
        updateTarget();
    }

    private void startStrafe() {
        target(null);
        clearAttacks();
        isStrafing = true;
        setStatusBarText(Messages.getString("FiringDisplay.Strafing.StatusLabel"));
        Entity strafingUnit = ce();
        if (strafingUnit != null) {
            clientgui.showBoardView(strafingUnit.getPassedThroughBoardId());
        }
        refreshAll();
    }

    private void updateStrafingTargets() {
        Entity strafingUnit = ce();
        if (strafingUnit == null) {
            return;
        }
        final int weaponId = clientgui.getUnitDisplay().wPan.getSelectedWeaponNum();
        final Mounted<?> m = strafingUnit.getEquipment(weaponId);
        StringBuilder toHitSummary = new StringBuilder();
        setFireEnabled(true);
        int strafedBoardId = strafingUnit.getPassedThroughBoardId();
        Board strafedBoard = game.getBoard(strafedBoardId);
        for (Coords c : strafingCoords) {
            for (Entity t : game.getEntitiesVector(c, strafedBoardId)) {
                if (t.isAirborne() || ((t instanceof Infantry) && t.isInBuilding())) {
                    // Airborne units cannot be strafed, neither infantry in a building
                    continue;
                }
                ToHitData toHit = WeaponAttackAction.toHit(game, currentEntity, t, weaponId,
                        Entity.LOC_NONE, AimingMode.NONE, true);
                toHitSummary.append("%s: %s<BR>".formatted(t.getShortName(), toHit.getDesc()));
                if (m.getType().hasFlag(WeaponType.F_AUTO_TARGET) || (toHit.getValue() == TargetRoll.IMPOSSIBLE)) {
                    setFireEnabled(false);
                }
            }

            Building bldg = strafedBoard.getBuildingAt(c);
            if (bldg != null) {
                Targetable t = new BuildingTarget(c, strafedBoard, false);
                ToHitData toHit = WeaponAttackAction.toHit(game, currentEntity, t, weaponId,
                        Entity.LOC_NONE, AimingMode.NONE, true);
                toHitSummary.append("%s: %s<BR>".formatted(t.getDisplayName(), toHit.getDesc()));
            }

            Targetable hexTarget = new HexTarget(c, strafedBoardId, HexTarget.TYPE_HEX_CLEAR);
            ToHitData toHit = WeaponAttackAction.toHit(game, currentEntity, hexTarget, weaponId,
                    Entity.LOC_NONE, AimingMode.NONE, true);
            if (m.getType().hasFlag(WeaponType.F_AUTO_TARGET) || (toHit.getValue() == TargetRoll.IMPOSSIBLE)) {
                setFireEnabled(false);
                if (toHitSummary.isEmpty()) {
                    toHitSummary.append(toHit.getDesc());
                }
            }
        }
        clientgui.getUnitDisplay().wPan.setToHit(toHitSummary.toString());
    }

    private HashMap<String, int[]> getBombPayloads(boolean isSpace, int limit) {
        HashMap<String, int[]> payloads = new HashMap<>();
        HashMap<String, int[]> loadouts = new HashMap<>();
        String[] titles = new String[] { "internal", "external" };
        for (String title : titles) {
            payloads.put(title, new int[BombType.B_NUM]);
        }

        // Have to return after map is filled in, not before
        if (!ce().isBomber()) {
            return payloads;
        }

        loadouts.put("internal", ce().getInternalBombLoadout());
        loadouts.put("external", ce().getExternalBombLoadout());

        for (String title : titles) {
            int[] loadout = loadouts.get(title);

            // this part is ugly, but we need to find any other bombing attacks by this
            // entity in the attack list and subtract those payloads from the relevant
            // loadout
            for (EntityAction o : attacks) {
                if (o instanceof WeaponAttackAction) {
                    WeaponAttackAction waa = (WeaponAttackAction) o;
                    if (waa.getEntityId() == ce().getId()) {
                        int[] priorLoad = waa.getBombPayloads().get(title);
                        for (int i = 0; i < priorLoad.length; i++) {
                            loadout[i] = loadout[i] - priorLoad[i];
                        }
                    }
                }
            }

            // Don't bother preparing a dialog for bombs that don't exist.
            if (Arrays.stream(loadout).sum() <= 0) {
                continue;
            }

            // Internal bay bombing is limited to 6 items per turn, but other limits may
            // also apply
            if ("internal".equals(title)) {
                int usedBombs = ((IBomber) ce()).getUsedInternalBombs();
                limit = (limit <= -1) ? 6 - usedBombs : Math.min(6 - usedBombs, limit);
            }

            int numFighters = ce().getActiveSubEntities().size();
            BombPayloadDialog bombsDialog = new BombPayloadDialog(
                    clientgui.getFrame(),
                    Messages.getString("FiringDisplay.BombNumberDialog" + ".title") + ", " + title,
                    loadout, isSpace, false, limit, numFighters);
            bombsDialog.setVisible(true);
            if (bombsDialog.getAnswer()) {
                int[] choices = bombsDialog.getChoices();
                for (int i = 0; i < choices.length; i++) {
                    payloads.get(title)[i] += choices[i];
                }
            }
        }
        return payloads;
    }

    /**
     * Adds a weapon attack with the currently selected weapon to the attack queue.
     */
    public void fire() {
        // get the selected weaponnum
        final int weaponNum = clientgui.getUnitDisplay().wPan.getSelectedWeaponNum();
        WeaponMounted mounted = (WeaponMounted) ce().getEquipment(weaponNum);

        // validate
        if ((ce() == null)
                || (target == null && (!isStrafing || strafingCoords.isEmpty()))
                || (mounted == null)) {
            logger.error("current fire parameters are invalid");
            return;
        }

        // check if we now shoot at a target in the front arc and previously
        // shot a target in side/rear arc that then was primary target
        // if so, ask and tell the user that to-hits will change
        if (!game.getOptions().booleanOption(OptionsConstants.ADVCOMBAT_NO_FORCED_PRIMARY_TARGETS)
                && (ce() instanceof Mek) || (ce() instanceof Tank)
                || (ce() instanceof ProtoMek)) {
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
                    boolean oldInFront = ComputeArc.isInArc(ce().getPosition(),
                            ce().getSecondaryFacing(), oldTarget, ce().getForwardArc());
                    boolean curInFront = ComputeArc.isInArc(ce().getPosition(),
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
        if (GUIP.getAutoDeclareSearchlight() && ce().isUsingSearchlight()) {
            doSearchlight();
        }

        ArrayList<Targetable> targets = new ArrayList<>();
        if (isStrafing) {
            for (Coords c : strafingCoords) {
                targets.add(new HexTarget(c, Targetable.TYPE_HEX_CLEAR));
                Building bldg = game.getBoard(ce()).getBuildingAt(c);
                if (bldg != null) {
                    targets.add(new BuildingTarget(c, game.getBoard(ce()), false));
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
                waa = new WeaponAttackAction(currentEntity, t.getTargetType(),
                        t.getId(), weaponNum);
            } else {
                waa = new ArtilleryAttackAction(currentEntity, t.getTargetType(),
                        t.getId(), weaponNum, game);
            }

            // check for a bomb payload dialog
            if (mounted.getType().hasFlag(WeaponType.F_SPACE_BOMB)) {
                waa.setBombPayloads(getBombPayloads(true, -1));
            } else if (mounted.getType().hasFlag(WeaponType.F_DIVE_BOMB)) {
                waa.setBombPayloads(getBombPayloads(false, -1));
            } else if (mounted.getType().hasFlag(WeaponType.F_ALT_BOMB)) {
                waa.setBombPayloads(getBombPayloads(false, 2));
            }

            if ((mounted.getLinked() != null)
                    && (mounted.getType().getAmmoType() != AmmoType.T_NA)
                    && (mounted.getLinked().getType() instanceof AmmoType)) {
                AmmoMounted ammoMount = mounted.getLinkedAmmo();
                AmmoType ammoType = ammoMount.getType();
                waa.setAmmoId(ammoMount.getEquipmentNum());
                EnumSet<AmmoType.Munitions> ammoMunitionType = ammoType.getMunitionType();
                waa.setAmmoMunitionType(ammoMunitionType);
                waa.setAmmoCarrier(ammoMount.getEntity().getId());
                if (((ammoMunitionType.contains(AmmoType.Munitions.M_THUNDER_VIBRABOMB)) &&
                        ((ammoType.getAmmoType() == AmmoType.T_LRM)
                                || (ammoType.getAmmoType() == AmmoType.T_LRM_IMP)
                                || (ammoType.getAmmoType() == AmmoType.T_MML)))
                        || (ammoType.getMunitionType().contains(AmmoType.Munitions.M_VIBRABOMB_IV))) {
                    VibrabombSettingDialog vsd = new VibrabombSettingDialog(
                            clientgui.getFrame());
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

            // Handle incrementing internal-bay weapons that are not used in bomb bay
            // attacks
            incrementInternalBombs(waa);

            // Temporarily add attack into the game. On turn done
            // this will be recomputed from the local
            // @attacks EntityAttackLog, but Game actions
            // must be populated to calculate ToHit mods etc.
            game.addAction(waa);

            // add the attack to our temporary queue
            addAttack(waa);
        }
        // set the weapon as used
        mounted.setUsedThisRound(true);

        // find the next available weapon
        int nextWeapon = clientgui.getUnitDisplay().wPan.getNextWeaponNum();

        // we fired a weapon, can't clear turret jams or weapon jams anymore
        updateClearTurret();
        updateClearWeaponJam();

        // check; if there are no ready weapons, you're done.
        if ((nextWeapon == -1) && GUIP.getAutoEndFiring()) {
            ready();
            return;
        }

        // otherwise, display firing info for the next weapon
        clientgui.getUnitDisplay().wPan.displayMek(ce());
        Mounted<?> nextMounted = ce().getEquipment(nextWeapon);
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
    public void nextWeapon() {
        if (ce() == null) {
            return;
        }
        clientgui.getUnitDisplay().wPan.selectNextWeapon();

        if (ce().getId() != clientgui.getUnitDisplay().wPan.getSelectedEntityId()) {
            clientgui.getUnitDisplay().wPan.displayMek(ce());
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
            clientgui.getUnitDisplay().wPan.displayMek(ce());
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

        removeAllAttacks();
        addAttack(new FindClubAction(currentEntity));

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
        addAttack(new SpotAction(currentEntity, target.getId()));
    }

    /**
     * Removes all current fire
     */
    protected void clearAttacks() {
        isStrafing = false;
        strafingCoords.clear();
        clientgui.onAllBoardViews(BoardView::clearStrafingCoords);

        // We may not have an entity selected
        if (ce() == null) {
            return;
        }

        // remove attacks, set weapons available again
        for (EntityAction o : attacks) {
            if (o instanceof WeaponAttackAction waa) {
                ce().getEquipment(waa.getWeaponId()).setUsedThisRound(false);
            }
        }
        removeAllAttacks();

        // remove temporary attacks from game & board
        removeTempAttacks();

        // restore any other movement to default
        ce().setSecondaryFacing(ce().getFacing());
        ce().setArmsFlipped(false);

        // restore count of internal bombs dropped this phase.
        if (ce().isBomber()) {
            phaseInternalBombs = ((IBomber) ce()).getUsedInternalBombs();
        }

        clientgui.getUnitDisplay().wPan.updateForEntity(ce());
    }

    /**
     * Removes temp attacks from the game and board
     */
    protected void removeTempAttacks() {
        // remove temporary attacks from game & board
        game.removeActionsFor(currentEntity);
        clientgui.onAllBoardViews(bv -> bv.removeAttacksFor(ce()));
    }

    /**
     * removes the last action
     */
    protected void removeLastFiring() {
        if (!attacks.isEmpty()) {
            EntityAction o = attacks.lastElement();
            if (o instanceof WeaponAttackAction waa) {
                ce().getEquipment(waa.getWeaponId()).setUsedThisRound(false);
                decrementInternalBombs(waa);
                removeAttack(o);
                clientgui.getUnitDisplay().wPan.displayMek(ce());
                game.removeAction(o);
                clientgui.onAllBoardViews(BoardView::refreshAttacks);
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
        clientgui.onAllBoardViews(bv -> bv.redrawEntity(ce()));
        clientgui.getUnitDisplay().displayEntity(ce());
        if (GUIP.getFireDisplayTabDuringFiringPhases()) {
            clientgui.getUnitDisplay().showPanel(MekPanelTabStrip.WEAPONS);
        }
        clientgui.getUnitDisplay().wPan.selectFirstWeapon();
        if (ce().isMakingVTOLGroundAttack()) {
            updateVTOLGroundTarget();
        }
        updateTarget();
        updateDonePanel();
        clientgui.updateFiringArc(ce());
    }

    /**
     * Make any necessary updates in the GUI after a new action has been added.
     */
    protected void updateForNewAction() {
        if (ce() == null) {
            return;
        }
        clientgui.onAllBoardViews(bv -> bv.redrawEntity(ce()));
        if (ce().isMakingVTOLGroundAttack()) {
            updateVTOLGroundTarget();
        }
        updateTarget();
        updateDonePanel();
    }

    /**
     * Targets something
     */
    public void target(Targetable t) {
        if (ce() == null) {
            return;
        }
        final int weaponId = clientgui.getUnitDisplay().wPan.getSelectedWeaponNum();
        Mounted<?> weapon = ce().getEquipment(weaponId);
        // Some weapons pick an automatic target
        if ((weapon != null) && weapon.getType().hasFlag(WeaponType.F_VGL)) {
            Targetable hexTarget = VehicularGrenadeLauncherWeapon.getTargetHex(weapon, weaponId);
            // Ignore events that will be generated by the select/cursor calls
            setIgnoringEvents(true);
            if (clientgui.getBoardView(hexTarget) != null) {
                clientgui.getBoardView(hexTarget).select(hexTarget.getPosition());
            }
            setIgnoringEvents(false);
            target = hexTarget;
        } else {
            target = t;
            if ((visibleTargets != null) && (target != null)) {
                // Set last target ID, so next/prev target behaves correctly
                for (int i = 0; i < visibleTargets.length; i++) {
                    if (visibleTargets[i].getId() == target.getId()) {
                        lastTargetID = i;
                        break;
                    }
                }
            }
        }
        if ((target instanceof Entity) && Compute.isGroundToAir(ce(), target)) {
            Coords targetPos = Compute.getClosestFlightPath(currentEntity, ce().getPosition(), (Entity) target);
            if (clientgui.getBoardView(ce()) != null) {
                clientgui.getBoardView(ce()).cursor(targetPos);
            }
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
        Entity attacker = ce();
        // allow spotting
        if ((attacker != null) && !attacker.isSpotting() && attacker.canSpot() && (target != null)
                && game.getOptions().booleanOption(OptionsConstants.BASE_INDIRECT_FIRE)) {
            boolean hasLos = LosEffects.calculateLOS(game, attacker, target).canSee();
            // In double blind, we need to "spot" the target as well as LoS
            if (hasLos
                    && game.getOptions().booleanOption(OptionsConstants.ADVANCED_DOUBLE_BLIND)
                    && !Compute.inVisualRange(game, attacker, target)
                    && !Compute.inSensorRange(game, attacker, target, null)) {
                hasLos = false;
            }
            setSpotEnabled(hasLos);
        } else {
            setSpotEnabled(false);
        }

        // update target panel

        final int weaponId = clientgui.getUnitDisplay().wPan.getSelectedWeaponNum();
        if (isStrafing && weaponId != -1) {
            clientgui.getUnitDisplay().wPan.setTarget(target, Messages
                    .getString("FiringDisplay.Strafing.TargetLabel"));

            updateStrafingTargets();
        } else if ((attacker != null) && attacker.equals(clientgui.getUnitDisplay().getCurrentEntity())
                && (target != null) && (target.getPosition() != null)
                && (weaponId != -1)) {
            ToHitData toHit;

            if (!ash.getAimingMode().isNone()) {
                WeaponMounted weapon = (WeaponMounted) attacker.getEquipment(weaponId);
                boolean aiming = ash.isAimingAtLocation() && ash.allowAimedShotWith(weapon);
                ash.setEnableAll(aiming);
                if (aiming) {
                    toHit = WeaponAttackAction.toHit(game, currentEntity, target,
                            weaponId, ash.getAimingAt(), ash.getAimingMode(),
                            false);
                    clientgui.getUnitDisplay().wPan.setTarget(target,
                            Messages.getFormattedString("MekDisplay.AimingAt", ash.getAimingLocation()));
                } else {
                    toHit = WeaponAttackAction.toHit(game, currentEntity, target, weaponId, Entity.LOC_NONE,
                            AimingMode.NONE, false);
                    clientgui.getUnitDisplay().wPan.setTarget(target, null);

                }
                ash.setPartialCover(toHit.getCover());
            } else {
                toHit = WeaponAttackAction.toHit(game, currentEntity, target, weaponId,
                        Entity.LOC_NONE, AimingMode.NONE, false);
                clientgui.getUnitDisplay().wPan.setTarget(target, null);
            }
            int effectiveDistance = Compute.effectiveDistance(game, attacker, target);
            clientgui.getUnitDisplay().wPan.wRangeR.setText("" + effectiveDistance);
            Mounted<?> m = attacker.getEquipment(weaponId);
            // If we have a Centurion Weapon System selected, we may need to
            // update ranges.
            if (m.getType().hasFlag(WeaponType.F_CWS)) {
                clientgui.getUnitDisplay().wPan.selectWeapon(weaponId);
            }
            if (m.isUsedThisRound()) {
                clientgui.getUnitDisplay().wPan.setToHit(Messages.getString("FiringDisplay.alreadyFired"));
                setFireEnabled(false);
            } else if ((m.getType().hasFlag(WeaponType.F_AUTO_TARGET)
                    && !m.curMode().equals(Weapon.MODE_AMS_MANUAL))
                    || (m.hasModes() && m.curMode().equals("Point Defense"))) {
                clientgui.getUnitDisplay().wPan.setToHit(Messages.getString("FiringDisplay.autoFiringWeapon"));
                setFireEnabled(false);
            } else if (m.isInBearingsOnlyMode()) {
                clientgui.getUnitDisplay().wPan.setToHit(Messages.getString("FiringDisplay.bearingsOnlyWrongPhase"));
                setFireEnabled(false);
            } else if (m.isInternalBomb() && phaseInternalBombs >= 6) {
                clientgui.getUnitDisplay().wPan
                        .setToHit(Messages.getString("WeaponAttackAction.AlreadyUsedMaxInternalBombs"));
                setFireEnabled(false);
            } else if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
                clientgui.getUnitDisplay().wPan.setToHit(toHit);
                setFireEnabled(false);
            } else if (toHit.getValue() == TargetRoll.AUTOMATIC_FAIL) {
                clientgui.getUnitDisplay().wPan.setToHit(toHit);
                setFireEnabled(true);
            } else {
                boolean natAptGunnery = attacker.hasAbility(OptionsConstants.PILOT_APTITUDE_GUNNERY);
                clientgui.getUnitDisplay().wPan.setToHit(toHit, natAptGunnery);

                setFireEnabled(true);
            }
            setSkipEnabled(true);
        } else {
            clientgui.getUnitDisplay().wPan.setTarget(null, null);
            clientgui.getUnitDisplay().wPan.wRangeR.setText("---");
            clientgui.getUnitDisplay().wPan.clearToHit();
        }

        if ((clientgui.getDisplayedUnit() != null) && (clientgui.getDisplayedUnit().equals(attacker))
                && !isStrafing && (weaponId != -1)) {
            adaptFireModeEnabled(attacker.getEquipment(weaponId));
        } else {
            setFireModeEnabled(false);
        }

        updateSearchlight();
        updateActivateSPA();
        updateClearWeaponJam();
        updateClearTurret();

        // Hidden units can only spot
        if ((attacker != null) && attacker.isHidden()) {
            setFireEnabled(false);
            setTwistEnabled(false);
            setFindClubEnabled(false);
            setFlipArmsEnabled(false);
            setStrafeEnabled(false);
            clientgui.getUnitDisplay().wPan.setToHit("Hidden units are only allowed to spot!");
        }
    }

    /**
     * A VTOL or LAM in airmek mode making a bombing or strafing attack already has the target set during the movement
     * phase.
     */
    void updateVTOLGroundTarget() {
        clientgui.onAllBoardViews(BoardView::clearStrafingCoords);
        target(null);
        isStrafing = false;
        strafingCoords.clear();
        Entity attacker = ce();
        if ((attacker instanceof IBomber bomber) && attacker.isBomber() && bomber.isVTOLBombing()) {
            // IBomber and isBomber are not equivalent; instanceof helps with pattern variable and null check
            target(bomber.getVTOLBombTarget());
            clientgui.getBoardView(ce()).addStrafingCoords(target.getPosition());
        } else if ((attacker instanceof VTOL vtol) && !vtol.getStrafingCoords().isEmpty()) {
            strafingCoords.addAll(vtol.getStrafingCoords());
            strafingCoords.forEach(c -> clientgui.getBoardView(vtol).addStrafingCoords(c));
            isStrafing = true;
        }
    }

    /**
     * Torso twist in the proper direction.
     */
    public void torsoTwist(Coords twistTarget) {
        if ((ce() == null) || ce().getAlreadyTwisted()) {
            return;
        }
        int direction = ce().getFacing();
        if (twistTarget != null) {
            direction = ce().clipSecondaryFacing(ce().getPosition().direction(twistTarget));
        }
        addTorsoTwistAction(direction);
    }

    /**
     * Adds a torso twist (a.k.a. secondary facing change) to the pending actions.
     * This first
     * clears out any existing attacks!
     *
     * @param twistDir 0 for twisting to the left, 1 to the right
     */
    public void torsoTwist(int twistDir) {
        if ((ce() == null) || ce().getAlreadyTwisted() || ((twistDir != 0) && (twistDir != 1))) {
            return;
        }
        int twistModifier = (twistDir == 0) ? 5 : 7;
        int direction = ce().clipSecondaryFacing((ce().getSecondaryFacing() + twistModifier) % 6);
        addTorsoTwistAction(direction);
    }

    private void addTorsoTwistAction(int direction) {
        if (direction != ce().getSecondaryFacing()) {
            clearAttacks();
            addAttack(new TorsoTwistAction(currentEntity, direction));
            ce().setSecondaryFacing(direction);
            clientgui.updateFiringArc(ce());
            updateForNewAction();
        }
    }

    //
    // BoardViewListener
    //
    @Override
    public void hexMoused(BoardViewEvent event) {
        if (isIgnoringEvents() || !isMyTurn() || (event.getButton() != MouseEvent.BUTTON1) ||
                  event.isCtrlHeld() || event.isAltHeld()) {
            // CTRL/ALT pressed means a line of sight check.
            return;
        }

        if (event.getType() == BoardViewEvent.BOARD_HEX_DRAGGED) {
            if ((ce() != null) && !ce().getAlreadyTwisted() && (event.isShiftHeld() || twisting)) {
                updateFlipArms(false);
                torsoTwist(event.getCoords());
            }
            event.getBoardView().cursor(event.getCoords());
        } else if (event.getType() == BoardViewEvent.BOARD_HEX_CLICKED) {
            twisting = false;
            if (!event.isShiftHeld()) {
                event.getBoardView().select(event.getCoords());
            }
        }
    }

    @Override
    public void hexSelected(BoardViewEvent event) {
        if (isIgnoringEvents()) {
            return;
        }

        Coords coords = event.getCoords();
        if (isMyTurn() && (coords != null) && (ce() != null)) {
            if (isStrafing) {
                if (ce().getPassedThroughBoardId() == event.getBoardId()) {
                    strafingCoords.clear();
                    strafingCoords.addAll(getStrafingCoords(coords));
                    event.getBoardView().setStrafingCoords(strafingCoords);
                    updateStrafingTargets();
                }
            } else if (!coords.equals(ce().getPosition())) {
                // HACK : sometimes we don't show the target choice window
                Targetable target = null;
                if (showTargetChoice) {
                    target = chooseTarget(coords, event.getBoardId());
                }
                if (event.isShiftHeld()) {
                    updateFlipArms(false);
                    torsoTwist(event.getCoords());
                } else if (target != null && !ce().isMakingVTOLGroundAttack()) {
                    if ((target instanceof Entity entTarg) && Compute.isGroundToAir(ce(), target)) {
                        boolean alreadyShotAt = false;
                        List<EntityAction> actions = game.getActionsVector();
                        for (EntityAction action : actions) {
                            if (action instanceof AttackAction aa) {
                                if ((action.getEntityId() == currentEntity) && (aa.getTargetId() == entTarg.getId())) {
                                    alreadyShotAt = true;
                                }
                            }
                        }
                        if (!alreadyShotAt) {
                            entTarg.setPlayerPickedPassThrough(currentEntity, coords);
                        }
                    }
                    target(target);
                }
            }
        }
    }

    //
    // GameListener
    //
    @Override
    public void gameTurnChange(GameTurnChangeEvent e) {
        if (isIgnoringEvents() || !game.getPhase().isFiring()) {
            return;
        }

        String s = getRemainingPlayerWithTurns();

        if (!game.getPhase().isSimultaneous(game)) {
            if (isMyTurn()) {
                setStatusBarText(Messages.getString("FiringDisplay.its_your_turn") + s);
            } else {
                String playerName;

                if (e.getPlayer() != null) {
                    playerName = e.getPlayer().getName();
                } else {
                    playerName = "Unknown";
                }

                setStatusBarText(Messages.getString("FiringDisplay.its_others_turn", playerName) + s);
            }
        } else {
            setStatusBarText(s);
        }

        // On simultaneous phases, each player ending their turn will generate a turn change
        // We want to ignore turns from other players and only listen to events we generated
        // Except on the first turn
        if (game.getPhase().isSimultaneous(game)
                && (e.getPreviousPlayerId() != clientgui.getClient().getLocalPlayerNumber())
                && (game.getTurnIndex() != 0)) {
            return;
        }

        if (isMyTurn()) {
            if (currentEntity == Entity.NONE) {
                beginMyTurn();
                clientgui.bingMyTurn();
            }
        } else {
            endMyTurn();
            clientgui.bingOthersTurn();
        }
    }

    @Override
    public void gamePhaseChange(GamePhaseChangeEvent e) {
        // In case of a /reset command, ensure the state gets reset
        if (game.getPhase().isLounge()) {
            endMyTurn();
        }

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (isMyTurn() && !game.getPhase().isFiring()) {
            endMyTurn();
        }
        // if we're ending the firing phase, unregister stuff.
        if (game.getPhase().isFiring()) {
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
        } else if (!isMyTurn()) {
            return;
        }

        if (ev.getActionCommand().equals(FiringCommand.FIRE_FIRE.getCmd())) {
            fire();
        } else if (ev.getActionCommand().equals(FiringCommand.FIRE_SKIP.getCmd())) {
            nextWeapon();
        } else if (ev.getActionCommand().equals(FiringCommand.FIRE_TWIST.getCmd())) {
            twisting = true;
        } else if (ev.getActionCommand().equals(FiringCommand.FIRE_NEXT.getCmd())) {
            selectEntity(clientgui.getClient().getNextEntityNum(currentEntity));
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
            startStrafe();
        } else if (ev.getActionCommand().equals(FiringCommand.FIRE_ACTIVATE_SPA.getCmd())) {
            doActivateSpecialAbility();
        }
    }

    /**
     * update for change of arms-flipping status
     *
     * @param armsFlipped
     */
    public void updateFlipArms(boolean armsFlipped) {
        if (ce() == null) {
            return;
        } else if (armsFlipped == ce().getArmsFlipped()) {
            return;
        } else if (ce().getAlreadyTwisted()) {
            return;
        }

        twisting = false;

        torsoTwist(null);

        clearAttacks();
        ce().setArmsFlipped(armsFlipped);
        addAttack(new FlipArmsAction(currentEntity, armsFlipped));
        updateTarget();
        refreshAll();
    }

    protected void updateSearchlight() {
        setSearchlightEnabled(
                (ce() != null)
                        && (target != null)
                        && (game.getBoard(ce()).contains(target.getPosition()))
                        && !ce().isHidden()
                        && ce().getCrew().isActive()
                        && ce().isUsingSearchlight()
                        && SearchlightAttackAction.isPossible(game, currentEntity, target,
                                null)
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
        Entity entity = ce();
        setStrafeEnabled((entity != null)
              && entity.isAero()
              && !entity.getPassedThrough().isEmpty()
              && game.hasBoard(entity.getPassedThroughBoardId())
              && game.getBoard(entity.getPassedThroughBoardId()).isGround()
              && !entity.isSpheroid()
              && (entity.getAltitude() <= 3)
              && (entity.getAltitude() > 0)
              && equippedForStrafing(entity));
    }

    private boolean equippedForStrafing(Entity entity) {
        return entity.getIndividualWeaponList().stream()
              .map(Mounted::getType)
              .filter(type -> type.hasFlag(WeaponType.F_ENERGY))
              .anyMatch(type -> type.getAmmoType() == AmmoType.F_NONE);
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
    protected void adaptFireModeEnabled(Mounted<?> m) {
        setFireModeEnabled(m.isModeSwitchable() && m.hasModes());
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
        if ((target instanceof Entity) && Compute.isGroundToAir(ce(), target)) {
            ((Entity) target).setPlayerPickedPassThrough(currentEntity, null);
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
        clearMarkedHexes();
        if (ce() != null) {
            clientgui.showBoardView(ce().getBoardId());
        }
        refreshAll();
    }

    // board view listener
    @Override
    public void finishedMovingUnits(BoardViewEvent b) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (isMyTurn() && (ce() != null)) {
            clientgui.maybeShowUnitDisplay();
            clientgui.centerOnUnit(ce());
        }
    }

    @Override
    public void unitSelected(BoardViewEvent event) {
        if (isIgnoringEvents()) {
            return;
        }

        Entity entity = game.getEntity(event.getEntityId());
        if (entity == null) {
            return;
        } else if (isMyTurn()) {
            if (clientgui.getClient().getMyTurn().isValidEntity(entity, game)) {
                selectEntity(entity.getId());
            }
        } else {
            clientgui.maybeShowUnitDisplay();
            clientgui.getUnitDisplay().displayEntity(entity);
            if (entity.isDeployed()) {
                clientgui.centerOnHex(entity.getBoardLocation());
            }
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }
        if ((event.getSource() == clientgui.getUnitDisplay().wPan.weaponList) && game.getPhase().isFiring()) {
            // If we aren't in the firing phase, there's no guarantee that cen is set properly, hence we can't update
            // update target data in weapon display
            updateTarget();
        }
    }

    @Override
    public void removeAllListeners() {
        super.removeAllListeners();
        clientgui.getUnitDisplay().wPan.weaponList.removeListSelectionListener(this);
    }

    /**
     * Have the player select a target from the entities at the given coords.
     *
     * @param pos - the <code>Coords</code> containing targets.
     */
    private @Nullable Targetable chooseTarget(Coords pos, int boardId) {
        boolean friendlyFire = game.getOptions().booleanOption(OptionsConstants.BASE_FRIENDLY_FIRE);
        // Assume that we have *no* choice.
        Targetable choice = null;

        int wn = clientgui.getUnitDisplay().wPan.getSelectedWeaponNum();
        Mounted<?> weapon = ce().getEquipment(wn);

        // Check for weapon/ammo types that should automatically target hexes
        if ((weapon != null) && (weapon.getLinked() != null)
                && (weapon.getLinked().getType() instanceof AmmoType)) {
            AmmoType aType = (AmmoType) weapon.getLinked().getType();
            EnumSet<AmmoType.Munitions> munitionType = aType.getMunitionType();
            // Mek mortar flares should default to deliver flare
            if ((aType.getAmmoType() == AmmoType.T_MEK_MORTAR)
                    && (munitionType.contains(AmmoType.Munitions.M_FLARE))) {
                return new HexTarget(pos, boardId, Targetable.TYPE_FLARE_DELIVER);
                // Certain mek mortar types and LRMs should target hexes
            } else if (((aType.getAmmoType() == AmmoType.T_MEK_MORTAR)
                    || (aType.getAmmoType() == AmmoType.T_LRM)
                    || (aType.getAmmoType() == AmmoType.T_LRM_IMP))
                    && ((munitionType.contains(AmmoType.Munitions.M_AIRBURST))
                            || (munitionType.contains(AmmoType.Munitions.M_SMOKE_WARHEAD)))) {
                return new HexTarget(pos, boardId, Targetable.TYPE_HEX_CLEAR);
            } else if (munitionType.contains(AmmoType.Munitions.M_MINE_CLEARANCE)) {
                return new HexTarget(pos, boardId, Targetable.TYPE_HEX_CLEAR);
            }
        }
        // Get the available choices, depending on friendly fire
        List<Entity> choices = new ArrayList<>(friendlyFire ?
                                                     game.getEntitiesVector(pos, boardId) :
                                                     game.getEnemyEntities(pos, boardId, ce()));
        // Safety first
        choices.removeIf(Objects::isNull);
        // Convert the choices into a List of targets.
        List<Targetable> targets = new ArrayList<>();
        final Player localPlayer = clientgui.getClient().getLocalPlayer();
        for (Entity possibleTarget : choices) {
            boolean isSensorReturn = possibleTarget.isSensorReturn(localPlayer);
            boolean isVisible = possibleTarget.hasSeenEntity(localPlayer);
            boolean isHidden = possibleTarget.isHidden();

            if (!ce().equals(possibleTarget) && !isSensorReturn && isVisible && !isHidden) {
                targets.add(possibleTarget);
            }
        }

        // If there aren't other targets, check for targets flying over pos
        if (targets.isEmpty()) {
            List<Entity> flyovers = ((BoardView) clientgui.boardViews.get(boardId)).getEntitiesFlyingOver(pos);
            for (Entity e : flyovers) {
                if (!targets.contains(e)) {
                    targets.add(e);
                }
            }
        }

        // Is there a building in the hex?
        Board board = game.getBoard(boardId);
        Building bldg = board.getBuildingAt(pos);
        if (bldg != null) {
            targets.add(new BuildingTarget(pos, board, false));
        }

        // If we clicked on a wooded hex with no other targets, clear woods
        if (targets.isEmpty()) {
            Hex hex = board.getHex(pos);
            if (hex.hasVegetation()) {
                targets.add(new HexTarget(pos, boardId, Targetable.TYPE_HEX_CLEAR));
            }
        }

        // Do we have a single choice?
        if (targets.size() == 1) {
            // Return that choice.
            choice = targets.get(0);
        } else if (targets.size() > 1) {
            // If we have multiple choices, display a selection dialog.
            choice = TargetChoiceDialog.showSingleChoiceDialog(clientgui.getFrame(),
                    "FiringDisplay.ChooseTargetDialog.title",
                    Messages.getString("FiringDisplay.ChooseTargetDialog.message", pos.getBoardNum()),
                    targets, clientgui, ce());
        }

        // Return the chosen unit.
        return choice;
    }

    public Targetable getTarget() {
        return target;
    }

    /**
     * Determines the five (or fewer in case the flight path is shorter than 5 hexes) hexes on the flight path of the
     * current entity centered around the given coord, if possible. In case of a flight path that crosses itself, the
     * player may select which of the hexes to use.
     *
     * @param center The middle hex of the strafing path
     * @return The up to five coords that make up a strafing path
     */
    private List<Coords> getStrafingCoords(Coords center) {
        Entity strafingAero = ce();

        if (!(strafingAero instanceof Aero)) {
            return Collections.emptyList();
        }

        // Can't update strafe hexes after weapons are fired, otherwise we'd
        // have to have a way to update the attacks vector
        if (!attacks.isEmpty()) {
            return Collections.emptyList();
        }

        // Can only strafe hexes that were flown over
        if (!strafingAero.passedThrough(center)) {
            return Collections.emptyList();
        }

        // Path could hit the same hex multiple times with aero on ground maps, find all such hexes
        List<Integer> centerCandidates = new ArrayList<>();
        List<Coords> flightPath = strafingAero.getPassedThrough();
        for (int index = 0; index < flightPath.size(); index++) {
            if (center.equals(flightPath.get(index))) {
                centerCandidates.add(index);
            }
        }

        int centerIndex;
        if (centerCandidates.isEmpty()) {
            // shouldn't happen here, but be safe
            return Collections.emptyList();

        } else if (centerCandidates.size() == 1) {
            centerIndex = centerCandidates.get(0);

        } else {
            // incomplete: choose one of the candidates
            centerIndex = (int) JOptionPane.showInputDialog(clientgui.getFrame(),
                  "Choose the hex to center the strafing path on",
                  "Strafing - Choose Hex", JOptionPane.QUESTION_MESSAGE, null,
                  centerCandidates.toArray(), centerCandidates.get(0));
        }

        // When the flight path is shorter than 5 hexes, only that many can be strafed (may happen for aeros that are
        // not on the ground board)
        int maxStrafingHexes = Math.min(flightPath.size(), 5);
        int startIndex = Math.max(centerIndex - 2, 0);
        startIndex = Math.min(flightPath.size() - 5, startIndex);
        startIndex = Math.max(startIndex, 0);
        List<Coords> strafingPath = new ArrayList<>();
        for (int index = startIndex; index < startIndex + maxStrafingHexes; index++) {
            if (flightPath.size() > index) {
                strafingPath.add(flightPath.get(index));
            }
        }
        return strafingPath;
    }

    private void incrementInternalBombs(WeaponAttackAction waa) {
        updateInternalBombs(waa, 1);
    }

    private void decrementInternalBombs(WeaponAttackAction waa) {
        updateInternalBombs(waa, -1);
    }

    private void updateInternalBombs(WeaponAttackAction waa, int amt) {
        if (ce().isBomber()) {
            if (ce().getEquipment(waa.getWeaponId()).isInternalBomb()) {
                int usedInternalBombs = ((IBomber) ce()).getUsedInternalBombs();
                phaseInternalBombs += amt;
                if (phaseInternalBombs < usedInternalBombs) {
                    phaseInternalBombs = usedInternalBombs;
                }
            }
        }
    }
}

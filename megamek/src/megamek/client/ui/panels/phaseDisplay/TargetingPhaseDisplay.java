/*
 * Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2002-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.ui.panels.phaseDisplay;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.*;
import javax.swing.JOptionPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import megamek.client.Client;
import megamek.client.event.BoardViewEvent;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.IBoardView;
import megamek.client.ui.dialogs.phaseDisplay.TargetChoiceDialog;
import megamek.client.ui.dialogs.phaseDisplay.TeleMissileSettingDialog;
import megamek.client.ui.dialogs.phaseDisplay.TriggerAPPodDialog;
import megamek.client.ui.dialogs.phaseDisplay.TriggerBPodDialog;
import megamek.client.ui.dialogs.phaseDisplay.VibrabombSettingDialog;
import megamek.client.ui.panels.phaseDisplay.FiringDisplay.FiringCommand;
import megamek.client.ui.util.KeyCommandBind;
import megamek.client.ui.util.MegaMekController;
import megamek.client.ui.widget.MegaMekButton;
import megamek.client.ui.widget.MekPanelTabStrip;
import megamek.common.HexTarget;
import megamek.common.Player;
import megamek.common.RangeType;
import megamek.common.ToHitData;
import megamek.common.actions.*;
import megamek.common.board.Board;
import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import megamek.common.board.CrossBoardAttackHelper;
import megamek.common.compute.Compute;
import megamek.common.enums.AimingMode;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GameTurnChangeEvent;
import megamek.common.game.GameTurn;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.turns.TriggerAPPodTurn;
import megamek.common.turns.TriggerBPodTurn;
import megamek.common.units.BuildingTarget;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.units.Tank;
import megamek.common.units.Targetable;
import megamek.common.weapons.Weapon;
import megamek.common.weapons.artillery.ArtilleryWeapon;
import megamek.common.weapons.bayWeapons.TeleOperatedMissileBayWeapon;
import megamek.common.weapons.capitalWeapons.CapitalMissileWeapon;
import megamek.logging.MMLogger;

/**
 * Targeting Phase Display. Breaks naming convention because TargetingDisplay is too easy to confuse with something
 * else
 */
public class TargetingPhaseDisplay extends AttackPhaseDisplay implements ListSelectionListener {
    private static final MMLogger logger = MMLogger.create(TargetingPhaseDisplay.class);

    @Serial
    private static final long serialVersionUID = 3441669419807288865L;

    /**
     * This enumeration lists all the possible ActionCommands that can be carried out during the deployment minefield
     * phase. Each command has a string for the command plus a flag that determines what unit type it is appropriate
     * for.
     *
     * @author arlith
     */
    public enum TargetingCommand implements PhaseCommand {
        FIRE_NEXT("fireNext"),
        FIRE_TWIST("fireTwist"),
        FIRE_FIRE("fireFire"),
        FIRE_SKIP("fireSkip"),
        FIRE_NEXT_TARG("fireNextTarg"),
        FIRE_MODE("fireMode"),
        FIRE_FLIP_ARMS("fireFlipArms"),
        FIRE_SEARCHLIGHT("fireSearchlight"),
        FIRE_CANCEL("fireCancel"),
        FIRE_DISENGAGE("fireDisengage"),
        FIRE_CLEAR_WEAPON("fireClearWeaponJam");

        final String cmd;

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

            String msg_left = Messages.getString("Left");
            String msg_right = Messages.getString("Right");
            String msg_next = Messages.getString("Next");
            String msg_previous = Messages.getString("Previous");
            String msg_valid = Messages.getString("TargetingPhaseDisplay.FireNextTarget.tooltip.Valid");
            String msgNoAllies = Messages.getString("TargetingPhaseDisplay.FireNextTarget.tooltip.NoAllies");

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
                    result += "&nbsp;&nbsp;" + msgNoAllies + " " + msg_next + ": "
                          + KeyCommandBind.getDesc(KeyCommandBind.NEXT_TARGET_NO_ALLIES);
                    result += "&nbsp;&nbsp;" + msg_previous + ": "
                          + KeyCommandBind.getDesc(KeyCommandBind.PREV_TARGET_NO_ALLIES);
                    result += "<BR>";
                    result += "&nbsp;&nbsp;" + msg_valid + " (" + msgNoAllies + ") " + msg_next + ": "
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
    protected Map<TargetingCommand, MegaMekButton> buttons;

    // let's keep track of what we're shooting and at what, too
    private Targetable target; // target

    // is the shift key held?
    private boolean shiftHeld;
    protected boolean twisting;

    private final GamePhase phase;

    private Entity[] visibleTargets;

    private int lastTargetID = -1;

    /**
     * Creates and lays out a new targeting phase display for the specified clientGUI.getClient().
     */
    public TargetingPhaseDisplay(final ClientGUI clientgui, boolean offboard) {
        super(clientgui);
        phase = offboard ? GamePhase.OFFBOARD : GamePhase.TARGETING;
        shiftHeld = false;

        setupStatusBar(Messages.getString("TargetingPhaseDisplay.waitingForTargetingPhase"));
        setButtons();
        setButtonsTooltips();
        setupButtonPanel();
        registerKeyCommands();
    }

    @Override
    protected String getDoneButtonLabel() {
        return Messages.getString("TargetingPhaseDisplay.Fire");
    }

    @Override
    protected String getSkipTurnButtonLabel() {
        return Messages.getString("TargetingPhaseDisplay.Skip");
    }

    @Override
    protected void setButtons() {
        buttons = new HashMap<>(
              (int) (TargetingCommand.values().length * 1.25 + 0.5));
        for (TargetingCommand cmd : TargetingCommand.values()) {
            buttons.put(cmd, createButton(cmd.getCmd(), "TargetingPhaseDisplay."));
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

    protected void twistLeft() {
        updateFlipArms(false);
        torsoTwist(0);
    }

    protected void twistRight() {
        updateFlipArms(false);
        torsoTwist(1);
    }

    private boolean shouldPerformFireKeyCommand() {
        return shouldReceiveKeyCommands() && buttons.get(TargetingCommand.FIRE_FIRE).isEnabled();
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
        controller.registerCommandAction(KeyCommandBind.CANCEL, this::shouldPerformClearKeyCommand, this::clear);
        controller.registerCommandAction(KeyCommandBind.NEXT_WEAPON, this, this::nextWeapon);
        controller.registerCommandAction(KeyCommandBind.PREV_WEAPON, this, this::prevWeapon);

        controller.registerCommandAction(KeyCommandBind.NEXT_UNIT, this,
              () -> selectEntity(clientgui.getClient().getNextEntityNum(currentEntity)));
        controller.registerCommandAction(KeyCommandBind.PREV_UNIT, this,
              () -> selectEntity(clientgui.getClient().getPrevEntityNum(currentEntity)));

        controller.registerCommandAction(KeyCommandBind.NEXT_TARGET, this, this::jumpToNextTarget);
        controller.registerCommandAction(KeyCommandBind.PREV_TARGET, this, this::jumpToPrevTarget);

        controller.registerCommandAction(KeyCommandBind.NEXT_MODE, this, () -> changeMode(true));
        controller.registerCommandAction(KeyCommandBind.PREV_MODE, this, () -> changeMode(false));
    }

    /**
     * Have the panel register itself as a listener wherever it's needed.
     * <p>
     * According to http://www-106.ibm.com/developerworks/java/library/j-jtp0618.html it is a major bad no-no to perform
     * these registrations before the constructor finishes, so this function has to be called after the panel is
     * created. Please note, this restriction only applies to listeners for objects that aren't on the panel itself.
     */
    public void initializeListeners() {
        game.addGameListener(this);
        clientgui.getUnitDisplay().wPan.weaponList.addListSelectionListener(this);
    }

    @Override
    protected ArrayList<MegaMekButton> getButtonList() {
        ArrayList<MegaMekButton> buttonList = new ArrayList<>();
        TargetingCommand[] commands = TargetingCommand.values();
        CommandComparator comparator = new CommandComparator();
        Arrays.sort(commands, comparator);
        for (TargetingCommand cmd : commands) {
            if (cmd == TargetingCommand.FIRE_CANCEL) {
                continue;
            }
            if ((cmd == TargetingCommand.FIRE_DISENGAGE) && ((currentEntity() == null)
                  || !currentEntity().isOffBoard())) {
                continue;
            }
            if (cmd == TargetingCommand.FIRE_CLEAR_WEAPON && !(currentEntity() instanceof Tank)) {
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
        if (en != currentEntity) {
            clearAttacks();
            refreshAll();
        }
        Client client = clientgui.getClient();
        Entity entity = game.getEntity(en);
        if ((entity != null) && entity.isWeaponOrderChanged()) {
            client.sendEntityWeaponOrderUpdate(entity);
        }

        if (entity != null) {
            currentEntity = en;
            clientgui.setSelectedEntityNum(en);

            // If the selected entity is not on the board, use the next one.
            // ASSUMPTION: there will always be *at least one* entity on map.
            if (null == currentEntity().getPosition()) {

                // Walk through the list of entities for this player.
                for (int nextId = client.getNextEntityNum(en); nextId != en; nextId = client.getNextEntityNum(nextId)) {
                    Entity nextEntity = game.getEntity(nextId);

                    if (nextEntity != null && null != nextEntity.getPosition()) {
                        currentEntity = nextId;
                        break;
                    }

                } // Check the player's next entity.

                // We were *supposed* to have found an on-board entity.
                if (null == currentEntity().getPosition()) {
                    logger.error("Could not find an on-board entity: {}", en);
                    return;
                }
            }

            target(null);
            clientgui.getBoardView(entity).clearMarkedHexes();
            clientgui.getBoardView(entity).highlight(entity.getPosition());

            refreshAll();
            cacheVisibleTargets();

            if (!clientgui.getBoardView(entity).isMovingUnits() && !entity.isOffBoard()) {
                clientgui.showBoardView(entity.getBoardId());
                clientgui.getBoardView(entity).centerOnHex(entity.getPosition());
            }

            setTwistEnabled(entity.canChangeSecondaryFacing() && entity.getCrew().isActive());
            setFlipArmsEnabled(entity.canFlipArms() && entity.getCrew().isActive());
            updateSearchlight();

            setFireModeEnabled(true);

            if (!entity.isOffBoard()) {
                clientgui.showFiringSolutions(entity);
            }

            updateClearWeaponJam();

        } else {
            logger.error("Tried to select non-existent entity: {}", en);
        }
    }

    /**
     * Does turn start stuff
     */
    private void beginMyTurn() {
        target = null;

        if (!clientgui.isCurrentBoardViewShowingAnimation()) {
            clientgui.maybeShowUnitDisplay();
        }
        clientgui.clearFieldOfFire();
        clientgui.clearTemporarySprites();

        if (GUIP.getAutoSelectNextUnit()) {
            selectEntity(clientgui.getClient().getFirstEntityNum());
        }
        setDisengageEnabled((currentEntity() != null)
              && attacks.isEmpty()
              && currentEntity().canFlee(currentEntity().getPosition()));

        GameTurn turn = clientgui.getClient().getMyTurn();
        // There's special processing for triggering AP Pods.
        if ((turn instanceof TriggerAPPodTurn) && (null != currentEntity())) {
            selectEntity(clientgui.getClient().getFirstEntityNum());
            disableButtons();
            TriggerAPPodDialog dialog = new TriggerAPPodDialog(clientgui.getFrame(), currentEntity());
            dialog.setVisible(true);
            removeAllAttacks();
            Enumeration<TriggerAPPodAction> actions = dialog.getActions();
            while (actions.hasMoreElements()) {
                addAttack(actions.nextElement());
            }
            ready();
        } else if ((turn instanceof TriggerBPodTurn) && (null != currentEntity())) {
            selectEntity(clientgui.getClient().getFirstEntityNum());
            disableButtons();
            TriggerBPodDialog dialog = new TriggerBPodDialog(clientgui, currentEntity(),
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
            butDone.setEnabled(true);
            clientgui.boardViews().forEach(boardView -> boardView.select(null));
            initDonePanelForNewTurn();
        }
        setupButtonPanel();

        startTimer();
    }

    /**
     * Does end turn stuff.
     */
    private void endMyTurn() {
        stopTimer();
        // end my turn, then.
        Entity next = game.getNextEntity(game.getTurnIndex());
        if ((phase == game.getPhase())
              && (null != next) && (null != currentEntity())
              && (next.getOwnerId() != currentEntity().getOwnerId())) {
            clientgui.maybeShowUnitDisplay();
        }
        currentEntity = Entity.NONE;
        target(null);

        clearMarkedHexes();
        clearMovementSprites();
        clientgui.clearFieldOfFire();
        clientgui.clearTemporarySprites();
        clientgui.setSelectedEntityNum(Entity.NONE);
        disableButtons();
    }

    /**
     * Disables all buttons in the interface
     */
    private void disableButtons() {
        setFireEnabled(false);
        setTwistEnabled(false);
        setSkipEnabled(false);
        setNextEnabled(false);
        butDone.setEnabled(false);
        setFlipArmsEnabled(false);
        setFireModeEnabled(false);
        setNextTargetEnabled(false);
        setDisengageEnabled(false);
        setFireClearWeaponJamEnabled(false);
    }

    /**
     * Fire Mode - Adds a Fire Mode Change to the current Attack Action
     */
    private void changeMode(boolean forward) {
        WeaponMounted weaponMounted = clientgui.getUnitDisplay().wPan.getSelectedWeapon();

        // Do nothing we have no unit selected or no weapon selected or if the weapon doesn't have modes
        if (currentEntity() == null || weaponMounted == null || !weaponMounted.hasModes()) {
            return;
        }

        // DropShip Artillery cannot be switched to "Direct" Fire
        final WeaponType weaponType = (WeaponType) weaponMounted.getType();
        if ((currentEntity() instanceof Dropship) && (weaponType instanceof ArtilleryWeapon)) {
            return;
        }

        // send change to the server
        int nMode = weaponMounted.switchMode(forward);
        clientgui.getClient().sendModeChange(weaponMounted.getEntity().getId(), weaponMounted.getEquipmentNum(), nMode);

        // notify the player
        if (weaponMounted.canInstantSwitch(nMode)) {
            clientgui.systemMessage(Messages.getString(
                  "FiringDisplay.switched", weaponMounted.getName(), weaponMounted.curMode().getDisplayableName()));
        } else {
            clientgui.systemMessage(Messages.getString(
                  "FiringDisplay.willSwitch",
                  weaponMounted.getName(),
                  weaponMounted.pendingMode().getDisplayableName()));
        }

        updateTarget();
        clientgui.getUnitDisplay().wPan.displayMek(currentEntity());
        clientgui.getUnitDisplay().wPan.selectWeapon(weaponMounted);
    }

    private boolean checkNags() {
        if (needNagForNoAction()) {
            if (attacks.isEmpty()) {
                // confirm this action
                String title = Messages.getString("TargetingPhaseDisplay.DontFireDialog.title");
                String body = Messages.getString("TargetingPhaseDisplay.DontFireDialog.message");
                if (checkNagForNoAction(title, body)) {
                    return true;
                }
            }
        }

        return currentEntity() == null;
    }

    @Override
    public void ready() {
        if (checkNags()) {
            return;
        }

        // stop further input (hopefully)
        disableButtons();

        // remove temporary attacks from game & board
        removeTempAttacks();

        // If the only action is a torso/turret twist, discard it as it would have no effect for the unit but
        // prevent twisting later
        if ((attacks.size() == 1) && (attacks.firstElement() instanceof TorsoTwistAction)) {
            attacks.clear();
        }

        // send out attacks
        clientgui.getClient().sendAttackData(currentEntity, attacks.toVector());

        // clear queue
        removeAllAttacks();

        if ((currentEntity() != null) && currentEntity().isWeaponOrderChanged()) {
            clientgui.getClient().sendEntityWeaponOrderUpdate(currentEntity());
        }
        endMyTurn();
    }

    private void doSearchlight() {
        if (!SearchlightAttackAction.isPossible(game, currentEntity, target, null)) {
            return;
        }

        // create and queue a searchlight action
        SearchlightAttackAction saa = new SearchlightAttackAction(currentEntity, target.getTargetType(),
              target.getId());
        addAttack(saa);

        // and add it into the game, temporarily
        game.addAction(saa);
        clientgui.getBoardView(game.getEntity(currentEntity)).addAttack(saa);

        // refresh weapon panel, as both will have changed
        updateTarget();
    }

    /**
     * Adds a weapon attack with the currently selected weapon to the attack queue.
     */
    private void fire() {
        // get the selected weapon num
        int weaponNum = clientgui.getUnitDisplay().wPan.getSelectedWeaponNum();
        Mounted<?> mounted = currentEntity().getEquipment(weaponNum);

        // validate
        if ((currentEntity() == null) || (target == null) || (mounted == null)
              || !(mounted.getType() instanceof WeaponType)) {
            throw new IllegalArgumentException("current fire parameters are invalid");
        }

        // declare searchlight, if possible
        if (GUIP.getAutoDeclareSearchlight()) {
            doSearchlight();
        }

        WeaponAttackAction waa = new WeaponAttackAction(currentEntity, target.getTargetType(),
              target.getId(), weaponNum);
        int distance = Compute.effectiveDistance(game, waa.getEntity(game), waa.getTarget(game));
        if ((mounted.getType().hasFlag(WeaponType.F_ARTILLERY))
              || (mounted.isInBearingsOnlyMode()
              && distance >= RangeType.RANGE_BEARINGS_ONLY_MINIMUM)
              || (CrossBoardAttackHelper.isOrbitToSurface(game, currentEntity(), target))
              || (mounted.getType() instanceof CapitalMissileWeapon
              && Compute.isGroundToGround(currentEntity(), target))) {
            waa = new ArtilleryAttackAction(currentEntity, target.getTargetType(),
                  target.getId(), weaponNum, game);
            // Get the launch velocity for bearings-only telemissiles
            if (mounted.getType() instanceof TeleOperatedMissileBayWeapon) {
                TeleMissileSettingDialog tsd = new TeleMissileSettingDialog(clientgui.getFrame(),
                      game);
                tsd.setVisible(true);
                waa.setLaunchVelocity(tsd.getSetting());
                waa.updateTurnsTilHit(game);
            }
        }

        updateClearWeaponJam();

        updateDisplayForPendingAttack(mounted, waa);
    }

    /**
     * Worker function that handles setting associated ammo and other bookkeeping/UI updates for a pending weapon attack
     * action.
     */
    public void updateDisplayForPendingAttack(Mounted<?> mounted, WeaponAttackAction waa) {
        // put this and the rest of the method into a separate function for access
        // externally.
        if ((null != mounted.getLinked())
              && (((WeaponType) mounted.getType()).getAmmoType() != AmmoType.AmmoTypeEnum.NA)) {
            Mounted<?> ammoMount = mounted.getLinked();
            waa.setAmmoId(ammoMount.getEntity().getEquipmentNum(ammoMount));
            EnumSet<AmmoType.Munitions> ammoMunitionType = ((AmmoType) ammoMount.getType()).getMunitionType();
            waa.setAmmoMunitionType(ammoMunitionType);
            waa.setAmmoCarrier(ammoMount.getEntity().getId());
            if (ammoMunitionType.contains(AmmoType.Munitions.M_VIBRABOMB_IV)) {
                VibrabombSettingDialog vsd = new VibrabombSettingDialog(clientgui.getFrame());
                vsd.setVisible(true);
                waa.setOtherAttackInfo(vsd.getSetting());
            }
        }

        // add the attack to our temporary queue
        addAttack(waa);

        // and add it into the game, temporarily
        game.addAction(waa);

        // set the weapon as used
        mounted.setUsedThisRound(true);

        // find the next available weapon
        int nextWeapon = clientgui.getUnitDisplay().wPan.selectNextWeapon();

        // check; if there are no ready weapons, you're done.
        if ((nextWeapon == -1) && GUIP.getAutoEndFiring()) {
            ready();
            return;
        }

        // otherwise, display firing info for the next weapon
        clientgui.getUnitDisplay().wPan.displayMek(currentEntity());
        clientgui.getUnitDisplay().wPan.selectWeapon(nextWeapon);
        updateTarget();
        setDisengageEnabled(false);
    }

    /**
     * Skips to the next weapon
     */
    private void nextWeapon() {
        if (currentEntity() == null) {
            return;
        }
        int weaponId = clientgui.getUnitDisplay().wPan.selectNextWeapon();
        if (currentEntity().getId() != clientgui.getUnitDisplay().wPan.getSelectedEntityId()) {
            clientgui.getUnitDisplay().wPan.displayMek(currentEntity());
        }

        if (weaponId == -1) {
            setFireModeEnabled(false);
        } else {
            Mounted<?> m = currentEntity().getEquipment(weaponId);
            setFireModeEnabled(m.isModeSwitchable());
        }
        updateTarget();
    }

    /**
     * Skips to the previous weapon
     */
    void prevWeapon() {
        if (currentEntity() == null) {
            return;
        }
        int weaponId = clientgui.getUnitDisplay().wPan.selectPrevWeapon();
        if (currentEntity().getId() != clientgui.getUnitDisplay().wPan.getSelectedEntityId()) {
            clientgui.getUnitDisplay().wPan.displayMek(currentEntity());
        }

        if (weaponId == -1) {
            setFireModeEnabled(false);
        } else {
            Mounted<?> m = currentEntity().getEquipment(weaponId);
            setFireModeEnabled(m.isModeSwitchable());
        }
        updateTarget();
    }

    /**
     * Removes all current fire
     */
    private void clearAttacks() {
        // We may not have an entity selected yet (race condition).
        if (currentEntity() == null) {
            return;
        }

        // remove attacks, set weapons available again
        for (EntityAction o : attacks) {
            if (o instanceof WeaponAttackAction waa) {
                currentEntity().getEquipment(waa.getWeaponId()).setUsedThisRound(false);
            }
        }
        removeAllAttacks();

        // remove temporary attacks from game & board
        removeTempAttacks();

        // restore any other movement to default
        currentEntity().setSecondaryFacing(currentEntity().getFacing());
        currentEntity().setArmsFlipped(false);
        setDisengageEnabled(currentEntity().isOffBoard() && currentEntity().canFlee(currentEntity().getPosition()));
    }

    /**
     * Removes temp attacks from the game and board
     */
    private void removeTempAttacks() {
        // remove temporary attacks from game & board
        game.removeActionsFor(currentEntity);
        clientgui.boardViews().forEach(bv -> ((BoardView) bv).removeAttacksFor(currentEntity()));
    }

    /**
     * removes the last action
     */
    private void removeLastFiring() {
        if (!attacks.isEmpty()) {
            EntityAction o = attacks.lastElement();
            if (o instanceof WeaponAttackAction waa) {
                currentEntity().getEquipment(waa.getWeaponId()).setUsedThisRound(false);
                removeAttack(o);
                setDisengageEnabled(attacks.isEmpty() && currentEntity().isOffBoard() && currentEntity().canFlee(
                      currentEntity().getPosition()));
                clientgui.getUnitDisplay().wPan.displayMek(currentEntity());
                game.removeAction(o);
                clientgui.boardViews().forEach(bv -> ((BoardView) bv).refreshAttacks());
            }
        }
    }

    /**
     * Refreshes all displays.
     */
    private void refreshAll() {
        if (currentEntity() == null) {
            return;
        }
        clientgui.boardViews().forEach(bv -> ((BoardView) bv).redrawEntity(currentEntity()));
        clientgui.getUnitDisplay().displayEntity(currentEntity());
        if (GUIP.getFireDisplayTabDuringFiringPhases()) {
            clientgui.getUnitDisplay().showPanel(MekPanelTabStrip.WEAPONS);
        }
        clientgui.getUnitDisplay().wPan.selectFirstWeapon();
        updateTarget();
        clientgui.updateFiringArc(currentEntity());
    }

    /**
     * Targets something
     */
    public void target(Targetable t) {
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
        final WeaponMounted selectedWeapon = clientgui.getUnitDisplay().wPan.getSelectedWeapon();
        if (selectedWeapon != null) {
            Entity weaponEntity = selectedWeapon.getEntity();
            Entity attacker = weaponEntity.getAttackingEntity();
            if ((attacker != null) && attacker.equals(clientgui.getUnitDisplay().getCurrentEntity())
                  && (target != null) && (weaponId != -1) && (attacker.getPosition() != null)) {
                clientgui.getUnitDisplay().wPan.setTarget(target, null);

                Mounted<?> weapon = attacker.getEquipment(weaponId);
                int effectiveDistance = Compute.effectiveDistance(game, attacker, target);
                String distanceText = Integer.toString(effectiveDistance);
                if (!game.onConnectedBoards(attacker, target)) {
                    distanceText = "Unreachable";
                } else if (showDistanceAsMapSheets(attacker, target, weapon)) {
                    distanceText = effectiveDistance / Board.DEFAULT_BOARD_HEIGHT + " Map sheets";
                    if (isArtilleryAttack(weapon)) {
                        ArtilleryAttackAction aaa = new ArtilleryAttackAction(attacker.getId(), target.getTargetType(),
                              target.getId(), weaponId, game);
                        distanceText += String.format(" (%d turns)", aaa.getTurnsTilHit());
                    }
                }
                clientgui.getUnitDisplay().wPan.wRangeR.setText(distanceText);

                ToHitData toHit = WeaponAttackAction.toHit(game,
                      currentEntity, target, weaponId, Entity.LOC_NONE, AimingMode.NONE, false);

                if (weapon.isUsedThisRound()) {
                    clientgui.getUnitDisplay().wPan.setToHit(
                          Messages.getString("TargetingPhaseDisplay.alreadyFired"));
                    setFireEnabled(false);
                } else if (weapon.isInBearingsOnlyMode() && effectiveDistance < RangeType.RANGE_BEARINGS_ONLY_MINIMUM) {
                    clientgui.getUnitDisplay().wPan.setToHit(
                          Messages.getString("TargetingPhaseDisplay.bearingsOnlyMinRange"));
                    setFireEnabled(false);
                } else if ((weapon.getType().hasFlag(WeaponType.F_AUTO_TARGET)
                      && !weapon.curMode().equals(Weapon.MODE_AMS_MANUAL))) {
                    clientgui.getUnitDisplay().wPan.setToHit(
                          Messages.getString("TargetingPhaseDisplay.autoFiringWeapon"));
                    setFireEnabled(false);
                } else if (toHit.getValue() == TargetRoll.IMPOSSIBLE) {
                    clientgui.getUnitDisplay().wPan.setToHit(toHit);
                    setFireEnabled(false);
                } else if (toHit.getValue() == TargetRoll.AUTOMATIC_FAIL) {
                    clientgui.getUnitDisplay().wPan.setToHit(toHit);
                    setFireEnabled(true);
                } else {
                    clientgui.getUnitDisplay().wPan.setToHit(toHit,
                          attacker.hasAbility(OptionsConstants.PILOT_APTITUDE_GUNNERY));
                    setFireEnabled(true);
                }
            }
            setSkipEnabled(true);
        } else {
            clientgui.getUnitDisplay().wPan.setTarget(null, null);
            clientgui.getUnitDisplay().wPan.wRangeR.setText("---");
            clientgui.getUnitDisplay().wPan.clearToHit();
        }
        updateSearchlight();
    }

    private boolean showDistanceAsMapSheets(Entity attacker, Targetable target, Mounted<?> weapon) {
        return (!game.onTheSameBoard(attacker, target) && game.isOnGroundMap(attacker) && game.isOnGroundMap(target))
              || attacker.isOffBoard() || isArtilleryAttack(weapon);
    }

    private boolean isArtilleryAttack(Mounted<?> weapon) {
        return (weapon instanceof WeaponMounted && weapon.getType().hasFlag(WeaponType.F_ARTILLERY));
    }

    /**
     * Cache the list of visible targets. This is used for the 'next target' button.
     * <p>
     * We'll sort it by range to us.
     */
    private void cacheVisibleTargets() {
        clearVisibleTargets();

        List<Entity> vec = game.getValidTargets(currentEntity());
        Comparator<Entity> sortComp = (x, y) -> {
            if (x.getId() == y.getId()) {
                return 0;
            }

            int rangeToX = currentEntity().getPosition().distance(x.getPosition());
            int rangeToY = currentEntity().getPosition().distance(y.getPosition());
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
        if (null == visibleTargets || visibleTargets.length == 0) {
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

        clientgui.getBoardView(targ).centerOnHex(targ.getPosition());
        clientgui.getBoardView(targ).select(targ.getPosition());

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

        clientgui.getBoardView(targ).centerOnHex(targ.getPosition());
        clientgui.getBoardView(targ).select(targ.getPosition());

        target(targ);
    }

    /**
     * Torso twist in the proper direction.
     */
    void torsoTwist(Coords twistTarget) {
        int direction = currentEntity().getFacing();

        if (twistTarget != null) {
            direction = currentEntity().clipSecondaryFacing(currentEntity().getPosition().direction(twistTarget));
        }

        if (direction != currentEntity().getSecondaryFacing()) {
            clearAttacks();
            addAttack(new TorsoTwistAction(currentEntity, direction));
            currentEntity().setSecondaryFacing(direction);
            clientgui.updateFiringArc(currentEntity());
            refreshAll();
        }
    }

    /**
     * Torso twist to the left or right
     *
     * @param twistDir An <code>int</code> specifying whether we're twisting left or right, 0 if we're twisting to the
     *                 left, 1 if to the right.
     */

    void torsoTwist(int twistDir) {
        int direction = currentEntity().getSecondaryFacing();
        if (twistDir == 0) {
            clearAttacks();
            direction = currentEntity().clipSecondaryFacing((direction + 5) % 6);
            addAttack(new TorsoTwistAction(currentEntity, direction));
            currentEntity().setSecondaryFacing(direction);
            refreshAll();
        } else if (twistDir == 1) {
            clearAttacks();
            direction = currentEntity().clipSecondaryFacing((direction + 7) % 6);
            addAttack(new TorsoTwistAction(currentEntity, direction));
            currentEntity().setSecondaryFacing(direction);
            refreshAll();
        }
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
        if (shiftHeld == ((b.getModifiers() & InputEvent.SHIFT_DOWN_MASK) == 0)) {
            shiftHeld = (b.getModifiers() & InputEvent.SHIFT_DOWN_MASK) != 0;
        }

        if (b.getType() == BoardViewEvent.BOARD_HEX_DRAGGED) {
            if (shiftHeld || twisting) {
                if ((currentEntity() != null) && !currentEntity().getAlreadyTwisted()) {
                    updateFlipArms(false);
                    torsoTwist(b.getCoords());
                }
            }
            b.getBoardView().cursor(b.getCoords());
        } else if (b.getType() == BoardViewEvent.BOARD_HEX_CLICKED) {
            twisting = false;
            if (!shiftHeld) {
                b.getBoardView().select(b.getCoords());
            }
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
              && (currentEntity() != null) && !b.getCoords().equals(currentEntity().getPosition())) {
            if (shiftHeld && !currentEntity().getAlreadyTwisted()) {
                updateFlipArms(false);
                torsoTwist(b.getCoords());
            } else if (phase.isTargeting()) {
                target(new HexTarget(b.getCoords(), b.getBoardView().getBoardId(), Targetable.TYPE_HEX_ARTILLERY));
            } else {
                target(chooseTarget(b.getBoardLocation()));
            }
        }
    }

    /**
     * Have the player select a target from the entities at the given coords.
     *
     * @param location - the <code>location</code> containing targets.
     */
    private Targetable chooseTarget(BoardLocation location) {
        boolean friendlyFire = game.getOptions().booleanOption(OptionsConstants.BASE_FRIENDLY_FIRE);
        // Assume that we have *no* choice.
        Targetable choice = null;
        Iterator<Entity> choices;

        // Get the available choices, depending on friendly fire
        if (friendlyFire) {
            choices = game.getEntities(location.coords());
        } else {
            choices = game.getEnemyEntities(location.coords(), currentEntity());
        }

        // Convert the choices into a List of targets.
        List<Targetable> targets = new ArrayList<>();
        final Player localPlayer = clientgui.getClient().getLocalPlayer();
        while (choices.hasNext()) {
            Entity targetedEntity = choices.next();
            boolean isSensorReturn = false;
            boolean isVisible = true;
            if (targetedEntity != null) {
                isSensorReturn = targetedEntity.isSensorReturn(localPlayer);
                isVisible = targetedEntity.hasSeenEntity(localPlayer);
            }
            if (!currentEntity().equals(targetedEntity) && !isSensorReturn && isVisible) {
                targets.add(targetedEntity);
            }
        }

        // Is there a building in the hex?
        IBuilding bldg = game.getBoard()
              .getBuildingAt(location.coords());
        if (bldg != null) {
            targets.add(new BuildingTarget(location.coords(), game
                  .getBoard(), Targetable.TYPE_BLDG_TAG));
        }

        targets.add(new HexTarget(location.coords(), location.boardId(), Targetable.TYPE_HEX_TAG));

        // Do we have a single choice?
        if (targets.size() == 1) {
            // Return that choice.
            choice = targets.get(0);
        } else if (targets.size() > 1) {
            // If we have multiple choices, display a selection dialog.
            choice = TargetChoiceDialog.showSingleChoiceDialog(clientgui.getFrame(),
                  "FiringDisplay.ChooseTargetDialog.title",
                  Messages.getString("FiringDisplay.ChooseTargetDialog.message", location.getBoardNum()),
                  targets, clientgui, currentEntity());
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
        if (game.getPhase().isLounge()) {
            endMyTurn();
        }
        // On simultaneous phases, each player ending their turn will generate a turn
        // change
        // We want to ignore turns from other players and only listen to events we
        // generated
        // Except on the first turn
        if (game.getPhase().isSimultaneous(game)
              && (e.getPreviousPlayerId() != clientgui.getClient().getLocalPlayerNumber())
              && (game.getTurnIndex() != 0)) {
            return;
        }

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        String s = getRemainingPlayerWithTurns();

        if (game.getPhase() == phase) {
            if (clientgui.getClient().isMyTurn()) {
                if (currentEntity == Entity.NONE) {
                    beginMyTurn();
                }
                String t = (phase.isTargeting()) ? Messages.getString("TargetingPhaseDisplay.its_your_turn")
                      : Messages.getString("TargetingPhaseDisplay.its_your_tag_turn");
                setStatusBarText(t + s);
                clientgui.bingOthersTurn();
            } else {
                endMyTurn();
                if (e.getPlayer() != null) {
                    setStatusBarText(Messages.getString("TargetingPhaseDisplay.its_others_turn",
                          e.getPlayer().getName()) + s);
                    clientgui.bingOthersTurn();
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
              && (game.getPhase() != phase)) {
            endMyTurn();
        }
        // if we're ending the firing phase, unregister stuff.
        if (game.getPhase() == phase) {
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
        } else if (ev.getActionCommand().equals(FiringCommand.FIRE_TWIST.getCmd())) {
            twisting = true;
        } else if (ev.getActionCommand().equals(TargetingCommand.FIRE_SKIP.getCmd())) {
            nextWeapon();
        } else if (ev.getActionCommand().equals(TargetingCommand.FIRE_NEXT.getCmd())) {
            selectEntity(clientgui.getClient().getNextEntityNum(currentEntity));
        } else if (ev.getActionCommand().equals(TargetingCommand.FIRE_NEXT_TARG.getCmd())) {
            jumpToNextTarget();
        } else if (ev.getActionCommand().equals(TargetingCommand.FIRE_FLIP_ARMS.getCmd())) {
            updateFlipArms(!currentEntity().getArmsFlipped());
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
            addAttack(new DisengageAction(currentEntity));
            ready();
        } else if (ev.getActionCommand().equals(TargetingCommand.FIRE_CLEAR_WEAPON.getCmd())) {
            doClearWeaponJam();
        }
    }

    /**
     * clear weapon jam
     */
    protected void doClearWeaponJam() {
        ArrayList<Mounted<?>> weapons = ((Tank) currentEntity()).getJammedWeapons();
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
                    RepairWeaponMalfunctionAction repairWeaponMalfunctionAction = new RepairWeaponMalfunctionAction(
                          currentEntity().getId(), currentEntity().getEquipmentNum(weapons.get(loop)));
                    addAttack(repairWeaponMalfunctionAction);
                    ready();
                }
            }
        }
    }

    private void updateFlipArms(boolean armsFlipped) {
        if (armsFlipped == currentEntity().getArmsFlipped()) {
            return;
        }

        twisting = false;

        torsoTwist(null);

        clearAttacks();
        currentEntity().setArmsFlipped(armsFlipped);
        addAttack(new FlipArmsAction(currentEntity, armsFlipped));
        updateTarget();
        refreshAll();
    }

    private void updateSearchlight() {
        setSearchlightEnabled((currentEntity() != null)
              && (target != null)
              && currentEntity().isUsingSearchlight()
              && currentEntity().getCrew().isActive()
              && SearchlightAttackAction.isPossible(game, currentEntity, target, null));
    }

    private void updateClearWeaponJam() {
        setFireClearWeaponJamEnabled((currentEntity() instanceof Tank) && ((Tank) currentEntity()).canUnjamWeapon()
              && attacks.isEmpty());
    }

    private void setFireEnabled(boolean enabled) {
        buttons.get(TargetingCommand.FIRE_FIRE).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(FiringCommand.FIRE_FIRE.getCmd(), enabled);
    }

    protected void setTwistEnabled(boolean enabled) {
        buttons.get(TargetingCommand.FIRE_TWIST).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(FiringCommand.FIRE_TWIST.getCmd(), enabled);
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

    private void setFireClearWeaponJamEnabled(boolean enabled) {
        if (buttons.containsKey(TargetingCommand.FIRE_CLEAR_WEAPON)) {
            buttons.get(TargetingCommand.FIRE_CLEAR_WEAPON).setEnabled(enabled);
        }
    }

    @Override
    public void clear() {
        clearAttacks();
        clientgui.boardViews().forEach(IBoardView::clearMarkedHexes);
        refreshAll();
    }

    // board view listener
    @Override
    public void finishedMovingUnits(BoardViewEvent b) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        if (clientgui.getClient().isMyTurn() && (currentEntity() != null)) {
            clientgui.maybeShowUnitDisplay();
            IBoardView bv = clientgui.getBoardView(currentEntity());
            if (bv != null) {
                bv.centerOnHex(currentEntity().getPosition());
            }
        }
    }

    @Override
    public void unitSelected(BoardViewEvent b) {
        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        Entity entity = game.getEntity(b.getEntityId());
        if (entity != null) {
            if (clientgui.getClient().isMyTurn()) {
                if (clientgui.getClient().getMyTurn().isValidEntity(entity, game)) {
                    selectEntity(entity.getId());
                }
            } else {
                clientgui.maybeShowUnitDisplay();
                clientgui.getUnitDisplay().displayEntity(entity);
                if (entity.isDeployed()) {
                    clientgui.getBoardView(entity).centerOnHex(entity.getPosition());
                }
            }
        }
    }

    @Override
    public void removeAllListeners() {
        super.removeAllListeners();
        clientgui.getUnitDisplay().wPan.weaponList.removeListSelectionListener(this);
    }

    @Override
    public void valueChanged(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }

        if ((game.getPhase().isTargeting()) &&
              (event.getSource().equals(clientgui.getUnitDisplay().wPan.weaponList))) {
            // update target data in weapon display
            updateTarget();
        }
    }
}

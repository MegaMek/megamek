/*
 * Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
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
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.swing.JOptionPane;

import megamek.client.event.BoardViewEvent;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.IBoardView;
import megamek.client.ui.dialogs.phaseDisplay.AimedShotDialog;
import megamek.client.ui.dialogs.phaseDisplay.TargetChoiceDialog;
import megamek.client.ui.util.KeyCommandBind;
import megamek.client.ui.util.MegaMekController;
import megamek.client.ui.widget.IndexedRadioButton;
import megamek.client.ui.widget.MegaMekButton;
import megamek.client.ui.widget.MekPanelTabStrip;
import megamek.common.ToHitData;
import megamek.common.actions.*;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.compute.ComputeArc;
import megamek.common.enums.AimingMode;
import megamek.common.equipment.INarcPod;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.enums.MiscTypeFlag;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GameTurnChangeEvent;
import megamek.common.game.GameTurn;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.turns.CounterGrappleTurn;
import megamek.common.units.BipedMek;
import megamek.common.units.BuildingTarget;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import megamek.common.units.QuadMek;
import megamek.common.units.Targetable;
import megamek.logging.MMLogger;

public class PhysicalDisplay extends AttackPhaseDisplay {
    private static final MMLogger logger = MMLogger.create(PhysicalDisplay.class);

    @Serial
    private static final long serialVersionUID = -3274750006768636001L;

    // HACK : track when we want to show the target choice dialog.
    protected boolean showTargetChoice = true;
    protected Entity[] visibleTargets = null;
    protected int lastTargetID = -1;
    protected boolean isStrafing = false;

    /**
     * This enumeration lists all the possible ActionCommands that can be carried out during the physical phase. Each
     * command has a string for the command plus a flag that determines what unit type it is appropriate for.
     *
     * @author arlith
     */
    public enum PhysicalCommand implements PhaseCommand {
        PHYSICAL_NEXT("next"),
        PHYSICAL_PUNCH("punch"),
        PHYSICAL_KICK("kick"),
        PHYSICAL_CLUB("club"),
        PHYSICAL_BRUSH_OFF("brushOff"),
        PHYSICAL_THRASH("thrash"),
        PHYSICAL_DODGE("dodge"),
        PHYSICAL_PUSH("push"),
        PHYSICAL_TRIP("trip"),
        PHYSICAL_GRAPPLE("grapple"),
        PHYSICAL_JUMP_JET("jumpjet"),
        PHYSICAL_PROTO("protoPhysical"),
        PHYSICAL_SEARCHLIGHT("fireSearchlight"),
        PHYSICAL_EXPLOSIVES("explosives"),
        PHYSICAL_VIBRO("vibro"),
        PHYSICAL_PHEROMONE("pheromone"),
        PHYSICAL_TOXIN("toxin"),
        PHYSICAL_MORE("more");

        final String cmd;

        /**
         * Priority that determines this buttons order
         */
        private int priority;

        PhysicalCommand(String c) {
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
            return Messages.getString("PhysicalDisplay." + getCmd());
        }

        public String getHotKeyDesc() {
            String result = "";

            switch (this) {
                case PHYSICAL_NEXT:
                    result = "<BR>";
                    result += "&nbsp;&nbsp;" + "Next" + ": " + KeyCommandBind.getDesc(KeyCommandBind.NEXT_UNIT);
                    result += "&nbsp;&nbsp;" + "Previous" + ": " + KeyCommandBind.getDesc(KeyCommandBind.PREV_UNIT);
                    break;
                case PHYSICAL_PUNCH:
                    result = "<BR>";
                    result += "&nbsp;&nbsp;" + KeyCommandBind.getDesc(KeyCommandBind.PHYS_PUNCH);
                    break;
                case PHYSICAL_KICK:
                    result = "<BR>";
                    result += "&nbsp;&nbsp;" + KeyCommandBind.getDesc(KeyCommandBind.PHYS_KICK);
                    break;
                case PHYSICAL_PUSH:
                    result = "<BR>";
                    result += "&nbsp;&nbsp;" + KeyCommandBind.getDesc(KeyCommandBind.PHYS_PUSH);
                    break;
            }

            return result;
        }
    }

    // buttons
    protected Map<PhysicalCommand, MegaMekButton> buttons;

    // let's keep track of what we're shooting and at what, too
    Targetable target; // target

    private final AimedShotHandler ash = new AimedShotHandler();

    /**
     * Creates and lays out a new movement phase display for the specified clientGUI.getClient().
     */
    public PhysicalDisplay(ClientGUI clientgui) {
        super(clientgui);
        game.addGameListener(this);
        setupStatusBar(Messages.getString("PhysicalDisplay.waitingForPhysicalAttackPhase"));
        setButtons();
        setButtonsTooltips();
        setupButtonPanel();
        MegaMekController controller = clientgui.controller;
        controller.registerCommandAction(KeyCommandBind.NEXT_UNIT,
              this,
              () -> selectEntity(clientgui.getClient().getNextEntityNum(currentEntity)));
        controller.registerCommandAction(KeyCommandBind.PREV_UNIT,
              this,
              () -> selectEntity(clientgui.getClient().getPrevEntityNum(currentEntity)));
        controller.registerCommandAction(KeyCommandBind.PHYS_PUNCH, this, this::physPunch);
        controller.registerCommandAction(KeyCommandBind.PHYS_KICK, this, this::physKick);
        controller.registerCommandAction(KeyCommandBind.PHYS_PUSH, this, this::physPush);

        controller.registerCommandAction(KeyCommandBind.NEXT_TARGET, this, () -> jumpToTarget(true, false, false));
        controller.registerCommandAction(KeyCommandBind.PREV_TARGET, this, () -> jumpToTarget(false, false, false));

        controller.registerCommandAction(KeyCommandBind.NEXT_TARGET_VALID, this, () -> jumpToTarget(true, true, false));
        controller.registerCommandAction(KeyCommandBind.PREV_TARGET_VALID,
              this,
              () -> jumpToTarget(false, true, false));

        controller.registerCommandAction(KeyCommandBind.NEXT_TARGET_NO_ALLIES,
              this,
              () -> jumpToTarget(true, false, true));
        controller.registerCommandAction(KeyCommandBind.PREV_TARGET_NO_ALLIES,
              this,
              () -> jumpToTarget(false, false, true));

        controller.registerCommandAction(KeyCommandBind.NEXT_TARGET_VALID_NO_ALLIES,
              this,
              () -> jumpToTarget(true, true, true));
        controller.registerCommandAction(KeyCommandBind.PREV_TARGET_VALID_NO_ALLIES,
              this,
              () -> jumpToTarget(false, true, true));
        controller.registerCommandAction(KeyCommandBind.CANCEL, this::shouldPerformClearKeyCommand, this::clear);
    }

    private void physPunch() {
        buttons.get(PhysicalCommand.PHYSICAL_PUNCH).doClick();
    }

    private void physKick() {
        buttons.get(PhysicalCommand.PHYSICAL_KICK).doClick();
    }

    private void physPush() {
        buttons.get(PhysicalCommand.PHYSICAL_PUSH).doClick();
    }


    /**
     * Cache the list of visible targets. This is used for the 'next target' button.
     * <p>
     * We'll sort it by range to us.
     */
    private void cacheVisibleTargets() {
        clearVisibleTargets();

        List<Entity> vec = clientgui.getClient().getGame().getValidTargets(currentEntity());
        TreeSet<Entity> tree = getEntityTreeSet();
        visibleTargets = new Entity[vec.size()];

        tree.addAll(vec);

        // not go through the sorted Set to cache the targets.
        Iterator<Entity> it = tree.iterator();
        int count = 0;
        while (it.hasNext()) {
            visibleTargets[count++] = it.next();
        }

        // setNextTargetEnabled(visibleTargets.length > 0);
    }

    private TreeSet<Entity> getEntityTreeSet() {
        Comparator<Entity> sortComp = (entX, entY) -> {
            if (entX.getId() == entY.getId()) {
                return 0;
            }

            int rangeToX = currentEntity().getPosition().distance(entX.getPosition());
            int rangeToY = currentEntity().getPosition().distance(entY.getPosition());

            if (rangeToX == rangeToY) {
                return ((entX.getId() < entY.getId()) ? -1 : 1);
            }

            return ((rangeToX < rangeToY) ? -1 : 1);
        };

        // put the vector in the TreeSet first to sort it.
        return new TreeSet<>(sortComp);
    }

    private void clearVisibleTargets() {
        visibleTargets = null;
        lastTargetID = -1;
        // setNextTargetEnabled(false);
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
        clientgui.getCurrentBoardView().ifPresent(bv -> bv.select(targ.getPosition()));

        // HACK : show the choice dialog again.
        showTargetChoice = true;
        target(targ);
    }

    /**
     * Get the next target. Return null if we don't have any targets.
     */
    private Entity getNextTarget(boolean nextOrPrev, boolean onlyValid, boolean ignoreAllies) {
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
            // TODO Implement "only valid" physical attack target selection
            if (ignoreAllies) {
                done = result.isEnemyOf(currentEntity());
            }
        }
        return result;
    }

    protected boolean shouldPerformClearKeyCommand() {
        return !clientgui.isChatBoxActive() && !isIgnoringEvents() && isVisible();
    }

    @Override
    protected String getDoneButtonLabel() {
        return Messages.getString("PhysicalDisplay.Attack");
    }

    @Override
    protected String getSkipTurnButtonLabel() {
        return Messages.getString("PhysicalDisplay.Skip");
    }

    @Override
    protected void setButtons() {
        buttons = new HashMap<>((int) (PhysicalCommand.values().length * 1.25 + 0.5));
        for (PhysicalCommand cmd : PhysicalCommand.values()) {
            buttons.put(cmd, createButton(cmd.getCmd(), "PhysicalDisplay."));
        }
        numButtonGroups = (int) Math.ceil((buttons.size() + 0.0) / buttonsPerGroup);
    }

    @Override
    protected void setButtonsTooltips() {
        for (PhysicalCommand cmd : PhysicalCommand.values()) {
            String tt = createToolTip(cmd.getCmd(), "PhysicalDisplay.", cmd.getHotKeyDesc());
            buttons.get(cmd).setToolTipText(tt);
        }
    }

    @Override
    protected ArrayList<MegaMekButton> getButtonList() {
        ArrayList<MegaMekButton> buttonList = new ArrayList<>();
        int i = 0;
        PhysicalCommand[] commands = PhysicalCommand.values();
        CommandComparator comparator = new CommandComparator();
        Arrays.sort(commands, comparator);
        for (PhysicalCommand cmd : commands) {
            if (cmd == PhysicalCommand.PHYSICAL_NEXT || cmd == PhysicalCommand.PHYSICAL_MORE) {
                continue;
            }
            if (i % buttonsPerGroup == 0) {
                buttonList.add(buttons.get(PhysicalCommand.PHYSICAL_NEXT));
                i++;
            }

            buttonList.add(buttons.get(cmd));
            i++;

            if ((i + 1) % buttonsPerGroup == 0) {
                buttonList.add(buttons.get(PhysicalCommand.PHYSICAL_MORE));
                i++;
            }
        }
        if (!buttonList.get(i - 1).getActionCommand().equals(PhysicalCommand.PHYSICAL_MORE.getCmd())) {
            while ((i + 1) % buttonsPerGroup != 0) {
                buttonList.add(null);
                i++;
            }
            buttonList.add(buttons.get(PhysicalCommand.PHYSICAL_MORE));
        }
        return buttonList;
    }

    /**
     * Selects an entity, by number, for movement.
     */
    public void selectEntity(int en) {
        if (game.getEntity(en) == null) {
            logger.error("Tried to select non-existent entity {}", en);
            return;
        }

        if ((currentEntity() != null) && currentEntity().isWeaponOrderChanged()) {
            clientgui.getClient().sendEntityWeaponOrderUpdate(currentEntity());
        }

        currentEntity = en;
        clientgui.setSelectedEntityNum(en);

        Entity entity = currentEntity();

        target(null);
        if (entity instanceof Mek) {
            int grapple = entity.getGrappled();
            if (grapple != Entity.NONE) {
                Entity t = game.getEntity(grapple);
                if (t != null) {
                    target(t);
                }
            }
        }
        clientgui.onAllBoardViews(IBoardView::clearMarkedHexes);
        clientgui.getBoardView(currentEntity()).highlight(currentEntity().getPosition());

        clientgui.getUnitDisplay().displayEntity(entity);
        if (GUIP.getMoveDisplayTabDuringMovePhases()) {
            clientgui.getUnitDisplay().showPanel(MekPanelTabStrip.SUMMARY);
        }

        clientgui.centerOnUnit(entity);

        // does it have a club?
        StringBuilder clubLabel = null;
        for (Mounted<?> club : entity.getClubs()) {
            String thisLab;
            if (club.getName().endsWith("Club")) {
                thisLab = Messages.getString("PhysicalDisplay.Club");
            } else {
                thisLab = club.getName();
            }
            if (clubLabel == null) {
                clubLabel = new StringBuilder(thisLab);
            } else {
                clubLabel.append("/").append(thisLab);
            }
        }
        if (clubLabel == null) {
            clubLabel = new StringBuilder(Messages.getString("PhysicalDisplay.Club"));
        }
        buttons.get(PhysicalCommand.PHYSICAL_CLUB).setText(clubLabel.toString());

        if ((entity instanceof Mek) && !entity.isProne() && entity.hasAbility(OptionsConstants.PILOT_DODGE_MANEUVER)) {
            setDodgeEnabled(true);
        }
        updateDonePanel();
        cacheVisibleTargets();
    }

    /**
     * Does turn start stuff
     */
    private void beginMyTurn() {
        clientgui.maybeShowUnitDisplay();
        GameTurn turn = clientgui.getClient().getMyTurn();
        // There's special processing for countering break grapple.
        if (turn instanceof CounterGrappleTurn) {
            disableButtons();
            selectEntity(((CounterGrappleTurn) turn).getEntityNum());
            grapple(true);
            ready();
        } else {
            target(null);
            if (GUIP.getAutoSelectNextUnit()) {
                selectEntity(clientgui.getClient().getFirstEntityNum());
            }
            setNextEnabled(true);
            butDone.setEnabled(true);
            if (numButtonGroups > 1) {
                buttons.get(PhysicalCommand.PHYSICAL_MORE).setEnabled(true);
            }
            initDonePanelForNewTurn();

        }
        clientgui.onAllBoardViews(bv -> bv.select(null));

        startTimer();
    }

    /**
     * Does end turn stuff.
     */
    private void endMyTurn() {
        stopTimer();

        // end my turn, then.
        Entity next = game.getNextEntity(game.getTurnIndex());
        if (game.getPhase().isPhysical() &&
              (null != next) &&
              (null != currentEntity()) &&
              (next.getOwnerId() != currentEntity().getOwnerId())) {
            clientgui.maybeShowUnitDisplay();
        }
        currentEntity = Entity.NONE;
        target(null);
        clientgui.onAllBoardViews(IBoardView::clearMarkedHexes);
        clientgui.onAllBoardViews(BoardView::clearMovementData);
        clientgui.setSelectedEntityNum(Entity.NONE);
        disableButtons();
    }

    /**
     * Disables all buttons in the interface
     */
    private void disableButtons() {
        setKickEnabled(false);
        setPunchEnabled(false);
        setPushEnabled(false);
        setTripEnabled(false);
        setGrappleEnabled(false);
        setJumpJetEnabled(false);
        setClubEnabled(false);
        setBrushOffEnabled(false);
        setThrashEnabled(false);
        setDodgeEnabled(false);
        setProtoEnabled(false);
        setVibroEnabled(false);
        setPheromoneEnabled(false);
        setToxinEnabled(false);
        setExplosivesEnabled(false);
        butDone.setEnabled(false);
        setNextEnabled(false);
    }

    private boolean checkNags() {
        if (needNagForNoAction()) {
            if (attacks.isEmpty()) {
                // confirm this action
                String title = Messages.getString("PhysicalDisplay.DontPhysicalAttackDialog.title");
                String body = Messages.getString("PhysicalDisplay.DontPhysicalAttackDialog.message");
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

        disableButtons();

        clientgui.getClient().sendAttackData(currentEntity, attacks.toVector());
        removeAllAttacks();
        // close aimed shot display, if any
        ash.closeDialog();
        if (currentEntity().isWeaponOrderChanged()) {
            clientgui.getClient().sendEntityWeaponOrderUpdate(currentEntity());
        }
        endMyTurn();
    }

    /**
     * Clears all current actions
     */
    @Override
    public void clear() {
        if (!attacks.isEmpty()) {
            removeAllAttacks();
        }

        if (currentEntity() != null) {
            clientgui.getUnitDisplay().wPan.displayMek(currentEntity());
        }
        updateTarget();

        Optional<Entity> entity = Optional.ofNullable(clientgui.getClient().getGame().getEntity(currentEntity));
        entity.ifPresent(e -> e.dodging = true);
    }

    /**
     * Punch the target!
     */
    public void punch() {
        if (currentEntity() == null) {
            return;
        }
        final Entity en = currentEntity();
        final boolean isAptPiloting = (en.getCrew() != null)
              && en.hasAbility(OptionsConstants.PILOT_APTITUDE_PILOTING);
        final boolean canZweihander = (en instanceof BipedMek)
              && ((BipedMek) en).canZweihander()
              && ComputeArc.isInArc(en.getPosition(), en.getSecondaryFacing(), target, en.getForwardArc());
        final boolean isMeleeMaster = (en.getCrew() != null)
              && en.hasAbility(OptionsConstants.PILOT_MELEE_MASTER);

        final ToHitData leftArm = PunchAttackAction.toHit(game, currentEntity,
              target, PunchAttackAction.LEFT, false);
        final ToHitData rightArm = PunchAttackAction.toHit(game, currentEntity,
              target, PunchAttackAction.RIGHT, false);

        final double punchOddsRight = Compute.oddsAbove(rightArm.getValue(), isAptPiloting);
        final int punchDmgRight = PunchAttackAction.getDamageFor(en,
              PunchAttackAction.RIGHT,
              target.isConventionalInfantry(),
              false);

        final double punchOddsLeft = Compute.oddsAbove(leftArm.getValue(), isAptPiloting);
        final int punchDmgLeft = PunchAttackAction.getDamageFor(en,
              PunchAttackAction.LEFT,
              target.isConventionalInfantry(),
              false);

        String title = Messages.getString("PhysicalDisplay.PunchDialog.title", target.getDisplayName());
        String message = Messages.getString("PhysicalDisplay.PunchDialog.message",
              rightArm.getValueAsString(),
              punchOddsRight,
              rightArm.getDesc(),
              punchDmgRight,
              rightArm.getTableDesc(),
              leftArm.getValueAsString(),
              punchOddsLeft,
              leftArm.getDesc(),
              punchDmgLeft,
              leftArm.getTableDesc());
        if (isMeleeMaster) {
            message = Messages.getString("PhysicalDisplay.MeleeMaster") + "\n\n" + message;
        }
        if (clientgui.doYesNoDialog(title, message)) {
            // check for retractable blade that can be extended in each arm
            boolean leftBladeExtend = false;
            boolean rightBladeExtend = false;
            if ((en instanceof Mek)
                  && (target instanceof Entity)
                  && (game.getOptions()
                  .booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_RETRACTABLE_BLADES) || game.getOptions().booleanOption(OptionsConstants.PLAYTEST_3))
                  && (leftArm.getValue() != TargetRoll.IMPOSSIBLE)
                  && ((Mek) currentEntity()).hasRetractedBlade(Mek.LOC_LEFT_ARM)) {
                leftBladeExtend = clientgui.doYesNoDialog(
                      Messages.getString("PhysicalDisplay.ExtendBladeDialog.title"),
                      Messages.getString("PhysicalDisplay.ExtendBladeDialog.message",
                            currentEntity().getLocationName(Mek.LOC_LEFT_ARM)));
            }
            if ((en instanceof Mek)
                  && (target instanceof Entity)
                  && (rightArm.getValue() != TargetRoll.IMPOSSIBLE)
                  && (game.getOptions()
                  .booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_RETRACTABLE_BLADES) || game.getOptions().booleanOption(OptionsConstants.PLAYTEST_3))
                  && ((Mek) en).hasRetractedBlade(Mek.LOC_RIGHT_ARM)) {
                rightBladeExtend = clientgui.doYesNoDialog(
                      Messages.getString("PhysicalDisplay.ExtendBladeDialog" + ".title"),
                      Messages.getString("PhysicalDisplay.ExtendBladeDialog.message",
                            en.getLocationName(Mek.LOC_RIGHT_ARM)));
            }

            boolean zweihandering = false;
            int armChosenZwei = PunchAttackAction.RIGHT;
            if (canZweihander) {
                // need to choose a primary arm. Do it based on highest predicted damage
                ToHitData leftArmZwei = PunchAttackAction.toHit(game,
                      currentEntity, target, PunchAttackAction.LEFT, true);
                ToHitData rightArmZwei = PunchAttackAction.toHit(game,
                      currentEntity, target, PunchAttackAction.RIGHT, true);
                int damageRightZwei = PunchAttackAction.getDamageFor(en, PunchAttackAction.RIGHT,
                      target.isConventionalInfantry(), true);
                int damageLeftZwei = PunchAttackAction.getDamageFor(en, PunchAttackAction.LEFT,
                      target.isConventionalInfantry(), true);
                double oddsLeft = Compute.oddsAbove(leftArmZwei.getValue(), isAptPiloting);
                double oddsRight = Compute.oddsAbove(rightArmZwei.getValue(), isAptPiloting);
                ToHitData toHitZwei = rightArmZwei;
                int damageZwei = damageRightZwei;
                double oddsZwei = oddsRight;
                if ((oddsLeft * damageLeftZwei) > (oddsRight * damageRightZwei)) {
                    toHitZwei = leftArmZwei;
                    damageZwei = damageLeftZwei;
                    oddsZwei = oddsLeft;
                    armChosenZwei = PunchAttackAction.LEFT;
                }

                zweihandering = clientgui.doYesNoDialog(Messages.getString("PhysicalDisplay.ZweihanderPunchDialog.title"),
                      Messages.getString("PhysicalDisplay.ZweihanderPunchDialog.message",
                            toHitZwei.getValueAsString(),
                            oddsZwei,
                            toHitZwei.getDesc(),
                            damageZwei,
                            toHitZwei.getTableDesc()));
            }

            if (zweihandering) {
                if (armChosenZwei == PunchAttackAction.LEFT) {
                    leftArm.addModifier(TargetRoll.IMPOSSIBLE, "zweihandering with other arm");
                } else {
                    rightArm.addModifier(TargetRoll.IMPOSSIBLE, "zweihandering with other arm");
                }
            }

            disableButtons();
            // declare searchlight, if possible
            if (GUIP.getAutoDeclareSearchlight()) {
                doSearchlight();
            }

            if ((leftArm.getValue() != TargetRoll.IMPOSSIBLE) && (rightArm.getValue() != TargetRoll.IMPOSSIBLE)) {
                addAttack(new PunchAttackAction(currentEntity,
                      target.getTargetType(),
                      target.getId(),
                      PunchAttackAction.BOTH,
                      leftBladeExtend,
                      rightBladeExtend,
                      zweihandering));
                if (isMeleeMaster && !zweihandering) {
                    // hit 'em again!
                    addAttack(new PunchAttackAction(currentEntity,
                          target.getTargetType(),
                          target.getId(),
                          PunchAttackAction.BOTH,
                          leftBladeExtend,
                          rightBladeExtend,
                          zweihandering));
                }
            } else if (leftArm.getValue() < rightArm.getValue()) {
                addAttack(new PunchAttackAction(currentEntity,
                      target.getTargetType(),
                      target.getId(),
                      PunchAttackAction.LEFT,
                      leftBladeExtend,
                      rightBladeExtend,
                      zweihandering));
                if (isMeleeMaster && !zweihandering) {
                    // hit 'em again!
                    addAttack(new PunchAttackAction(currentEntity,
                          target.getTargetType(),
                          target.getId(),
                          PunchAttackAction.LEFT,
                          leftBladeExtend,
                          rightBladeExtend,
                          zweihandering));
                }
            } else {
                addAttack(new PunchAttackAction(currentEntity,
                      target.getTargetType(),
                      target.getId(),
                      PunchAttackAction.RIGHT,
                      leftBladeExtend,
                      rightBladeExtend,
                      zweihandering));
                if (isMeleeMaster && !zweihandering) {
                    // hit 'em again!
                    addAttack(new PunchAttackAction(currentEntity,
                          target.getTargetType(),
                          target.getId(),
                          PunchAttackAction.RIGHT,
                          leftBladeExtend,
                          rightBladeExtend,
                          zweihandering));
                }
            }
            ready();
        }
    }

    private void doSearchlight() {
        // validate
        if ((currentEntity() == null) || (target == null)) {
            throw new IllegalArgumentException("current searchlight parameters are invalid");
        }

        if (!SearchlightAttackAction.isPossible(game, currentEntity, target, null)) {
            return;
        }

        // create and queue a searchlight action
        SearchlightAttackAction saa = new SearchlightAttackAction(currentEntity,
              target.getTargetType(),
              target.getId());
        addAttack(saa);

        // and add it into the game, temporarily
        game.addAction(saa);
        clientgui.getBoardView(currentEntity()).addAttack(saa);

        // and prevent duplicates
        setSearchlightEnabled(false);

        // refresh weapon panel, as bth will have changed
        updateTarget();
    }

    /**
     * Kick the target!
     */
    public void kick() {
        if (currentEntity() == null) {
            return;
        }
        final Entity en = currentEntity();
        final boolean isAptPiloting = (en.getCrew() != null) && en.hasAbility(OptionsConstants.PILOT_APTITUDE_PILOTING);
        final boolean isMeleeMaster = (en.getCrew() != null) && en.hasAbility(OptionsConstants.PILOT_MELEE_MASTER);

        ToHitData leftLeg = KickAttackAction.toHit(clientgui.getClient().getGame(),
              currentEntity,
              target,
              KickAttackAction.LEFT);
        ToHitData rightLeg = KickAttackAction.toHit(clientgui.getClient().getGame(),
              currentEntity,
              target,
              KickAttackAction.RIGHT);
        ToHitData rightRearLeg;
        ToHitData leftRearLeg;

        ToHitData attackLeg;
        int attackSide = KickAttackAction.LEFT;
        int value = leftLeg.getValue();
        attackLeg = leftLeg;

        if (value > rightLeg.getValue()) {
            value = rightLeg.getValue();
            attackSide = KickAttackAction.RIGHT;
            attackLeg = rightLeg;
        }
        if (game.getEntity(currentEntity) instanceof QuadMek) {
            rightRearLeg = KickAttackAction.toHit(clientgui.getClient()
                  .getGame(), currentEntity, target, KickAttackAction.RIGHT_MULE);
            leftRearLeg = KickAttackAction.toHit(clientgui.getClient()
                  .getGame(), currentEntity, target, KickAttackAction.LEFT_MULE);
            if (value > rightRearLeg.getValue()) {
                value = rightRearLeg.getValue();
                attackSide = KickAttackAction.RIGHT_MULE;
                attackLeg = rightRearLeg;
            }
            if (value > leftRearLeg.getValue()) {
                attackSide = KickAttackAction.LEFT_MULE;
                attackLeg = leftRearLeg;
            }
        }

        final double kickOdds = Compute.oddsAbove(attackLeg.getValue(), isAptPiloting);
        final int kickDmg = KickAttackAction.getDamageFor(en, attackSide, target.isConventionalInfantry());

        String title = Messages.getString("PhysicalDisplay.KickDialog.title", target.getDisplayName());
        String message = Messages.getString("PhysicalDisplay.KickDialog.message",
              attackLeg.getValueAsString(),
              kickOdds,
              attackLeg.getDesc(),
              kickDmg,
              attackLeg.getTableDesc());

        if (isMeleeMaster) {
            message = Messages.getString("PhysicalDisplay.MeleeMaster") + "\n\n" + message;
        }

        if (clientgui.doYesNoDialog(title, message)) {
            disableButtons();
            // declare searchlight, if possible
            if (GUIP.getAutoDeclareSearchlight()) {
                doSearchlight();
            }

            addAttack(new KickAttackAction(currentEntity, target.getTargetType(), target.getId(), attackSide));
            if (isMeleeMaster) {
                // hit 'em again!
                addAttack(new KickAttackAction(currentEntity, target.getTargetType(), target.getId(), attackSide));
            }
            ready();
        }
    }

    /**
     * Push that target!
     */
    public void push() {
        ToHitData toHit = PushAttackAction.toHit(game, currentEntity, target);
        String title = Messages.getString("PhysicalDisplay.PushDialog.title", target.getDisplayName());
        String message = Messages.getString("PhysicalDisplay.PushDialog.message",
              toHit.getValueAsString(),
              Compute.oddsAbove(toHit.getValue(), currentEntity().hasAbility(OptionsConstants.PILOT_APTITUDE_PILOTING)),
              toHit.getDesc());
        if (clientgui.doYesNoDialog(title, message)) {
            disableButtons();
            // declare searchlight, if possible
            if (GUIP.getAutoDeclareSearchlight()) {
                doSearchlight();
            }

            addAttack(new PushAttackAction(currentEntity,
                  target.getTargetType(),
                  target.getId(),
                  target.getPosition()));
            ready();
        }
    }

    /**
     * Trip that target!
     */
    public void trip() {
        ToHitData toHit = TripAttackAction.toHit(game, currentEntity, target);
        String title = Messages.getString("PhysicalDisplay.TripDialog.title", target.getDisplayName());
        String message = Messages.getString("PhysicalDisplay.TripDialog.message",
              toHit.getValueAsString(),
              Compute.oddsAbove(toHit.getValue(), currentEntity().hasAbility(OptionsConstants.PILOT_APTITUDE_PILOTING)),
              toHit.getDesc());
        if (clientgui.doYesNoDialog(title, message)) {
            disableButtons();
            // declare searchlight, if possible
            if (GUIP.getAutoDeclareSearchlight()) {
                doSearchlight();
            }

            addAttack(new TripAttackAction(currentEntity, target.getTargetType(), target.getId()));
            ready();
        }
    }

    /**
     * Grapple that target!
     */
    public void doGrapple() {
        if (currentEntity().getGrappled() == Entity.NONE) {
            grapple(false);
        } else {
            breakGrapple();
        }
    }

    private void grapple(boolean counter) {
        ToHitData toHit = GrappleAttackAction.toHit(game, currentEntity, target);
        String title = Messages.getString("PhysicalDisplay.GrappleDialog.title", target.getDisplayName());
        String message = Messages.getString("PhysicalDisplay.GrappleDialog.message",
              toHit.getValueAsString(),
              Compute.oddsAbove(toHit.getValue(), currentEntity().hasAbility(OptionsConstants.PILOT_APTITUDE_PILOTING)),
              toHit.getDesc());
        if (counter) {
            message = Messages.getString("PhysicalDisplay.CounterGrappleDialog.message",
                  target.getDisplayName(),
                  toHit.getValueAsString(),
                  Compute.oddsAbove(toHit.getValue(), currentEntity().hasAbility(OptionsConstants.PILOT_APTITUDE_PILOTING)),
                  toHit.getDesc());
        }

        if (clientgui.doYesNoDialog(title, message)) {
            disableButtons();
            // declare searchlight, if possible
            if (GUIP.getAutoDeclareSearchlight()) {
                doSearchlight();
            }

            addAttack(new GrappleAttackAction(currentEntity, target.getTargetType(), target.getId()));
            ready();
        }
    }

    private void breakGrapple() {
        ToHitData toHit = BreakGrappleAttackAction.toHit(game, currentEntity, target);
        String title = Messages.getString("PhysicalDisplay.BreakGrappleDialog.title", target.getDisplayName());
        String message = Messages.getString("PhysicalDisplay.BreakGrappleDialog.message",
              toHit.getValueAsString(),
              Compute.oddsAbove(toHit.getValue(), currentEntity().hasAbility(OptionsConstants.PILOT_APTITUDE_PILOTING)),
              toHit.getDesc());
        if (clientgui.doYesNoDialog(title, message)) {
            disableButtons();
            // declare searchlight, if possible
            if (GUIP.getAutoDeclareSearchlight()) {
                doSearchlight();
            }

            addAttack(new BreakGrappleAttackAction(currentEntity, target.getTargetType(), target.getId()));
            ready();
        }
    }

    /**
     * slice 'em up with your vibroclaws
     */
    public void vibroclawAttack() {
        BAVibroClawAttackAction act = new BAVibroClawAttackAction(currentEntity,
              target.getTargetType(),
              target.getId());
        ToHitData toHit = act.toHit(game);

        String title = Messages.getString("PhysicalDisplay.BAVibroClawDialog.title", target.getDisplayName());
        String message = Messages.getString("PhysicalDisplay.BAVibroClawDialog.message",
              toHit.getValueAsString(),
              Compute.oddsAbove(toHit.getValue(), currentEntity().hasAbility(OptionsConstants.PILOT_APTITUDE_PILOTING)),
              toHit.getDesc(),
              currentEntity().getVibroClaws() + toHit.getTableDesc());

        // Give the user to cancel the attack.
        if (clientgui.doYesNoDialog(title, message)) {
            disableButtons();
            addAttack(act);
            ready();
        }
    }

    /**
     * Release pheromone gas to impair enemy conventional infantry (IO pg 79).
     */
    public void pheromoneAttack() {
        PheromoneAttackAction act = new PheromoneAttackAction(currentEntity,
              target.getTargetType(),
              target.getId());
        ToHitData toHit = act.toHit(game);

        String title = Messages.getString("PhysicalDisplay.PheromoneDialog.title", target.getDisplayName());
        String message = Messages.getString("PhysicalDisplay.PheromoneDialog.message",
              toHit.getValueAsString(),
              Compute.oddsAbove(toHit.getValue(), currentEntity().hasAbility(OptionsConstants.PILOT_APTITUDE_PILOTING)),
              toHit.getDesc());

        // Give the user a chance to cancel the attack.
        if (clientgui.doYesNoDialog(title, message)) {
            disableButtons();
            addAttack(act);
            ready();
        }
    }

    /**
     * Release toxin gas to damage enemy conventional infantry (IO pg 79).
     */
    public void toxinAttack() {
        ToxinAttackAction act = new ToxinAttackAction(currentEntity,
              target.getTargetType(),
              target.getId());
        ToHitData toHit = act.toHit(game);
        int damage = ToxinAttackAction.getDamageFor((Infantry) currentEntity());

        String title = Messages.getString("PhysicalDisplay.ToxinDialog.title", target.getDisplayName());
        String message = Messages.getString("PhysicalDisplay.ToxinDialog.message",
              toHit.getValueAsString(),
              Compute.oddsAbove(toHit.getValue(), currentEntity().hasAbility(OptionsConstants.PILOT_APTITUDE_PILOTING)),
              toHit.getDesc(),
              damage);

        // Give the user a chance to cancel the attack.
        if (clientgui.doYesNoDialog(title, message)) {
            disableButtons();
            addAttack(act);
            ready();
        }
    }

    public void jumpJetAttack() {
        ToHitData toHit;
        int leg;
        int damage;
        if (currentEntity().isProne()) {
            toHit = JumpJetAttackAction.toHit(game,
                  currentEntity,
                  target,
                  JumpJetAttackAction.BOTH);
            leg = JumpJetAttackAction.BOTH;
            damage = JumpJetAttackAction.getDamageFor(currentEntity(), JumpJetAttackAction.BOTH);
        } else {
            ToHitData left = JumpJetAttackAction.toHit(clientgui.getClient().getGame(),
                  currentEntity,
                  target,
                  JumpJetAttackAction.LEFT);
            ToHitData right = JumpJetAttackAction.toHit(clientgui.getClient().getGame(),
                  currentEntity,
                  target,
                  JumpJetAttackAction.RIGHT);
            int d_left = JumpJetAttackAction.getDamageFor(currentEntity(), JumpJetAttackAction.LEFT);
            int d_right = JumpJetAttackAction.getDamageFor(currentEntity(), JumpJetAttackAction.RIGHT);
            if ((d_left *
                  Compute.oddsAbove(left.getValue(), currentEntity().hasAbility(OptionsConstants.PILOT_APTITUDE_PILOTING))) >
                  (d_right *
                        Compute.oddsAbove(right.getValue(),
                              currentEntity().hasAbility(OptionsConstants.PILOT_APTITUDE_PILOTING)))) {
                toHit = left;
                leg = JumpJetAttackAction.LEFT;
                damage = d_left;
            } else {
                toHit = right;
                leg = JumpJetAttackAction.RIGHT;
                damage = d_right;
            }
        }

        String title = Messages.getString("PhysicalDisplay.JumpJetDialog.title", target.getDisplayName());
        String message = Messages.getString("PhysicalDisplay.JumpJetDialog.message",
              toHit.getValueAsString(),
              Compute.oddsAbove(toHit.getValue(), currentEntity().hasAbility(OptionsConstants.PILOT_APTITUDE_PILOTING)),
              toHit.getDesc(),
              damage);
        if (clientgui.doYesNoDialog(title, message)) {
            disableButtons();
            // declare searchlight, if possible
            if (GUIP.getAutoDeclareSearchlight()) {
                doSearchlight();
            }

            addAttack(new JumpJetAttackAction(currentEntity, target.getTargetType(), target.getId(), leg));
            ready();
        }
    }

    private MiscMounted chooseClub() {
        java.util.List<MiscMounted> clubs = currentEntity().getClubs();
        if (clubs.size() == 1) {
            return clubs.get(0);
        } else if (clubs.size() > 1) {
            String[] names = new String[clubs.size()];
            for (int loop = 0; loop < names.length; loop++) {
                MiscMounted club = clubs.get(loop);
                final ToHitData toHit = ClubAttackAction.toHit(game, currentEntity,
                      target, club, ash.getAimTable(), false);
                final int dmg = ClubAttackAction.getDamageFor(currentEntity(), club,
                      target.isConventionalInfantry(), false);
                // Need to do this outside getDamageFor, as it only returns int
                String dmgString = String.valueOf(dmg);
                if ((club.getType().hasAnyFlag(MiscTypeFlag.S_COMBINE, MiscTypeFlag.S_CHAINSAW,
                      MiscTypeFlag.S_DUAL_SAW)) && target.isConventionalInfantry()) {
                    dmgString = "1d6";
                }
                names[loop] = Messages.getString("PhysicalDisplay.ChooseClubDialog.line",
                      club.getName(),
                      toHit.getValueAsString(),
                      dmgString);
            }

            String input = (String) JOptionPane.showInputDialog(clientgui.getFrame(),
                  Messages.getString("PhysicalDisplay.ChooseClubDialog.message"),
                  Messages.getString("PhysicalDisplay.ChooseClubDialog.title"),
                  JOptionPane.QUESTION_MESSAGE,
                  null,
                  names,
                  null);
            if (input != null) {
                for (int i = 0; i < clubs.size(); i++) {
                    if (input.equals(names[i])) {
                        return clubs.get(i);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Club that target!
     */
    void club() {
        MiscMounted club = chooseClub();
        club(club);
    }

    /**
     * Club that target!
     */
    public void club(MiscMounted club) {
        if (null == club) {
            return;
        }
        if (currentEntity() == null) {
            return;
        }
        final Entity en = currentEntity();

        final boolean isAptPiloting = (en.getCrew() != null)
              && en.hasAbility(OptionsConstants.PILOT_APTITUDE_PILOTING);
        final boolean isMeleeMaster = (en.getCrew() != null)
              && en.hasAbility(OptionsConstants.PILOT_MELEE_MASTER);
        final boolean canZweihander = (en instanceof BipedMek)
              && ((BipedMek) en).canZweihander()
              && ComputeArc.isInArc(en.getPosition(), en.getSecondaryFacing(), target, en.getForwardArc());

        final ToHitData toHit = ClubAttackAction.toHit(clientgui.getClient().getGame(),
              currentEntity,
              target,
              club,
              ash.getAimTable(),
              false);
        final double clubOdds = Compute.oddsAbove(toHit.getValue(), isAptPiloting);
        final int clubDmg = ClubAttackAction.getDamageFor(en, club, target.isConventionalInfantry(), false);
        // Need to do this outside getDamageFor, as it only returns int
        String dmgString = String.valueOf(clubDmg);
        if ((club.getType().hasAnyFlag(MiscTypeFlag.S_COMBINE, MiscTypeFlag.S_CHAINSAW, MiscTypeFlag.S_DUAL_SAW))
              && target.isConventionalInfantry()) {
            dmgString = "1d6";
        }
        String title = Messages.getString("PhysicalDisplay.ClubDialog.title", target.getDisplayName());
        String message = Messages.getString("PhysicalDisplay.ClubDialog.message",
              toHit.getValueAsString(),
              clubOdds,
              toHit.getDesc(),
              dmgString,
              toHit.getTableDesc());

        if (isMeleeMaster) {
            message = Messages.getString("PhysicalDisplay.MeleeMaster") + "\n\n" + message;
        }

        if (clientgui.doYesNoDialog(title, message)) {
            boolean zweihandering = false;
            if (canZweihander) {
                ToHitData toHitZwei = ClubAttackAction.toHit(game, currentEntity,
                      target, club, ash.getAimTable(), true);
                zweihandering = clientgui.doYesNoDialog(
                      Messages.getString("PhysicalDisplay.ZweihanderClubDialog.title"),
                      Messages.getString("PhysicalDisplay.ZweihanderClubDialog.message",
                            toHitZwei.getValueAsString(),
                            Compute.oddsAbove(toHit.getValue(), isAptPiloting),
                            toHitZwei.getDesc(),
                            ClubAttackAction.getDamageFor(en, club, target.isConventionalInfantry(), true),
                            toHitZwei.getTableDesc()));
            }

            disableButtons();
            // declare searchlight, if possible
            if (GUIP.getAutoDeclareSearchlight()) {
                doSearchlight();
            }

            addAttack(new ClubAttackAction(currentEntity,
                  target.getTargetType(),
                  target.getId(),
                  club,
                  ash.getAimTable(),
                  zweihandering));
            if (isMeleeMaster && !zweihandering) {
                // hit 'em again!
                addAttack(new ClubAttackAction(currentEntity,
                      target.getTargetType(),
                      target.getId(),
                      club,
                      ash.getAimTable(),
                      zweihandering));
            }
            ready();
        }
    }

    /**
     * Make a protoMek physical attack on the target.
     */
    private void proto() {
        ToHitData proto = ProtoMekPhysicalAttackAction.toHit(game, currentEntity, target);
        String title = Messages.getString("PhysicalDisplay.ProtoMekAttackDialog.title",
              target.getDisplayName());
        String message = Messages.getString("PhysicalDisplay.ProtoMekAttackDialog.message",
              proto.getValueAsString(),
              Compute.oddsAbove(proto.getValue(), currentEntity().hasAbility(OptionsConstants.PILOT_APTITUDE_PILOTING)),
              proto.getDesc(),
              ProtoMekPhysicalAttackAction.getDamageFor(currentEntity(), target) + proto.getTableDesc());
        if (clientgui.doYesNoDialog(title, message)) {
            disableButtons();
            // declare searchlight, if possible
            if (GUIP.getAutoDeclareSearchlight()) {
                doSearchlight();
            }

            addAttack(new ProtoMekPhysicalAttackAction(currentEntity, target.getTargetType(), target.getId()));
            ready();
        }
    }

    private void explosives() {
        ToHitData explosives = LayExplosivesAttackAction.toHit(game, currentEntity, target);
        String title = Messages.getString("PhysicalDisplay.LayExplosivesAttackDialog.title",
              target.getDisplayName());
        String message = Messages.getString("PhysicalDisplay.LayExplosivesAttackDialog.message",
              explosives.getValueAsString(),
              Compute.oddsAbove(explosives.getValue()),
              explosives.getDesc());
        if (clientgui.doYesNoDialog(title, message)) {
            disableButtons();
            addAttack(new LayExplosivesAttackAction(currentEntity, target.getTargetType(), target.getId()));
            ready();
        }
    }

    /**
     * Sweep off the target with the arms that the player selects.
     */
    private void brush() {
        ToHitData toHitLeft = BrushOffAttackAction.toHit(clientgui.getClient().getGame(),
              currentEntity,
              target,
              BrushOffAttackAction.LEFT);
        ToHitData toHitRight = BrushOffAttackAction.toHit(clientgui.getClient().getGame(),
              currentEntity,
              target,
              BrushOffAttackAction.RIGHT);
        boolean canHitLeft = (TargetRoll.IMPOSSIBLE != toHitLeft.getValue());
        boolean canHitRight = (TargetRoll.IMPOSSIBLE != toHitRight.getValue());
        int damageLeft;
        int damageRight;
        String title;
        StringBuilder warn;
        String left = null;
        String right = null;
        String both = null;
        String[] choices;

        // If the entity can't brush off, display an error message and abort.
        if (!canHitLeft && !canHitRight) {
            clientgui.doAlertDialog(Messages.getString("PhysicalDisplay.AlertDialog.title"),
                  Messages.getString("PhysicalDisplay.AlertDialog.message"));
            return;
        }

        // If we can hit with both arms, the player will have to make a choice.
        // Otherwise, the player is just confirming the arm in the attack.
        if (canHitLeft && canHitRight) {
            both = Messages.getString("PhysicalDisplay.bothArms");
            warn = new StringBuilder(Messages.getString("PhysicalDisplay.whichArm"));
            title = Messages.getString("PhysicalDisplay.chooseBrushOff");
        } else {
            warn = new StringBuilder(Messages.getString("PhysicalDisplay.confirmArm"));
            title = Messages.getString("PhysicalDisplay.confirmBrushOff");
        }

        // Build the rest of the warning string.
        // Use correct text when the target is an iNarc pod.
        if (Targetable.TYPE_I_NARC_POD == target.getTargetType()) {
            warn.append(Messages.getString("PhysicalDisplay.brushOff1", target));
        } else {
            warn.append(Messages.getString("PhysicalDisplay.brushOff2"));
        }

        // If we can hit with the left arm, get
        // the damage and construct the string.
        if (canHitLeft) {
            damageLeft = BrushOffAttackAction.getDamageFor(currentEntity(), BrushOffAttackAction.LEFT);
            left = Messages.getString("PhysicalDisplay.LAHit",
                  toHitLeft.getValueAsString(),
                  Compute.oddsAbove(toHitLeft.getValue(), currentEntity().hasAbility(OptionsConstants.PILOT_APTITUDE_PILOTING)),
                  damageLeft);
        }

        // If we can hit with the right arm, get
        // the damage and construct the string.
        if (canHitRight) {
            damageRight = BrushOffAttackAction.getDamageFor(currentEntity(), BrushOffAttackAction.RIGHT);
            right = Messages.getString("PhysicalDisplay.RAHit",
                  toHitRight.getValueAsString(),
                  Compute.oddsAbove(toHitRight.getValue(), currentEntity().hasAbility(OptionsConstants.PILOT_APTITUDE_PILOTING)),
                  damageRight);
        }

        // Allow the player to cancel or choose which arm(s) to use.
        if (canHitLeft && canHitRight) {
            choices = new String[3];
            choices[0] = left;
            choices[1] = right;
            choices[2] = both;

            String input = (String) JOptionPane.showInputDialog(clientgui.getFrame(),
                  warn.toString(),
                  title,
                  JOptionPane.WARNING_MESSAGE,
                  null,
                  choices,
                  null);
            int index = -1;
            if (input != null) {
                for (int i = 0; i < choices.length; i++) {
                    if (input.equals(choices[i])) {
                        index = i;
                        break;
                    }
                }
            }
            if (index != -1) {
                disableButtons();
                switch (index) {
                    case 0:
                        addAttack(new BrushOffAttackAction(currentEntity,
                              target.getTargetType(),
                              target.getId(),
                              BrushOffAttackAction.LEFT));
                        break;
                    case 1:
                        addAttack(new BrushOffAttackAction(currentEntity,
                              target.getTargetType(),
                              target.getId(),
                              BrushOffAttackAction.RIGHT));
                        break;
                    case 2:
                        addAttack(new BrushOffAttackAction(currentEntity,
                              target.getTargetType(),
                              target.getId(),
                              BrushOffAttackAction.BOTH));
                        break;
                }
                ready();
            }
        } else if (canHitLeft) {
            // If only the left arm is available, confirm that choice.
            choices = new String[1];
            choices[0] = left;
            String input = (String) JOptionPane.showInputDialog(clientgui.getFrame(),
                  warn.toString(),
                  title,
                  JOptionPane.WARNING_MESSAGE,
                  null,
                  choices,
                  null);
            if (input != null) {
                disableButtons();
                addAttack(new BrushOffAttackAction(currentEntity,
                      target.getTargetType(),
                      target.getId(),
                      BrushOffAttackAction.LEFT));
                ready();

            }
        } else if (canHitRight) {
            // If only the right arm is available, confirm that choice.
            choices = new String[1];
            choices[0] = right;
            String input = (String) JOptionPane.showInputDialog(clientgui.getFrame(),
                  warn.toString(),
                  title,
                  JOptionPane.WARNING_MESSAGE,
                  null,
                  choices,
                  null);
            if (input != null) {
                disableButtons();
                addAttack(new BrushOffAttackAction(currentEntity,
                      target.getTargetType(),
                      target.getId(),
                      BrushOffAttackAction.RIGHT));
                ready();

            } // End not-cancel

        } // End confirm-right

    } // End private void brush()

    /**
     * Thrash at the target, unless the player cancels the action.
     */
    public void thrash() {
        ThrashAttackAction act = new ThrashAttackAction(currentEntity, target.getTargetType(), target.getId());
        ToHitData toHit = act.toHit(game);

        String title = Messages.getString("PhysicalDisplay.TrashDialog.title", target.getDisplayName());
        String message = Messages.getString("PhysicalDisplay.TrashDialog.message",
              toHit.getValueAsString(),
              Compute.oddsAbove(toHit.getValue(), currentEntity().hasAbility(OptionsConstants.PILOT_APTITUDE_PILOTING)),
              toHit.getDesc(),
              ThrashAttackAction.getDamageFor(currentEntity()) + toHit.getTableDesc());

        // Give the user to cancel the attack.
        if (clientgui.doYesNoDialog(title, message)) {
            disableButtons();
            addAttack(act);
            ready();
        }
    }

    public void dodge() {
        if (clientgui.doYesNoDialog(Messages.getString("PhysicalDisplay.DodgeDialog.title"),
              Messages.getString("PhysicalDisplay.DodgeDialog.message"))) {
            Entity entity = clientgui.getClient().getGame().getEntity(currentEntity);
            if (entity != null) {
                disableButtons();
                entity.dodging = true;

                DodgeAction act = new DodgeAction(currentEntity);
                addAttack(act);

                ready();
            } else {
                try {
                    throw new NullPointerException("Entity ID=" + currentEntity + " is null");
                } catch (NullPointerException e1) {
                    int playerId = clientgui.getClient().getLocalPlayerNumber();
                    String entities = clientgui.getClient()
                          .getGame()
                          .inGameTWEntities()
                          .stream()
                          .filter(e -> e != null && e.getOwnerId() == playerId)
                          .map(Entity::toString)
                          .collect(Collectors.joining(", "));
                    logger.error(e1,
                          "Current Entity ID {} returned empty from clientGUI.getClient().getGame()" +
                                ".getEntity" +
                                "(currentEntity), present units are: {}",
                          currentEntity,
                          entities);
                    logger.errorDialog("Unable to do action",
                          "An unknown event happened and it was impossible to do the action you selected");
                }
            }
        }
    }

    /**
     * Targets something
     */
    public void target(Targetable t) {
        target = t;
        updateTarget();
        ash.showDialog();
    }

    /**
     * Targets an entity
     */
    void updateTarget() {
        // dis/enable physical attach buttons
        if ((currentEntity != Entity.NONE) &&
              currentEntity().equals(clientgui.getUnitDisplay().getCurrentEntity()) &&
              (target != null)) {
            if (target.getTargetType() != Targetable.TYPE_I_NARC_POD) {
                // punch?
                final ToHitData leftArm = PunchAttackAction.toHit(clientgui.getClient().getGame(),
                      currentEntity,
                      target,
                      PunchAttackAction.LEFT,
                      false);
                final ToHitData rightArm = PunchAttackAction.toHit(clientgui.getClient().getGame(),
                      currentEntity,
                      target,
                      PunchAttackAction.RIGHT,
                      false);
                boolean canPunch = (leftArm.getValue() != TargetRoll.IMPOSSIBLE) ||
                      (rightArm.getValue() != TargetRoll.IMPOSSIBLE);
                setPunchEnabled(canPunch);

                // kick?
                ToHitData leftLeg = KickAttackAction.toHit(clientgui.getClient().getGame(),
                      currentEntity,
                      target,
                      KickAttackAction.LEFT);
                ToHitData rightLeg = KickAttackAction.toHit(clientgui.getClient().getGame(),
                      currentEntity,
                      target,
                      KickAttackAction.RIGHT);
                boolean canKick = (leftLeg.getValue() != TargetRoll.IMPOSSIBLE) ||
                      (rightLeg.getValue() != TargetRoll.IMPOSSIBLE);
                ToHitData rightRearLeg = KickAttackAction.toHit(clientgui.getClient().getGame(),
                      currentEntity,
                      target,
                      KickAttackAction.RIGHT_MULE);
                ToHitData leftRearLeg = KickAttackAction.toHit(clientgui.getClient().getGame(),
                      currentEntity,
                      target,
                      KickAttackAction.LEFT_MULE);
                canKick |= (leftRearLeg.getValue() != TargetRoll.IMPOSSIBLE) ||
                      (rightRearLeg.getValue() != TargetRoll.IMPOSSIBLE);

                setKickEnabled(canKick);

                // how about push?
                ToHitData push = PushAttackAction.toHit(clientgui.getClient().getGame(), currentEntity, target);
                setPushEnabled(push.getValue() != TargetRoll.IMPOSSIBLE);

                // how about trip?
                ToHitData trip = TripAttackAction.toHit(clientgui.getClient().getGame(), currentEntity, target);
                setTripEnabled(trip.getValue() != TargetRoll.IMPOSSIBLE);

                // how about grapple?
                ToHitData grapple = GrappleAttackAction.toHit(clientgui.getClient().getGame(), currentEntity, target);
                ToHitData breakGrapple = BreakGrappleAttackAction.toHit(clientgui.getClient().getGame(),
                      currentEntity,
                      target);
                setGrappleEnabled((grapple.getValue() != TargetRoll.IMPOSSIBLE) ||
                      (breakGrapple.getValue() != TargetRoll.IMPOSSIBLE));

                // how about JJ?
                ToHitData jjl = JumpJetAttackAction.toHit(clientgui.getClient().getGame(),
                      currentEntity,
                      target,
                      JumpJetAttackAction.LEFT);
                ToHitData jjr = JumpJetAttackAction.toHit(clientgui.getClient().getGame(),
                      currentEntity,
                      target,
                      JumpJetAttackAction.RIGHT);
                ToHitData jjb = JumpJetAttackAction.toHit(clientgui.getClient().getGame(),
                      currentEntity,
                      target,
                      JumpJetAttackAction.BOTH);
                setJumpJetEnabled(!((jjl.getValue() == TargetRoll.IMPOSSIBLE) &&
                      (jjr.getValue() == TargetRoll.IMPOSSIBLE) &&
                      (jjb.getValue() == TargetRoll.IMPOSSIBLE)));

                // clubbing?
                boolean canClub = false;
                boolean canAim = false;
                for (Mounted<?> club : currentEntity().getClubs()) {
                    if (club != null) {
                        ToHitData clubToHit = ClubAttackAction.toHit(game,
                              currentEntity, target, club, ash.getAimTable(), false);
                        canClub |= (clubToHit.getValue() != TargetRoll.IMPOSSIBLE);
                        // assuming S7 vibroswords count as swords and maces
                        // count as hatchets
                        if (club.getType().hasAnyFlag(MiscTypeFlag.S_SWORD,
                              MiscTypeFlag.S_HATCHET,
                              MiscTypeFlag.S_VIBRO_SMALL,
                              MiscTypeFlag.S_VIBRO_MEDIUM,
                              MiscTypeFlag.S_VIBRO_LARGE,
                              MiscTypeFlag.S_MACE,
                              MiscTypeFlag.S_LANCE,
                              MiscTypeFlag.S_CHAIN_WHIP,
                              MiscTypeFlag.S_RETRACTABLE_BLADE,
                              MiscTypeFlag.S_SHIELD_LARGE,
                              MiscTypeFlag.S_SHIELD_MEDIUM,
                              MiscTypeFlag.S_SHIELD_SMALL)) {
                            canAim = true;
                        }
                    }
                }
                setClubEnabled(canClub);
                ash.setCanAim(canAim);

                // Thrash at infantry?
                ToHitData thrash = new ThrashAttackAction(currentEntity, target)
                      .toHit(game);
                setThrashEnabled(thrash.getValue() != TargetRoll.IMPOSSIBLE);

                // make a ProtoMek physical attack?
                ToHitData proto = ProtoMekPhysicalAttackAction.toHit(clientgui.getClient().getGame(),
                      currentEntity,
                      target);
                setProtoEnabled(proto.getValue() != TargetRoll.IMPOSSIBLE);

                ToHitData explosives = LayExplosivesAttackAction.toHit(clientgui.getClient().getGame(),
                      currentEntity,
                      target);
                setExplosivesEnabled(explosives.getValue() != TargetRoll.IMPOSSIBLE);

                // vibro attack?
                ToHitData vibro = BAVibroClawAttackAction.toHit(clientgui.getClient().getGame(), currentEntity, target);
                setVibroEnabled(vibro.getValue() != TargetRoll.IMPOSSIBLE);

                // pheromone attack?
                ToHitData pheromone = PheromoneAttackAction.toHit(clientgui.getClient().getGame(),
                      currentEntity,
                      target);
                setPheromoneEnabled(pheromone.getValue() != TargetRoll.IMPOSSIBLE);

                // toxin attack?
                ToHitData toxin = ToxinAttackAction.toHit(clientgui.getClient().getGame(),
                      currentEntity,
                      target);
                setToxinEnabled(toxin.getValue() != TargetRoll.IMPOSSIBLE);
            }
            // Brush off swarming infantry or iNarcPods?
            ToHitData brushRight = BrushOffAttackAction.toHit(clientgui.getClient().getGame(),
                  currentEntity,
                  target,
                  BrushOffAttackAction.RIGHT);
            ToHitData brushLeft = BrushOffAttackAction.toHit(clientgui.getClient().getGame(),
                  currentEntity,
                  target,
                  BrushOffAttackAction.LEFT);
            boolean canBrush = ((brushRight.getValue() != TargetRoll.IMPOSSIBLE) ||
                  (brushLeft.getValue() != TargetRoll.IMPOSSIBLE));
            setBrushOffEnabled(canBrush);
        } else {
            setPunchEnabled(false);
            setPushEnabled(false);
            setTripEnabled(false);
            setGrappleEnabled(false);
            setJumpJetEnabled(false);
            setKickEnabled(false);
            setClubEnabled(false);
            setBrushOffEnabled(false);
            setThrashEnabled(false);
            setProtoEnabled(false);
            setVibroEnabled(false);
            setPheromoneEnabled(false);
            setToxinEnabled(false);
        }
        setSearchlightEnabled((currentEntity() != null) && (target != null) && currentEntity().isUsingSearchlight());
    }

    //
    // BoardListener
    //
    @Override
    public void hexMoused(BoardViewEvent event) {

        // Are we ignoring events?
        if (isIgnoringEvents()) {
            return;
        }

        // control pressed means a line of sight check.
        if ((event.getModifiers() & InputEvent.CTRL_DOWN_MASK) != 0) {
            return;
        }
        if (clientgui.getClient().isMyTurn()
              && (event.getButton() == MouseEvent.BUTTON1)) {
            if (event.getType() == BoardViewEvent.BOARD_HEX_DRAGGED) {
                if (!event.getCoords().equals(
                      event.getBoardView().getLastCursor())) {
                    event.getBoardView().cursor(event.getCoords());
                }
            } else if (event.getType() == BoardViewEvent.BOARD_HEX_CLICKED) {
                event.getBoardView().select(event.getCoords());
            }
        }
    }

    @Override
    public void hexSelected(BoardViewEvent event) {
        if (isIgnoringEvents()) {
            return;
        }

        if (isMyTurn() && (event.getCoords() != null) && (currentEntity() != null)) {
            Targetable target = chooseTarget(event);
            target(target);
        }
    }

    /**
     * Returns a target that the player may choose from those at the position of the given event.
     *
     * @param event The hex selection event
     */
    private Targetable chooseTarget(BoardViewEvent event) {
        Entity attacker = currentEntity();
        if (attacker == null) {
            return null;
        }

        List<Targetable> targets = new ArrayList<>();
        boolean usingFriendlyFire = game.getOptions().booleanOption(OptionsConstants.BASE_FRIENDLY_FIRE);

        for (Entity possibleTarget : game.getEntitiesVector(event.getBoardLocation())) {
            if (!attacker.equals(possibleTarget) && (usingFriendlyFire || possibleTarget.isEnemyOf(attacker))) {
                targets.add(possibleTarget);
            }
        }

        targets.sort((o1, o2) -> {
            boolean enemy1 = o1.isEnemyOf(attacker);
            boolean enemy2 = o2.isEnemyOf(attacker);
            return (enemy1 && enemy2) ? 0 : enemy1 ? -1 : 1;
        });

        // Is there a building in the hex?
        Coords pos = event.getCoords();
        Board board = game.getBoard(event.getBoardId());
        if (board.getBuildingAt(pos) != null) {
            targets.add(new BuildingTarget(pos, board, false));
        }

        // Add any iNarc pods attached to the entity if the attacker is targeting its own hex
        if (attacker.getPosition().equals(pos)) {
            Iterator<INarcPod> pods = attacker.getINarcPodsAttached();
            while (pods.hasNext()) {
                Targetable choice = pods.next();
                targets.add(choice);
            }
        }

        if (targets.size() == 1) {
            // Return the single choice
            return targets.get(0);

        } else if (targets.size() > 1) {
            // If we have multiple choices, display a selection dialog.
            return TargetChoiceDialog.showSingleChoiceDialog(clientgui.getFrame(),
                  "PhysicalDisplay.ChooseTargetDialog.title",
                  Messages.getString("PhysicalDisplay.ChooseTargetDialog.message", pos.getBoardNum()),
                  targets,
                  clientgui,
                  attacker);

        } else {
            return null;
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

        if (!game.getPhase().isPhysical()) {
            return;
        }

        String s = getRemainingPlayerWithTurns();

        if (!game.getPhase().isSimultaneous(game)) {
            if (clientgui.getClient().isMyTurn()) {
                setStatusBarText(Messages.getString("PhysicalDisplay.its_your_turn") + s);
            } else {
                String playerName;

                if (e.getPlayer() != null) {
                    playerName = e.getPlayer().getName();
                } else {
                    playerName = "Unknown";
                }

                setStatusBarText(Messages.getString("PhysicalDisplay.its_others_turn", playerName) + s);
            }
        } else {
            setStatusBarText(s);
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

        if (clientgui.getClient().isMyTurn()) {
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

        if (clientgui.getClient().isMyTurn()
              && !game.getPhase().isPhysical()) {
            endMyTurn();
        }
        // if we're ending the firing phase, unregister stuff.
        if (game.getPhase().isPhysical()) {
            setStatusBarText(Messages.getString("PhysicalDisplay.waitingForPhysicalAttackPhase"));
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
            // odd...
            return;
        }
        if (ev.getActionCommand().equals(PhysicalCommand.PHYSICAL_PUNCH.getCmd())) {
            punch();
        } else if (ev.getActionCommand().equals(PhysicalCommand.PHYSICAL_KICK.getCmd())) {
            kick();
        } else if (ev.getActionCommand().equals(PhysicalCommand.PHYSICAL_PUSH.getCmd())) {
            push();
        } else if (ev.getActionCommand().equals(PhysicalCommand.PHYSICAL_TRIP.getCmd())) {
            trip();
        } else if (ev.getActionCommand().equals(PhysicalCommand.PHYSICAL_GRAPPLE.getCmd())) {
            doGrapple();
        } else if (ev.getActionCommand().equals(PhysicalCommand.PHYSICAL_JUMP_JET.getCmd())) {
            jumpJetAttack();
        } else if (ev.getActionCommand().equals(PhysicalCommand.PHYSICAL_CLUB.getCmd())) {
            club();
        } else if (ev.getActionCommand().equals(PhysicalCommand.PHYSICAL_BRUSH_OFF.getCmd())) {
            brush();
        } else if (ev.getActionCommand().equals(PhysicalCommand.PHYSICAL_THRASH.getCmd())) {
            thrash();
        } else if (ev.getActionCommand().equals(PhysicalCommand.PHYSICAL_DODGE.getCmd())) {
            dodge();
        } else if (ev.getActionCommand().equals(PhysicalCommand.PHYSICAL_PROTO.getCmd())) {
            proto();
        } else if (ev.getActionCommand().equals(PhysicalCommand.PHYSICAL_EXPLOSIVES.getCmd())) {
            explosives();
        } else if (ev.getActionCommand().equals(PhysicalCommand.PHYSICAL_VIBRO.getCmd())) {
            vibroclawAttack();
        } else if (ev.getActionCommand().equals(PhysicalCommand.PHYSICAL_PHEROMONE.getCmd())) {
            pheromoneAttack();
        } else if (ev.getActionCommand().equals(PhysicalCommand.PHYSICAL_TOXIN.getCmd())) {
            toxinAttack();
        } else if (ev.getActionCommand().equals(PhysicalCommand.PHYSICAL_NEXT.getCmd())) {
            selectEntity(clientgui.getClient().getNextEntityNum(currentEntity));
        } else if (ev.getActionCommand().equals(PhysicalCommand.PHYSICAL_SEARCHLIGHT.getCmd())) {
            doSearchlight();
        } else if (ev.getActionCommand().equals(PhysicalCommand.PHYSICAL_MORE.getCmd())) {
            currentButtonGroup++;
            currentButtonGroup %= numButtonGroups;
            setupButtonPanel();
        }
    }

    //
    // BoardViewListener
    //
    @Override
    public void finishedMovingUnits(BoardViewEvent b) {
        // no action
    }

    @Override
    public void unitSelected(BoardViewEvent b) {
        if (isIgnoringEvents()) {
            return;
        }

        Entity e = game.getEntity(b.getEntityId());
        if (e != null) {
            if (isMyTurn()) {
                if (clientgui.getClient().getMyTurn().isValidEntity(e, game)) {
                    selectEntity(e.getId());
                }
            } else {
                clientgui.maybeShowUnitDisplay();
                clientgui.getUnitDisplay().displayEntity(e);
                clientgui.centerOnUnit(e);
            }
        }
    }

    public void setThrashEnabled(boolean enabled) {
        buttons.get(PhysicalCommand.PHYSICAL_THRASH).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(PhysicalCommand.PHYSICAL_THRASH.getCmd(), enabled);
    }

    public void setPunchEnabled(boolean enabled) {
        buttons.get(PhysicalCommand.PHYSICAL_PUNCH).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(PhysicalCommand.PHYSICAL_PUNCH.getCmd(), enabled);
    }

    public void setKickEnabled(boolean enabled) {
        buttons.get(PhysicalCommand.PHYSICAL_KICK).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(PhysicalCommand.PHYSICAL_KICK.getCmd(), enabled);
    }

    public void setPushEnabled(boolean enabled) {
        buttons.get(PhysicalCommand.PHYSICAL_PUSH).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(PhysicalCommand.PHYSICAL_PUSH.getCmd(), enabled);
    }

    public void setTripEnabled(boolean enabled) {
        buttons.get(PhysicalCommand.PHYSICAL_TRIP).setEnabled(enabled);
    }

    public void setGrappleEnabled(boolean enabled) {
        buttons.get(PhysicalCommand.PHYSICAL_GRAPPLE).setEnabled(enabled);
    }

    public void setJumpJetEnabled(boolean enabled) {
        buttons.get(PhysicalCommand.PHYSICAL_JUMP_JET).setEnabled(enabled);
    }

    public void setClubEnabled(boolean enabled) {
        buttons.get(PhysicalCommand.PHYSICAL_CLUB).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(PhysicalCommand.PHYSICAL_CLUB.getCmd(), enabled);
    }

    public void setBrushOffEnabled(boolean enabled) {
        buttons.get(PhysicalCommand.PHYSICAL_BRUSH_OFF).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(PhysicalCommand.PHYSICAL_BRUSH_OFF.getCmd(), enabled);
    }

    public void setDodgeEnabled(boolean enabled) {
        buttons.get(PhysicalCommand.PHYSICAL_DODGE).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(PhysicalCommand.PHYSICAL_DODGE.getCmd(), enabled);
    }

    public void setProtoEnabled(boolean enabled) {
        buttons.get(PhysicalCommand.PHYSICAL_PROTO).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(PhysicalCommand.PHYSICAL_PROTO.getCmd(), enabled);
    }

    public void setVibroEnabled(boolean enabled) {
        buttons.get(PhysicalCommand.PHYSICAL_VIBRO).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(PhysicalCommand.PHYSICAL_VIBRO.getCmd(), enabled);
    }

    public void setPheromoneEnabled(boolean enabled) {
        buttons.get(PhysicalCommand.PHYSICAL_PHEROMONE).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(PhysicalCommand.PHYSICAL_PHEROMONE.getCmd(), enabled);
    }

    public void setToxinEnabled(boolean enabled) {
        buttons.get(PhysicalCommand.PHYSICAL_TOXIN).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(PhysicalCommand.PHYSICAL_TOXIN.getCmd(), enabled);
    }

    public void setExplosivesEnabled(boolean enabled) {
        buttons.get(PhysicalCommand.PHYSICAL_EXPLOSIVES).setEnabled(enabled);
        // clientGUI.getMenuBar().setExplosivesEnabled(enabled);
    }

    public void setNextEnabled(boolean enabled) {
        buttons.get(PhysicalCommand.PHYSICAL_NEXT).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(PhysicalCommand.PHYSICAL_NEXT.getCmd(), enabled);
    }

    private void setSearchlightEnabled(boolean enabled) {
        buttons.get(PhysicalCommand.PHYSICAL_SEARCHLIGHT).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(PhysicalCommand.PHYSICAL_SEARCHLIGHT.getCmd(), enabled);
    }

    private class AimedShotHandler implements ActionListener, ItemListener {
        private int aimingAt = -1;

        private AimingMode aimingMode = AimingMode.NONE;

        private AimedShotDialog asd;

        private boolean canAim;

        public AimedShotHandler() {
            // no action
        }

        public int getAimTable() {
            return switch (aimingAt) {
                case 0 -> ToHitData.HIT_PUNCH;
                case 1 -> ToHitData.HIT_KICK;
                default -> ToHitData.HIT_NORMAL;
            };
        }

        public void setCanAim(boolean v) {
            canAim = v;
        }

        public void showDialog() {

            if ((currentEntity() == null) || (target == null)) {
                return;
            }

            if (asd != null) {
                AimingMode oldAimingMode = aimingMode;
                closeDialog();
                aimingMode = oldAimingMode;
            }

            if (canAim) {
                final int attackerElevation = currentEntity().getElevation() + game.getHexOf(currentEntity()).getLevel();
                final int targetElevation = target.getElevation() + game.getHexOf(target).getLevel();

                if ((target instanceof Mek) && (currentEntity() instanceof Mek) && (attackerElevation == targetElevation)) {
                    String[] options = { "punch", "kick" };
                    boolean[] enabled = { true, true };

                    asd = new AimedShotDialog(clientgui.getFrame(),
                          Messages.getString("PhysicalDisplay.AimedShotDialog.title"),
                          Messages.getString("PhysicalDisplay.AimedShotDialog.message"),
                          options,
                          enabled,
                          aimingAt,
                          clientgui,
                          target,
                          this,
                          this);

                    asd.setVisible(true);
                    updateTarget();
                }
            }
        }

        public void closeDialog() {
            if (asd != null) {
                aimingAt = Entity.LOC_NONE;
                aimingMode = AimingMode.NONE;
                asd.dispose();
                asd = null;
                updateTarget();
            }
        }

        // ActionListener, listens to the button in the dialog.
        @Override
        public void actionPerformed(ActionEvent ev) {
            closeDialog();
        }

        // ItemListener, listens to the radiobuttons in the dialog.
        @Override
        public void itemStateChanged(ItemEvent ev) {
            IndexedRadioButton icb = (IndexedRadioButton) ev.getSource();
            aimingAt = icb.getIndex();
            updateTarget();
        }
    }
}

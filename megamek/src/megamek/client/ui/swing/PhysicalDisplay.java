/*
 * MegaMek - Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
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
import megamek.client.ui.swing.util.KeyCommandBind;
import megamek.client.ui.swing.widget.IndexedRadioButton;
import megamek.client.ui.swing.widget.MegamekButton;
import megamek.common.*;
import megamek.common.actions.*;
import megamek.common.enums.AimingMode;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GameTurnChangeEvent;
import megamek.common.options.OptionsConstants;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.awt.event.*;
import java.util.*;

import static megamek.client.ui.swing.util.UIUtil.guiScaledFontHTML;
import static megamek.client.ui.swing.util.UIUtil.uiLightViolet;

public class PhysicalDisplay extends StatusBarPhaseDisplay {
    private static final long serialVersionUID = -3274750006768636001L;

    /**
     * This enumeration lists all the possible ActionCommands that can be
     * carried out during the physical phase.  Each command has a string for the
     * command plus a flag that determines what unit type it is appropriate for.
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
        PHYSICAL_JUMPJET("jumpjet"),
        PHYSICAL_PROTO("protoPhysical"),
        PHYSICAL_SEARCHLIGHT("fireSearchlight"),
        PHYSICAL_EXPLOSIVES("explosives"),
        PHYSICAL_VIBRO("vibro"),
        PHYSICAL_MORE("more");

        String cmd;

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

            if (this == PHYSICAL_NEXT) {
                result = "<BR>";
                result += "&nbsp;&nbsp;" + "Next" + ": " + KeyCommandBind.getDesc(KeyCommandBind.NEXT_UNIT);
                result += "&nbsp;&nbsp;" + "Previous" + ": " + KeyCommandBind.getDesc(KeyCommandBind.PREV_UNIT);
            }

            return result;
        }
    }

    // buttons
    protected Map<PhysicalCommand, MegamekButton> buttons;

    // let's keep track of what we're shooting and at what, too
    private int cen = Entity.NONE; // current entity number
    Targetable target; // target

    // stuff we want to do
    private Vector<EntityAction> attacks;

    private AimedShotHandler ash = new AimedShotHandler();

    /**
     * Creates and lays out a new movement phase display for the specified
     * clientgui.getClient().
     */
    public PhysicalDisplay(ClientGUI clientgui) {
        super(clientgui);

        clientgui.getClient().getGame().addGameListener(this);

        clientgui.getBoardView().addBoardViewListener(this);
        setupStatusBar(Messages.getString("PhysicalDisplay.waitingForPhysicalAttackPhase"));

        attacks = new Vector<>();

        setButtons();
        setButtonsTooltips();

        butDone.setText("<html><body>" + Messages.getString("PhysicalDisplay.Done") + "</body></html>");
        String f = guiScaledFontHTML(uiLightViolet()) +  KeyCommandBind.getDesc(KeyCommandBind.DONE)+ "</FONT>";
        butDone.setToolTipText("<html><body>" + f + "</body></html>");
        butDone.setEnabled(false);

        setupButtonPanel();

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
    protected ArrayList<MegamekButton> getButtonList() {
        ArrayList<MegamekButton> buttonList = new ArrayList<>();
        int i = 0;
        PhysicalCommand[] commands = PhysicalCommand.values();
        CommandComparator comparator = new CommandComparator();
        Arrays.sort(commands, comparator);
        for (PhysicalCommand cmd : commands) {
            if (cmd == PhysicalCommand.PHYSICAL_NEXT
                    || cmd == PhysicalCommand.PHYSICAL_MORE) {
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
        if (clientgui.getClient().getGame().getEntity(en) == null) {
            LogManager.getLogger().error("Tried to select non-existent entity " + en);
            return;
        }

        if ((ce() != null) &&ce().isWeapOrderChanged()) {
            clientgui.getClient().sendEntityWeaponOrderUpdate(ce());
        }

        cen = en;
        clientgui.setSelectedEntityNum(en);

        Entity entity = ce();

        target(null);
        if (entity instanceof Mech) {
            int grapple = entity.getGrappled();
            if (grapple != Entity.NONE) {
                Entity t = clientgui.getClient().getGame().getEntity(grapple);
                if (t != null) {
                    target(t);
                }
            }
        }
        clientgui.getBoardView().highlight(ce().getPosition());
        clientgui.getBoardView().select(null);
        clientgui.getBoardView().cursor(null);

        clientgui.getUnitDisplay().displayEntity(entity);
        clientgui.getUnitDisplay().showPanel("movement");

        clientgui.getBoardView().centerOnHex(entity.getPosition());

        // does it have a club?
        String clubLabel = null;
        for (Mounted club : entity.getClubs()) {
            String thisLab;
            if (club.getName().endsWith("Club")) {
                thisLab = Messages.getString("PhysicalDisplay.Club");
            } else {
                thisLab = club.getName();
            }
            if (clubLabel == null) {
                clubLabel = thisLab;
            } else {
                clubLabel = clubLabel + "/" + thisLab;
            }
        }
        if (clubLabel == null) {
            clubLabel = Messages.getString("PhysicalDisplay.Club");
        }
        buttons.get(PhysicalCommand.PHYSICAL_CLUB).setText(clubLabel);

        if ((entity instanceof Mech)
            && !entity.isProne()
            && entity.hasAbility(OptionsConstants.PILOT_DODGE_MANEUVER)) {
            setDodgeEnabled(true);
        }
    }

    /**
     * Does turn start stuff
     */
    private void beginMyTurn() {
        clientgui.maybeShowUnitDisplay();
        GameTurn turn = clientgui.getClient().getMyTurn();
        // There's special processing for countering break grapple.
        if (turn instanceof GameTurn.CounterGrappleTurn) {
            disableButtons();
            selectEntity(((GameTurn.CounterGrappleTurn) turn).getEntityNum());
            grapple(true);
            ready();
        } else {
            target(null);
            selectEntity(clientgui.getClient().getFirstEntityNum());
            setNextEnabled(true);
            butDone.setEnabled(true);
            if (numButtonGroups > 1) {
                buttons.get(PhysicalCommand.PHYSICAL_MORE).setEnabled(true);
            }
        }
        clientgui.getBoardView().select(null);
    }

    /**
     * Does end turn stuff.
     */
    private void endMyTurn() {
        // end my turn, then.
        Entity next = clientgui.getClient().getGame()
                .getNextEntity(clientgui.getClient().getGame().getTurnIndex());
        if (clientgui.getClient().getGame().getPhase().isPhysical() && (null != next)
                && (null != ce()) && (next.getOwnerId() != ce().getOwnerId())) {
            clientgui.maybeShowUnitDisplay();
        }
        cen = Entity.NONE;
        target(null);
        clientgui.getBoardView().select(null);
        clientgui.getBoardView().highlight(null);
        clientgui.getBoardView().cursor(null);
        clientgui.getBoardView().clearMovementData();
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
        setExplosivesEnabled(false);
        butDone.setEnabled(false);
        setNextEnabled(false);
    }

    /**
     * Called when the current entity is done with physical attacks.
     */
    @Override
    public void ready() {
        if (attacks.isEmpty() && GUIP.getNagForNoAction()) {
            // confirm this action
            ConfirmDialog response = clientgui.doYesNoBotherDialog(
                    Messages.getString("PhysicalDisplay.DontPhysicalAttackDialog.title"),
                    Messages.getString("PhysicalDisplay.DontPhysicalAttackDialog.message"));
            if (!response.getShowAgain()) {
                GUIP.setNagForNoAction(false);
            }
            if (!response.getAnswer()) {
                return;
            }
        }
        disableButtons();
        clientgui.getClient().sendAttackData(cen, attacks);
        attacks.removeAllElements();
        // close aimed shot display, if any
        ash.closeDialog();
        if (ce().isWeapOrderChanged()) {
            clientgui.getClient().sendEntityWeaponOrderUpdate(ce());
        }
        endMyTurn();
    }

    /**
     * Clears all current actions
     */
    @Override
    public void clear() {
        if (!attacks.isEmpty()) {
            attacks.removeAllElements();
        }

        if (ce() != null) {
            clientgui.getUnitDisplay().wPan.displayMech(ce());
        }
        updateTarget();

        Entity entity = clientgui.getClient().getGame().getEntity(cen);
        entity.dodging = true;
    }

    /**
     * Punch the target!
     */
    void punch() {
        if (ce() == null) {
            return;
        }
        final Entity en = ce();
        final boolean isAptPiloting = (en.getCrew() != null)
                && en.hasAbility(OptionsConstants.PILOT_APTITUDE_PILOTING);
        final boolean canZweihander = (en instanceof BipedMech)
                && ((BipedMech) en).canZweihander()
                && Compute.isInArc(en.getPosition(), en.getSecondaryFacing(), target, en.getForwardArc());
        final boolean isMeleeMaster = (en.getCrew() != null)
                && en.hasAbility(OptionsConstants.PILOT_MELEE_MASTER);

        final ToHitData leftArm = PunchAttackAction.toHit(clientgui.getClient().getGame(), cen,
                target, PunchAttackAction.LEFT, false);
        final ToHitData rightArm = PunchAttackAction.toHit(clientgui.getClient().getGame(), cen,
                target, PunchAttackAction.RIGHT, false);

        final double punchOddsRight = Compute.oddsAbove(rightArm.getValue(), isAptPiloting);
        final int punchDmgRight = PunchAttackAction.getDamageFor(en,
                PunchAttackAction.RIGHT, target.isConventionalInfantry(), false);

        final double punchOddsLeft = Compute.oddsAbove(leftArm.getValue(), isAptPiloting);
        final int punchDmgLeft = PunchAttackAction.getDamageFor(en, PunchAttackAction.LEFT,
                target.isConventionalInfantry(), false);

        String title = Messages.getString("PhysicalDisplay.PunchDialog.title", target.getDisplayName());
        String message = Messages.getString("PhysicalDisplay.PunchDialog.message",
                rightArm.getValueAsString(), punchOddsRight, rightArm.getDesc(), punchDmgRight,
                rightArm.getTableDesc(), leftArm.getValueAsString(), punchOddsLeft,
                leftArm.getDesc(), punchDmgLeft, leftArm.getTableDesc());
        if (isMeleeMaster) {
            message = Messages.getString("PhysicalDisplay.MeleeMaster") + "\n\n" + message;
        }
        if (clientgui.doYesNoDialog(title, message)) {
            // check for retractable blade that can be extended in each arm
            boolean leftBladeExtend = false;
            boolean rightBladeExtend = false;
            if ((en instanceof Mech)
                    && (target instanceof Entity)
                    && clientgui.getClient().getGame().getOptions()
                            .booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_RETRACTABLE_BLADES)
                    && (leftArm.getValue() != TargetRoll.IMPOSSIBLE)
                    && ((Mech) ce()).hasRetractedBlade(Mech.LOC_LARM)) {
                leftBladeExtend = clientgui.doYesNoDialog(
                        Messages.getString("PhysicalDisplay.ExtendBladeDialog.title"),
                        Messages.getString("PhysicalDisplay.ExtendBladeDialog.message",
                                ce().getLocationName(Mech.LOC_LARM)));
            }
            if ((en instanceof Mech)
                    && (target instanceof Entity)
                    && (rightArm.getValue() != TargetRoll.IMPOSSIBLE)
                    && clientgui.getClient().getGame().getOptions()
                            .booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_RETRACTABLE_BLADES)
                    && ((Mech) en).hasRetractedBlade(Mech.LOC_RARM)) {
                rightBladeExtend = clientgui.doYesNoDialog(
                        Messages.getString("PhysicalDisplay.ExtendBladeDialog" + ".title"),
                        Messages.getString("PhysicalDisplay.ExtendBladeDialog.message",
                                en.getLocationName(Mech.LOC_RARM)));
            }

            boolean zweihandering = false;
            int armChosenZwei = PunchAttackAction.RIGHT;
            if (canZweihander) {
                // need to choose a primary arm. Do it based on highest predicted damage
                ToHitData leftArmZwei = PunchAttackAction.toHit(clientgui.getClient().getGame(),
                        cen, target, PunchAttackAction.LEFT, true);
                ToHitData rightArmZwei = PunchAttackAction.toHit(clientgui.getClient().getGame(),
                        cen, target, PunchAttackAction.RIGHT, true);
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

                zweihandering = clientgui.doYesNoDialog(
                        Messages.getString("PhysicalDisplay.ZweihanderPunchDialog.title"),
                        Messages.getString("PhysicalDisplay.ZweihanderPunchDialog.message",
                                toHitZwei.getValueAsString(), oddsZwei, toHitZwei.getDesc(),
                                damageZwei, toHitZwei.getTableDesc()));
            }

            if (zweihandering) {
                if (armChosenZwei==PunchAttackAction.LEFT) {
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

            if ((leftArm.getValue() != TargetRoll.IMPOSSIBLE)
                    && (rightArm.getValue() != TargetRoll.IMPOSSIBLE)) {
                attacks.addElement(new PunchAttackAction(cen, target
                        .getTargetType(), target.getId(),
                        PunchAttackAction.BOTH, leftBladeExtend,
                        rightBladeExtend, zweihandering));
                if (isMeleeMaster && !zweihandering) {
                    // hit 'em again!
                    attacks.addElement(new PunchAttackAction(cen, target
                            .getTargetType(), target.getId(),
                            PunchAttackAction.BOTH, leftBladeExtend,
                            rightBladeExtend, zweihandering));
                }
            } else if (leftArm.getValue() < rightArm.getValue()) {
                attacks.addElement(new PunchAttackAction(cen, target
                        .getTargetType(), target.getId(),
                        PunchAttackAction.LEFT, leftBladeExtend,
                        rightBladeExtend, zweihandering));
                if (isMeleeMaster  && !zweihandering) {
                    // hit 'em again!
                    attacks.addElement(new PunchAttackAction(cen, target
                            .getTargetType(), target.getId(),
                            PunchAttackAction.LEFT, leftBladeExtend,
                            rightBladeExtend, zweihandering));
                }
            } else {
                attacks.addElement(new PunchAttackAction(cen, target
                        .getTargetType(), target.getId(),
                        PunchAttackAction.RIGHT, leftBladeExtend,
                        rightBladeExtend, zweihandering));
                if (isMeleeMaster && !zweihandering) {
                    // hit 'em again!
                    attacks.addElement(new PunchAttackAction(cen, target
                            .getTargetType(), target.getId(),
                            PunchAttackAction.RIGHT, leftBladeExtend,
                            rightBladeExtend, zweihandering));
                }
            }
            ready();
        }
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
        SearchlightAttackAction saa = new SearchlightAttackAction(cen, target.getTargetType(),
                target.getId());
        attacks.addElement(saa);

        // and add it into the game, temporarily
        clientgui.getClient().getGame().addAction(saa);
        clientgui.getBoardView().addAttack(saa);

        // and prevent duplicates
        setSearchlightEnabled(false);

        // refresh weapon panel, as bth will have changed
        updateTarget();
    }

    /**
     * Kick the target!
     */
    void kick() {
        if (ce() == null) {
            return;
        }
        final Entity en = ce();
        final boolean isAptPiloting = (en.getCrew() != null)
                && en.hasAbility(OptionsConstants.PILOT_APTITUDE_PILOTING);
        final boolean isMeleeMaster = (en.getCrew() != null)
                && en.hasAbility(OptionsConstants.PILOT_MELEE_MASTER);

        ToHitData leftLeg = KickAttackAction.toHit(clientgui.getClient()
                .getGame(), cen, target, KickAttackAction.LEFT);
        ToHitData rightLeg = KickAttackAction.toHit(clientgui.getClient()
                .getGame(), cen, target, KickAttackAction.RIGHT);
        ToHitData rightRearLeg = null;
        ToHitData leftRearLeg = null;

        ToHitData attackLeg;
        int attackSide = KickAttackAction.LEFT;
        int value = leftLeg.getValue();
        attackLeg = leftLeg;

        if (value > rightLeg.getValue()) {
            value = rightLeg.getValue();
            attackSide = KickAttackAction.RIGHT;
            attackLeg = rightLeg;
        }
        if (clientgui.getClient().getGame().getEntity(cen) instanceof QuadMech) {
            rightRearLeg = KickAttackAction.toHit(clientgui.getClient()
                    .getGame(), cen, target, KickAttackAction.RIGHTMULE);
            leftRearLeg = KickAttackAction.toHit(clientgui.getClient()
                    .getGame(), cen, target, KickAttackAction.LEFTMULE);
            if (value > rightRearLeg.getValue()) {
                value = rightRearLeg.getValue();
                attackSide = KickAttackAction.RIGHTMULE;
                attackLeg = rightRearLeg;
            }
            if (value > leftRearLeg.getValue()) {
                value = leftRearLeg.getValue();
                attackSide = KickAttackAction.LEFTMULE;
                attackLeg = leftRearLeg;
            }
        }

        final double kickOdds = Compute.oddsAbove(attackLeg.getValue(),
                isAptPiloting);
        final int kickDmg = KickAttackAction.getDamageFor(en, attackSide, target.isConventionalInfantry());

        String title = Messages.getString("PhysicalDisplay.KickDialog.title", target.getDisplayName());
        String message = Messages.getString("PhysicalDisplay.KickDialog.message",
                attackLeg.getValueAsString(), kickOdds, attackLeg.getDesc(), kickDmg, attackLeg.getTableDesc());

        if (isMeleeMaster) {
            message = Messages.getString("PhysicalDisplay.MeleeMaster") + "\n\n" + message;
        }

        if (clientgui.doYesNoDialog(title, message)) {
            disableButtons();
            // declare searchlight, if possible
            if (GUIP.getAutoDeclareSearchlight()) {
                doSearchlight();
            }

            attacks.addElement(new KickAttackAction(cen,
                    target.getTargetType(), target.getId(), attackSide));
            if (isMeleeMaster) {
                // hit 'em again!
                attacks.addElement(new KickAttackAction(cen, target
                        .getTargetType(), target.getId(), attackSide));
            }
            ready();
        }
    }

    /**
     * Push that target!
     */
    void push() {
        ToHitData toHit = PushAttackAction.toHit(clientgui.getClient().getGame(), cen, target);
        String title = Messages.getString("PhysicalDisplay.PushDialog.title", target.getDisplayName());
        String message = Messages.getString("PhysicalDisplay.PushDialog.message",
                toHit.getValueAsString(),
                Compute.oddsAbove(toHit.getValue(), ce().hasAbility(OptionsConstants.PILOT_APTITUDE_PILOTING)),
                toHit.getDesc());
        if (clientgui.doYesNoDialog(title, message)) {
            disableButtons();
            // declare searchlight, if possible
            if (GUIP.getAutoDeclareSearchlight()) {
                doSearchlight();
            }

            attacks.addElement(new PushAttackAction(cen,
                    target.getTargetType(), target.getId(), target
                            .getPosition()));
            ready();
        }
    }

    /**
     * Trip that target!
     */
    void trip() {
        ToHitData toHit = TripAttackAction.toHit(clientgui.getClient().getGame(), cen, target);
        String title = Messages.getString("PhysicalDisplay.TripDialog.title", target.getDisplayName());
        String message = Messages.getString("PhysicalDisplay.TripDialog.message",
                toHit.getValueAsString(),
                Compute.oddsAbove(toHit.getValue(), ce().hasAbility(OptionsConstants.PILOT_APTITUDE_PILOTING)),
                toHit.getDesc());
        if (clientgui.doYesNoDialog(title, message)) {
            disableButtons();
            // declare searchlight, if possible
            if (GUIP.getAutoDeclareSearchlight()) {
                doSearchlight();
            }

            attacks.addElement(new TripAttackAction(cen,
                    target.getTargetType(), target.getId()));
            ready();
        }
    }

    /**
     * Grapple that target!
     */
    void doGrapple() {
        if (ce().getGrappled() == Entity.NONE) {
            grapple(false);
        } else {
            breakGrapple();
        }
    }

    private void grapple(boolean counter) {
        ToHitData toHit = GrappleAttackAction.toHit(clientgui.getClient().getGame(), cen, target);
        String title = Messages.getString("PhysicalDisplay.GrappleDialog.title", target.getDisplayName());
        String message = Messages.getString("PhysicalDisplay.GrappleDialog.message",
                toHit.getValueAsString(),
                Compute.oddsAbove(toHit.getValue(), ce().hasAbility(OptionsConstants.PILOT_APTITUDE_PILOTING)),
                toHit.getDesc());
        if (counter) {
            message = Messages.getString("PhysicalDisplay.CounterGrappleDialog.message",
                    target.getDisplayName(), toHit.getValueAsString(),
                    Compute.oddsAbove(toHit.getValue(), ce().hasAbility(OptionsConstants.PILOT_APTITUDE_PILOTING)),
                    toHit.getDesc());
        }

        if (clientgui.doYesNoDialog(title, message)) {
            disableButtons();
            // declare searchlight, if possible
            if (GUIP.getAutoDeclareSearchlight()) {
                doSearchlight();
            }

            attacks.addElement(new GrappleAttackAction(cen, target.getTargetType(), target.getId()));
            ready();
        }
    }

    private void breakGrapple() {
        ToHitData toHit = BreakGrappleAttackAction.toHit(clientgui.getClient().getGame(), cen, target);
        String title = Messages.getString("PhysicalDisplay.BreakGrappleDialog.title",
                target.getDisplayName());
        String message = Messages.getString("PhysicalDisplay.BreakGrappleDialog.message",
                toHit.getValueAsString(),
                Compute.oddsAbove(toHit.getValue(), ce().hasAbility(OptionsConstants.PILOT_APTITUDE_PILOTING)),
                toHit.getDesc());
        if (clientgui.doYesNoDialog(title, message)) {
            disableButtons();
            // declare searchlight, if possible
            if (GUIP.getAutoDeclareSearchlight()) {
                doSearchlight();
            }

            attacks.addElement(new BreakGrappleAttackAction(cen, target.getTargetType(), target.getId()));
            ready();
        }
    }

    /**
     * slice 'em up with your vibroclaws
     */
    public void vibroclawatt() {
        BAVibroClawAttackAction act = new BAVibroClawAttackAction(cen, target.getTargetType(),
                target.getId());
        ToHitData toHit = act.toHit(clientgui.getClient().getGame());

        String title = Messages.getString("PhysicalDisplay.BAVibroClawDialog.title",
                target.getDisplayName());
        String message = Messages.getString("PhysicalDisplay.BAVibroClawDialog.message",
                toHit.getValueAsString(),
                Compute.oddsAbove(toHit.getValue(), ce().hasAbility(OptionsConstants.PILOT_APTITUDE_PILOTING)),
                toHit.getDesc(), ce().getVibroClaws() + toHit.getTableDesc());

        // Give the user to cancel the attack.
        if (clientgui.doYesNoDialog(title, message)) {
            disableButtons();
            attacks.addElement(act);
            ready();
        }
    }

    void jumpjetatt() {
        ToHitData toHit;
        int leg;
        int damage;
        if (ce().isProne()) {
            toHit = JumpJetAttackAction.toHit(clientgui.getClient().getGame(),
                    cen, target, JumpJetAttackAction.BOTH);
            leg = JumpJetAttackAction.BOTH;
            damage = JumpJetAttackAction.getDamageFor(ce(),
                    JumpJetAttackAction.BOTH);
        } else {
            ToHitData left = JumpJetAttackAction.toHit(clientgui.getClient()
                    .getGame(), cen, target, JumpJetAttackAction.LEFT);
            ToHitData right = JumpJetAttackAction.toHit(clientgui.getClient()
                    .getGame(), cen, target, JumpJetAttackAction.RIGHT);
            int d_left = JumpJetAttackAction.getDamageFor(ce(),
                    JumpJetAttackAction.LEFT);
            int d_right = JumpJetAttackAction.getDamageFor(ce(),
                    JumpJetAttackAction.RIGHT);
            if ((d_left * Compute.oddsAbove(
                    left.getValue(),
                    ce().hasAbility(OptionsConstants.PILOT_APTITUDE_PILOTING))) > (d_right * Compute
                    .oddsAbove(
                            right.getValue(),
                            ce().hasAbility(OptionsConstants.PILOT_APTITUDE_PILOTING)))) {
                toHit = left;
                leg = JumpJetAttackAction.LEFT;
                damage = d_left;
            } else {
                toHit = right;
                leg = JumpJetAttackAction.RIGHT;
                damage = d_right;
            }
        }

        String title = Messages.getString("PhysicalDisplay.JumpJetDialog.title",
                target.getDisplayName());
        String message = Messages.getString("PhysicalDisplay.JumpJetDialog.message",
                toHit.getValueAsString(),
                Compute.oddsAbove(toHit.getValue(), ce().hasAbility(OptionsConstants.PILOT_APTITUDE_PILOTING)),
                toHit.getDesc(), damage);
        if (clientgui.doYesNoDialog(title, message)) {
            disableButtons();
            // declare searchlight, if possible
            if (GUIP.getAutoDeclareSearchlight()) {
                doSearchlight();
            }

            attacks.addElement(new JumpJetAttackAction(cen, target
                    .getTargetType(), target.getId(), leg));
            ready();
        }
    }

    private Mounted chooseClub() {
        java.util.List<Mounted> clubs = ce().getClubs();
        if (clubs.size() == 1) {
            return clubs.get(0);
        } else if (clubs.size() > 1) {
            String[] names = new String[clubs.size()];
            for (int loop = 0; loop < names.length; loop++) {
                Mounted club = clubs.get(loop);
                final ToHitData toHit = ClubAttackAction.toHit(clientgui.getClient().getGame(), cen,
                        target, club, ash.getAimTable(), false);
                final int dmg = ClubAttackAction.getDamageFor(ce(), club,
                        target.isConventionalInfantry(), false);
                // Need to do this outside getDamageFor, as it only returns int
                String dmgString = dmg + "";
                if ((club.getType().hasSubType(MiscType.S_COMBINE)
                        || club.getType().hasSubType(MiscType.S_CHAINSAW)
                        || club.getType().hasSubType(MiscType.S_DUAL_SAW))
                        && target.isConventionalInfantry()) {
                    dmgString = "1d6";
                }
                names[loop] = Messages.getString("PhysicalDisplay.ChooseClubDialog.line",
                        club.getName(), toHit.getValueAsString(), dmgString);
            }

            String input = (String) JOptionPane.showInputDialog(clientgui,
                    Messages.getString("PhysicalDisplay.ChooseClubDialog.message"),
                    Messages.getString("PhysicalDisplay.ChooseClubDialog.title"),
                    JOptionPane.QUESTION_MESSAGE, null, names, null);
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
        Mounted club = chooseClub();
        club(club);
    }

    /**
     * Club that target!
     */
    void club(Mounted club) {
        if (null == club) {
            return;
        }
        if (ce() == null) {
            return;
        }
        final Entity en = ce();

        final boolean isAptPiloting = (en.getCrew() != null)
                && en.hasAbility(OptionsConstants.PILOT_APTITUDE_PILOTING);
        final boolean isMeleeMaster = (en.getCrew() != null)
                && en.hasAbility(OptionsConstants.PILOT_MELEE_MASTER);
        final boolean canZweihander = (en instanceof BipedMech)
                && ((BipedMech) en).canZweihander()
                && Compute.isInArc(en.getPosition(), en.getSecondaryFacing(), target, en.getForwardArc());

        final ToHitData toHit = ClubAttackAction.toHit(clientgui.getClient()
                .getGame(), cen, target, club, ash.getAimTable(), false);
        final double clubOdds = Compute.oddsAbove(toHit.getValue(),
                isAptPiloting);
        final int clubDmg = ClubAttackAction.getDamageFor(en, club, target.isConventionalInfantry(), false);
        // Need to do this outside getDamageFor, as it only returns int
        String dmgString = clubDmg + "";
        if ((club.getType().hasSubType(MiscType.S_COMBINE)
                || club.getType().hasSubType(MiscType.S_CHAINSAW)
                || club.getType().hasSubType(MiscType.S_DUAL_SAW))
                && target.isConventionalInfantry()) {
            dmgString = "1d6";
        }
        String title = Messages.getString("PhysicalDisplay.ClubDialog.title", target.getDisplayName());
        String message = Messages.getString("PhysicalDisplay.ClubDialog.message",
                toHit.getValueAsString(), clubOdds, toHit.getDesc(), dmgString, toHit.getTableDesc());

        if (isMeleeMaster) {
            message = Messages.getString("PhysicalDisplay.MeleeMaster") + "\n\n" + message;
        }

        if (clientgui.doYesNoDialog(title, message)) {
            boolean zweihandering = false;
            if (canZweihander) {
                ToHitData toHitZwei = ClubAttackAction.toHit(clientgui.getClient().getGame(), cen,
                        target, club, ash.getAimTable(), true);
                zweihandering = clientgui.doYesNoDialog(Messages.getString("PhysicalDisplay.ZweihanderClubDialog.title"),
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

            attacks.addElement(new ClubAttackAction(cen,
                    target.getTargetType(), target.getId(), club, ash
                            .getAimTable(), zweihandering));
            if (isMeleeMaster && !zweihandering) {
                // hit 'em again!
                attacks.addElement(new ClubAttackAction(cen, target
                        .getTargetType(), target.getId(), club, ash
                        .getAimTable(), zweihandering));
            }
            ready();
        }
    }

    /**
     * Make a protomech physical attack on the target.
     */
    private void proto() {
        ToHitData proto = ProtomechPhysicalAttackAction.toHit(clientgui.getClient().getGame(), cen, target);
        String title = Messages.getString("PhysicalDisplay.ProtoMechAttackDialog.title",
                target.getDisplayName());
        String message = Messages.getString("PhysicalDisplay.ProtoMechAttackDialog.message",
                proto.getValueAsString(),
                Compute.oddsAbove(proto.getValue(), ce().hasAbility(OptionsConstants.PILOT_APTITUDE_PILOTING)),
                proto.getDesc(),
                ProtomechPhysicalAttackAction.getDamageFor(ce(), target) + proto.getTableDesc());
        if (clientgui.doYesNoDialog(title, message)) {
            disableButtons();
            // declare searchlight, if possible
            if (GUIP.getAutoDeclareSearchlight()) {
                doSearchlight();
            }

            attacks.addElement(new ProtomechPhysicalAttackAction(cen, target
                    .getTargetType(), target.getId()));
            ready();
        }
    }

    private void explosives() {
        ToHitData explo = LayExplosivesAttackAction.toHit(clientgui.getClient().getGame(), cen, target);
        String title = Messages.getString("PhysicalDisplay.LayExplosivesAttackDialog.title",
                target.getDisplayName());
        String message = Messages.getString("PhysicalDisplay.LayExplosivesAttackDialog.message",
                explo.getValueAsString(), Compute.oddsAbove(explo.getValue()), explo.getDesc());
        if (clientgui.doYesNoDialog(title, message)) {
            disableButtons();
            attacks.addElement(new LayExplosivesAttackAction(cen, target.getTargetType(),
                    target.getId()));
            ready();
        }
    }

    /**
     * Sweep off the target with the arms that the player selects.
     */
    private void brush() {
        ToHitData toHitLeft = BrushOffAttackAction.toHit(clientgui.getClient()
                .getGame(), cen, target, BrushOffAttackAction.LEFT);
        ToHitData toHitRight = BrushOffAttackAction.toHit(clientgui.getClient()
                .getGame(), cen, target, BrushOffAttackAction.RIGHT);
        boolean canHitLeft = (TargetRoll.IMPOSSIBLE != toHitLeft.getValue());
        boolean canHitRight = (TargetRoll.IMPOSSIBLE != toHitRight.getValue());
        int damageLeft = 0;
        int damageRight = 0;
        String title = null;
        StringBuffer warn = null;
        String left = null;
        String right = null;
        String both = null;
        String[] choices = null;

        // If the entity can't brush off, display an error message and abort.
        if (!canHitLeft && !canHitRight) {
            clientgui.doAlertDialog(
                    Messages.getString("PhysicalDisplay.AlertDialog.title"),
                    Messages.getString("PhysicalDisplay.AlertDialog.message"));
            return;
        }

        // If we can hit with both arms, the player will have to make a choice.
        // Otherwise, the player is just confirming the arm in the attack.
        if (canHitLeft && canHitRight) {
            both = Messages.getString("PhysicalDisplay.bothArms");
            warn = new StringBuffer(Messages.getString("PhysicalDisplay.whichArm"));
            title = Messages.getString("PhysicalDisplay.chooseBrushOff");
        } else {
            warn = new StringBuffer(Messages.getString("PhysicalDisplay.confirmArm"));
            title = Messages.getString("PhysicalDisplay.confirmBrushOff");
        }

        // Build the rest of the warning string.
        // Use correct text when the target is an iNarc pod.
        if (Targetable.TYPE_INARC_POD == target.getTargetType()) {
            warn.append(Messages.getString("PhysicalDisplay.brushOff1", target));
        } else {
            warn.append(Messages.getString("PhysicalDisplay.brushOff2"));
        }

        // If we can hit with the left arm, get
        // the damage and construct the string.
        if (canHitLeft) {
            damageLeft = BrushOffAttackAction.getDamageFor(ce(), BrushOffAttackAction.LEFT);
            left = Messages.getString("PhysicalDisplay.LAHit", toHitLeft.getValueAsString(),
                    Compute.oddsAbove(toHitLeft.getValue(), ce().hasAbility(OptionsConstants.PILOT_APTITUDE_PILOTING)), damageLeft);
        }

        // If we can hit with the right arm, get
        // the damage and construct the string.
        if (canHitRight) {
            damageRight = BrushOffAttackAction.getDamageFor(ce(), BrushOffAttackAction.RIGHT);
            right = Messages.getString("PhysicalDisplay.RAHit", toHitRight.getValueAsString(),
                    Compute.oddsAbove(toHitRight.getValue(), ce().hasAbility(OptionsConstants.PILOT_APTITUDE_PILOTING)),
                    damageRight);
        }

        // Allow the player to cancel or choose which arm(s) to use.
        if (canHitLeft && canHitRight) {
            choices = new String[3];
            choices[0] = left;
            choices[1] = right;
            choices[2] = both;

            String input = (String) JOptionPane.showInputDialog(clientgui,
                    warn.toString(), title, JOptionPane.WARNING_MESSAGE, null,
                    choices, null);
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
                        attacks.addElement(new BrushOffAttackAction(cen, target
                                .getTargetType(), target.getId(),
                                BrushOffAttackAction.LEFT));
                        break;
                    case 1:
                        attacks.addElement(new BrushOffAttackAction(cen, target
                                .getTargetType(), target.getId(),
                                BrushOffAttackAction.RIGHT));
                        break;
                    case 2:
                        attacks.addElement(new BrushOffAttackAction(cen, target
                                .getTargetType(), target.getId(),
                                BrushOffAttackAction.BOTH));
                        break;
                }
                ready();
            }
        } else if (canHitLeft) {
            // If only the left arm is available, confirm that choice.
            choices = new String[1];
            choices[0] = left;
            String input = (String) JOptionPane.showInputDialog(clientgui,
                    warn.toString(), title, JOptionPane.WARNING_MESSAGE, null,
                    choices, null);
            if (input != null) {
                disableButtons();
                attacks.addElement(new BrushOffAttackAction(cen, target
                        .getTargetType(), target.getId(),
                        BrushOffAttackAction.LEFT));
                ready();

            }
        } else if (canHitRight) {
            // If only the right arm is available, confirm that choice.
            choices = new String[1];
            choices[0] = right;
            String input = (String) JOptionPane.showInputDialog(clientgui,
                    warn.toString(), title, JOptionPane.WARNING_MESSAGE, null,
                    choices, null);
            if (input != null) {
                disableButtons();
                attacks.addElement(new BrushOffAttackAction(cen, target
                        .getTargetType(), target.getId(),
                        BrushOffAttackAction.RIGHT));
                ready();

            } // End not-cancel

        } // End confirm-right

    } // End private void brush()

    /**
     * Thrash at the target, unless the player cancels the action.
     */
    void thrash() {
        ThrashAttackAction act = new ThrashAttackAction(cen, target.getTargetType(), target.getId());
        ToHitData toHit = act.toHit(clientgui.getClient().getGame());

        String title = Messages.getString("PhysicalDisplay.TrashDialog.title", target.getDisplayName());
        String message = Messages.getString("PhysicalDisplay.TrashDialog.message", toHit.getValueAsString(),
                Compute.oddsAbove(toHit.getValue(), ce().hasAbility(OptionsConstants.PILOT_APTITUDE_PILOTING)),
                toHit.getDesc(), ThrashAttackAction.getDamageFor(ce()) + toHit.getTableDesc());

        // Give the user to cancel the attack.
        if (clientgui.doYesNoDialog(title, message)) {
            disableButtons();
            attacks.addElement(act);
            ready();
        }
    }

    /**
     * Dodge like that guy in that movie that I won't name for copywrite
     * reasons!
     */
    void dodge() {
        if (clientgui.doYesNoDialog(
                Messages.getString("PhysicalDisplay.DodgeDialog.title"),
                Messages.getString("PhysicalDisplay.DodgeDialog.message"))) {
            disableButtons();

            Entity entity = clientgui.getClient().getGame().getEntity(cen);
            entity.dodging = true;

            DodgeAction act = new DodgeAction(cen);
            attacks.addElement(act);

            ready();
        }
    }

    /**
     * Targets something
     */
    void target(Targetable t) {
        target = t;
        updateTarget();
        ash.showDialog();
    }

    /**
     * Targets an entity
     */
    void updateTarget() {
        // dis/enable physical attach buttons
        if ((cen != Entity.NONE) && (target != null)) {
            if (target.getTargetType() != Targetable.TYPE_INARC_POD) {
                // punch?
                final ToHitData leftArm = PunchAttackAction.toHit(clientgui
                        .getClient().getGame(), cen, target,
                        PunchAttackAction.LEFT, false);
                final ToHitData rightArm = PunchAttackAction.toHit(clientgui
                        .getClient().getGame(), cen, target,
                        PunchAttackAction.RIGHT, false);
                boolean canPunch = (leftArm.getValue() != TargetRoll.IMPOSSIBLE)
                        || (rightArm.getValue() != TargetRoll.IMPOSSIBLE);
                setPunchEnabled(canPunch);

                // kick?
                ToHitData leftLeg = KickAttackAction.toHit(clientgui
                        .getClient().getGame(), cen, target,
                        KickAttackAction.LEFT);
                ToHitData rightLeg = KickAttackAction.toHit(clientgui
                        .getClient().getGame(), cen, target,
                        KickAttackAction.RIGHT);
                boolean canKick = (leftLeg.getValue() != TargetRoll.IMPOSSIBLE)
                        || (rightLeg.getValue() != TargetRoll.IMPOSSIBLE);
                ToHitData rightRearLeg = KickAttackAction.toHit(clientgui
                        .getClient().getGame(), cen, target,
                        KickAttackAction.RIGHTMULE);
                ToHitData leftRearLeg = KickAttackAction.toHit(clientgui
                        .getClient().getGame(), cen, target,
                        KickAttackAction.LEFTMULE);
                canKick |= (leftRearLeg.getValue() != TargetRoll.IMPOSSIBLE)
                        || (rightRearLeg.getValue() != TargetRoll.IMPOSSIBLE);

                setKickEnabled(canKick);

                // how about push?
                ToHitData push = PushAttackAction.toHit(clientgui.getClient()
                        .getGame(), cen, target);
                setPushEnabled(push.getValue() != TargetRoll.IMPOSSIBLE);

                // how about trip?
                ToHitData trip = TripAttackAction.toHit(clientgui.getClient()
                        .getGame(), cen, target);
                setTripEnabled(trip.getValue() != TargetRoll.IMPOSSIBLE);

                // how about grapple?
                ToHitData grap = GrappleAttackAction.toHit(clientgui
                        .getClient().getGame(), cen, target);
                ToHitData bgrap = BreakGrappleAttackAction.toHit(clientgui
                        .getClient().getGame(), cen, target);
                setGrappleEnabled((grap.getValue() != TargetRoll.IMPOSSIBLE)
                        || (bgrap.getValue() != TargetRoll.IMPOSSIBLE));

                // how about JJ?
                ToHitData jjl = JumpJetAttackAction.toHit(clientgui.getClient()
                        .getGame(), cen, target, JumpJetAttackAction.LEFT);
                ToHitData jjr = JumpJetAttackAction.toHit(clientgui.getClient()
                        .getGame(), cen, target, JumpJetAttackAction.RIGHT);
                ToHitData jjb = JumpJetAttackAction.toHit(clientgui.getClient()
                        .getGame(), cen, target, JumpJetAttackAction.BOTH);
                setJumpJetEnabled(!((jjl.getValue() == TargetRoll.IMPOSSIBLE)
                        && (jjr.getValue() == TargetRoll.IMPOSSIBLE) && (jjb
                            .getValue() == TargetRoll.IMPOSSIBLE)));

                // clubbing?
                boolean canClub = false;
                boolean canAim = false;
                for (Mounted club : ce().getClubs()) {
                    if (club != null) {
                        ToHitData clubToHit = ClubAttackAction.toHit(clientgui.getClient().getGame(),
                                cen, target, club, ash.getAimTable(), false);
                        canClub |= (clubToHit.getValue() != TargetRoll.IMPOSSIBLE);
                        // assuming S7 vibroswords count as swords and maces
                        // count as hatchets
                        if (club.getType().hasSubType(MiscType.S_SWORD)
                                || club.getType().hasSubType(MiscType.S_HATCHET)
                                || club.getType().hasSubType(MiscType.S_VIBRO_SMALL)
                                || club.getType().hasSubType(MiscType.S_VIBRO_MEDIUM)
                                || club.getType().hasSubType(MiscType.S_VIBRO_LARGE)
                                || club.getType().hasSubType(MiscType.S_MACE)
                                || club.getType().hasSubType(MiscType.S_LANCE)
                                || club.getType().hasSubType(MiscType.S_CHAIN_WHIP)
                                || club.getType().hasSubType(MiscType.S_RETRACTABLE_BLADE)
                                || club.getType().hasSubType(MiscType.S_SHIELD_LARGE)
                                || club.getType().hasSubType(MiscType.S_SHIELD_MEDIUM)
                                || club.getType().hasSubType(MiscType.S_SHIELD_SMALL)) {
                            canAim = true;
                        }
                    }
                }
                setClubEnabled(canClub);
                ash.setCanAim(canAim);

                // Thrash at infantry?
                ToHitData thrash = new ThrashAttackAction(cen, target)
                        .toHit(clientgui.getClient().getGame());
                setThrashEnabled(thrash.getValue() != TargetRoll.IMPOSSIBLE);

                // make a Protomech physical attack?
                ToHitData proto = ProtomechPhysicalAttackAction.toHit(clientgui
                        .getClient().getGame(), cen, target);
                setProtoEnabled(proto.getValue() != TargetRoll.IMPOSSIBLE);

                ToHitData explo = LayExplosivesAttackAction.toHit(clientgui
                        .getClient().getGame(), cen, target);
                setExplosivesEnabled(explo.getValue() != TargetRoll.IMPOSSIBLE);

                // vibro attack?
                ToHitData vibro = BAVibroClawAttackAction.toHit(clientgui
                        .getClient().getGame(), cen, target);
                setVibroEnabled(vibro.getValue() != TargetRoll.IMPOSSIBLE);
            }
            // Brush off swarming infantry or iNarcPods?
            ToHitData brushRight = BrushOffAttackAction.toHit(clientgui
                    .getClient().getGame(), cen, target,
                    BrushOffAttackAction.RIGHT);
            ToHitData brushLeft = BrushOffAttackAction.toHit(clientgui
                    .getClient().getGame(), cen, target,
                    BrushOffAttackAction.LEFT);
            boolean canBrush = ((brushRight.getValue() != TargetRoll.IMPOSSIBLE) || (brushLeft
                    .getValue() != TargetRoll.IMPOSSIBLE));
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
        }
        setSearchlightEnabled((ce() != null) && (target != null)
                && ce().isUsingSearchlight());
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

        // control pressed means a line of sight check.
        if ((b.getModifiers() & InputEvent.CTRL_DOWN_MASK) != 0) {
            return;
        }
        if (clientgui.getClient().isMyTurn()
            && (b.getButton() == MouseEvent.BUTTON1)) {
            if (b.getType() == BoardViewEvent.BOARD_HEX_DRAGGED) {
                if (!b.getCoords().equals(
                        clientgui.getBoardView().getLastCursor())) {
                    clientgui.getBoardView().cursor(b.getCoords());
                }
            } else if (b.getType() == BoardViewEvent.BOARD_HEX_CLICKED) {
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

        if (clientgui.getClient().isMyTurn() && (b.getCoords() != null) && (ce() != null)) {
            final Targetable targ = chooseTarget(b.getCoords());
            target(targ);
        }
    }

    /**
     * Have the player select a target from the entities at the given coords.
     *
     * @param pos - the <code>Coords</code> containing targets.
     */
    private Targetable chooseTarget(Coords pos) {
        final Game game = clientgui.getClient().getGame();
        // Assume that we have *no* choice.
        Targetable choice = null;

        // Get the available choices.
        Iterator<Entity> choices = game.getEntities(pos);

        // Convert the choices into a List of targets.
        List<Targetable> targets = new ArrayList<>();
        boolean friendlyFire = game.getOptions().booleanOption(OptionsConstants.BASE_FRIENDLY_FIRE);
        while (choices.hasNext()) {
            choice = choices.next();
            if (!ce().equals(choice) && (friendlyFire || choice.isEnemyOf(ce()))) {
                targets.add(choice);
            }
        }
        targets.sort((o1, o2) -> {
            boolean enemy1 = o1.isEnemyOf(ce());
            boolean enemy2 = o2.isEnemyOf(ce());
            if (enemy1 && enemy2) {
                return 0;
            } else if (enemy1) {
                return -1;
            } else {
                return 1;
            }
        });

        // Is there a building in the hex?
        Building bldg = game.getBoard().getBuildingAt(pos);
        if (bldg != null) {
            targets.add(new BuildingTarget(pos, game.getBoard(), false));
        }

        // Is the attacker targeting its own hex?
        if (ce().getPosition().equals(pos)) {
            // Add any iNarc pods attached to the entity.
            Iterator<INarcPod> pods = ce().getINarcPodsAttached();
            while (pods.hasNext()) {
                choice = pods.next();
                targets.add(choice);
            }
        }

        // Do we have a single choice?
        if (targets.size() == 1) {
            // Return that choice.
            choice = targets.get(0);
        } else if (targets.size() > 1) {
            // If we have multiple choices, display a selection dialog.
            choice = TargetChoiceDialog.showSingleChoiceDialog(clientgui.getFrame(),
                    Messages.getString("PhysicalDisplay.ChooseTargetDialog.message", pos.getBoardNum()),
                    Messages.getString("PhysicalDisplay.ChooseTargetDialog.title"),
                    targets, clientgui, ce());
        }

        // Return the chosen unit.
        return choice;
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

        if (!clientgui.getClient().getGame().getPhase().isPhysical()) {
            LogManager.getLogger().error("Got TurnChange event during the "
                    + clientgui.getClient().getGame().getPhase() + " phase");
            return;
        }

        String s = getRemainingPlayerWithTurns();

        if (clientgui.getClient().isMyTurn()) {
            if (cen == Entity.NONE) {
                beginMyTurn();
            }

            setStatusBarText(Messages.getString("PhysicalDisplay.its_your_turn") + s);
            clientgui.bingMyTurn();
        } else {
            endMyTurn();
            String playerName;

            if (e.getPlayer() != null) {
                playerName = e.getPlayer().getName();
            } else {
                playerName = "Unknown";
            }

            setStatusBarText(Messages.getString("PhysicalDisplay.its_others_turn", playerName) + s);
            clientgui.bingOthersTurn();
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

        if (clientgui.getClient().isMyTurn()
                && !clientgui.getClient().getGame().getPhase().isPhysical()) {
            endMyTurn();
        }
        // if we're ending the firing phase, unregister stuff.
        if (clientgui.getClient().getGame().getPhase().isPhysical()) {
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
        } else if (ev.getActionCommand().equals(PhysicalCommand.PHYSICAL_JUMPJET.getCmd())) {
            jumpjetatt();
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
            vibroclawatt();
        } else if (ev.getActionCommand().equals(PhysicalCommand.PHYSICAL_NEXT.getCmd())) {
            selectEntity(clientgui.getClient().getNextEntityNum(cen));
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
        buttons.get(PhysicalCommand.PHYSICAL_JUMPJET).setEnabled(enabled);
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

    public void setExplosivesEnabled(boolean enabled) {
        buttons.get(PhysicalCommand.PHYSICAL_EXPLOSIVES).setEnabled(enabled);
        // clientgui.getMenuBar().setExplosivesEnabled(enabled);
    }

    public void setNextEnabled(boolean enabled) {
        buttons.get(PhysicalCommand.PHYSICAL_NEXT).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(PhysicalCommand.PHYSICAL_NEXT.getCmd(), enabled);
    }

    private void setSearchlightEnabled(boolean enabled) {
        buttons.get(PhysicalCommand.PHYSICAL_SEARCHLIGHT).setEnabled(enabled);
        clientgui.getMenuBar().setEnabled(PhysicalCommand.PHYSICAL_SEARCHLIGHT.getCmd(), enabled);
    }

    /**
     * Stop just ignoring events and actually stop listening to them.
     */
    @Override
    public void removeAllListeners() {
        clientgui.getClient().getGame().removeGameListener(this);
        clientgui.getBoardView().removeBoardViewListener(this);
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
            switch (aimingAt) {
                case 0:
                    return ToHitData.HIT_PUNCH;
                case 1:
                    return ToHitData.HIT_KICK;
                default:
                    return ToHitData.HIT_NORMAL;
            }
        }

        public void setCanAim(boolean v) {
            canAim = v;
        }

        public void showDialog() {

            if ((ce() == null) || (target == null)) {
                return;
            }

            if (asd != null) {
                AimingMode oldAimingMode = aimingMode;
                closeDialog();
                aimingMode = oldAimingMode;
            }

            if (canAim) {

                final int attackerElevation = ce().getElevation()
                        + ce().getGame().getBoard().getHex(ce().getPosition())
                                .getLevel();
                final int targetElevation = target.getElevation()
                        + ce().getGame().getBoard()
                                .getHex(target.getPosition()).getLevel();

                if ((target instanceof Mech) && (ce() instanceof Mech)
                        && (attackerElevation == targetElevation)) {
                    String[] options = { "punch", "kick" };
                    boolean[] enabled = { true, true };

                    asd = new AimedShotDialog(
                            clientgui.frame,
                            Messages.getString("PhysicalDisplay.AimedShotDialog.title"),
                            Messages.getString("PhysicalDisplay.AimedShotDialog.message"),
                            options, enabled, aimingAt, this, this);

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

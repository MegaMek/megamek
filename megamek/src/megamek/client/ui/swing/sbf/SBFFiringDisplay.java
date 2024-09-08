/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.swing.sbf;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import megamek.client.event.BoardViewEvent;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.SBFClientGUI;
import megamek.client.ui.swing.SBFTargetDialog;
import megamek.client.ui.swing.util.KeyCommandBind;
import megamek.client.ui.swing.widget.MegaMekButton;
import megamek.common.BTObject;
import megamek.common.BoardLocation;
import megamek.common.InGameObject;
import megamek.common.TargetRoll;
import megamek.common.actions.EntityAction;
import megamek.common.actions.sbf.SBFStandardUnitAttack;
import megamek.common.alphaStrike.ASRange;
import megamek.common.annotations.Nullable;
import megamek.common.event.GameTurnChangeEvent;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.strategicBattleSystems.SBFFormationTurn;
import megamek.common.strategicBattleSystems.SBFGame;
import megamek.common.strategicBattleSystems.SBFToHitData;

public class SBFFiringDisplay extends SBFActionPhaseDisplay implements ListSelectionListener {

    private enum FiringCommand implements PhaseCommand {
        FIRE_NEXT("moveNext"),
        FIRE_PREVIOUS("movePrevious"),
        FIRE_MORE("MoveMore"),
        FIRE_UNIT("fireunit");

        private final String cmd;
        private final Predicate<SBFFormation> isEligible;
        private int priority;

        FiringCommand(String c) {
            this(c, formation -> true);
        }

        FiringCommand(String c, Predicate<SBFFormation> isEligible) {
            cmd = c;
            this.isEligible = isEligible;
            priority = ordinal();
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
            return Messages.getString("MovementDisplay." + getCmd());
        }
    }

    private final List<EntityAction> plannedActions = new ArrayList<>();
    private InGameObject selectedTarget;
    private int firingUnit = BTObject.NONE;
    private final SBFTargetDialog targetDialog;

    private final Map<FiringCommand, MegaMekButton> buttons = new HashMap<>();

    public SBFFiringDisplay(SBFClientGUI cg) {
        super(cg);
        setupStatusBar(Messages.getString("FiringDisplay.waitingForFiringPhase"));
        setButtons();
        setButtonsTooltips();
        setupButtonPanel();
        registerKeyCommands();
        game().addGameListener(this);
        //TODO: rather have clientgui take BVListeners and forward all events -> dont have to deal with changing
        // boardviews
        clientgui.boardViews().forEach(b -> b.addBoardViewListener(this));
        targetDialog = new SBFTargetDialog(getClientgui().getFrame(), game(), this);
    }

    @Override
    protected void updateDonePanel() {
        if (plannedActions.isEmpty()) {
            updateDonePanelButtons("Done", "Skip Firing", false, null);
        } else {
            updateDonePanelButtons("Fire", "Skip Firing", true, null);
        }
    }

    private void selectFormation(@Nullable SBFFormation formation) {
        if (formation == null) {
            currentFormation = SBFFormation.NONE;
            firingUnit = BTObject.NONE;
        } else {
            if (currentFormation == formation.getId()) {
                // Selection hasn't changed, do nothing
                return;
            }
            firingUnit = BTObject.NONE;
            currentFormation = formation.getId();
        }
        resetPlannedActions();
        clientgui.selectForAction(formation);
        updateTargetingData();
        updateDonePanel();
    }

    protected boolean shouldPerformClearKeyCommand() {
        return !clientgui.isChatBoxActive() && !isIgnoringEvents() && isVisible();
    }

    private void registerKeyCommands() {
        controller.registerCommandAction(KeyCommandBind.NEXT_UNIT, this, this::selectNextFormation);
        controller.registerCommandAction(KeyCommandBind.PREV_UNIT, this, this::selectPreviousFormation);
        controller.registerCommandAction(KeyCommandBind.CANCEL, this::shouldPerformClearKeyCommand, this::clear);
    }

    @Override
    protected List<MegaMekButton> getButtonList() {
        return new ArrayList<>(buttons.values());
    }

    @Override
    protected void setButtons() {
        for (FiringCommand cmd : FiringCommand.values()) {
            buttons.put(cmd, createButton(cmd.getCmd(), "MovementDisplay."));
        }
    }

    @Override
    protected void setButtonsTooltips() {

    }

    @Override
    public void clear() {
        resetPlannedActions();
        updateButtonStatus();
        updateDonePanel();
    }

    private void resetPlannedActions() {
        plannedActions.clear();
    }

    private void selectNextFormation() {
        clientgui.getClient().getGame().getNextEligibleFormation(currentFormation).ifPresent(this::selectFormation);
    }

    private void selectPreviousFormation() {
        clientgui.getClient().getGame().getPreviousEligibleFormation(currentFormation).ifPresent(this::selectFormation);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (isIgnoringEvents() || !isMyTurn()) {
            return;
        }

        final String actionCmd = e.getActionCommand();
        if (actionCmd.equals(FiringCommand.FIRE_NEXT.getCmd())) {
            selectNextFormation();
        } else if (actionCmd.equals(FiringCommand.FIRE_PREVIOUS.getCmd())) {
            selectPreviousFormation();
        } else if (actionCmd.equals(FiringCommand.FIRE_UNIT.cmd)) {
            fire();
        }
    }

    private void fire() {
        if (actingFormation().isEmpty() || !isMyTurn() || selectedTarget == null) {
            return;
        }
        var attack = new SBFStandardUnitAttack(actingFormation().get().getId(), firingUnit, selectedTarget.getId(), ASRange.LONG);
        plannedActions.add(attack);
        updateButtonStatus();
        updateDonePanel();
    }

    @Override
    public void ready() {
        Optional<SBFFormation> formation = game().getFormation(currentFormation);
        if (formation.isEmpty()) {
            return;
        }

        clientgui.getClient().sendAttackData(plannedActions, currentFormation);
        endMyTurn();
    }

    /**
     * Clears out old movement data and disables relevant buttons.
     */
    private void endMyTurn() {
        stopTimer();
        updateButtonStatus();
        selectFormation(null);
        hideTargetDialog();
    }

    @Override
    public void removeAllListeners() {
        game().removeGameListener(this);
        clientgui.boardViews().forEach(b -> b.removeBoardViewListener(this));
    }

    private void beginMyTurn() {
        initDonePanelForNewTurn();
        updateButtonStatus();
        if (GUIP.getAutoSelectNextUnit()) {
            clientgui.getClient().getGame().getNextEligibleFormation().ifPresent(this::selectFormation);
        }
//            clientgui.bingMyTurn();
        startTimer();
    }

    private SBFGame game() {
        return clientgui.getClient().getGame();
    }

    private void updateButtonStatus() {
        boolean myTurn = isMyTurn();
        boolean turnIsFormationTurn = game().getTurn() instanceof SBFFormationTurn;
        boolean hasAvailableUnits = turnIsFormationTurn && game().hasEligibleFormation((SBFFormationTurn) game().getTurn());
        boolean hasTarget = selectedTarget != null;

        buttons.get(FiringCommand.FIRE_NEXT).setEnabled(myTurn && hasAvailableUnits);
        buttons.get(FiringCommand.FIRE_MORE).setEnabled(myTurn && (numButtonGroups > 1));
        buttons.get(FiringCommand.FIRE_UNIT).setEnabled(myTurn && hasTarget && isFirePossible());
    }

    private boolean isFirePossible() {
        return actingFormation().isPresent()
                && (firingUnit >= 0)
                && (actingFormation().get().getUnits().size() > firingUnit)
                && actingFormation().get().isEligibleForPhase(game().getPhase())
                && !unitHasPlannedFire();
    }

    private boolean unitHasPlannedFire() {
        return plannedActions.stream()
                .filter(a -> a instanceof SBFStandardUnitAttack)
                .anyMatch(a -> ((SBFStandardUnitAttack) a).getUnitNumber() == firingUnit);
    }

    private boolean isMyTurn() {
        return clientgui.getClient().isMyTurn();
    }

    @Override
    public void gameTurnChange(GameTurnChangeEvent e) {
        if (isIgnoringEvents()) {
            return;
        }

        if (isMyTurn()) {
            setStatusBarText(Messages.getString("FiringDisplay.its_your_turn"));
            beginMyTurn();
        } else {
            setStatusBarText(Messages.getString("FiringDisplay.its_others_turn", playerNameOrUnknown(e.getPlayer())));
            endMyTurn();
        }
    }

    private void setTarget(@Nullable InGameObject target) {
        selectedTarget = target;
        updateButtonStatus();
    }

    @Override
    public void hexMoused(BoardViewEvent b) {
        if (isIgnoringEvents() || !isMyTurn() || (b.getButton() != MouseEvent.BUTTON1)) {
            return;
        }

        if (!game().getActiveFormationsAt(new BoardLocation(b.getCoords(), 0)).isEmpty()) {
            setTarget(game().getActiveFormationsAt(new BoardLocation(b.getCoords(), 0)).get(0));
        } else {
            setTarget(null);
        }
        updateTargetingData();
    }

    /**
     * Recalculates toHit from the current selections for attacker and target and updates the targeting
     * dialog accordingly.
     */
    private void updateTargetingData() {
        SBFToHitData toHitData = new SBFToHitData();
        if (selectedTarget == null) {
            toHitData.addModifier(TargetRoll.IMPOSSIBLE, "No target selected");
        } else if (firingUnit == BTObject.NONE) {
            toHitData.addModifier(TargetRoll.IMPOSSIBLE, "No Unit selected for firing");
        } else if (actingFormation().isEmpty()) {
            toHitData.addModifier(TargetRoll.IMPOSSIBLE, "No Formation selected for firing");
        } else {
            SBFFormation attacker = actingFormation().get();
            if (firingUnit >= attacker.getUnits().size() || firingUnit < 0) {
                toHitData.addModifier(TargetRoll.IMPOSSIBLE, "Invalid Unit");
            } else {
                toHitData = SBFToHitData.compiletoHit(game(),
                        new SBFStandardUnitAttack(attacker.getId(), firingUnit, selectedTarget.getId(), ASRange.LONG));
            }
        }
        showTargetDialog();
        targetDialog.setContent(game().getFormation(currentFormation).orElse(null), selectedTarget, toHitData);
    }

    public void showTargetDialog() {
        targetDialog.setVisible(true);
    }

    public void hideTargetDialog() {
        targetDialog.setVisible(false);
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting() && !isIgnoringEvents()) {
            firingUnit = e.getFirstIndex();
            updateTargetingData();
            updateButtonStatus();
        }
    }
}

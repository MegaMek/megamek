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

import megamek.client.event.BoardViewEvent;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.SBFClientGUI;
import megamek.client.ui.swing.util.KeyCommandBind;
import megamek.client.ui.swing.widget.MegamekButton;
import megamek.common.BoardLocation;
import megamek.common.InGameObject;
import megamek.common.actions.EntityAction;
import megamek.common.actions.sbf.SBFStandardUnitAttack;
import megamek.common.alphaStrike.ASRange;
import megamek.common.annotations.Nullable;
import megamek.common.event.GameTurnChangeEvent;
import megamek.common.strategicBattleSystems.*;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.function.Predicate;

public class SBFFiringDisplay extends SBFActionPhaseDisplay {

    private enum FiringCommand implements PhaseCommand {
        FIRE_NEXT("moveNext"),
        FIRE_PREVIOUS("movePrevious"),
        FIRE_MORE("MoveMore"),
        FIRE_UNIT1("fireunit1"),
        FIRE_UNIT2("fireunit2"),
        FIRE_UNIT3("fireunit3"),
        FIRE_UNIT4("fireunit4"),
        FIRE_UNIT5("fireunit5"),
        FIRE_UNIT6("fireunit6");

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

    private final Map<FiringCommand, MegamekButton> buttons = new HashMap<>();

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
            currentUnit = SBFFormation.NONE;
        } else {
            currentUnit = formation.getId();
        }
        resetPlannedActions();
        clientgui.selectForAction(game().getFormation(currentUnit).orElse(null));
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
    protected List<MegamekButton> getButtonList() {
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
        clientgui.getClient().getGame().getNextEligibleFormation(currentUnit).ifPresent(this::selectFormation);
    }

    private void selectPreviousFormation() {
        clientgui.getClient().getGame().getPreviousEligibleFormation(currentUnit).ifPresent(this::selectFormation);
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
        } else if (actionCmd.equals(FiringCommand.FIRE_UNIT1.cmd)) {
            fire(1);
        } else if (actionCmd.equals(FiringCommand.FIRE_UNIT2.cmd)) {
            fire(2);
        } else if (actionCmd.equals(FiringCommand.FIRE_UNIT3.cmd)) {
            fire(3);
        } else if (actionCmd.equals(FiringCommand.FIRE_UNIT4.cmd)) {
            fire(4);
        } else if (actionCmd.equals(FiringCommand.FIRE_UNIT5.cmd)) {
            fire(5);
        } else if (actionCmd.equals(FiringCommand.FIRE_UNIT6.cmd)) {
            fire(6);
        }
    }

    private void fire(int unit) {
        if (actingFormation().isEmpty() || !isMyTurn() || selectedTarget == null) {
            return;
        }
        var attack = new SBFStandardUnitAttack(actingFormation().get().getId(), unit, selectedTarget.getId(), ASRange.LONG);
        plannedActions.add(attack);
        updateButtonStatus();
        updateDonePanel();
    }

    @Override
    public void ready() {
        Optional<SBFFormation> formation = game().getFormation(currentUnit);
        if (formation.isEmpty() || plannedActions.isEmpty()) {
            return;
        }

        clientgui.getClient().sendAttackData(plannedActions, currentUnit);
        endMyTurn();
    }

    /**
     * Clears out old movement data and disables relevant buttons.
     */
    private void endMyTurn() {
        stopTimer();
        updateButtonStatus();
        selectFormation(null);
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
        if (actingFormation().isPresent() && myTurn) {
            SBFFormation f = actingFormation().get();
            buttons.get(FiringCommand.FIRE_UNIT1).setEnabled(!f.getUnits().isEmpty() && hasTarget && isUnitEligible(1));
            buttons.get(FiringCommand.FIRE_UNIT2).setEnabled(f.getUnits().size() >= 2 && hasTarget && isUnitEligible(2));
            buttons.get(FiringCommand.FIRE_UNIT3).setEnabled(f.getUnits().size() >= 3 && hasTarget && isUnitEligible(3));
            buttons.get(FiringCommand.FIRE_UNIT4).setEnabled(f.getUnits().size() >= 4 && hasTarget && isUnitEligible(4));
            buttons.get(FiringCommand.FIRE_UNIT5).setEnabled(f.getUnits().size() >= 5 && hasTarget && isUnitEligible(5));
            buttons.get(FiringCommand.FIRE_UNIT6).setEnabled(f.getUnits().size() == 6 && hasTarget && isUnitEligible(6));
        }
    }

    private boolean isUnitEligible(int unit) {
        return actingFormation().isPresent() && actingFormation().get().getUnits().size() >= unit
                && plannedActions.stream()
                .filter(a -> a instanceof SBFStandardUnitAttack)
                .noneMatch(a -> ((SBFStandardUnitAttack) a).getUnitNumber() == unit);
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

//        String s = getRemainingPlayerWithTurns();
//        setStatusBarText(s);

        // if all our entities are actually done, don't start up the turn.
//        if (clientgui.getClient().getGame().getPlayerEntities(clientgui.getClient().getLocalPlayer(), false)
//                .stream().allMatch(Entity::isDone)) {
//            setStatusBarTextOthersTurn(e.getPlayer(), s);
//            clientgui.bingOthersTurn();
//            return;
//        }
//        String playerName;
//
//        if (e.getPlayer() != null) {
//            playerName = e.getPlayer().getName();
//        } else {
//            playerName = "Unknown";
//        }
//        if (isMyTurn()) {
//            setStatusBarText(Messages.getString("FiringDisplay.its_your_turn"));
//            if (currentUnit == SBFFormation.NONE) {
//                setStatusBarText(Messages.getString("FiringDisplay.its_your_turn") + s);
//            }
//            beginMyTurn();
//        } else {
//            setStatusBarText(Messages.getString("FiringDisplay.its_others_turn", playerNameOrUnknown(e.getPlayer())));
//            endMyTurn();
//            setStatusBarText(Messages.getString("FiringDisplay.its_others_turn", playerName) + s);
//            clientgui.bingOthersTurn();
//        }
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
            clientgui.showTargetDialog(selectedTarget);
        }
    }
}

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
        MOVE_MORE("MoveMore");

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

    private final Map<FiringCommand, MegamekButton> buttons = new HashMap<>();

    private SBFMovePath plannedMovement;

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

    }

    private void selectFormation(@Nullable SBFFormation formation) {
        if (formation == null) {
            currentUnit = SBFFormation.NONE;
//            clientgui.clearMovementEnvelope();
        } else {
            currentUnit = formation.getId();
//            if (isMyTurn() && GUIP.getMoveEnvelope()) {
//                computeMovementEnvelope(formation);
//            }
        }
//        resetPlannedMovement();
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

    /**
     * Resets the planned movement for the current formation, if any. This also validates the current
     * formation and does some extra checks to avoid errors.
     */
//    private void resetPlannedMovement() {
//        if (currentUnit == SBFFormation.NONE || game().getFormation(currentUnit).isEmpty()) {
//            currentUnit = SBFFormation.NONE;
//            plannedMovement = null;
//        } else {
//            SBFFormation formation = game().getFormation(currentUnit).get();
//            if (!formation.isDeployed() || formation.getPosition() == null) {
//                plannedMovement = null;
//            } else {
//                plannedMovement = new SBFMovePath(currentUnit, formation.getPosition(), game());
//            }
//        }
//        clientgui.selectForAction(game().getFormation(currentUnit).orElse(null));
//        clientgui.showMovePath(plannedMovement);
//    }

    @Override
    public void clear() {
//        resetPlannedMovement();
        updateButtonStatus();
        updateDonePanel();
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
        }
    }

    @Override
    public void ready() {
        Optional<SBFFormation> formation = game().getFormation(currentUnit);
        if (formation.isEmpty() || plannedMovement == null) {
            return;
        }

        clientgui.getClient().moveUnit(plannedMovement);
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

        buttons.get(FiringCommand.FIRE_NEXT).setEnabled(myTurn && hasAvailableUnits);
        buttons.get(FiringCommand.MOVE_MORE).setEnabled(myTurn && (numButtonGroups > 1));
    }

    private boolean isMyTurn() {
        return clientgui.getClient().isMyTurn();
    }

    @Override
    public void gameTurnChange(GameTurnChangeEvent e) {
        if (isIgnoringEvents() || !clientgui.getClient().getGame().getPhase().isMovement()) {
            return;
        }

        String s = getRemainingPlayerWithTurns();
        setStatusBarText(s);

        // if all our entities are actually done, don't start up the turn.
//        if (clientgui.getClient().getGame().getPlayerEntities(clientgui.getClient().getLocalPlayer(), false)
//                .stream().allMatch(Entity::isDone)) {
//            setStatusBarTextOthersTurn(e.getPlayer(), s);
//            clientgui.bingOthersTurn();
//            return;
//        }

        if (isMyTurn()) {
            if (currentUnit == SBFFormation.NONE) {
                setStatusBarText(Messages.getString("FiringDisplay.its_your_turn") + s);
            }
            beginMyTurn();
        } else {
            endMyTurn();
//            if ((e.getPlayer() == null)
//                    && (clientgui.getClient().getGame().getTurn() instanceof UnloadStrandedTurn)) {
//                setStatusBarText(Messages.getString("MovementDisplay.waitForAnother") + s);
//            } else {
//                setStatusBarTextOthersTurn(e.getPlayer(), s);
//            }
//            clientgui.bingOthersTurn();
        }
    }

    @Override
    public void hexMoused(BoardViewEvent b) {
        if (isIgnoringEvents() || !isMyTurn() || (b.getButton() != MouseEvent.BUTTON1)) {
            return;
        }

//        currentMove(b.getCoords());
    }

}

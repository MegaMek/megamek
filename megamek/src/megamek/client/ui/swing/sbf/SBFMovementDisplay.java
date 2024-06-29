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

import megamek.client.ui.Messages;
import megamek.client.ui.swing.*;
import megamek.client.ui.swing.util.KeyCommandBind;
import megamek.client.ui.swing.widget.MegamekButton;
import megamek.common.BTObject;
import megamek.common.annotations.Nullable;
import megamek.common.event.GameTurnChangeEvent;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.strategicBattleSystems.SBFFormationTurn;
import megamek.common.strategicBattleSystems.SBFGame;

import java.awt.event.ActionEvent;
import java.util.*;
import java.util.function.Predicate;

public class SBFMovementDisplay extends SBFActionPhaseDisplay {

    private enum MoveCommand implements PhaseCommand {
        MOVE_NEXT("moveNext"),
        MOVE_PREVIOUS("movePrevious"),
        MOVE_WALK("moveWalk", BTObject::isGround),
        MOVE_FLEE("moveFlee"),
        MOVE_MORE("MoveMore");

        private final String cmd;
        private final Predicate<SBFFormation> isEligible;
        private int priority;

        MoveCommand(String c) {
            this(c, formation -> true);
        }

        MoveCommand(String c, Predicate<SBFFormation> isEligible) {
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

    private final Map<MoveCommand, MegamekButton> buttons = new HashMap<>();

    public SBFMovementDisplay(SBFClientGUI cg) {
        super(cg);
        setupStatusBar(Messages.getString("MovementDisplay.waitingForMovementPhase"));
        setButtons();
        setButtonsTooltips();
        setupButtonPanel();
        registerKeyCommands();
        game().addGameListener(this);
    }

    @Override
    protected void updateDonePanel() {

    }

    private void deselect() {
        selectFormation(null);
    }

    private void selectFormation(@Nullable SBFFormation formation) {
        if (formation == null) {
            currentUnit = SBFFormation.NONE;
        } else {
            currentUnit = formation.getId();
        }
        clientgui.selectForAction(formation);
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
        for (MoveCommand cmd : MoveCommand.values()) {
            buttons.put(cmd, createButton(cmd.getCmd(), "MovementDisplay."));
        }
    }

    @Override
    protected void setButtonsTooltips() {

    }

    @Override
    public void clear() {

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
        if (actionCmd.equals(MoveCommand.MOVE_NEXT.getCmd())) {
            selectNextFormation();
        } else if (actionCmd.equals(MoveCommand.MOVE_PREVIOUS.getCmd())) {
            selectPreviousFormation();
        }
    }

    @Override
    public void ready() {

    }

    @Override
    public void removeAllListeners() {

    }

    /**
     * Enables relevant buttons and sets up for your turn.
     */
    private void beginMyTurn() {
        initDonePanelForNewTurn();
        updateButtonStatus();
        if (GUIP.getAutoSelectNextUnit()) {
            clientgui.getClient().getGame().getNextEligibleFormation().ifPresent(this::selectFormation);
        }

        startTimer();
    }

    private SBFGame game() {
        return clientgui.getClient().getGame();
    }

    private void updateButtonStatus() {
        boolean myTurn = isMyTurn();
        boolean turnIsFormationTurn = game().getTurn() instanceof SBFFormationTurn;
        boolean hasAvailableUnits = turnIsFormationTurn && game().hasEligibleFormation((SBFFormationTurn) game().getTurn());

        buttons.get(MoveCommand.MOVE_NEXT).setEnabled(myTurn && hasAvailableUnits);
        buttons.get(MoveCommand.MOVE_MORE).setEnabled(myTurn && (numButtonGroups > 1));
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
                setStatusBarText(Messages.getString("MovementDisplay.its_your_turn") + s);
                beginMyTurn();
            }
//            clientgui.bingMyTurn();
        } else {
//            endMyTurn();
//            if ((e.getPlayer() == null)
//                    && (clientgui.getClient().getGame().getTurn() instanceof UnloadStrandedTurn)) {
//                setStatusBarText(Messages.getString("MovementDisplay.waitForAnother") + s);
//            } else {
//                setStatusBarTextOthersTurn(e.getPlayer(), s);
//            }
//            clientgui.bingOthersTurn();
        }
    }
}

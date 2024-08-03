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
import megamek.client.ui.swing.*;
import megamek.client.ui.swing.util.KeyCommandBind;
import megamek.client.ui.swing.widget.MegamekButton;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.event.GameTurnChangeEvent;
import megamek.common.pathfinder.AbstractPathFinder;
import megamek.common.preference.PreferenceManager;
import megamek.common.strategicBattleSystems.*;
import org.apache.logging.log4j.LogManager;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
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

    private SBFMovePath plannedMovement;

    public SBFMovementDisplay(SBFClientGUI cg) {
        super(cg);
        setupStatusBar(Messages.getString("MovementDisplay.waitingForMovementPhase"));
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
            clientgui.clearMovementEnvelope();
        } else {
            currentUnit = formation.getId();
            if (isMyTurn() && GUIP.getMoveEnvelope()) {
                computeMovementEnvelope(formation);
            }
        }
        resetPlannedMovement();
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

    /**
     * Resets the planned movement for the current formation, if any. This also validates the current
     * formation and does some extra checks to avoid errors.
     */
    private void resetPlannedMovement() {
        if (currentUnit == SBFFormation.NONE || game().getFormation(currentUnit).isEmpty()) {
            currentUnit = SBFFormation.NONE;
            plannedMovement = null;
        } else {
            SBFFormation formation = game().getFormation(currentUnit).get();
            if (!formation.isDeployed() || formation.getPosition() == null) {
                plannedMovement = null;
            } else {
                plannedMovement = new SBFMovePath(currentUnit, formation.getPosition(), game());
            }
        }
        clientgui.selectForAction(game().getFormation(currentUnit).orElse(null));
        clientgui.showMovePath(plannedMovement);
    }

    @Override
    public void clear() {
        resetPlannedMovement();
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
        if (actionCmd.equals(MoveCommand.MOVE_NEXT.getCmd())) {
            selectNextFormation();
        } else if (actionCmd.equals(MoveCommand.MOVE_PREVIOUS.getCmd())) {
            selectPreviousFormation();
        }
    }

    @Override
    public void ready() {
        Optional<SBFFormation> formation = game().getFormation(currentUnit);
        if (formation.isEmpty() || plannedMovement == null) {
            return;
        }

//        plannedMovement.clip;
//        cmd.clipToPossible();

//        if (checkNags()) {
//            return;
//        }

//        disableButtons();

//        clientgui.clearTemporarySprites();
//        clientgui.getBoardView().clearMovementData();

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
        resetPlannedMovement();
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

    /**
     * Computes all of the possible moves for an Entity in a particular gear. The Entity can either
     * be a suggested Entity or the currently selected one. If there is a selected entity (which
     * implies it's the current players turn), then the current gear is used (which is set by the
     * user). If there is no selected entity, then the current gear is invalid, and it defaults to
     * GEAR_LAND (standard "walk forward").
     *
     */
    public void computeMovementEnvelope(SBFFormation formation) {
        if ((formation == null) || (formation.getPosition() == null) || !formation.isDeployed()) {
            return;
        }
//        if (en.isDone()) {
//            return;
//        }

        Map<BoardLocation, SBFMovePath> mvEnvData;
        SBFMovePath mp = new SBFMovePath(formation.getId(), formation.getPosition(), game());

        int maxMP = formation.getMovement();
        if (game().usesSprintingMove()) {
            maxMP *= 1.5;
        }

        SBFMovePathFinder pathFinder = SBFMovePathFinder.moveEnvelopeFinder(maxMP, game());
        pathFinder.run(mp);
        mvEnvData = pathFinder.getAllComputedPaths();
        Map<Coords, Integer> mvEnvMP = new HashMap<>();
        for (BoardLocation c : mvEnvData.keySet()) {
            mvEnvMP.put(c.getCoords(), mvEnvData.get(c).getMpUsed());
        }
        clientgui.showMovementEnvelope(formation, mvEnvMP);
    }

    @Override
    public void hexMoused(BoardViewEvent b) {
        if (isIgnoringEvents() || !isMyTurn() || (b.getButton() != MouseEvent.BUTTON1)) {
            return;
        }

        currentMove(b.getCoords());
    }

    /**
     * Returns new MovePath for the currently selected movement type
     */
    private void currentMove(Coords dest) {
        findPathTo(new BoardLocation(dest, 0), plannedMovement);
    }

    /**
     * Extend the current path to the destination <code>Coords</code>.
     *
     * @param dest the destination <code>Coords</code> of the move.
     */
    public void findPathTo(final BoardLocation dest, SBFMovePath currentPath) {
        if (currentUnit == SBFFormation.NONE) {
            return;
        }
        if (currentPath == null) {
            currentPath = new SBFMovePath(currentUnit, game().getFormation(currentUnit).get().getPosition(), game());
        }
        final int timeLimit = PreferenceManager.getClientPreferences().getMaxPathfinderTime();
        SBFMovePathFinder pf = SBFMovePathFinder.aStarFinder(dest, game());
        AbstractPathFinder.StopConditionTimeout<SBFMovePath> timeoutCondition =
                new AbstractPathFinder.StopConditionTimeout<>(timeLimit);
        pf.addStopCondition(timeoutCondition);
        pf.run(SBFMovePath.createMovePathShallow(currentPath));
        SBFMovePath finPath = pf.getComputedPath(dest);

        if (finPath != null) {
            plannedMovement = finPath;
            clientgui.showMovePath(plannedMovement);
        } else {
            resetPlannedMovement();
            LogManager.getLogger().error("Unable to find a move path for formation {} to {}!", currentUnit, dest);
        }
    }
}
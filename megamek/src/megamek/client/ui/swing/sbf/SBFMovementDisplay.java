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
            if (isMyTurn() && GUIP.getMoveEnvelope()) {
                computeMovementEnvelope(formation);
            }
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

    /**
     * Computes all of the possible moves for an Entity in a particular gear. The Entity can either
     * be a suggested Entity or the currently selected one. If there is a selected entity (which
     * implies it's the current players turn), then the current gear is used (which is set by the
     * user). If there is no selected entity, then the current gear is invalid, and it defaults to
     * GEAR_LAND (standard "walk forward").
     *
     */
    public void computeMovementEnvelope(SBFFormation formation) {
        if (formation == null || formation.getPosition() == null || !formation.isDeployed()) {
            return;
        }
//        if (en.isDone()) {
//            return;
//        }

        Map<BoardLocation, SBFMovePath> mvEnvData = new HashMap<>();
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

//            LongestPathFinder lpf;
//            if (ce().isAero()) {
//                lpf = LongestPathFinder.newInstanceOfAeroPath(maxMp, ce().getGame());
//            } else {
//                lpf = LongestPathFinder.newInstanceOfLongestPath(maxMp, stepType, ce().getGame());
//            }
//            final int timeLimit = PreferenceManager.getClientPreferences().getMaxPathfinderTime();
//            lpf.addStopCondition(new AbstractPathFinder.StopConditionTimeout<>(timeLimit * 4));
//
//            lpf.run(cmd);
//            MovePath lPath = lpf.getComputedPath(dest);
//            if (lPath != null) {
//                cmd = lPath;
//            }
    }

//        clientgui.showSensorRanges(ce(), cmd.getFinalCoords());
//        clientgui.updateFiringArc(ce());


    /**
     * Extend the current path to the destination <code>Coords</code>.
     *
     * @param dest the destination <code>Coords</code> of the move.
     */
    public void findPathTo(final BoardLocation dest, SBFMovePath currentPath) {
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
        // this can be used for debugging the "destruction aware pathfinder"
        //MovePath finPath = calculateDestructionAwarePath(dest);

//        if (timeoutCondition.timeoutEngaged || finPath == null) {
//            /*
//             * Either we have forced searcher to end prematurely or no path was
//             * found. Lets try to fix it by taking the path that ended closest
//             * to the target and greedily extend it.
//             */
//            MovePath bestMp = Collections.min(pf.getAllComputedPaths().values(), new ShortestPathFinder.MovePathGreedyComparator(dest));
//            pf = ShortestPathFinder.newInstanceOfGreedy(dest, type, game);
//            pf.run(bestMp);
//            finPath = pf.getComputedPath(dest);
//            // If no path could be found, use the best one returned by A*
//            if (finPath == null) {
//                finPath = bestMp;
//            }
//        }

        if (finPath != null) {
            plannedMovement = finPath;
//            finPath.compile();
//            this.steps = finPath.steps;
        } else {
//            LogManager.getLogger().error("Unable to find a path to the destination hex! \tMoving "
//                    + getEntity() + "from " + getFinalCoords() + " to " + dest);
        }

    }
}
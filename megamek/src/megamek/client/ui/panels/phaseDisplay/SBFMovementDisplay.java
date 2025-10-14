/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import megamek.client.event.BoardViewEvent;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.SBFClientGUI;
import megamek.client.ui.dialogs.phaseDisplay.SBFJumpChoiceDialog;
import megamek.client.ui.enums.DialogResult;
import megamek.client.ui.util.KeyCommandBind;
import megamek.client.ui.widget.MegaMekButton;
import megamek.common.annotations.Nullable;
import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import megamek.common.event.GameTurnChangeEvent;
import megamek.common.pathfinder.StopConditionTimeout;
import megamek.common.preference.PreferenceManager;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.strategicBattleSystems.SBFFormationTurn;
import megamek.common.strategicBattleSystems.SBFGame;
import megamek.common.strategicBattleSystems.SBFMovePath;
import megamek.common.strategicBattleSystems.SBFMovePathFinder;
import megamek.common.units.BTObject;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

public class SBFMovementDisplay extends SBFActionPhaseDisplay {
    private static final MMLogger logger = MMLogger.create(SBFMovementDisplay.class);

    private enum MoveCommand implements PhaseCommand {
        MOVE_NEXT("moveNext"),
        MOVE_PREVIOUS("movePrevious"),
        MOVE_WALK("moveWalk", BTObject::isGround),
        MOVE_FLEE("moveFlee"),
        MOVE_MORE("MoveMore");

        private final String cmd;
        private int priority;

        MoveCommand(String c) {
            this(c, formation -> true);
        }

        MoveCommand(String c, Predicate<SBFFormation> isEligible) {
            cmd = c;
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

    private final Map<MoveCommand, MegaMekButton> buttons = new HashMap<>();

    private SBFMovePath plannedMovement;

    public SBFMovementDisplay(SBFClientGUI cg) {
        super(cg);
        setupStatusBar(Messages.getString("MovementDisplay.waitingForMovementPhase"));
        setButtons();
        setButtonsTooltips();
        setupButtonPanel();
        registerKeyCommands();
        game().addGameListener(this);
        // TODO: rather have clientGUI take BVListeners and forward all events -> dont have to deal with changing
        //  BoardViews
        clientGUI.boardViews().forEach(b -> b.addBoardViewListener(this));
    }

    @Override
    protected void updateDonePanel() {
        if (plannedMovement == null || plannedMovement.getSteps().isEmpty()) {
            updateDonePanelButtons("Done", "Skip Movement", false, null);
        } else {
            updateDonePanelButtons("Move", "Skip Movement", true, null);
        }
    }

    private void selectFormation(@Nullable SBFFormation formation) {
        if (formation == null) {
            currentFormation = SBFFormation.NONE;
            clientGUI.clearMovementEnvelope();
        } else {
            currentFormation = formation.getId();
            if (isMyTurn() && GUIP.getMoveEnvelope()) {
                computeMovementEnvelope(formation);
            }
        }
        resetPlannedMovement();
    }

    protected boolean shouldPerformClearKeyCommand() {
        return !clientGUI.isChatBoxActive() && !isIgnoringEvents() && isVisible();
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
        for (MoveCommand cmd : MoveCommand.values()) {
            buttons.put(cmd, createButton(cmd.getCmd(), "MovementDisplay."));
        }
    }

    @Override
    protected void setButtonsTooltips() {

    }

    /**
     * Resets the planned movement for the current formation, if any. This also validates the current formation and does
     * some extra checks to avoid errors.
     */
    private void resetPlannedMovement() {
        if (currentFormation == SBFFormation.NONE || game().getFormation(currentFormation).isEmpty()) {
            currentFormation = SBFFormation.NONE;
            plannedMovement = null;
        } else {
            SBFFormation formation = game().getFormation(currentFormation).get();
            if (!formation.isDeployed() || formation.getPosition() == null) {
                plannedMovement = null;
            } else {
                plannedMovement = new SBFMovePath(currentFormation, formation.getPosition(), game());
            }
        }
        clientGUI.selectForAction(game().getFormation(currentFormation).orElse(null));
        clientGUI.showMovePath(plannedMovement);
        updateDonePanel();
    }

    @Override
    public void clear() {
        resetPlannedMovement();
        updateButtonStatus();
        updateDonePanel();
    }

    private void selectNextFormation() {
        clientGUI.getClient().getGame().getNextEligibleFormation(currentFormation).ifPresent(this::selectFormation);
    }

    private void selectPreviousFormation() {
        clientGUI.getClient().getGame().getPreviousEligibleFormation(currentFormation).ifPresent(this::selectFormation);
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
        Optional<SBFFormation> formation = game().getFormation(currentFormation);
        if (formation.isEmpty() || plannedMovement == null) {
            return;
        }

        if (plannedMovement.getSteps().isEmpty() || planJump(formation.get()).isConfirmed()) {
            clientGUI.getClient().moveUnit(plannedMovement);
            endMyTurn();
        }
    }

    private DialogResult planJump(SBFFormation formation) {
        // TODO SBF RULES Can you use JUMP if you remain in the hex?
        if (formation.getJumpMove() > 0) {
            List<Integer> choices = Stream.iterate(0, n -> n + 1).limit(formation.getJumpMove() + 1).toList();
            SBFJumpChoiceDialog jumpChoiceDialog = new SBFJumpChoiceDialog(clientGUI.getFrame(), choices);
            jumpChoiceDialog.setLocationRelativeTo(clientGUI.getFrame());
            jumpChoiceDialog.pack();
            DialogResult result = jumpChoiceDialog.showDialog();
            if (result.isConfirmed()) {
                plannedMovement.setJumpUsed(jumpChoiceDialog.getFirstChoice());
            }
            return result;
        } else {
            plannedMovement.setJumpUsed(0);
            return DialogResult.CONFIRMED;
        }
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
        clientGUI.boardViews().forEach(b -> b.removeBoardViewListener(this));
    }

    private void beginMyTurn() {
        initDonePanelForNewTurn();
        updateButtonStatus();
        if (GUIP.getAutoSelectNextUnit()) {
            clientGUI.getClient().getGame().getNextEligibleFormation().ifPresent(this::selectFormation);
        }
        startTimer();
    }

    private SBFGame game() {
        return clientGUI.getClient().getGame();
    }

    private void updateButtonStatus() {
        boolean myTurn = isMyTurn();
        boolean turnIsFormationTurn = (game().getTurn() instanceof SBFFormationTurn);
        boolean hasAvailableUnits = turnIsFormationTurn &&
              game().hasEligibleFormation((SBFFormationTurn) game().getTurn());

        buttons.get(MoveCommand.MOVE_NEXT).setEnabled(myTurn && hasAvailableUnits);
        buttons.get(MoveCommand.MOVE_MORE).setEnabled(myTurn && (numButtonGroups > 1));
    }

    @Override
    public void gameTurnChange(GameTurnChangeEvent e) {
        if (isIgnoringEvents()) {
            return;
        }

        if (isMyTurn()) {
            setStatusBarText(Messages.getString("MovementDisplay.its_your_turn"));
            beginMyTurn();
        } else {
            setStatusBarText(Messages.getString("MovementDisplay.its_others_turn", playerNameOrUnknown(e.getPlayer())));
            endMyTurn();
        }
    }

    /**
     * Computes all the possible moves for an {@link Entity} in a particular gear. The {@link Entity} can either be a
     * suggested {@link Entity} or the currently selected one. If there is a selected {@link Entity} (which implies it's
     * the current players turn), then the current gear is used (which is set by the user). If there is no selected
     * {@link Entity}, then the current gear is invalid, and it defaults to GEAR_LAND (standard "walk forward").
     */
    public void computeMovementEnvelope(SBFFormation formation) {
        if ((formation == null) || (formation.getPosition() == null) || !formation.isDeployed()) {
            return;
        }

        Map<BoardLocation, SBFMovePath> mvEnvData;
        SBFMovePath mp = new SBFMovePath(formation.getId(), formation.getPosition(), game());

        int maxMP = formation.getMovement();
        // TO:AR PG 18 - Forth Printing.
        if (game().usesSprintingMove()) {
            double sprintingMovementPoints = maxMP * 1.5;
            maxMP = (int) Math.floor(sprintingMovementPoints);
        }

        SBFMovePathFinder pathFinder = SBFMovePathFinder.moveEnvelopeFinder(maxMP, game());
        pathFinder.run(mp);
        mvEnvData = pathFinder.getAllComputedPaths();
        Map<Coords, Integer> mvEnvMP = new HashMap<>();

        for (BoardLocation c : mvEnvData.keySet()) {
            mvEnvMP.put(c.coords(), mvEnvData.get(c).getMpUsed());
        }

        clientGUI.showMovementEnvelope(formation, mvEnvMP);
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
        findPathTo(BoardLocation.of(dest, 0), plannedMovement);
    }

    /**
     * Extend the current path to the destination <code>Coords</code>.
     *
     * @param dest the destination <code>Coords</code> of the move.
     */
    public void findPathTo(final BoardLocation dest, SBFMovePath currentPath) {
        if (currentFormation == SBFFormation.NONE) {
            return;
        }

        // check if currentPath is null and formation is present. If neither, confirm if currentPath is null and
        // escape out.
        if (currentPath == null && game().getFormation(currentFormation).isPresent()) {
            currentPath = new SBFMovePath(currentFormation,
                  game().getFormation(currentFormation).get().getPosition(),
                  game());
        } else if (currentPath == null) {
            return;
        }

        final int timeLimit = PreferenceManager.getClientPreferences().getMaxPathfinderTime();
        SBFMovePathFinder pf = SBFMovePathFinder.aStarFinder(dest, game());
        StopConditionTimeout<SBFMovePath> timeoutCondition = new StopConditionTimeout<>(
              timeLimit);
        pf.addStopCondition(timeoutCondition);
        pf.run(SBFMovePath.createMovePathShallow(currentPath));
        SBFMovePath finPath = pf.getComputedPath(dest);

        if (finPath != null) {
            plannedMovement = finPath;
            clientGUI.showMovePath(plannedMovement);
        } else {
            resetPlannedMovement();
            logger.error("Unable to find a move path for formation {} to {}!", currentFormation, dest);
        }
        updateDonePanel();
    }
}

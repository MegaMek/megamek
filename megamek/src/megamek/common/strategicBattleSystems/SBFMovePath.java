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
package megamek.common.strategicBattleSystems;

import static java.util.stream.Collectors.toSet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import megamek.common.BoardLocation;
import megamek.common.Player;
import megamek.common.actions.EntityAction;
import megamek.logging.MMLogger;

public class SBFMovePath implements EntityAction, Serializable {
    private static final MMLogger logger = MMLogger.create(SBFMovePath.class);

    private final int formationId;
    private final List<SBFMoveStep> steps = new ArrayList<>();
    private final BoardLocation startLocation;
    private boolean isIllegal;
    private int jumpUsed = 0;

    // The game is used mainly durinng creation of the movepath and shouldn't be
    // sent in packets
    private transient SBFGame game;

    public SBFMovePath(int formationId, BoardLocation startLocation, SBFGame game) {
        this.formationId = formationId;
        this.startLocation = startLocation;
        this.game = game;
    }

    /**
     * Creates a new move path that is a copy of the given original. Note that the
     * steps are not copied,
     * i.e. the step list is only a shallow copy!
     *
     * @param original The move path to copy
     * @return A new move path that is equal to the original
     */
    public static SBFMovePath createMovePathShallow(SBFMovePath original) {
        SBFMovePath newPath = new SBFMovePath(original.formationId, original.startLocation, original.game);
        newPath.steps.addAll(original.steps);
        return newPath;
    }

    /**
     * Creates a new move path that is a copy of the given original. Note that the
     * steps are copied,
     * i.e. the step list is a deep copy. The returned move path is completely
     * independent from the
     * original.
     *
     * @param original The move path to copy
     * @return A new move path that is equal to the original
     */
    public static SBFMovePath createMovePathDeep(SBFMovePath original) {
        SBFMovePath newPath = new SBFMovePath(original.formationId, original.startLocation, original.game);
        newPath.steps.addAll(original.steps.stream().map(SBFMoveStep::copy).collect(Collectors.toList()));
        return newPath;
    }

    @Override
    public int getEntityId() {
        return formationId;
    }

    public int getMpUsed() {
        return steps.stream().mapToInt(SBFMoveStep::getMpUsed).sum();
    }

    public void addStep(SBFMoveStep step) {
        steps.add(step);
        compile();
    }

    public SBFMoveStep getLastStep() {
        return steps.get(steps.size() - 1);
    }

    public BoardLocation getLastPosition() {
        return steps.isEmpty() ? startLocation : getLastStep().getLastPosition();
    }

    public boolean isIllegal() {
        return isIllegal;
    }

    /**
     * Assembles and computes all data for this move path, especially if it is
     * legal.
     */
    private void compile() {
        if (game == null) {
            logger.error("Trying to compile but game is null. Call restore after serialization!");
        }

        SBFFormation formation = game.getFormation(formationId).orElseThrow();

        isIllegal = steps.stream().anyMatch(s -> s.isIllegal);
        isIllegal |= getMpUsed() > formation.getMovement();

        // may not leave after entering hostile hex
        for (SBFMoveStep step : steps) {
            if (game.isHostileActiveFormationAt(step.startingPoint, formation)
                    && !step.startingPoint.equals(step.destination)
                    && !startLocation.equals(step.startingPoint)) {
                isIllegal = true;
            }
        }

        // stacking friendly at end of turn
        Player owner = game.getPlayer(formation.getOwnerId());
        List<SBFFormation> friendliesAtDestination = game.getActiveFormationsAt(getLastPosition()).stream()
                .filter(f -> !game.areHostile(f, owner))
                .toList();

        Set<SBFElementType> friendliesTypes = friendliesAtDestination.stream()
                .map(SBFFormation::getType).collect(toSet());
        isIllegal |= friendliesAtDestination.size() > 2;
        isIllegal |= (friendliesAtDestination.size() == 2) && !friendliesTypes.contains(SBFElementType.CI)
                && !friendliesTypes.contains(SBFElementType.BA);
    }

    /**
     * Restores the move path after serialization. This is unnecessary unless the
     * {@link #compile()}
     * method is used.
     *
     * @param game The SBFGame
     */
    public void restore(SBFGame game) {
        this.game = game;
    }

    /**
     * Returns the number of hexes moved
     */
    public int getHexesMoved() {
        return startLocation.coords().distance(getLastPosition().coords());
    }

    @Override
    public String toString() {
        return "[SBFMovePath]: ID: " + formationId + "; steps: " + steps;
    }

    /**
     * @return The steps of this move path as an unmodifiable list.
     */
    public List<SBFMoveStep> getSteps() {
        return Collections.unmodifiableList(steps);
    }

    public boolean isEndStep(SBFMoveStep step) {
        return (step != null) && steps.contains(step) && step.destination.equals(getLastPosition());
    }

    /**
     * Returns the number of mp used up to and including the given step. Returns -1
     * if the step is
     * not part of this move path.
     *
     * @param step The last step to include in the cost
     * @return The total mp up to the given step
     */
    public int getMpUpTo(SBFMoveStep step) {
        if (steps.contains(step)) {
            int mpUsed = 0;
            for (SBFMoveStep step2 : steps) {
                mpUsed += step2.getMpUsed();
                if (step.equals(step2)) {
                    return mpUsed;
                }
            }
        }
        logger.error("Tried to find the mp used with a step that is not part of this move path!");
        return -1;
    }

    public void setJumpUsed(int jumpUsed) {
        this.jumpUsed = jumpUsed;
    }

    public int getJumpUsed() {
        return jumpUsed;
    }
}

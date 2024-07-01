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

import megamek.common.BoardLocation;
import megamek.common.Player;
import megamek.common.actions.EntityAction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

public class SBFMovePath implements EntityAction, Serializable {

    private final int formationId;
    private final List<SBFMoveStep> steps = new ArrayList<>();
    private final BoardLocation startLocation;
    private boolean isIllegal;

    // The game is used mainly durinng creation of the movepath and shouldn't be sent in packets
    private transient SBFGame game;

    public SBFMovePath(int formationId, BoardLocation startLocation, SBFGame game) {
        this.formationId = formationId;
        this.startLocation = startLocation;
        this.game = game;
    }

    /**
     * Creates a new move path that is a copy of the given original. Note that the steps are not copied,
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
     * Creates a new move path that is a copy of the given original. Note that the steps are copied,
     * i.e. the step list is a deep copy. The returned move path is completely independent from the
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
     * Assembles and computes all data for this move path, especially if it is legal.
     */
    private void compile() {
        isIllegal = steps.stream().anyMatch(s -> s.isIllegal);

        // may not leave after entering hostile hex
        SBFFormation formation = game.getFormation(formationId).orElseThrow();
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
                .collect(toList());

        Set<SBFElementType> friendliesTypes = friendliesAtDestination.stream()
                .map(SBFFormation::getType).collect(toSet());
        isIllegal |= friendliesAtDestination.size() > 2;
        isIllegal |= (friendliesAtDestination.size() == 2) && !friendliesTypes.contains(SBFElementType.CI)
                && !friendliesTypes.contains(SBFElementType.BA);
    }

    /**
     * Restores the move path after serialization.
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
        return startLocation.getCoords().distance(getLastPosition().getCoords());
    }
}

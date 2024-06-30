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
import megamek.common.actions.EntityAction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SBFMovePath implements EntityAction, Serializable {

    private final int formationId;
    private final List<SBFMoveStep> steps = new ArrayList<>();
    private final BoardLocation startLocation;


    public SBFMovePath(int formationId, BoardLocation startLocation) {
        this.formationId = formationId;
        this.startLocation = startLocation;
    }

    /**
     * Creates a new move path that is a copy of the given original. Note that the steps are not copied,
     * i.e. the step list is only a shallow copy!
     *
     * @param original The move path to copy
     * @return A new move path that is equal to the original
     */
    public static SBFMovePath createMovePathShallow(SBFMovePath original) {
        SBFMovePath newPath = new SBFMovePath(original.formationId, original.startLocation);
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
        SBFMovePath newPath = new SBFMovePath(original.formationId, original.startLocation);
        newPath.steps.addAll(original.steps.stream().map(SBFMoveStep::createStep).collect(Collectors.toList()));
        return newPath;
    }

    @Override
    public int getEntityId() {
        return formationId;
    }

    public int getMpUsed() {
        // TODO: placeholder
        return steps.size();
    }

    public void addStep(SBFMoveStep step) {
        steps.add(step);
    }

    public SBFMoveStep getLastStep() {
        return steps.get(steps.size() - 1);
    }

    public BoardLocation getLastPosition() {
        return steps.isEmpty() ? startLocation : getLastStep().getLastPosition();
    }
}

/*
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common.pathfinder;

import java.util.ArrayList;
import java.util.Collection;

import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.MoveStepType;
import megamek.common.moves.MovePath;
import megamek.common.moves.MoveStep;
import megamek.common.units.Entity;
import megamek.common.units.Tank;

/**
 * Functional Interface for {@link #getAdjacent(MovePath)}
 */
public class NextStepsAdjacencyMap implements AdjacencyMap<MovePath> {
    protected final MoveStepType stepType;
    protected final boolean backwardsStep;

    /**
     *
     */
    public NextStepsAdjacencyMap(MoveStepType stepType) {
        this.stepType = stepType;
        backwardsStep = stepType == MoveStepType.BACKWARDS;
    }

    /**
     * Produces set of MovePaths by extending MovePath mp with MoveSteps. The set of extending steps include {F, L, R,
     * UP, ShL, ShR} if applicable. If stepType is equal to MoveStepType.BACKWARDS then extending steps include also {B,
     * ShBL, ShBR}. If stepType is equal to MoveStep.DFA or MoveStep.CHARGE then it is added to the resulting set.
     *
     * @param movePath the MovePath to be extended
     *
     * @see AdjacencyMap
     */
    @Override
    public Collection<MovePath> getAdjacent(MovePath movePath) {
        if (movePath.contains(MoveStepType.JUMP_MEK_MECHANICAL_BOOSTER)) {
            return getMechanicalBoosterJumpSteps(movePath);
        }

        final MoveStep last = movePath.getLastStep();
        final MoveStepType lType = (last == null) ? null : last.getType();
        final Entity entity = movePath.getEntity();

        final ArrayList<MovePath> movePathList = new ArrayList<>();

        // we're trying to prevent
        // a) turning left and right endlessly
        // b) spinning around endlessly
        // especially during movement where turning costs 0 MP
        if (lType != MoveStepType.TURN_LEFT &&
            (movePath.getEndStepCount(MoveStepType.TURN_RIGHT) < MovePathFinder.MAX_TURN_COUNT)) {
            movePathList.add(movePath.clone().addStep(MoveStepType.TURN_RIGHT));
        }
        if (lType != MoveStepType.TURN_RIGHT &&
            (movePath.getEndStepCount(MoveStepType.TURN_LEFT) < MovePathFinder.MAX_TURN_COUNT)) {
            movePathList.add(movePath.clone().addStep(MoveStepType.TURN_LEFT));
        }

        /*
         * If the unit is prone or hull-down it limits movement options,
         * such units can only turn or get up. (unless it's a tank; tanks
         * can just drive out of hull-down, and they cannot be prone)
         */
        if (movePath.getFinalProne() || (movePath.getFinalHullDown() && !(entity instanceof Tank))) {
            if (entity.isCarefulStand()) {
                movePathList.add(movePath.clone().addStep(MoveStepType.CAREFUL_STAND));
            } else {
                movePathList.add(movePath.clone().addStep(MoveStepType.GET_UP));
            }
            return movePathList;
        }

        if (movePath.canShift()) {
            if (backwardsStep) {
                movePathList.add(movePath.clone().addStep(MoveStepType.LATERAL_RIGHT_BACKWARDS));
                movePathList.add(movePath.clone().addStep(MoveStepType.LATERAL_LEFT_BACKWARDS));
            } else {
                movePathList.add(movePath.clone().addStep(MoveStepType.LATERAL_RIGHT));
                movePathList.add(movePath.clone().addStep(MoveStepType.LATERAL_LEFT));
            }
        }

        Board board = movePath.getGame().getBoard(movePath.getFinalBoardId());
        if (backwardsStep &&
            board.contains(movePath.getFinalCoords().translated((movePath.getFinalFacing() + 3) % 6))) {
            MovePath newPath = movePath.clone();
            PathDecorator.AdjustElevationForForwardMovement(newPath);
            movePathList.add(newPath.addStep(MoveStepType.BACKWARDS));
        } else if (board.contains(movePath.getFinalCoords().translated(movePath.getFinalFacing()))) {
            MovePath newPath = movePath.clone();
            PathDecorator.AdjustElevationForForwardMovement(newPath);
            movePathList.add(newPath.addStep(MoveStepType.FORWARDS));
        }

        return movePathList;
    }

    private Collection<MovePath> getMechanicalBoosterJumpSteps(MovePath movePath) {
        ArrayList<MovePath> mechanicalJumpPathList = new ArrayList<>();
        Board board = movePath.getGame().getBoard(movePath.getFinalBoardId());
        Coords coords = movePath.getFinalCoords();
        int facing = movePath.getFinalFacing();

        for (int direction = 0; direction < 6; direction++) {
            if (board.contains(coords.translated(direction))) {
                MovePath nextPath = movePath.clone();
                mechanicalJumpPathList.add(nextPath.addStep(MoveStepType.stepTypeForRelativeDirection(direction,
                                                                                                      facing)));
            }
        }
        return mechanicalJumpPathList;
    }
}

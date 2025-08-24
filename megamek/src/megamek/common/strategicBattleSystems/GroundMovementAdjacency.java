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

package megamek.common.strategicBattleSystems;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import megamek.common.board.BoardLocation;
import megamek.common.pathfinder.AdjacencyMap;

public record GroundMovementAdjacency(SBFGame game) implements AdjacencyMap<SBFMovePath> {

    /**
     * Produces set of MovePaths by extending MovePath mp with MoveSteps. The set of extending steps include {F, L, R,
     * UP, ShL, ShR} if applicable. If stepType is equal to MoveStepType.BACKWARDS then extending steps include also {B,
     * ShBL, ShBR}. If stepType is equal to MoveStep.DFA or MoveStep.CHARGE then it is added to the resulting set.
     *
     * @param mp the MovePath to be extended
     *
     * @see AdjacencyMap
     */
    @Override
    public Collection<SBFMovePath> getAdjacent(SBFMovePath mp) {
        List<SBFMovePath> result = new ArrayList<>();
        BoardLocation currentDestination = mp.getLastPosition();
        List<BoardLocation> possibleDestinations = currentDestination.allAdjacent();
        possibleDestinations.removeIf(bl -> !game.hasBoardLocation(bl));
        for (BoardLocation newDestination : possibleDestinations) {
            SBFMovePath newPath = SBFMovePath.createMovePathShallow(mp);
            newPath.addStep(SurfaceSBFMoveStep.createSurfaceMoveStep(game, mp.getEntityId(),
                  currentDestination, newDestination));
            result.add(newPath);
        }

        return result;
    }
}

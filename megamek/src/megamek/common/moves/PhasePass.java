/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common.moves;

import java.util.Set;

import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.pathfinder.CachedEntityState;

/**
 * Interface for a phase pass. A phase pass is a step in the move compilation.
 * @author Luana Coppio
 * @since 0.50.07
 */
interface PhasePass {

    /**
     * Get the move step types of interest for this phase pass.
     * @return a set of move step types
     */
    Set<MovePath.MoveStepType> getTypesOfInterest();

    /**
     * Check if the phase pass is interested in the move step.
     * @param moveStep the current move step type
     * @param game reference to {@link Game}
     * @param entity the entity that is moving
     * @return true if the phase pass is interested in this move step
     */
    default boolean isInterested(final MoveStep moveStep, final Game game, final Entity entity) {
        return getTypesOfInterest().contains(moveStep.getType());
    }

    /**
     * Pre-compile the move step. This method is called before the move step is compiled.
     * @param moveStep the current move step type
     * @param game reference to {@link Game}
     * @param entity the entity that is moving
     * @param prev the previous move step
     * @param cachedEntityState the cached entity state
     * @return if the phase pass should compile the move or not after the pre-compile step
     */
    PhasePassResult preCompilation(final MoveStep moveStep, final Game game, final Entity entity, final MoveStep prev,
          final CachedEntityState cachedEntityState);

    /**
     * Post-compile the move step. This method is called after the move step is compiled.
     * @param moveStep the current move step type
     * @param entity the entity that is moving
     */
    default void postCompilation(final MoveStep moveStep, final Entity entity) {
        // no-op
    }

    /**
     * Execute the phase pass on the current {@link MoveStep}.
     * @param moveStep the current move step type
     * @param game reference to {@link Game}
     * @param entity the entity that is moving
     * @param prev the previous move step
     * @param cachedEntityState the cached entity state
     */
    default void execute(final MoveStep moveStep, final Game game, final Entity entity, final MoveStep prev,
          final CachedEntityState cachedEntityState) {
        if (isInterested(moveStep, game, entity)) {
            PhasePassResult result = preCompilation(moveStep, game, entity, prev, cachedEntityState);
            if (result.isCompile()) {
                moveStep.compileMove(game, entity, prev, cachedEntityState);
                postCompilation(moveStep, entity);
            }
        }
    }
}

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

package megamek.common;

import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.moves.MovePath;
import megamek.common.units.Entity;

public class AtmosphericLandingMovePath extends MovePath {

    private final BoardLocation groundMapLandingLocation;

    /**
     * Creates a complete move path for an aero landing from an atmospheric hex onto a ground hex (i.e. when not using
     * the aero-on-ground-map movement option). For a horizontal landing, the given location is the start hex for the
     * landing process on the ground map, not the final resting hex of the unit when it has landed (this is done for
     * simplicity so the usual landing strip checks can be used. For player convenience this could be changed to use the
     * landing hex and check backwards).
     *
     * @param location The target location to begin the landing on the ground map
     */
    public AtmosphericLandingMovePath(Game game, Entity entity, BoardLocation location,
          LandingDirection landingDirection) {
        super(game, entity);
        groundMapLandingLocation = location;
        addStep(landingDirection.moveStepType());
    }

    @Override
    public void compile(Game g, Entity en, boolean clip) {}

    @Override
    public int getFinalBoardId() {
        return groundMapLandingLocation.boardId();
    }

    @Override
    public Coords getFinalCoords() {
        return groundMapLandingLocation.coords();
    }

    @Override
    public MovePath clone() {
        return super.clone();
    }
}

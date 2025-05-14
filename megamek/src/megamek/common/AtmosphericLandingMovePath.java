/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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
package megamek.common;

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
    public void compile(Game g, Entity en, boolean clip) { }

    @Override
    public int getFinalBoardId() {
        return groundMapLandingLocation.boardId();
    }

    @Override
    public Coords getFinalCoords() {
        return groundMapLandingLocation.coords();
    }
}

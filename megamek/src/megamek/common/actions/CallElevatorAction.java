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
package megamek.common.actions;

import java.io.Serial;

import megamek.common.board.BoardLocation;
import megamek.common.game.Game;
import megamek.common.units.Entity;

/**
 * Represents an action where a unit calls an industrial elevator during the End Phase. The caller must be adjacent to
 * the elevator hex. The elevator will be queued to move toward the caller's level based on distance priority (nearest
 * caller first, ties resolved by initiative).
 *
 * @author MegaMek Team
 * @since 0.50.07
 */
public class CallElevatorAction extends AbstractEntityAction {

    @Serial
    private static final long serialVersionUID = 1L;

    private final BoardLocation elevatorLocation;
    private final int targetLevel;

    /**
     * Creates a new CallElevatorAction.
     *
     * @param entityId         the ID of the entity calling the elevator
     * @param elevatorLocation the location of the industrial elevator being called
     * @param targetLevel      the level where the caller wants the elevator to arrive
     */
    public CallElevatorAction(int entityId, BoardLocation elevatorLocation, int targetLevel) {
        super(entityId);
        this.elevatorLocation = elevatorLocation;
        this.targetLevel = targetLevel;
    }

    /**
     * Returns the location of the industrial elevator being called.
     *
     * @return the elevator's BoardLocation
     */
    public BoardLocation getElevatorLocation() {
        return elevatorLocation;
    }

    /**
     * Returns the target level where the caller wants the elevator to arrive.
     *
     * @return the target level
     */
    public int getTargetLevel() {
        return targetLevel;
    }

    @Override
    public String toSummaryString(final Game game) {
        Entity entity = game.getEntity(getEntityId());
        String entityName = (entity != null) ? entity.getShortName() : "Unknown";
        return entityName + " calls elevator at " + elevatorLocation.toFriendlyString() + " to level " + targetLevel;
    }

    @Override
    public String toString() {
        return "[CallElevatorAction]: Unit ID " + getEntityId() +
              " calling elevator at " + elevatorLocation + " to level " + targetLevel;
    }
}

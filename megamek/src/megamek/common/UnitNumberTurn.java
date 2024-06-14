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
package megamek.common;

/**
 * A type of game turn that allows only entities belonging to certain units to move.
 */
public class UnitNumberTurn extends GameTurn {
    private static final long serialVersionUID = -681892308327846884L;
    private final short unitNumber;

    /**
     * Only allow entities for the given player which have types in the class mask to move.
     *
     * @param playerId the <code>int</code> ID of the player
     * @param unit     the <code>int</code> unit number of the entities allowed to move.
     */
    public UnitNumberTurn(int playerId, short unit) {
        super(playerId);
        unitNumber = unit;
    }

    /**
     * @return true if the specified entity is a valid one to use for this turn.
     */
    @Override
    public boolean isValidEntity(Entity entity, Game game, boolean useValidNonInfantryCheck) {
        return super.isValidEntity(entity, game, useValidNonInfantryCheck)
                && (unitNumber == entity.getUnitNumber());
    }
}

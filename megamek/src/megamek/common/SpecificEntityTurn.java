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
 * A type of game turn that allows only one specific entity to move.
 */
public class SpecificEntityTurn extends GameTurn {
    private static final long serialVersionUID = -4209080275946913689L;

    private final int entityId;

    public SpecificEntityTurn(int playerId, int entityId) {
        super(playerId);
        this.entityId = entityId;
    }

    public int getEntityNum() {
        return entityId;
    }

    @Override
    public boolean isValidEntity(Entity entity, Game game, boolean useValidNonInfantryCheck) {
        return super.isValidEntity(entity, game, useValidNonInfantryCheck) && (entity.getId() == entityId);
    }

    @Override
    public String toString() {
        return super.toString() + "; unit: " + entityId;
    }
}

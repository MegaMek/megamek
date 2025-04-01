/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 2 or (at your option) any later version,
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
 */
package megamek.ai.dataset;

import megamek.client.bot.common.Pathing;
import megamek.common.Coords;
import megamek.common.CubeCoords;
import megamek.common.Entity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UnitPath implements Pathing {

    private final UnitAction unitAction;
    private final Entity entity;
    private final Set<Coords> coords;

    public UnitPath(UnitAction unitAction, Entity entity) {
        this.unitAction = unitAction;
        this.entity = entity;
        this.coords = new HashSet<>();
        List<CubeCoords> cubeCoordsLine = unitAction.currentCubePosition().lineTo(unitAction.finalCubePosition());
        cubeCoordsLine.forEach(c -> coords.add(c.toOffset()));
    }

    @Override
    public Coords getStartCoords() {
        return unitAction.currentPosition();
    }

    @Override
    public Coords getFinalCoords() {
        return unitAction.finalPosition();
    }

    @Override
    public int getFinalFacing() {
        return unitAction.facing();
    }

    @Override
    public Entity getEntity() {
        return entity;
    }

    @Override
    public Set<Coords> getCoordsSet() {
        return coords;
    }

    @Override
    public int getHexesMoved() {
        return unitAction.hexesMoved();
    }

    @Override
    public int getDistanceTravelled() {
        return unitAction.distance();
    }

    @Override
    public boolean hasWaypoint() {
        return false;
    }

    @Override
    public Coords getWaypoint() {
        return null;
    }

    @Override
    public boolean isJumping() {
        return unitAction.jumping();
    }

    @Override
    public boolean getFinalProne() {
        return unitAction.prone();
    }

    @Override
    public int getMpUsed() {
        return unitAction.mpUsed();
    }
}

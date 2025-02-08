/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 */
package megamek.utilities.ai;

import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.UnitRole;

/**
 * Represents the state of a unit.
 * @param id
 * @param teamId
 * @param round
 * @param playerId
 * @param chassis
 * @param model
 * @param type
 * @param role
 * @param x
 * @param y
 * @param facing
 * @param mp
 * @param heat
 * @param prone
 * @param airborne
 * @param offBoard
 * @param crippled
 * @param destroyed
 * @param armorP
 * @param internalP
 * @param done
 * @param maxRange
 * @param totalDamage
 * @param turnsWithoutMovement // unused right now
 * @param entity
 * @author Luana Coppio
 */
public record UnitState(int id, int teamId, int round, int playerId, String chassis, String model, String type, UnitRole role,
                        int x, int y, int facing, double mp, double heat, boolean prone, boolean airborne,
                        boolean offBoard, boolean crippled, boolean destroyed, double armorP,
                        double internalP, boolean done, int maxRange, int totalDamage, int turnsWithoutMovement, Entity entity) {
    public Coords position() {
        return new Coords(x, y);
    }
}

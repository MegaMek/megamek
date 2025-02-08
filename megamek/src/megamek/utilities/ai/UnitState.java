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
 * @param id unit id
 * @param teamId team id
 * @param round round number
 * @param playerId player id
 * @param chassis chassis
 * @param model model
 * @param type type is actually the simple name of the class of the entity
 * @param role UnitRole
 * @param x x coordinate
 * @param y y coordinate
 * @param facing facing direction
 * @param mp movement points
 * @param heat heat points
 * @param prone prone
 * @param airborne airborne
 * @param offBoard off board
 * @param crippled crippled
 * @param destroyed destroyed
 * @param armorP armor percent
 * @param internalP internal percent
 * @param done done
 * @param maxRange max weapon range
 * @param totalDamage total damage it can cause
 * @param turnsWithoutMovement // unused right now
 * @param entity entity
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

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

/**
 * Represents a unit action.
 * @param id
 * @param facing
 * @param fromX
 * @param fromY
 * @param toX
 * @param toY
 * @param hexesMoved
 * @param distance
 * @param mpUsed
 * @param maxMp
 * @param mpP
 * @param heatP
 * @param armorP
 * @param internalP
 * @param jumping
 * @param prone
 * @param legal
 * @author Luana Coppio
 */
public record UnitAction(int id, int facing, int fromX, int fromY, int toX, int toY, int hexesMoved, int distance, int mpUsed,
                         int maxMp, double mpP, double heatP, double armorP, double internalP, boolean jumping, boolean prone,
                         boolean legal) {
    public double chanceOfFailure() {
        return 0.0;
    }

    public Coords currentPosition() {
        return new Coords(fromX, fromY);
    }

    public Coords finalPosition() {
        return new Coords(toX, toY);
    }
}

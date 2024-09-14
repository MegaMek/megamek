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
package megamek.common.util;

import megamek.common.ToHitData;

/**
 * Used to track data for displaying "FiringSolution" data in the BoardView.
 * This contains information about to-hit
 * numbers and any other useful information for determining what units to are
 * the best to shoot at.
 *
 * @author arlith
 *
 */
public class FiringSolution {

    private ToHitData toHit;

    private boolean targetSpotted;

    /**
     *
     * @param toHit
     * @param targetSpotted
     */
    public FiringSolution(ToHitData toHit, boolean targetSpotted) {
        this.toHit = toHit;
        this.targetSpotted = targetSpotted;
    }

    public ToHitData getToHitData() {
        return toHit;
    }

    public boolean isTargetSpotted() {
        return targetSpotted;
    }

}

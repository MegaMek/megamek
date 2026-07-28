package megamek.common.rules;

/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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


public abstract class RulesCharts {
    /**
     * Escalating failures charts.
     *
     * @param round the current round
     * @return the escalating failure value
     */
    public abstract int escalatingFailure(int round);

    /**
     * Get facing for a fall.
     *
     * @return the facing direction for a fall
     */
    public abstract int getFacingForFall();
    
    /**
     * Get location names.
     *
     * @param loc the location code
     * @param quad whether the unit is quadrupedal
     * @return the name of the location
     */
    public abstract String getLocationName(int loc, boolean quad);

    /**
     * Mek Punch hit chart.
     *
     * @param roll the dice roll
     * @param side the side being hit
     * @param quad whether the unit is quadrupedal
     * @return the hit location
     */
    public abstract int getPunchHitLocation(int roll, int side, boolean quad);
    
    /**
     * Mek Punch hit chart.
     *
     * @param roll the dice roll
     * @param side the side being hit
     * @return the hit location
     */
    public int getPunchHitLocation(int roll, int side) {
        return getPunchHitLocation(roll, side, false);
    }

    /**
     * Mek punch hit chart side.
     *
     * @param roll the dice roll
     * @param side the side being hit
     * @param quad whether the unit is quadrupedal
     * @return the hit location side
     */
    public abstract int getPunchHitLocationSide(int roll, int side, boolean quad);
}

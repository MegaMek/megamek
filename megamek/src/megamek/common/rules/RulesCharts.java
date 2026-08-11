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


import megamek.common.Messages;
import megamek.common.ToHitData;
import megamek.common.units.Mek;

public abstract class RulesCharts {
    /**
     * Escalating failures charts.
     *
     * @param count the current c
     * @return the escalating failure value
     */
    public int escalatingFailure(int count) {
        // For each count after 5, the number is 11+
        return switch (count) {
            case 0 -> 2;
            case 1 -> 3;
            case 2 -> 5;
            case 3 -> 7;
            case 4 -> 10;
            default -> 11;
        };
    }

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
    public String getLocationName(int loc, boolean quad) {
        switch (loc) {
            case Mek.LOC_LEFT_LEG:
                if (quad) {
                    return Messages.getString("Locations.RearLeftLeg");
                }
                return Messages.getString("Locations.LeftLeg");
            case Mek.LOC_RIGHT_LEG:
                if (quad) {
                    return Messages.getString("Locations.RearRightLeg");
                }
                return Messages.getString("Locations.RightLeg");
            case Mek.LOC_CENTER_LEG:
                return Messages.getString("Locations.CenterLeg");
            case Mek.LOC_LEFT_ARM:
                if (quad) {
                    return Messages.getString("Locations.FrontLeftLeg");
                }
                return Messages.getString("Locations.LeftArm");
            case Mek.LOC_RIGHT_ARM:
                if (quad) {
                    return Messages.getString("Locations.FrontRightLeg");
                }
                return Messages.getString("Locations.RightArm");
            case Mek.LOC_RIGHT_TORSO:
                return Messages.getString("Locations.RightTorso");
            case Mek.LOC_CENTER_TORSO:
                return Messages.getString("Locations.CenterTorso");
            case Mek.LOC_LEFT_TORSO:
                return Messages.getString("Locations.LeftTorso");
        }

        return "";
    }

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
    public int getPunchHitLocationSide(int roll, int side, boolean quad) {
        if (side == ToHitData.SIDE_LEFT) {
            // left side punch hits
            switch (roll) {
                case 1:
                case 2:
                    return Mek.LOC_LEFT_TORSO;
                case 3:
                    return Mek.LOC_CENTER_TORSO;
                case 4:
                    if (quad) {return Mek.LOC_LEFT_ARM;}
                case 5:
                    if (quad) {return Mek.LOC_LEFT_LEG;}
                    return Mek.LOC_LEFT_ARM;
                case 6:
                    return Mek.LOC_HEAD;
            }
        }
        if (side == ToHitData.SIDE_RIGHT) {
            // right side punch hits
            switch (roll) {
                case 1:
                case 2:
                    return Mek.LOC_RIGHT_TORSO;
                case 3:
                    return Mek.LOC_CENTER_TORSO;
                case 4:
                    if (quad) {return Mek.LOC_RIGHT_ARM;}
                case 5:
                    if (quad) {return Mek.LOC_RIGHT_LEG;}
                    return Mek.LOC_RIGHT_ARM;
                case 6:
                    return Mek.LOC_HEAD;
            }
        }
        // No location found. Should never occur
        return Mek.LOC_NONE;
    }
}

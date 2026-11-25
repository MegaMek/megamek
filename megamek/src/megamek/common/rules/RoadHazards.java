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

import megamek.common.Hex;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.LargeSupportTank;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import megamek.common.units.Terrains;

/** Surface-dependent movement hazards (TW pp. 32, 61–62 and TO:AR pp. 38, 48–49, 57). */
public final class RoadHazards {
    private RoadHazards() {
    }

    /** A unit can touch the bridge deck, full pavement, or the paved road it is following. */
    public static boolean touchesPavement(Hex hex, int elevation, boolean onRoad) {
        return (hex.containsTerrain(Terrains.BRIDGE) && elevation == hex.terrainLevel(Terrains.BRIDGE_ELEV))
              || (elevation == 0 && (hex.containsTerrain(Terrains.PAVEMENT) || (onRoad && hex.hasPavedRoad())));
    }

    public static boolean canRevealBlackIce(Entity unit) {
        return !unit.isAirborne() && (unit instanceof Mek
              || (unit instanceof Tank && !unit.getMovementMode().isHover() && !unit.getMovementMode().isMarine()));
    }

    public static boolean blackIceWeather(Game game) {
        PlanetaryConditions conditions = game.getPlanetaryConditions();
        return conditions.getWeather().isIceStorm()
              || (game.getOptions().booleanOption(OptionsConstants.ADVANCED_BLACK_ICE)
                    && conditions.getTemperature() <= PlanetaryConditions.BLACK_ICE_TEMP);
    }

    /** Rain can cover an unpaved road itself with mud. Other underlying terrain is bypassed. */
    public static boolean mudOnRoad(Hex hex) {
        return hex.containsTerrain(Terrains.MUD) && hex.containsTerrain(Terrains.ROAD) && !hex.hasPavedGround();
    }

    public static int bogDownModifier(Entity unit, Hex hex, boolean onRoad, int elevation) {
        boolean largeVehicle = unit instanceof LargeSupportTank;
        // TO:AR p.48 exempts Meks and DropShips from bogging in mud. Driving uses the vehicle rules.
        boolean ignoresMud = unit instanceof Dropship || (unit instanceof Mek
              && !unit.getMovementMode().isTrackedWheeledOrHover());
        if (!onRoad) {
            return hex.getBogDownModifier(unit.getMovementMode(), largeVehicle, ignoresMud);
        }
        if (!ignoresMud && elevation == 0 && mudOnRoad(hex) && !touchesPavement(hex, elevation, true)) {
            return hex.getTerrain(Terrains.MUD).getBogDownModifier(unit.getMovementMode(), largeVehicle);
        }
        return TargetRoll.AUTOMATIC_SUCCESS;
    }
}

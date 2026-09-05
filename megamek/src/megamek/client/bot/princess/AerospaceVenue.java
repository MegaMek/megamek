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
package megamek.client.bot.princess;

import megamek.common.game.Game;
import megamek.common.units.Entity;

/**
 * Which set of atmospheric aerospace rules a unit is flying under.
 *
 * <p>Aerospace units on ground mapsheets still fly "as though they were moving on the low-altitude map"
 * (TW p.91) - same altitudes, never elevations - but the hexes underneath them are a different size, and
 * almost every derived quantity changes with it. A ground map hex is 30 metres; a low-altitude hex is 500,
 * roughly a whole ground map. That is the {@value #GROUND_HEXES_PER_LOW_ALTITUDE_HEX}-to-one ratio the game
 * uses to translate between them (TW p.92).</p>
 *
 * <p>The two venues diverge far enough that treating them as one case is what produced the bot's current
 * aerospace behaviour. Naming the venue makes each divergence a deliberate branch instead of a scattered
 * {@code isAirborneAeroOnGroundMap()} test:</p>
 *
 * <ul>
 *     <li><b>Turning</b> - a low-altitude fighter buys a facing change for {@code ceil(velocity/2)} thrust.
 *     On a ground map it must first fly 8 to 52 hexes in a straight line, depending on velocity and class
 *     (TW p.92). Facing is close to unusable within a single turn; altitude is not.</li>
 *     <li><b>Reach</b> - one velocity point carries a low-altitude fighter one hex, and a ground-map fighter
 *     sixteen (TW p.92).</li>
 *     <li><b>The dead zone</b> - measured in low-altitude hexes either way, so on a ground map it covers
 *     sixteen times the ground. One altitude of separation costs a low-altitude fighter two hexes of
 *     approach and a ground-map fighter seventeen.</li>
 * </ul>
 */
public enum AerospaceVenue {
    /** Flying directly over a ground mapsheet, under the Aerospace Units on Ground Mapsheets rules (TW p.91). */
    GROUND_MAP,

    /** Flying on a low-altitude map, with or without terrain beneath it. */
    LOW_ALTITUDE;

    /**
     * Ground map hexes spanned by one low-altitude hex.
     *
     * <p>The map scales are 30 metres and 500 metres respectively, so the true ratio is nearer seventeen, but
     * the game rounds it to sixteen and uses that everywhere - one velocity point moves a ground-map fighter
     * sixteen hexes (TW p.92), and {@code Compute.effectiveDistance} divides by the same figure.</p>
     */
    public static final int GROUND_HEXES_PER_LOW_ALTITUDE_HEX = 16;

    /**
     * The venue an airborne aerospace unit is currently flying under.
     *
     * @param game   the current game
     * @param entity the unit to place
     *
     * @return {@link #GROUND_MAP} when the unit is an airborne aero over a ground mapsheet, otherwise
     *       {@link #LOW_ALTITUDE}
     */
    public static AerospaceVenue of(Game game, Entity entity) {
        if ((game == null) || (entity == null)) {
            return LOW_ALTITUDE;
        }
        return entity.isAirborneAeroOnGroundMap() ? GROUND_MAP : LOW_ALTITUDE;
    }

    /**
     * @return {@code true} when this is the ground mapsheet venue
     */
    public boolean isGroundMap() {
        return this == GROUND_MAP;
    }

    /**
     * How far one point of velocity carries an aerodyne unit here, in hexes of this venue's own map (TW p.92).
     *
     * @return {@value #GROUND_HEXES_PER_LOW_ALTITUDE_HEX} on a ground mapsheet, 1 at low altitude
     */
    public int hexesPerVelocityPoint() {
        return isGroundMap() ? GROUND_HEXES_PER_LOW_ALTITUDE_HEX : 1;
    }

    /**
     * Converts a hex distance measured on this venue's map into the low-altitude hexes that aerospace range,
     * bracket and dead-zone rules are written in.
     *
     * <p>Rounds up, matching the engine's own conversion in {@code Compute.effectiveDistance}. The rounding
     * is not cosmetic: on a ground map it decides the boundary case, because seventeen hexes of separation
     * rounds to two low-altitude hexes and sixteen rounds to one.</p>
     *
     * @param hexDistance distance in hexes of this venue's map, negative values treated as zero
     *
     * @return the equivalent distance in low-altitude hexes
     */
    public int toEngagementRange(int hexDistance) {
        int distance = Math.max(0, hexDistance);
        if (!isGroundMap()) {
            return distance;
        }
        return (int) Math.ceil(distance / (double) GROUND_HEXES_PER_LOW_ALTITUDE_HEX);
    }
}

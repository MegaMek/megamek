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

import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.units.Entity;
import megamek.common.units.IAero;

/**
 * The air-to-air geometry an aerospace unit has to satisfy before it can shoot at anything.
 *
 * <p>Every method here takes a <i>pose</i> - a position and an altitude the unit might occupy - rather than
 * an {@link Entity} whose state it reads. That is deliberate. Path ranking evaluates hundreds of positions
 * the unit is not in, and the engine's own {@code Compute.inDeadZone} reads {@code attacker.getAltitude()},
 * so calling it during ranking answers a question about the present rather than the hypothesis. The same
 * mistake produced the {@code Entity.height()} bug found in the #8659 review.</p>
 *
 * <h2>The dead zone</h2>
 *
 * <p>TW p.241: two airborne units add one hex of range per level of altitude between them, and <i>"units
 * cannot aim into the area immediately above or below their own hexes"</i>. Quantified by the same
 * paragraph: one altitude apart needs two hexes of horizontal separation, two altitudes needs three, and so
 * on - a widening cone above and below every fighter, not a fixed bubble.</p>
 *
 * <p>What makes this the decisive rule in atmospheric combat is that it is measured in low-altitude hexes on
 * both venues. Over a ground mapsheet one altitude of separation therefore blocks fire within seventeen
 * ground hexes and two altitudes within thirty-three - most of a normal playing area. See
 * {@link AerospaceVenue}.</p>
 */
public final class AerospaceGeometry {

    /** Lowest altitude an airborne aerospace unit can hold; below this it is landing or crashing. */
    public static final int MINIMUM_ALTITUDE = 1;

    /** Highest altitude in atmosphere; a unit reaching it may leave vertically under the climb-out option. */
    public static final int MAXIMUM_ALTITUDE = 10;

    /** Thrust points to climb one altitude level. Descending is free, which is what makes diving cheap. */
    public static final int THRUST_PER_ALTITUDE_GAIN = 2;

    /**
     * Altitude levels a unit will willingly shed in one turn.
     *
     * <p>Descent costs no thrust, but dropping more than two levels forces a severe control roll, so no
     * sane pilot - and neither does {@link AeroPathUtil#generateValidAltitudeChanges} - plans past it.</p>
     */
    public static final int SAFE_DESCENT_PER_TURN = 2;

    private AerospaceGeometry() {
    }

    /**
     * Whether a shot between two poses is inside the attacker's dead zone.
     *
     * <p>Pure geometry, so it answers the same question for either side of an engagement and for a position
     * nobody occupies yet. It does not know about spheroids, which are exempt - use
     * {@link #deadZoneBlocksAttack} to ask whether a particular attacker is actually barred.</p>
     *
     * <p>The predicate is the engine's, rearranged. {@code Compute.inDeadZone} compares the altitude
     * difference against {@code effectiveDistance - altitudeDifference}, and since {@code effectiveDistance}
     * is the converted horizontal range <i>plus</i> that same difference, the two cancel and what remains is
     * a comparison against horizontal range alone.</p>
     *
     * @param venue           which set of atmospheric rules is in force
     * @param attackerPosition the hex the attacker would be firing from
     * @param attackerAltitude the altitude the attacker would be firing from
     * @param targetPosition   the target's hex
     * @param targetAltitude   the target's altitude
     *
     * @return {@code true} if the target sits in the cone above or below the attacker and cannot be aimed at
     */
    public static boolean inDeadZone(AerospaceVenue venue, Coords attackerPosition, int attackerAltitude,
          Coords targetPosition, int targetAltitude) {
        if ((venue == null) || (attackerPosition == null) || (targetPosition == null)) {
            return false;
        }
        int altitudeDifference = Math.abs(attackerAltitude - targetAltitude);
        int horizontalRange = venue.toEngagementRange(attackerPosition.distance(targetPosition));
        return altitudeDifference >= horizontalRange;
    }

    /**
     * Whether the dead zone actually bars this attacker from shooting.
     *
     * <p>Spheroids are the exception the rules carve out (TW p.241): they may fire nose weapons at a target
     * above them and aft weapons at one below, so the geometry never blocks them outright. The engine gates
     * its own block the same way, in {@code Compute.getRangeMods}.</p>
     *
     * @param venue                   which set of atmospheric rules is in force
     * @param attackerPosition        the hex the attacker would be firing from
     * @param attackerAltitude        the altitude the attacker would be firing from
     * @param attackerFliesAsSpheroid {@code true} if the attacker is behaving as a spheroid in atmosphere
     * @param targetPosition          the target's hex
     * @param targetAltitude          the target's altitude
     *
     * @return {@code true} if the attacker has no legal shot from this pose
     */
    public static boolean deadZoneBlocksAttack(AerospaceVenue venue, Coords attackerPosition,
          int attackerAltitude, boolean attackerFliesAsSpheroid, Coords targetPosition, int targetAltitude) {
        if (attackerFliesAsSpheroid) {
            return false;
        }
        return inDeadZone(venue, attackerPosition, attackerAltitude, targetPosition, targetAltitude);
    }

    /**
     * The range an air-to-air shot is resolved at, in the low-altitude hexes that weapon brackets use.
     *
     * <p>Two parts, both from TW p.241: the horizontal distance converted to low-altitude hexes, plus one
     * hex for every level of altitude between the two units. The book's own example - two fighters ten hexes
     * apart at altitudes 3 and 5 are at an effective twelve - is the low-altitude case.</p>
     *
     * @param venue            which set of atmospheric rules is in force
     * @param attackerPosition the hex the attacker would be firing from
     * @param attackerAltitude the altitude the attacker would be firing from
     * @param targetPosition   the target's hex
     * @param targetAltitude   the target's altitude
     *
     * @return the effective range in low-altitude hexes
     */
    public static int effectiveRange(AerospaceVenue venue, Coords attackerPosition, int attackerAltitude,
          Coords targetPosition, int targetAltitude) {
        if ((venue == null) || (attackerPosition == null) || (targetPosition == null)) {
            return 0;
        }
        int altitudeDifference = Math.abs(attackerAltitude - targetAltitude);
        return venue.toEngagementRange(attackerPosition.distance(targetPosition)) + altitudeDifference;
    }

    /**
     * How many hexes a unit can fly straight ahead from this pose before leaving the board.
     *
     * <p>Aerospace movement is mostly committed displacement: the velocity a path ends with must be flown
     * next turn, largely along the facing it ends on. A pose a few hexes from the edge, pointing out, is a
     * departure already in progress whatever the pilot intends - so the ranker needs this distance to price
     * edge headings before they become exits.</p>
     *
     * @param start   the hex the pose ends in
     * @param facing  the facing the pose ends on
     * @param board   the board being flown over
     * @param maximum cap on the walk, so the cost stays bounded by what the caller cares about
     *
     * @return hexes travelled before the first off-board hex, capped at {@code maximum}
     */
    public static int hexesUntilOffBoard(Coords start, int facing, Board board, int maximum) {
        if ((start == null) || (board == null)) {
            return maximum;
        }
        Coords current = start;
        for (int travelled = 0; travelled < maximum; travelled++) {
            current = current.translated(facing);
            if (!board.contains(current)) {
                return travelled;
            }
        }
        return maximum;
    }

    /**
     * How far this hex sits from the nearest board edge, in hexes.
     *
     * <p>The directional walk in {@link #hexesUntilOffBoard} prices the exit a pose is pointing at; this
     * prices the edges it is merely standing beside. A fighter parked on the westernmost column facing north
     * reads as safe to the directional walk while one hexside of leftward drift is an instant exit - observed
     * live as a damaged bot hugging the map edge with the directional charge barely registering.</p>
     *
     * @param position the hex to measure
     * @param board    the board being flown over
     *
     * @return hexes to the nearest edge; 0 means standing on an edge row or column
     */
    public static int hexesToNearestEdge(Coords position, Board board) {
        if ((position == null) || (board == null)) {
            return Integer.MAX_VALUE;
        }
        int fromWest = position.getX();
        int fromEast = board.getWidth() - 1 - position.getX();
        int fromNorth = position.getY();
        int fromSouth = board.getHeight() - 1 - position.getY();
        int westToEast = Math.min(fromWest, fromEast);
        int northToSouth = Math.min(fromNorth, fromSouth);
        return Math.min(westToEast, northToSouth);
    }

    /**
     * The altitudes a unit could still be at when its turn ends.
     *
     * <p>Asymmetric, because the rules are: climbing costs {@value #THRUST_PER_ALTITUDE_GAIN} thrust per
     * level and is limited by the thrust budget, while descending is free and limited only by how much
     * control risk a pilot will accept. A fighter with six thrust can therefore reach three levels up but
     * only two down, and it is much likelier to take the free option.</p>
     *
     * <p>Used to model an enemy that has not moved yet. Reading such a unit's current altitude as though it
     * were settled is what leaves the bot committing to geometry its opponent is about to leave.</p>
     *
     * @param entity the unit to bound, which need not have moved
     *
     * @return the inclusive band of altitudes it can reach, clamped to legal airborne altitudes
     */
    public static AltitudeBand reachableAltitudeBand(Entity entity) {
        if (!(entity instanceof IAero aero) || !entity.isAirborne()) {
            return new AltitudeBand(MINIMUM_ALTITUDE, MINIMUM_ALTITUDE);
        }
        int currentAltitude = entity.getAltitude();
        int availableThrust = Math.max(0, AeroPathUtil.calculateMaxSafeThrust(aero));
        int climb = availableThrust / THRUST_PER_ALTITUDE_GAIN;
        return new AltitudeBand(clampAltitude(currentAltitude - SAFE_DESCENT_PER_TURN),
              clampAltitude(currentAltitude + climb));
    }

    /**
     * Holds an altitude to the range an airborne aerospace unit can legally occupy.
     *
     * @param altitude the altitude to clamp
     *
     * @return the altitude, bounded to {@value #MINIMUM_ALTITUDE} through {@value #MAXIMUM_ALTITUDE}
     */
    public static int clampAltitude(int altitude) {
        return Math.clamp(altitude, MINIMUM_ALTITUDE, MAXIMUM_ALTITUDE);
    }

    /**
     * An inclusive range of altitudes.
     *
     * <p>A transient value passed between bot calculations; it is never written to a save file, so it needs
     * no {@code SerializationHelper} converter.</p>
     *
     * @param lowest  the lowest altitude in the band
     * @param highest the highest altitude in the band
     */
    public record AltitudeBand(int lowest, int highest) {

        /**
         * @param altitude the altitude to test
         *
         * @return {@code true} if the altitude falls inside this band
         */
        public boolean contains(int altitude) {
            return (altitude >= lowest) && (altitude <= highest);
        }

        /**
         * The altitude in this band a unit is likeliest to be found at, used when nothing better is known.
         *
         * @return the midpoint of the band
         */
        public int midpoint() {
            return (lowest + highest) / 2;
        }
    }
}

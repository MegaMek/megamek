/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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
package megamek.common;

public class ComputeSideTable {

    /**
     * Returns the side table to use for hits (e.g. ToHitData.SIDE_REAR) for the given attacker and target, assuming no
     * called shot. This takes into account aero attacks within the same hex (TW p.236) and G2A attacks against a flight
     * path position for aeros on the ground board or on an atmospheric board.
     *
     * @return A side table, such as ToHitData.SIDE_REAR
     *
     * @see ToHitData
     */
    public static int sideTable(Entity attacker, Targetable target) {
        return sideTable(attacker, target, CalledShot.CALLED_NONE);
    }

    /**
     * Returns the side table to use for hits (e.g. ToHitData.SIDE_REAR) for the given attacker and target, assuming the
     * given calledShotType shot status (e.g. CalledShot.CALLED_NONE). This takes into account aero attacks within the
     * same hex (TW p.236) and G2A attacks against a flight path position for aeros on the ground board or on an
     * atmospheric board.
     *
     * @param calledShotType The calledShotType shot status, e.g. CalledShot.CALLED_NONE
     *
     * @return A side table, such as ToHitData.SIDE_REAR
     *
     * @see ToHitData
     * @see CalledShot
     */
    public static int sideTable(Entity attacker, Targetable target, int calledShotType) {
        if (target instanceof Entity entityTarget) {
            return sideTableForEntityTarget(attacker, entityTarget, calledShotType);

        } else {
            Coords attackPos = attacker.getPosition();
            if (Compute.isAirToGround(attacker, target)) {
                // attacker position is given by the direction from which they entered the target hex
                attackPos = attacker.passedThroughPrevious(target.getPosition());
            }
            return target.sideTable(attackPos, false);
        }
    }

    private static int sideTableForEntityTarget(Entity attacker, Entity target, int calledShotType) {
        Coords attackPos = attacker.getPosition();

        boolean usePrior = false;
        // aeros in the same hex need to adjust position to get side table
        if (Compute.isAirToAir(attacker.getGame(), attacker, target)
              && attackPos.equals(target.getPosition())
              && attacker.isAero()
              && target.isAero()) {
            int moveSort = Compute.shouldMoveBackHex(attacker, target);
            if (moveSort < 0) {
                attackPos = attacker.getPriorPosition();
            }
            usePrior = moveSort > 0;
        }

        if (Compute.isAirToGround(attacker, target)) {
            // attacker position is given by the direction from which they entered the target hex
            attackPos = attacker.passedThroughPrevious(target.getPosition());

        } else if (Compute.isGroundToAir(attacker, target)) {
            int facing = Compute.getClosestFlightPathFacing(attacker.getId(), attackPos, target);
            Coords pos = Compute.getClosestFlightPath(attacker.getId(), attackPos, target);
            return target.sideTable(attackPos, usePrior, facing, pos);
        }

        if (calledShotType == CalledShot.CALLED_LEFT) {
            return target.sideTable(attackPos, usePrior, (target.getFacing() + 5) % 6);

        } else if (calledShotType == CalledShot.CALLED_RIGHT) {
            return target.sideTable(attackPos, usePrior, (target.getFacing() + 1) % 6);

        } else {
            return target.sideTable(attackPos, usePrior);
        }
    }

    private ComputeSideTable() { }
}

/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.compute;

import megamek.common.CalledShot;
import megamek.common.ToHitData;
import megamek.common.board.Coords;
import megamek.common.board.CrossBoardAttackHelper;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.common.units.Targetable;

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
        Game game = attacker.getGame();

        boolean usePrior = false;
        // aeros in the same hex need to adjust position to get side table
        if (Compute.isAirToAir(attacker.getGame(), attacker, target)
              && attackPos.equals(target.getPosition())
              && game.onTheSameBoard(attacker, target)
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

        // In A2A attacks between different maps (only ground/ground, ground/atmo or atmo/ground), replace the
        // position of the unit on the ground map with the position of the ground map itself in the atmo map
        Coords effectiveTargetPosition = target.getPosition();
        if (Compute.isAirToAir(game, attacker, target) && !game.onTheSameBoard(attacker, target)
              && (game.onDirectlyConnectedBoards(attacker, target)
              || CrossBoardAttackHelper.onGroundMapsWithinOneAtmosphereMap(game, attacker, target))) {
            if (game.isOnGroundMap(attacker) && game.isOnAtmosphericMap(target)) {
                attackPos = game.getBoard(target).embeddedBoardPosition(attacker.getBoardId());
            } else if (game.isOnAtmosphericMap(attacker) && game.isOnGroundMap(target)) {
                effectiveTargetPosition = game.getBoard(attacker).embeddedBoardPosition(target.getBoardId());
            } else if (game.isOnGroundMap(attacker) && game.isOnGroundMap(target)) {
                // Different ground maps, here replace both positions with their respective atmo map hexes
                attackPos = game.getBoard(target).embeddedBoardPosition(attacker.getBoardId());
                effectiveTargetPosition = game.getBoard(attacker).embeddedBoardPosition(target.getBoardId());
            }
        }

        if (calledShotType == CalledShot.CALLED_LEFT) {
            return target.sideTable(attackPos, usePrior, (target.getFacing() + 5) % 6, effectiveTargetPosition);

        } else if (calledShotType == CalledShot.CALLED_RIGHT) {
            return target.sideTable(attackPos, usePrior, (target.getFacing() + 1) % 6, effectiveTargetPosition);

        } else {
            return target.sideTable(attackPos, usePrior, target.getFacing(), effectiveTargetPosition);
        }
    }

    private ComputeSideTable() {}
}

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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.board.CrossBoardAttackHelper;
import megamek.common.enums.FacingArc;
import megamek.common.equipment.HandheldWeapon;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import megamek.common.units.Targetable;
import megamek.common.units.UnitPosition;
import megamek.logging.MMLogger;

public class ComputeArc {

    private final static MMLogger LOGGER = MMLogger.create(ComputeArc.class);

    public static boolean isInArc(Game game, int attackerId, int weaponId, Targetable target) {
        Entity weaponEntity = game.getEntity(attackerId);
        Entity attacker = weaponEntity instanceof HandheldWeapon ?
              game.getEntity(weaponEntity.getTransportId()) : game.getEntity(attackerId);

        if ((weaponEntity == null) || (attacker == null) || (target == null)) {
            LOGGER.error("Trying to compute arc with a null attacker or target");
            return false;
        }

        if ((attacker.getPosition() == null) || (target.getPosition() == null)) {
            LOGGER.error("Trying to compute arc with null position on attacker or target");
            return false;
        }

        if ((attacker instanceof Mek) && (attacker.getGrappled() == target.getId())) {
            return true;
        }

        int facing = getFacing(weaponId, weaponEntity);

        Coords aPos = attacker.getPosition();
        Coords tPos = target.getPosition();
        UnitPosition targetPosition;

        // aeros in the same hex in space may still be able to fire at one another. Translate
        // their positions to see who was further back
        if (attacker.isSpaceborne() && (target instanceof Entity targetEntity) && aPos.equals(tPos)
              && attacker.isAero() && target.isAero()) {
            if (Compute.shouldMoveBackHex(attacker, targetEntity) < 0) {
                aPos = attacker.getPriorPosition();
            } else {
                tPos = targetEntity.getPriorPosition();
            }
        }

        // Allow dive-bombing VTOLs to attack the hex they are in, if they didn't select one for bombing while moving
        if ((attacker.getMovementMode() == EntityMovementMode.VTOL) && aPos.equals(tPos)
              && game.onTheSameBoard(attacker, target)) {
            if (attacker.getEquipment(weaponId).getType().hasFlag(WeaponType.F_DIVE_BOMB)) {
                return true;
            }
        }

        // if using advanced AA options, then ground-to-air fire determines arc by closest position
        if (Compute.isGroundToAir(attacker, target) && (target instanceof Entity targetEntity)) {
            tPos = Compute.getClosestFlightPath(attacker.getId(), attacker.getPosition(), targetEntity);
            targetPosition = UnitPosition.of(tPos, targetEntity.getFacing());
        } else {

            // AMS defending against Ground to Air fire needs to calculate arc based on the closest flight path
            // Technically it's an AirToGround attack since the AMS is on the aircraft
            if (Compute.isAirToGround(attacker, target)
                  && (weaponEntity.getEquipment(weaponId).getType().hasFlag(WeaponType.F_AMS)
                  || weaponEntity.getEquipment(weaponId).getType().hasFlag(WeaponType.F_AMS_BAY))) {
                aPos = Compute.getClosestFlightPath(target.getId(), target.getPosition(), attacker);
            }

            if (CrossBoardAttackHelper.isCrossBoardArtyAttack(attacker, target, game)) {
                // When attacking between two ground boards, replace the attacker and target positions with the positions
                // of the boards themselves on the atmospheric map
                // When the ground boards are only connected through a high atmospheric map, the arrangement of
                // the maps is unkown and the arc cannot be tested; therefore return false in that case, although
                // a distance could be computed
                Board attackerAtmoBoard = game.getEnclosingBoard(game.getBoard(attacker));
                Board targetAtmoBoard = game.getEnclosingBoard(game.getBoard(target));
                if (attackerAtmoBoard.getBoardId() != targetAtmoBoard.getBoardId()) {
                    return false;
                }
                aPos = attackerAtmoBoard.embeddedBoardPosition(attacker.getBoardId());
                targetPosition = UnitPosition.of(attackerAtmoBoard.embeddedBoardPosition(target.getBoardId()));
            } else if (CrossBoardAttackHelper.isOrbitToSurface(game, attacker, target)) {
                // For this attack, the ground row hex enclosing the ground map target must be in arc; replace position
                Board targetAtmoBoard = game.getEnclosingBoard(game.getBoard(target.getBoardId()));
                targetPosition = UnitPosition.of(game.getBoard(attacker)
                      .embeddedBoardPosition(targetAtmoBoard.getBoardId()));
            } else if (Compute.isAirToAir(game, attacker, target) && !game.onTheSameBoard(attacker, target)
                  && (game.onDirectlyConnectedBoards(attacker, target)
                  || CrossBoardAttackHelper.onGroundMapsWithinOneAtmosphereMap(game, attacker, target))) {
                // In A2A attacks between different maps (only ground/ground, ground/atmo or atmo/ground), replace the
                // position of the unit on the ground map with the position of the ground map itself in the atmo map
                if (game.isOnGroundMap(attacker) && game.isOnAtmosphericMap(target)) {
                    aPos = game.getBoard(target).embeddedBoardPosition(attacker.getBoardId());
                    targetPosition = UnitPosition.of(target);
                } else if (game.isOnAtmosphericMap(attacker) && game.isOnGroundMap(target)) {
                    targetPosition = UnitPosition.of(game.getBoard(attacker)
                          .embeddedBoardPosition(target.getBoardId()));
                } else if (game.isOnGroundMap(attacker) && game.isOnGroundMap(target)) {
                    // Different ground maps, here replace both positions with their respective atmo map hexes
                    aPos = game.getBoard(target).embeddedBoardPosition(attacker.getBoardId());
                    targetPosition = UnitPosition.of(game.getBoard(attacker)
                          .embeddedBoardPosition(target.getBoardId()));
                } else {
                    targetPosition = UnitPosition.of(target);
                }
            } else {
                targetPosition = UnitPosition.of(target);
            }
        }

        // When the above methods all deliver BoardLocations, matching boardIds can be checked:
        //        final int attackerBoardId = attacker.getBoardId();
        //        if (targetPositions.stream().anyMatch(bl -> !bl.isOnBoard(attackerBoardId))) {
        //            LOGGER.error("Target Coords must be on the same board as the attacker!");
        //        }

        FacingArc facingArc = FacingArc.valueOf(weaponEntity.getWeaponArc(weaponId));
        return facingArc.isInsideArc(UnitPosition.of(aPos, facing), targetPosition);
    }

    public static boolean isInArc(Coords src, int facing, Targetable target, int arc) {
        FacingArc facingArc = FacingArc.valueOf(arc);
        return facingArc.isInsideArc(UnitPosition.of(src, facing), UnitPosition.of(target));
    }

    public static boolean isInArc(Coords src, int facing, Coords dest, int arc) {
        FacingArc facingArc = FacingArc.valueOf(arc);
        return facingArc.isInsideArc(UnitPosition.of(src, facing), UnitPosition.of(dest));
    }

    @SuppressWarnings("unused")
    public static boolean isInArc(Coords src, int facing, List<Coords> destV, int arc) {
        FacingArc facingArc = FacingArc.valueOf(arc);
        return facingArc.isInsideArc(UnitPosition.of(src, facing), UnitPosition.of(destV));
    }

    private static int getFacing(int weaponId, Entity attacker) {
        int facing = attacker.isSecondaryArcWeapon(weaponId) ? attacker.getSecondaryFacing() : attacker.getFacing();

        if ((attacker instanceof Tank tank) && (tank.getEquipment(weaponId).getLocation() == tank.getLocTurret2())) {
            facing = tank.getDualTurretFacing();
        }

        if (attacker.getEquipment(weaponId).isMekTurretMounted()) {
            facing = attacker.getSecondaryFacing() + (attacker.getEquipment(weaponId).getFacing() % 6);
        }
        return facing;
    }

    /**
     * Returns true if the target is in the specified arc. Note: This has to take vectors of coordinates to account for
     * potential secondary positions
     *
     * @param src    the attack coordinates
     * @param facing the appropriate attacker sfacing
     * @param destV  A vector of target coordinates
     * @param arc    the arc
     */
    @Deprecated(forRemoval = true, since = "0.50.07")
    public static boolean isInArcOld(Coords src, int facing, List<Coords> destV,
          int arc) {
        if ((src == null) || (destV == null)) {
            return true;
        }

        // Jay: I have to adjust this to take in vectors of coordinates to
        // account for secondary positions of the
        // target - I am fairly certain that secondary positions of the attacker
        // shouldn't matter because you don't get
        // to move the angle based on the secondary positions

        // if any of the destination coords are in the right place, then return
        // true
        for (Coords dest : destV) {
            // Sometimes we get non-null destV containing null Coord entries.
            if (null == dest) {
                return true;
            }

            // calculate firing angle
            int fa = src.degree(dest) - (facing * 60);
            if (fa < 0) {
                fa += 360;
            }
            // is it in the specifed arc?
            switch (arc) {
                case Compute.ARC_FORWARD:
                    if ((fa >= 300) || (fa <= 60)) {
                        return true;
                    }
                    break;
                case Compute.ARC_RIGHT_ARM:
                    if ((fa >= 300) || (fa <= 120)) {
                        return true;
                    }
                    break;
                case Compute.ARC_LEFT_ARM:
                    if ((fa >= 240) || (fa <= 60)) {
                        return true;
                    }
                    break;
                case Compute.ARC_REAR, Compute.ARC_AFT:
                    if ((fa > 120) && (fa < 240)) {
                        return true;
                    }
                    break;
                case Compute.ARC_RIGHT_SIDE:
                    if ((fa > 60) && (fa <= 120)) {
                        return true;
                    }
                    break;
                case Compute.ARC_LEFT_SIDE:
                    if ((fa < 300) && (fa >= 240)) {
                        return true;
                    }
                    break;
                case Compute.ARC_MAIN_GUN:
                    if ((fa >= 240) || (fa <= 120)) {
                        return true;
                    }
                    break;
                case Compute.ARC_360:
                    return true;
                case Compute.ARC_NORTH:
                    if ((fa >= 270) || (fa <= 30)) {
                        return true;
                    }
                    break;
                case Compute.ARC_EAST:
                    if ((fa >= 30) && (fa <= 150)) {
                        return true;
                    }
                    break;
                case Compute.ARC_WEST:
                    if ((fa >= 150) && (fa <= 270)) {
                        return true;
                    }
                    break;
                case Compute.ARC_NOSE:
                    if ((fa > 300) || (fa < 60)) {
                        return true;
                    }
                    break;
                case Compute.ARC_NOSE_WPL:
                    if ((fa > 240) || (fa < 120)) {
                        return true;
                    }
                    break;
                case Compute.ARC_LEFT_WING:
                    if ((fa > 300) || (fa <= 0)) {
                        return true;
                    }
                    break;
                case Compute.ARC_LEFT_WING_WPL:
                    if ((fa > 240) || (fa < 60)) {
                        return true;
                    }
                    break;
                case Compute.ARC_RIGHT_WING:
                    if ((fa >= 0) && (fa < 60)) {
                        return true;
                    }
                    break;
                case Compute.ARC_RIGHT_WING_WPL:
                    if ((fa > 300) || (fa < 120)) {
                        return true;
                    }
                    break;
                case Compute.ARC_LEFT_WING_AFT:
                    if ((fa >= 180) && (fa < 240)) {
                        return true;
                    }
                    break;
                case Compute.ARC_LEFT_WING_AFT_WPL:
                    if ((fa > 120) && (fa < 300)) {
                        return true;
                    }
                    break;
                case Compute.ARC_RIGHT_WING_AFT:
                    if ((fa > 120) && (fa <= 180)) {
                        return true;
                    }
                    break;
                case Compute.ARC_RIGHT_WING_AFT_WPL:
                    if ((fa > 60) && (fa < 240)) {
                        return true;
                    }
                    break;
                case Compute.ARC_AFT_WPL:
                    if ((fa > 60) && (fa < 300)) {
                        return true;
                    }
                    break;
                case Compute.ARC_LEFT_SIDE_SPHERE:
                    if ((fa > 240) || (fa < 0)) {
                        return true;
                    }
                    break;
                case Compute.ARC_LEFT_SIDE_SPHERE_WPL:
                    if ((fa > 180) || (fa < 60)) {
                        return true;
                    }
                    break;
                case Compute.ARC_RIGHT_SIDE_SPHERE:
                    if ((fa > 0) && (fa < 120)) {
                        return true;
                    }
                    break;
                case Compute.ARC_RIGHT_SIDE_SPHERE_WPL:
                    if ((fa > 300) || (fa < 180)) {
                        return true;
                    }
                    break;
                case Compute.ARC_LEFT_SIDE_AFT_SPHERE:
                    if ((fa > 180) && (fa < 300)) {
                        return true;
                    }
                    break;
                case Compute.ARC_LEFT_SIDE_AFT_SPHERE_WPL:
                    if ((fa > 120) && (fa < 360)) {
                        return true;
                    }
                    break;
                case Compute.ARC_RIGHT_SIDE_AFT_SPHERE:
                    if ((fa > 60) && (fa < 180)) {
                        return true;
                    }
                    break;
                case Compute.ARC_RIGHT_SIDE_AFT_SPHERE_WPL:
                    if ((fa > 0) && (fa < 240)) {
                        return true;
                    }
                    break;
                case Compute.ARC_LEFT_BROADSIDE:
                    if ((fa >= 240) && (fa <= 300)) {
                        return true;
                    }
                    break;
                case Compute.ARC_LEFT_BROADSIDE_WPL:
                    if ((fa > 180) && (fa <= 360)) {
                        return true;
                    }
                    break;
                case Compute.ARC_RIGHT_BROADSIDE:
                    if ((fa >= 60) && (fa <= 120)) {
                        return true;
                    }
                    break;
                case Compute.ARC_RIGHT_BROADSIDE_WPL:
                    if ((fa > 0) && (fa < 180)) {
                        return true;
                    }
                    break;
                case Compute.ARC_LEFT_SPHERE_GROUND:
                    if ((fa >= 180) && (fa < 360)) {
                        return true;
                    }
                    break;
                case Compute.ARC_RIGHT_SPHERE_GROUND:
                    if ((fa >= 0) && (fa < 180)) {
                        return true;
                    }
                    break;
                case Compute.ARC_TURRET:
                    if ((fa >= 330) || (fa <= 30)) {
                        return true;
                    }
                    break;
                case Compute.ARC_SPONSON_TURRET_LEFT:
                case Compute.ARC_PINTLE_TURRET_LEFT:
                    if ((fa >= 180) || (fa == 0)) {
                        return true;
                    }
                    break;
                case Compute.ARC_SPONSON_TURRET_RIGHT:
                case Compute.ARC_PINTLE_TURRET_RIGHT:
                    if ((fa >= 0) && (fa <= 180)) {
                        return true;
                    }
                    break;
                case Compute.ARC_PINTLE_TURRET_FRONT:
                    if ((fa >= 270) || (fa <= 90)) {
                        return true;
                    }
                    break;
                case Compute.ARC_PINTLE_TURRET_REAR:
                    if ((fa >= 90) && (fa <= 270)) {
                        return true;
                    }
                    break;
                case Compute.ARC_VGL_FRONT:
                    return (fa >= 270) || (fa <= 90);
                case Compute.ARC_VGL_RF:
                    return (fa >= 330) || (fa <= 150);
                case Compute.ARC_VGL_RR:
                    return (fa >= 30) && (fa <= 210);
                case Compute.ARC_VGL_REAR:
                    return (fa >= 90) && (fa <= 270);
                case Compute.ARC_VGL_LR:
                    return (fa >= 150) && (fa <= 330);
                case Compute.ARC_VGL_LF:
                    return (fa >= 210) || (fa <= 30);
            }
        }
        // if we got here then no matches
        return false;
    }

    private ComputeArc() {
    }
}

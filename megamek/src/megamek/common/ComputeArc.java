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

import megamek.logging.MMLogger;

import java.util.*;

public class ComputeArc {

    private final static MMLogger LOGGER = MMLogger.create(ComputeArc.class);

    /**
     * Checks to see if a target is in arc of the specified weapon, on the specified entity
     */
    public static boolean isInArc(Game game, int attackerId, int weaponId, Targetable target) {
        Entity attacker = game.getEntity(attackerId);

        if ((attacker == null) || (target == null)) {
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

        int facing = attacker.isSecondaryArcWeapon(weaponId) ? attacker.getSecondaryFacing() : attacker.getFacing();

        if ((attacker instanceof Tank tank) && (tank.getEquipment(weaponId).getLocation() == tank.getLocTurret2())) {
            facing = tank.getDualTurretFacing();
        }

        if (attacker.getEquipment(weaponId).isMekTurretMounted()) {
            facing = attacker.getSecondaryFacing() + (attacker.getEquipment(weaponId).getFacing() % 6);
        }

        Coords aPos = attacker.getPosition();
        Coords tPos = target.getPosition();

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
        }

        // AMS defending against Ground to Air fire needs to calculate arc based on the closest flight path
        // Technically it's an AirToGround attack since the AMS is on the aircraft
        if (Compute.isAirToGround(attacker, target)
                && (attacker.getEquipment(weaponId).getType().hasFlag(WeaponType.F_AMS)
                || attacker.getEquipment(weaponId).getType().hasFlag(WeaponType.F_AMSBAY))) {
            aPos = Compute.getClosestFlightPath(target.getId(), target.getPosition(), attacker);
        }

        List<Coords> targetPositions = new ArrayList<>();
        targetPositions.add(tPos);
        targetPositions.addAll(target.getSecondaryPositions().values());
        targetPositions.removeIf(Objects::isNull);

        if (CrossBoardAttackHelper.isCrossBoardArtyAttack(attacker, target, game)) {
            // When attacking between two ground boards, replace the attacker and target positions with the positions
            // of the boards themselves on the atmospheric map
            // When the ground boards are only connected through a high atmospheric map, the arrangement of
            // the maps is unkown and the arc cannot be tested; therefore return false in that case, although
            // a distance could be computed
            Board attackerAtmoBoard = game.getEnclosingBoard(game.getBoard(attacker));
            Board targetAtmoBoard = game.getEnclosingBoard(game.getBoard(target));
            if (attackerAtmoBoard.getBoardId() == targetAtmoBoard.getBoardId()) {
                aPos = attackerAtmoBoard.embeddedBoardLocation(attacker.getBoardId());
                targetPositions.clear();
                targetPositions.add(attackerAtmoBoard.embeddedBoardLocation(target.getBoardId()));
            } else {
                return false;
            }
        }

        if (CrossBoardAttackHelper.isOrbitToSurface(attacker, target, game)) {
            // For this attack, the ground row hex enclosing the ground map target must be in arc; replace position
            Board targetAtmoBoard = game.getEnclosingBoard(game.getBoard(target.getBoardId()));
            targetPositions.clear();
            targetPositions.add(game.getBoard(attacker).embeddedBoardLocation(targetAtmoBoard.getBoardId()));
        }

        if (Compute.isAirToAir(game, attacker, target) && !game.onTheSameBoard(attacker, target)
                && (game.onDirectlyConnectedBoards(attacker, target) || CrossBoardAttackHelper.onGroundMapsWithinOneAtmoMap(game, attacker, target))) {
            // In A2A attacks between different maps (only ground/ground, ground/atmo or atmo/ground), replace the
            // position of the unit on the ground map with the position of the ground map itself in the atmo map
            if (game.isOnGroundMap(attacker) && game.isOnAtmosphericMap(target)) {
                aPos = game.getBoard(target).embeddedBoardLocation(attacker.getBoardId());
            } else if (game.isOnAtmosphericMap(attacker) && game.isOnGroundMap(target)) {
                targetPositions.clear();
                targetPositions.add(game.getBoard(attacker).embeddedBoardLocation(target.getBoardId()));
            } else if (game.isOnGroundMap(attacker) && game.isOnGroundMap(target)) {
                // Different ground maps, here replace both positions with their respective atmo map hexes
                aPos = game.getBoard(target).embeddedBoardLocation(attacker.getBoardId());
                targetPositions.clear();
                targetPositions.add(game.getBoard(attacker).embeddedBoardLocation(target.getBoardId()));
            }
        }

        // When the above methods all deliver BoardLocations, matching boardIds can be checked:
//        final int attackerBoardId = attacker.getBoardId();
//        if (targetPositions.stream().anyMatch(bl -> !bl.isOnBoard(attackerBoardId))) {
//            LogManager.getLogger().error("Target Coords must be on the same board as the attacker!");
//        }

        return isInArc(aPos, facing, targetPositions, attacker.getWeaponArc(weaponId));
    }

    public static boolean isInArc(Coords src, int facing, Targetable target, int arc) {
        List<Coords> targetPositions = new ArrayList<>();
        targetPositions.add(target.getPosition());
        targetPositions.addAll(target.getSecondaryPositions().values());
        targetPositions.removeIf(Objects::isNull);
        return isInArc(src, facing, targetPositions, arc);
    }

    public static boolean isInArc(Coords src, int facing, Coords dest, int arc) {
        return isInArc(src, facing, List.of(dest), arc);
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
    public static boolean isInArc(Coords src, int facing, List<Coords> destV,
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
                case Compute.ARC_RIGHTARM:
                    if ((fa >= 300) || (fa <= 120)) {
                        return true;
                    }
                    break;
                case Compute.ARC_LEFTARM:
                    if ((fa >= 240) || (fa <= 60)) {
                        return true;
                    }
                    break;
                case Compute.ARC_REAR, Compute.ARC_AFT:
                    if ((fa > 120) && (fa < 240)) {
                        return true;
                    }
                    break;
                case Compute.ARC_RIGHTSIDE:
                    if ((fa > 60) && (fa <= 120)) {
                        return true;
                    }
                    break;
                case Compute.ARC_LEFTSIDE:
                    if ((fa < 300) && (fa >= 240)) {
                        return true;
                    }
                    break;
                case Compute.ARC_MAINGUN:
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
                case Compute.ARC_LWING:
                    if ((fa > 300) || (fa <= 0)) {
                        return true;
                    }
                    break;
                case Compute.ARC_LWING_WPL:
                    if ((fa > 240) || (fa < 60)) {
                        return true;
                    }
                    break;
                case Compute.ARC_RWING:
                    if ((fa >= 0) && (fa < 60)) {
                        return true;
                    }
                    break;
                case Compute.ARC_RWING_WPL:
                    if ((fa > 300) || (fa < 120)) {
                        return true;
                    }
                    break;
                case Compute.ARC_LWINGA:
                    if ((fa >= 180) && (fa < 240)) {
                        return true;
                    }
                    break;
                case Compute.ARC_LWINGA_WPL:
                    if ((fa > 120) && (fa < 300)) {
                        return true;
                    }
                    break;
                case Compute.ARC_RWINGA:
                    if ((fa > 120) && (fa <= 180)) {
                        return true;
                    }
                    break;
                case Compute.ARC_RWINGA_WPL:
                    if ((fa > 60) && (fa < 240)) {
                        return true;
                    }
                    break;
                case Compute.ARC_AFT_WPL:
                    if ((fa > 60) && (fa < 300)) {
                        return true;
                    }
                    break;
                case Compute.ARC_LEFTSIDE_SPHERE:
                    if ((fa > 240) || (fa < 0)) {
                        return true;
                    }
                    break;
                case Compute.ARC_LEFTSIDE_SPHERE_WPL:
                    if ((fa > 180) || (fa < 60)) {
                        return true;
                    }
                    break;
                case Compute.ARC_RIGHTSIDE_SPHERE:
                    if ((fa > 0) && (fa < 120)) {
                        return true;
                    }
                    break;
                case Compute.ARC_RIGHTSIDE_SPHERE_WPL:
                    if ((fa > 300) || (fa < 180)) {
                        return true;
                    }
                    break;
                case Compute.ARC_LEFTSIDEA_SPHERE:
                    if ((fa > 180) && (fa < 300)) {
                        return true;
                    }
                    break;
                case Compute.ARC_LEFTSIDEA_SPHERE_WPL:
                    if ((fa > 120) && (fa < 360)) {
                        return true;
                    }
                    break;
                case Compute.ARC_RIGHTSIDEA_SPHERE:
                    if ((fa > 60) && (fa < 180)) {
                        return true;
                    }
                    break;
                case Compute.ARC_RIGHTSIDEA_SPHERE_WPL:
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

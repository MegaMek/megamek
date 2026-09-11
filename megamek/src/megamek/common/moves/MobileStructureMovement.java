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

package megamek.common.moves;

import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import megamek.common.Hex;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.enums.BuildingType;
import megamek.common.enums.MoveStepType;
import megamek.common.game.Game;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.IBuilding;
import megamek.common.units.MobileStructure;
import megamek.common.units.Terrains;
import megamek.common.units.WallRules;

/** TO:AUE pp.33–36. The movement engine represents one MP as four quarter-points for these units. */
public final class MobileStructureMovement {
    public static final int PROHIBITED = Integer.MAX_VALUE;

    private MobileStructureMovement() {
    }

    public static MobileStructure undercarriage(Game game, int boardId, Coords coords, int elevation, int height) {
        return game.getBoard(boardId).getBuildingsAt(coords).stream()
              .filter(MobileStructure.class::isInstance).map(MobileStructure.class::cast)
              .filter(mobile -> mobile.getMovementMode() == EntityMovementMode.TRACKED
                    && elevation + height + 1 <= mobile.getBaseElevation(coords)).findFirst().orElse(null);
    }

    public static boolean canPassUnder(megamek.common.units.Entity entity) {
        if (!(entity instanceof megamek.common.units.Tank)) {
            return true;
        }
        return entity.getMovementMode().isTracked()
              || (entity.getMovementMode() == EntityMovementMode.WHEELED
                    && (entity.hasAbility(megamek.common.options.OptionsConstants.PILOT_CROSS_COUNTRY)
                          || entity.hasQuirk(megamek.common.options.OptionsConstants.QUIRK_POS_SCOUT_BIKE)));
    }

    /** The hull is not a legal forced-displacement destination; its roof and undercarriage remain usable. */
    public static MobileStructure displacementObstacle(megamek.common.units.Entity entity, Coords from, Coords to) {
        if (entity.getGame() == null || entity instanceof MobileStructure || from == null || to == null) {
            return null;
        }
        Board board = entity.getGame().getBoard(entity);
        Hex source = board.getHex(from);
        Hex destination = board.getHex(to);
        if (source == null || destination == null) { return null; }
        int base = Math.max(source.getLevel() + entity.getElevation(), destination.floor()) - destination.getLevel();
        return board.getBuildingsAt(to).stream().filter(MobileStructure.class::isInstance)
              .map(MobileStructure.class::cast).filter(mobile -> base < mobile.getBaseElevation(to) + mobile.getHeight(to)
                    && (base + entity.height() + 1 > mobile.getBaseElevation(to)
                          || mobile.getMovementMode() == EntityMovementMode.TRACKED && !canPassUnder(entity)))
              .findFirst().orElse(null);
    }

    /** A surfacing unit may attempt this collision, but the server leaves it at its original depth (TO:AUE p.41). */
    public static MobileStructure surfacingObstacle(megamek.common.units.Entity unit, Coords coords,
          int fromElevation, int toElevation) {
        if (unit instanceof MobileStructure || toElevation <= fromElevation || unit.getGame() == null
              || !unit.getGame().getBoard(unit).getHex(coords).containsTerrain(Terrains.WATER)) {
            return null;
        }
        return unit.getGame().getBoard(unit).getBuildingsAt(coords).stream()
              .filter(MobileStructure.class::isInstance).map(MobileStructure.class::cast)
              .filter(mobile -> fromElevation + unit.height() + 1 <= mobile.getBaseElevation(coords)
                    && toElevation + unit.height() + 1 > mobile.getBaseElevation(coords)).findFirst().orElse(null);
    }

    public static boolean isMovementStep(MoveStepType type) {
        return switch (type) {
            case FORWARDS, BACKWARDS, LATERAL_LEFT, LATERAL_RIGHT, LATERAL_LEFT_BACKWARDS,
                  LATERAL_RIGHT_BACKWARDS, TURN_LEFT, TURN_RIGHT, UP, DOWN, HOVER -> true;
            default -> false;
        };
    }

    public static Set<Coords> enteredHexes(MobileStructure unit, Coords from, int fromFacing, Coords to, int toFacing) {
        Set<Coords> entered = new HashSet<>(unit.computeBuildingCoordsForPositionAndFacing(to, toFacing));
        if (fromFacing != toFacing) {
            MobileStructureGeometry.contacts(unit, from, fromFacing, to, toFacing).stream()
                  .map(MobileStructureGeometry.Contact::boardHex).forEach(entered::add);
        }
        entered.removeAll(unit.computeBuildingCoordsForPositionAndFacing(from, fromFacing));
        return entered;
    }

    /** Absolute physical extent, including the two motive levels of a ground mobile structure. */
    public record HullInterval(int bottom, int top) {
        public boolean intersects(int base, int roof) { return roof > bottom && base < top; }
    }

    /** Swept height at each contacted cell; a short local hex does not inherit the tallest hex's height. */
    public static Map<Coords, HullInterval> sweptHull(Game game, MobileStructure unit, Coords from, int fromFacing,
          int fromElevation, Coords to, int toFacing, int toElevation, Set<Coords> entered) {
          Map<CubeCoords, Coords> source = unit.computeLayoutForPositionAndFacing(from, fromFacing);
          List<Coords> startFootprint = MobileStructureLinkage.footprint(unit, from, fromFacing, fromElevation);
          List<Coords> endFootprint = MobileStructureLinkage.footprint(unit, to, toFacing, toElevation);
          List<MobileStructureGeometry.Contact> contacts = new java.util.ArrayList<>(
              MobileStructureGeometry.contacts(unit, from, fromFacing, to, toFacing));
        if (fromElevation != toElevation) {
            unit.computeLayoutForPositionAndFacing(to, toFacing).forEach((relative, cell) ->
                  contacts.add(new MobileStructureGeometry.Contact(relative, cell, 1)));
        }
        Map<Coords, HullInterval> result = new HashMap<>();
        for (MobileStructureGeometry.Contact contact : contacts) {
            if (!entered.contains(contact.boardHex())) { continue; }
            HullInterval start = hullAt(game, unit, contact.hex(), from, fromElevation, source.get(contact.hex()), startFootprint);
            HullInterval end = hullAt(game, unit, contact.hex(), to, toElevation, contact.boardHex(), endFootprint);
            HullInterval swept = new HullInterval(Math.min(start.bottom(), end.bottom()), Math.max(start.top(), end.top()));
            result.merge(contact.boardHex(), swept, (a, b) ->
                  new HullInterval(Math.min(a.bottom(), b.bottom()), Math.max(a.top(), b.top())));
        }
        return result;
    }

    private static HullInterval hullAt(Game game, MobileStructure unit, CubeCoords relative, Coords origin,
          int elevation, Coords cell, List<Coords> footprint) {
        int motives = unit.getMovementMode() == EntityMovementMode.TRACKED ? 2 : 0;
        int surface = terrain(game, unit, cell).getLevel();
        int bottom;
        if (MobileStructureAirMovement.isAir(unit)) {
            bottom = MobileStructureAirMovement.absoluteBase(unit, origin, elevation);
        } else if (unit.getNavalState().isSinking()) {
            bottom = surface + elevation + unit.getNavalState().getBaseOffsets()
                  .getOrDefault(relative, unit.getStructureBaseElevation()) - motives;
        } else if (motives > 0) {
            bottom = MobileStructureSupport.level(game, unit, footprint, cell);
        } else {
            bottom = surface + unit.getStructureBaseElevation() + elevation;
        }
        return new HullInterval(bottom, bottom + motives + unit.getInternalBuilding().getHeight(relative));
    }

    /** MP and server demolition use exactly the same physical wall contact, including elevation changes. */
    public static List<WallRules.Segment> wallContacts(Game game, MobileStructure unit, Coords from, int fromFacing,
          int fromElevation, Coords to, int toFacing, int toElevation, Set<Coords> entered) {
        Map<Coords, HullInterval> hull = sweptHull(game, unit, from, fromFacing, fromElevation, to, toFacing, toElevation, entered);
        return WallRules.intersecting(game, unit.getBoardId(), entered).stream()
              .filter(segment -> hull.entrySet().stream().anyMatch(cell -> segment.borders(cell.getKey())
                    && cell.getValue().intersects(segment.baseAltitude(), segment.topAltitude()))).toList();
    }

    public static int cost(Game game, MobileStructure unit, Coords from, int fromFacing, int fromElevation,
          Coords to, int toFacing, int toElevation) {
        if (from == null || to == null) {
            return PROHIBITED;
        }
        List<MobileStructure> modules = MobileStructureLinkage.group(unit);
        List<Coords> footprint = MobileStructureLinkage.footprint(unit, to, toFacing, toElevation);
        List<Coords> oldFootprint = MobileStructureLinkage.footprint(unit, from, fromFacing, fromElevation);
        Set<Coords> allEntered = new HashSet<>();
        int maximum = 0;
        for (MobileStructure module : modules) {
            MobileStructureLinkage.Pose start = MobileStructureLinkage.pose(unit, module, from, fromFacing, fromElevation);
            MobileStructureLinkage.Pose end = MobileStructureLinkage.pose(unit, module, to, toFacing, toElevation);
            int cost = moduleCost(game, module, start.position(), start.facing(), start.elevation(),
                  end.position(), end.facing(), end.elevation(), footprint, oldFootprint);
            if (cost == PROHIBITED) {
                return PROHIBITED;
            }
            maximum = Math.max(maximum, cost);
            allEntered.addAll(enteredHexes(module, start.position(), start.facing(), end.position(), end.facing()));
        }
        allEntered.removeAll(oldFootprint);
        if (unit.getMovementMode() == EntityMovementMode.TRACKED && fromFacing == toFacing && !from.equals(to)
              && !allEntered.isEmpty()) {
            Map<Integer, Long> changes = allEntered.stream().map(coords -> MobileStructureSupport.level(game, unit, footprint, coords)
                  - MobileStructureSupport.level(game, unit, oldFootprint, coords.translated(to.direction(from))))
                  .collect(java.util.stream.Collectors.groupingBy(change -> change, java.util.stream.Collectors.counting()));
            long sameChange = changes.values().stream().mapToLong(Long::longValue).max().orElse(0);
            if (sameChange * 2 <= allEntered.size() && allEntered.size() > 1) {
                return PROHIBITED;
            }
        }
        return maximum;
    }

    private static int moduleCost(Game game, MobileStructure unit, Coords from, int fromFacing, int fromElevation,
          Coords to, int toFacing, int toElevation, List<Coords> footprint, List<Coords> oldFootprint) {
        if (from == null || to == null) {
            return PROHIBITED;
        }
        boolean turning = fromFacing != toFacing;
        if (turning && ((Math.floorMod(toFacing - fromFacing, 6) != 1
              && Math.floorMod(toFacing - fromFacing, 6) != 5)
              || MobileStructureGeometry.pivotFor(unit, from, fromFacing, to, toFacing) == null)) {
            return PROHIBITED;
        }
        if (!turning && from.distance(to) > 1) {
            return PROHIBITED;
        }
        int maximum = 1;
        Set<Coords> entered = enteredHexes(unit, from, fromFacing, to, toFacing);
        entered.removeAll(oldFootprint);
        boolean water = unit.isWaterStructure();
        if (MobileStructureAirMovement.isAir(unit)) {
            int startBase = MobileStructureAirMovement.absoluteBase(unit, from, fromElevation);
            int endBase = MobileStructureAirMovement.absoluteBase(unit, to, toElevation);
            // Spheroid small craft cannot taxi; landing and liftoff are separate actions (TW pp.87–88).
            if (!game.getBoard(unit).isGround() || startBase <= MobileStructureAirMovement.landingSurface(unit, oldFootprint)
                  || Math.abs(endBase - startBase) > 1
                  || (!from.equals(to) && !turning && startBase != endBase)
                  || endBase <= MobileStructureAirMovement.landingSurface(unit, footprint)
                  || footprint.stream().noneMatch(c -> game.getBoard(unit).contains(c))) {
                return PROHIBITED;
            }
            if (startBase != endBase) {
                entered.addAll(footprint);
            }
            for (Coords coords : entered) {
                Hex hex = game.getBoard(unit).getHex(coords);
                if (hex != null && (hex.containsTerrain(Terrains.IMPASSABLE)
                      || MobileStructureAirMovement.obstructionCeiling(unit, coords) >= endBase)) {
                    return PROHIBITED;
                }
            }
            return turning ? unit.getMaximumMPQuarters() : 4;
        }
        if (Math.abs(toElevation - fromElevation) > 1
              || (toElevation != fromElevation && (unit.getMovementMode() == EntityMovementMode.TRACKED
                    || unit.getMovementMode() == EntityMovementMode.NAVAL
                    || (unit.getMovementMode().isSubmarine() && unit.getDepthChangesThisTurn() > 0)))
              || (unit.getMovementMode().isSubmarine() && toElevation > 0)
              || (unit.getMovementMode() == EntityMovementMode.VTOL && toElevation < 0)) {
            return PROHIBITED;
        }
        if (toElevation != fromElevation) {
            entered.addAll(footprint);
        }
        List<WallRules.Segment> walls = wallContacts(game, unit, from, fromFacing, fromElevation, to, toFacing, toElevation, entered);
        for (Coords coords : entered) {
            Hex hex = game.getBoard(unit.getBoardId()).getHex(coords);
            // Part of a Mobile Structure may extend beyond the map (TO:AUE p.35).
            if (hex == null) {
                continue;
            }
            if (hex.containsTerrain(Terrains.IMPASSABLE)) {
                return PROHIBITED;
            }
            int depth = hex.depth();
            if (water) {
                if (!hex.containsTerrain(Terrains.WATER)) {
                    return PROHIBITED;
                }
                continue; // Shallow water causes grounding during execution, rather than preventing entry.
            }
            if ((depth >= 3 && !unit.hasEnvironmentalSealing()) || depth >= 16) {
                return PROHIBITED;
            }
            int terrain = depth >= 3 ? 2 : depth > 0 ? 1 : 0;
            terrain += hex.terrainLevel(Terrains.ROUGH) >= 2 ? 1 : 0;
            terrain += Math.max(hex.terrainLevel(Terrains.WOODS), hex.terrainLevel(Terrains.JUNGLE)) >= 3 ? 1 : 0;
            terrain += hex.terrainLevel(Terrains.RUBBLE) >= 6 ? 2 : 0;
            terrain += hex.containsTerrain(Terrains.INDUSTRIAL) ? 1 : 0;
            if (walls.stream().anyMatch(segment -> segment.borders(coords)
                  && segment.building().getBuildingType() == BuildingType.HARDENED)) {
                terrain++;
            }
            int supportedLevel = MobileStructureSupport.level(game, unit, footprint, coords);
            int previousLevel = MobileStructureSupport.level(game, unit, oldFootprint, coords.translated(to.direction(from)));
            int change = Math.abs(supportedLevel - previousLevel);
            if (change >= 3) {
                return PROHIBITED;
            }
            terrain += change == 2 ? 1 : 0;
            IBuilding building = game.getBuildingAt(coords, unit.getBoardId()).orElse(null);
            if (building != null && building != unit
                  && (!(building instanceof MobileStructure mobile) || !MobileStructureLinkage.group(unit).contains(mobile))) {
                if (building.getBldgClass() == IBuilding.CASTLE_BRIAN
                      || (building.getBldgClass() == IBuilding.FORTRESS && building.getBuildingType() == BuildingType.HARDENED)) {
                    // The server resolves a paid attempt without entering this prohibited obstruction (TO:AUE p.36).
                } else if (building.getBldgClass() == IBuilding.FORTRESS) {
                    terrain += building.getBuildingType() == BuildingType.HEAVY ? 2 : 1;
                } else if ((building.getBldgClass() == IBuilding.WALL || building.getBldgClass() == IBuilding.GUN_EMPLACEMENT)
                      && building.getBuildingType() == BuildingType.HARDENED) {
                    terrain++;
                }
            }
            maximum = Math.max(maximum, 1 + terrain);
        }
        if (footprint.stream().noneMatch(c -> game.getBoard(unit.getBoardId()).contains(c))) {
            return PROHIBITED;
        }
        return turning ? unit.getMaximumMPQuarters() : maximum * 4;
    }

    /** Off-board portions continue the level at the nearest map edge (TO:AUE p.35). */
    public static Hex terrain(Game game, MobileStructure unit, Coords coords) {
        Board board = game.getBoard(unit.getBoardId());
        Hex onBoard = board.getHex(coords);
        Integer tunnelFloor = megamek.common.units.MobileStructurePortalRules.supportElevation(unit, coords);
        if (onBoard != null && tunnelFloor != null) {
            return new Hex(onBoard.getLevel() + tunnelFloor);
        }
        if (onBoard != null) {
            return onBoard;
        }
        Hex edge = board.getHex(new Coords(Math.clamp(coords.getX(), 0, board.getWidth() - 1),
              Math.clamp(coords.getY(), 0, board.getHeight() - 1)));
        return new Hex(edge == null ? 0 : edge.getLevel());
    }

    /** The preview uses the same committed progress as server execution, including any MP left for a second step. */
    public static int plannedCost(Game game, MobileStructure unit, Coords from, int fromFacing, int fromElevation,
          Coords to, int toFacing, int toElevation) {
        int cost = cost(game, unit, from, fromFacing, fromElevation, to, toFacing, toElevation);
        MobileStructure.MovementProgress progress = unit.getMovementProgress();
        if (cost != PROHIBITED && progress != null && from.equals(unit.getPosition())
              && fromFacing == unit.getFacing() && fromElevation == unit.getElevation()
              && progress.destination().equals(to) && progress.facing() == toFacing && progress.elevation() == toElevation) {
            return Math.max(0, cost - progress.quarters());
        }
        return cost;
    }
}

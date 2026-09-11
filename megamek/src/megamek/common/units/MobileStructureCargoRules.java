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

package megamek.common.units;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import megamek.common.bays.Bay;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.moves.MobileStructureLinkage;

/** Door-based mounting and dismounting, including sinking vessels (TO:AUE pp.29,36,84; TW pp.89–90). */
public final class MobileStructureCargoRules {
    private MobileStructureCargoRules() { }

    public record Exit(Coords position, int elevation, int deckElevation, boolean fall,
          EntityMovementType movement, BuildingDesign.BayDoor door) { }

    private record Door(Coords inside, Coords outside, int level, BuildingDesign.BayDoor placement) { }

    private static boolean active(Entity unit) {
        // Carried units need not be deployed. Entity.isActive() includes deployment and is unsuitable here.
        return !unit.isShutDown() && !unit.isManualShutdown() && !unit.isDestroyed() && !unit.isDoomed()
              && unit.getCrew().isActive() && unit.getRecoveryTurn() == 0;
    }

    private static List<Door> doors(MobileStructure carrier, Bay bay, MobileStructureLinkage.Pose pose, Entity passenger) {
        var board = carrier.getGame().getBoard(carrier);
        var footprint = MobileStructureLinkage.footprint(carrier, pose.position(), pose.facing(), pose.elevation());
        var layout = carrier.computeLayoutForPositionAndFacing(pose.position(), pose.facing());
        var placed = BuildingBayDoors.placements(carrier, bay);
        List<Door> result;
        if (!placed.isEmpty()) {
            result = BuildingBayDoors.usablePlacements(carrier, bay, pose.position(), pose.facing()).stream().map(door -> {
                Coords inside = layout.get(door.position().hex());
                return new Door(inside, inside.translated(Math.floorMod(door.facing() + pose.facing(), 6)),
                      BuildingBayDoors.physicalLevel(carrier, door), door);
            }).filter(door -> board.contains(door.inside()) && board.contains(door.outside())
                  && !footprint.contains(door.outside())).toList();
        } else {
            // Old designs contain only a door count; use their perimeter without inventing saved placements.
            result = layout.values().stream().filter(board::contains).flatMap(inside -> inside.allAdjacent().stream()
                  .filter(outside -> board.contains(outside) && !footprint.contains(outside))
                  .map(outside -> new Door(inside, outside, 0, null))).toList();
        }
        if (carrier.getMovementMode() == EntityMovementMode.TRACKED
              && megamek.common.moves.MobileStructureMovement.canPassUnder(passenger) && passenger.height() + 1 <= 2) {
            var under = new ArrayList<>(result);
            result.forEach(door -> under.add(new Door(door.inside(), door.inside(), door.level(), door.placement())));
            return under;
        }
        return result;
    }

    private static MobileStructureLinkage.Pose pose(MobileStructure carrier) {
        return new MobileStructureLinkage.Pose(carrier.getPosition(), carrier.getFacing(), carrier.getElevation());
    }

    private static int base(MobileStructure carrier, MobileStructureLinkage.Pose pose, Coords inside) {
        var game = carrier.getGame();
        if (carrier.getNavalState().isSinking()) {
            var relative = carrier.computeLayoutForPositionAndFacing(pose.position(), pose.facing()).entrySet().stream()
                  .filter(entry -> entry.getValue().equals(inside)).map(Map.Entry::getKey).findFirst().orElseThrow();
            return pose.elevation() + carrier.getNavalState().getBaseOffsets()
                  .getOrDefault(relative, carrier.getStructureBaseElevation());
        }
        if (carrier.getMovementMode() == EntityMovementMode.TRACKED) {
            Integer tunnelFloor = MobileStructurePortalRules.supportElevation(carrier, inside);
            if (tunnelFloor != null) { return tunnelFloor + carrier.getStructureBaseElevation(); }
            return 2 + megamek.common.moves.MobileStructureSupport.level(game, carrier,
                  MobileStructureLinkage.footprint(carrier, pose.position(), pose.facing(), pose.elevation()), inside)
                  - game.getBoard(carrier).getHex(inside).getLevel();
        }
        if (carrier.getMovementMode() == EntityMovementMode.VTOL) {
            return megamek.common.moves.MobileStructureAirMovement.translatedElevation(carrier, pose.position(), inside,
                  pose.elevation());
        }
        return carrier.getStructureBaseElevation() + pose.elevation();
    }

    public static List<Exit> exits(MobileStructure carrier, Entity passenger) {
        return exits(carrier, passenger, pose(carrier));
    }

    public static List<Exit> exits(MobileStructure carrier, Entity passenger, MobileStructureLinkage.Pose pose) {
        if ((passenger instanceof IAero || passenger instanceof VTOL) && active(passenger)
              && carrier.getLoadedUnits().contains(passenger) && !passenger.wasLoadedThisTurn()
              && carrier.getBay(passenger) != null && carrier.getBay(passenger).canUnloadUnits()) {
            var projected = carrier.computeLayoutForPositionAndFacing(pose.position(), pose.facing());
            return BuildingFlightDeckRules.stagingDecks(carrier, passenger).stream().flatMap(deck -> deck.hexes().stream()
                  .map(coords -> carrier.boardToRelative(coords)).filter(projected::containsKey)
                  .map(relative -> {
                      Coords coords = projected.get(relative);
                      int roof = base(carrier, pose, coords) + carrier.getInternalBuilding().getHeight(relative);
                      return new Exit(coords, roof, roof, false, EntityMovementType.MOVE_NONE, null);
                  })).filter(exit -> exit.elevation() >= 0 && carrier.getGame().getBoard(carrier).contains(exit.position())).toList();
        }
        if (carrier.getPosition() == null || !active(passenger) || passenger.wasLoadedThisTurn()
              || carrier.isAirborne() || carrier.isAirborneVTOLorWIGE()) {
            return List.of();
        }
        Bay bay = carrier.getBay(passenger);
        if (bay == null || BuildingBayDoors.usableDoors(carrier, bay, pose.position(), pose.facing())
              <= bay.getNumberUnloadedThisTurn()) {
            return List.of();
        }
        var board = carrier.getGame().getBoard(carrier);
        var doors = doors(carrier, bay, pose, passenger);
        var layout = carrier.computeLayoutForPositionAndFacing(pose.position(), pose.facing());
        boolean sinking = carrier.getNavalState().isSinking();
        boolean submerged = MobileStructureLinkage.group(carrier).stream().allMatch(module -> {
            var projected = MobileStructureLinkage.pose(carrier, module, pose.position(), pose.facing(), pose.elevation());
            return module.computeLayoutForPositionAndFacing(projected.position(), projected.facing()).entrySet().stream()
                  .filter(entry -> board.contains(entry.getValue())).allMatch(entry -> base(module, projected, entry.getValue())
                        + module.getInternalBuilding().getHeight(entry.getKey()) < 0);
        });
        boolean landAdjacent = doors.stream().anyMatch(door -> {
            var hex = board.getHex(door.outside());
            int floor = base(carrier, pose, door.inside()) + door.level()
                  + board.getHex(door.inside()).getLevel() - hex.getLevel();
            return hex.depth() == 0 && Math.abs(floor) <= 2
                  && !passenger.isLocationProhibited(door.outside(), carrier.getBoardId(), 0)
                  && Compute.stackingViolation(carrier.getGame(), passenger, 0, door.outside(),
                        carrier.getBoardId(), carrier, false, true) == null;
        });
        Map<Coords, Exit> result = new LinkedHashMap<>();
        for (Door door : doors) {
            var hex = board.getHex(door.outside());
            int height = layout.entrySet().stream().filter(e -> e.getValue().equals(door.inside()))
                  .mapToInt(e -> carrier.getInternalBuilding().getHeight(e.getKey())).findFirst().orElse(0);
            int deck = base(carrier, pose, door.inside()) + height + board.getHex(door.inside()).getLevel()
                  - hex.getLevel();
            int floor = base(carrier, pose, door.inside()) + door.level()
                  + board.getHex(door.inside()).getLevel() - hex.getLevel();
            boolean water = hex.depth() > 0;
            if (water && submerged && passenger.getMovementMode().isHover()) { continue; }
            int elevation = hex.floor() - hex.getLevel();
            boolean fall = false;
            EntityMovementType movement = EntityMovementType.MOVE_NONE;
            if (sinking && water) {
                boolean canEscape = passenger instanceof Mek || passenger instanceof ProtoMek
                      || passenger instanceof Infantry && passenger.hasUMU()
                      || !submerged && passenger.getMovementMode().isHover();
                if (landAdjacent || !canEscape) { continue; }
                elevation = passenger.hasUMU() ? Math.max(-hex.depth(), Math.min(-1, deck))
                      : passenger.getMovementMode().isHover() ? 0 : -hex.depth();
                fall = !passenger.hasUMU();
            } else if (carrier.isWaterStructure() && water && passenger.hasUMU()) {
                elevation = Math.max(-hex.depth(), Math.min(-1, floor));
            } else if (carrier.isWaterStructure() && water && !submerged
                  && floor >= 0 && passenger.getMovementMode().isVTOL()) {
                elevation = 1;
                movement = EntityMovementType.MOVE_VTOL_WALK;
            } else {
                if (water && (passenger.getMovementMode().isHover() || passenger.getMovementMode().isNaval()
                      || passenger.getMovementMode().isHydrofoil())) { elevation = 0; }
                if (Math.abs(floor - elevation) > 2) { continue; }
            }
            if (passenger.isLocationProhibited(door.outside(), carrier.getBoardId(), elevation)
                  || !passenger.isElevationValid(elevation, hex)
                  || Compute.stackingViolation(carrier.getGame(), passenger, elevation, door.outside(),
                        carrier.getBoardId(), carrier, false, true) != null) {
                continue;
            }
            result.putIfAbsent(door.outside(), new Exit(door.outside(), elevation, deck, fall, movement, door.placement()));
        }
        if (carrier.isWaterStructure() && !sinking && !submerged && !landAdjacent && passenger.getJumpMP() > 0) {
            // TO:AUE p.38 uses TW p.225's naval dismount rules for all carried units. Only the cheapest
            // reachable land destinations are choices; elevation is measured from the actual bay door.
            Map<Coords, Integer> jumpCosts = new LinkedHashMap<>();
            Map<Coords, Exit> jumpExits = new LinkedHashMap<>();
            var footprint = MobileStructureLinkage.footprint(carrier, pose.position(), pose.facing(), pose.elevation());
            for (Door door : doors) {
                int start = board.getHex(door.inside()).getLevel() + base(carrier, pose, door.inside()) + door.level();
                if (start < board.getHex(door.inside()).getLevel()) { continue; }
                for (Coords destination : door.inside().allAtDistanceOrLess(passenger.getJumpMP())) {
                    var hex = board.getHex(destination);
                    if (hex == null || hex.depth() > 0 || footprint.contains(destination)) { continue; }
                    int elevation = jumpSurface(carrier, destination) - hex.getLevel();
                    if (passenger.isLocationProhibited(destination, carrier.getBoardId(), elevation)
                          || !passenger.isElevationValid(elevation, hex)
                          || Compute.stackingViolation(carrier.getGame(), passenger, elevation, destination,
                                carrier.getBoardId(), carrier, false, true) != null) { continue; }
                    int cost = Math.max(door.inside().distance(destination), hex.getLevel() + elevation - start);
                    if (cost <= passenger.getJumpMP() && cost < jumpCosts.getOrDefault(destination, Integer.MAX_VALUE)
                          && jumpPath(carrier, door, destination, start + passenger.getJumpMP(), footprint)) {
                        jumpCosts.put(destination, cost);
                        jumpExits.put(destination, new Exit(destination, elevation, 0, false,
                              EntityMovementType.MOVE_JUMP, door.placement()));
                    }
                }
            }
            int cheapest = jumpCosts.values().stream().mapToInt(Integer::intValue).min().orElse(Integer.MAX_VALUE);
            jumpCosts.forEach((destination, cost) -> {
                if (cost == cheapest) {
                    result.putIfAbsent(destination, jumpExits.get(destination));
                }
            });
        }
        return new ArrayList<>(result.values());
    }

    /** TW pp.53–54: any shortest path is allowed, but it must leave through the selected door. */
    private static boolean jumpPath(MobileStructure carrier, Door door, Coords destination, int ceiling,
          List<Coords> footprint) {
        int distance = door.inside().distance(destination);
        if (distance == 0 || door.outside().distance(destination) != distance - 1) { return false; }
        var board = carrier.getGame().getBoard(carrier);
        var pending = new ArrayDeque<Coords>();
        var seen = new HashSet<Coords>();
        pending.add(door.outside());
        while (!pending.isEmpty()) {
            Coords coords = pending.removeFirst();
            if (!seen.add(coords) || !board.contains(coords) || footprint.contains(coords)
                  || jumpSurface(carrier, coords) > ceiling
                  || carrier.getGame().getEntitiesVector(coords, carrier.getBoardId()).stream()
                        .anyMatch(unit -> unit instanceof Dropship && !unit.isAirborne()
                              && board.getHex(coords).getLevel() + unit.getElevation() + unit.height() > ceiling)) {
                continue;
            }
            if (coords.equals(destination)) { return true; }
            int remaining = coords.distance(destination);
            coords.allAdjacent().stream().filter(next -> next.distance(destination) == remaining - 1)
                  .forEach(pending::addLast);
        }
        return false;
    }

    /** Woods do not impede jumping. Buildings and bridge decks supply the landing surface. */
    private static int jumpSurface(MobileStructure carrier, Coords coords) {
        var board = carrier.getGame().getBoard(carrier);
        var hex = board.getHex(coords);
        var buildings = board.getBuildingsAt(coords);
        var modules = MobileStructureLinkage.group(carrier);
        int roof = buildings.stream().filter(building -> !modules.contains(building))
              .mapToInt(building -> BuildingElevation.roof(building, coords)).max().orElse(0);
        if (buildings.isEmpty()) { roof = Math.max(0, hex.terrainLevel(Terrains.BLDG_ELEV)); }
        return hex.getLevel() + Math.max(roof, Math.max(0, hex.terrainLevel(Terrains.BRIDGE_ELEV)));
    }

    public static Exit exit(MobileStructure carrier, Entity passenger, Coords destination) {
        return exits(carrier, passenger).stream().filter(exit -> exit.position().equals(destination)).findFirst().orElse(null);
    }

    /** The last sinking hex has already been removed; evacuation still uses the original physical footprint. */
    public static List<Coords> evacuationPositions(MobileStructure carrier) {
        return carrier.getInternalBuilding().getOriginalCoordsList().stream()
              .map(hex -> carrier.getPosition().toCube().add(MobileStructureLinkage.rotate(hex, carrier.getFacing())).toOffset())
              .flatMap(hex -> hex.allAtDistanceOrLess(1).stream()).distinct()
              .filter(carrier.getGame().getBoard(carrier)::contains).toList();
    }

    public static boolean canMount(MobileStructure carrier, Entity passenger, Coords position, int absoluteElevation) {
        return canMount(carrier, passenger, position, absoluteElevation, pose(carrier));
    }

    public static boolean canMount(MobileStructure carrier, Entity passenger, Coords position, int absoluteElevation,
          MobileStructureLinkage.Pose pose) {
        if (active(passenger) && !carrier.getNavalState().isSinking()
              && BuildingFlightDeckRules.canLeaveDeckForBay(carrier, passenger, position, absoluteElevation)) {
            return mountingBays(carrier, passenger, position, absoluteElevation, pose).stream().anyMatch(bay ->
                  passenger.getTargetBay() < 0 || passenger.getTargetBay() == bay.getBayNumber());
        }
        if (carrier.getNavalState().isSinking() || carrier.isDestroyed() || carrier.isDoomed()
              || carrier.isAirborne() || carrier.isAirborneVTOLorWIGE() || !active(passenger)
              || !BuildingFlightDeckRules.canEnterBay(passenger)
              || passenger instanceof Aero || passenger instanceof VTOL
              || passenger.isEnemyOf(carrier)) { return false; }
        return mountingBays(carrier, passenger, position, absoluteElevation, pose).stream()
              .anyMatch(bay -> passenger.getTargetBay() < 0 || passenger.getTargetBay() == bay.getBayNumber());
    }

    /** Bay selection follows the same physical doorway used by the server. */
    public static List<Bay> mountingBays(MobileStructure carrier, Entity passenger, Coords position,
          int absoluteElevation, MobileStructureLinkage.Pose pose) {
        boolean fromDeck = BuildingFlightDeckRules.canLeaveDeckForBay(carrier, passenger, position, absoluteElevation);
        return carrier.getTransportBays().stream().filter(bay -> {
            int usableDoors = BuildingBayDoors.usableDoors(carrier, bay, pose.position(), pose.facing());
            return usableDoors > 0 && bay.canLoadAt(passenger, pose.position(), pose.facing())
                  && (passenger instanceof Infantry || usableDoors > bay.getNumberLoadedThisTurn())
                  && (fromDeck || !mountingDoors(carrier, bay, passenger, position, absoluteElevation, pose).isEmpty());
        }).toList();
    }

    private static List<Door> mountingDoors(MobileStructure carrier, Bay bay, Entity passenger,
          Coords position, int absoluteElevation, MobileStructureLinkage.Pose pose) {
        var board = carrier.getGame().getBoard(carrier);
        return doors(carrier, bay, pose, passenger).stream().filter(door -> door.outside().equals(position)
              && Math.abs(board.getHex(door.inside()).getLevel() + base(carrier, pose, door.inside())
                    + door.level() - absoluteElevation) <= 2).toList();
    }

    /** The used physical placement; null denotes a legacy bay with only an aggregate door count. */
    public static BuildingDesign.BayDoor mountingDoor(MobileStructure carrier, Bay bay, Entity passenger,
          Coords position, int absoluteElevation) {
        var matches = mountingDoors(carrier, bay, passenger, position, absoluteElevation, pose(carrier));
        return matches.isEmpty() ? null : matches.getFirst().placement();
    }

    /** The carrier's Load action names one waiting passenger at an actual door, using the plotted carrier pose. */
    public static List<Entity> loadableUnits(MobileStructure carrier, MobileStructureLinkage.Pose pose) {
        var board = carrier.getGame().getBoard(carrier);
        return carrier.getGame().getEntitiesVector().stream().filter(passenger -> passenger != carrier
              && passenger.getBoardId() == carrier.getBoardId() && passenger.getPosition() != null
              && board.contains(passenger.getPosition()) && passenger.isLoadableThisTurn() && !passenger.isDone()
              && passenger.moved != EntityMovementType.MOVE_RUN && passenger.moved != EntityMovementType.MOVE_VTOL_RUN
              && (passenger.mpUsed == 0 || passenger.mpUsed + (passenger.getWalkMP() + 1) / 2
                    <= passenger.getWalkMP())
              && canMount(carrier, passenger, passenger.getPosition(), board.getHex(passenger.getPosition()).getLevel()
                    + passenger.getElevation(), pose)).toList();
    }
}

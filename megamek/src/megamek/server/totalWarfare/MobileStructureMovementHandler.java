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

package megamek.server.totalWarfare;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import megamek.common.Hex;
import megamek.common.IndustrialElevator;
import megamek.common.Report;
import megamek.common.bays.Bay;
import megamek.common.board.Board;
import megamek.common.board.BoardLocation;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.enums.BuildingType;
import megamek.common.moves.MobileStructureLinkage;
import megamek.common.moves.MobileStructureMovement;
import megamek.common.moves.MovePath;
import megamek.common.moves.MoveStep;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.EntityMovementType;
import megamek.common.units.IBuilding;
import megamek.common.units.MobileStructure;
import megamek.common.units.Targetable;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import megamek.common.units.WallRules;

/** Executes committed quarter-MP moves without moving a structure before the entire maneuver has been paid. */
final class MobileStructureMovementHandler extends AbstractTWRuleHandler {
    MobileStructureMovementHandler(TWGameManager manager) {
        super(manager);
    }

    /** Called only after receiveMovement validates phase, connection owner and active turn; does not end the turn. */
    boolean processLinkAction(MobileStructure unit, MovePath path) {
        Set<MobileStructure> affected = new HashSet<>(MobileStructureLinkage.group(unit));
        boolean changed = false;
        if (path.length() == 1 && unit.mpUsed == 0 && !unit.isDone()
              && path.getLastStep().getTarget(getGame()) instanceof MobileStructure other
              && other.getOwnerId() == unit.getOwnerId()) {
            affected.addAll(MobileStructureLinkage.group(other));
            if (affected.stream().noneMatch(m -> m.isDone() || m.mpUsed > 0)) {
                changed = switch (path.getLastStep().getType()) {
                    case MODULE_LINK -> MobileStructureLinkage.link(unit, other);
                    case MODULE_UNLINK -> MobileStructureLinkage.directlyLinked(unit, other)
                          && MobileStructureLinkage.unlink(unit, other);
                    default -> false;
                };
            }
        }
        affected.stream().filter(m -> m != unit).forEach(m -> gameManager.entityUpdate(m.getId()));
        // The initiating entity is the acknowledgement, after all canonical links have reached the client.
        gameManager.entityUpdate(unit.getId());
        return changed;
    }

    void process(MobileStructure unit, MovePath path) {
        List<MobileStructure> modules = MobileStructureLinkage.group(unit);
        if (modules.stream().anyMatch(MobileStructure::isDone)) {
            return;
        }
        if (path.length() == 1 && megamek.common.moves.MobileStructureAirMovement.isAction(path.getLastStep().getType())) {
            new MobileStructureAirMovementHandler(gameManager).process(unit, path.getLastStep().getType(), this);
            finishMovement(unit, modules);
            return;
        }
        boolean hovering = path.length() == 0 && megamek.common.moves.MobileStructureAirMovement.isAirborne(unit);
        if (path.getStepVector().isEmpty() && !hovering) {
            modules.forEach(MobileStructure::cancelMovementProgress);
        }
        modules.forEach(m -> m.delta_distance = 0);
        int budget = modules.stream().mapToInt(m -> m.isWaterStructure()
              ? m.declareWaterSpeed(path.getMobileSpeedQuarters() == null ? path.getMpUsed()
                    : path.getMobileSpeedQuarters(), getGame().getRoundCount()) : m.getMaximumMPQuarters()).min().orElse(0);
          List<MoveStep> steps = new ArrayList<>(path.getStepVector());
        if (hovering) {
            steps.add(new MovePath(getGame(), unit).addStep(megamek.common.enums.MoveStepType.HOVER).getLastStep());
        }
        if (unit.isWaterStructure() && unit.isImmobile() && !unit.isGrounded()) {
            steps.clear();
        }
        int index = 0;
        while (index < steps.size() || (unit.isWaterStructure() && unit.mpUsed < budget && !unit.isGrounded())) {
            MoveStep step;
            if (index < steps.size()) {
                step = steps.get(index++);
            } else {
                MovePath coast = new MovePath(getGame(), unit);
                int relative = Math.floorMod(unit.getTravelDirection() - unit.getFacing(), 6);
                coast.addStep(switch (relative) {
                    case 0 -> megamek.common.enums.MoveStepType.FORWARDS;
                    case 1 -> megamek.common.enums.MoveStepType.LATERAL_RIGHT;
                    case 2 -> megamek.common.enums.MoveStepType.LATERAL_RIGHT_BACKWARDS;
                    case 3 -> megamek.common.enums.MoveStepType.BACKWARDS;
                    case 4 -> megamek.common.enums.MoveStepType.LATERAL_LEFT_BACKWARDS;
                    default -> megamek.common.enums.MoveStepType.LATERAL_LEFT;
                });
                step = coast.getLastStep();
            }
            if (step.getType() == megamek.common.enums.MoveStepType.LOAD) {
                Targetable passenger = step.getTarget(getGame());
                if (passenger instanceof Entity cargo && megamek.common.units.MobileStructureCargoRules.loadableUnits(unit,
                      new MobileStructureLinkage.Pose(unit.getPosition(), unit.getFacing(), unit.getElevation())).contains(cargo)) {
                    Coords source = cargo.getPosition();
                    int absoluteElevation = getGame().getBoard(unit).getHex(source).getLevel() + cargo.getElevation();
                  Bay selectedBay = megamek.common.units.MobileStructureCargoRules.mountingBays(unit, cargo, source,
                          absoluteElevation, new MobileStructureLinkage.Pose(unit.getPosition(), unit.getFacing(), unit.getElevation()))
                          .stream().filter(bay -> cargo.getTargetBay() < 0 || bay.getBayNumber() == cargo.getTargetBay())
                          .findFirst().orElse(null);
                    if (selectedBay == null) { continue; }
                    gameManager.loadUnit(unit, cargo, selectedBay.getBayNumber());
                  Bay bay = unit.getBay(cargo);
                    if (bay != null) {
                        cargo.mpUsed += (cargo.getWalkMP() + 1) / 2;
                        cargo.moved = megamek.common.units.EntityMovementType.MOVE_WALK;
                        cargo.delta_distance = 1;
                        cargo.setDone(true);
                        if (!(cargo instanceof megamek.common.units.Infantry) && megamek.common.compute.Compute.d6(2) == 2) {
                            megamek.common.units.BuildingBayDoors.damage(unit, bay,
                                  megamek.common.units.MobileStructureCargoRules.mountingDoor(unit, bay, cargo, source, absoluteElevation));
                        }
                        gameManager.entityUpdate(cargo.getId());
                        gameManager.entityUpdate(unit.getId());
                    }
                }
                continue;
            }
            if (step.getType() == megamek.common.enums.MoveStepType.UNLOAD) {
                Targetable passenger = step.getTarget(getGame());
                if (passenger instanceof Entity cargo && unit.getLoadedUnits().contains(cargo)
                      && gameManager.unloadUnit(unit, cargo, step.getTargetPosition(), step.getFacing(), step.getElevation())) {
                    Report report = new Report(2514);
                    report.subject = cargo.getId();
                    report.add(unit.getDisplayName());
                    report.add(cargo.generalName());
                    report.add(cargo.getPosition().toFriendlyString());
                    addReport(report);
                }
                continue;
            }
            if (step.getType() == megamek.common.enums.MoveStepType.LAUNCH) {
                new MobileStructureBayLaunchHandler(gameManager).launch(unit, step);
                continue;
            }
            if (!MobileStructureMovement.isMovementStep(step.getType())
                  || (step.getType() == megamek.common.enums.MoveStepType.HOVER && !megamek.common.moves.MobileStructureAirMovement.isAirborne(unit))
                  || (unit.isImmobile() && !unit.isWaterStructure())
                  || unit.isDestroyed() || unit.isDoomed()) {
                break;
            }
            Coords from = unit.getPosition();
            int facing = unit.getFacing();
            int available = budget - unit.mpUsed;
            int cost = MobileStructureMovement.cost(getGame(), unit, from, facing, unit.getElevation(),
                  step.getPosition(), step.getFacing(), step.getElevation());
            if (cost == MobileStructureMovement.PROHIBITED || available <= 0
                  || (facing != step.getFacing() && unit.mpUsed > 0)) {
                break;
            }
            Map<MobileStructure, MobileStructureLinkage.Pose> destinations = new LinkedHashMap<>();
            for (MobileStructure module : modules) {
                destinations.put(module, MobileStructureLinkage.pose(unit, module, step.getPosition(),
                      step.getFacing(), step.getElevation()));
            }
            boolean complete = true;
            for (Map.Entry<MobileStructure, MobileStructureLinkage.Pose> entry : destinations.entrySet()) {
                MobileStructureLinkage.Pose pose = entry.getValue();
                complete &= entry.getKey().advanceMovement(pose.position(), pose.facing(), pose.elevation(), cost, available);
            }
            if (!complete) {
                Report report = new Report(9891);
                report.subject = unit.getId();
                report.addDesc(unit);
                report.add(Double.toString(unit.getMovementProgress().quarters() / 4.0));
                report.add(Double.toString(cost / 4.0));
                addReport(report);
                break;
            }
            // Check the whole assembly before relocating its first member. Collisions still inflict their normal damage.
            boolean clear = true;
            for (Map.Entry<MobileStructure, MobileStructureLinkage.Pose> entry : destinations.entrySet()) {
                MoveStep memberStep = MobileStructureLinkage.step(entry.getKey(), step, entry.getValue());
                clear &= clearBuildings(entry.getKey(), memberStep);
            }
            if (!clear) {
                break;
            }
            for (Map.Entry<MobileStructure, MobileStructureLinkage.Pose> entry : destinations.entrySet()) {
                MoveStep memberStep = MobileStructureLinkage.step(entry.getKey(), step, entry.getValue());
                if (!new MobileStructureCollisionHandler(gameManager).resolve(entry.getKey(), memberStep)) {
                    clear = false;
                    break;
                }
            }
            if (!clear || modules.stream().anyMatch(m -> m.isDestroyed() || m.isDoomed())
                  || !MobileStructureLinkage.group(unit).equals(modules)) {
                break;
            }
            for (Map.Entry<MobileStructure, MobileStructureLinkage.Pose> entry : destinations.entrySet()) {
                MobileStructure module = entry.getKey();
                MobileStructureLinkage.Pose pose = entry.getValue();
                if (pose.elevation() != module.getElevation()) {
                    module.recordDepthChange();
                }
                if (pose.facing() == module.getFacing() && !module.getPosition().equals(pose.position())) {
                    module.setTravelDirection(module.getPosition().direction(pose.position()));
                }
                module.delta_distance += module.getPosition().distance(pose.position());
            }
            relocateGroup(destinations);
            modules.forEach(m -> new MobileStructureCollisionHandler(gameManager).ground(m));
        }
        finishMovement(unit, modules);
    }

    private void finishMovement(MobileStructure unit, List<MobileStructure> modules) {
        for (MobileStructure module : modules) {
            module.moved = module.mpUsed == 0 ? EntityMovementType.MOVE_NONE : EntityMovementType.MOVE_RUN;
            if (module != unit && !module.isDone()) {
                getGame().removeTurnFor(module);
            }
            module.setDone(true);
            gameManager.entityUpdate(module.getId());
        }
        if (modules.size() > 1) {
            gameManager.send(gameManager.getPacketHelper().createTurnListPacket());
        }
    }

    /** TO:AUE p.36: a prohibited building receives the attempted entry's damage, but is not entered that turn. */
    boolean clearBuildings(MobileStructure unit, MoveStep step) {
        boolean landing = step.getType() == megamek.common.enums.MoveStepType.VERTICAL_LAND;
        if (!landing && unit.getMovementMode() != EntityMovementMode.TRACKED && step.getElevation() > 0) {
            return true;
        }
        Set<Coords> entered = MobileStructureMovement.enteredHexes(unit, unit.getPosition(), unit.getFacing(),
              step.getPosition(), step.getFacing());
        if (landing || step.getElevation() != unit.getElevation()) {
            entered.addAll(unit.computeBuildingCoordsForPositionAndFacing(step.getPosition(), step.getFacing()));
        }
                Map<Coords, MobileStructureMovement.HullInterval> hull = MobileStructureMovement.sweptHull(getGame(), unit, unit.getPosition(), unit.getFacing(),
              unit.getElevation(), step.getPosition(), step.getFacing(), step.getElevation(), entered);
        boolean blocked = false;
        for (Coords coords : entered) {
            for (IBuilding obstacle : obstructions(unit, coords, step, hull)) {
                if (obstacle == null || obstacle == unit || obstacle instanceof MobileStructure) {
                    continue;
                }

                if (obstacle.getBldgClass() == IBuilding.CASTLE_BRIAN || (obstacle.getBldgClass() == IBuilding.FORTRESS
                      && obstacle.getBuildingType() == BuildingType.HARDENED)) {
                    int levels = obstacle.usesExpandedCF() ? obstacle.getHeight(coords) : 1;
                    for (int level = 0; level < levels; level++) {
                        addReport(gameManager.damageBuilding(obstacle, 150, "mobile structure attempted entry",
                              coords, level, unit, false));
                    }
                    blocked = true;
                }
            }
        }
        if (blocked) {
            gameManager.applyBuildingDamage();
            return false;
        }
                for (WallRules.Segment side : MobileStructureMovement.wallContacts(getGame(), unit, unit.getPosition(), unit.getFacing(),
              unit.getElevation(), step.getPosition(), step.getFacing(), step.getElevation(), entered)) {
            addReport(gameManager.damageWall(side, side.armor() + side.cf(), false));
        }
        for (Coords coords : entered) {
            for (IBuilding obstacle : obstructions(unit, coords, step, hull)) {
                if (obstacle == null || obstacle == unit || obstacle instanceof MobileStructure) {
                    continue;
                }

                Map<CubeCoords, Coords> next = unit.computeLayoutForPositionAndFacing(step.getPosition(), step.getFacing());
                CubeCoords entering = next.entrySet().stream().filter(e -> e.getValue().equals(coords))
                      .map(Map.Entry::getKey).findFirst().orElseGet(() ->
                            megamek.common.moves.MobileStructureGeometry.contacts(unit, unit.getPosition(), unit.getFacing(),
                                        step.getPosition(), step.getFacing()).stream()
                                  .filter(contact -> contact.boardHex().equals(coords))
                                  .map(megamek.common.moves.MobileStructureGeometry.Contact::hex).findFirst().orElseThrow());
                int damage = (int) Math.ceil(obstacle.getPhaseCF(coords) * obstacle.getHeight(coords) / 10.0);
                addReport(gameManager.damageBuilding(unit, damage, "demolishing a building", unit.relativeToBoard(entering),
                      0, null, false));
                if (obstacle.getArmor(coords) > 0) {
                    for (int group = 0; group < 10; group++) {
                        addReport(gameManager.damageBuilding(obstacle, 10, "mobile structure collision", coords,
                              0, unit, false));
                    }
                    gameManager.applyBuildingDamage();
                    if (obstacle.hasCFIn(coords)) {
                        return false;
                    }
                    continue;
                }
                new BuildingCollapseHandler(gameManager).collapseBuilding(obstacle, getGame().getPositionMapMulti(), coords,
                      gameManager.getMainPhaseReport());
            }
        }
        return !unit.isDestroyed() && !unit.isDoomed();
    }

    private List<IBuilding> obstructions(MobileStructure unit, Coords coords, MoveStep step,
          Map<Coords, MobileStructureMovement.HullInterval> hull) {
          List<Coords> next = MobileStructureLinkage.footprint(unit, step.getPosition(), step.getFacing(), step.getElevation());
          MobileStructureMovement.HullInterval volume = hull.get(coords);
          Hex hex = getGame().getBoard(unit.getBoardId()).getHex(coords);
        if (volume == null || hex == null) { return List.of(); }
        return getGame().getBoard(unit.getBoardId()).getBuildingsAt(coords).stream()
              .filter(building -> building != unit && !(building instanceof MobileStructure))
              .filter(building -> building.getBldgClass() != IBuilding.WALL && building.getBldgClass() != IBuilding.FENCE)
              .filter(building -> volume.intersects(hex.getLevel() + megamek.common.units.BuildingElevation.base(building, coords),
                    hex.getLevel() + megamek.common.units.BuildingElevation.roof(building, coords)))
              .filter(building -> !megamek.common.units.MobileStructurePortalRules.canEnter(unit, building, next, step.getFacing()))
              .toList();
    }

    void relocate(MobileStructure unit, Coords position, int facing, int elevation) {
        Map<MobileStructure, MobileStructureLinkage.Pose> destinations = new LinkedHashMap<>();
        for (MobileStructure module : MobileStructureLinkage.group(unit)) {
            destinations.put(module, MobileStructureLinkage.pose(unit, module, position, facing, elevation));
        }
        relocateGroup(destinations);
    }

    private record CarriedState(MobileStructure unit, List<Coords> oldFootprint, int rotation,
          Map<Entity, CubeCoords> riders, Map<Entity, Integer> riderFloors,
          Map<CubeCoords, List<IndustrialElevator>> elevators, Map<CubeCoords, Integer> elevatorBases) { }

    private CarriedState captureCarriedState(MobileStructure unit, int facing) {
        Board board = getGame().getBoard(unit.getBoardId());
        List<Coords> oldFootprint = List.copyOf(unit.getCoordsList());
        int rotation = Math.floorMod(facing - unit.getFacing(), 6);
        Map<Entity, CubeCoords> riders = new HashMap<>();
        Map<Entity, Integer> riderFloors = new HashMap<>();
        Map<CubeCoords, List<IndustrialElevator>> elevators = new HashMap<>();
        Map<CubeCoords, Integer> elevatorBases = new HashMap<>();
        for (Coords coords : oldFootprint) {
            for (Entity rider : getGame().getEntitiesVector(coords, unit.getBoardId())) {
                if (rider != unit && !(rider instanceof IBuilding) && !rider.isAirborne()
                      && !rider.isAirborneVTOLorWIGE()
                      && rider.getElevation() >= unit.getBaseElevation(coords)
                      && rider.getElevation() <= unit.getBaseElevation(coords) + unit.getHeight(coords)) {
                    riders.put(rider, unit.boardToRelative(coords));
                    riderFloors.put(rider, rider.getElevation() - unit.getBaseElevation(coords));
                }
            }
            BoardLocation location = BoardLocation.of(coords, unit.getBoardId());
            List<IndustrialElevator> shafts = getGame().getIndustrialElevators().stream()
                  .filter(e -> e.getLocation().equals(location) && e.getBuildingId() == unit.getId()).toList();
            if (!shafts.isEmpty()) {
                elevators.put(unit.boardToRelative(coords), shafts);
                elevatorBases.put(unit.boardToRelative(coords), unit.getBaseElevation(coords));
                shafts.forEach(getGame()::removeIndustrialElevator);
                if (board.getHex(coords) != null) {
                    board.getHex(coords).removeTerrain(Terrains.INDUSTRIAL_ELEVATOR);
                }
            }
        }
        return new CarriedState(unit, oldFootprint, rotation, riders, riderFloors, elevators, elevatorBases);
    }

    /** Capture every rider first and remove all old terrain before publishing any member's destination. */
    void relocateGroup(Map<MobileStructure, MobileStructureLinkage.Pose> destinations) {
          List<CarriedState> states = destinations.entrySet().stream()
              .map(e -> captureCarriedState(e.getKey(), e.getValue().facing())).toList();
          Set<Coords> oldFootprint = states.stream().flatMap(state -> state.oldFootprint().stream())
              .collect(java.util.stream.Collectors.toSet());
        destinations.forEach((unit, pose) -> megamek.common.units.MobileStructurePortalRules.recordMovement(unit,
              MobileStructureLinkage.footprint(unit, pose.position(), pose.facing(), pose.elevation()), pose.facing()));
        gameManager.sendRemovedBuildings(new Vector<IBuilding>(destinations.keySet()));
        destinations.keySet().forEach(unit -> getGame().getBoard(unit.getBoardId()).removeBuilding(unit));
        destinations.forEach((unit, pose) -> {
            unit.setFacing(pose.facing());
            unit.setPosition(pose.position());
            unit.setElevation(pose.elevation());
        });
        for (CarriedState state : states) {
            MobileStructure unit = state.unit();
            Board board = getGame().getBoard(unit.getBoardId());
            state.riders().forEach((rider, relative) -> {
                rider.setPosition(unit.relativeToBoard(relative));
                rider.setElevation(unit.getBaseElevation(rider.getPosition()) + state.riderFloors().get(rider));
                rider.setFacing(Math.floorMod(rider.getFacing() + state.rotation(), 6));
                gameManager.entityUpdate(rider.getId());
            });
            state.elevators().forEach((relative, shafts) -> shafts.forEach(shaft -> getGame().addIndustrialElevator(
                  shaft.relocated(BoardLocation.of(unit.relativeToBoard(relative), unit.getBoardId()),
                        unit.getBaseElevation(unit.relativeToBoard(relative)) - state.elevatorBases().get(relative), state.rotation()))));
            for (Coords coords : unit.getCoordsList()) {
                Hex hex = board.getHex(coords);
                if (hex != null && !oldFootprint.contains(coords) && (unit.getMovementMode() == EntityMovementMode.TRACKED
                      || (unit.getMovementMode() == EntityMovementMode.VTOL && unit.getElevation() == 0))) {
                    for (int terrain : new int[] { Terrains.WOODS, Terrains.JUNGLE, Terrains.FOLIAGE_ELEV,
                                                  Terrains.ROAD, Terrains.PAVEMENT, Terrains.FIELDS }) {
                        hex.removeTerrain(terrain);
                    }
                    if (!hex.containsTerrain(Terrains.RUBBLE)) {
                        hex.addTerrain(new Terrain(Terrains.ROUGH, 1));
                    }
                }
            }
            unit.updateBuildingEntityHexes(unit.getBoardId(), gameManager);
            state.oldFootprint().stream().filter(board::contains).forEach(c -> gameManager.sendChangedHex(c, unit.getBoardId()));
        }
    }
}

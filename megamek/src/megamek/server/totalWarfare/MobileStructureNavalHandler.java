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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import megamek.common.Report;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.compute.Compute;
import megamek.common.enums.MoveStepType;
import megamek.common.moves.MovePath;
import megamek.common.moves.MobileStructureLinkage;
import megamek.common.options.OptionsConstants;
import megamek.common.units.BuildingElevation;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import megamek.common.units.MobileStructure;
import megamek.common.units.MobileStructureNavalRules;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.EntityMovementType;
import megamek.common.units.Tank;
import megamek.common.rolls.PilotingRollData;
import megamek.common.HitData;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.units.ProtoMek;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;

/** Delayed sinking and roof-vessel release (TO:AUE pp.27-29 and 40-41). */
final class MobileStructureNavalHandler extends AbstractTWRuleHandler {
    MobileStructureNavalHandler(TWGameManager manager) { super(manager); }

    boolean mustSink(MobileStructure mobile) {
        return mobile.getPosition() != null && !mobile.isDestroyed() && !mobile.getNavalState().isSettled()
              && MobileStructureLinkage.group(mobile).stream().anyMatch(member -> member.getCoordsList().stream().anyMatch(coords -> {
                  var hex = getGame().getBoard(member).getHex(coords);
                  int motives = member.getMovementMode() == EntityMovementMode.TRACKED ? 2 : 0;
                  return hex != null && hex.containsTerrain(Terrains.WATER) && hex.depth() > 0
                        && member.getBaseElevation(coords) - motives < 0;
              }));
    }

    Vector<Report> beginSinking(MobileStructure mobile, String reason) {
        Vector<Report> reports = new Vector<>();
        if (mobile.getNavalState().isSinking()) { return reports; }
        var members = MobileStructureLinkage.group(mobile);
        int rate = sinkingRate(mobile);
        // Intact linkages make one unit. Mark every member before removing any bottomed linkage hex.
        for (var member : members) {
            member.getCoordsList().forEach(coords -> member.getNavalState().getBaseOffsets()
                  .put(member.boardToRelative(coords), member.getBaseElevation(coords) - member.getElevation()));
            member.getNavalState().beginSinking(getGame().getRoundCount(), rate, getGame().getPhase().isMovement());
            member.setCarcass(true);
            member.setPowerSwitchedOff(true);
            member.cancelMovementProgress();
            member.setDone(true);
            getGame().getIndustrialElevators().stream().filter(shaft -> shaft.getBuildingId() == member.getId())
                  .forEach(shaft -> shaft.setFunctional(false));
            var report = new Report(9897, Report.PUBLIC);
            report.subject = member.getId();
            report.addDesc(member);
            report.add(reason);
            report.add(rate);
            reports.add(report);
        }
        for (var member : members) {
            removeBottomedHexes(member, reports);
            gameManager.entityUpdate(member.getId());
        }
        gameManager.sendIndustrialElevatorUpdate();
        return reports;
    }

    /** Mobile footprints lack a naval template: use the equivalent longest 1/3/5/7/9-hex template (AUE pp.27,57). */
    static int sinkingRate(MobileStructure mobile) {
        var hexes = MobileStructureLinkage.group(mobile).stream().flatMap(member -> member.getCoordsList().stream()).toList();
        int length = hexes.stream().mapToInt(a -> hexes.stream().mapToInt(a::distance)
              .max().orElse(0)).max().orElse(0) + 1;
        return Math.max(1, 5 - length / 2);
    }

    void endMovement() {
        var mobiles = getGame().getEntitiesVector().stream().filter(MobileStructure.class::isInstance)
              .map(MobileStructure.class::cast).toList();
        var due = mobiles.stream().filter(mobile -> mobile.getNavalState().beginSinkingTurn(getGame().getRoundCount())).toList();
        int rate = due.stream().mapToInt(mobile -> mobile.getNavalState().getDepthsPerTurn()).max().orElse(0);
        Set<MobileStructure> stopped = new HashSet<>();
        for (int depth = 0; depth < rate; depth++) {
            int increment = depth;
            var descending = due.stream().filter(mobile -> mobile.getNavalState().isSinking()
                  && !stopped.contains(mobile) && increment < mobile.getNavalState().getDepthsPerTurn())
                  .sorted(java.util.Comparator.comparingInt(this::lowestAltitude)).toList();
            Set<Integer> processed = new HashSet<>();
            Set<Integer> approved = new HashSet<>();
            var moving = new ArrayList<MobileStructure>();
            // Lower hulls resolve first. An upper hull cannot hit the old position of a lower hull descending with it.
            for (var mobile : descending) {
                if (!processed.add(mobile.getId())) { continue; }
                var members = MobileStructureLinkage.group(mobile).stream().filter(descending::contains).toList();
                members.forEach(member -> processed.add(member.getId()));
                boolean blocked = false;
                for (var member : members) {
                    var step = new MovePath(getGame(), member).addStep(MoveStepType.DOWN).getLastStep();
                    step = MobileStructureLinkage.step(member, step, new MobileStructureLinkage.Pose(member.getPosition(),
                          member.getFacing(), member.getElevation() - 1));
                    if (!new MobileStructureMovementHandler(gameManager).clearBuildings(member, step)
                          || !new MobileStructureCollisionHandler(gameManager).resolve(member, step, approved)) {
                        blocked = true;
                        break;
                    }
                }
                if (blocked) { stopped.addAll(members); }
                else {
                    moving.addAll(members);
                    members.forEach(member -> approved.add(member.getId()));
                }
            }
            var destinations = new java.util.LinkedHashMap<MobileStructure, MobileStructureLinkage.Pose>();
            moving.stream().filter(member -> member.getNavalState().isSinking()).forEach(member -> destinations.put(member,
                  new MobileStructureLinkage.Pose(member.getPosition(), member.getFacing(), member.getElevation() - 1)));
            new MobileStructureMovementHandler(gameManager).relocateGroup(destinations);
            Vector<Report> reports = new Vector<>();
            for (var member : destinations.keySet()) {
                member.getNavalState().descended(getGame().getRoundCount());
                removeBottomedHexes(member, reports);
            }
            addReport(reports);
        }
        mobiles.forEach(this::releaseSubmergedVessels);
    }

    private int lowestAltitude(MobileStructure mobile) {
        var board = getGame().getBoard(mobile);
        return mobile.getCoordsList().stream().filter(board::contains)
              .mapToInt(coords -> board.getHex(coords).getLevel() + mobile.getBaseElevation(coords)).min().orElse(0);
    }

    /** A stranded ship floats free only at movement end, after the carrier's final depth is known. */
    private void releaseSubmergedVessels(MobileStructure mobile) {
        for (var entry : List.copyOf(mobile.getNavalState().getStrandedVessels().entrySet())) {
            Entity vessel = getGame().getEntity(entry.getKey());
            Coords coords = mobile.relativeToBoard(entry.getValue());
            if (vessel == null) {
                mobile.getNavalState().getStrandedVessels().remove(entry.getKey());
            } else if (coords != null && (!mobile.hasCFIn(coords) || BuildingElevation.roof(mobile, coords) <= -1)) {
                releaseVessel(mobile, vessel, coords);
            }
        }
    }

    /** Returns released IDs so a normal structural-collapse handler does not also damage the freed ships. */
    Set<Integer> releaseVesselsInHex(MobileStructure mobile, Coords coords) {
        Set<Integer> released = new HashSet<>();
        for (var entry : List.copyOf(mobile.getNavalState().getStrandedVessels().entrySet())) {
            if (!coords.equals(mobile.relativeToBoard(entry.getValue()))) { continue; }
            released.add(entry.getKey());
            Entity vessel = getGame().getEntity(entry.getKey());
            if (vessel != null) { releaseVessel(mobile, vessel, coords); }
            else { mobile.getNavalState().getStrandedVessels().remove(entry.getKey()); }
        }
        return released;
    }

    private void releaseVessel(MobileStructure mobile, Entity vessel, Coords coords) {
        mobile.getNavalState().getStrandedVessels().remove(vessel.getId());
        var hex = getGame().getBoard(mobile).getHex(coords);
        vessel.setStuck(false);
        vessel.setDone(true);
        if (hex == null || !hex.containsTerrain(Terrains.WATER)) {
            addReport(gameManager.destroyEntity(vessel, "stranded vessel lost its support on land", false));
        } else {
            vessel.setElevation(0);
        }
        gameManager.entityUpdate(vessel.getId());
        gameManager.entityUpdate(mobile.getId());
    }

    private void removeBottomedHexes(MobileStructure mobile, Vector<Report> reports) {
        var board = getGame().getBoard(mobile);
        var bottomed = mobile.getCoordsList().stream().filter(coords -> board.getHex(coords) != null
              && (!board.getHex(coords).containsTerrain(Terrains.WATER)
                    || BuildingElevation.base(mobile, coords) - (mobile.getMovementMode()
                          == megamek.common.units.EntityMovementMode.TRACKED ? 2 : 0)
                          <= -board.getHex(coords).depth())).toList();
        for (Coords coords : bottomed) {
            int oldRoof = BuildingElevation.roof(mobile, coords);
            int oldBase = BuildingElevation.base(mobile, coords);
            var released = releaseVesselsInHex(mobile, coords);
            getGame().getIndustrialElevators().stream().filter(shaft -> shaft.getBuildingId() == mobile.getId()
                  && shaft.getLocation().coords().equals(coords)).toList().forEach(getGame()::removeIndustrialElevator);
            var riders = getGame().getEntitiesVector(coords, mobile.getBoardId()).stream().filter(unit ->
                  unit != mobile && !(unit instanceof IBuilding) && !unit.isAirborne() && !unit.isAirborneVTOLorWIGE()
                        && !released.contains(unit.getId()) && unit.getElevation() >= oldBase
                        && unit.getElevation() <= oldRoof).toList();
            gameManager.sendRemovedBuildings(new Vector<>(List.of(mobile)));
            board.removeBuilding(mobile);
            mobile.removeHex(coords);
            if (getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_TAC_OPS_BATTLE_WRECK)) {
                board.getHex(coords).addTerrain(new Terrain(Terrains.RUBBLE, 6));
            }
            mobile.updateBuildingEntityHexes(mobile.getBoardId(), gameManager);
            gameManager.sendChangedHex(coords, mobile.getBoardId());
            for (Entity rider : riders) { fallFromLostDeck(rider, rider.getElevation(), coords, reports); }
        }
        if (!bottomed.isEmpty()) { gameManager.sendIndustrialElevatorUpdate(); }
        if (mobile.getCoordsList().stream().noneMatch(board::contains)) {
            mobile.getNavalState().finishSinking();
            board.removeBuilding(mobile);
            reports.addAll(gameManager.destroyEntity(mobile, "sunk", true));
        }
    }

    void fallFromLostDeck(Entity rider, int oldElevation, Coords destination, Vector<Report> reports) {
        var hex = destination == null ? null : getGame().getBoard(rider).getHex(destination);
        if (hex == null) {
            reports.addAll(gameManager.destroyEntity(rider, "no legal water hex beside the sinking structure", false));
        } else if (hex.containsTerrain(Terrains.WATER)) {
            if (rider.hasUMU() && (rider instanceof Mek || rider instanceof Infantry)) {
                if (oldElevation > 0) {
                    reports.addAll(gameManager.doEntityFallsInto(rider, oldElevation, rider.getPosition(), destination,
                          rider.getBasePilotingRoll(), true, 0));
                }
                rider.setPosition(destination);
                rider.setElevation(Math.min(-1, oldElevation));
            } else if (rider.getMovementMode().isNaval() || rider.getMovementMode().isSubmarine()) {
                if (oldElevation > 0) {
                    reports.addAll(gameManager.doEntityFallsInto(rider, oldElevation, rider.getPosition(), destination,
                          rider.getBasePilotingRoll(), true, 0));
                }
                rider.setPosition(destination);
                rider.setElevation(0);
            } else if (rider instanceof Mek || rider instanceof ProtoMek) {
                reports.addAll(gameManager.doEntityFallsInto(rider, oldElevation, rider.getPosition(), destination,
                      rider.getBasePilotingRoll(), true, 0));
            } else {
                reports.addAll(gameManager.destroyEntity(rider, "fell into water from a sinking structure", false));
            }
        } else {
            reports.addAll(gameManager.doEntityFallsInto(rider, oldElevation, rider.getPosition(), destination,
                  rider.getBasePilotingRoll(), true, 0));
        }
        gameManager.entityUpdate(rider.getId());
    }

    /** Failure ends this movement before any further path steps; takeoff fails at source, jumps fail at destination. */
    boolean departDeck(Entity rider, MovePath path) {
        MobileStructure carrier = MobileStructureNavalRules.departureCarrier(rider, path);
        if (carrier == null || MobileStructureNavalRules.canSwimOff(rider, rider.getElevation())) { return true; }
        boolean vtol = rider.getMovementMode().isVTOL()
              || rider.isAero() && path.contains(MoveStepType.VERTICAL_TAKE_OFF);
        if (rider.isAero() && MobileStructureNavalRules.hasFlightDeckAt(rider)) { return true; }
        if (!path.isJumping() && !vtol) {
            rider.setDone(true);
            gameManager.entityUpdate(rider.getId());
            return false;
        }
        Coords source = rider.getPosition();
        Coords destination = vtol ? source : path.getFinalCoords();
        int elevation = vtol ? rider.getElevation() : path.getFinalElevation();
        var reports = new Vector<Report>();
        var roll = rider instanceof Infantry ? new PilotingRollData(rider.getId(), rider.getCrew().getPiloting(),
              "leaving sinking deck") : rider.getBasePilotingRoll();
        if (gameManager.doSkillCheckWhileMoving(rider, elevation, source, destination, roll, false, reports) <= 0) {
            addReport(reports);
            return true;
        }
        rider.setPosition(destination);
        rider.setElevation(elevation);
        if (rider instanceof BattleArmor armor) {
            for (int trooper = 1; trooper < armor.locations(); trooper++) {
                if (armor.getInternal(trooper) > 0) { reports.addAll(gameManager.damageEntity(rider, new HitData(trooper), 1)); }
            }
        } else if (rider instanceof Infantry) {
            reports.addAll(gameManager.damageEntity(rider, new HitData(megamek.common.units.ConvInfantry.LOC_INFANTRY), 5));
        } else if (vtol && rider instanceof Tank tank) {
            reports.addAll(gameManager.crashVTOLorWiGE(tank, false, false, 1, source, elevation, Compute.d6() % 4));
        } else if (vtol && rider.isAero()) {
            reports.addAll(gameManager.processCrash(rider, 1, source));
        } else {
            reports.addAll(gameManager.doEntityFallsInto(rider, elevation, destination, destination, roll, true, 0));
        }
        if (rider.isLocationProhibited(destination, rider.getBoardId(), rider.getElevation())) {
            reports.addAll(gameManager.destroyEntity(rider, "illegal destination after leaving sinking deck", false));
        }
        rider.setDone(true);
        rider.moved = path.isJumping() ? EntityMovementType.MOVE_JUMP : EntityMovementType.MOVE_VTOL_WALK;
        rider.mpUsed = path.getMpUsed();
        rider.delta_distance = path.getHexesMoved();
        gameManager.entityUpdate(rider.getId());
        addReport(reports);
        return false;
    }

    /** Standing Meks check footing at turn end after the Movement Phase's descent (AUE p.28). */
    void endTurn() {
        for (Entity entity : List.copyOf(getGame().getEntitiesVector())) {
            if (!(entity instanceof MobileStructure mobile)
                  || !mobile.getNavalState().checkFootingThisRound(getGame().getRoundCount())) { continue; }
            for (Coords coords : List.copyOf(mobile.getCoordsList())) {
                int roof = BuildingElevation.roof(mobile, coords);
                for (Entity rider : List.copyOf(getGame().getEntitiesVector(coords, mobile.getBoardId()))) {
                    if (!(rider instanceof Mek) || rider.isProne() || rider.isAirborne() || rider.getElevation() != roof) { continue; }
                    var reports = new Vector<Report>();
                    var psr = rider.getBasePilotingRoll();
                    if (gameManager.doSkillCheckWhileMoving(rider, roof, coords, coords, psr, false, reports) > 0) {
                        if (roof >= 0) {
                            reports.addAll(gameManager.doEntityFall(rider, coords, 0, Compute.d6(), psr, false, false));
                            rider.setElevation(roof);
                        } else {
                            List<Coords> exits = new ArrayList<>(coords.allAdjacent());
                            var board = getGame().getBoard(mobile);
                            var footprint = MobileStructureLinkage.group(mobile).stream()
                                  .flatMap(member -> member.getCoordsList().stream()).toList();
                            exits.removeIf(hex -> !board.contains(hex) || footprint.contains(hex)
                                  || board.getHex(hex).containsTerrain(Terrains.IMPASSABLE)
                                  || board.getHex(hex).getLevel() - board.getHex(coords).getLevel()
                                        - board.getHex(hex).depth() > roof
                                  || board.getBuildingsAt(hex).stream().anyMatch(building ->
                                        BuildingElevation.contains(building, hex, roof)));
                            Coords destination = exits.isEmpty() ? null : exits.get(Compute.randomInt(exits.size()));
                            fallFromLostDeck(rider, roof, destination, reports);
                        }
                    }
                    addReport(reports);
                }
            }
        }
    }
}

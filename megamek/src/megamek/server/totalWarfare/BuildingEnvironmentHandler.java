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

import java.util.List;
import java.util.Vector;

import megamek.common.Report;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.compute.Compute;
import megamek.common.units.*;

/** Environmental breaches and progressive flooding, TO:AR pp.134,138 and the v7 breach erratum. */
final class BuildingEnvironmentHandler extends AbstractTWRuleHandler {
    BuildingEnvironmentHandler(TWGameManager manager) {
        super(manager);
    }

    /** Resolve the breach attempt before movement, so failure cannot put the unit through an intact sealed wall. */
    boolean canCross(Entity unit, Coords from, int fromElevation, Coords to, int toElevation, Vector<Report> reports) {
        if (from == null || to == null || from.equals(to)) {
            return true;
        }
        var board = getGame().getBoard(unit.getBoardId());
        var crossed = new java.util.LinkedHashSet<IBuilding>(board.getBuildingsAt(from));
        crossed.addAll(board.getBuildingsAt(to));
        for (IBuilding candidate : crossed) {
            if (!(candidate instanceof AbstractBuildingEntity building) || !building.hasEnvironmentalSealing()
                  || BuildingConstruction.usesHexsides(building)) {
                continue;
            }
            boolean leaving = BuildingElevation.contains(building, from, fromElevation);
            boolean entering = BuildingElevation.contains(building, to, toElevation);
            if (leaving == entering) {
                continue;
            }
            int elevation = entering ? toElevation : fromElevation;
            Coords wallHex = entering ? to : from;
            if (building.getBuildingRuntimeState().openPassage(building, unit, from, to, elevation)) {
                continue;
            }
            if (building.getArmor(wallHex, BuildingElevation.floor(building, wallHex, elevation)) > 0
                  || !attemptBreach(building, wallHex, BuildingElevation.floor(building, wallHex, elevation), reports)) {
                return false;
            }
        }
        return true;
    }

    /** Special open-space timing is independent of the optional expanded-CF and collapse rules. */
    void collapsed(AbstractBuildingEntity building, Coords coords, int height, Vector<Report> reports) {
        var state = building.getBuildingRuntimeState();
        state.rememberCollapsedHex(building.boardToRelative(coords), height);
        if (!building.getDesign().isOpenSpace()) {
            return;
        }
        for (CubeCoords hex : building.getInternalBuilding().getOriginalCoordsList()) {
            state.breach(hex);
        }
        if (building.getDesign().getSite() == BuildingDesign.Site.SURFACE) {
            state.loseOpenSpaceAtmosphere(getGame().getRoundCount());
        } else if (building.getDesign().getSite() == BuildingDesign.Site.UNDERWATER) {
            state.startOpenSpaceFlood(getGame().getRoundCount(), -BuildingElevation.roof(building, coords));
        } else if (building.getDesign().getSite() == BuildingDesign.Site.UNDERGROUND) {
            var hex = getGame().getHex(coords, building.getBoardId());
            if (hex != null && hex.depth() > 0) {
                int cover = BuildingElevation.groundCover(getGame(), building, coords);
                int target = 10 + (cover > 6 ? 2 : cover < 3 ? -4 : cover <= 4 ? -2 : 0);
                if (Compute.d6(2) >= target) {
                    state.startOpenSpaceFlood(getGame().getRoundCount(), -BuildingElevation.roof(building, coords));
                }
            }
        }
    }

    void damage(IBuilding target, Coords coords, int level, int cfLost, Vector<Report> reports) {
        if (!(target instanceof AbstractBuildingEntity building) || cfLost <= 0) {
            return;
        }
        var design = building.getDesign();
        var state = building.getBuildingRuntimeState();
        boolean subsurface = design.getSite() != BuildingDesign.Site.SURFACE && !building.usesCapitalScale();
        boolean phaseThreshold = subsurface && state.crossesSubsurfaceDamageThreshold(building.boardToRelative(coords),
              cfLost, getGame().getRoundCount(), getGame().getPhase());
        // The 2023 v7 erratum changes "in excess of 10" to "10 or more" CF points per hit.
        if ((building.hasEnvironmentalSealing() && cfLost >= 10) || phaseThreshold) {
            attemptBreach(building, coords, level, reports);
        }
    }

    boolean attemptBreach(AbstractBuildingEntity building, Coords coords, int level, Vector<Report> reports) {
        CubeCoords hex = building.boardToRelative(coords);
        if (building.getBuildingRuntimeState().hasStructuralBreach(hex)) {
            return true;
        }
        int modifier = switch (building.getBuildingType()) {
            case LIGHT -> 2;
            case HEAVY -> -2;
            case HARDENED -> -4;
            default -> 0;
        };
        if (building.getDesign().getSite() != BuildingDesign.Site.SURFACE) {
            modifier += (Math.max(0, -BuildingElevation.roof(building, coords)) + 1) / 2;
        }
        int roll = Compute.d6(2);
        Report report = new Report(9892, Report.PUBLIC);
        report.add(building.getDisplayName());
        report.add(coords.getBoardNum());
        report.add(roll);
        report.add(modifier);
        report.choose(roll + modifier >= 10);
        reports.add(report);
        if (roll + modifier < 10) {
            return false;
        }
        building.getBuildingRuntimeState().markStructuralBreach(hex);
        breach(building, coords, level, false, reports);
        return true;
    }

    private void breach(AbstractBuildingEntity building, Coords coords, int level, boolean fromTunnel,
          Vector<Report> reports) {
        var state = building.getBuildingRuntimeState();
        CubeCoords hitHex = building.boardToRelative(coords);
        boolean changed = state.breach(hitHex);
        List<CubeCoords> affected = building.usesCapitalScale() ? List.of(hitHex)
              : List.copyOf(building.getInternalBuilding().getCoordsList());
        for (CubeCoords hex : affected) {
            changed |= state.breach(hex);
            if (building.getDesign().getSite() == BuildingDesign.Site.UNDERWATER || fromTunnel) {
                if (building.getDesign().isOpenSpace() && building.getCurrentCF(coords) <= 0) {
                    state.startOpenSpaceFlood(getGame().getRoundCount(), -BuildingElevation.roof(building, coords));
                    continue;
                }
                boolean local = building.usesExpandedCF() && building.getBldgClass() != IBuilding.HANGAR
                      && !building.getDesign().isTunnel() && !building.getDesign().isOpenSpace();
                int bottom = local ? (fromTunnel ? 0 : level) : 0;
                int top = local ? bottom : building.getInternalBuilding().getHeight(hex) - 1;
                for (int floor = bottom; floor <= top; floor++) {
                    changed |= state.flood(hex, floor, fromTunnel ? 1 : -1, getGame().getRoundCount());
                }
            }
        }
        if (!changed) {
            return;
        }
        if (building.getDesign().getSite() == BuildingDesign.Site.UNDERGROUND) {
            building.setCurrentCF(0, coords, level);
        }
        applyExposure(building, reports);
        spreadThroughOpenDoors(building, reports);
        gameManager.entityUpdate(building.getId());
    }

    void doorChanged(AbstractBuildingEntity building, BuildingDesign.Door door, Vector<Report> reports) {
        int doorHeight = BuildingElevation.doorwayHeight(building, door);
        if (!building.getBuildingRuntimeState().isDoorOpen(door) || doorHeight == 0) {
            return;
        }
        Coords inner = building.relativeToBoard(door.position().hex());
        Coords outer = inner.translated((door.facing() + building.getFacing()) % 6);
        int bottom = doorAltitude(building, door);
        boolean outside = !getGame().hasBoardLocation(outer, building.getBoardId())
              || getGame().getBoard(building).getBuildingsAt(outer).stream().noneMatch(other ->
                    BuildingElevation.altitude(getGame(), other, outer, 0) <= bottom
                          && BuildingElevation.altitude(getGame(), other, outer, other.getHeight(outer)) >= bottom + doorHeight);
        var conditions = getGame().getPlanetaryConditions();
        boolean hostile = building.getDesign().getSite() == BuildingDesign.Site.UNDERWATER
              || conditions.getAtmosphere().isLighterThan(megamek.common.planetaryConditions.Atmosphere.THIN)
              || !conditions.getAtmosphericTaint().isBreathable();
        if (outside && hostile) {
            breach(building, inner, BuildingElevation.currentFloor(building, door.position().hex(), door.position().level()), false, reports);
        } else {
            spreadThroughOpenDoors(building, reports);
        }
    }

    private void spreadThroughOpenDoors(AbstractBuildingEntity building, Vector<Report> reports) {
        for (var door : building.getDesign().getDoors()) {
            var state = building.getBuildingRuntimeState();
            if (!state.isDoorOpen(door) || BuildingElevation.doorwayHeight(building, door) == 0
                  || !state.isBreached(door.position().hex())) {
                continue;
            }
            Coords source = building.relativeToBoard(door.position().hex());
            Coords otherHex = source.translated((door.facing() + building.getFacing()) % 6);
            if (!getGame().hasBoardLocation(otherHex, building.getBoardId())) {
                continue;
            }
            for (IBuilding other : getGame().getBoard(building).getBuildingsAt(otherHex)) {
                if (!(other instanceof AbstractBuildingEntity neighbor) || other == building) {
                    continue;
                }
                for (var otherDoor : neighbor.getDesign().getDoors()) {
                    if (neighbor.getBuildingRuntimeState().isDoorOpen(otherDoor)
                          && BuildingElevation.doorwayHeight(neighbor, otherDoor) > 0
                          && neighbor.relativeToBoard(otherDoor.position().hex()).equals(otherHex)
                          && otherHex.translated((otherDoor.facing() + neighbor.getFacing()) % 6).equals(source)
                          && doorwaysOverlap(building, door, neighbor, otherDoor)) {
                        int bottom = Math.max(doorAltitude(building, door), doorAltitude(neighbor, otherDoor));
                        int top = Math.min(doorAltitude(building, door) + BuildingElevation.doorwayHeight(building, door),
                              doorAltitude(neighbor, otherDoor) + BuildingElevation.doorwayHeight(neighbor, otherDoor));
                        for (int altitude = bottom; altitude < top; altitude++) {
                            int sourceFloor = altitude - BuildingElevation.altitude(getGame(), building, source, 0);
                            int targetFloor = altitude - BuildingElevation.altitude(getGame(), neighbor, otherHex, 0);
                            breach(neighbor, otherHex, targetFloor, state.isFlooded(door.position().hex(), sourceFloor), reports);
                        }
                    }
                }
            }
        }
    }

    private boolean doorwaysOverlap(AbstractBuildingEntity building, BuildingDesign.Door door,
          AbstractBuildingEntity neighbor, BuildingDesign.Door otherDoor) {
        int bottom = doorAltitude(building, door);
        int otherBottom = doorAltitude(neighbor, otherDoor);
        return Math.max(bottom, otherBottom) < Math.min(bottom + BuildingElevation.doorwayHeight(building, door),
              otherBottom + BuildingElevation.doorwayHeight(neighbor, otherDoor));
    }

    private int doorAltitude(AbstractBuildingEntity building, BuildingDesign.Door door) {
        return BuildingElevation.altitude(getGame(), building, building.relativeToBoard(door.position().hex()),
              BuildingElevation.currentFloor(building, door.position().hex(), door.position().level()));
    }

    void endPhase(Vector<Report> reports) {
        for (Entity entity : List.copyOf(getGame().getEntitiesVector())) {
            if (entity instanceof AbstractBuildingEntity building) {
                boolean changed = building.getBuildingRuntimeState().advanceFloods(building, getGame().getRoundCount());
                changed |= building.getBuildingRuntimeState().advanceOpenSpaceFlood(building, getGame().getRoundCount());
                changed |= building.getBuildingRuntimeState().advanceAtmosphere(getGame().getRoundCount());
                applyExposure(building, reports);
                spreadThroughOpenDoors(building, reports);
                if (changed) {
                    gameManager.entityUpdate(building.getId());
                }
            }
        }
    }

    private void applyExposure(AbstractBuildingEntity building, Vector<Report> reports) {
        var state = building.getBuildingRuntimeState();
        boolean hostileAir = getGame().getPlanetaryConditions().getAtmosphere()
              .isLighterThan(megamek.common.planetaryConditions.Atmosphere.THIN)
              || !getGame().getPlanetaryConditions().getAtmosphericTaint().isBreathable();
        boolean elevatorChanged = false;
        for (CubeCoords hex : building.getInternalBuilding().getOriginalCoordsList()) {
            Coords coords = building.relativeToBoard(hex);
            if (!state.isBreached(hex)) {
                continue;
            }
            for (var elevator : getGame().getIndustrialElevators()) {
                if (elevator.isFunctional() && elevator.getLocation().coords().equals(coords)
                      && elevator.getLocation().boardId() == building.getBoardId()
                      && elevator.getBuildingId() == building.getId()) {
                    elevator.setFunctional(false);
                    elevatorChanged = true;
                }
            }
            for (var mount : building.getEquipmentInHex(hex)) {
                var interior = BuildingConstruction.equipmentPositions(building, mount).stream()
                      .map(position -> new BuildingDesign.Position(position.hex(),
                            BuildingElevation.currentFloor(building, position.hex(), position.level())))
                      .filter(position -> position.level() >= 0)
                      .toList();
                boolean submerged = interior.stream()
                      .anyMatch(position -> state.isFlooded(position.hex(), position.level()));
                if (submerged || (hostileAir && !state.atmosphereGrace(getGame().getRoundCount()) && !interior.isEmpty())) {
                    mount.setBreached(true);
                }
            }
            boolean fatalAir = hostileAir && !state.atmosphereGrace(getGame().getRoundCount());
            if (building.usesExpandedCF()) {
                for (int location : building.getLocationsAt(coords)) {
                    if (fatalAir || state.isFlooded(hex, building.getLocationLevel(location))) {
                        building.setGunnersKilledAtLocation(location, true);
                    }
                }
            } else if (fatalAir || state.isFlooded(hex, 0)) {
                building.killGunnersAt(coords);
            }
            for (Entity occupant : List.copyOf(getGame().getEntitiesVector(coords, building.getBoardId()))) {
                if (occupant instanceof IBuilding || occupant.isDoomed() || occupant.isDestroyed()
                      || !state.encloses(building, occupant)) {
                    continue;
                }
                if (occupant instanceof megamek.common.units.ConvInfantry infantry && infantry.isSurvivalGearStored()) {
                    state.setSurvivalGearInactive(occupant.getId(), true);
                }
                if (state.atmosphereExposureDue(getGame().getRoundCount()) && state.needsSurvivalGearRoll(occupant.getId())) {
                    state.resolveSurvivalGear(occupant.getId(), Compute.d6(2) >= 8);
                    if (occupant instanceof megamek.common.units.ConvInfantry infantry) {
                        infantry.setSurvivalGearStored(false);
                        gameManager.entityUpdate(infantry.getId());
                    }
                }
                int floor = BuildingElevation.floor(building, coords, occupant.getElevation());
                boolean flooded = state.isFlooded(hex, floor);
                boolean unprotected = flooded ? !EnvironmentalSealingRules.canOperateFullySubmerged(occupant)
                      && !BuildingRuntimeState.protectsFromEnvironment(getGame(), occupant)
                      : fatalAir && (state.survivalGearFailed(occupant.getId())
                            || getGame().getPlanetaryConditions().whyDoomed(occupant, getGame()) != null);
                if (unprotected) {
                    reports.addAll(gameManager.destroyEntity(occupant, flooded ? "flooded building" : "breached building"));
                    gameManager.entityUpdate(occupant.getId());
                }
            }
        }
        if (elevatorChanged) {
            gameManager.sendIndustrialElevatorUpdate();
        }
    }
}

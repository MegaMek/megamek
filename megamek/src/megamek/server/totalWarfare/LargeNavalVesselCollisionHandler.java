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

import megamek.common.HitData;
import megamek.common.Report;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.equipment.MiscType;
import megamek.common.moves.MobileStructureLinkage;
import megamek.common.moves.MoveStep;
import megamek.common.units.BuildingElevation;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementType;
import megamek.common.units.LargeNavalVesselRules;
import megamek.common.units.MobileStructure;
import megamek.common.units.Tank;
import megamek.common.units.Terrains;

/** The large-vessel exceptions to mobile displacement and surfacing (TO:AUE pp.40-41). */
final class LargeNavalVesselCollisionHandler extends AbstractTWRuleHandler {
    LargeNavalVesselCollisionHandler(TWGameManager manager) { super(manager); }

    /** Returns false when the vessel is displaced or the attempted entry remains blocked. */
    boolean entering(Tank vessel, Coords from, int fromElevation, MoveStep step) {
        var obstacles = LargeNavalVesselRules.obstacles(vessel, from, vessel.getFacing(), fromElevation,
              step.getPosition(), step.getFacing(), step.getElevation());
        boolean surfacing = from.equals(step.getPosition()) && step.getElevation() > fromElevation;
        var handler = new MobileStructureCollisionHandler(gameManager);
        Set<Integer> handled = new HashSet<>();
        for (var mobile : obstacles) {
            if (!handled.add(mobile.getId())) { continue; }
            var modules = MobileStructureLinkage.group(mobile);
            modules.forEach(module -> handled.add(module.getId()));
            var footprint = new HashSet<>(LargeNavalVesselRules.footprint(vessel, step.getPosition(), step.getFacing()));
            boolean submerged = fullyUnderwater(vessel, step.getElevation())
                  && modules.stream().allMatch(MobileStructureCollisionHandler::fullyUnderwater);
            for (var module : modules) {
                for (Coords coords : LargeNavalVesselRules.contactHexes(vessel, module, from, vessel.getFacing(), fromElevation,
                      step.getPosition(), step.getFacing(), step.getElevation())) {
                    int dealt = MobileStructureCollisionHandler.standardChargeDamage(vessel, step.getDistance());
                    int returned = MobileStructureCollisionHandler.standardChargeReturnDamage(vessel, module);
                    if (submerged) { dealt /= 2; returned /= 2; }
                    while (dealt > 0) {
                        int cluster = Math.min(10, dealt);
                        handler.damageHex(module, module.boardToRelative(coords), cluster);
                        dealt -= cluster;
                    }
                    handler.damageUnit(vessel, returned, module);
                }
            }
            vessel.applyDamage();
            gameManager.applyBuildingDamage();
            if (vessel.isDestroyed() || vessel.isDoomed()) { return false; }
            if (mobile.isDestroyed() || mobile.isDoomed()) { continue; }
            int comparison = surfacing ? Integer.compare(footprint.size(), currentHexes(mobile).size()) : 1;
            while (comparison == 0) { comparison = Integer.compare(Compute.d6(2), Compute.d6(2)); }
            // Template A follows the ordinary submarine rule and remains at its previous depth.
            if (surfacing && LargeNavalVesselRules.template(vessel) == 0) { return false; }
            if (comparison > 0) {
                Coords destination = handler.displacement(mobile, footprint);
                if (destination == null) { return false; }
                new MobileStructureMovementHandler(gameManager).relocate(mobile, destination,
                      mobile.getFacing(), mobile.getElevation());
                handler.ground(mobile);
            } else {
                Coords destination = displacement(vessel, new HashSet<>(currentHexes(mobile)));
                if (destination != null) { relocate(vessel, destination); }
                return false;
            }
        }
        gameManager.entityUpdate(vessel.getId());
        return true;
    }

    /** The target was on the surface before the mobile rose into it; submerged targets do not capsize. */
    boolean afterMobileSurfacing(Tank vessel, MobileStructure mobile, MoveStep step) {
        Vector<Report> reports = new Vector<>();
        var roll = vessel.getBasePilotingRoll(EntityMovementType.MOVE_WALK);
        roll.addModifier(4, "mobile structure surfacing beneath vessel");
        if (gameManager.doSkillCheckWhileMoving(vessel, vessel.getElevation(), vessel.getPosition(),
              vessel.getPosition(), roll, false, reports) > 0) {
            reports.addAll(gameManager.damageCrew(vessel, 1));
            boolean submersible = vessel.hasMisc(MiscType.F_SUBMERSIBLE);
            for (int check = 0; check < (submersible ? 1 : 2); check++) {
                reports.addAll(gameManager.vehicleMotiveDamage(vessel, 0));
            }
            for (int check = 0; check < (submersible ? 1 : 3); check++) {
                reports.addAll(gameManager.criticalEntity(vessel, Tank.LOC_FRONT, false, 0, false, false, 0));
            }
        }
        addReport(reports);
        return displaceAfterCollision(vessel, mobile, step, true);
    }

    boolean displaceAfterCollision(Tank vessel, MobileStructure mobile, MoveStep step, boolean surfacing) {
        Set<Coords> occupied = new HashSet<>(MobileStructureLinkage.footprint(mobile,
              step.getPosition(), step.getFacing(), step.getElevation()));
        Coords destination = displacement(vessel, occupied);
        if (destination != null) {
            relocate(vessel, destination);
            return true;
        }
        if (!surfacing) {
            addReport(gameManager.destroyEntity(vessel, "no legal displacement hex after mobile structure collision", false));
            return true;
        }
        // If the ship cannot be placed within one land hex of water, the mobile must move instead.
        var handler = new MobileStructureCollisionHandler(gameManager);
        destination = handler.displacement(mobile, new HashSet<>(LargeNavalVesselRules.footprint(vessel)));
        if (destination != null) {
            new MobileStructureMovementHandler(gameManager).relocate(mobile, destination, mobile.getFacing(), mobile.getElevation());
            handler.ground(mobile);
        }
        return false;
    }

    static boolean fullyUnderwater(Entity vessel, int elevation) {
        return elevation + LargeNavalVesselRules.aboveSurface(vessel) < 0;
    }

    private List<Coords> currentHexes(MobileStructure mobile) {
        return MobileStructureLinkage.group(mobile).stream().flatMap(module -> module.getCoordsList().stream())
              .filter(getGame().getBoard(mobile)::contains).distinct().toList();
    }

    Coords displacement(Tank vessel, Set<Coords> forbidden) {
        var board = getGame().getBoard(vessel);
        for (int distance = 1; distance <= Math.max(board.getWidth(), board.getHeight()); distance++) {
            var candidates = new ArrayList<Coords>();
            for (Coords coords : vessel.getPosition().allAtDistance(distance)) {
                var footprint = LargeNavalVesselRules.footprint(vessel, coords, vessel.getFacing());
                if (footprint.stream().allMatch(hex -> !forbidden.contains(hex) && legalHex(vessel, hex))) {
                    candidates.add(coords);
                }
            }
            if (!candidates.isEmpty()) { return candidates.get(Compute.randomInt(candidates.size())); }
        }
        return null;
    }

    private boolean legalHex(Tank vessel, Coords coords) {
        var board = getGame().getBoard(vessel);
        var hex = board.getHex(coords);
        if (hex == null || hex.containsTerrain(Terrains.IMPASSABLE)
              || !hex.containsTerrain(Terrains.WATER) && coords.allAdjacent().stream().map(board::getHex)
                    .filter(java.util.Objects::nonNull).noneMatch(adjacent -> adjacent.containsTerrain(Terrains.WATER))) {
            return false;
        }
        int bottom = vessel.getElevation() - LargeNavalVesselRules.belowSurface(vessel);
        int top = vessel.getElevation() + LargeNavalVesselRules.aboveSurface(vessel);
        if (board.getBuildingsAt(coords).stream().anyMatch(building -> BuildingElevation.base(building, coords) <= top
              && BuildingElevation.roof(building, coords) > bottom)) { return false; }
        return getGame().getEntitiesVector().stream().noneMatch(other -> other != vessel
              && other.getBoardId() == vessel.getBoardId() && !other.isDestroyed() && !other.isDoomed()
              && (LargeNavalVesselRules.applies(other) ? LargeNavalVesselRules.footprint(other).contains(coords)
                    : other.getOccupiedCoords().contains(coords))
              && !other.isAirborne() && !other.isAirborneVTOLorWIGE()
              && other.getElevation() <= top && other.getElevation() + other.height() + 1 > bottom);
    }

    private void relocate(Tank vessel, Coords destination) {
        vessel.setPosition(destination);
        var board = getGame().getBoard(vessel);
        if (LargeNavalVesselRules.footprint(vessel).stream().anyMatch(coords -> board.getHex(coords).depth()
              < LargeNavalVesselRules.belowSurface(vessel) - vessel.getElevation())) {
            int damage = (int) Math.ceil(vessel.getWeight() / 100) * (vessel.delta_distance >= 10 ? 3
                  : vessel.delta_distance >= 4 ? 2 : 1);
            addReport(gameManager.damageEntity(vessel, new HitData(Tank.LOC_FRONT), damage));
            vessel.immobilize();
            vessel.applyDamage();
        }
        gameManager.entityUpdate(vessel.getId());
    }
}

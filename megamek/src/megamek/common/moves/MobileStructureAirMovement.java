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

import java.util.List;

import megamek.common.Hex;
import megamek.common.board.Coords;
import megamek.common.enums.MoveStepType;
import megamek.common.rolls.PilotingRollData;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.BuildingElevation;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.EntityMovementType;
import megamek.common.units.MobileStructure;
import megamek.common.units.Terrains;

/** Air Mobile Structures retain a flat underside and use vertical small-craft landing/liftoff (TO:AUE p.33). */
public final class MobileStructureAirMovement {
    private MobileStructureAirMovement() { }

    public static boolean isAir(MobileStructure unit) {
        return unit.getMovementMode() == EntityMovementMode.VTOL;
    }

    public static boolean isAction(MoveStepType step) {
        return step == MoveStepType.VERTICAL_TAKE_OFF || step == MoveStepType.VERTICAL_LAND;
    }

    /** TW pp.88/204: lift off costs 2 MP, an Airship landing costs 1 MP. */
    public static int actionCost(MoveStepType step) {
        return step == MoveStepType.VERTICAL_TAKE_OFF ? 8 : 4;
    }

    public static int surface(MobileStructure unit, Coords coords) {
        return unit.getGame() == null || coords == null ? 0 : MobileStructureMovement.terrain(unit.getGame(), unit, coords).getLevel();
    }

    public static int absoluteBase(MobileStructure unit, Coords origin, int elevation) {
        return surface(unit, origin) + elevation;
    }

    /** Converts six-metre terrain levels to the low-altitude bands in TW p.81; an airborne craft is at least NOE. */
    public static int aerospaceAltitude(int absoluteLevel) {
        long metres = Math.max(1, (long) absoluteLevel * 6);
        int[] upperBounds = { 50, 100, 150, 250, 500, 750, 1000, 2000, 5000, 18000 };
        for (int band = 0; band < upperBounds.length; band++) {
            if (metres <= upperBounds[band]) {
                return band + 1;
            }
        }
        return 11;
    }

    /** Native floor zero is the carrier's underside; the physical datum also accounts for terrain and submersion. */
    public static int aerospaceAltitude(AbstractBuildingEntity unit, Coords position, int nativeFloor) {
        return aerospaceAltitude(BuildingElevation.altitude(unit.getGame(), unit, position, nativeFloor));
    }

    public static int translatedElevation(MobileStructure unit, Coords from, Coords to, int elevation) {
        return elevation + surface(unit, from) - surface(unit, to);
    }

    public static List<Coords> footprint(MobileStructure unit) {
        return MobileStructureLinkage.footprint(unit, unit.getPosition(), unit.getFacing(), unit.getElevation());
    }

    /** All underside hexes share one plane, including linked modules and portions beyond the map edge. */
    public static int landingSurface(MobileStructure unit, List<Coords> footprint) {
        return footprint.stream().mapToInt(coords -> surface(unit, coords)).max().orElse(0);
    }

    public static boolean isAirborne(MobileStructure unit) {
        return isAir(unit) && unit.getPosition() != null && unit.getGame() != null
              && absoluteBase(unit, unit.getPosition(), unit.getElevation()) > landingSurface(unit, footprint(unit));
    }

    public static int actionElevation(MobileStructure unit, MoveStepType step) {
        return landingSurface(unit, footprint(unit)) - surface(unit, unit.getPosition())
              + (step == MoveStepType.VERTICAL_TAKE_OFF ? 1 : 0);
    }

    public static int plannedActionCost(MobileStructure unit, MoveStepType step) {
        int cost = actionCost(step);
        var progress = unit.getMovementProgress();
        return progress != null && progress.destination().equals(unit.getPosition())
              && progress.facing() == unit.getFacing() && progress.elevation() == actionElevation(unit, step)
              ? Math.max(0, cost - progress.quarters()) : cost;
    }

    public static boolean canAttempt(MobileStructure unit, MoveStepType step) {
        if (!isAir(unit) || !isAction(step) || unit.isImmobile() || unit.isDestroyed() || unit.isDoomed()
              || unit.getGame() == null || unit.getPosition() == null || !unit.getGame().getBoard(unit).isGround()) {
            return false;
        }
        var footprint = footprint(unit);
        if (absoluteBase(unit, unit.getPosition(), unit.getElevation()) != landingSurface(unit, footprint)
              + (step == MoveStepType.VERTICAL_TAKE_OFF ? 0 : 1)) {
            return false;
        }
        // The general mobile partial-off-map rule still applies during liftoff/landing (TO:AUE p.35).
        int plane = landingSurface(unit, footprint);
        return footprint.stream().anyMatch(coords -> unit.getGame().getBoard(unit).contains(coords))
              && footprint.stream().map(coords -> unit.getGame().getBoard(unit).getHex(coords))
                    .filter(java.util.Objects::nonNull)
                    .noneMatch(hex -> hex.containsTerrain(Terrains.IMPASSABLE)
                          || step == MoveStepType.VERTICAL_LAND && hex.getLevel() == plane
                                && (hex.containsTerrain(Terrains.WATER) || hex.terrainLevel(Terrains.ROUGH) >= 2
                                      || hex.terrainLevel(Terrains.RUBBLE) >= 6));
    }

    /** Ignore the moving structure's own published building terrain when checking the air beneath it. */
    public static int obstructionCeiling(MobileStructure unit, Coords coords) {
        Hex original = unit.getGame().getBoard(unit).getHex(coords);
        if (original == null) {
            return Integer.MIN_VALUE;
        }
        Hex terrain = original.duplicate();
        for (int type : new int[] { Terrains.BUILDING, Terrains.BLDG_ELEV, Terrains.BLDG_CF, Terrains.BLDG_ARMOR,
                                   Terrains.BLDG_CLASS }) {
            terrain.removeTerrain(type);
        }
        int ceiling = terrain.ceiling();
        for (var building : unit.getGame().getBoard(unit).getBuildingsAt(coords)) {
            // Other mobile structures are handled by the mobile collision rules, not blocked by map terrain.
            if (!(building instanceof MobileStructure)) {
                ceiling = Math.max(ceiling, original.getLevel() + BuildingElevation.roof(building, coords));
            }
        }
        return ceiling;
    }

    public static PilotingRollData controlRoll(MobileStructure unit, MoveStepType step) {
        PilotingRollData roll = unit.getBasePilotingRoll(EntityMovementType.MOVE_SAFE_THRUST);
        var conditions = unit.getGame().getPlanetaryConditions().forEntity(unit);
        int light = conditions.getLightHitPenalty(false);
        if (light != 0) {
            roll.addModifier(light, conditions.getLight().toString());
        }
        if (MobileStructureLinkage.group(unit).stream().anyMatch(MobileStructure::isAirLandingGearDamaged)) {
            roll.addModifier(step == MoveStepType.VERTICAL_TAKE_OFF ? 1 : 5, "landing gear damaged");
        }
        List<Coords> footprint = footprint(unit);
        if (step == MoveStepType.VERTICAL_TAKE_OFF) {
            if (footprint.stream().map(c -> unit.getGame().getBoard(unit).getHex(c)).filter(java.util.Objects::nonNull)
                  .allMatch(hex -> hex.hasPavementOrRoad() && !hex.containsTerrain(Terrains.RUBBLE))) {
                roll.addModifier(-1, "airfield or landing pad");
            }
            int level = landingSurface(unit, footprint);
            boolean crater = footprint.stream().flatMap(c -> c.allAdjacent().stream()).filter(c -> !footprint.contains(c))
                  .allMatch(c -> surface(unit, c) > level);
            if (crater) {
                roll.addModifier(3, "lifting off from crater");
            }
        } else {
            // TW p.86 table (11th printing): use only the highest terrain modifier, halved for vertical landing.
            int highest = footprint.stream().mapToInt(coords -> {
                Hex hex = unit.getGame().getBoard(unit).getHex(coords);
                if (hex == null || hex.hasPavementOrRoad()) {
                    return 0;
                }
                int modifier = 2; // clear terrain
                for (int type : hex.getTerrainTypes()) {
                    if (type != Terrains.BUILDING) {
                        modifier = Math.max(modifier, Terrains.landingModifier(type, hex.terrainLevel(type)));
                    }
                }
                return modifier;
            }).max().orElse(0);
            if (highest > 0) {
                roll.addModifier(highest / 2, "landing terrain (vertical)");
            }
        }
        return roll;
    }
}

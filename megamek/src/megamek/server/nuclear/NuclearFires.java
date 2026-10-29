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
package megamek.server.nuclear;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import megamek.common.Hex;
import megamek.common.InfernoTracker;
import megamek.common.Report;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.enums.BuildingType;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import megamek.logging.MMLogger;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Starts the flash fires of a nuclear detonation (IO:AE pp. 165-166). In the blast area, every non-crater, non-water
 * hex with flammable terrain or ground units rolls 2D6 + one-tenth of the blast damage at that hex and catches fire on
 * 5 or more; the units standing there are set aflame as if hit by Inferno until the end of the scenario or until they
 * are submerged. Beyond the blast area but inside the secondary radius, flammable terrain catches fire on 9 or more.
 * Hardened buildings do not burn.
 */
class NuclearFires {
    private static final MMLogger LOGGER = MMLogger.create(NuclearFires.class);

    /** In the blast area a hex catches fire on this or more (IO:AE p.166). */
    static final int BLAST_AREA_FIRE_TARGET = 5;
    /** Beyond the blast area, inside the secondary radius, a hex catches fire on this or more (IO:AE p.166). */
    static final int SECONDARY_RADIUS_FIRE_TARGET = 9;
    /** The fire roll adds 1 for every this much blast damage at the hex (IO:AE p.166). */
    static final int DAMAGE_PER_FIRE_POINT = 10;
    /**
     * Rounds of Inferno burn given to a unit set aflame: it burns for the rest of the scenario (IO:AE p.165), or until
     * it washes the fire off in water.
     */
    static final int SCENARIO_BURN_ROUNDS = 999;
    private static final int BOARD_ID = 0;

    private final Game game;

    NuclearFires(TWGameManager gameManager) {
        this.game = gameManager.getGame();
    }

    /**
     * Rolls for fire in every hex of the blast area and secondary radius. Follows the TacOps start fires option, like
     * every other fire source (HammerGS, 2026-10-28).
     *
     * @param groundZero   the Ground Zero hex
     * @param profile      the detonation's numbers
     * @param changedHexes filled with every hex that catches fire
     * @param vDesc        the report the results are added to
     */
    void startFires(Coords groundZero, NuclearBlastProfile profile, Set<Coords> changedHexes, Vector<Report> vDesc) {
        if (!game.getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_START_FIRE)) {
            LOGGER.info("[Nukes] Flash fires: none, the start fires option is off");
            Report report = new Report(3008);
            report.indent(2);
            report.type = Report.PUBLIC;
            vDesc.addElement(report);
            return;
        }
        String cannotBurn = game.getPlanetaryConditions().cannotStartFire();
        if (cannotBurn != null) {
            LOGGER.info("[Nukes] Flash fires: none, nothing burns in a {}", cannotBurn);
            Report report = new Report(3007);
            report.indent(2);
            report.add(cannotBurn);
            report.type = Report.PUBLIC;
            vDesc.addElement(report);
            return;
        }

        int[] damages = profile.blastDamageByDistance();
        int craterRadius = profile.craterRadius();
        Map<Coords, List<Entity>> unitsByHex = unitsThatCanBurn(groundZero, damages.length);
        Board board = game.getBoard(BOARD_ID);
        int blastHexesRolled = 0;
        int blastHexesIgnited = 0;
        int outerHexesRolled = 0;
        int outerHexesIgnited = 0;
        int unitsAflame = 0;
        for (int y = 0; y < board.getHeight(); y++) {
            for (int x = 0; x < board.getWidth(); x++) {
                Coords coords = new Coords(x, y);
                int distance = groundZero.distance(coords);
                if ((distance < craterRadius) || (distance > profile.secondaryRadius())) {
                    continue;
                }
                Hex hex = board.getHex(coords);
                if ((hex == null) || hex.containsTerrain(Terrains.WATER)) {
                    continue;
                }
                boolean flammable = hasFlammableTerrain(hex) && !hex.containsTerrain(Terrains.FIRE);
                boolean inBlastArea = distance < damages.length;
                List<Entity> units = inBlastArea ? unitsByHex.getOrDefault(coords, List.of()) : List.of();
                if (!flammable && units.isEmpty()) {
                    continue;
                }
                int damage = inBlastArea ? damages[distance] : 0;
                int target = inBlastArea ? BLAST_AREA_FIRE_TARGET : SECONDARY_RADIUS_FIRE_TARGET;
                int roll = Compute.rollD6(2).getIntValue();
                boolean ignites = (roll + (damage / DAMAGE_PER_FIRE_POINT)) >= target;
                LOGGER.debug("[Nukes] Hex {} ({} hexes out, {} damage) rolls {} + {} against {}: {}",
                      coords.getBoardNum(), distance, damage, roll, damage / DAMAGE_PER_FIRE_POINT, target,
                      ignites ? "catches fire" : "does not catch fire");
                if (inBlastArea) {
                    blastHexesRolled++;
                } else {
                    outerHexesRolled++;
                }
                if (!ignites) {
                    continue;
                }
                if (flammable) {
                    hex.addTerrain(new Terrain(Terrains.FIRE, Terrains.FIRE_LVL_NORMAL));
                    changedHexes.add(coords);
                    if (inBlastArea) {
                        blastHexesIgnited++;
                    } else {
                        outerHexesIgnited++;
                    }
                }
                for (Entity entity : units) {
                    setAflame(entity, vDesc);
                    unitsAflame++;
                }
            }
        }

        LOGGER.info("[Nukes] Flash fires: blast area {} of {} hex(es) rolled caught fire (needs {}+ with damage/10); "
                    + "secondary radius {} of {} caught fire (needs {}+); {} unit(s) set aflame", blastHexesIgnited,
              blastHexesRolled, BLAST_AREA_FIRE_TARGET, outerHexesIgnited, outerHexesRolled,
              SECONDARY_RADIUS_FIRE_TARGET, unitsAflame);
        Report report = new Report(6562);
        report.indent(2);
        report.type = Report.PUBLIC;
        report.add(blastHexesIgnited + outerHexesIgnited);
        report.add(blastHexesIgnited);
        report.add(outerHexesIgnited);
        vDesc.addElement(report);
    }

    /**
     * Ground units that can be set aflame, by hex: units inside a building get the building's fire instead, and
     * airborne units, VTOLs included, are not set aflame (HammerGS, 2026-10-28).
     */
    private Map<Coords, List<Entity>> unitsThatCanBurn(Coords groundZero, int blastAreaRings) {
        Map<Coords, List<Entity>> unitsByHex = new HashMap<>();
        for (Entity entity : game.getEntitiesVector()) {
            if (entity.isDestroyed() || entity.isDoomed() || (entity.getPosition() == null)
                  || (entity.getTransportId() != Entity.NONE)
                  || (groundZero.distance(entity.getPosition()) >= blastAreaRings)) {
                continue;
            }
            if (entity.isAirborne() || entity.isAirborneVTOLorWIGE()) {
                LOGGER.info("[Nukes] {} is airborne: not set aflame by the flash fires", entity.getDisplayName());
                continue;
            }
            if (Compute.isInBuilding(game, entity)) {
                LOGGER.info("[Nukes] {} is inside a building: the building burns instead of the unit",
                      entity.getDisplayName());
                continue;
            }
            unitsByHex.computeIfAbsent(entity.getPosition(), coords -> new ArrayList<>()).add(entity);
        }
        return unitsByHex;
    }

    /**
     * @return {@code true} if the hex holds terrain that can burn; hardened buildings do not (IO:AE p.165)
     */
    static boolean hasFlammableTerrain(Hex hex) {
        if (hex.containsTerrain(Terrains.WOODS) || hex.containsTerrain(Terrains.JUNGLE)
              || hex.containsTerrain(Terrains.FIELDS) || hex.containsTerrain(Terrains.INDUSTRIAL)
              || hex.containsTerrain(Terrains.FUEL_TANK)) {
            return true;
        }
        return hex.containsTerrain(Terrains.BUILDING)
              && (hex.terrainLevel(Terrains.BUILDING) != BuildingType.HARDENED.getTypeValue());
    }

    /**
     * Sets a unit aflame as if hit by Inferno, burning until the end of the scenario or until it is submerged (IO:AE
     * p.165). A Mek gains Inferno heat every turn; other units take fire damage.
     */
    private void setAflame(Entity entity, Vector<Report> vDesc) {
        entity.infernos.add(InfernoTracker.STANDARD_TURN, SCENARIO_BURN_ROUNDS);
        LOGGER.info("[Nukes] {} at hex {} is set aflame by the flash fire", entity.getDisplayName(),
              entity.getPosition().getBoardNum());
        Report report = new Report(6561);
        report.subject = entity.getId();
        report.indent(2);
        report.addDesc(entity);
        vDesc.addElement(report);
    }
}

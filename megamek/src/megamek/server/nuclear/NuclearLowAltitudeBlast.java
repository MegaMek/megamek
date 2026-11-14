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

import java.util.Vector;

import megamek.common.Report;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import megamek.server.totalWarfare.TWGameManager;

/**
 * Deals a nuclear detonation's blast to the airborne units on a Low-Altitude Map (IO:AE p.166). The blast reaches the
 * blast radius divided by 17 in Low-Altitude Map hexes, and only units at or below the weapon's Blast Height. Example:
 * an Alamo air burst 3 hexes from a Stuka at altitude 4 deals 541; the Stuka then rolls at +2 or falls.
 */
class NuclearLowAltitudeBlast {
    private static final MMLogger LOGGER = MMLogger.create(NuclearLowAltitudeBlast.class);

    private final Game game;
    private final int skyBoardId;
    private final NuclearBlastDamage blastDamage;
    private final NuclearSecondaryEffects secondaryEffects;

    /**
     * @param gameManager the game manager running the game the detonation happens in
     * @param skyBoardId  the Low-Altitude Map the blast sweeps
     */
    NuclearLowAltitudeBlast(TWGameManager gameManager, int skyBoardId) {
        this.game = gameManager.getGame();
        this.skyBoardId = skyBoardId;
        this.blastDamage = new NuclearBlastDamage(gameManager, skyBoardId);
        this.secondaryEffects = new NuclearSecondaryEffects(gameManager);
    }

    /**
     * Strikes every airborne unit on the Low-Altitude Map within the blast radius and at or below the Blast Height,
     * then has the survivors roll at +2 or fall.
     *
     * @param skyGroundZero the detonation's hex on the Low-Altitude Map
     * @param profile       the detonation's numbers
     * @param vDesc         the report the results are added to
     * @param blastedUnits  filled with the ids of the units the blast struck
     */
    void apply(Coords skyGroundZero, NuclearBlastProfile profile, Vector<Report> vDesc,
          Vector<Integer> blastedUnits) {
        int radius = profile.lowAltitudeRadius();
        int blastHeight = profile.blastHeight();
        LOGGER.info("[Nukes] Low-altitude blast on board {} at hex {}: radius {} hex(es) ({} / {}), blast height {}, "
                    + "damage reduction {} x {}", skyBoardId, skyGroundZero.getBoardNum(), radius,
              profile.blastRadius(), NuclearBlastProfile.GROUND_HEXES_PER_LOW_ALTITUDE_HEX, blastHeight,
              NuclearBlastProfile.LOW_ALTITUDE_DISTANCE_FACTOR, profile.lowAltitudeDamageReduction());
        Report header = new Report(6566, Report.PUBLIC);
        header.add(radius);
        header.add(skyGroundZero.getBoardNum());
        header.add(blastHeight);
        vDesc.addElement(header);

        Vector<Integer> struckHere = new Vector<>();
        for (Entity entity : game.inGameTWEntities()) {
            if (entity.isDestroyed() || entity.isDoomed() || !entity.isDeployed() || (entity.getPosition() == null)
                  || (entity.getBoardId() != skyBoardId)) {
                continue;
            }
            int distance = skyGroundZero.distance(entity.getPosition());
            if (!entity.isAirborne()) {
                LOGGER.info("[Nukes] {} at hex {} of the low-altitude map is not airborne: the low-altitude blast "
                      + "passes it by", entity.getDisplayName(), entity.getPosition().getBoardNum());
                continue;
            }
            if (distance > radius) {
                LOGGER.info("[Nukes] {} at hex {} is {} hexes out, beyond the low-altitude radius {}",
                      entity.getDisplayName(), entity.getPosition().getBoardNum(), distance, radius);
                continue;
            }
            if (entity.getAltitude() > blastHeight) {
                LOGGER.info("[Nukes] {} at hex {} flies at altitude {}, above blast height {}: out of the blast",
                      entity.getDisplayName(), entity.getPosition().getBoardNum(), entity.getAltitude(),
                      blastHeight);
                continue;
            }
            int damage = profile.lowAltitudeDamageAt(distance);
            LOGGER.info("[Nukes] {} at hex {}, altitude {}, {} hex(es) from the detonation: {} blast damage",
                  entity.getDisplayName(), entity.getPosition().getBoardNum(), entity.getAltitude(), distance,
                  damage);
            if (damage == 0) {
                continue;
            }
            struckHere.add(entity.getId());
            int damageTaken = blastDamage.applyClusters(entity, damage, entity.sideTable(skyGroundZero),
                  skyGroundZero, vDesc);
            blastDamage.addBlastHeat(entity, damageTaken, vDesc);
            Report.addNewline(vDesc);
        }

        for (int unitId : struckHere) {
            Entity entity = game.getEntity(unitId);
            if ((entity != null) && !entity.isDoomed() && !entity.isDestroyed()) {
                blastDamage.rollToWithstandBlast(entity, vDesc);
            }
        }
        blastedUnits.addAll(struckHere);
        LOGGER.info("[Nukes] Low-altitude blast struck {} unit(s)", struckHere.size());
    }

    /**
     * Secondary effects on the Low-Altitude Map: every airborne unit within the secondary radius divided by 17 and at
     * or below the Secondary Effect Height rolls on the Secondary Nuclear Effects Table and suffers EMI for the rest of
     * the scenario (IO:AE p.165; HammerGS, 2026-11-06). A unit carried by an aircraft rolls at its carrier's hex and
     * altitude. Example: an Alamo air burst reaches 114 / 17 = 7 hexes, up to altitude 9.
     *
     * @param skyGroundZero the detonation's hex on the Low-Altitude Map
     * @param profile       the detonation's numbers
     * @param vDesc         the report the results are added to
     */
    void applySecondaryEffects(Coords skyGroundZero, NuclearBlastProfile profile, Vector<Report> vDesc) {
        int radius = profile.lowAltitudeSecondaryRadius();
        int secondaryHeight = profile.secondaryHeight();
        LOGGER.info("[Nukes] Low-altitude secondary effects on board {} at hex {}: radius {} hex(es) ({} / {}), "
                    + "secondary effect height {}", skyBoardId, skyGroundZero.getBoardNum(), radius,
              profile.secondaryRadius(), NuclearBlastProfile.GROUND_HEXES_PER_LOW_ALTITUDE_HEX, secondaryHeight);
        Report header = new Report(6567, Report.PUBLIC);
        header.indent(2);
        header.add(radius);
        header.add(skyGroundZero.getBoardNum());
        header.add(secondaryHeight);
        vDesc.addElement(header);

        int unitsRolled = 0;
        for (Entity entity : game.getEntitiesVector()) {
            if (entity.isDoomed() || entity.isDestroyed()) {
                continue;
            }
            Entity positioned = (entity.getTransportId() == Entity.NONE)
                  ? entity
                  : game.getEntity(entity.getTransportId());
            if ((positioned == null) || (positioned.getPosition() == null)
                  || (positioned.getBoardId() != skyBoardId)) {
                continue;
            }
            if (!positioned.isAirborne()) {
                LOGGER.info("[Nukes] {} at hex {} of the low-altitude map is not airborne: no secondary effects",
                      entity.getDisplayName(), positioned.getPosition().getBoardNum());
                continue;
            }
            int distance = skyGroundZero.distance(positioned.getPosition());
            if (distance > radius) {
                LOGGER.info("[Nukes] {} at hex {} is {} hexes out, beyond the low-altitude secondary radius {}",
                      entity.getDisplayName(), positioned.getPosition().getBoardNum(), distance, radius);
                continue;
            }
            if (positioned.getAltitude() > secondaryHeight) {
                LOGGER.info("[Nukes] {} at hex {} flies at altitude {}, above secondary effect height {}: out of "
                      + "reach", entity.getDisplayName(), positioned.getPosition().getBoardNum(),
                      positioned.getAltitude(), secondaryHeight);
                continue;
            }
            LOGGER.info("[Nukes] {} at hex {}, altitude {}, {} hex(es) from the detonation: secondary effects",
                  entity.getDisplayName(), positioned.getPosition().getBoardNum(), positioned.getAltitude(),
                  distance);
            secondaryEffects.apply(entity, skyGroundZero, vDesc);
            unitsRolled++;
        }
        LOGGER.info("[Nukes] Low-altitude secondary effects reached {} unit(s)", unitsRolled);
    }
}

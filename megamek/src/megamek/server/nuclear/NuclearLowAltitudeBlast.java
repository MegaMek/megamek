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

import jakarta.annotation.Nullable;
import megamek.common.Report;
import megamek.common.board.Board;
import megamek.common.board.BoardLocation;
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

    /**
     * @param gameManager the game manager running the game the detonation happens in
     * @param skyBoardId  the Low-Altitude Map the blast sweeps
     */
    NuclearLowAltitudeBlast(TWGameManager gameManager, int skyBoardId) {
        this.game = gameManager.getGame();
        this.skyBoardId = skyBoardId;
        this.blastDamage = new NuclearBlastDamage(gameManager, skyBoardId);
    }

    /**
     * Finds where a detonation sits on the Low-Altitude Map. A detonation on a low-altitude map is at its own hex. A
     * detonation on a ground map that lies inside a low-altitude map is at the low-altitude hex holding that ground
     * map. Example: a ground map embedded in sky hex 0505 puts any detonation on it at 0505 of the sky map.
     *
     * @param game     the game
     * @param position the Ground Zero hex
     * @param boardId  the board Ground Zero is on
     *
     * @return the detonation's place on the Low-Altitude Map, or {@code null} if there is no low-altitude map above it
     */
    static @Nullable BoardLocation lowAltitudeGroundZero(Game game, Coords position, int boardId) {
        Board board = game.getBoard(boardId);
        if (board == null) {
            LOGGER.info("[Nukes] Board {} is not in the game: no low-altitude blast", boardId);
            return null;
        }
        if (board.isLowAltitude()) {
            LOGGER.info("[Nukes] Detonation at hex {} is on low-altitude board {}", position.getBoardNum(), boardId);
            return BoardLocation.of(position, boardId);
        }
        Board enclosingBoard = game.getEnclosingBoard(board);
        if ((enclosingBoard == null) || !enclosingBoard.isLowAltitude()) {
            LOGGER.info("[Nukes] Ground board {} lies inside no low-altitude map: the blast stays on the ground map",
                  boardId);
            return null;
        }
        Coords skyHex = enclosingBoard.embeddedBoardPosition(boardId);
        if (skyHex == null) {
            LOGGER.info("[Nukes] Ground board {} names low-altitude board {} as its enclosing board, but has no hex "
                  + "there: no low-altitude blast", boardId, enclosingBoard.getBoardId());
            return null;
        }
        LOGGER.info("[Nukes] Ground board {} lies in hex {} of low-altitude board {}: the blast also sweeps that map",
              boardId, skyHex.getBoardNum(), enclosingBoard.getBoardId());
        return BoardLocation.of(skyHex, enclosingBoard.getBoardId());
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
}

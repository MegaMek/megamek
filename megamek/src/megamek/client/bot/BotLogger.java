/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.bot;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import megamek.client.bot.princess.RankedPath;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.UnitRole;
import megamek.logging.MMLogger;

/**
 * The GameDatasetLogger class is used to log game data to a file in the log directory with TSV format. It contains
 * every action taken by  every unit in the game and the result of the game state after those actions.
 *
 * @author Luana Coppio
 */

public class BotLogger {
    private static final MMLogger LOGGER = MMLogger.create("BotLogger");
    protected final DecimalFormat LOG_DECIMAL = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.US));

    /**
     * Appends a game state to the log file.
     *
     * @param game       the game state to append, which contains all the unit informations
     * @param withHeader if true, includes a header line with column names in the log file
     */
    public void append(Game game, boolean withHeader) {
        try {
            if (withHeader) {
                append(
                      String.join(
                            "\t",
                            "ROUND",
                            "PHASE",
                            "PLAYER_ID",
                            "ENTITY_ID",
                            "CHASSIS",
                            "MODEL",
                            "TYPE",
                            "ROLE",
                            "X",
                            "Y",
                            "FACING",
                            "MP",
                            "HEAT",
                            "PRONE",
                            "AIRBORNE",
                            "OFF_BOARD",
                            "CRIPPLED",
                            "DESTROYED",
                            "ARMOR_P",
                            "INTERNAL_P",
                            "DONE"
                      )
                );
            }
            var currentRound = game.getCurrentRound() + "";
            var gamePhase = game.getPhase().name();
            for (var inGameObject : game.getInGameObjects()) {
                if (!(inGameObject instanceof Entity entity)) {
                    continue;
                }
                var ownerID = entity.getOwner().getId() + "";
                var chassis = entity.getChassis();
                var model = entity.getModel();
                var entityId = entity.getId() + "";
                var coords = entity.getPosition() != null ?
                      entity.getPosition().getX() + "\t" + entity.getPosition().getY() :
                      "-1\t-1";
                var facing = entity.getFacing() + "";
                var mp = entity.getRunMP() > 0 ?
                      LOG_DECIMAL.format(Math.min(1.0, entity.getMpUsedLastRound() / (double) entity.getRunMP())) :
                      "0.00";
                var isProne = entity.isProne() ? "1" : "0";
                var heatP = entity.getHeatCapacity() > 0 ?
                      LOG_DECIMAL.format(entity.getHeat() / (double) entity.getHeatCapacity()) :
                      "0.00";
                var isAirborne = entity.isAirborne() ? "1" : "0";
                var isOffBoard = entity.isOffBoard() ? "1" : "0";
                var isDone = entity.isDone() ? "1" : "0";
                var armorP = LOG_DECIMAL.format(entity.getArmorRemainingPercent());
                var internalP = LOG_DECIMAL.format(entity.getInternalRemainingPercent());
                var isCrippled = entity.isCrippled() ? "1" : "0";
                var isDestroyed = entity.isDestroyed() || entity.isDoomed() ? "1" : "0";
                var type = entity.getClass().getSimpleName();
                var role = entity.getRole() == null ? UnitRole.NONE.name() : entity.getRole().name();

                append(
                      String.join(
                            "\t",
                            currentRound,
                            gamePhase,
                            ownerID,
                            entityId,
                            chassis,
                            model,
                            type,
                            role,
                            coords,
                            facing,
                            mp,
                            heatP,
                            isProne,
                            isAirborne,
                            isOffBoard,
                            isCrippled,
                            isDestroyed,
                            armorP,
                            internalP,
                            isDone
                      )
                );
            }

            var minefields = game.getMinedCoords();
            if (minefields != null && minefields.hasMoreElements()) {
                if (withHeader) {
                    append(String.join("\t", "ROUND", "PHASE", "OBJECT", "X", "Y"));
                }

                while (minefields.hasMoreElements()) {
                    var minefield = minefields.nextElement();
                    if (minefield == null) {
                        continue;
                    }
                    append(String.join("\t",
                          currentRound,
                          gamePhase,
                          "MINEFIELD",
                          minefield.getX() + "",
                          minefield.getY() + ""));
                }
            }
        } catch (Exception ex) {
            LOGGER.error(ex, "Error logging entity action");
        }
    }

    /**
     * Appends a move path to the log file
     *
     * @param rankedPath the RankedPath to append, which contains the path, rank, scores, and entity information
     * @param index      if 0 it will print header, otherwise it wil just add the index here.
     */
    public void append(RankedPath rankedPath, int index) {
        try {
            var movePath = rankedPath.getPath();
            var rank = rankedPath.getRank() + "";
            var score = rankedPath.getScores();
            var ownerID = movePath.getEntity().getOwner().getId() + "";
            var chassis = movePath.getEntity().getChassis();
            var model = movePath.getEntity().getModel();
            var entityId = movePath.getEntity().getId() + "";
            var from = movePath.getStartCoords() == null ?
                  "-1\t-1" :
                  movePath.getStartCoords().getX() + "\t" + movePath.getStartCoords().getY();
            var to = movePath.getFinalCoords() == null ?
                  "-1\t-1" :
                  movePath.getFinalCoords().getX() + "\t" + movePath.getFinalCoords().getY();
            var hexesMoved = movePath.getHexesMoved() + "";
            var facing = movePath.getFinalFacing() + "";
            var mpUsed = movePath.getMpUsed() + "";
            var maxMp = movePath.getMaxMP() + "";
            var usedPercentMp = movePath.getMaxMP() > 0 ?
                  LOG_DECIMAL.format(movePath.getMpUsed() / (double) movePath.getMaxMP()) :
                  "0.00";
            var heatP = movePath.getEntity().getHeatCapacity() > 0 ?
                  LOG_DECIMAL.format(movePath.getEntity().getHeat() / (double) movePath.getEntity().getHeatCapacity()) :
                  "0.00";
            var distanceTravelled = movePath.getDistanceTravelled() + "";
            var isJumping = movePath.isJumping() ? "1" : "0";
            var isProne = movePath.getFinalProne() ? "1" : "0";
            var isMoveLegal = movePath.isMoveLegal() ? "1" : "0";
            var armor = LOG_DECIMAL.format(Math.max(0, movePath.getEntity().getArmorRemainingPercent()));
            var internal = LOG_DECIMAL.format(Math.max(0, movePath.getEntity().getInternalRemainingPercent()));
            var steps = new StringBuilder();
            movePath.getStepVector().forEach(step -> steps.append(step.toString()).append(" "));

            var header = new ArrayList<>(List.of("INDEX", "PLAYER_ID", "ENTITY_ID", "RANK", "CHASSIS", "MODEL",
                  "FACING", "FROM_X", "FROM_Y", "TO_X", "TO_Y", "HEXES_MOVED", "DISTANCE", "MP_USED", "MAX_MP", "MP_P",
                  "HEAT_P", "ARMOR_P", "INTERNAL_P", "JUMPING", "PRONE", "LEGAL", "STEPS"));
            var scoreHeaders = new ArrayList<>(score.keySet());
            scoreHeaders.sort(String::compareTo);
            scoreHeaders.forEach(key -> header.add(key + "_SCORE"));

            if (index == 0) {
                append(
                      String.join(
                            "\t",
                            header
                      )
                );
            }

            var values = new ArrayList<>(List.of(index + "",
                  ownerID,
                  entityId,
                  rank,
                  chassis,
                  model,
                  facing,
                  from,
                  to,
                  hexesMoved,
                  distanceTravelled,
                  mpUsed,
                  maxMp,
                  usedPercentMp,
                  heatP,
                  armor,
                  internal,
                  isJumping,
                  isProne,
                  isMoveLegal,
                  steps.toString()));
            for (var key : header) {
                if (key.endsWith("_SCORE")) {
                    var k = key.substring(0, key.length() - 6);
                    values.add(score.get(k) + "");
                }
            }
            append(
                  String.join("\t", values)
            );
        } catch (Exception ex) {
            LOGGER.error(ex, "Error logging entity action {}", ex.getMessage());
        }
    }

    private void append(String toLog) {
        LOGGER.info(toLog);
    }
}

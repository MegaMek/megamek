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
import java.util.Map;
import java.util.Locale;

import megamek.client.bot.princess.RankedPath;
import megamek.common.units.Entity;
import megamek.common.units.IAero;
import megamek.common.game.Game;
import megamek.common.units.UnitRole;
import megamek.logging.MMLogger;

/**
 * The GameDatasetLogger class is used to log game data to a file in the log directory with TSV format. It contains
 * every action taken by  every unit in the game and the result of the game state after those actions.
 *
 * @author Luana Coppio
 */

public class BotLogger {

    /**
     * Column names of the score block as last written, so every row can be read against a known schema.
     *
     * <p>Score keys are contributed by whichever path ranker ran, so the set can differ from row to row: a different
     * ranker for infantry, a doctrine recording its own reasoning, a term only recorded in some situations. Rows of
     * differing width under a header written once are not analysable, which defeats the point of logging the
     * reasoning at all. A fresh header is written whenever the set changes.</p>
     *
     * <p>Guarded by the lock on this logger: one instance serves every bot in the JVM, so the decision to write a
     * header and the writing of it have to happen together.</p>
     */
    private List<String> currentScoreHeaders = List.of();

    /**
     * Marks a path-ranking row.
     *
     * <p>Several kinds of record share one file, and two bots with different path rankers write different score
     * columns into it, so rows vary in both shape and meaning. A leading record type lets a reader filter to the
     * rows it understands before anything else, the same way the game-state log names its rows.</p>
     */
    private static final String PATH_RANK_RECORD = "PathRank";
    private static final MMLogger LOGGER = MMLogger.create("BotLogger");
    protected final DecimalFormat LOG_DECIMAL = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.US));

    /**
     * Appends a game state to the log file.
     *
     * @param game       the game state to append, which contains all the unit informations
     * @param withHeader if true, includes a header line with column names in the log file
     */
    /**
     * How a unit is fighting, as its own bot already decided it - not recomputed here.
     *
     * @param behavior the unit behavior state, or empty if the bot has not evaluated this unit yet this turn
     * @param posture  the force posture on this unit's board, or empty if none was resolved this round
     */
    public record UnitContext(String behavior, String posture) {
        static final UnitContext UNKNOWN = new UnitContext("", "");
    }

    public void append(Game game, boolean withHeader) {
        append(game, withHeader, Map.of());
    }

    /**
     * Appends a game state, tagging this bot's own units with how they are fighting.
     *
     * @param game         the game state to append
     * @param withHeader   if true, includes a header line with column names
     * @param unitContexts entity id to behavior and posture, for the logging bot's units only; other
     *                     players' units are left blank because this bot does not know their intent
     */
    public void append(Game game, boolean withHeader, Map<Integer, UnitContext> unitContexts) {
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
                            "BOARD_ID",
                            "FACING",
                            "MP",
                            "HEAT",
                            "PRONE",
                            "AIRBORNE",
                            "ALTITUDE",
                            "VELOCITY",
                            "OUT_OF_CONTROL",
                            "OFF_BOARD",
                            "CRIPPLED",
                            "DESTROYED",
                            "ARMOR_P",
                            "INTERNAL_P",
                            "SI_P",
                            "BEHAVIOR",
                            "POSTURE",
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
                var boardId = entity.getBoardId() + "";
                // Aerospace state. AIRBORNE alone cannot tell altitude 1 from altitude 10, and altitude is
                // what decides whether two aircraft can shoot at each other at all (TW p.241), so without
                // these an aerospace game is unanalysable from this file.
                var altitude = entity.getAltitude() + "";
                var velocity = (entity instanceof IAero aero) ? aero.getCurrentVelocity() + "" : "0";
                var isOutOfControl = (entity instanceof IAero aero) && aero.isOutControlTotal() ? "1" : "0";
                // Structural integrity is the aerospace health bar; internal structure is not. Blank for
                // anything that has no SI, so a reader sees a missing value rather than a misleading 1.00.
                var structuralIntegrityP = structuralIntegrityPercent(entity);
                var context = unitContexts.getOrDefault(entity.getId(), UnitContext.UNKNOWN);

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
                            boardId,
                            facing,
                            mp,
                            heatP,
                            isProne,
                            isAirborne,
                            altitude,
                            velocity,
                            isOutOfControl,
                            isOffBoard,
                            isCrippled,
                            isDestroyed,
                            armorP,
                            internalP,
                            structuralIntegrityP,
                            context.behavior(),
                            context.posture(),
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
    // Synchronized because PathRanker holds one BotLogger for the whole JVM and every bot ranks paths on its own
    // thread. Choosing whether to write a header and then writing it is a check-then-act on shared state: without
    // this, two bots with different score columns can interleave and leave rows with no matching header above them.
    public synchronized void append(RankedPath rankedPath, int index) {
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
            // The altitude this path starts and ends at, and the velocity it carries away. On a ground
            // mapsheet altitude is the only control an aircraft can change within a turn - facing needs 8 to
            // 52 hexes of straight flight first (TW p.92) - so a path record without it does not say what the
            // bot actually decided. Logged for every ranker, not only the ones that score altitude, so a
            // control run and a doctrine run can be compared column for column.
            var boardId = movePath.getFinalBoardId() + "";
            var fromAltitude = movePath.getEntity().getAltitude() + "";
            var toAltitude = movePath.getFinalAltitude() + "";
            var finalVelocity = movePath.getFinalVelocity() + "";
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

            var header = new ArrayList<>(List.of("RECORD", "INDEX", "PLAYER_ID", "ENTITY_ID", "RANK", "CHASSIS",
                  "MODEL",
                  "FACING", "FROM_X", "FROM_Y", "TO_X", "TO_Y", "BOARD_ID", "FROM_ALT", "TO_ALT", "VELOCITY",
                  "HEXES_MOVED", "DISTANCE", "MP_USED", "MAX_MP", "MP_P",
                  "HEAT_P", "ARMOR_P", "INTERNAL_P", "JUMPING", "PRONE", "LEGAL", "STEPS"));
            var scoreHeaders = new ArrayList<>(score.keySet());
            scoreHeaders.sort(String::compareTo);
            scoreHeaders.forEach(key -> header.add(key + "_SCORE"));

            // Write a header whenever the schema changes, not only for the best-ranked path, so that every
            // row which follows can be read against the header above it.
            if ((index == 0) || !scoreHeaders.equals(currentScoreHeaders)) {
                currentScoreHeaders = List.copyOf(scoreHeaders);
                append(String.join("\t", header));
            }

            var values = new ArrayList<>(List.of(PATH_RANK_RECORD,
                  index + "",
                  ownerID,
                  entityId,
                  rank,
                  chassis,
                  model,
                  facing,
                  from,
                  to,
                  boardId,
                  fromAltitude,
                  toAltitude,
                  finalVelocity,
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
                    Double value = score.get(k);
                    // Empty rather than the literal "null", so a reader sees a missing value, not a parse error.
                    values.add(value == null ? "" : value.toString());
                }
            }
            append(
                  String.join("\t", values)
            );
        } catch (Exception ex) {
            LOGGER.error(ex, "Error logging entity action {}", ex.getMessage());
        }
    }

    /**
     * Structural integrity remaining, as a fraction of the airframe's original.
     *
     * <p>The real health bar for an aerospace unit: armour is a buffer and internal structure barely moves,
     * but SI loss is what kills the airframe, and it also caps how hard the unit can manoeuvre - {@code
     * AeroPathUtil.calculateMaxSafeThrust} takes the lower of safe thrust and SI. A fighter can therefore
     * read as undamaged on {@code INTERNAL_P} while being close to dead and unable to turn.</p>
     *
     * @param entity the unit to measure
     *
     * @return the remaining fraction, or an empty string for anything with no structural integrity
     */
    private String structuralIntegrityPercent(Entity entity) {
        if (!(entity instanceof IAero aero) || (aero.getOSI() <= 0)) {
            return "";
        }
        return LOG_DECIMAL.format(Math.max(0, aero.getSI()) / (double) aero.getOSI());
    }

    private void append(String toLog) {
        LOGGER.info(toLog);
    }
}

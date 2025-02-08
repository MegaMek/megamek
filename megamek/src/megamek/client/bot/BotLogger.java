/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.client.bot;

import megamek.client.bot.princess.RankedPath;
import megamek.common.*;
import megamek.common.actions.AbstractAttackAction;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.EntityAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.enums.AimingMode;
import megamek.common.planetaryconditions.PlanetaryConditions;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.StringUtil;
import megamek.logging.MMLogger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * The GameDatasetLogger class is used to log game data to a file in the log directory
 * with TSV format. It contains every action taken by  every unit in the game and the result of the game state after those actions.
 * @author Luana Coppio
 */

public class BotLogger {
    private static final MMLogger logger = MMLogger.create(BotLogger.class);
    public static final String LOG_DIR = PreferenceManager.getClientPreferences().getLogDirectory();
    protected final DecimalFormat LOG_DECIMAL = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance());

    private File logfile;

    private BufferedWriter writer;

    /**
     * Creates Game Dataset Log named
     */
    public BotLogger(String filename) {
        try {
            File logDir = new File(LOG_DIR);
            if (!logDir.exists()) {
                logDir.mkdir();
            }
            if (PreferenceManager.getClientPreferences().stampFilenames()) {
                filename = StringUtil.addDateTimeStamp(filename);
            }
            if (logger.isDebugEnabled()) {
                logfile = new File(LOG_DIR + File.separator + filename + ".tsv");
                writer = new BufferedWriter(new FileWriter(logfile));
            } else {
                writer = null;
            }
            initialize();
        } catch (Exception ex) {
            logger.error("Failure to initialize BotLogger, no log will be persisted", ex);
            writer = null;
        }
    }

    protected void initialize() {
        append("# BotLogger file created at " + LocalDateTime.now());
    }

    public void append(Game game, EntityAction entityAction, boolean withHeader) {
        try {
            if (entityAction instanceof AbstractAttackAction attackAction) {
                String currentRound = game.getCurrentRound() + "";
                String entityId = "-1";
                String playerId = "-1";
                String type;
                String role = UnitRole.NONE.name();
                String coords = "-1\t-1";
                String facing = "-1";
                String targetPlayerId = "-1";
                String targetId = "-1";
                String targetType = "UNKNOWN";
                String targetRole = UnitRole.NONE.name();
                String targetCoords = "-1\t-1";
                String targetFacing = "-1";
                String aimingLoc = "-1";
                String aimingMode = AimingMode.NONE.name();
                String weaponId = "-1";
                String ammoId = "-1";
                String ata = "0";
                String atg = "0";
                String gtg = "0";
                String gta = "0";
                String toHit = "0";
                String turnsToHit = "0";
                String spotterId = "-1";
                var attacker = attackAction.getEntity(game);
                if (attacker != null) {
                    entityId = attacker.getId() + "";
                    playerId = attacker.getOwner().getId() + "";
                    type = attacker.getClass().getSimpleName();
                    role = attacker.getRole() == null ? UnitRole.NONE.name() : attacker.getRole().name();
                    coords = attacker.getPosition() != null ? attacker.getPosition().getX() + "\t" + attacker.getPosition().getY() : "-1\t-1";
                    facing = attacker.getFacing() + "";
                } else {
                    type = "UNKNOWN";
                }
                var target = attackAction.getTarget(game);
                if (target != null ) {
                    targetId = target.getId() + "";
                    targetType = target.getClass().getSimpleName();
                    targetCoords = target.getPosition() != null ? target.getPosition().getX() + "\t" + target.getPosition().getY() : "-1\t-1";
                    if (target instanceof Entity entity) {
                        targetPlayerId = entity.getOwner().getId() + "";
                        targetRole = entity.getRole() == null ? UnitRole.NONE.name() : entity.getRole().name();
                        targetFacing = entity.getFacing() + "";
                    }
                }
                if (attackAction instanceof ArtilleryAttackAction artilleryAttackAction) {
                    if (!artilleryAttackAction.getSpotterIds().isEmpty()) {
                        spotterId = artilleryAttackAction.getSpotterIds().get(0) + "";
                    }
                    turnsToHit = artilleryAttackAction.getTurnsTilHit() + "";
                    toHit = LOG_DECIMAL.format(artilleryAttackAction.toHit(game).getValue());
                    ammoId = artilleryAttackAction.getAmmoId() + "";

                } else if (attackAction instanceof WeaponAttackAction weaponAttackAction) {
                    toHit = LOG_DECIMAL.format(weaponAttackAction.toHit(game).getValue());
                    aimingLoc = weaponAttackAction.getAimedLocation() + "";
                    aimingMode = weaponAttackAction.getAimingMode().name();
                    ammoId = weaponAttackAction.getAmmoId() + "";
                    ata = weaponAttackAction.isAirToAir(game) ? "1" : "0";
                    atg = weaponAttackAction.isAirToGround(game) ? "1" : "0";
                    gta = weaponAttackAction.isGroundToAir(game) ? "1" : "0";
                    gtg = (!weaponAttackAction.isAirToAir(game) && !weaponAttackAction.isGroundToAir(game)
                        && !weaponAttackAction.isAirToGround(game)) ? "1" : "0";
                    weaponId = weaponAttackAction.getWeaponId() + "";
                }
                if (withHeader) {
                    append(
                        String.join(
                            "\t",
                            "ROUND", "PLAYER_ID", "ENTITY_ID", "TYPE", "ROLE", "X", "Y", "FACING",
                            "TARGET_PLAYER_ID", "TARGET_ID", "TARGET_TYPE", "TARGET_ROLE", "TARGET_X", "TARGET_Y", "TARGET_FACING",
                            "AIMING_LOC", "AIMING_MODE", "WEAPON_ID", "AMMO_ID", "ATA", "ATG", "GTG", "GTA", "TO_HIT", "TURNS_TO_HIT", "SPOTTER_ID"
                        )
                    );
                }
                append(
                    String.join(
                        "\t",
                        currentRound, playerId, entityId, type, role, coords, facing, targetPlayerId, targetId, targetType, targetRole, targetCoords, targetFacing,
                        aimingLoc, aimingMode, weaponId, ammoId, ata, atg, gtg, gta, toHit, turnsToHit, spotterId)
                );
            }
        } catch (Exception ex) {
            logger.error(ex, "Error logging entity action");
        }

    }

    /**
     * Appends a game state to the log file
     * @param game
     * @param withHeader
     */
    public void append(Game game, boolean withHeader) {
        try {
            if (withHeader) {
                append(
                    String.join(
                        "\t",
                        "ROUND", "PHASE", "PLAYER_ID", "ENTITY_ID", "CHASSIS", "MODEL" ,"TYPE", "ROLE", "X", "Y", "FACING", "MP",
                        "HEAT", "PRONE", "AIRBORNE", "OFF_BOARD", "CRIPPLED", "DESTROYED", "ARMOR_P", "INTERNAL_P", "DONE"
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
                var coords = entity.getPosition() != null ? entity.getPosition().getX() + "\t" + entity.getPosition().getY() : "-1\t-1";
                var facing = entity.getFacing() + "";
                var mp = entity.getRunMP() > 0 ? LOG_DECIMAL.format(Math.min(1.0, entity.getMpUsedLastRound()/ (double) entity.getRunMP())) : "0.00";
                var isProne = entity.isProne() ? "1" : "0";
                var heatP = entity.getHeatCapacity() > 0 ? LOG_DECIMAL.format(entity.getHeat() / (double) entity.getHeatCapacity()) : "0.00";
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
                        currentRound, gamePhase, ownerID, entityId, chassis, model, type, role, coords, facing, mp, heatP, isProne,
                        isAirborne, isOffBoard, isCrippled, isDestroyed, armorP, internalP, isDone
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
                    append(String.join("\t", currentRound, gamePhase, "MINEFIELD", minefield.getX() + "", minefield.getY() + ""));
                }
            }
        } catch (Exception ex) {
            logger.error(ex, "Error logging entity action");
        }
    }

    /**
     * Appends a move path to the log file
     * @param rankedPath
     * @param withHeader
     */
    public void append(RankedPath rankedPath, boolean withHeader) {
        try {
            var movePath = rankedPath.getPath();
            var rank = rankedPath.getRank() + "";
            var score = rankedPath.getScores();
            var ownerID = movePath.getEntity().getOwner().getId() + "";
            var chassis = movePath.getEntity().getChassis();
            var model = movePath.getEntity().getModel();
            var entityId = movePath.getEntity().getId() + "";
            var from = movePath.getStartCoords() == null ? "-1\t-1" : movePath.getStartCoords().getX() + "\t" + movePath.getStartCoords().getY();
            var to = movePath.getFinalCoords() == null ? "-1\t-1" : movePath.getFinalCoords().getX() + "\t" + movePath.getFinalCoords().getY();
            var hexesMoved = movePath.getHexesMoved() + "";
            var facing = movePath.getFinalFacing() + "";
            var mpUsed = movePath.getMpUsed() + "";
            var maxMp = movePath.getMaxMP() + "";
            var usedPercentMp = movePath.getMaxMP() > 0 ? LOG_DECIMAL.format(movePath.getMpUsed() / (double) movePath.getMaxMP()) : "0.00";
            var heatP = movePath.getEntity().getHeatCapacity() > 0 ? LOG_DECIMAL.format(movePath.getEntity().getHeat() / (double) movePath.getEntity().getHeatCapacity()) : "0.00";
            var distanceTravelled = movePath.getDistanceTravelled() + "";
            var isJumping = movePath.isJumping() ? "1" : "0";
            var isProne = movePath.getFinalProne() ? "1" : "0";
            var isMoveLegal = movePath.isMoveLegal() ? "1" : "0";
            var armor = LOG_DECIMAL.format(Math.max(0, movePath.getEntity().getArmorRemainingPercent()));
            var internal = LOG_DECIMAL.format(Math.max(0, movePath.getEntity().getInternalRemainingPercent()));
            var steps = new StringBuilder();
            movePath.getStepVector().forEach(step -> steps.append(step.toString()).append(" "));

            var header = new ArrayList<>(List.of("PLAYER_ID", "ENTITY_ID", "RANK", "CHASSIS", "MODEL", "FACING", "FROM_X", "FROM_Y", "TO_X", "TO_Y", "HEXES_MOVED", "DISTANCE",
                "MP_USED", "MAX_MP", "MP_P", "HEAT_P", "ARMOR_P", "INTERNAL_P", "JUMPING", "PRONE", "LEGAL", "STEPS"));
            var scoreHeaders = new ArrayList<>(score.keySet());
            scoreHeaders.sort(String::compareTo);
            scoreHeaders.forEach(key -> header.add(key + "_SCORE"));

            if (withHeader) {
                append(
                    String.join(
                        "\t",
                        header
                    )
                );
            }
            var values = new ArrayList<>(List.of(ownerID, entityId, rank, chassis, model, facing, from, to, hexesMoved, distanceTravelled,
                mpUsed, maxMp, usedPercentMp, heatP, armor, internal, isJumping, isProne, isMoveLegal, steps.toString()));
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
            logger.error(ex, "Error logging entity action");
        }
    }

    private void append(String toLog) {
        if (!logger.isDebugEnabled() || (writer == null)) {
            return;
        }
        try {
            writer.write(toLog);
            writer.newLine();
            writer.flush();
        } catch (Exception ex) {
            logger.error("", ex);
            writer = null;
        }
    }

    public void close() throws Exception {
        if (writer != null) {
            writer.close();
        }
    }
}

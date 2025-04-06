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
package megamek.common;

import megamek.common.actions.*;
import megamek.ai.dataset.UnitAction;
import megamek.ai.dataset.UnitActionSerde;
import megamek.ai.dataset.UnitState;
import megamek.ai.dataset.UnitStateSerde;
import megamek.common.enums.AimingMode;
import megamek.common.planetaryconditions.PlanetaryConditions;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.StringUtil;
import megamek.logging.MMLogger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
public class GameDatasetLogger {
    private static final MMLogger logger = MMLogger.create(GameDatasetLogger.class);
    public static final String LOG_DIR = PreferenceManager.getClientPreferences().getLogDirectory();
    private final DecimalFormat LOG_DECIMAL = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance());
    private final UnitActionSerde unitActionSerde = new UnitActionSerde();
    private final UnitStateSerde unitStateSerde = new UnitStateSerde();
    private final String prefix;
    private BufferedWriter writer;
    private boolean createNewFile = true;
    private int counter = 0;
    /**
     * Creates Game Dataset Log named
     */
    public GameDatasetLogger(String prefix) {
        this.prefix = prefix;
        File logDir = new File(LOG_DIR);
        if (!logDir.exists()) {
            if (!logDir.mkdir()) {
                logger.error("Failed to create log directory, GameDatasetLogger wont log anything");
            }
        }
    }

    /**
     * When called, the next log entry will be written to a new file.
     */
    public void requestNewLogFile() {
        createNewFile = true;
        counter++;
    }

    /**
     * Creates a new log file.
     * If there is already a logfile with the same name, it will delete it and create a new one.
     */
    private void newLogFile() {
        try {
            boolean timestampFilenames = PreferenceManager.getClientPreferences().stampFilenames();
            String filename = timestampFilenames ? StringUtil.addDateTimeStamp(prefix) : prefix + "_" + counter;
            File logfile = new File(LOG_DIR + File.separator + filename  + ".tsv");
            // if a file with the same name already exists, delete it.
            if (logfile.exists()) {
                if (!logfile.delete()) {
                    logger.error("Failed to delete existing log file, GameDatasetLogger wont log anything");
                    writer = null;
                    return;
                }
            }
            writer = new BufferedWriter(new FileWriter(logfile));
            initialize();
        } catch (Exception ex) {
            logger.error("", ex);
            writer = null;
        }
    }

    private void initialize() {
        appendToFile("# Log file created at " + LocalDateTime.now());
    }

    /**
     * Append a planetary conditions to the log file
     * @param planetaryConditions the planetary conditions to log
     * @param withHeader  whether to include the header in the log file
     */
    public void append(PlanetaryConditions planetaryConditions, boolean withHeader) {
        try {
            if (planetaryConditions == null) {
                return;
            }
            if (withHeader) {
                appendToFile(
                    String.join(
                        "\t",
                        "TEMPERATURE", "WEATHER", "GRAVITY", "WIND", "ATMOSPHERE", "FOG", "LIGHT"
                    )
                );
            }
            appendToFile(
                String.join(
                    "\t",
                    planetaryConditions.getTemperature() + "",
                    planetaryConditions.getWeather().name(),
                    planetaryConditions.getGravity() + "",
                    planetaryConditions.getWind() + "",
                    planetaryConditions.getAtmosphere() + "",
                    planetaryConditions.getFog() + "",
                    planetaryConditions.getLight() + ""
                )
            );
        } catch (Exception ex) {
            logger.error(ex, "Error logging entity action");
        }
    }

    /**
     * Append a board to the log file
     * @param board the board to log
     * @param withHeader whether to include the header in the log file
     */
    public void append(Board board, boolean withHeader) {
        try {
            if (board == null) {
                return;
            }
            if (withHeader) {
                appendToFile(
                    String.join(
                        "\t",
                        "BOARD_NAME", "WIDTH", "HEIGHT"
                    )
                );
            }
            appendToFile(
                String.join(
                    "\t",
                    board.getBoardName(), board.getWidth() + "", board.getHeight() + ""
                )
            );

            StringBuilder sb = new StringBuilder(1024);
            for (int x = 0; x < board.getWidth(); x++) {
                sb.append("COL_").append(x).append("\t");
            }
            appendToFile(sb.toString());

            List<Hex> lineHexes = new ArrayList<>();
            for (int x = 0; x < board.getWidth(); x++) {
                lineHexes.clear();
                for (int y = 0; y < board.getHeight(); y++) {
                    lineHexes.add(board.getHex(x, y));
                }
                appendToFile("ROW_" + x + "\t" + String.join("\t", lineHexes.stream().map(Hex::toString).toList()));
            }

        } catch (Exception ex) {
            logger.error(ex, "Error logging entity action");
        }
    }

    /**
     * Append a map settings to the log file
     * @param mapSettings the map settings to log
     * @param withHeader whether to include the header in the log file
     */
    public void append(MapSettings mapSettings, boolean withHeader) {
        try {
            if (mapSettings == null) {
                return;
            }
            if (withHeader) {
                appendToFile(
                    String.join(
                        "\t",
                        "THEME", "CITY_TYPE", "MAP_SIZE", "MAP_WIDTH", "MAP_HEIGHT"
                    )
                );
            }
            appendToFile(
                String.join(
                    "\t",
                    mapSettings.getTheme(),
                    mapSettings.getCityType(),
                    mapSettings.getMapWidth() + "",
                    mapSettings.getMapHeight() + ""
                )
            );
        } catch (Exception ex) {
            logger.error(ex, "Error logging entity action");
        }
    }

    /**
     * Append an entity action to the log file
     * @param game the game
     * @param entityAction the entity action to log
     * @param withHeader whether to include the header in the log file
     */
    public void append(Game game, EntityAction entityAction, boolean withHeader) {
        try {
            if (entityAction instanceof AbstractAttackAction abstractAttackAction) {
                append(game, abstractAttackAction, withHeader);
            }
        } catch (Exception ex) {
            logger.error(ex, "Error logging entity action");
        }
    }

    /**
     * Append an attack action to the log file
     * @param game the game
     * @param attackAction the attack action to log
     * @param withHeader whether to include the header in the log file
     */
    private void append(Game game, AbstractAttackAction attackAction, boolean withHeader) {
        // To be changed in the near future by a proper parser for the attack action
        // as soon as it become necessary
        if (attackAction == null) {
            return;
        }
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
            coords = attacker.isDeployed() ? attacker.getPosition().toTSV() : "-1\t-1";
            facing = attacker.getFacing() + "";
        } else {
            type = "UNKNOWN";
        }
        var target = attackAction.getTarget(game);
        if (target != null ) {
            targetId = target.getId() + "";
            targetType = target.getClass().getSimpleName();
            targetCoords = !(target instanceof INarcPod) && target.getPosition() != null ? target.getPosition().toTSV() : "-1\t-1";
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
            appendToFile(
                String.join(
                    "\t",
                    "ROUND", "PLAYER_ID", "ENTITY_ID", "TYPE", "ROLE", "X", "Y", "FACING",
                    "TARGET_PLAYER_ID", "TARGET_ID", "TARGET_TYPE", "TARGET_ROLE", "TARGET_X", "TARGET_Y", "TARGET_FACING",
                    "AIMING_LOC", "AIMING_MODE", "WEAPON_ID", "AMMO_ID", "ATA", "ATG", "GTG", "GTA", "TO_HIT", "TURNS_TO_HIT", "SPOTTER_ID"
                )
            );
        }
        appendToFile(
            String.join(
                "\t",
                currentRound, playerId, entityId, type, role, coords, facing, targetPlayerId, targetId, targetType, targetRole, targetCoords, targetFacing,
                aimingLoc, aimingMode, weaponId, ammoId, ata, atg, gtg, gta, toHit, turnsToHit, spotterId)
        );
    }

    /**
     * Appends a game state to the log file
     * @param game the game
     * @param withHeader whether to include the header in the log file
     */
    public void append(Game game, boolean withHeader) {

        try {
            if (withHeader) {
                appendToFile(unitStateSerde.getHeaderLine());
            }
            for (var inGameObject : game.getInGameObjects()) {
                if (!(inGameObject instanceof Entity entity)) {
                    continue;
                }
                UnitState unitState = UnitState.fromEntity(entity, game);
                appendToFile(unitStateSerde.toTsv(unitState));
            }
        } catch (Exception ex) {
            logger.error("Error logging global Game UnitState", ex);
        }

        try {
            // Example of also logging minefields, which might have different columns
            var minefields = game.getMinedCoords();
            if (minefields != null && minefields.hasMoreElements()) {
                if (withHeader) {
                    // This is optional, or you can define a separate enum if you want a strict schema
                    appendToFile("ROUND\tPHASE\tOBJECT\tX\tY");
                }

                String currentRound = String.valueOf(game.getCurrentRound());
                String gamePhase = game.getPhase().name();

                while (minefields.hasMoreElements()) {
                    var minefield = minefields.nextElement();
                    if (minefield == null) {
                        continue;
                    }
                    // You’d define the line any way you like
                    appendToFile(String.join(
                        "\t",
                        currentRound,
                        gamePhase,
                        "MINEFIELD",
                        String.valueOf(minefield.getX()),
                        String.valueOf(minefield.getY())
                    ));
                }
            }
        } catch (Exception ex) {
            logger.error("Error logging Game UnitState Minefield", ex);
        }
    }

    /**
     * Appends a move path to the log file
     * @param movePath the move path to log
     */
    public void append(MovePath movePath) {
        append(movePath, false);
    }

    /**
     * Appends a move path to the log file
     * @param movePath the move path to log
     * @param withHeader whether to include the header in the log file
     */
    public void append(MovePath movePath, boolean withHeader) {
        try {
            UnitAction unitAction = UnitAction.fromMovePath(movePath);
            if (withHeader) {
                appendToFile(unitActionSerde.getHeaderLine());
            }
            appendToFile(unitActionSerde.toTsv(unitAction));
        } catch (Exception ex) {
            logger.error(ex, "Error logging MovePath unit action");
        }
    }

    /**
     * Appends the text to the log
     * @param toLog the text to log
     */
    private void appendToFile(String toLog) {
        if (!PreferenceManager.getClientPreferences().dataLoggingEnabled()) {
            return;
        }
        if (createNewFile) {
            createNewFile = false;
            newLogFile();
        }
        if (writer == null) {
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

    /**
     * Closes the log file
     * @throws IOException if an error occurs while closing the log file
     */
    public void close() throws IOException {
        if (writer != null) {
            writer.close();
        }
    }
}

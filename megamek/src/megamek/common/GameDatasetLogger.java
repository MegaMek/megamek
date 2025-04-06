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

import megamek.ai.dataset.UnitAttackAction;
import megamek.ai.dataset.UnitAttackSerializer;
import megamek.common.actions.*;
import megamek.ai.dataset.UnitAction;
import megamek.ai.dataset.UnitActionSerializer;
import megamek.ai.dataset.UnitState;
import megamek.ai.dataset.UnitStateSerializer;
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
import java.util.Locale;

/**
 * The GameDatasetLogger class is used to log game data to a file in the log directory
 * with TSV format. It contains every action taken by  every unit in the game and the result of the game state after those actions.
 * @author Luana Coppio
 */
public class GameDatasetLogger {
    private static final MMLogger logger = MMLogger.create(GameDatasetLogger.class);
    public static final String LOG_DIR = PreferenceManager.getClientPreferences().getLogDirectory();
    private final DecimalFormat LOG_DECIMAL = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.US));
    private final UnitActionSerializer unitActionSerde = new UnitActionSerializer();
    private final UnitStateSerializer unitStateSerde = new UnitStateSerializer();
    private final UnitAttackSerializer unitAttackSerializer = new UnitAttackSerializer();
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
            logger.error("An error happened while setting up a new log file, writer is being shut down", ex);
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
                    board.getMapName(), board.getWidth() + "", board.getHeight() + ""
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
        try {
            UnitAttackAction unitAttackAction = UnitAttackAction.fromAttackAction(attackAction, game);
            if (withHeader) {
                appendToFile(unitAttackSerializer.getHeaderLine());
            }
            appendToFile(unitAttackSerializer.serialize(unitAttackAction));
        } catch (Exception ex) {
            logger.error(ex, "Error logging Attack unit action");
        }
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
                appendToFile(unitStateSerde.serialize(unitState));
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
            appendToFile(unitActionSerde.serialize(unitAction));
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

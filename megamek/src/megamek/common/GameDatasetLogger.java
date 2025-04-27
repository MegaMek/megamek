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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;

import megamek.SuiteConstants;
import megamek.Version;
import megamek.ai.dataset.*;
import megamek.common.actions.AbstractAttackAction;
import megamek.common.actions.EntityAction;
import megamek.common.planetaryconditions.PlanetaryConditions;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.StringUtil;
import megamek.logging.MMLogger;

/**
 * The GameDatasetLogger class is used to log game data to a file in the log directory
 * with TSV format. It contains every action taken by  every unit in the game and the result of the game state after those actions.
 * @author Luana Coppio
 */
public class GameDatasetLogger {
    private static final MMLogger logger = MMLogger.create(GameDatasetLogger.class);

    public static final String LOG_DIR = PreferenceManager.getClientPreferences().getLogDirectory();
    private static final Version VERSION = new Version("1.0.0");

    private final UnitActionSerializer unitActionSerializer = new UnitActionSerializer();
    private final UnitAttackSerializer unitAttackSerializer = new UnitAttackSerializer();
    private final BoardDataSerializer boardDataSerializer = new BoardDataSerializer();
    private final MapSettingsDataSerializer mapSettingsDataSerializer = new MapSettingsDataSerializer();
    private final PlanetaryConditionsDataSerializer planetaryConditionsDataSerializer = new PlanetaryConditionsDataSerializer();
    private final GameDataSerializer gameDataSerializer = new GameDataSerializer();

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
        appendToFile("# Log file created at " + LocalDateTime.now() + " MegaMek Version " + SuiteConstants.VERSION);
        appendToFile("# GameDatasetLogger - " + VERSION);
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

            PlanetaryConditionsData data = PlanetaryConditionsData.fromPlanetaryConditions(planetaryConditions);

            if (withHeader) {
                appendToFile(planetaryConditionsDataSerializer.getHeaderLine());
            }

            appendToFile(planetaryConditionsDataSerializer.serialize(data));
        } catch (Exception ex) {
            logger.error("Error logging planetary conditions", ex);
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

            BoardData data = BoardData.fromBoard(board);
            String lines = boardDataSerializer.serialize(data);
            appendToFile(lines);
            // Write all lines from the board serializer
            // maybe its not necessary to split by new line
//            for (String line : lines.split("\n")) {
//                appendToFile(line);
//            }

        } catch (Exception ex) {
            logger.error("Error logging board", ex);
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

            MapSettingsData data = MapSettingsData.fromMapSettings(mapSettings);

            if (withHeader) {
                appendToFile(mapSettingsDataSerializer.getHeaderLine());
            }

            appendToFile(mapSettingsDataSerializer.serialize(data));

        } catch (Exception ex) {
            logger.error("Error logging map settings", ex);
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
            UnitAttack unitAttackAction = UnitAttack.fromAttackAction(attackAction, game);
            if (withHeader) {
                appendToFile(unitAttackSerializer.getHeaderLine());
            }
            appendToFile(unitAttackSerializer.serialize(unitAttackAction));
        } catch (Exception ex) {
            logger.error(ex, "Error logging Attack unit action");
        }
    }


    /**
     * Appends a game state to the log file.
     * @param game The game
     * @param withHeader Whether to include the header in the log file
     */
    public void append(Game game, boolean withHeader) {
        try {
            GameData gameData = GameData.fromGame(game);
            String lines = gameDataSerializer.serialize(gameData);
            appendToFile(lines);
        } catch (Exception ex) {
            logger.error("Error logging game state", ex);
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
                appendToFile(unitActionSerializer.getHeaderLine());
            }
            appendToFile(unitActionSerializer.serialize(unitAction));
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

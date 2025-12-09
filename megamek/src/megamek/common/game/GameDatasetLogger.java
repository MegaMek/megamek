/*
 * Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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

package megamek.common.game;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Map;

import megamek.ai.dataset.*;
import megamek.common.Configuration;
import megamek.common.actions.AbstractAttackAction;
import megamek.common.actions.EntityAction;
import megamek.common.board.Board;
import megamek.common.loaders.MapSettings;
import megamek.common.moves.MovePath;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.preference.PreferenceManager;
import megamek.logging.MMLogger;

/**
 * The GameDatasetLogger class is used to log game data to separate TSV files in the log directory.
 * Each file type contains a specific category of data for easier parsing and analysis:
 * <ul>
 *   <li>board - Board terrain, map settings, and planetary conditions (static game setup)</li>
 *   <li>actions - Unit movement actions (UnitAction records)</li>
 *   <li>states - Game state snapshots (UnitState records)</li>
 *   <li>attacks - Attack actions (UnitAttack records)</li>
 * </ul>
 *
 * @author Luana Coppio
 */
public class GameDatasetLogger {
    private static final MMLogger logger = MMLogger.create(GameDatasetLogger.class);
    // TSV files are written to the same directory as GIF minimap summaries
    public static final String LOG_DIR = Configuration.gameSummaryImagesMMDir().getAbsolutePath();
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * Enum defining the different log file types.
     */
    public enum LogFileType {
        BOARD("board"),
        ACTIONS("actions"),
        STATES("states"),
        ATTACKS("attacks");

        private final String suffix;

        LogFileType(String suffix) {
            this.suffix = suffix;
        }

        public String getSuffix() {
            return suffix;
        }
    }

    private final UnitActionSerializer unitActionSerializer = new UnitActionSerializer();
    private final UnitAttackSerializer unitAttackSerializer = new UnitAttackSerializer();
    private final BoardDataSerializer boardDataSerializer = new BoardDataSerializer();
    private final MapSettingsDataSerializer mapSettingsDataSerializer = new MapSettingsDataSerializer();
    private final PlanetaryConditionsDataSerializer planetaryConditionsDataSerializer = new PlanetaryConditionsDataSerializer();
    private final UnitStateSerializer unitStateSerializer = new UnitStateSerializer();

    private final String prefix;
    private final Map<LogFileType, BufferedWriter> writers = new EnumMap<>(LogFileType.class);
    private final Map<LogFileType, Boolean> headerWritten = new EnumMap<>(LogFileType.class);
    private boolean createNewFiles = true;
    private String nextTimestamp = null;
    private String currentTimestamp = null;

    /**
     * Creates Game Dataset Logger with the given prefix.
     *
     * @param prefix the prefix for log file names
     */
    public GameDatasetLogger(String prefix) {
        this.prefix = prefix;
        File logDir = new File(LOG_DIR);
        if (!logDir.exists()) {
            if (!logDir.mkdirs()) {
                logger.error("Failed to create log directory, GameDatasetLogger wont log anything");
            }
        }
        // Initialize header tracking
        for (LogFileType type : LogFileType.values()) {
            headerWritten.put(type, false);
        }
    }

    /**
     * When called, the next log entry will be written to new files.
     */
    public void requestNewLogFile() {
        createNewFiles = true;
        nextTimestamp = null;
        // Reset header tracking for new files
        for (LogFileType type : LogFileType.values()) {
            headerWritten.put(type, false);
        }
    }

    /**
     * Sets the timestamp to use for the next log files.
     * This allows the TSV files to use the same timestamp as the game's GIF summary.
     *
     * @param timestamp the timestamp string (format: yyyyMMdd_HHmmss)
     */
    public void setNextTimestamp(String timestamp) {
        this.nextTimestamp = timestamp;
    }

    /**
     * Creates new log files with a unique timestamp-based filename.
     * Each game will get its own set of TSV files.
     */
    private void createNewLogFiles() {
        closeAllWriters();
        currentTimestamp = (nextTimestamp != null) ? nextTimestamp : LocalDateTime.now().format(TIMESTAMP_FORMAT);

        for (LogFileType type : LogFileType.values()) {
            try {
                String filename = prefix + "_" + type.getSuffix() + "_" + currentTimestamp + ".tsv";
                File logfile = new File(LOG_DIR + File.separator + filename);
                BufferedWriter writer = new BufferedWriter(new FileWriter(logfile));
                writers.put(type, writer);
                // Write file header comment
                writer.write("# " + type.name() + " log file created at " + LocalDateTime.now());
                writer.newLine();
                writer.flush();
                headerWritten.put(type, false);
            } catch (Exception ex) {
                logger.error("Failed to create log file for type " + type, ex);
                writers.put(type, null);
            }
        }
    }

    /**
     * Closes all open writers.
     */
    private void closeAllWriters() {
        for (LogFileType type : LogFileType.values()) {
            BufferedWriter writer = writers.get(type);
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ex) {
                    logger.error("Error closing writer for " + type, ex);
                }
            }
        }
        writers.clear();
    }

    /**
     * Append planetary conditions to the board log file.
     *
     * @param planetaryConditions the planetary conditions to log
     * @param withHeader          whether to include the header in the log file (ignored, headers auto-managed)
     */
    public void append(PlanetaryConditions planetaryConditions, boolean withHeader) {
        try {
            if (planetaryConditions == null) {
                return;
            }

            PlanetaryConditionsData data = PlanetaryConditionsData.fromPlanetaryConditions(planetaryConditions);

            if (!headerWritten.get(LogFileType.BOARD)) {
                appendToFile(LogFileType.BOARD, planetaryConditionsDataSerializer.getHeaderLine());
            }

            appendToFile(LogFileType.BOARD, planetaryConditionsDataSerializer.serialize(data));
        } catch (Exception ex) {
            logger.error("Error logging planetary conditions", ex);
        }
    }

    /**
     * Append board data to the board log file.
     *
     * @param board      the board to log
     * @param withHeader whether to include the header in the log file (ignored, headers auto-managed)
     */
    public void append(Board board, boolean withHeader) {
        try {
            if (board == null) {
                return;
            }

            BoardData data = BoardData.fromBoard(board);
            String lines = boardDataSerializer.serialize(data);
            appendToFile(LogFileType.BOARD, lines);
        } catch (Exception ex) {
            logger.error("Error logging board", ex);
        }
    }

    /**
     * Append map settings to the board log file.
     *
     * @param mapSettings the map settings to log
     * @param withHeader  whether to include the header in the log file (ignored, headers auto-managed)
     */
    public void append(MapSettings mapSettings, boolean withHeader) {
        try {
            if (mapSettings == null) {
                return;
            }

            MapSettingsData data = MapSettingsData.fromMapSettings(mapSettings);
            appendToFile(LogFileType.BOARD, mapSettingsDataSerializer.getHeaderLine());
            appendToFile(LogFileType.BOARD, mapSettingsDataSerializer.serialize(data));
        } catch (Exception ex) {
            logger.error("Error logging map settings", ex);
        }
    }

    /**
     * Append an entity action to the appropriate log file.
     *
     * @param game         the game
     * @param entityAction the entity action to log
     * @param withHeader   whether to include the header in the log file (ignored, headers auto-managed)
     */
    public void append(Game game, EntityAction entityAction, boolean withHeader) {
        try {
            if (entityAction instanceof AbstractAttackAction abstractAttackAction) {
                appendAttack(game, abstractAttackAction);
            }
        } catch (Exception ex) {
            logger.error(ex, "Error logging entity action");
        }
    }

    /**
     * Append an attack action to the attacks log file.
     *
     * @param game         the game
     * @param attackAction the attack action to log
     */
    private void appendAttack(Game game, AbstractAttackAction attackAction) {
        if (attackAction == null) {
            return;
        }
        try {
            UnitAttack unitAttackAction = UnitAttack.fromAttackAction(attackAction, game);

            if (!headerWritten.get(LogFileType.ATTACKS)) {
                appendToFile(LogFileType.ATTACKS, unitAttackSerializer.getHeaderLine());
                headerWritten.put(LogFileType.ATTACKS, true);
            }

            appendToFile(LogFileType.ATTACKS, unitAttackSerializer.serialize(unitAttackAction));
        } catch (Exception ex) {
            logger.error(ex, "Error logging Attack unit action");
        }
    }

    /**
     * Appends game state (all unit states) to the states log file.
     *
     * @param game       The game
     * @param withHeader Whether to include the header in the log file (ignored, headers auto-managed)
     */
    public void append(Game game, boolean withHeader) {
        try {
            GameData gameData = GameData.fromGame(game);

            // Write header if not already written
            if (!headerWritten.get(LogFileType.STATES)) {
                appendToFile(LogFileType.STATES, unitStateSerializer.getHeaderLine());
                headerWritten.put(LogFileType.STATES, true);
            }

            // Write each unit state as a separate line
            for (UnitState unitState : gameData.getUnitStates()) {
                appendToFile(LogFileType.STATES, unitStateSerializer.serialize(unitState));
            }
        } catch (Exception ex) {
            logger.error("Error logging game state", ex);
        }
    }

    /**
     * Appends a move path to the actions log file.
     *
     * @param movePath the move path to log
     */
    public void append(MovePath movePath) {
        append(movePath, false);
    }

    /**
     * Appends a move path to the actions log file.
     *
     * @param movePath   the move path to log
     * @param withHeader whether to include the header in the log file (ignored, headers auto-managed)
     */
    public void append(MovePath movePath, boolean withHeader) {
        try {
            UnitAction unitAction = UnitAction.fromMovePath(movePath);

            if (!headerWritten.get(LogFileType.ACTIONS)) {
                appendToFile(LogFileType.ACTIONS, unitActionSerializer.getHeaderLine());
                headerWritten.put(LogFileType.ACTIONS, true);
            }

            appendToFile(LogFileType.ACTIONS, unitActionSerializer.serialize(unitAction));
        } catch (Exception ex) {
            logger.error(ex, "Error logging MovePath unit action");
        }
    }

    /**
     * Appends the text to the specified log file.
     *
     * @param fileType the type of log file to write to
     * @param toLog    the text to log
     */
    private void appendToFile(LogFileType fileType, String toLog) {
        if (!PreferenceManager.getClientPreferences().dataLoggingEnabled()) {
            return;
        }
        if (createNewFiles) {
            createNewFiles = false;
            createNewLogFiles();
        }
        BufferedWriter writer = writers.get(fileType);
        if (writer == null) {
            return;
        }
        try {
            writer.write(toLog);
            writer.newLine();
            writer.flush();
        } catch (Exception ex) {
            logger.error("Error writing to " + fileType + " log", ex);
            writers.put(fileType, null);
        }
    }

    /**
     * Closes all log files.
     *
     * @throws IOException if an error occurs while closing the log files
     */
    public void close() throws IOException {
        closeAllWriters();
    }

    /**
     * Gets the current timestamp being used for log files.
     *
     * @return the current timestamp string, or null if no files have been created yet
     */
    public String getCurrentTimestamp() {
        return currentTimestamp;
    }
}

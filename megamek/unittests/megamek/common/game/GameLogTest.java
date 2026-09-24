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

package megamek.common.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.jupiter.api.Test;

class GameLogTest {

    /**
     * {@link GameLog#close()} used to close its writer but leave the field pointing at it, so the {@code null}
     * guards in {@link GameLog#append(String)} and {@link GameLog#appendRaw(String)} never held afterwards.
     * Shutdown writes the closing HTML tag through {@code appendRaw} and runs more than once, so every game
     * ended by logging an {@code IOException: Stream closed} (issue #8937). A write after close must now be a
     * silent no-op.
     */
    @Test
    void writingAfterCloseLogsNothingAndChangesNothing() throws Exception {
        GameLog gameLog = new GameLog("gameLogTest-afterClose.html");
        File logFile = gameLog.getLogFile();
        try {
            gameLog.appendRaw("<BODY>");
            gameLog.close();
            String contentAtClose = Files.readString(logFile.toPath(), StandardCharsets.UTF_8);

            String captured = captureErrors(() -> {
                gameLog.appendRaw("</BODY></HTML>");
                gameLog.append("a second shutdown pass");
            });

            assertEquals("", captured, "writing to a closed game log must not log an error");
            assertEquals(contentAtClose, Files.readString(logFile.toPath(), StandardCharsets.UTF_8));
        } finally {
            deleteQuietly(logFile);
        }
    }

    /**
     * Shutdown can close the log more than once. That must stay silent too.
     */
    @Test
    void closingTwiceLogsNothing() throws Exception {
        GameLog gameLog = new GameLog("gameLogTest-doubleClose.html");
        File logFile = gameLog.getLogFile();
        try {
            gameLog.append("one line");
            gameLog.close();

            String captured = captureErrors(gameLog::close);

            assertEquals("", captured, "closing a game log twice must not log an error");
        } finally {
            deleteQuietly(logFile);
        }
    }

    /**
     * A log that is still open keeps writing, so the guard has not been left permanently armed.
     */
    @Test
    void writingBeforeCloseStillReachesTheFile() throws Exception {
        GameLog gameLog = new GameLog("gameLogTest-beforeClose.html");
        File logFile = gameLog.getLogFile();
        try {
            gameLog.appendRaw("a line that must be written");
            assertTrue(Files.readString(logFile.toPath(), StandardCharsets.UTF_8)
                  .contains("a line that must be written"));
        } finally {
            closeQuietly(gameLog);
            deleteQuietly(logFile);
        }
    }

    /** Runs the action with an appender attached to GameLog's logger and returns everything it logged. */
    private String captureErrors(ThrowingAction action) throws Exception {
        CapturingAppender appender = new CapturingAppender();
        Logger gameLogLogger = (Logger) LogManager.getLogger(GameLog.class);
        Level originalLevel = gameLogLogger.getLevel();
        gameLogLogger.addAppender(appender);
        gameLogLogger.setLevel(Level.ERROR);
        appender.start();
        try {
            action.run();
        } finally {
            gameLogLogger.removeAppender(appender);
            gameLogLogger.setLevel(originalLevel);
            appender.stop();
        }
        return appender.message;
    }

    /**
     * Closes the log without letting a close failure mask an assertion failure from the test body, or stop the
     * log file from being deleted.
     */
    private void closeQuietly(GameLog gameLog) {
        try {
            gameLog.close();
        } catch (Exception exception) {
            // Nothing useful to do in cleanup; the assertions above are what the test is reporting on.
        }
    }

    private void deleteQuietly(File file) {
        if ((file != null) && file.exists() && !file.delete()) {
            file.deleteOnExit();
        }
    }

    @FunctionalInterface
    private interface ThrowingAction {
        void run() throws Exception;
    }

    private static final class CapturingAppender extends AbstractAppender {
        private String message = "";

        private CapturingAppender() {
            super("GameLogTest", null, PatternLayout.createDefaultLayout(), false, null);
        }

        @Override
        public void append(LogEvent event) {
            message += event.getMessage().getFormattedMessage() + event.getLevel();
        }
    }
}

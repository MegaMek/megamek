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

package megamek.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests the bug report archive's file selection.
 *
 * <p>These assert on the archive's actual contents rather than on log output, because the behaviour that matters is
 * "the maintainer receives the right files, and an attachable number of them".</p>
 */
class BugReportBundleTest {

    private static final String SYSTEM_INFORMATION = "Starting MegaMek v0.51.01\n    Java Version: 21.0.5\n";
    private static final String GAME_LOG_FILE_NAME = "gamelog.html";

    @TempDir
    private Path temporaryDirectory;

    private File logDirectory;
    private File archiveFile;

    @BeforeEach
    void setUp() throws IOException {
        logDirectory = temporaryDirectory.resolve("logs").toFile();
        assertTrue(logDirectory.mkdirs(), "test fixture: could not create the log directory");
        archiveFile = temporaryDirectory.resolve("bug-report.zip").toFile();
    }

    @Test
    void emptyLogDirectoryStillProducesAnArchiveWithSystemInformation() throws IOException {
        BugReportBundle bundle = new BugReportBundle(logDirectory, null, SYSTEM_INFORMATION, GAME_LOG_FILE_NAME);

        BugReportBundle.BundleResult result = bundle.writeTo(archiveFile);

        assertTrue(archiveFile.isFile(), "an archive should be written even with nothing to collect");
        assertEquals(List.of(BugReportBundle.SYSTEM_INFO_ENTRY_NAME), entryNamesOf(archiveFile));
        assertFalse(result.hasSkippedEntries());
    }

    @Test
    void missingLogDirectoryIsToleratedRatherThanFailing() throws IOException {
        File absentDirectory = temporaryDirectory.resolve("no-such-directory").toFile();
        BugReportBundle bundle = new BugReportBundle(absentDirectory, null, SYSTEM_INFORMATION, GAME_LOG_FILE_NAME);

        bundle.writeTo(archiveFile);

        assertEquals(List.of(BugReportBundle.SYSTEM_INFO_ENTRY_NAME), entryNamesOf(archiveFile));
    }

    @Test
    void collectsTheWantedLogsAndIgnoresTheClutterAroundThem() throws IOException {
        writeLog("megamek.log", "engine log");
        writeLog("unified_log.log", "unified log");
        writeLog("princess.log", "bot log");
        // The log directory also accumulates a unit list per bot per game; none of these belong in a bug report.
        for (int index = 0; index < 40; index++) {
            writeLog("Bot_Clan Jade Falcon OpFor_%d.mul".formatted(index), "unit list");
        }

        BugReportBundle bundle = new BugReportBundle(logDirectory, null, SYSTEM_INFORMATION, GAME_LOG_FILE_NAME);
        bundle.writeTo(archiveFile);

        List<String> entryNames = entryNamesOf(archiveFile);
        assertTrue(entryNames.contains("logs/megamek.log"));
        assertTrue(entryNames.contains("logs/unified_log.log"));
        assertTrue(entryNames.contains("logs/princess.log"));
        assertTrue(entryNames.stream().noneMatch(name -> name.endsWith(".mul")),
              "bot unit lists should never be collected: " + entryNames);
    }

    @Test
    void collectsOnlyTheNewestCombatReport() throws IOException {
        File oldestReport = writeLog("gamelog_2026-07-21_10-29-35.html", "round reports");
        File middleReport = writeLog("gamelog_2026-07-21_11-16-49.html", "round reports");
        File newestReport = writeLog("gamelog_2026-07-21_12-09-27.html", "round reports");
        assertTrue(oldestReport.setLastModified(1_000_000L), "test fixture: could not set a modification time");
        assertTrue(middleReport.setLastModified(2_000_000L), "test fixture: could not set a modification time");
        assertTrue(newestReport.setLastModified(3_000_000L), "test fixture: could not set a modification time");

        BugReportBundle bundle = new BugReportBundle(logDirectory, null, SYSTEM_INFORMATION, GAME_LOG_FILE_NAME);
        bundle.writeTo(archiveFile);

        List<String> combatReports = entryNamesOf(archiveFile).stream()
              .filter(name -> name.endsWith(".html"))
              .toList();
        assertEquals(List.of("logs/" + newestReport.getName()), combatReports);
    }

    @Test
    void placesTheSaveGameAtTheArchiveRootRatherThanUnderLogs() throws IOException {
        File saveGame = temporaryDirectory.resolve("savegame.sav.gz").toFile();
        Files.writeString(saveGame.toPath(), "serialized game");
        writeLog("megamek.log", "engine log");

        BugReportBundle bundle = new BugReportBundle(logDirectory, saveGame, SYSTEM_INFORMATION, GAME_LOG_FILE_NAME);
        bundle.writeTo(archiveFile);

        List<String> entryNames = entryNamesOf(archiveFile);
        assertTrue(entryNames.contains("savegame.sav.gz"),
              "the save should sit at the archive root: " + entryNames);
        assertTrue(entryNames.contains("logs/megamek.log"),
              "logs should sit under logs/: " + entryNames);
    }

    @Test
    void skipsAnOversizedLogAndNamesItInsteadOfFailing() throws IOException {
        writeLog("megamek.log", "engine log");
        File oversizedLog = new File(logDirectory, "princess.log");
        setFileLength(oversizedLog, BugReportBundle.MAX_ARCHIVE_BYTES + 1);

        BugReportBundle bundle = new BugReportBundle(logDirectory, null, SYSTEM_INFORMATION, GAME_LOG_FILE_NAME);
        BugReportBundle.BundleResult result = bundle.writeTo(archiveFile);

        assertTrue(result.hasSkippedEntries());
        assertTrue(result.skippedEntries().contains("princess.log"),
              "the oversized log should be named: " + result.skippedEntries());
        assertTrue(entryNamesOf(archiveFile).contains("logs/megamek.log"),
              "the smaller logs should still be collected");
        assertTrue(result.totalBytes() < BugReportBundle.MAX_ARCHIVE_BYTES);
    }

    /**
     * Creates a log file with the given contents.
     *
     * @param fileName the name to create inside the log directory
     * @param contents the text to write
     *
     * @return the created file
     */
    private File writeLog(String fileName, String contents) throws IOException {
        File logFile = new File(logDirectory, fileName);
        Files.writeString(logFile.toPath(), contents, StandardCharsets.UTF_8);
        return logFile;
    }

    /**
     * Creates a file that reports the given length without writing that many bytes, so the size-cap test does not
     * have to produce 25 MB of real data.
     *
     * @param file   the file to create
     * @param length the length the file should report
     */
    private static void setFileLength(File file, long length) throws IOException {
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw")) {
            randomAccessFile.setLength(length);
        }
    }

    /**
     * @param archive the archive to inspect
     *
     * @return every entry path inside the archive, in the order written
     */
    private static List<String> entryNamesOf(File archive) throws IOException {
        List<String> entryNames = new ArrayList<>();
        try (ZipFile zipFile = new ZipFile(archive)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                entryNames.add(entries.nextElement().getName());
            }
        }
        return entryNames;
    }
}

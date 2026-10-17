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
        BugReportBundle bundle = newBundle(null);

        BugReportBundle.BundleResult result = bundle.writeTo(archiveFile);

        assertTrue(archiveFile.isFile(), "an archive should be written even with nothing to collect");
        assertEquals(List.of(BugReportBundle.SYSTEM_INFO_ENTRY_NAME), entryNamesOf(archiveFile));
        assertFalse(result.hasSkippedEntries());
    }

    @Test
    void missingLogDirectoryIsToleratedRatherThanFailing() throws IOException {
        File absentDirectory = temporaryDirectory.resolve("no-such-directory").toFile();
        BugReportBundle bundle = new BugReportBundle(absentDirectory, absentDirectory, null, SYSTEM_INFORMATION,
              GAME_LOG_FILE_NAME);

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

        BugReportBundle bundle = newBundle(null);
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

        BugReportBundle bundle = newBundle(null);
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

        BugReportBundle bundle = newBundle(saveGame);
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

        BugReportBundle bundle = newBundle(null);
        BugReportBundle.BundleResult result = bundle.writeTo(archiveFile);

        assertTrue(result.hasSkippedEntries());
        assertTrue(result.skippedEntries().contains("princess.log"),
              "the oversized log should be named: " + result.skippedEntries());
        assertTrue(entryNamesOf(archiveFile).contains("logs/megamek.log"),
              "the smaller logs should still be collected");
        assertTrue(result.totalBytes() < BugReportBundle.MAX_ARCHIVE_BYTES);
    }

    @Test
    void collectsEverySuiteLogInPriorityOrder() throws IOException {
        // Written out of order, so the test proves the archive order comes from the manifest, not from the disk.
        writeLog("megameklab.log", "lab log");
        writeLog("bot_path_ranker.log", "path scores");
        writeLog("princess.log", "bot log");
        writeLog("unified_log.log", "unified log");
        writeLog("mekhq.log", "campaign log");
        writeLog("megamek.log", "engine log");

        newBundle(null).writeTo(archiveFile);

        assertEquals(List.of(BugReportBundle.SYSTEM_INFO_ENTRY_NAME,
              "logs/megamek.log",
              "logs/mekhq.log",
              "logs/unified_log.log",
              "logs/princess.log",
              "logs/bot_path_ranker.log",
              "logs/megameklab.log"), entryNamesOf(archiveFile));
    }

    @Test
    void skipsTheEmptyLogsOfProgramsThatDidNotRun() throws IOException {
        // MegaMek's own log configuration declares the MekHQ and MegaMekLab logs but routes nothing to them, so a
        // standalone MegaMek leaves both files present and empty.
        writeLog("megamek.log", "engine log");
        writeLog("mekhq.log", "");
        writeLog("megameklab.log", "");

        newBundle(null).writeTo(archiveFile);

        assertEquals(List.of(BugReportBundle.SYSTEM_INFO_ENTRY_NAME, "logs/megamek.log"),
              entryNamesOf(archiveFile));
    }

    @Test
    void collectsRolledLogsForEveryCollectedLogAndNothingElse() throws IOException {
        writeLog("megamek_1.log.gz", "rolled engine log");
        writeLog("mekhq_1.log.gz", "rolled campaign log");
        writeLog("princess_1.log.gz", "rolled bot log");
        writeLog("megameklab_1.log.gz", "rolled lab log");
        writeLog("forcegen_1.log.gz", "an unrelated archive");

        newBundle(null).writeTo(archiveFile);

        List<String> entryNames = entryNamesOf(archiveFile);
        assertTrue(entryNames.contains("logs/megamek_1.log.gz"), entryNames.toString());
        assertTrue(entryNames.contains("logs/mekhq_1.log.gz"), entryNames.toString());
        assertTrue(entryNames.contains("logs/princess_1.log.gz"), entryNames.toString());
        assertTrue(entryNames.contains("logs/megameklab_1.log.gz"), entryNames.toString());
        assertFalse(entryNames.contains("logs/forcegen_1.log.gz"),
              "an archive that is not a rolled suite log should be left out: " + entryNames);
    }

    @Test
    void collectsOnlyTheNewestGameDatasetAfterTheCombatReport() throws IOException {
        File olderDataset = writeLog("game_actions_0.tsv", "moves and attacks");
        File newerDataset = writeLog("game_actions_1.tsv", "moves and attacks");
        assertTrue(olderDataset.setLastModified(1_000_000L), "test fixture: could not set a modification time");
        assertTrue(newerDataset.setLastModified(2_000_000L), "test fixture: could not set a modification time");
        // Other tab-separated files, such as benchmark results, are not part of a bug report.
        writeLog("ai_match_results_2026-09-25_23-52-10_game1_orders.tsv", "benchmark rows");
        writeLog(GAME_LOG_FILE_NAME, "round reports");
        writeLog("megamek_1.log.gz", "rolled engine log");

        newBundle(null).writeTo(archiveFile);

        assertEquals(List.of(BugReportBundle.SYSTEM_INFO_ENTRY_NAME,
              "logs/" + GAME_LOG_FILE_NAME,
              "logs/game_actions_1.tsv",
              "logs/megamek_1.log.gz"), entryNamesOf(archiveFile));
    }

    @Test
    void readsCombatReportsFromTheRelocatedReportDirectory() throws IOException {
        // The player moved the combat reports elsewhere; Log4j still writes the text logs to the usual folder.
        File reportDirectory = temporaryDirectory.resolve("reports").toFile();
        assertTrue(reportDirectory.mkdirs(), "test fixture: could not create the report directory");
        Files.writeString(new File(reportDirectory, GAME_LOG_FILE_NAME).toPath(), "round reports");
        writeLog("megamek.log", "engine log");

        new BugReportBundle(logDirectory, reportDirectory, null, SYSTEM_INFORMATION, GAME_LOG_FILE_NAME)
              .writeTo(archiveFile);

        List<String> entryNames = entryNamesOf(archiveFile);
        assertTrue(entryNames.contains("logs/megamek.log"),
              "the text logs should still be collected from the log directory: " + entryNames);
        assertTrue(entryNames.contains("logs/" + GAME_LOG_FILE_NAME),
              "the combat report should be found in the report directory: " + entryNames);
    }

    /**
     * @param saveGame the save to include, or {@code null} for none
     *
     * @return a bundle reading both the text logs and the combat reports from the test's log directory
     */
    private BugReportBundle newBundle(File saveGame) {
        return new BugReportBundle(logDirectory, logDirectory, saveGame, SYSTEM_INFORMATION, GAME_LOG_FILE_NAME);
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

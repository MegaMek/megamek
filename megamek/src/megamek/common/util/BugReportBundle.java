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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import jakarta.annotation.Nullable;
import megamek.logging.MMLogger;

/**
 * Collects the files a MegaMek bug report needs and writes them into a single zip archive that the player can attach
 * to a GitHub issue.
 *
 * <p>The file list is a deliberate manifest rather than a directory scan. MegaMek's log directory also accumulates
 * one {@code gamelog*.html} combat report per game and a {@code Bot_*.mul} unit list per bot per game, so a working
 * installation can hold hundreds of megabytes there. Sweeping the directory wholesale would produce an archive far
 * too large to attach, while a naive {@code *.log} filter would miss the combat report, which is usually the single
 * most useful artifact in a MegaMek bug report.</p>
 *
 * <p>The text logs and the combat reports can live in different directories. Log4j always writes the text logs to
 * {@value #TEXT_LOG_DIRECTORY} under the working directory, as set in {@code mmconf/log4j2.xml}, while the combat
 * reports follow the player's log directory preference. The two are the same folder unless the player has relocated
 * the reports.</p>
 *
 * <p>The archive is capped at {@link #MAX_ARCHIVE_BYTES}. Candidates are added in priority order and any that would
 * push the total past the cap are skipped and named in the returned {@link BundleResult}, so the caller can tell the
 * player exactly what was left out instead of silently truncating.</p>
 *
 * <p>This class deliberately has no Swing or game-state dependencies so that it can be exercised headlessly.</p>
 *
 * @see megamek.common.util.IssueReportUrl
 */
public class BugReportBundle {
    private static final MMLogger LOGGER = MMLogger.create(BugReportBundle.class);

    /** GitHub refuses issue attachments larger than 25 MB, whatever the file type. */
    public static final long MAX_ARCHIVE_BYTES = 25L * 1024L * 1024L;

    /** The archive entry holding the version, OS and Java details. */
    public static final String SYSTEM_INFO_ENTRY_NAME = "system-info.txt";

    /** Archive sub-directory holding the collected logs, matching the layout MekHQ's bug report bundle uses. */
    private static final String LOG_ENTRY_PREFIX = "logs/";

    /**
     * The directory Log4j writes the text logs to, relative to the working directory. Both MegaMek's and MekHQ's
     * {@code mmconf/log4j2.xml} use it, so it holds MekHQ's logs too when MegaMek runs inside MekHQ.
     */
    public static final String TEXT_LOG_DIRECTORY = "logs";

    /**
     * Log basenames always worth collecting, most important first. {@code mekhq.log} holds the campaign side of a
     * game launched from MekHQ; when MegaMek runs on its own the file is empty and is skipped.
     */
    private static final List<String> ESSENTIAL_LOG_BASE_NAMES = List.of("megamek", "mekhq", "unified_log");

    /** Log basenames written only when a bot ran, most important first. */
    private static final List<String> BOT_LOG_BASE_NAMES = List.of("princess", "bot_path_ranker");

    /** Log basenames from the other suite programs, rarely part of a game's story, so collected last. */
    private static final List<String> COMPANION_LOG_BASE_NAMES = List.of("megameklab");

    /**
     * Every basename this bundle collects, in priority order. Precomputed because the rolled-log filter is applied
     * once per file in the log directory, which can hold thousands.
     */
    private static final List<String> ALL_LOG_BASE_NAMES =
          Stream.of(ESSENTIAL_LOG_BASE_NAMES, BOT_LOG_BASE_NAMES, COMPANION_LOG_BASE_NAMES)
                .flatMap(List::stream)
                .toList();

    /**
     * The name the server's game dataset log starts with, followed by a game counter or a timestamp. Written to the
     * combat-report directory, one file per game, only when the dataset logging preference is on.
     *
     * @see megamek.common.game.GameDatasetLogger
     */
    private static final String GAME_DATASET_BASE_NAME = "game_actions";
    private static final String GAME_DATASET_EXTENSION = ".tsv";

    /**
     * Archive sub-directory for a game's minimap images, matching their layout on disk so the folder name still
     * carries the game's UUID.
     */
    private static final String GAME_SUMMARY_ENTRY_PREFIX = LOG_ENTRY_PREFIX + "gameSummaries/minimap/";
    private static final String GAME_SUMMARY_FRAME_EXTENSION = ".png";

    /** Archive sub-directory for the battle autosave, shared with MekHQ's bug report bundle. */
    private static final String AUTOSAVE_ENTRY_PREFIX = "battle-autosave/";

    /**
     * Every server autosave has this in its name: {@code autosave.sav.gz}, a timestamped {@code autosave<stamp>} and
     * the rolling {@code Round-<n>-autosave<stamp>}.
     */
    private static final String AUTOSAVE_NAME_MARKER = "autosave";
    private static final String SAVE_GAME_EXTENSION = ".sav.gz";
    private static final String GAME_SUMMARY_RECORDING_EXTENSION = ".gif";

    private static final String LOG_EXTENSION = ".log";
    private static final String ROLLED_LOG_EXTENSION = ".log.gz";
    private static final int COPY_BUFFER_SIZE = 8192;

    private final File logDirectory;
    private final File gameReportDirectory;
    private final File autosaveDirectory;
    private final @Nullable File saveGameFile;
    private final @Nullable File gameSummaryDirectory;
    private final String systemInformation;
    private final @Nullable String gameLogFileName;

    /**
     * The outcome of writing a bug report archive.
     *
     * @param archiveFile     the archive that was written
     * @param includedEntries archive-relative paths of everything that made it in, in the order written
     * @param skippedEntries  names of candidate files omitted because they would have breached the size cap; empty
     *                        when everything fitted
     * @param totalBytes      the uncompressed total of every included file
     */
    public record BundleResult(File archiveFile, List<String> includedEntries, List<String> skippedEntries,
                               long totalBytes) {

        /** @return {@code true} if at least one candidate file was omitted because of the size cap */
        public boolean hasSkippedEntries() {
            return !skippedEntries.isEmpty();
        }
    }

    /**
     * Where a bug report's files come from. Not serialized; it only carries the inputs into the bundle.
     *
     * @param logDirectory         the directory holding the text logs, normally {@value #TEXT_LOG_DIRECTORY}. A
     *                             directory that does not exist is tolerated and yields a logs-free archive.
     * @param gameReportDirectory  the directory holding the combat reports; read from
     *                             {@code ClientPreferences.getLogDirectory()} rather than assumed, because players
     *                             can relocate it
     * @param autosaveDirectory    the directory the server writes its autosaves to, normally
     *                             {@code MMConstants.SAVEGAME_DIR}
     * @param saveGameFile         the saved game to place at the archive root, or {@code null} when no game was
     *                             running (the main-menu and startup-crash cases)
     * @param systemInformation    the version, OS and Java details to write to {@value #SYSTEM_INFO_ENTRY_NAME}
     * @param gameLogFileName      the configured combat-report filename, normally {@code gamelog.html}, used to find
     *                             the newest matching report; or {@code null} to collect no combat report
     * @param gameSummaryDirectory the current game's minimap image folder, normally
     *                             {@code gameSummaries/minimap/<game UUID>}, holding a PNG per phase and the GIF
     *                             recording; or {@code null} to collect none. A folder that does not exist (both
     *                             options off) is skipped.
     */
    public record Sources(File logDirectory, File gameReportDirectory, File autosaveDirectory,
          @Nullable File saveGameFile, String systemInformation, @Nullable String gameLogFileName,
          @Nullable File gameSummaryDirectory) {}

    /**
     * Creates a bundle description. Nothing is read from disk until {@link #writeTo(File)} is called.
     *
     * @param sources where the report's files come from
     */
    public BugReportBundle(Sources sources) {
        this.logDirectory = sources.logDirectory();
        this.gameReportDirectory = sources.gameReportDirectory();
        this.autosaveDirectory = sources.autosaveDirectory();
        this.saveGameFile = sources.saveGameFile();
        this.gameSummaryDirectory = sources.gameSummaryDirectory();
        this.systemInformation = sources.systemInformation();
        this.gameLogFileName = sources.gameLogFileName();
    }

    /**
     * Writes the archive, adding candidates in priority order until the size cap is reached.
     *
     * <p>Reaching the cap is not an error: remaining candidates are recorded as skipped and the archive is still
     * completed, because a partial bundle is far more useful to a maintainer than none.</p>
     *
     * @param archiveFile the zip file to create, overwriting any existing file at that path
     *
     * @return a description of what was written
     *
     * @throws IOException if the archive itself cannot be created or written
     */
    public BundleResult writeTo(File archiveFile) throws IOException {
        List<String> includedEntries = new ArrayList<>();
        List<String> skippedEntries = new ArrayList<>();
        long totalBytes = 0;

        try (OutputStream fileOutputStream = new FileOutputStream(archiveFile);
              ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream)) {

            // The system information is tiny and always wanted, so it is written first and never counted against
            // the cap; a bundle that omitted it would be of little use.
            byte[] systemInformationBytes = systemInformation.getBytes(StandardCharsets.UTF_8);
            writeEntry(zipOutputStream, SYSTEM_INFO_ENTRY_NAME, systemInformationBytes);
            includedEntries.add(SYSTEM_INFO_ENTRY_NAME);
            totalBytes += systemInformationBytes.length;

            for (CandidateFile candidate : gatherCandidates()) {
                long candidateLength = candidate.source().length();
                if ((totalBytes + candidateLength) > MAX_ARCHIVE_BYTES) {
                    skippedEntries.add(candidate.source().getName());
                    continue;
                }
                copyFileToArchive(candidate.source(), candidate.entryName(), zipOutputStream);
                includedEntries.add(candidate.entryName());
                totalBytes += candidateLength;
            }
        }

        LOGGER.info("[BugReport] Archive written to {}: {} entries included, {} skipped, {} bytes uncompressed",
              archiveFile.getName(), includedEntries.size(), skippedEntries.size(), totalBytes);
        if (!skippedEntries.isEmpty()) {
            LOGGER.warn("[BugReport] Omitted {} file(s) to stay under the {} byte attachment limit: {}",
                  skippedEntries.size(), MAX_ARCHIVE_BYTES, String.join(", ", skippedEntries));
        }

        return new BundleResult(archiveFile, List.copyOf(includedEntries), List.copyOf(skippedEntries), totalBytes);
    }

    /** One file destined for the archive, paired with the path it should occupy inside it. */
    private record CandidateFile(File source, String entryName) {}

    /**
     * Builds the ordered manifest. Order is load-bearing: everything after the point where the cap is reached is
     * dropped, so the most diagnostically valuable files come first.
     *
     * @return the candidate files, in the order they should be added
     */
    private List<CandidateFile> gatherCandidates() {
        List<CandidateFile> candidates = new ArrayList<>();

        // 1. The save game, at the archive root. A MegaMek save is a complete serialization of the game, so this
        // alone lets a maintainer reproduce the exact board and units, custom designs included.
        if ((saveGameFile != null) && saveGameFile.isFile()) {
            candidates.add(new CandidateFile(saveGameFile, saveGameFile.getName()));
        }

        // 1b. The newest autosave. With no running game it is the only save in the report; with one, it is the
        // state from the start of the round, before whatever went wrong. Saves are small, so it sits up here.
        File autosave = newestAutosave();
        if (autosave != null) {
            candidates.add(new CandidateFile(autosave, AUTOSAVE_ENTRY_PREFIX + autosave.getName()));
        }

        boolean hasLogDirectory = logDirectory.isDirectory();
        if (!hasLogDirectory) {
            LOGGER.warn("[BugReport] Log directory {} does not exist; the archive will contain no text logs",
                  logDirectory.getPath());
        }

        // 2. The current logs, which carry the gate decisions explaining why a feature did or did not fire. Files
        // the running program never writes to are left empty by Log4j and skipped here.
        if (hasLogDirectory) {
            for (String baseName : ALL_LOG_BASE_NAMES) {
                addIfPresent(candidates, new File(logDirectory, baseName + LOG_EXTENSION));
            }
        }

        // 3. The newest combat report only. These run to several megabytes each and a busy log directory holds
        // hundreds of them, so collecting them all would blow the attachment limit on its own.
        newestGameLog().ifPresent(gameLog -> candidates.add(
              new CandidateFile(gameLog, LOG_ENTRY_PREFIX + gameLog.getName())));

        // 4. The newest game dataset log, for the same reason. It exists only when dataset logging is switched on.
        newestGameDataset().ifPresent(dataset -> candidates.add(
              new CandidateFile(dataset, LOG_ENTRY_PREFIX + dataset.getName())));

        // 5. Rolled archives last, newest first: useful when the interesting event scrolled out of the live log,
        // but the first thing worth dropping when space runs short.
        if (hasLogDirectory) {
            for (File rolledLog : rolledLogsNewestFirst()) {
                candidates.add(new CandidateFile(rolledLog, LOG_ENTRY_PREFIX + rolledLog.getName()));
            }
        }

        // 6. The current game's minimap images last, so the cap drops them before any log: they show the board at
        // each phase, but the save already holds the same game. The named PNGs come newest first, so the cap drops
        // the oldest rounds; the GIF holds the same frames without phase names, so it is dropped first of all.
        if ((gameSummaryDirectory != null) && gameSummaryDirectory.isDirectory()) {
            String entryPrefix = GAME_SUMMARY_ENTRY_PREFIX + gameSummaryDirectory.getName() + "/";
            for (File frame : gameSummaryFilesNewestFirst(GAME_SUMMARY_FRAME_EXTENSION)) {
                candidates.add(new CandidateFile(frame, entryPrefix + frame.getName()));
            }
            for (File recording : gameSummaryFilesNewestFirst(GAME_SUMMARY_RECORDING_EXTENSION)) {
                candidates.add(new CandidateFile(recording, entryPrefix + recording.getName()));
            }
        }

        LOGGER.info("[BugReport] Collecting text logs from {} and combat reports from {}: {} candidate file(s)",
              logDirectory.getPath(), gameReportDirectory.getPath(), candidates.size());
        return candidates;
    }

    /** Adds a log file to the manifest if it exists and holds anything worth reading. */
    private void addIfPresent(List<CandidateFile> candidates, File logFile) {
        if (logFile.isFile() && (logFile.length() > 0)) {
            candidates.add(new CandidateFile(logFile, LOG_ENTRY_PREFIX + logFile.getName()));
        }
    }

    /**
     * Finds the most recently modified combat report.
     *
     * <p>The configured name is normally {@code gamelog.html}, but when the client's filename stamping preference is
     * on, MegaMek writes {@code gamelog_2026-07-21_11-16-49.html} instead. Matching is therefore done on the
     * basename as a prefix rather than on an exact filename.</p>
     *
     * @return the newest matching report, or empty when none exists or no report name is configured
     */
    private Optional<File> newestGameLog() {
        if ((gameLogFileName == null) || gameLogFileName.isBlank()) {
            return Optional.empty();
        }

        int extensionIndex = gameLogFileName.lastIndexOf('.');
        String baseName = (extensionIndex > 0) ? gameLogFileName.substring(0, extensionIndex) : gameLogFileName;

        return newestReport(baseName, "");
    }

    /**
     * Finds the most recently written game dataset log. It records every move and attack of a game as tab-separated
     * rows, so it can show what a bot did turn by turn when the save alone cannot.
     *
     * @return the newest dataset log, or empty when dataset logging was never switched on
     */
    private Optional<File> newestGameDataset() {
        return newestReport(GAME_DATASET_BASE_NAME + "_", GAME_DATASET_EXTENSION);
    }

    /**
     * Finds the most recently modified non-empty file in the combat-report directory with the given name prefix and
     * suffix.
     *
     * @param prefix the name the file must start with
     * @param suffix the name the file must end with, or an empty string for any
     *
     * @return the newest match, or empty when there is none
     */
    private Optional<File> newestReport(String prefix, String suffix) {
        File[] reports = gameReportDirectory.listFiles(file -> file.isFile()
              && (file.length() > 0)
              && file.getName().startsWith(prefix)
              && file.getName().endsWith(suffix));
        if ((reports == null) || (reports.length == 0)) {
            return Optional.empty();
        }

        return Arrays.stream(reports).max(Comparator.comparingLong(File::lastModified));
    }

    /**
     * Finds the most recently written server autosave.
     *
     * @return the newest non-empty autosave, or {@code null} when there is none
     */
    private @Nullable File newestAutosave() {
        File[] saves = autosaveDirectory.listFiles(file -> file.isFile()
              && (file.length() > 0)
              && file.getName().toLowerCase(Locale.ROOT).contains(AUTOSAVE_NAME_MARKER)
              && file.getName().toLowerCase(Locale.ROOT).endsWith(SAVE_GAME_EXTENSION));
        if ((saves == null) || (saves.length == 0)) {
            return null;
        }

        File newestSave = saves[0];
        for (File save : saves) {
            if (save.lastModified() > newestSave.lastModified()) {
                newestSave = save;
            }
        }
        return newestSave;
    }

    /**
     * Lists the non-empty files in the game summary folder with the given extension, newest first.
     *
     * @param extension the extension to match, such as {@code .png}
     *
     * @return the matching files, newest first
     */
    private List<File> gameSummaryFilesNewestFirst(String extension) {
        File[] files = gameSummaryDirectory.listFiles(file -> file.isFile()
              && (file.length() > 0)
              && file.getName().toLowerCase(Locale.ROOT).endsWith(extension));
        if (files == null) {
            return List.of();
        }

        List<File> sortedFiles = new ArrayList<>(List.of(files));
        sortedFiles.sort(Comparator.comparingLong(File::lastModified).reversed());
        return sortedFiles;
    }

    /**
     * Finds the most recently changed game summary folder, for when there is no current game to name one. A game
     * that crashed or was abandoned leaves its folder behind, and that is often the game the report is about.
     *
     * @param minimapDirectory the folder holding one sub-folder per game, normally {@code gameSummaries/minimap}
     *
     * @return the newest game folder, or {@code null} when there is none
     */
    public static @Nullable File newestGameSummaryDirectory(File minimapDirectory) {
        File[] gameDirectories = minimapDirectory.listFiles(File::isDirectory);
        if ((gameDirectories == null) || (gameDirectories.length == 0)) {
            return null;
        }

        File newestDirectory = gameDirectories[0];
        for (File gameDirectory : gameDirectories) {
            if (gameDirectory.lastModified() > newestDirectory.lastModified()) {
                newestDirectory = gameDirectory;
            }
        }
        return newestDirectory;
    }

    /**
     * Collects the rolled, compressed log archives for every basename in the manifest, newest first.
     *
     * @return the rolled logs, newest first; empty when none have been rolled yet
     */
    private List<File> rolledLogsNewestFirst() {
        File[] rolledLogs = logDirectory.listFiles(BugReportBundle::isCollectableRolledLog);

        if (rolledLogs == null) {
            return List.of();
        }

        return Arrays.stream(rolledLogs)
              .sorted(Comparator.comparingLong(File::lastModified).reversed())
              .toList();
    }

    /**
     * Whether a file is a rolled archive of one of the logs this bundle collects.
     *
     * <p>Log4j rolls {@code megamek.log} to {@code megamek_1.log.gz} and so on, so a rolled archive is recognised by
     * the basename, an underscore, and the compressed extension. This excludes the unrelated compressed files that
     * also accumulate in the log directory.</p>
     *
     * @param file the file to test
     *
     * @return {@code true} if the file is a rolled archive of a collected log
     */
    private static boolean isCollectableRolledLog(File file) {
        if (!file.isFile() || !file.getName().endsWith(ROLLED_LOG_EXTENSION)) {
            return false;
        }
        return ALL_LOG_BASE_NAMES.stream().anyMatch(baseName -> file.getName().startsWith(baseName + "_"));
    }

    /**
     * Writes an in-memory entry to the archive.
     *
     * @param zipOutputStream the open archive
     * @param entryName       the path the entry should occupy inside the archive
     * @param content         the bytes to write
     *
     * @throws IOException if the entry cannot be written
     */
    private static void writeEntry(ZipOutputStream zipOutputStream, String entryName, byte[] content)
          throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(entryName));
        zipOutputStream.write(content);
        zipOutputStream.closeEntry();
    }

    /**
     * Streams a file into the archive.
     *
     * <p>A file that vanishes or becomes unreadable between the manifest being built and this call is logged and
     * skipped rather than aborting the archive; log files are written to continuously and may be rolled away
     * underneath us.</p>
     *
     * @param source          the file to read
     * @param entryName       the path the file should occupy inside the archive
     * @param zipOutputStream the open archive
     *
     * @throws IOException if the archive itself cannot be written
     */
    private static void copyFileToArchive(File source, String entryName, ZipOutputStream zipOutputStream)
          throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(entryName));
        try (InputStream fileInputStream = new FileInputStream(source)) {
            byte[] buffer = new byte[COPY_BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                zipOutputStream.write(buffer, 0, bytesRead);
            }
        } catch (IOException exception) {
            LOGGER.warn(exception, "[BugReport] Could not read {}; it will be empty in the archive", source.getName());
        }
        zipOutputStream.closeEntry();
    }
}

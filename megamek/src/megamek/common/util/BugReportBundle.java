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

    /** Log basenames always worth collecting, most important first. */
    private static final List<String> ESSENTIAL_LOG_BASE_NAMES = List.of("megamek", "unified_log");

    /** Log basenames collected only when a bot actually ran, most important first. */
    private static final List<String> BOT_LOG_BASE_NAMES = List.of("princess", "bot_path_ranker");

    /**
     * Every basename this bundle collects. Precomputed because the rolled-log filter is applied once per file in the
     * log directory, which can hold thousands.
     */
    private static final List<String> ALL_LOG_BASE_NAMES =
          Stream.concat(ESSENTIAL_LOG_BASE_NAMES.stream(), BOT_LOG_BASE_NAMES.stream()).toList();

    private static final String LOG_EXTENSION = ".log";
    private static final String ROLLED_LOG_EXTENSION = ".log.gz";
    private static final int COPY_BUFFER_SIZE = 8192;

    private final File logDirectory;
    private final File saveGameFile;
    private final String systemInformation;
    private final String gameLogFileName;

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
     * Creates a bundle description. Nothing is read from disk until {@link #writeTo(File)} is called.
     *
     * @param logDirectory      the directory holding MegaMek's logs; read from
     *                          {@code ClientPreferences.getLogDirectory()} rather than assumed, because players can
     *                          relocate it. A directory that does not exist is tolerated and yields a logs-free
     *                          archive.
     * @param saveGameFile      the saved game to place at the archive root, or {@code null} when no game was running
     *                          (the main-menu and startup-crash cases)
     * @param systemInformation the version, OS and Java details to write to {@value #SYSTEM_INFO_ENTRY_NAME}
     * @param gameLogFileName   the configured combat-report filename, normally {@code gamelog.html}, used to find the
     *                          newest matching report; or {@code null} to collect no combat report
     */
    public BugReportBundle(File logDirectory, @Nullable File saveGameFile, String systemInformation,
          @Nullable String gameLogFileName) {
        this.logDirectory = logDirectory;
        this.saveGameFile = saveGameFile;
        this.systemInformation = systemInformation;
        this.gameLogFileName = gameLogFileName;
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

        if (!logDirectory.isDirectory()) {
            LOGGER.warn("[BugReport] Log directory {} does not exist; the archive will contain no logs",
                  logDirectory.getPath());
            return candidates;
        }

        // 2. The current logs, which carry the gate decisions explaining why a feature did or did not fire.
        for (String baseName : ESSENTIAL_LOG_BASE_NAMES) {
            addIfPresent(candidates, new File(logDirectory, baseName + LOG_EXTENSION));
        }
        for (String baseName : BOT_LOG_BASE_NAMES) {
            addIfPresent(candidates, new File(logDirectory, baseName + LOG_EXTENSION));
        }

        // 3. The newest combat report only. These run to several megabytes each and a busy log directory holds
        // hundreds of them, so collecting them all would blow the attachment limit on its own.
        newestGameLog().ifPresent(gameLog -> candidates.add(
              new CandidateFile(gameLog, LOG_ENTRY_PREFIX + gameLog.getName())));

        // 4. Rolled archives last, newest first: useful when the interesting event scrolled out of the live log,
        // but the first thing worth dropping when space runs short.
        for (File rolledLog : rolledLogsNewestFirst()) {
            candidates.add(new CandidateFile(rolledLog, LOG_ENTRY_PREFIX + rolledLog.getName()));
        }

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

        File[] gameLogs = logDirectory.listFiles(file -> file.isFile() && file.getName().startsWith(baseName));
        if ((gameLogs == null) || (gameLogs.length == 0)) {
            return Optional.empty();
        }

        return Arrays.stream(gameLogs).max(Comparator.comparingLong(File::lastModified));
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

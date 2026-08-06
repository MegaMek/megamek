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
package megamek.utilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Year;
import java.util.Comparator;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import megamek.codeUtilities.MathUtility;
import megamek.common.Configuration;
import megamek.common.annotations.Nullable;
import megamek.common.loaders.BLKFile;
import megamek.common.loaders.EntitySavingException;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.util.BuildingBlock;
import megamek.logging.MMLogger;

/**
 * Author: Drake
 * <p>
 * Loads all canon units and resaves them to an output folder, preserving relative paths. Injects a license header into
 * each file.
 * <p>
 * Usage from the command line (run from the megamek root directory):
 * <p>
 * Default output to data/mekfiles_resaved: ./gradlew :megamek:resaveUnits
 * <p>
 * Custom output folder: ./gradlew :megamek:resaveUnits -PresaveOutputDir="C:/path/to/output"
 */
public class UnitFileResaver {
    private static final MMLogger logger = MMLogger.create(UnitFileResaver.class);

    private static final String LICENSE_HEADER_TEMPLATE = """
          # MegaMek Data (C) %s by The MegaMek Team is licensed under CC BY-NC-SA 4.0.
          # To view a copy of this license, visit https://creativecommons.org/licenses/by-nc-sa/4.0/
          #
          # NOTICE: The MegaMek organization is a non-profit group of volunteers
          # creating free software for the BattleTech community.
          #
          # MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
          # of The Topps Company, Inc. All Rights Reserved.
          #
          # Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
          # InMediaRes Productions, LLC.
          #
          # MechWarrior Copyright Microsoft Corporation. MegaMek Data was created under
          # Microsoft's "Game Content Usage Rules"
          # <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
          # affiliated with Microsoft.
          """;

    /** Matches the year, or the first year of a year range, in an existing license header. */
    private static final Pattern COPYRIGHT_YEARS_PATTERN =
          Pattern.compile("MegaMek Data \\(C\\) (\\d{4})(?:-(\\d{4}))? by");

    /** How far into a source file to look for the license header before giving up. */
    private static final int HEADER_SEARCH_LINE_LIMIT = 20;

    private static final int CURRENT_YEAR = Year.now().getValue();

    public static void main(String... args) {
        Path outputDir;
        if (args.length > 0) {
            outputDir = Path.of(args[0]);
        } else {
            // Default: adjacent to the mekfiles folder
            outputDir = Configuration.unitsDir().toPath().getParent().resolve("mekfiles_resaved");
        }

        logger.info("Output directory: {}", outputDir.toAbsolutePath());

        // Clean output directory if it exists
        if (Files.exists(outputDir)) {
            logger.info("Cleaning existing output directory...");
            try (var walk = Files.walk(outputDir)) {
                walk.sorted(Comparator.reverseOrder())
                      .forEach(path -> {
                          try {
                              Files.deleteIfExists(path);
                          } catch (IOException e) {
                              logger.warn("Failed to delete: {}", path);
                          }
                      });
            } catch (IOException e) {
                logger.error("Failed to clean output directory: {}", e.getMessage());
                System.exit(1);
            }
        }

        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            logger.error("Failed to create output directory: {}", e.getMessage());
            System.exit(1);
        }

        // Load all units
        MekSummaryCache cache = MekSummaryCache.getInstance(true);
        MekSummary[] units = cache.getAllMeks();
        logger.info("Found {} units to resave.", units.length);

        AtomicInteger saved = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        AtomicInteger skipped = new AtomicInteger(0);

        for (MekSummary summary : units) {
            Entity entity;
            try {
                entity = summary.loadEntity();
            } catch (Exception e) {
                logger.error("Failed to load entity {}: {}", summary.getName(), e.getMessage());
                failed.incrementAndGet();
                continue;
            }

            if (entity == null) {
                skipped.incrementAndGet();
                continue;
            }

            // Resolve the relative path for this unit
            Path relativePath = getUnitSourceRelativePath(summary);
            if (relativePath == null) {
                String extension = (entity instanceof Mek) ? ".mtf" : ".blk";
                String safeName = summary.getName().replaceAll("[^a-zA-Z0-9_\\-. ]", "_");
                relativePath = Path.of(safeName + extension);
            }

            Path normalizedPath = relativePath.normalize();
            if (normalizedPath.isAbsolute() || normalizedPath.startsWith("..")) {
                normalizedPath = Path.of(normalizedPath.getFileName().toString());
            }

            File outputFile = outputDir.resolve(normalizedPath).toFile();

            // Create parent directories
            File parent = outputFile.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                logger.error("Failed to create directory: {}", parent.getPath());
                failed.incrementAndGet();
                continue;
            }

            try {
                saveUnit(outputFile, summary.getSourceFile(), entity);
                saved.incrementAndGet();
            } catch (Exception e) {
                logger.error("Failed to save {}: {}", summary.getName(), e.getMessage());
                failed.incrementAndGet();
            }
        }

        logger.info("Done. Saved: {}, Failed: {}, Skipped: {}", saved.get(), failed.get(), skipped.get());
    }

    /**
     * Saves an entity to a file with the license header prepended.
     *
     * @param outputFile The file to write the unit to
     * @param sourceFile The file the unit was loaded from, used to carry its copyright years forward, or
     *                   {@code null} when the source is unknown
     * @param entity     The unit to save
     */
    private static void saveUnit(File outputFile, @Nullable File sourceFile, Entity entity)
          throws IOException, EntitySavingException {
        String licenseHeader = LICENSE_HEADER_TEMPLATE.formatted(copyrightYears(sourceFile));

        if (entity instanceof Mek mek) {
            try (FileOutputStream outputStream = new FileOutputStream(outputFile);
                  PrintStream printStream = new PrintStream(outputStream, false, StandardCharsets.UTF_8)) {
                printStream.println(licenseHeader);
                printStream.print(mek.getMtf());
            }
        } else {
            BuildingBlock buildingBlock = BLKFile.getBlock(entity);
            try (FileOutputStream outputStream = new FileOutputStream(outputFile);
                  OutputStreamWriter streamWriter = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
                  BufferedWriter bufferedWriter = new BufferedWriter(streamWriter)) {
                bufferedWriter.write(licenseHeader);
                bufferedWriter.newLine();
                for (String line : buildingBlock.getAllDataAsString()) {
                    bufferedWriter.write(line);
                    bufferedWriter.newLine();
                }
                bufferedWriter.flush();
            }
        }
    }

    /**
     * Works out the copyright years for a resaved file. A file that already carries an earlier year keeps it as the
     * start of a range, so resaving an existing unit does not erase the year its content was first published. A file
     * with no readable header, or one already stamped with the current year, gets the current year on its own.
     *
     * <p>Units loaded from inside an archive have no readable plain-text header, so they fall back to the current
     * year.</p>
     *
     * @param sourceFile The file the unit was loaded from, or {@code null} when the source is unknown
     *
     * @return The year, or year range, to write into the license header
     */
    // Package-private so the year-carrying behavior can be tested directly.
    static String copyrightYears(@Nullable File sourceFile) {
        int startYear = readCopyrightStartYear(sourceFile);
        if ((startYear > 0) && (startYear < CURRENT_YEAR)) {
            return startYear + "-" + CURRENT_YEAR;
        }
        return String.valueOf(CURRENT_YEAR);
    }

    /**
     * Reads the first copyright year out of a source file's existing license header.
     *
     * <p>Only the plain-text unit formats are opened. A unit stored in an archive reports the archive as its source
     * file, and reading that as text would scan a large binary file for a header it cannot contain.</p>
     *
     * @param sourceFile The file to read, or {@code null} when the source is unknown
     *
     * @return The first copyright year in the header, or 0 if the file has no readable header
     */
    private static int readCopyrightStartYear(@Nullable File sourceFile) {
        if ((sourceFile == null) || !sourceFile.isFile() || !hasPlainTextUnitExtension(sourceFile)) {
            return 0;
        }

        try (BufferedReader reader = Files.newBufferedReader(sourceFile.toPath(), StandardCharsets.UTF_8)) {
            for (int lineNumber = 0; lineNumber < HEADER_SEARCH_LINE_LIMIT; lineNumber++) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                Matcher matcher = COPYRIGHT_YEARS_PATTERN.matcher(line);
                if (matcher.find()) {
                    return MathUtility.parseInt(matcher.group(1), 0);
                }
            }
        } catch (IOException exception) {
            // A file that cannot be read as UTF-8 text simply has no header to carry forward
            logger.debug("Could not read copyright year from {}: {}", sourceFile.getPath(), exception.getMessage());
        }

        return 0;
    }

    /**
     * Checks whether a file is one of the plain-text unit formats that carry a license header.
     *
     * @param sourceFile The file to check
     *
     * @return {@code true} if the file is a format whose header can be read as text
     */
    private static boolean hasPlainTextUnitExtension(File sourceFile) {
        String fileName = sourceFile.getName().toLowerCase(Locale.ROOT);
        return fileName.endsWith(".mtf") || fileName.endsWith(".blk");
    }

    /**
     * Resolves the relative path of a unit's source file within the mekfiles directory.
     */
    private static @Nullable Path getUnitSourceRelativePath(MekSummary mekSummary) {
        String entryName = mekSummary.getEntryName();
        if (entryName != null && !entryName.isBlank()) {
            return Path.of(entryName.replace('\\', '/'));
        }

        File sourceFile = mekSummary.getSourceFile();
        if (sourceFile == null) {
            return null;
        }

        try {
            Path sourcePath = sourceFile.toPath().toAbsolutePath().normalize();
            Path unitsRoot = Configuration.unitsDir().toPath().toAbsolutePath().normalize();
            if (sourcePath.startsWith(unitsRoot)) {
                return unitsRoot.relativize(sourcePath);
            }
        } catch (Exception e) {
            logger.warn("Failed to resolve source path for {}", mekSummary.getName());
        }

        return Path.of(sourceFile.getName());
    }

    private UnitFileResaver() {
        // utility class
    }
}

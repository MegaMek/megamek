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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;

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

    private static final String LICENSE_HEADER = """
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
          """.formatted(Calendar.getInstance().get(Calendar.YEAR));

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
                saveUnit(outputFile, entity);
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
     */
    private static void saveUnit(File outputFile, Entity entity) throws IOException, EntitySavingException {
        if (entity instanceof Mek mek) {
            try (FileOutputStream fos = new FileOutputStream(outputFile);
                  PrintStream ps = new PrintStream(fos, false, StandardCharsets.UTF_8)) {
                ps.println(LICENSE_HEADER);
                ps.print(mek.getMtf());
            }
        } else {
            BuildingBlock blk = BLKFile.getBlock(entity);
            try (FileOutputStream fos = new FileOutputStream(outputFile);
                  OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
                  BufferedWriter bw = new BufferedWriter(osw)) {
                bw.write(LICENSE_HEADER);
                bw.newLine();
                for (String line : blk.getAllDataAsString()) {
                    bw.write(line);
                    bw.newLine();
                }
                bw.flush();
            }
        }
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

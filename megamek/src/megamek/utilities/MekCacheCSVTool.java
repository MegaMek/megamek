/*
  Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 * Copyright © 2013 Nicholas Walczak (walczak@cs.umn.edu)
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import megamek.codeUtilities.StringUtility;
import megamek.common.TechConstants;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.ArmorType;
import megamek.common.equipment.EquipmentType;
import megamek.common.loaders.MekFileParser;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.templates.TROView;
import megamek.common.units.Aero;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import megamek.common.units.System;
import megamek.logging.MMLogger;

/**
 * This class provides a utility to read in all the /data/mekfiles and print that data out into a CSV format.
 *
 * @author arlith
 * @author Simon (Juliez)
 */
public final class MekCacheCSVTool {
    private static final MMLogger logger = MMLogger.create(MekCacheCSVTool.class);

    // Excel import works better with the .txt extension instead of .csv
    private static final String FILE_NAME = "Units.txt";
    private static final String DELIM = "|";
    private static boolean includeGunEmplacement = false; // Variable to control inclusion of Gun Emplacement units

    private static final String NOT_APPLICABLE = "Not Applicable";

    private static final List<String> HEADERS = List.of("Chassis", "Model", "MUL ID", "Combined", "Source",
          "Tech Base", "File Location", "File Modified", "Weight", "Intro Date", "Experimental year", "Advanced year",
          "Standard year", "Extinct Year", "Unit Type", "Omni", "Role", "BV", "Cost", "Rules", "Engine Name",
          "Internal Structure", "Myomer", "Cockpit Type", "Gyro Type", "Armor Types", "Equipment", "Tech Rating",
          "Unit Quirks", "Weapon Quirks", "Manufacturer", "Factory", "Targeting", "Comms", "Armor", "JJ", "Engine",
          "Chassis", "Capabilities", "Overview", "History", "Deployment", "Notes", "Fluff Date");

    public static void main(String... args) {
        if (args.length > 0) {
            includeGunEmplacement = Boolean.parseBoolean(args[0]);
        }

        try (PrintWriter pw = new PrintWriter(FILE_NAME);
              BufferedWriter bw = new BufferedWriter(pw)) {
            MekSummaryCache cache = MekSummaryCache.getInstance(true);
            MekSummary[] units = cache.getAllMeks();

            StringBuilder csvLine = new StringBuilder();
            csvLine.append(String.join(DELIM, HEADERS)).append("\n");
            bw.write(csvLine.toString());

            for (MekSummary unit : units) {
                if (!includeGunEmplacement && unit.getUnitType().equals("Gun Emplacement")) {
                    continue;
                }

                csvLine = new StringBuilder();
                csvLine.append(unit.getFullChassis()).append(DELIM);
                csvLine.append(unit.getModel()).append(DELIM);
                csvLine.append(unit.getMulId()).append(DELIM);
                csvLine.append(unit.getFullChassis()).append(" ").append(unit.getModel()).append(DELIM);
                csvLine.append(unit.getSource()).append(DELIM);
                csvLine.append(unit.getTechBase()).append(DELIM);
                csvLine.append(unit.getSourceFile()).append(DELIM);
                csvLine.append(getFileModifiedDate(unit.getSourceFile(), unit.getEntryName())).append(DELIM);
                csvLine.append(unit.getTons()).append(DELIM);
                csvLine.append(unit.getYear()).append(DELIM);

                // Experimental Tech Year
                if (unit.getAdvancedTechYear() > unit.getYear()) {
                    csvLine.append(unit.getYear());
                }
                csvLine.append(DELIM);

                // Advanced Tech Year
                if (unit.getAdvancedTechYear() > 0) {
                    csvLine.append(unit.getAdvancedTechYear());
                }
                csvLine.append(DELIM);

                // Standard Tech Year
                if (unit.getStandardTechYear() > 0) {
                    csvLine.append(unit.getStandardTechYear());
                }
                csvLine.append(DELIM);

                // Extinct Tech Year
                csvLine.append(unit.getExtinctRange()).append(DELIM);
                // Unit Type.
                csvLine.append(unit.getFullAccurateUnitType()).append(DELIM);
                // Omni
                csvLine.append(unit.getOmni()).append(DELIM);
                // Unit Role
                csvLine.append(unit.getRole()).append(DELIM);
                // Unit BV
                csvLine.append(unit.getBV()).append(DELIM);
                // Unit Dry Cost
                csvLine.append(unit.getDryCost()).append(DELIM);
                // Unit Tech Level
                csvLine.append(unit.getLevel()).append(DELIM);
                // Engine Type
                csvLine.append(unit.getEngineName()).append(DELIM);

                // Internals Type
                if (unit.getInternalsType() >= 0) {
                    String isString = unit.isClan() ? "Clan " : "IS ";
                    isString += EquipmentType.structureNames[unit.getInternalsType()] + DELIM;
                    csvLine.append(isString);
                } else if (unit.getInternalsType() < 0) {
                    csvLine.append(NOT_APPLICABLE).append(DELIM);
                }

                // Myomer type
                csvLine.append(unit.getMyomerName()).append(DELIM);

                // Cockpit Type
                if ((unit.getCockpitType() >= 0) && (unit.getCockpitType() < Mek.COCKPIT_STRING.length)) {
                    if (unit.getUnitType().equals("Mek")) {
                        csvLine.append(Mek.COCKPIT_STRING[unit.getCockpitType()]).append(DELIM);
                    } else {
                        csvLine.append(Aero.COCKPIT_STRING[unit.getCockpitType()]).append(DELIM);
                    }
                } else {
                    csvLine.append(NOT_APPLICABLE).append(DELIM);
                }

                // Gyro Type
                if (unit.getGyroType() >= 0) {
                    csvLine.append(Mek.GYRO_STRING[unit.getGyroType()]).append(DELIM);
                } else if (unit.getGyroType() < 0) {
                    csvLine.append(NOT_APPLICABLE).append(DELIM);
                }

                // Armor type - prints different armor types on the unit
                ArrayList<Integer> armorType = new ArrayList<>();
                ArrayList<Integer> armorTech = new ArrayList<>();
                int[] at;
                int[] att;

                at = unit.getArmorTypes();
                att = unit.getArmorTechTypes();
                for (int i = 0; i < at.length; i++) {
                    boolean contains = false;
                    for (int j = 0; j < armorType.size(); j++) {
                        if ((armorType.get(j) == at[i]) && (armorTech.get(j) == att[i])) {
                            contains = true;
                            break;
                        }
                    }

                    if (!contains) {
                        armorType.add(at[i]);
                        armorTech.add(att[i]);
                    }
                }
                for (int i = 0; i < armorType.size(); i++) {
                    csvLine.append(EquipmentType.getArmorTypeName(armorType.get(i),
                          TechConstants.isClan(armorTech.get(i)))).append(",");
                }
                csvLine.append(DELIM);

                // Equipment Names
                List<String> equipmentNames = new ArrayList<>();
                for (String name : unit.getEquipmentNames()) {
                    // Ignore armor critical
                    if (ArmorType.allArmorNames().contains(name)) {
                        continue;
                    }

                    // Ignore internal structure critical
                    if (Stream.of(EquipmentType.structureNames).anyMatch(name::contains)) {
                        continue;
                    }

                    if (Stream.of("Bay", "Ammo", "Infantry Auto Rifle", Infantry.LEG_ATTACK,
                                Infantry.SWARM_MEK, Infantry.SWARM_WEAPON_MEK, Infantry.STOP_SWARM)
                          .anyMatch(name::contains)) {
                        continue;
                    }
                    equipmentNames.add(name);
                }
                csvLine.append(String.join(",", equipmentNames)).append(DELIM);

                Entity entity = loadEntity(unit.getSourceFile(), unit.getEntryName());
                if (entity != null) {
                    csvLine.append(entity.getFullRatingName()).append(DELIM);
                }

                csvLine.append(unit.getQuirkNames()).append(DELIM);
                csvLine.append(unit.getWeaponQuirkNames()).append(DELIM);

                if (entity != null) {
                    if (!entity.getFluff().getManufacturer().isBlank()) {
                        csvLine.append(entity.getFluff().getManufacturer());
                    } else {
                        csvLine.append("--");
                    }
                    csvLine.append(DELIM);

                    if (!entity.getFluff().getPrimaryFactory().isBlank()) {
                        csvLine.append(entity.getFluff().getPrimaryFactory());
                    } else {
                        csvLine.append("--");
                    }
                    csvLine.append(DELIM);

                    csvLine.append(TROView.formatSystemFluff(System.TARGETING, entity.getFluff(),
                          () -> "--")).append(DELIM);
                    csvLine.append(TROView.formatSystemFluff(System.COMMUNICATIONS, entity.getFluff(),
                          () -> "--")).append(DELIM);
                    csvLine.append(TROView.formatSystemFluff(System.ARMOR, entity.getFluff(),
                          () -> "--")).append(DELIM);
                    csvLine.append(TROView.formatSystemFluff(System.JUMP_JET, entity.getFluff(),
                          () -> "--")).append(DELIM);
                    csvLine.append(TROView.formatSystemFluff(System.ENGINE, entity.getFluff(),
                          () -> "--")).append(DELIM);
                    csvLine.append(TROView.formatSystemFluff(System.CHASSIS, entity.getFluff(),
                          () -> "--")).append(DELIM);

                    csvLine.append(entity.getFluff().getCapabilities().isBlank() ? "no" : "yes").append(DELIM);
                    csvLine.append(entity.getFluff().getOverview().isBlank() ? "no" : "yes").append(DELIM);
                    csvLine.append(entity.getFluff().getDeployment().isBlank() ? "no" : "yes").append(DELIM);
                    csvLine.append(entity.getFluff().getHistory().isBlank() ? "no" : "yes").append(DELIM);

                    String notes = entity.getFluff().getNotes();
                    if (!StringUtility.isNullOrBlank(notes)) {
                        csvLine.append(notes);
                    } else {
                        csvLine.append("--");
                    }
                }

                csvLine.append(DELIM);
                csvLine.append(getFluffDate(unit.getSourceFile(), unit.getEntryName()));

                csvLine.append("\n");
                bw.write(csvLine.toString());
            }
        } catch (FileNotFoundException e) {
            logger.error(e, "Could not open file for output!");
        } catch (IOException e) {
            logger.error(e, "IO Exception");
        }
    }

    public static @Nullable Entity loadEntity(File f, String entityName) {
        try {
            return new MekFileParser(f, entityName).getEntity();
        } catch (megamek.common.loaders.EntityLoadingException e) {
            return null;
        }
    }

    /**
     * Returns the last modified date for a unit file. For files inside zip archives, attempts to resolve the standalone
     * file in the mm-data repository (a sibling of the megamek project directory) to get the accurate filesystem
     * modification date, since zip entry timestamps are often unreliable.
     *
     * @param sourceFile the source file (may be a zip archive)
     * @param entryName  the entry name within a zip, or {@code null} for standalone files
     *
     * @return the last modified date as a {@link LocalDate} in YYYY-MM-DD format, or "--" if it cannot be determined
     */
    private static String getFileModifiedDate(File sourceFile, @Nullable String entryName) {
        File fileToCheck = sourceFile;

        if (entryName != null && sourceFile.getName().toLowerCase().endsWith(".zip")) {
            // The zip lives under <project>/megamek/data/mekfiles/. The mm-data repo
            // is a sibling of the megamek project and mirrors the same data/mekfiles/ structure.
            // Use absolute path to ensure getParent() calls don't return null on relative paths.
            Path zipParent = sourceFile.toPath().toAbsolutePath().getParent();
            if (zipParent != null) {
                // Walk up from data/mekfiles/ to the project root (megamek/megamek/data/mekfiles -> megamek)
                Path projectRoot = zipParent.getParent().getParent().getParent();
                Path mmDataDir = projectRoot.resolveSibling("mm-data")
                      .resolve("data").resolve("mekfiles");
                Path mmDataFile = mmDataDir.resolve(entryName).normalize();

                // Guard against path traversal (Zip Slip) in entry names
                if (mmDataFile.startsWith(mmDataDir)) {
                    File standaloneFile = mmDataFile.toFile();
                    if (standaloneFile.exists()) {
                        fileToCheck = standaloneFile;
                    }
                }
            }
        }

        long lastModified = fileToCheck.lastModified();
        if (lastModified > 0) {
            return LocalDate.ofInstant(
                  Instant.ofEpochMilli(lastModified),
                  ZoneId.systemDefault()).toString();
        }

        return "--";
    }

    private static final String FLUFF_DATE_PREFIX = "# Fluff Date: ";

    /**
     * Scans a unit file for a {@code # Fluff Date:} comment line and returns the date value if found. Handles both
     * standalone files and entries inside zip archives.
     *
     * @param sourceFile the source file (may be a zip archive)
     * @param entryName  the entry name within a zip, or {@code null} for standalone files
     *
     * @return the fluff date string, or "--" if the line is not present
     */
    private static String getFluffDate(File sourceFile, @Nullable String entryName) {
        try {
            if (entryName != null && sourceFile.getName().toLowerCase().endsWith(".zip")) {
                try (ZipFile zipFile = new ZipFile(sourceFile)) {
                    ZipEntry entry = zipFile.getEntry(entryName);
                    if (entry != null) {
                        try (BufferedReader reader = new BufferedReader(
                              new InputStreamReader(zipFile.getInputStream(entry), StandardCharsets.UTF_8))) {
                            return scanForFluffDate(reader);
                        }
                    }
                }
            } else {
                try (BufferedReader reader = Files.newBufferedReader(sourceFile.toPath(), StandardCharsets.UTF_8)) {
                    return scanForFluffDate(reader);
                }
            }
        } catch (IOException e) {
            logger.debug("Could not read fluff date from {}: {}", sourceFile, e.getMessage());
        }

        return "--";
    }

    /**
     * Reads lines from the given reader, looking for a {@code # Fluff Date:} prefix.
     *
     * @param reader the reader to scan
     *
     * @return the date string after the prefix, or "--" if not found
     *
     * @throws IOException if an I/O error occurs
     */
    private static String scanForFluffDate(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith(FLUFF_DATE_PREFIX)) {
                return line.substring(FLUFF_DATE_PREFIX.length()).trim();
            }
        }
        return "--";
    }

    private MekCacheCSVTool() {
    }
}

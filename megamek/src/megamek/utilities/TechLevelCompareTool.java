/*
 * Copyright (C) 2017-2026 The MegaMek Team. All Rights Reserved.
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeSet;

import megamek.common.SimpleTechLevel;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.loaders.MekFileParser;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * Compares computed static tech level to what is in the unit file and reports all units that have equipment that
 * exceeds the declared tech level, followed by a list of all the equipment that caused failures.
 * <p>
 * Note that some failures may be due to system or construction options rather than EquipmentType.
 *
 * @author Neoancient
 */

public class TechLevelCompareTool {
    private static final MMLogger logger = MMLogger.create(TechLevelCompareTool.class);

    private static final String CSV_FILE_NAME = "TechLevelMismatches.txt";
    private static final String DELIM = "|";

    static Set<EquipmentType> staticWeaponSet = new TreeSet<>(Comparator.comparing(EquipmentType::getName));
    static Set<EquipmentType> staticAmmoSet = new TreeSet<>(Comparator.comparing(EquipmentType::getName));
    static Set<EquipmentType> staticMiscSet = new TreeSet<>(Comparator.comparing(EquipmentType::getName));

    static Set<EquipmentType> variableWeaponSet = new TreeSet<>(Comparator.comparing(EquipmentType::getName));
    static Set<EquipmentType> variableAmmoSet = new TreeSet<>(Comparator.comparing(EquipmentType::getName));
    static Set<EquipmentType> variableMiscSet = new TreeSet<>(Comparator.comparing(EquipmentType::getName));

    private static final List<String> csvRows = new ArrayList<>();

    private static final String EQUIPMENT_TYPE_FORMATTED_STRING = "\t%s (%s)";
    private static int staticBadMeks = 0;
    private static int variableBadMeks = 0;

    public static void main(String[] args) {
        MekSummaryCache msc = MekSummaryCache.getInstance();

        while (!msc.isInitialized()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
        }

        logger.info("Any output you see from here are errors with the units.");

        for (MekSummary ms : msc.getAllMeks()) {
            Entity en;

            try {
                en = new MekFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
            } catch (EntityLoadingException ignored) {
                String message = String.format("Could not load entity %s", ms.getName());
                logger.error(message);
                continue;
            }

            handleBadEntity(en, ms);
        }

        printDetails();
        writeCsvReport();
    }

    private static void handleBadEntity(Entity entity, MekSummary ms) {
        int mulId = ms.getMulId();
        int introYear = entity.getYear();
        SimpleTechLevel declared = SimpleTechLevel.convertCompoundToSimple(entity.getTechLevel());
        SimpleTechLevel staticCalc = entity.getStaticTechLevel();
        SimpleTechLevel variableCalc = entity.getSimpleLevel(introYear);

        boolean staticMismatch = declared.compareTo(staticCalc) < 0;
        boolean variableMismatch = declared.compareTo(variableCalc) < 0;

        if (staticMismatch || variableMismatch) {
            List<String> staticOffending = new ArrayList<>();
            List<String> variableOffending = new ArrayList<>();

            if (staticMismatch) {
                String message = String.format("[Static] %s (MUL ID: %d): %s/%s",
                      entity.getShortName(), mulId, declared, staticCalc);
                logger.info(message);
                staticBadMeks++;
            }

            if (variableMismatch) {
                String message = String.format("[Variable] %s (MUL ID: %d, Year: %d): %s/%s",
                      entity.getShortName(), mulId, introYear, declared, variableCalc);
                logger.info(message);
                variableBadMeks++;
            }

            for (Mounted<?> m : entity.getEquipment()) {
                EquipmentType mountedEquipmentType = m.getType();

                if (staticMismatch && declared.compareTo(mountedEquipmentType.getStaticTechLevel()) < 0) {
                    staticOffending.add(mountedEquipmentType.getName()
                          + " (" + mountedEquipmentType.getStaticTechLevel() + ")");

                    if (mountedEquipmentType instanceof WeaponType weaponType) {
                        staticWeaponSet.add(weaponType);
                    } else if (mountedEquipmentType instanceof AmmoType ammoType) {
                        staticAmmoSet.add(ammoType);
                    } else {
                        staticMiscSet.add(mountedEquipmentType);
                    }
                }

                if (variableMismatch) {
                    SimpleTechLevel equipmentVariableLevel = mountedEquipmentType.getSimpleLevel(introYear);
                    if (declared.compareTo(equipmentVariableLevel) < 0) {
                        variableOffending.add(mountedEquipmentType.getName()
                              + " (" + equipmentVariableLevel + ")");

                        if (mountedEquipmentType instanceof WeaponType weaponType) {
                            variableWeaponSet.add(weaponType);
                        } else if (mountedEquipmentType instanceof AmmoType ammoType) {
                            variableAmmoSet.add(ammoType);
                        } else {
                            variableMiscSet.add(mountedEquipmentType);
                        }
                    }
                }
            }

            StringJoiner row = new StringJoiner(DELIM);
            row.add(String.valueOf(mulId));
            row.add(ms.getFullChassis());
            row.add(ms.getModel());
            row.add(String.valueOf(introYear));
            row.add(declared.toString());
            row.add(staticCalc.toString());
            row.add(variableCalc.toString());
            row.add(staticMismatch ? "YES" : "NO");
            row.add(variableMismatch ? "YES" : "NO");
            row.add(String.valueOf(ms.getSourceFile()));
            row.add(ms.getEntryName() != null ? ms.getEntryName() : "");
            row.add(String.join(", ", staticOffending));
            row.add(String.join(", ", variableOffending));
            csvRows.add(row.toString());
        }
    }

    private static void printDetails() {
        String message;
        int totalMeks = MekSummaryCache.getInstance().getAllMeks().length;

        logger.info("--- Static Tech Level Mismatches ---");

        logger.info("Weapons:");
        for (EquipmentType et : staticWeaponSet) {
            message = String.format(EQUIPMENT_TYPE_FORMATTED_STRING, et.getName(), et.getStaticTechLevel());
            logger.info(message);
        }

        logger.info("Ammo:");
        for (EquipmentType et : staticAmmoSet) {
            message = String.format(EQUIPMENT_TYPE_FORMATTED_STRING, et.getName(), et.getStaticTechLevel());
            logger.info(message);
        }

        logger.info("MiscType:");
        for (EquipmentType et : staticMiscSet) {
            message = String.format(EQUIPMENT_TYPE_FORMATTED_STRING, et.getName(), et.getStaticTechLevel());
            logger.info(message);
        }

        message = String.format("Static Failed: %d/%d", staticBadMeks, totalMeks);
        logger.info(message);

        logger.info("--- Variable Tech Level Mismatches (by intro year) ---");

        logger.info("Weapons:");
        for (EquipmentType et : variableWeaponSet) {
            message = String.format(EQUIPMENT_TYPE_FORMATTED_STRING, et.getName(), et.getStaticTechLevel());
            logger.info(message);
        }

        logger.info("Ammo:");
        for (EquipmentType et : variableAmmoSet) {
            message = String.format(EQUIPMENT_TYPE_FORMATTED_STRING, et.getName(), et.getStaticTechLevel());
            logger.info(message);
        }

        logger.info("MiscType:");
        for (EquipmentType et : variableMiscSet) {
            message = String.format(EQUIPMENT_TYPE_FORMATTED_STRING, et.getName(), et.getStaticTechLevel());
            logger.info(message);
        }

        message = String.format("Variable Failed: %d/%d", variableBadMeks, totalMeks);
        logger.info(message);
    }

    private static void writeCsvReport() {
        try (PrintWriter pw = new PrintWriter(CSV_FILE_NAME);
              BufferedWriter bw = new BufferedWriter(pw)) {
            bw.write(String.join(DELIM, "MUL ID", "Chassis", "Model", "Intro Year",
                  "Declared Level", "Static Level", "Variable Level",
                  "Static Mismatch", "Variable Mismatch",
                  "Source File", "Entry Name",
                  "Static Offending Equipment", "Variable Offending Equipment"));
            bw.newLine();

            for (String row : csvRows) {
                bw.write(row);
                bw.newLine();
            }

            logger.info("CSV report written to " + CSV_FILE_NAME + " (" + csvRows.size() + " mismatches)");
        } catch (FileNotFoundException e) {
            logger.error(e, "Could not open CSV file for output!");
        } catch (IOException e) {
            logger.error(e, "IO Exception writing CSV report");
        }
    }
}

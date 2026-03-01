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

    static Set<EquipmentType> weaponSet = new TreeSet<>(Comparator.comparing(EquipmentType::getName));
    static Set<EquipmentType> ammoSet = new TreeSet<>(Comparator.comparing(EquipmentType::getName));
    static Set<EquipmentType> miscSet = new TreeSet<>(Comparator.comparing(EquipmentType::getName));

    private static final List<String> csvRows = new ArrayList<>();

    private static final String EQUIPMENT_TYPE_FORMATTED_STRING = "\t%s (%s)";
    private static int badMeks = 0;

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
        SimpleTechLevel fixed = SimpleTechLevel.convertCompoundToSimple(entity.getTechLevel());
        SimpleTechLevel calc = entity.getStaticTechLevel();

        if (fixed.compareTo(calc) < 0) {
            String message = String.format("%s (MUL ID: %d): %s/%s", entity.getShortName(), mulId, fixed, calc);
            logger.info(message);

            List<String> offendingEquipment = new ArrayList<>();

            for (Mounted<?> m : entity.getEquipment()) {
                EquipmentType mountedEquipmentType = m.getType();

                if (fixed.compareTo(mountedEquipmentType.getStaticTechLevel()) < 0) {
                    offendingEquipment.add(mountedEquipmentType.getName()
                          + " (" + mountedEquipmentType.getStaticTechLevel() + ")");

                    if (mountedEquipmentType instanceof WeaponType weaponType) {
                        weaponSet.add(weaponType);
                    } else if (mountedEquipmentType instanceof AmmoType ammoType) {
                        ammoSet.add(ammoType);
                    } else {
                        miscSet.add(mountedEquipmentType);
                    }
                }
            }

            StringJoiner row = new StringJoiner(DELIM);
            row.add(String.valueOf(mulId));
            row.add(ms.getFullChassis());
            row.add(ms.getModel());
            row.add(fixed.toString());
            row.add(calc.toString());
            row.add(String.valueOf(ms.getSourceFile()));
            row.add(ms.getEntryName() != null ? ms.getEntryName() : "");
            row.add(String.join(", ", offendingEquipment));
            csvRows.add(row.toString());

            badMeks++;
        }
    }

    private static void printDetails() {
        String message;

        logger.info("Weapons:");
        for (EquipmentType et : weaponSet) {
            message = String.format(EQUIPMENT_TYPE_FORMATTED_STRING, et.getName(), et.getStaticTechLevel());
            logger.info(message);
        }

        logger.info("Ammo:");
        for (EquipmentType et : ammoSet) {
            message = String.format(EQUIPMENT_TYPE_FORMATTED_STRING, et.getName(), et.getStaticTechLevel());
            logger.info(message);
        }

        logger.info("MiscType:");
        for (EquipmentType et : miscSet) {
            message = String.format(EQUIPMENT_TYPE_FORMATTED_STRING, et.getName(), et.getStaticTechLevel());
            logger.info(message);
        }

        message = String.format("Failed: %d/%d", badMeks, MekSummaryCache.getInstance().getAllMeks().length);
        logger.info(message);
    }

    private static void writeCsvReport() {
        try (PrintWriter pw = new PrintWriter(CSV_FILE_NAME);
              BufferedWriter bw = new BufferedWriter(pw)) {
            bw.write(String.join(DELIM, "MUL ID", "Chassis", "Model", "Declared Level",
                  "Computed Level", "Source File", "Entry Name", "Offending Equipment"));
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

/*
 * MegaMek - Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 * Copyright © 2013 Nicholas Walczak (walczak@cs.umn.edu)
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.utilities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import megamek.codeUtilities.StringUtility;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.ArmorType;
import megamek.common.templates.TROView;
import megamek.logging.MMLogger;

/**
 * This class provides a utility to read in all the /data/mekfiles and print
 * that data out into a CSV format.
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

    private static final List<String> HEADERS = List.of("Chassis", "Model", "MUL ID", "Combined", "Clan",
            "Source", "File Location", "Weight", "Intro Date", "Experimental year", "Advanced year",
            "Standard year", "Extinct Year", "Unit Type", "Role", "BV", "Cost", "Rules", "Engine Name",
            "Internal Structure", "Myomer", "Cockpit Type", "Gyro Type", "Armor Types", "Equipment", "Tech Rating",
            "Unit Quirks", "Weapon Quirks", "Manufacturer", "Factory", "Targeting", "Comms", "Armor", "JJ", "Engine",
            "Chassis", "Capabilities", "Overview", "History", "Deployment", "Notes");

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
                csvLine.append(unit.getChassis()).append(DELIM);
                csvLine.append(unit.getModel()).append(DELIM);
                csvLine.append(unit.getMulId()).append(DELIM);
                csvLine.append(unit.getChassis()).append(" ").append(unit.getModel()).append(DELIM);
                csvLine.append(unit.isClan()).append(DELIM);
                csvLine.append(unit.getSource()).append(DELIM);
                csvLine.append(unit.getSourceFile()).append(DELIM);
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

                    csvLine.append(TROView.formatSystemFluff(EntityFluff.System.TARGETING, entity.getFluff(),
                            () -> "--")).append(DELIM);
                    csvLine.append(TROView.formatSystemFluff(EntityFluff.System.COMMUNICATIONS, entity.getFluff(),
                            () -> "--")).append(DELIM);
                    csvLine.append(TROView.formatSystemFluff(EntityFluff.System.ARMOR, entity.getFluff(),
                            () -> "--")).append(DELIM);
                    csvLine.append(TROView.formatSystemFluff(EntityFluff.System.JUMPJET, entity.getFluff(),
                            () -> "--")).append(DELIM);
                    csvLine.append(TROView.formatSystemFluff(EntityFluff.System.ENGINE, entity.getFluff(),
                            () -> "--")).append(DELIM);
                    csvLine.append(TROView.formatSystemFluff(EntityFluff.System.CHASSIS, entity.getFluff(),
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

    private MekCacheCSVTool() {
    }
}

/*
 * MegaMek - Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 * Copyright © 2013 Nicholas Walczak (walczak@cs.umn.edu)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.utilities;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Vector;
import megamek.common.*;
import java.util.stream.Stream;

/**
 * This class provides a utility to read in all the /data/mechfiles and print
 * that data out into a CSV format.
 *
 * @author arlith
 * @author Simon (Juliez)
 */
public class MechCacheCSVTool {

    public static void main(String... args) {
        try (PrintWriter pw = new PrintWriter("Units.csv"); // TODO : Remove inline filename
             BufferedWriter bw = new BufferedWriter(pw)) {
            MechSummaryCache cache = MechSummaryCache.getInstance(true);
            MechSummary[] units = cache.getAllMechs();

            StringBuilder csvLine = new StringBuilder();

            csvLine.append("Chassis,Model,MUL ID,Combined,Clan,Source,Weight,Intro Date,Experimental year,Advanced year," +
                    "Standard year,Unit Type,Role,BV,Cost,Rules,Engine Name,Internal Structure,Myomer," +
                    "Cockpit Type,Gyro Type,Armor Types,Equipment (multiple entries)\n");
            bw.write(csvLine.toString());

            for (MechSummary unit : units) {
                if (unit.getUnitType().equals("Gun Emplacement")) {
                    continue;
                }

                csvLine = new StringBuilder();
                csvLine.append(unit.getChassis()).append(",");
                csvLine.append(unit.getModel()).append(",");
                csvLine.append(unit.getMulId()).append(",");
                csvLine.append(unit.getChassis()).append(" ").append(unit.getModel()).append(",");
                csvLine.append(unit.isClan()).append(",");
                csvLine.append(unit.getSourceFile()).append(",");
                csvLine.append(unit.getTons()).append(",");
                csvLine.append(unit.getYear()).append(",");

                // Experimental Tech Year
                if (unit.getAdvancedTechYear() > unit.getYear()) {
                    csvLine.append(unit.getYear());
                }
                csvLine.append(",");

                // Advanced Tech Year
                if (unit.getAdvancedTechYear() > 0) {
                    csvLine.append(unit.getAdvancedTechYear());
                }
                csvLine.append(",");

                // Standard Tech Year
                if (unit.getStandardTechYear() > 0) {
                    csvLine.append(unit.getStandardTechYear());
                }
                csvLine.append(",");

                csvLine.append(unit.getFullAccurateUnitType()).append(",");
                csvLine.append(UnitRoleHandler.getRoleFor(unit)).append(",");
                csvLine.append(unit.getBV()).append(",");
                csvLine.append(unit.getDryCost()).append(",");
                csvLine.append(unit.getLevel()).append(",");
                csvLine.append(unit.getEngineName()).append(",");

                // Internals Type
                if (unit.getInternalsType() >= 0) {
                    String isString = unit.isClan() ? "Clan " : "IS ";
                    isString += EquipmentType.structureNames[unit.getInternalsType()] + ",";
                    csvLine.append(isString);
                } else if (unit.getInternalsType() < 0) {
                    csvLine.append("Not Applicable,");
                }

                // Myomer type
                csvLine.append(unit.getMyomerName()).append(",");

                // Cockpit Type
                if ((unit.getCockpitType() >= 0) && (unit.getCockpitType() < Mech.COCKPIT_STRING.length)) {
                    if (unit.getUnitType().equals("Mek")) {
                        csvLine.append(Mech.COCKPIT_STRING[unit.getCockpitType()]).append(",");
                    } else {
                        csvLine.append(Aero.COCKPIT_STRING[unit.getCockpitType()]).append(",");
                    }
                } else {
                    csvLine.append("Not Applicable,");
                }

                // Gyro Type
                if (unit.getGyroType() >= 0) {
                    csvLine.append(Mech.GYRO_STRING[unit.getGyroType()]).append(",");
                } else if (unit.getGyroType() < 0) {
                    csvLine.append("Not Applicable,");
                }

                // Armor type - prints different armor types on the unit
                Vector<Integer> armorType = new Vector<>();
                Vector<Integer> armorTech = new Vector<>();
                int[] at, att;
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

                // Equipment Names
                for (String name : unit.getEquipmentNames()) {
                    // Ignore armor critical
                    if (Stream.of(EquipmentType.armorNames).anyMatch(name::contains)) {
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

                    csvLine.append(name).append(",");
                }
                csvLine.append("\n");
                bw.write(csvLine.toString());
            }
        } catch (FileNotFoundException e) {
            System.out.println("Could not open file for output!");
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
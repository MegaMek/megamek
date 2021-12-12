/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 * Copyright © 2013 Nicholas Walczak (walczak@cs.umn.edu)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

package megamek.utils;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Vector;
import megamek.common.*;
import java.util.stream.Stream;


/**
 * This class provides a utility to read in all of the data/mechfiles and print
 * that data out into a CSV format.
 * 
 * @author arlith
 *
 */
public class MechCacheCSVTool {

    public static void main(String[] args) {
        MechSummaryCache cache = MechSummaryCache.getInstance(true);
        BufferedWriter fout;
        try {
            fout = new BufferedWriter(new PrintWriter("Units.csv"));
        } catch (FileNotFoundException e) {
            System.out.println("Could not open file for output!");
            return;
        }
        MechSummary[] mechs = cache.getAllMechs();
        
        try {
            StringBuffer csvLine = new StringBuffer();


            csvLine.append("Chassis,Model,Combined,Clan,Source,Weight,Intro Date,Experimental year,Advanced year,Standard year,Unit Type,Role,BV,Cost,Rules,Engine Name,Internal Structure," +
                    "Myomer,Cockpit Type,Gyro Type," +
                    "Armor Types," +
                    "Equipment (multiple entries)\n");
            fout.write(csvLine.toString());
            for (MechSummary mech : mechs) {
                if (mech.getUnitType().equals("Gun Emplacement")) {
                    continue;
                }
                
                csvLine = new StringBuffer();
                // Chasis Name
                csvLine.append(mech.getChassis() + ",");
                // Model Name
                csvLine.append(mech.getModel() + ",");
                
                // Combined Name
                csvLine.append(mech.getChassis() + " " + mech.getModel() + ",");
                
                //
                csvLine.append(mech.isClan() + ",");
                
                // Source Book
                csvLine.append(mech.getSourceFile() + ",");

                // Weight
                csvLine.append(mech.getTons() + ",");
                
                // IntroDate
                csvLine.append(mech.getYear() + ",");
                
                // Experimental Tech Year
                if (mech.getAdvancedTechYear() <= mech.getYear()) {
                    csvLine.append(",");
                } else {
                    csvLine.append(mech.getYear() + ",");
                }
                         

                // Advanced Tech Year
                if (mech.getAdvancedTechYear() > 0) {
                    csvLine.append(mech.getAdvancedTechYear()).append(",");
                } else {
                    csvLine.append(",");
                }

                // Standard Tech Year
                if (mech.getStandardTechYear() > 0) {
                    csvLine.append(mech.getStandardTechYear()).append(",");
                } else {
                    csvLine.append(",");
                }

                // Unit Type
                csvLine.append(mech.getUnitType()  + "-" + (mech.getUnitSubType() + ","));
                

                //Role
                csvLine.append(UnitRoleHandler.getRoleFor(mech) + ",");
                
                // BV
                csvLine.append(mech.getBV()  + ",");
                
                // Cost
                csvLine.append(mech.getCost() + ",");

                //Level
                csvLine.append(mech.getLevel() + ",");
                
                // Engine Type
                csvLine.append(mech.getEngineName() + ",");
                
                // Internals Type
                if (mech.getInternalsType() >= 0) {
                    String isString;
                    if (mech.isClan()) {
                        isString = "Clan ";
                    } else {
                        isString = "IS ";
                    }
                    isString += EquipmentType.structureNames[mech.getInternalsType()] + ",";
                    csvLine.append(isString);
                } else if (mech.getInternalsType() < 0) {
                    csvLine.append("Not Applicable,");
                }
                
                // Myomer type
                csvLine.append(mech.getMyomerName()+ ",");
                
                // Cockpit Type
                if ((mech.getCockpitType() >= 0) && (mech.getCockpitType() < Mech.COCKPIT_STRING.length)) {
                    if (mech.getUnitType().equals("Mek")) {
                        csvLine.append(Mech.COCKPIT_STRING[mech.getCockpitType()]+ ",");
                    } else
                        csvLine.append(Aero.COCKPIT_STRING[mech.getCockpitType()]+ ",");
                    } else {
                    csvLine.append("Not Applicable,");
                }
                
                // Gyro Type
                if (mech.getGyroType() >= 0) {
                    csvLine.append(Mech.GYRO_STRING[mech.getGyroType()] + ",");
                } else if (mech.getGyroType() < 0) {
                    csvLine.append("Not Applicable,");    
                   }
                
                // Armor type - prints different armor types on the unit
                Vector<Integer> armorType = new Vector<>();
                Vector<Integer> armorTech = new Vector<>();
                int[] at, att;
                at = mech.getArmorTypes();
                att = mech.getArmorTechTypes();
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
                            TechConstants.isClan(armorTech.get(i))) + ",");
                }
                
                // Equipment Names
                for (String name : mech.getEquipmentNames()) {
                    // Ignore armor critical
                    if (Stream.of(EquipmentType.armorNames).anyMatch(name::contains)) {
                        continue;
                    }

                    // Ignore internal structure critical
                    if (Stream.of(EquipmentType.structureNames).anyMatch(name::contains)) {
                        continue;
                    }

                    // Ignore Bays
                    if (name.contains("Bay")) {
                        continue;
                    }

                    // Ignore Ammo
                    if (name.contains("Ammo")) {
                        continue;
                    }

                    // Ignore Rifle
                    if (name.contains("Infantry Auto Rifle")) {
                        continue;
                    }

                    if (name.contains("SwarmMek")
                            || name.contains("SwarmWeaponMek")
                            || name.contains("StopSwarm")
                            || name.contains("LegAttack")) {
                        continue;
                    }

                    csvLine.append(name).append(",");
                }
                csvLine.append("\n");
                fout.write(csvLine.toString());
            }
            fout.close();
        } catch (IOException e) {
            System.out.println("IOException!");
            e.printStackTrace();
        }
    }
}
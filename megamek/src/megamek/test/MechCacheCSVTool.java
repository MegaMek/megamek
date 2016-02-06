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

package megamek.test;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Vector;

import megamek.common.Aero;
import megamek.common.EquipmentType;
import megamek.common.Mech;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.TechConstants;

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
        try{
            fout = new BufferedWriter(new PrintWriter("mechs.csv"));
        } catch (FileNotFoundException e){
            System.out.println("Could not open file for output!");
            return;
        }
        MechSummary[] mechs = cache.getAllMechs();
        
        try {
            StringBuffer csvLine = new StringBuffer();
            csvLine.append("Chassis,Model,Weight,Intro Date,Unit Type,BV,Rules,Engine Name,Internal Structure," +
            		"Myomer,Cockpit Type,Gyro Type," +
            		"Armor Types," +
            		"Equipment (multiple entries)\n");
            fout.write(csvLine.toString());
            for (MechSummary mech : mechs){
                if (mech.getUnitType().equals("Infantry") || (mech.getUnitType().equals("Gun Emplacement"))){
                    continue;
                }
                
                csvLine = new StringBuffer();
                // Chasis Name
                csvLine.append(mech.getChassis() + ",");
                // Model Name
                csvLine.append(mech.getModel() + ",");
                
                
                //if (mech.getModel().equals("")){
                //    csvLine.append("(Standard),");
                //} else {                    
                //    csvLine.append(mech.getModel() + ",");
                //}
                
                //Weight
                csvLine.append(mech.getTons() + ",");
                
                // IntroDate
                csvLine.append(mech.getYear() + ",");
                
                //Unit Type
                csvLine.append(mech.getUnitType()  + "-" + (mech.getUnitSubType() + ","));
                
                // BV
                csvLine.append(mech.getBV()  + ",");

                csvLine.append(mech.getLevel() + ",");
                // Engine Type
                csvLine.append(mech.getEngineName() + ",");
                
                // Internals Type
                if (mech.getInternalsType() >= 0){
                    String isString;
                    if (mech.isClan()){
                        isString = "Clan ";
                    } else {
                        isString = "IS ";
                    }
                    isString += EquipmentType.structureNames[mech
                            .getInternalsType()] + ",";
                    csvLine.append(isString);
                }else if
                	(mech.getInternalsType() < 0){
                    csvLine.append("Not Applicable,");
                }
                
                // Myomer type
                csvLine.append(mech.getMyomerName()+ ",");
                
                // Cockpit Type
                if (mech.getCockpitType() >= 0 && 
                        mech.getCockpitType() < Mech.COCKPIT_STRING.length){
                    if (mech.getUnitType().equals("Mek")){
                        csvLine.append(Mech.COCKPIT_STRING[mech.getCockpitType()]+ ",");
                    } else
                        csvLine.append(Aero.COCKPIT_STRING[mech.getCockpitType()]+ ",");
                    } else {
                    csvLine.append("Not Applicable,");
                }
                
                // Gyro Type
                if (mech.getGyroType() >= 0){
                    csvLine.append(Mech.GYRO_STRING[mech.getGyroType()] + ",");
                } else if 
                	(mech.getGyroType() <0){   
                    csvLine.append("Not Applicable,");	
               	}
                
                // Armor type - prints different armor types on the unit
                Vector<Integer> armorType, armorTech;
                armorType = new Vector<Integer>();
                armorTech = new Vector<Integer>();
                int [] at, att;
                at = mech.getArmorTypes();
                att = mech.getArmorTechTypes();
                for (int i = 0; i < at.length; i++){
                    boolean contains = false;
                    for (int j = 0; j < armorType.size(); j++){
                        if (armorType.get(j) == at[i] 
                                && armorTech.get(j) == att[i]){
                            contains = true;
                            break;
                        }
                    }
                    if (!contains){
                        armorType.add(at[i]);
                        armorTech.add(att[i]);
                    }
                }
                for (int i = 0; i < armorType.size(); i++){
                    csvLine.append(EquipmentType.getArmorTypeName(
                            armorType.get(i),
                            TechConstants.isClan(armorTech.get(i)))
                            + ",");
                }
                
                // Equipment Names
                for (String name : mech.getEquipmentNames()){
                    boolean ignore = false;
                    // Ignore armor criticals
                    for (String armorName : EquipmentType.armorNames){
                        if (name.contains(armorName.trim())){
                            ignore = true;
                        }
                    }
                    // Ignore internal structure criticals
                    for (String isName : EquipmentType.structureNames){
                        if (name.contains(isName.trim())){
                            ignore = true;
                        }
                    }
                    // Ignore Bays
                    if (name.contains("Bay")){
                        ignore = true;
                    }
                    // Ignore Ammo
                    if (name.contains("Ammo")){
                        ignore = true;
                    }
                    // Ignore Rifle
                    if (name.contains("Infantry Auto Rifle")){
                        ignore = true;
                    }
                    
                    
                    if (name.contains("SwarmMek")
                            || name.contains("SwarmWeaponMek")
                            || name.contains("StopSwarm")
                            || name.contains("LegAttack")){
                        ignore = true;
                    }
                    
                    if (!ignore){
                        csvLine.append(name + ",");
                    }
                }     
                csvLine.append("\n");
                fout.write(csvLine.toString());
            }
        fout.close();
        }catch (IOException e){
            System.out.println("IOException!");
            e.printStackTrace();
        }
        
        
    }
}

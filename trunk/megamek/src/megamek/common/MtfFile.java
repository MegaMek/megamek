/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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

/*
 * MtfFile.java
 *
 * Created on April 7, 2002, 8:47 PM
 */

package megamek.common;

import java.io.*;

/**
 *
 * @author  Ben
 * @version
 */
public class MtfFile {
    
    String name;
    String model;
    
    String chassisConfig;
    String techBase;
    String techYear;
    String rulesLevel;
    
    String tonnage;
    String engine;
    String internalType;
    String myomerType;
    
    String heatSinks;
    String walkMP;
    String jumpMP;
    
    String armorType;
    String larmArmor;
    String rarmArmor;
    String ltArmor;
    String rtArmor;
    String ctArmor;
    String headArmor;
    String llegArmor;
    String rlegArmor;
    String ltrArmor;
    String rtrArmor;
    String ctrArmor;
    
    String weaponCount;
    String[] weaponData;
    
    String[][] critData;
    
    
    /** Creates new MtfFile */
    public MtfFile(File file) {
        try {
            BufferedReader r = new BufferedReader(new FileReader(file));
            
            name = r.readLine();
            model = r.readLine();
            
            r.readLine();
            
            chassisConfig = r.readLine();
            techBase = r.readLine();
            techYear = r.readLine();
            rulesLevel = r.readLine();
            
            r.readLine();
            
            tonnage = r.readLine();
            engine = r.readLine();
            internalType = r.readLine();
            myomerType = r.readLine();
            
            r.readLine();
            
            heatSinks = r.readLine();
            walkMP = r.readLine();
            jumpMP = r.readLine();
            
            r.readLine();
            
            armorType = r.readLine();
            larmArmor = r.readLine();
            rarmArmor = r.readLine();
            ltArmor = r.readLine();
            rtArmor = r.readLine();
            ctArmor = r.readLine();
            headArmor = r.readLine();
            llegArmor = r.readLine();
            rlegArmor = r.readLine();
            ltrArmor = r.readLine();
            rtrArmor = r.readLine();
            ctrArmor = r.readLine();
            
            r.readLine();
            
            weaponCount = r.readLine();
            
            int a = 9;
            
            int weapons = Integer.parseInt(weaponCount.substring(8));
            weaponData = new String[weapons];
            for(int i = 0; i < weapons; i++) {
                weaponData[i] = r.readLine();
            }
            
            critData = new String[8][12];
            
            readCrits(r, Mech.LOC_LARM);
            readCrits(r, Mech.LOC_RARM);
            readCrits(r, Mech.LOC_LT);
            readCrits(r, Mech.LOC_RT);
            readCrits(r, Mech.LOC_CT);
            readCrits(r, Mech.LOC_HEAD);
            readCrits(r, Mech.LOC_LLEG);
            readCrits(r, Mech.LOC_RLEG);
            
            r.close();
        } catch (IOException ex) {
            //arg!
            System.err.println("MtfFile: error reading file");
        }
    }
    
    private void readCrits(BufferedReader r, int loc) throws IOException {
        r.readLine(); // blank line
        r.readLine(); // location name.... verify?
        
        for (int i = 0; i < 12; i++) {
            critData[loc][i] = r.readLine();
        }
    }
    
    public Mech getMech() {
        Mech mech = new Mech();

        if (techYear == null || !techYear.substring(4).equalsIgnoreCase("3025")) {
            return null;
        }
        
        mech.setName(name);
        mech.setModel(model);
        
        mech.weight = (float)Integer.parseInt(tonnage.substring(5));
        mech.heatSinks = Integer.parseInt(heatSinks.substring(11, 14).trim()) - 10;
        
        mech.setOriginalWalkMP(Integer.parseInt(walkMP.substring(8)));
        mech.setOriginalJumpMP(Integer.parseInt(jumpMP.substring(8)));
        
        mech.autoSetInternal();
        
        mech.setArmor(Integer.parseInt(larmArmor.substring(9)), Mech.LOC_LARM, false);
        mech.setArmor(Integer.parseInt(rarmArmor.substring(9)), Mech.LOC_RARM, false);
        mech.setArmor(Integer.parseInt(ltArmor.substring(9)), Mech.LOC_LT, false);
        mech.setArmor(Integer.parseInt(rtArmor.substring(9)), Mech.LOC_RT, false);
        mech.setArmor(Integer.parseInt(ctArmor.substring(9)), Mech.LOC_CT, false);
        mech.setArmor(Integer.parseInt(headArmor.substring(9)), Mech.LOC_HEAD, false);
        mech.setArmor(Integer.parseInt(llegArmor.substring(9)), Mech.LOC_LLEG, false);
        mech.setArmor(Integer.parseInt(rlegArmor.substring(9)), Mech.LOC_RLEG, false);
        mech.setArmor(Integer.parseInt(ltrArmor.substring(10)), Mech.LOC_LT, true);
        mech.setArmor(Integer.parseInt(rtrArmor.substring(10)), Mech.LOC_RT, true);
        mech.setArmor(Integer.parseInt(ctrArmor.substring(10)), Mech.LOC_CT, true);
        
        // oog, crits.
        for (int i = 0; i < mech.locations(); i++) {
            parseCrits(mech, i);
        }

        return mech;
    }
    
    private void parseCrits(Mech mech, int loc) {
        for (int i = 0; i < mech.getNumberOfCriticals(loc); i++) {
            // if the slot's full already, skip it.
            if (mech.getCritical(loc, i) != null) {
                continue;
            }
            
            // parse out and add the critical
            String critName = critData[loc][i];
            boolean rearMounted = false;
            
            if (critName.endsWith("(R)")) {
                rearMounted = true;
                critName = critName.substring(0, critName.length() - 3).trim();
            }
            
            EquipmentType etype = EquipmentType.getByMtfName(critName);
            if (etype instanceof WeaponType) {
                mech.addWeapon(new Mounted(etype), loc, rearMounted);
            } else if (etype instanceof AmmoType) {
                mech.addAmmo(new Mounted(etype), loc);
            } else if (etype instanceof MiscType) {
                mech.addMisc(new Mounted(etype), loc);
            } else {
//                System.out.println("mtffile: could not find equipment " + critName);
            }
        }
    }
}

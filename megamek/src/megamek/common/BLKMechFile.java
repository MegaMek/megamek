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
 * BLkFile.java
 *
 * Created on April 6, 2002, 2:06 AM
 */

/**
 *
 * @author  njrkrynn
 * @version 
 */
package megamek.common;

import java.io.*;

import megamek.common.util.*;

public class BLKMechFile {

    //armor locatioms
    public static final int HD = 0;
    public static final int LA = 1;
    public static final int LF = 2;
    public static final int LB = 3;
    public static final int CF = 4;
    public static final int CB = 5;
    public static final int RF = 6;
    public static final int RB = 7;
    public static final int RA = 8;
    public static final int LL = 9;
    public static final int RL = 10;
    
    public static final int CT = 4;
    public static final int RT = 6;
    public static final int LT = 2;
    //
    
    
    BuildingBlock dataFile;
    
    /** Creates new BLkFile */
    public BLKMechFile(String fileName) {
        
        dataFile = new BuildingBlock(fileName);
        
    }
    
    public BLKMechFile(File file) {
        
        dataFile = new BuildingBlock(file.getPath());
        
    }  
      
    //if it's a block file it should have this...
    public boolean isMine() {
     
        if (dataFile.exists("blockversion") ) return true;
        
        return false;
        
    }

    public Mech getMech() {
    
        Mech mech = new Mech();
        
        //Do I even write the year for these??
        

    if (dataFile.exists("year")) {
                    
            if (dataFile.getDataAsInt("year")[0] != 3025) return null;
            
        }
        
        
        if (!dataFile.exists("name")) return null;
            mech.setName(dataFile.getDataAsString("Name")[0]);
        
        if (!dataFile.exists("model")) return null;
            mech.setModel(dataFile.getDataAsString("Model")[0]);
        
        if (!dataFile.exists("tonnage")) return null;
            mech.weight = dataFile.getDataAsFloat("tonnage")[0];
            
        if (!dataFile.exists("walkingMP")) return null;
            mech.setOriginalWalkMP(dataFile.getDataAsInt("walkingMP")[0]);
            
        if (!dataFile.exists("jumpingMP")) return null;
            mech.setOriginalJumpMP(dataFile.getDataAsInt("jumpingMP")[0]);
            
        //I keep internal(integral) heat sinks seperate...
        if (!dataFile.exists("heatsinks")) return null;
            mech.heatSinks = 
            dataFile.getDataAsInt("heatsinks")[0]+
            dataFile.getDataAsInt("heatsinks")[1]-10;
        
        
            if (!dataFile.exists("armor") ) return null;
            
            int [] armor = new int[11]; //only 11 locations...
            
            if (dataFile.getDataAsInt("armor").length < 11) {
             
                System.err.println("BLKMechFile->Read armor array doesn't match my armor array...");
                return null;
                
            }
            armor = dataFile.getDataAsInt("Armor");
            
            mech.setArmor(armor[this.HD],mech.LOC_HEAD) ;
            
            mech.setArmor( armor[this.LA], mech.LOC_LARM );
            mech.setArmor(armor[this.RA], mech.LOC_RARM );
            mech.setArmor(armor[this.LL], mech.LOC_LLEG );
            mech.setArmor(armor[this.RL], mech.LOC_RLEG );
            
            mech.setArmor(armor[this.CF],mech.LOC_CT );
            mech.setArmor( armor[this.LF],mech.LOC_LT);
            mech.setArmor(armor[this.RF],mech.LOC_RT );
            
            //changed...
            mech.setArmor( armor[this.CB],mech.LOC_CT, true);
            mech.setArmor(armor[this.LB],mech.LOC_LT, true);
            mech.setArmor(armor[this.RB],mech.LOC_RT, true);
            
            
            if (!dataFile.exists("internal armor") ) {
                //try to guess...
                mech.setInternal( 3, (int)(armor[CF]+armor[CB])/2, (int)(armor[LF]+armor[LB])/2, (int)(armor[LA]/2), (int)(armor[LL]/2) );
            }else {
            
                armor = dataFile.getDataAsInt("internal armor");
            
                //all the locations should be about the same...
                mech.setInternal( armor[HD], armor[CT], armor[LT], armor[LA], armor[LL] );
            
                
            }
            
            //check for removed arm actuators...
            
            
            //no lower right arm
            if (!dataFile.getDataAsString("ra criticals")[2].trim().equalsIgnoreCase("Lower Arm Actuator"))
                mech.removeCriticals(Mech.LOC_RARM, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_LOWER_ARM));
            //no right hand
            if (!dataFile.getDataAsString("ra criticals")[3].trim().equalsIgnoreCase("Hand Actuator"))
                mech.removeCriticals(Mech.LOC_RARM, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_HAND));
            
            //no lower left arm
            if (!dataFile.getDataAsString("la criticals")[2].trim().equalsIgnoreCase("Lower Arm Actuator"))
                mech.removeCriticals(Mech.LOC_LARM, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_LOWER_ARM));
            //no left hand
            if (!dataFile.getDataAsString("la criticals")[3].trim().equalsIgnoreCase("Hand Actuator"))
                mech.removeCriticals(Mech.LOC_LARM, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_HAND));
            
            
            
            
            //load equipment stuff...
            com.sun.java.util.collections.Vector [] criticals = new com.sun.java.util.collections.Vector[8];
            
            criticals[mech.LOC_HEAD] = dataFile.getDataAsVector("hd criticals");
            criticals[mech.LOC_LARM] = dataFile.getDataAsVector("la criticals");
            criticals[mech.LOC_RARM] = dataFile.getDataAsVector("ra criticals");
            criticals[mech.LOC_LLEG] = dataFile.getDataAsVector("ll criticals");
            criticals[mech.LOC_RLEG] = dataFile.getDataAsVector("rl criticals");
            criticals[mech.LOC_LT] = dataFile.getDataAsVector("lt criticals");
            criticals[mech.LOC_RT] = dataFile.getDataAsVector("rt criticals");
            criticals[mech.LOC_CT] = dataFile.getDataAsVector("ct criticals");
            
            //criticals[mech.LOC_LTR] = new com.sun.java.util.collections.Vector(0);
            //criticals[mech.LOC_RTR] = new com.sun.java.util.collections.Vector(0);
            //criticals[mech.LOC_CTR] = new com.sun.java.util.collections.Vector(0);
            
                                   
            
            for (int loc = 0; loc < criticals.length; loc++ ) {
             
                
             
            for (int c = 0; c < criticals[loc].size(); c++) {
                String critName = criticals[loc].get(c).toString().trim();
                boolean rearMounted = false;
                
             if (critName.startsWith("(R) ")) {
                rearMounted = true;
                critName = critName.substring(4);
            }
            
                //changed...
            EquipmentType etype = EquipmentType.getByMepName(critName);
            if (etype != null) {
                mech.addEquipment(new Mounted(etype), loc, rearMounted);
            } else {
                //System.out.println("blkmechfile: could not find equipment " + critName);
            }
            
            }//end of specific location
            }//end of all crits
            
            
            return mech;
                    
    }
}


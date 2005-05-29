/*
 * MegaMek - Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
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
package megamek.common.loaders;

import megamek.common.BipedMech;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.LocationFullException;
import megamek.common.Mech;
import megamek.common.QuadMech;
import megamek.common.TechConstants;
import megamek.common.util.*;

public class BLKMechFile extends BLKFile implements MechLoader {

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
    
    
    public BLKMechFile(BuildingBlock bb)
    {
        dataFile = bb;
    }
      
    public Entity getEntity() throws EntityLoadingException {
    
      int chassisType = 0;
      if (!dataFile.exists("chassis_type")) {
        chassisType = 0;
      } else {
        chassisType = dataFile.getDataAsInt("chassis_type")[0];
      }
        
      Mech mech = null;
        
      if ( chassisType == 1 )
        mech = new QuadMech();
      else
        mech = new BipedMech();

      //Do I even write the year for these??
        
        if (!dataFile.exists("name")) throw new EntityLoadingException("Could not find block.");
            mech.setChassis(dataFile.getDataAsString("Name")[0]);
        
        if (!dataFile.exists("model")) throw new EntityLoadingException("Could not find block.");
            mech.setModel(dataFile.getDataAsString("Model")[0]);
        
        if (!dataFile.exists("year")) throw new EntityLoadingException("Could not find block.");
            mech.setYear(dataFile.getDataAsInt("year")[0]);
            
        if (!dataFile.exists("type")) throw new EntityLoadingException("Could not find block.");
            
        if (dataFile.getDataAsString("type")[0].equals("IS")) {
            if (mech.getYear() == 3025) {
                mech.setTechLevel(TechConstants.T_IS_LEVEL_1);
            } else {
                mech.setTechLevel(TechConstants.T_IS_LEVEL_2);
            }
        } else if (dataFile.getDataAsString("type")[0].equals("Clan")) {
            mech.setTechLevel(TechConstants.T_CLAN_LEVEL_2);
        } else if (dataFile.getDataAsString("type")[0].equals("Mixed (IS Chassis)")) {
            mech.setTechLevel(TechConstants.T_IS_LEVEL_3);
            mech.setMixedTech(true);
        } else if (dataFile.getDataAsString("type")[0].equals("Mixed (Clan Chassis)")) {
            mech.setTechLevel(TechConstants.T_CLAN_LEVEL_3);
            mech.setMixedTech(true);
        } else if (dataFile.getDataAsString("type")[0].equals("Mixed")) {
            throw new EntityLoadingException("Unsupported tech base: \"Mixed\" is no longer allowed by itself.  You must specify \"Mixed (IS Chassis)\" or \"Mixed (Clan Chassis)\".");
        } else {
            throw new EntityLoadingException("Unsupported tech level: " + dataFile.getDataAsString("type")[0]);
        }
        
        if (!dataFile.exists("tonnage")) throw new EntityLoadingException("Could not find block.");
            mech.setWeight(dataFile.getDataAsFloat("tonnage")[0]);
            
        if (!dataFile.exists("walkingMP")) throw new EntityLoadingException("Could not find block.");
            mech.setOriginalWalkMP(dataFile.getDataAsInt("walkingMP")[0]);
            
        if (!dataFile.exists("jumpingMP")) throw new EntityLoadingException("Could not find block.");
            mech.setOriginalJumpMP(dataFile.getDataAsInt("jumpingMP")[0]);
            
        //I keep internal(integral) heat sinks seperate...
        if (!dataFile.exists("heatsinks")) throw new EntityLoadingException("Could not find block.");
            mech.addEngineSinks(dataFile.getDataAsInt("heatsinks")[0], false);
        
            if (!dataFile.exists("armor") ) throw new EntityLoadingException("Could not find block.");
            
            int [] armor = new int[11]; //only 11 locations...
            
            if (dataFile.getDataAsInt("armor").length < 11) {
             
                System.err.println("BLKMechFile->Read armor array doesn't match my armor array...");
                throw new EntityLoadingException("Could not find block.");
                
            }
            armor = dataFile.getDataAsInt("Armor");
            
            mech.initializeArmor(armor[BLKMechFile.HD],Mech.LOC_HEAD) ;
            
            mech.initializeArmor( armor[BLKMechFile.LA], Mech.LOC_LARM );
            mech.initializeArmor(armor[BLKMechFile.RA], Mech.LOC_RARM );
            mech.initializeArmor(armor[BLKMechFile.LL], Mech.LOC_LLEG );
            mech.initializeArmor(armor[BLKMechFile.RL], Mech.LOC_RLEG );
            
            mech.initializeArmor(armor[BLKMechFile.CF],Mech.LOC_CT );
            mech.initializeArmor( armor[BLKMechFile.LF],Mech.LOC_LT);
            mech.initializeArmor(armor[BLKMechFile.RF],Mech.LOC_RT );
            
            //changed...
            mech.initializeRearArmor( armor[BLKMechFile.CB],Mech.LOC_CT);
            mech.initializeRearArmor(armor[BLKMechFile.LB],Mech.LOC_LT);
            mech.initializeRearArmor(armor[BLKMechFile.RB],Mech.LOC_RT);
            
            
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
            
            criticals[Mech.LOC_HEAD] = dataFile.getDataAsVector("hd criticals");
            criticals[Mech.LOC_LARM] = dataFile.getDataAsVector("la criticals");
            criticals[Mech.LOC_RARM] = dataFile.getDataAsVector("ra criticals");
            criticals[Mech.LOC_LLEG] = dataFile.getDataAsVector("ll criticals");
            criticals[Mech.LOC_RLEG] = dataFile.getDataAsVector("rl criticals");
            criticals[Mech.LOC_LT] = dataFile.getDataAsVector("lt criticals");
            criticals[Mech.LOC_RT] = dataFile.getDataAsVector("rt criticals");
            criticals[Mech.LOC_CT] = dataFile.getDataAsVector("ct criticals");
            
            //criticals[mech.LOC_LTR] = new com.sun.java.util.collections.Vector(0);
            //criticals[mech.LOC_RTR] = new com.sun.java.util.collections.Vector(0);
            //criticals[mech.LOC_CTR] = new com.sun.java.util.collections.Vector(0);
            
                                   
            
        // prefix is "Clan " or "IS "
        String prefix;
        if (mech.getTechLevel() == TechConstants.T_CLAN_LEVEL_2) {
            prefix = "Clan ";
        } else {
            prefix = "IS ";
        }

            for (int loc = 0; loc < criticals.length; loc++ ) {
             
                
             
            for (int c = 0; c < criticals[loc].size(); c++) {
                String critName = criticals[loc].get(c).toString().trim();
                boolean rearMounted = false;
                
             if (critName.startsWith("(R) ")) {
                rearMounted = true;
                critName = critName.substring(4);
            }
            
            EquipmentType etype = EquipmentType.get(critName);
            
            if (etype == null) {
                // try w/ prefix
                etype = EquipmentType.get(prefix + critName);
            }
            if (etype != null) {
                try {
                    mech.addEquipment(etype, loc, rearMounted);
                } catch (LocationFullException ex) {
                    throw new EntityLoadingException(ex.getMessage());
                }
            }
            
            }//end of specific location
            }//end of all crits
            
         if (mech.isClan()) {
            mech.addClanCase();
        }

            
            return mech;
                    
    }
}


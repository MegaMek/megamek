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

package megamek.common;

import java.io.*;
import java.util.Enumeration;

/**
 * You know what mechs are, silly.
 */
public abstract class Mech
    extends Entity
    implements Serializable
{
    private static final int      NUM_MECH_LOCATIONS = 8;
    
    // weight class limits
    public static final int        WEIGHT_LIGHT        = 35;
    public static final int        WEIGHT_MEDIUM        = 55;
    public static final int        WEIGHT_HEAVY        = 75;
    public static final int        WEIGHT_ASSAULT        = 100;

    // system designators for critical hits
    public static final int        SYSTEM_LIFE_SUPPORT    = 0;
    public static final int        SYSTEM_SENSORS        = 1;
    public static final int        SYSTEM_COCKPIT        = 2;
    public static final int        SYSTEM_ENGINE        = 3;
    public static final int        SYSTEM_GYRO            = 4;

    // actutors are systems too, for now
    public static final int        ACTUATOR_SHOULDER    = 7;
    public static final int        ACTUATOR_UPPER_ARM    = 8;
    public static final int        ACTUATOR_LOWER_ARM    = 9;
    public static final int        ACTUATOR_HAND        = 10;
    public static final int        ACTUATOR_HIP        = 11;
    public static final int        ACTUATOR_UPPER_LEG    = 12;
    public static final int        ACTUATOR_LOWER_LEG    = 13;
    public static final int        ACTUATOR_FOOT        = 14;
    
    public static final String systemNames[] = {"Life Support", "Sensors", "Cockpit",
        "Engine", "Gyro", "x", "x", "Shoulder", "Upper Arm", 
        "Lower Arm", "Hand", "Hip", "Upper Leg", "Lower Leg", "Foot"};
    
    // locations
    public static final int        LOC_HEAD             = 0;
    public static final int        LOC_CT               = 1;
    public static final int        LOC_RT               = 2;
    public static final int        LOC_LT               = 3;
    public static final int        LOC_RARM             = 4;
    public static final int        LOC_LARM             = 5;
    public static final int        LOC_RLEG             = 6;
    public static final int        LOC_LLEG             = 7;
    
    // rear armor
    private int[] rearArmor;
    private int[] orig_rearArmor;
    
    // MASCLevel is the # of turns MASC has been used - the # of turns it hasn't, kind of
    private int nMASCLevel = 0;
    
    /**
     * Construct a new, blank, mech.
     */
    public Mech() {
        super();
        
        rearArmor = new int[locations()];
        orig_rearArmor = new int[locations()];
        
        for (int i = 0; i < locations(); i++) {
            if (!hasRearArmor(i)) {
              initializeRearArmor(ARMOR_NA, i);
            }
        }

        setCritical(LOC_HEAD, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_LIFE_SUPPORT));
        setCritical(LOC_HEAD, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS));
        setCritical(LOC_HEAD, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_COCKPIT));
        setCritical(LOC_HEAD, 4, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_SENSORS));
        setCritical(LOC_HEAD, 5, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_LIFE_SUPPORT));

        setCritical(LOC_CT, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_ENGINE));
        setCritical(LOC_CT, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_ENGINE));
        setCritical(LOC_CT, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_ENGINE));
        setCritical(LOC_CT, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_GYRO));
        setCritical(LOC_CT, 4, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_GYRO));
        setCritical(LOC_CT, 5, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_GYRO));
        setCritical(LOC_CT, 6, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_GYRO));
        setCritical(LOC_CT, 7, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_ENGINE));
        setCritical(LOC_CT, 8, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_ENGINE));
        setCritical(LOC_CT, 9, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, SYSTEM_ENGINE));
        
        setCritical(LOC_RLEG, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_HIP));
        setCritical(LOC_RLEG, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_UPPER_LEG));
        setCritical(LOC_RLEG, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_LOWER_LEG));
        setCritical(LOC_RLEG, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_FOOT));

        setCritical(LOC_LLEG, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_HIP));
        setCritical(LOC_LLEG, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_UPPER_LEG));
        setCritical(LOC_LLEG, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_LOWER_LEG));
        setCritical(LOC_LLEG, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_FOOT));
    }
    
    private static int[] MASC_FAILURE = { 0, 2, 4, 6, 10, 12, 12, 12 };
    
    public boolean checkForMASCFailure(StringBuffer phaseReport)
    {        
        if (hasActiveMASC()) {
            boolean bFailure = false;
            
            // iterate before checking
            nMASCLevel++;
            
            int nRoll = Compute.d6(2);
            
            phaseReport.append("\n" + getDisplayName() +
                   " checking for MASC failure.\n");       
            phaseReport.append("Needs " + (MASC_FAILURE[nMASCLevel] + 1) +
                    ", rolls " + nRoll + " : ");
            
            
            if (nRoll <= MASC_FAILURE[nMASCLevel]) {
                // uh oh
                bFailure = true;
                phaseReport.append("MASC fails!.\n");
                
                // do the damage.  Rules say 'as if you took 2 hip crits'. We'll
                // just do the hip crits
                getCritical(LOC_RLEG, 0).setDestroyed(true);
                getCritical(LOC_LLEG, 0).setDestroyed(true);
                for (Enumeration e = getEquipment(); e.hasMoreElements(); ) {
                    Mounted m = (Mounted)e.nextElement();
                    if (m.getType().hasFlag(MiscType.F_MASC)) {
                        m.setDestroyed(true);
                    }
                }                
            }
            else {
                phaseReport.append("succeeds.\n");
            }
            
            // also iterate after checking, to make up for the decrement that all 
            // mechs have at the end of the turn
            nMASCLevel++;
            return bFailure;
        }
        return false;
    }
    

    /**
     * Returns the number of locations in the entity
     */
    public int locations() {
      return NUM_MECH_LOCATIONS;
    }
    
    /**
     * Override entity.newRound()
     */
    public void newRound() {
        super.newRound();

        nMASCLevel = Math.max(0, nMASCLevel - 1);        
        setSecondaryFacing(getFacing());
        
    }
    
    
    /**
     * Returns true if the location in question is a torso location
     */
    public boolean locationIsTorso(int loc) {
        return loc == LOC_CT || loc == LOC_RT || loc == LOC_LT;
    }
                                    
    /**
     * Count the number of destroyed legs on the mech
     */
      public int countDestroyedLegs() {
        int destroyed = 0;
        
        for ( int i = 0; i < locations(); i++ ) {    
          destroyed += (locationIsLeg(i) && isLocationDestroyed(i)) ? 1 : 0;
                }
        
        return destroyed;
                }

    /**
     * Return true is the location is a leg and has a hip crit
     */   
      public boolean legHasHipCrit(int loc) {
        if ( isLocationDestroyed(loc) )
          return false;
          
        if ( locationIsLeg(loc) ) {
          return (getGoodCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_HIP, loc) == 0);
                }
        
        return false;
            }
    
    /**
     * Count non-hip leg actuator crits
     */
      public int countLegActuatorCrits(int loc) {
        if ( isLocationDestroyed(loc) )
          return 0;
          
        int crits = 0;
        
        if ( locationIsLeg(loc) ) {
          if(getGoodCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_UPPER_LEG, loc) == 0) {
              crits++;
        }
          if(getGoodCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_LOWER_LEG, loc) == 0) {
              crits++;
            }
          if(getGoodCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_FOOT, loc) == 0) {
              crits++;
        }
    }

        return crits;
    }
    
    /**
     * Returns true if this mech has an XL engine.  For now, just checks if 
     * there are any engine critical slots in the right torso.
     */
    public boolean hasXL() {
        return getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE, Mech.LOC_RT) > 0;
    }
    
    /**
     * Allocates torso engine crits for an XL engine.  Uses the mech's current
     * techlevel for the engine.
     */
    public void giveXL() {
        giveXL(isClan());
    }
    
    /**
     * Allocates torso engine crits for an XL engine.
     */
    public void giveXL(boolean clan) {
        int crits = clan ? 2 : 3;
        
        for (int i = 0; i < crits; i++) {
            setCritical(LOC_RT, i, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE));
            setCritical(LOC_LT, i, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE));
        }
    }
    
    /**
     * Utility function to loop through the equipment
     */
    public boolean hasActiveMASC() {
        for (Enumeration e = getEquipment(); e.hasMoreElements(); ) {
            Mounted m = (Mounted)e.nextElement();
            if (!m.isDestroyed() && m.getType().hasFlag(MiscType.F_MASC) && m.curMode().equals("On")) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Same
     */
    public boolean hasTSM() {
        for (Enumeration e = getEquipment(); e.hasMoreElements(); ) {
            Mounted m = (Mounted)e.nextElement();
            if (m.getType().hasFlag(MiscType.F_TSM)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Potentially adjust runMP for MASC
     */
    public int getRunMP() {
        if (hasActiveMASC()) {
            return getWalkMP() * 2;
        }
        return super.getRunMP();
    }
    
    /**
     * This mech's jumping MP modified for missing jump jets
     */
    public int getJumpMP() {
        int jump = 0;
        
        for (Enumeration i = miscList.elements(); i.hasMoreElements();) {
            Mounted mounted = (Mounted)i.nextElement();
            if (mounted.getType().hasFlag(MiscType.F_JUMP_JET) && !mounted.isDestroyed()) {
                jump++;
            }
        }
        
        return jump;
    }
    
    /**
     * Returns this mech's jumping MP, modified for missing & underwater jets.
     */
    public int getJumpMPWithTerrain() {
        int waterLevel = game.board.getHex(getPosition()).levelOf(Terrain.WATER);
        if (waterLevel <= 0) {
            return getJumpMP();
        } else if (waterLevel > 1) {
            return 0;
        } else { // waterLevel == 1
            return torsoJumpJets();
        }
    }
    
    /**
     * Returns the number of (working) jump jets mounted in the torsos.
     */
    public int torsoJumpJets() {
        int jump = 0;
        
        for (Enumeration i = miscList.elements(); i.hasMoreElements();) {
            Mounted mounted = (Mounted)i.nextElement();
            if (mounted.getType().hasFlag(MiscType.F_JUMP_JET) && !mounted.isDestroyed()
            && locationIsTorso(mounted.getLocation())) {
                jump++;
            }
        }
        
        return jump;
    }
    
    /**
     * Returns the elevation of this entity.  Mechs do funny stuff in the 
     * middle of a DFA.
     */
    public int elevation() {
        int cElev = super.elevation();
        if (!isMakingDfa()) {
            return cElev;
        }
        // otherwise, we are one elevation above our hex or the target's hex,
        // whichever is higher
        int tElev = game.board.getHex(displacementAttack.getTargetPos()).floor();
        return Math.max(cElev, tElev) + 1;
    }
    
    /**
     * Return the height of this mech above the terrain.
     */
    public int height() {
        return isProne() ? 0 : 1;
    }
    
    /**
     * Adds heat sinks to the engine.  Uses clan/normal depending on the
     * currently set techLevel
     */
    public void addEngineSinks(int totalSinks, boolean dblSinks) {
        addEngineSinks(totalSinks, dblSinks, isClan());
    }
    
    /**
     * Adds heat sinks to the engine.  Adds either the engine capacity, or
     * the entire number of heat sinks, whichever is less
     */
    public void addEngineSinks(int totalSinks, boolean dblSinks, boolean clan) {
        // this relies on these being the correct internalNames for these items
        EquipmentType sinkType;
        if (dblSinks) {
            sinkType = EquipmentType.getByInternalName(clan ? "CLDoubleHeatSink" : "ISDoubleHeatSink");
        } else {
            sinkType = EquipmentType.getByInternalName("Heat Sink");
        }
        
        if (sinkType == null) {
            System.out.println("Mech: can't find heat sink to add to engine");
        }
        
        int toAllocate = Math.min(totalSinks, integralSinkCapacity());
        
        if (toAllocate == 0) {
            System.out.println("Mech: not putting any heat sinks in the engine?!?!");
        }
        
        for (int i = 0; i < toAllocate; i++) {
            try {
                addEquipment(new Mounted(this, sinkType), Mech.LOC_NONE, false);
            } catch (LocationFullException ex) {
                // um, that's impossible.
            }
        }
    }
    
    /**
     * Returns the number if heat sinks that can be added to the engine
     */
    private int integralSinkCapacity() {
        return engineRating() / 25;
    }
    
    /**
     * Returns the engine rating
     */
    public int engineRating() {
        return (int)Math.round(walkMP * weight);
    }
    
    /**
     * Returns the about of heat that the entity can sink each 
     * turn.
     */
    public int getHeatCapacity() {
        int capacity = 0;
        
        for (Enumeration i = miscList.elements(); i.hasMoreElements();) {
            Mounted mounted = (Mounted)i.nextElement();
            if (mounted.isDestroyed()) {
                continue;
            }
            if (mounted.getType().hasFlag(MiscType.F_HEAT_SINK)) {
                capacity++;
            } else if(mounted.getType().hasFlag(MiscType.F_DOUBLE_HEAT_SINK)) {
                capacity += 2;
            }
        }
        
        return capacity;
    }
    
    /**
     * Returns the about of heat that the entity can sink each
     * turn, factoring for water.
     */
    public int getHeatCapacityWithWater() {
        return getHeatCapacity() + Math.min(sinksUnderwater(), 6);
    }
    
    /**
     * Gets the number of heat sinks that are underwater.
     */
    private int sinksUnderwater() {
        Hex curHex = game.board.getHex(getPosition());
        // are we even in water?  is it depth 1+
        if (curHex.levelOf(Terrain.WATER) <= 0) {
            return 0;
        }
        
        // are we entirely underwater?
        if (isProne() || curHex.levelOf(Terrain.WATER) >= 2) {
            return getHeatCapacity();
        }
        
        // okay, count leg sinks
        int sinksUnderwater = 0;
        for (Enumeration i = miscList.elements(); i.hasMoreElements();) {
            Mounted mounted = (Mounted)i.nextElement();
            if (mounted.isDestroyed() || !locationIsLeg(mounted.getLocation())) {
                continue;
            }
            if (mounted.getType().hasFlag(MiscType.F_HEAT_SINK)) {
                sinksUnderwater++;
            } else if (mounted.getType().hasFlag(MiscType.F_DOUBLE_HEAT_SINK)) {
                sinksUnderwater += 2;
            }
        }
        return sinksUnderwater;
    }

    /**
     * Returns the name of the type of movement used.
     * This is mech-specific.
     */
    public String getMovementString(int mtype) {
        switch(mtype) {
        case MOVE_NONE :
            return "None";
        case MOVE_WALK :
            return "Walked";
        case MOVE_RUN :
            return "Ran";
        case MOVE_JUMP :
            return "Jumped";
        default :
            return "Unknown!";
        }
    }
    
    /**
     * Returns the name of the type of movement used.
     * This is mech-specific.
     */
    public String getMovementAbbr(int mtype) {
        switch(mtype) {
        case MOVE_NONE :
            return "N";
        case MOVE_WALK :
            return "W";
        case MOVE_RUN :
            return "R";
        case MOVE_JUMP :
            return "J";
        default :
            return "?";
        }
    }
    
    public boolean canChangeSecondaryFacing() {
        return !isProne();
    }
  
    /**
     * Can this mech torso twist in the given direction?
     */
    public boolean isValidSecondaryFacing(int dir) {
        int rotate = dir - getFacing();
        if (canChangeSecondaryFacing()) {
            return rotate == 0 || rotate == 1 || rotate == -1 || rotate == -5;
        } else {
            return rotate == 0;
        }
    }

    /**
     * Return the nearest valid direction to torso twist in
     */
    public int clipSecondaryFacing(int dir) {
        if (isValidSecondaryFacing(dir)) {
            return dir;
        }
        // can't twist while prone
        if (!canChangeSecondaryFacing()) {
            return getFacing();
        }
        // otherwise, twist once in the appropriate direction
        final int rotate = (dir + (6 - getFacing())) % 6;
        return rotate >= 3 ? (getFacing() + 5) % 6 : (getFacing() + 1) % 6;
    }
    
    public boolean hasRearArmor(int loc) {
        return loc == LOC_CT || loc == LOC_RT || loc == LOC_LT;
    }
    
    /**
     * Returns the amount of armor in the location specified.  Mech version,
     * handles rear armor.
     */
    public int getArmor(int loc, boolean rear) {
        if (rear && hasRearArmor(loc)) {
            return rearArmor[loc];
        } else {
            return super.getArmor(loc, rear);
        }
    }

    /**
     * Returns the original amount of armor in the location specified.  Mech version,
     * handles rear armor.
     */
    public int getOArmor(int loc, boolean rear) {
        if (rear && hasRearArmor(loc)) {
            return orig_rearArmor[loc];
        } else {
            return super.getOArmor(loc, rear);
        }
    }

    /**
     * Sets the amount of armor in the location specified.  Mech version, handles
     * rear armor.
     */
    public void setArmor(int val, int loc, boolean rear) {
        if (rear && hasRearArmor(loc)) {
            rearArmor[loc] = val;
        } else {
            super.setArmor(val, loc, rear);
        }
    }

    /**
     * Initializes the rear armor on the mech. Sets the original and starting point
     * of the armor to the same number.
     */
      public void initializeRearArmor(int val, int loc) {
        orig_rearArmor[loc] = val;
        setArmor(val, loc, true);
      }
      
    /**
     * Returns the Compute.ARC that the weapon fires into.
     */
    public int getWeaponArc(int wn) {
        final Mounted mounted = getEquipment(wn);
        // rear mounted?
        if (mounted.isRearMounted()) {
            return Compute.ARC_REAR;
        }
        // front mounted
        switch(mounted.getLocation()) {
        case LOC_HEAD :
        case LOC_CT :
        case LOC_RT :
        case LOC_LT :
        case LOC_RLEG :
        case LOC_LLEG :
            return Compute.ARC_FORWARD;
        case LOC_RARM :
            return getArmsFlipped() ? Compute.ARC_REAR : Compute.ARC_RIGHTARM;
        case LOC_LARM :
            return getArmsFlipped() ? Compute.ARC_REAR : Compute.ARC_LEFTARM;
        default :
            return Compute.ARC_360;
        }
    }

    /**
     * Returns true if this weapon fires into the secondary facing arc.  If
     * false, assume it fires into the primary.
     */
    public boolean isSecondaryArcWeapon(int weaponId) {
        // leg-mounted weapons fire into the primary arc, always
        if (getEquipment(weaponId).getLocation() == LOC_RLEG || getEquipment(weaponId).getLocation() == LOC_LLEG) {
            return false;
        }
        // other weapons into the secondary
        return true;
    }
    
    /**
     * Rolls up a hit location
     */
    public HitData rollHitLocation(int table, int side) {
        if(table == ToHitData.HIT_NORMAL) {
            if(side == ToHitData.SIDE_FRONT) {
                // normal front hits
                switch(Compute.d6(2)) {
                case 2:
                    return tac(table, side, Mech.LOC_CT, false);
                case 3:
                case 4:
                    return new HitData(Mech.LOC_RARM);
                case 5:
                    return new HitData(Mech.LOC_RLEG);
                case 6:
                    return new HitData(Mech.LOC_RT);
                case 7:
                    return new HitData(Mech.LOC_CT);
                case 8:
                    return new HitData(Mech.LOC_LT);
                case 9:
                    return new HitData(Mech.LOC_LLEG);
                case 10:
                case 11:
                    return new HitData(Mech.LOC_LARM);
                case 12:
                    return new HitData(Mech.LOC_HEAD);
                }
            }
            if(side == ToHitData.SIDE_LEFT) {
                // normal left side hits
                switch(Compute.d6(2)) {
                case 2:
                    return tac(table, side, Mech.LOC_LT, false);
                case 3:
                    return new HitData(Mech.LOC_LLEG);
                case 4:
                case 5:
                    return new HitData(Mech.LOC_LARM);
                case 6:
                    return new HitData(Mech.LOC_LLEG);
                case 7:
                    return new HitData(Mech.LOC_LT);
                case 8:
                    return new HitData(Mech.LOC_CT);
                case 9:
                    return new HitData(Mech.LOC_RT);
                case 10:
                    return new HitData(Mech.LOC_RARM);
                case 11:
                    return new HitData(Mech.LOC_RLEG);
                case 12:
                    return new HitData(Mech.LOC_HEAD);
                }
            }
            if(side == ToHitData.SIDE_RIGHT) {
                // normal right side hits
                switch(Compute.d6(2)) {
                case 2:
                    return tac(table, side, Mech.LOC_RT, false);
                case 3:
                    return new HitData(Mech.LOC_RLEG);
                case 4:
                case 5:
                    return new HitData(Mech.LOC_RARM);
                case 6:
                    return new HitData(Mech.LOC_RLEG);
                case 7:
                    return new HitData(Mech.LOC_RT);
                case 8:
                    return new HitData(Mech.LOC_CT);
                case 9:
                    return new HitData(Mech.LOC_LT);
                case 10:
                    return new HitData(Mech.LOC_LARM);
                case 11:
                    return new HitData(Mech.LOC_LLEG);
                case 12:
                    return new HitData(Mech.LOC_HEAD);
                }
            }
            if(side == ToHitData.SIDE_REAR) {
                // normal rear hits
                switch(Compute.d6(2)) {
                case 2:
                    return tac(table, side, Mech.LOC_CT, true);
                case 3:
                case 4:
                    return new HitData(Mech.LOC_RARM, true);
                case 5:
                    return new HitData(Mech.LOC_RLEG, true);
                case 6:
                    return new HitData(Mech.LOC_RT, true);
                case 7:
                    return new HitData(Mech.LOC_CT, true);
                case 8:
                    return new HitData(Mech.LOC_LT, true);
                case 9:
                    return new HitData(Mech.LOC_LLEG, true);
                case 10:
                case 11:
                    return new HitData(Mech.LOC_LARM, true);
                case 12:
                    return new HitData(Mech.LOC_HEAD, true);
                }
            }
        }
        if(table == ToHitData.HIT_PUNCH) {
            if(side == ToHitData.SIDE_FRONT) {
                // front punch hits
                switch(Compute.d6(1)) {
                case 1:
                    return new HitData(Mech.LOC_LARM);
                case 2:
                    return new HitData(Mech.LOC_LT);
                case 3:
                    return new HitData(Mech.LOC_CT);
                case 4:
                    return new HitData(Mech.LOC_RT);
                case 5:
                    return new HitData(Mech.LOC_RARM);
                case 6:
                    return new HitData(Mech.LOC_HEAD);
                }
            }
            if(side == ToHitData.SIDE_LEFT) {
                // left side punch hits
                switch(Compute.d6(1)) {
                case 1:
                case 2:
                    return new HitData(Mech.LOC_LT);
                case 3:
                    return new HitData(Mech.LOC_CT);
                case 4:
                case 5:
                    return new HitData(Mech.LOC_LARM);
                case 6:
                    return new HitData(Mech.LOC_HEAD);
                }
            }
            if(side == ToHitData.SIDE_RIGHT) {
                // right side punch hits
                switch(Compute.d6(1)) {
                case 1:
                case 2:
                    return new HitData(Mech.LOC_RT);
                case 3:
                    return new HitData(Mech.LOC_CT);
                case 4:
                case 5:
                    return new HitData(Mech.LOC_RARM);
                case 6:
                    return new HitData(Mech.LOC_HEAD);
                }
            }
            if(side == ToHitData.SIDE_REAR) {
                // rear punch hits
                switch(Compute.d6(1)) {
                case 1:
                    return new HitData(Mech.LOC_LARM, true);
                case 2:
                    return new HitData(Mech.LOC_LT, true);
                case 3:
                    return new HitData(Mech.LOC_CT, true);
                case 4:
                    return new HitData(Mech.LOC_RT, true);
                case 5:
                    return new HitData(Mech.LOC_RARM, true);
                case 6:
                    return new HitData(Mech.LOC_HEAD, true);
                }
            }
        }
        if(table == ToHitData.HIT_KICK) {
            if(side == ToHitData.SIDE_FRONT || side == ToHitData.SIDE_REAR) {
                // front/rear kick hits
                switch(Compute.d6(1)) {
                case 1:
                case 2:
                case 3:
                    return new HitData(Mech.LOC_RLEG);
                case 4:
                case 5:
                case 6:
                    return new HitData(Mech.LOC_LLEG);
                }
            }
            if(side == ToHitData.SIDE_LEFT) {
                // left side kick hits
                return new HitData(Mech.LOC_LLEG);
            }
            if(side == ToHitData.SIDE_RIGHT) {
                // right side kick hits
                return new HitData(Mech.LOC_RLEG);
            }
        }
        return null;
    }
    
    /**
     * Called when a thru-armor-crit is rolled.  Checks the game options and
     * either returns no critical hit, rolls a floating crit, or returns a TAC 
     * in the specified location.
     */
    private HitData tac(int table, int side, int location, boolean rear) {
        if (game.getOptions().booleanOption("no_tac")) {
            return new HitData(location, rear);
        } else if (game.getOptions().booleanOption("floating_crits")) {
            HitData hd = rollHitLocation(table, side);
            return new HitData(hd.getLocation(), hd.isRear(), HitData.EFFECT_CRITICAL);
        } else {
            return new HitData(location, rear, HitData.EFFECT_CRITICAL);
        }
    }

    
    /**
     * Gets the location that excess damage transfers to
     */
    public HitData getTransferLocation(HitData hit) {
        switch(hit.getLocation()) {
        case LOC_RT :
        case LOC_LT :
            return new HitData(LOC_CT, hit.isRear());
        case LOC_LLEG :
        case LOC_LARM :
            return new HitData(LOC_LT, hit.isRear());
        case LOC_RLEG :
        case LOC_RARM :
            return new HitData(LOC_RT, hit.isRear());
        case LOC_HEAD :
        case LOC_CT :
        default:
            return new HitData(LOC_DESTROYED);
        }
    }
    
    /**
     * Gets the location that is destroyed recursively
     */
    public int getDependentLocation(int loc) {
        switch(loc) {
        case LOC_RT :
            return LOC_RARM;
        case LOC_LT :
            return LOC_LARM;
        case LOC_LLEG :
        case LOC_LARM :
        case LOC_RLEG :
        case LOC_RARM :
        case LOC_HEAD :
        case LOC_CT :
        default:
            return LOC_NONE;
        }
    }
    
    /**
     * Sets the internal structure for the mech.
     * 
     * @param head head
     * @param ct center torso
     * @param t right/left torso
     * @param arm right/left arm
     * @param leg right/left leg
     */
    public abstract void setInternal(int head, int ct, int t, int arm, int leg);
    
    /**
     * Set the internal structure to the appropriate value for the mech's
     * weight class
     */
    public void autoSetInternal() {
        // stupid irregular table... grr.
        switch ((int)weight) {
            //                     H, CT,TSO,ARM,LEG
            case 20  : setInternal(3,  6,  5,  3,  4); break;
            case 25  : setInternal(3,  8,  6,  4,  6); break;
            case 30  : setInternal(3, 10,  7,  5,  7); break;
            case 35  : setInternal(3, 11,  8,  6,  8); break;
            case 40  : setInternal(3, 12, 10,  6, 10); break;
            case 45  : setInternal(3, 14, 11,  7, 11); break;
            case 50  : setInternal(3, 16, 12,  8, 12); break;
            case 55  : setInternal(3, 18, 13,  9, 13); break;
            case 60  : setInternal(3, 20, 14, 10, 14); break;
            case 65  : setInternal(3, 21, 15, 10, 15); break;
            case 70  : setInternal(3, 22, 15, 11, 15); break;
            case 75  : setInternal(3, 23, 16, 12, 16); break;
            case 80  : setInternal(3, 25, 17, 13, 17); break;
            case 85  : setInternal(3, 27, 18, 14, 18); break;
            case 90  : setInternal(3, 29, 19, 15, 19); break;
            case 95  : setInternal(3, 30, 20, 16, 20); break;
            case 100 : setInternal(3, 31, 21, 17, 21); break;
        }
    }
    
    /**
     * Adds clan CASE in every location
     */
    public void addClanCase() {
        EquipmentType clCase = EquipmentType.getByInternalName("CLCASE");
        for (int i = 0; i < locations(); i++) {
            try {
                addEquipment(new Mounted(this, clCase), i, false);
            } catch (LocationFullException ex) {
                // um, that's impossible.
            }
        }
    }
    
    /**
     * Mounts the specified weapon in the specified location.
     */
    protected void addEquipment(Mounted mounted, int loc, boolean rearMounted)
        throws LocationFullException 
    {
        // if there's no actual location, then don't add criticals
        if (loc == LOC_NONE) {
            super.addEquipment(mounted, loc, rearMounted);
            return;
        }
        
        // spreadable or split equipment only gets added to 1 crit at a time, 
        // since we don't know how many are in this location
        int crits = mounted.getType().getCriticals(this);
        if (mounted.getType().isSpreadable() || mounted.isSplit()) {
            crits = 1;
        }
        
        // check criticals for space
        if(getEmptyCriticals(loc) < crits) {
            throw new LocationFullException(mounted.getName() + " does not fit in " + getLocationAbbr(loc) + " on " + getDisplayName());
        }
        
        // add it
        super.addEquipment(mounted, loc, rearMounted);

        // add criticals
        int num = getEquipmentNum(mounted);        
        
        for(int i = 0; i < crits; i++) {
            addCritical(loc, new CriticalSlot(CriticalSlot.TYPE_EQUIPMENT, num, mounted.getType().isHittable()));
        }        
    }    
  
    /**
     * Calculates the battle value of this mech
     */
    public int calculateBattleValue() {
        double dbv = 0; // defensive battle value
        double obv = 0; // offensive bv
        
        // total armor points
        dbv += getTotalArmor() * 2.0;
        
        // total internal structure
        double internalMultiplier;
        if (hasXL()) {
            internalMultiplier = isClan() ? 1.125 : 0.75;
        } else {
            internalMultiplier = 1.5;
        }
        
        dbv += getTotalInternal() * internalMultiplier;
        
        // add weight
        dbv += getWeight();
        
        // subtract for explosive ammo
        for (Enumeration i = ammoList.elements(); i.hasMoreElements();) {
            Mounted mounted = (Mounted)i.nextElement();
            int loc = mounted.getLocation();
            AmmoType atype = (AmmoType)mounted.getType();
            
            // only count explosive ammo
            if (!atype.isExplosive()) {
                continue;
            }
            
            if (isClan()) {
                // clan mechs only count ammo in ct, legs or head
                if (loc != LOC_CT && loc != LOC_RLEG && loc != LOC_LLEG 
                && loc != LOC_HEAD) {
                    continue;
                }
            } else {
                // inner sphere with XL counts everywhere
                if (!hasXL()) {
                    // without XL, only count torsos if not CASEed, and arms
                    // if arm & torso not CASEed
                    if ((loc == LOC_RT || loc == LOC_LT) && locationHasCase(loc)) {
                        continue;
                    }
                    if (loc == LOC_LARM && (locationHasCase(loc) || locationHasCase(LOC_LT))) {
                        continue;
                    }
                    if (loc == LOC_RARM && (locationHasCase(loc) || locationHasCase(LOC_RT))) {
                        continue;
                    }
                }
            }
            
            dbv -= (int)(20.0 * atype.getTonnage(this));
        }
        
        
        // total up maximum heat generated
        int maxumumHeatFront = 0;
        int maxumumHeatRear = 0;
        for (Enumeration i = weaponList.elements(); i.hasMoreElements();) {
            Mounted mounted = (Mounted)i.nextElement();
            WeaponType wtype = (WeaponType)mounted.getType();
            if (mounted.isRearMounted()) {
                maxumumHeatRear += wtype.getHeat();
            } else {
                maxumumHeatFront += wtype.getHeat();
            }
        }
        int maximumHeat = Math.max(maxumumHeatFront, maxumumHeatRear);
        if (getJumpMP() > 0) {
            maximumHeat += Math.max(3, getJumpMP());
        } else {
            maximumHeat += 2;
        }
        // adjust for heat efficiency
        if (maximumHeat > getHeatCapacity()) {
            dbv -= ((maximumHeat - getHeatCapacity()) * 5);
        }
        
        // adjust for target movement modifier
        int tmmRan = Compute.getTargetMovementModifier(getRunMP(), false).getValue();
        int tmmJumped = Compute.getTargetMovementModifier(getJumpMP(), true).getValue();
        int targetMovementModidifer = Math.max(tmmRan, tmmJumped);
        if (targetMovementModidifer > 5) {
            targetMovementModidifer = 5;
        }
        double[] tmmFactors = { 1.0, 1.1, 1.2, 1.3, 1.4, 1.5 };
        dbv *= tmmFactors[targetMovementModidifer];
        
        double weaponBV = 0;
        
        // figure out base weapon bv
        double weaponsBVFront = 0;
        double weaponsBVRear = 0;
        for (Enumeration i = weaponList.elements(); i.hasMoreElements();) {
            Mounted mounted = (Mounted)i.nextElement();
            WeaponType wtype = (WeaponType)mounted.getType();
            if (mounted.isRearMounted()) {
                weaponsBVRear += wtype.getBV(this);
            } else {
                weaponsBVFront += wtype.getBV(this);
            }
        }
        if (weaponsBVFront > weaponsBVRear) {
            weaponBV += weaponsBVFront;
            weaponBV += (weaponsBVRear * 0.5);
        } else {
            weaponBV += weaponsBVRear;
            weaponBV += (weaponsBVFront * 0.5);
        }
        
        // add ammo bv
        double ammoBV = 0;
        for (Enumeration i = ammoList.elements(); i.hasMoreElements();) {
            Mounted mounted = (Mounted)i.nextElement();
            AmmoType atype = (AmmoType)mounted.getType();
            ammoBV += atype.getBV(this);
        }
        weaponBV += ammoBV;
        
        // adjust for heat efficiency
        if (maximumHeat > getHeatCapacity()) {
            double x = (getHeatCapacity()  * weaponBV) / maximumHeat;
            double y = (weaponBV - x) / 2;
            weaponBV = x + y;
        }
        
        // adjust further for speed factor
        double speedFactor = getRunMP() + getJumpMP() - 5;
        speedFactor /= 10;
        speedFactor++;
        speedFactor = Math.pow(speedFactor, 1.2);
        speedFactor = Math.round(speedFactor * 100) / 100.0;
        
        obv = weaponBV * speedFactor;
        
        // and then factor in pilot
        double pilotFactor = crew.getBVSkillMultiplier();
        
        return (int)Math.round((dbv + obv) * pilotFactor);
    }
    
    /**
     * Returns an end-of-battle report for this mech
     */
    public String victoryReport() {
        StringBuffer report = new StringBuffer();
        
        report.append(getDisplayName());
        report.append('\n');
        report.append("Pilot : " + crew.getDesc());
        report.append('\n');
        
        return report.toString();
    }
  
    /**
     * Add in any piloting skill mods
     */
      public PilotingRollData addEntityBonuses(PilotingRollData roll) {
        // gyro hit?
          if (getDestroyedCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO, Mech.LOC_CT) > 0) {
            roll.addModifier(3, "Gyro damaged");
          }
        
        return roll;
      }
      
    public int getMaxElevationChange() {
        return 2;
    }
}

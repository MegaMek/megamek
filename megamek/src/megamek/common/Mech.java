/**
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
public class Mech
    extends Entity
    implements Serializable
{
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
    public static final int        SYSTEM_HEAT_SINK    = 5;
    public static final int        SYSTEM_JUMP_JET        = 6;

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
        "Engine", "Gyro", "Heat Sink", "Jump Jet", "Shoulder", "Upper Arm", 
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
    
    public static final String[] locationNames = {"Head",
        "Center Torso", "Right Torso", "Left Torso", 
//        "Center Torso Rear", "Right Torso Rear", "Left Torso Rear", 
        "Right Arm", "Left Arm", "Right Leg", "Left Leg"};
    
    public static final String[] locationAbbrs = {"HD", "CT", "RT",
        "LT", /*"CTR", "RTR", "LTR",*/ "RA", "LA", "RL", "LL"};
    
    // critical hit slots
    public static final int[] noOfSlots = {6, 12, 12, 12, 12, 12, 6, 6};
    
    // rear armor
    private int[] rearArmor;
    
    /**
     * Construct a new, blank, mech.
     */
    public Mech() {
        super();
        
        rearArmor = new int[locations()];
        
        for (int i = 0; i < locations(); i++) {
            if (!hasRearArmor(i)) {
                setArmor(ARMOR_NA, i, true);
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
        
        setCritical(LOC_RARM, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_SHOULDER));
        setCritical(LOC_RARM, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_UPPER_ARM));
        setCritical(LOC_RARM, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_LOWER_ARM));
        setCritical(LOC_RARM, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_HAND));

        setCritical(LOC_LARM, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_SHOULDER));
        setCritical(LOC_LARM, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_UPPER_ARM));
        setCritical(LOC_LARM, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_LOWER_ARM));
        setCritical(LOC_LARM, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_HAND));
        
        setCritical(LOC_RLEG, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_HIP));
        setCritical(LOC_RLEG, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_UPPER_LEG));
        setCritical(LOC_RLEG, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_LOWER_LEG));
        setCritical(LOC_RLEG, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_FOOT));

        setCritical(LOC_LLEG, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_HIP));
        setCritical(LOC_LLEG, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_UPPER_LEG));
        setCritical(LOC_LLEG, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_LOWER_LEG));
        setCritical(LOC_LLEG, 3, new CriticalSlot(CriticalSlot.TYPE_SYSTEM, ACTUATOR_FOOT));
    }
    

    /**
     * Returns the number of locations in the entity
     */
    public int locations() {
        return 8;
    }
                                    
    /**
     * Returns this entity's walking/cruising mp, factored
     * for heat and leg damage.
     */
    public int getWalkMP() {
        int wmp = getOriginalWalkMP();
        int legsDestroyed = 0;
        int hipHits = 0;
        int actuatorHits = 0;
        // count leg damage, right leg
        if(getInternal(Mech.LOC_RLEG) > 0) {
            // hip hit reduces mp by half and ignores other hits
            if (getGoodCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_HIP, Mech.LOC_RLEG) == 0) {
                hipHits++;
            } else {
                // check for other leg actuators
                if(getGoodCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_UPPER_LEG, Mech.LOC_RLEG) == 0) {
                    actuatorHits++;
                }
                if(getGoodCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_LOWER_LEG, Mech.LOC_RLEG) == 0) {
                    actuatorHits++;
                }
                if(getGoodCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_FOOT, Mech.LOC_RLEG) == 0) {
                    actuatorHits++;
                }
            }
        } else {
            legsDestroyed++;
        }
        // left leg
        if(getInternal(Mech.LOC_LLEG) > 0) {
            // hip hit reduces mp by half and ignores other hits
            if(getGoodCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_HIP, Mech.LOC_LLEG) == 0) {
                hipHits++;
            } else {
                // check for other leg actuators
                if(getGoodCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_UPPER_LEG, Mech.LOC_LLEG) == 0) {
                    actuatorHits++;
                }
                if(getGoodCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_LOWER_LEG, Mech.LOC_LLEG) == 0) {
                    actuatorHits++;
                }
                if(getGoodCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_FOOT, Mech.LOC_LLEG) == 0) {
                    actuatorHits++;
                }
            }
        } else {
            legsDestroyed++;
        }
        // leg damage effects
        if(legsDestroyed > 0) {
            wmp = (legsDestroyed == 1) ? 1 : 0;
        } else {
            if(hipHits > 0) {
                wmp = (hipHits == 1) ? (int)Math.floor((double)wmp / 2.0) : 0;
            } else {
                wmp -= actuatorHits;
            }
        }
        // and we still need to factor in heat!
        return Math.max(wmp - (int)(heat / 5), 0);
    }

    /**
     * Returns this mech's running/flank mp modified for leg loss & stuff.
     */
    public int getRunMP() {
        if(getInternal(Mech.LOC_RLEG) > 0 && getInternal(Mech.LOC_LLEG) > 0) {
            return (int)Math.ceil(getWalkMP() * 1.5);
        } else {
            return getWalkMP();
        }
    }
    
    /**
     * This mech's jumping MP modified for missing jump jets
     */
    public int getJumpMP() {
        int jump = 0;
        
        for (int i = 0; i < locations(); i++) {
            jump += getGoodCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_JUMP_JET, i);
        }
        
        return jump;
    }
    
    /**
     * Returns the about of heat that the entity can sink each 
     * turn.
     */
    public int getHeatCapacity() {
        int capacity = super.getHeatCapacity();
        
        for (int i = 0; i < locations(); i++) {
            capacity -= getDestroyedCriticals(CriticalSlot.TYPE_SYSTEM, 
                                              Mech.SYSTEM_HEAT_SINK, i);
        }
        
        return capacity;
    }
    
    /**
     * Returns the about of heat that the entity can sink each 
     * turn, factoring for water.
     */
    public int getHeatCapacityWithWater(Game game) {
        int capacity = getHeatCapacity();
        int sinksUnderwater = 0;
        final Hex curHex = game.board.getHex(getPosition());
        // are we even in water?  is it depth 1+
        if (curHex.getTerrainType() != Terrain.WATER
            || curHex.getElevation() >= 0) {
            return capacity;
        } else if (curHex.getElevation() == -1) {
            sinksUnderwater += getGoodCriticals(CriticalSlot.TYPE_SYSTEM,
                                        Mech.SYSTEM_HEAT_SINK, Mech.LOC_RLEG);
            sinksUnderwater += getGoodCriticals(CriticalSlot.TYPE_SYSTEM,
                                        Mech.SYSTEM_HEAT_SINK, Mech.LOC_LLEG);
        } else if (curHex.getElevation() <= -2) {
            sinksUnderwater = getHeatCapacity();
        }
        
        return capacity + Math.min(sinksUnderwater, 6);
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
     * Can this mech torso twist in the given direction?
     */
    public boolean isValidSecondaryFacing(int dir) {
        int rotate = dir - getFacing();
        if (isProne()) {
            return rotate == 0;
        } else {
            return rotate == 0 || rotate == 1 || rotate == -1 || rotate == -5;
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
        if (isProne()) {
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
     * Returns the name of the location specified.
     */
    public String getLocationName(HitData hit) {
        return locationNames[hit.getLocation()];
    }
    
    /**
     * Returns the abbreviated name of the location specified.
     */
    public String getLocationAbbr(HitData hit) {
        return locationAbbrs[hit.getLocation()] + (hit.isRear() && hasRearArmor(hit.getLocation()) ? "R" : "") + (hit.getEffect() == HitData.EFFECT_CRITICAL ? " (critical)" : "");
    }
  
    /**
     * Returns the name of the location specified.
     */
    public String getLocationName(int loc) {
        return locationNames[loc];
    }
    
    /**
     * Returns the abbreviated name of the location specified.
     */
    public String getLocationAbbr(int loc) {
        return locationAbbrs[loc];
    }
  
    /**
     * Returns the location that the specified abbreviation indicates
     */
    public int getLocationFromAbbr(String abbr) {
        for (int i = 0; i < locationAbbrs.length; i++) {
            if (getLocationAbbr(i).equalsIgnoreCase(abbr)) {
                return i;
            }
        }
      return this.LOC_NONE;
    }
    
    /**
     * Returns the Compute.ARC that the weapon fires into.
     */
    public int getWeaponArc(int wn) {
        final MountedWeapon weapon = getWeapon(wn);
        // rear mounted?
        if (weapon.isRearMounted()) {
            return Compute.ARC_REAR;
        }
        // front mounted
        switch(weapon.getLocation()) {
        case LOC_HEAD :
        case LOC_CT :
        case LOC_RT :
        case LOC_LT :
        case LOC_RLEG :
        case LOC_LLEG :
            return Compute.ARC_FORWARD;
        case LOC_RARM :
            return Compute.ARC_RIGHTARM;
        case LOC_LARM :
            return Compute.ARC_LEFTARM;
        default :
            return Compute.ARC_360;
        }
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
                    return new HitData(Mech.LOC_CT, false, HitData.EFFECT_CRITICAL);
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
                    return new HitData(Mech.LOC_LT, false, HitData.EFFECT_CRITICAL);
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
                    return new HitData(Mech.LOC_RT, false, HitData.EFFECT_CRITICAL);
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
                    return new HitData(Mech.LOC_CT, true, HitData.EFFECT_CRITICAL);
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
     * Sets the armor for the mech.
     * 
     * @param head head
     * @param ct center torso
     * @param ctr center torso rear
     * @param t right/left torso
     * @param tr right/left torso rear
     * @param arm right/left arm
     * @param leg right/left leg
     */
    public void setArmor(int head, int ct, int ctr, int t, 
                         int tr, int arm, int leg) {
        armor[LOC_HEAD] = head;
        armor[LOC_CT] = ct;
        rearArmor[LOC_CT] = ctr;
        armor[LOC_RT] = t;
        armor[LOC_LT] = t;
        rearArmor[LOC_RT] = tr;
        rearArmor[LOC_LT] = tr;
        armor[LOC_RARM] = arm;
        armor[LOC_LARM] = arm;
        armor[LOC_RLEG] = leg;
        armor[LOC_LLEG] = leg;
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
    public void setInternal(int head, int ct, int t, int arm, int leg) {
        internal[LOC_HEAD] = head;
        internal[LOC_CT] = ct;
        internal[LOC_RT] = t;
        internal[LOC_LT] = t;
        internal[LOC_RARM] = arm;
        internal[LOC_LARM] = arm;
        internal[LOC_RLEG] = leg;
        internal[LOC_LLEG] = leg;
    }
    
    /**
     * Returns the number of total critical slots in a location
     */
    public int getNumberOfCriticals(int loc) {
        return noOfSlots[loc];
    }
    
    /**
     * Mounts the specified weapon in the specified location.
     * 
     * Throw exception if full, maybe?
     */
    public void addWeapon(MountedWeapon w, int loc) {
        if(getEmptyCriticals(loc) < w.getType().getCriticals()) {
            System.err.println("mech: tried to add weapon to full location");
            return;
        }
        
        w.setLocation(loc);
        this.weapons.addElement(w);
        
        int wn = this.weapons.indexOf(w);

        for(int i = 0; i < w.getType().getCriticals(); i++) {
            addCritical(loc, new CriticalSlot(CriticalSlot.TYPE_WEAPON, wn));
        }        
    }    

    
    /**
     * Adds the specified ammo to the specified location.
     */
    public void addAmmo(Ammo a, int loc) {
        if(getEmptyCriticals(loc) < 1) {
            System.err.println("mech: tried to add ammo to full location");
            return;
        }
        
        a.location = loc;
        this.ammo.addElement(a);
        
        int idx = this.ammo.indexOf(a);

        addCritical(loc, new CriticalSlot(CriticalSlot.TYPE_AMMO, idx));
    }    
  
  /**
   * Calculates the battle value of this mech
   */
  public int calculateBattleValue() {
    double dbv = 0; // defensive battle value
    double obv = 0; // offensive bv
    
    // total armor points
    dbv += getTotalArmor() * 2;
    
    // total internal structure
    dbv += getTotalInternal() * 1.5;
    
    // add weight
    dbv += getWeight();
    
    // subtract for explosive ammo
    dbv -= (ammo.size() * 20);
    
    // total up maximum heat generated
    int maxumumHeatFront = 0;
    int maxumumHeatRear = 0;
    for (Enumeration i = weapons.elements(); i.hasMoreElements();) {
      MountedWeapon weapon = (MountedWeapon)i.nextElement();
      if (weapon.isRearMounted()) {
        maxumumHeatRear += weapon.getType().getHeat();
      } else {
        maxumumHeatFront += weapon.getType().getHeat();
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
    int weaponsBVFront = 0;
    int weaponsBVRear = 0;
    for (Enumeration i = weapons.elements(); i.hasMoreElements();) {
      MountedWeapon weapon = (MountedWeapon)i.nextElement();
      if (weapon.isRearMounted()) {
        weaponsBVRear += weapon.getType().getBV();
      } else {
        weaponsBVFront += weapon.getType().getBV();
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
    int ammoBV = 0;
    for (Enumeration i = ammo.elements(); i.hasMoreElements();) {
      Ammo ammunition = (Ammo)i.nextElement();
      ammoBV += ammunition.getBV();
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
}

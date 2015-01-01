/*
* MegaAero - Copyright (C) 2007 Jay Lawson
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
 * Created on Jun 17, 2007
 *
 */
package megamek.common;

import java.io.Serializable;

/**
 * @author Jay Lawson
 */
public class SmallCraft extends Aero implements Serializable {
           
    /**
     * 
     */
    private static final long serialVersionUID = 6708788176436555036L;
    protected static String[] LOCATION_ABBRS = { "NOS", "LS", "RS", "AFT" };
    protected static String[] LOCATION_NAMES = { "Nose", "Left Side", "Right Side", "Aft" };
    
    //crew and passengers
    private int nCrew = 0;
    private int nPassenger = 0;
    
    public void setNCrew(int crew) {
        this.nCrew = crew;
    }
    
    public void setNPassenger(int pass) {
        this.nPassenger = pass;
    }
    
    public int getNCrew() {
        return nCrew;
    }
    
    public int getNPassenger() {
         return nPassenger;
    }
    
    public String[] getLocationAbbrs() {
        return LOCATION_ABBRS;
    }
    
    public String[] getLocationNames() { 
        return LOCATION_NAMES; 
    }
  
    public int locations() {
        return 4;
   }
    
    public void setEngine(Engine e) {
        engine = e;
    }
    
    //what is different - hit table is about it
    public HitData rollHitLocation(int table, int side) {

        /* 
         * Unlike other units, ASFs determine potential crits based on the to-hit roll
         * so I need to set this potential value as well as return the to hit data
         */

        int roll = Compute.d6(2);
        
        if(table == ToHitData.HIT_ABOVE || table == ToHitData.HIT_BELOW) {
            
            //have to decide which wing
            int wingloc = LOC_RWING;
            int wingroll = Compute.d6(1);
            if(wingroll > 3) {
                wingloc = LOC_LWING;
            }
            switch( roll ) {
            case 2:
                setPotCrit(CRIT_WEAPON);
                return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
            case 3:
                setPotCrit(CRIT_FCS);
                return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
            case 4:
                setPotCrit(CRIT_SENSOR);
                return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
            case 5:
                setPotCrit(CRIT_RIGHT_THRUSTER);
                if(wingroll > 3) {
                    setPotCrit(CRIT_LEFT_THRUSTER);
                }
                return new HitData(wingloc, false, HitData.EFFECT_NONE);
            case 6:
                setPotCrit(CRIT_CARGO);
                return new HitData(wingloc, false, HitData.EFFECT_NONE);
            case 7:
                setPotCrit(CRIT_WEAPON);
                return new HitData(wingloc, false, HitData.EFFECT_NONE);
            case 8:
                setPotCrit(CRIT_DOOR);
                return new HitData(wingloc, false, HitData.EFFECT_NONE);
            case 9:
                setPotCrit(CRIT_RIGHT_THRUSTER);
                if(wingroll > 3) {
                    setPotCrit(CRIT_LEFT_THRUSTER);
                }
                return new HitData(wingloc, false, HitData.EFFECT_NONE);
            case 10:
                setPotCrit(CRIT_AVIONICS);
                return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
            case 11:
                setPotCrit(CRIT_ENGINE);
                return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
            case 12:
                setPotCrit(CRIT_WEAPON);
                return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
            }
        }
       
        if(side == ToHitData.SIDE_FRONT) {
            // normal front hits
            switch( roll ) {
            case 2:
                setPotCrit(CRIT_CREW);
                return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
            case 3:
                setPotCrit(CRIT_AVIONICS);
                return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
            case 4:
                setPotCrit(CRIT_WEAPON);
                return new HitData(LOC_RWING, false, HitData.EFFECT_NONE);
            case 5:
                setPotCrit(CRIT_RIGHT_THRUSTER);
                return new HitData(LOC_RWING, false, HitData.EFFECT_NONE);
            case 6:
                setPotCrit(CRIT_FCS);
                return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
            case 7:
                setPotCrit(CRIT_WEAPON);
                return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
            case 8:
                setPotCrit(CRIT_CONTROL);
                return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
            case 9:
                setPotCrit(CRIT_LEFT_THRUSTER);
                return new HitData(LOC_LWING, false, HitData.EFFECT_NONE);
            case 10:
                setPotCrit(CRIT_WEAPON);
                return new HitData(LOC_LWING, false, HitData.EFFECT_NONE);
            case 11:
                setPotCrit(CRIT_SENSOR);
                return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
            case 12:
                setPotCrit(CRIT_KF_BOOM);
                return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
            }
        }
        else if(side == ToHitData.SIDE_LEFT) {
            // normal left-side hits
            switch( roll ) {
            case 2:
                setPotCrit(CRIT_WEAPON);
                return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
            case 3:
                setPotCrit(CRIT_FCS);
                return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
            case 4:
                setPotCrit(CRIT_SENSOR);
                return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
            case 5:
                setPotCrit(CRIT_LEFT_THRUSTER);
                return new HitData(LOC_LWING, false, HitData.EFFECT_NONE);
            case 6:
                setPotCrit(CRIT_CARGO);
                return new HitData(LOC_LWING, false, HitData.EFFECT_NONE);
            case 7:
                setPotCrit(CRIT_WEAPON);
                return new HitData(LOC_LWING, false, HitData.EFFECT_NONE);
            case 8:
                setPotCrit(CRIT_DOOR);
                return new HitData(LOC_LWING, false, HitData.EFFECT_NONE);
            case 9:
                setPotCrit(CRIT_LEFT_THRUSTER);
                return new HitData(LOC_LWING, false, HitData.EFFECT_NONE);
            case 10:
                setPotCrit(CRIT_AVIONICS);
                return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
            case 11:
                setPotCrit(CRIT_ENGINE);
                return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
            case 12:
                setPotCrit(CRIT_WEAPON);
                return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
            }
        }
        else if(side == ToHitData.SIDE_RIGHT) {
            // normal right-side hits
            switch( roll ) {
            case 2:
                setPotCrit(CRIT_WEAPON);
                return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
            case 3:
                setPotCrit(CRIT_FCS);
                return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
            case 4:
                setPotCrit(CRIT_SENSOR);
                return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
            case 5:
                setPotCrit(CRIT_RIGHT_THRUSTER);
                return new HitData(LOC_RWING, false, HitData.EFFECT_NONE);
            case 6:
                setPotCrit(CRIT_CARGO);
                return new HitData(LOC_RWING, false, HitData.EFFECT_NONE);
            case 7:
                setPotCrit(CRIT_WEAPON);
                return new HitData(LOC_RWING, false, HitData.EFFECT_NONE);
            case 8:
                setPotCrit(CRIT_DOOR);
                return new HitData(LOC_RWING, false, HitData.EFFECT_NONE);
            case 9:
                setPotCrit(CRIT_RIGHT_THRUSTER);
                return new HitData(LOC_RWING, false, HitData.EFFECT_NONE);
            case 10:
                setPotCrit(CRIT_AVIONICS);
                return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
            case 11:
                setPotCrit(CRIT_ENGINE);
                return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
            case 12:
                setPotCrit(CRIT_WEAPON);
                return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
            }
        }
        else if(side == ToHitData.SIDE_REAR) {
            // normal aft hits
            switch( roll ) {
            case 2:
                setPotCrit(CRIT_LIFE_SUPPORT);
                return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
            case 3:
                setPotCrit(CRIT_CONTROL);
                return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
            case 4:
                setPotCrit(CRIT_WEAPON);
                return new HitData(LOC_RWING, false, HitData.EFFECT_NONE);
            case 5:
                setPotCrit(CRIT_DOOR);
                return new HitData(LOC_RWING, false, HitData.EFFECT_NONE);
            case 6:
                setPotCrit(CRIT_ENGINE);
                return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
            case 7:
                setPotCrit(CRIT_WEAPON);
                return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
            case 8:
                setPotCrit(CRIT_DOCK_COLLAR);
                return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
            case 9:
                setPotCrit(CRIT_DOOR);
                return new HitData(LOC_LWING, false, HitData.EFFECT_NONE);
            case 10:
                setPotCrit(CRIT_WEAPON);
                return new HitData(LOC_LWING, false, HitData.EFFECT_NONE);
            case 11:
                setPotCrit(CRIT_GEAR);
                return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
            case 12:
                setPotCrit(CRIT_FUEL_TANK);
                return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
            }
        }    
            return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
        }
    
    //weapon arcs
    public int getWeaponArc(int wn) {
        final Mounted mounted = getEquipment(wn);
        
        int arc = Compute.ARC_NOSE;
        if(!isSpheroid()) {
            switch (mounted.getLocation()) {
                case LOC_NOSE:
                    arc = Compute.ARC_NOSE;
                    break;
                case LOC_RWING:
                    if(mounted.isRearMounted()) {
                        arc = Compute.ARC_RWINGA;
                    } else {
                        arc = Compute.ARC_RWING;
                    }
                    break;
                case LOC_LWING:
                    if(mounted.isRearMounted()) {
                        arc = Compute.ARC_LWINGA;
                    } else {
                        arc = Compute.ARC_LWING;
                    }
                    break;
                case LOC_AFT:
                    arc = Compute.ARC_AFT;
                    break;
                default:
                    arc = Compute.ARC_360;
            }
        } else {
            switch (mounted.getLocation()) {
            case LOC_NOSE:
                arc = Compute.ARC_NOSE;
                break;
            case LOC_RWING:
                if(mounted.isRearMounted()) {
                    arc = Compute.ARC_RIGHTSIDEA_SPHERE;
                } else {
                    arc = Compute.ARC_RIGHTSIDE_SPHERE;
                }
                break;
            case LOC_LWING:
                if(mounted.isRearMounted()) {
                    arc = Compute.ARC_LEFTSIDEA_SPHERE;
                } else {
                    arc = Compute.ARC_LEFTSIDE_SPHERE;
                }
                break;
            case LOC_AFT:
                arc = Compute.ARC_AFT;
                break;
            default:
                arc = Compute.ARC_360;
            }
        
        } 
        
        return rollArcs(arc);
        
    }
    
    public int getArcswGuns() {
        //return the number 
        int nArcs = 0;
        for(int i = 0; i < locations(); i++) {
                if(hasWeaponInArc(i, false))
                    nArcs++;
                //check for rear locations
                if(hasWeaponInArc(i, true))
                    nArcs++;
        }      
        return nArcs;
    }
    
    public boolean hasWeaponInArc(int loc, boolean rearMount) {
        boolean hasWeapons = false;
        for(Mounted weap: getWeaponList()) {
            if(weap.getLocation() == loc && weap.isRearMounted() == rearMount) {
                hasWeapons = true;
            }
        }
        return hasWeapons;
    }
    
    public double getArmorWeight() {
        //first I need to subtract SI bonus from total armor
        int armorPoints = getTotalOArmor();
        armorPoints -= getSI() * locations();
        //this roundabout method is actually necessary to avoid rounding weirdness.  Yeah, it's dumb.
        //now I need to determine base armor points by type and weight
        
        double baseArmor = 16.0;
        if(isClan()) {
            baseArmor = 20.0;
        }
        if(isSpheroid()) {
            if(weight >= 12500) {
                baseArmor = 14.0;
                if(isClan()) {
                    baseArmor = 17.0;
                }
            } else if(weight >= 20000) {
                baseArmor = 12.0;
                if(isClan()) {
                    baseArmor = 14.0;
                }
            } else if(weight >= 35000) {
                baseArmor = 10.0;
                if(isClan()) {
                    baseArmor = 12.0;
                }
            } else if(weight >= 50000) {
                baseArmor = 8.0;
                if(isClan()) {
                    baseArmor = 10.0;
                }
            } else if(weight >= 65000) {
                baseArmor = 6.0;
                if(isClan()) {
                    baseArmor = 7.0;
                }
            }
        } else {
            if(weight >= 6000) {
                baseArmor = 14.0;
                if(isClan()) {
                    baseArmor = 17.0;
                }
            } else if(weight >= 9500) {
                baseArmor = 12.0;
                if(isClan()) {
                    baseArmor = 14.0;
                }
            } else if(weight >= 12500) {
                baseArmor = 10.0;
                if(isClan()) {
                    baseArmor = 12.0;
                }
            } else if(weight >= 17500) {
                baseArmor = 8.0;
                if(isClan()) {
                    baseArmor = 10.0;
                }
            } else if(weight >= 25000) {
                baseArmor = 6.0;
                if(isClan()) {
                    baseArmor = 7.0;
                }
            }
        }
        
        double armorPerTon = baseArmor*EquipmentType.getArmorPointMultiplier(armorType,techLevel);
        double weight=0.0;
        for(;((int)Math.round(weight*armorPerTon))<armorPoints;weight+=.5) {}
        return weight;
    }
    
    /*There is a mistake in some of the AT2r costs
     * for some reason they added ammo twice for a lot of the 
     * level 2 designs, leading to costs that are too high
     */
    public double getCost() {
        
        double cost = 0;

        //add in controls
        //bridge
        cost += 200000 + 10 * weight;
        //computer
        cost += 200000;
        //life support
        cost += 5000 * (getNCrew() + getNPassenger());
        //sensors
        cost += 80000;
        //fcs
        cost += 100000;
        //gunnery/control systems
        cost += 10000 * getArcswGuns();
        
        //structural integrity
        cost += 100000 * getSI();
        
        //additional flight systems (attitude thruster and landing gear)
        cost += 25000 + 10 * getWeight();
        
        //engine
        double engineMultiplier = 0.065;
        if(isClan()) {
            engineMultiplier = 0.061;
        }
        double engineWeight = getOriginalWalkMP() * weight * engineMultiplier;
        cost += engineWeight * 1000;
        //drive unit
        cost += 500 * getOriginalWalkMP() * weight / 100.0;
  
        //fuel tanks
        cost += 200 * getFuel() / 80.0;

        //armor
        cost += getArmorWeight()*EquipmentType.getArmorCost(armorType);
        
        //heat sinks
        int sinkCost = 2000 + 4000 * getHeatType();// == HEAT_DOUBLE ? 6000: 2000;    
        cost += sinkCost*getHeatSinks();
        
        //weapons 
        cost += getWeaponsAndEquipmentCost();
        
        double weightMultiplier = 1 + (weight / 50f);
        
        return Math.round(cost * weightMultiplier);
        
    }
    
    
    public int getMaxEngineHits() {
        return 6;
    }

    public double getBVTypeModifier() {
        return 1.0;
    }
    
    /**
     * need to check bay location before loading ammo
     */
    public boolean loadWeapon(Mounted mounted, Mounted mountedAmmo) {
        boolean success = false;
        WeaponType wtype = (WeaponType) mounted.getType();
        AmmoType atype = (AmmoType) mountedAmmo.getType();
        
        if(mounted.getLocation() != mountedAmmo.getLocation())
            return success;
        
        if (mountedAmmo.isAmmoUsable() && !wtype.hasFlag(WeaponType.F_ONESHOT)
                && atype.getAmmoType() == wtype.getAmmoType()
                && atype.getRackSize() == wtype.getRackSize()) {
            mounted.setLinked(mountedAmmo);
            success = true;
        }
        return success;
    }
    
    /*
     * (non-Javadoc)
     * @see megamek.common.Entity#getTotalCommGearTons()
     */
    public int getTotalCommGearTons() {
        return 3 + getExtraCommGearTons();
    }
    
    /**
     * All military small craft automatically have ECM if in space
     */
    public boolean hasActiveECM() {
        if(!game.getOptions().booleanOption("stratops_ecm") || !game.getBoard().inSpace()) {
            return super.hasActiveECM();
        }
        return getECMRange() >= 0;
    }
    
    /**
     * What's the range of the ECM equipment? 
     * 
     * @return the <code>int</code> range of this unit's ECM. This value will
     *         be <code>Entity.NONE</code> if no ECM is active.
     */
    public int getECMRange() {
        if(!game.getOptions().booleanOption("stratops_ecm") || !game.getBoard().inSpace()) {
            return super.getECMRange();
        }
        if(!this.isMilitary()) {
            return Entity.NONE;
        }
        int range = -1;      
        //if the unit has an ECM unit, then the range might be extended by one
        if ( !isShutDown() ){
            for (Mounted m : getMisc()) {
                EquipmentType type = m.getType();
                if (type instanceof MiscType && type.hasFlag(MiscType.F_ECM) && !m.isInoperable()) {
                    if(type.hasFlag(MiscType.F_SINGLE_HEX_ECM)) {
                        range += 1;
                    } else {
                        range += 2;
                    }
                    break;
                }
            }          
        }
        //the range might be affected by sensor/FCS damage
        range = range - getFCSHits() - getSensorHits();     
        return range;
    }
    
    /**
     * @return is  the crew of this vessel protected from gravitational effects, see StratOps, pg. 36
     */
    public boolean isCrewProtected() {
        return isMilitary() && this.getOriginalWalkMP() > 4;
    }
}
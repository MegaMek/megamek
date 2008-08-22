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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import megamek.common.weapons.BayWeapon;

/**
 * @author Jay Lawson
 */
public class Jumpship extends Aero {
    
    /**
     * 
     */
    private static final long serialVersionUID = 8835064735203453570L;
    //     locations
    public static final int        LOC_NOSE               = 0;
    public static final int        LOC_FLS                = 1;
    public static final int        LOC_FRS                = 2;
    public static final int        LOC_AFT                = 3;
    //aft comes first so it is consistent with Aero
    public static final int        LOC_ALS                = 4;
    public static final int        LOC_ARS                = 5;
    
    protected static String[] LOCATION_ABBRS = { "NOS", "FLS", "FRS", "AFT", "ALS", "ARS" };
    protected static String[] LOCATION_NAMES = { "Nose", "Left Front Side", "Right Front Side", "Aft", "Aft Left Side", "Aft Right Side" };
 
    private int kf_integrity = 0;
    private int sail_integrity = 0;
    
    private int damThresh[] = {0,0,0,0,0,0};
   
    //this may be bizarre, but I am going to put
    //the sum of standard damage here for standard-to-capital damage conversion
    private int standard_damage[] = {0,0,0,0,0,0};
    
    //crew and passengers
    private int nCrew = 0;
    private int nPassenger = 0;
    //lifeboats and escape pods
    private int lifeBoats = 0;
    private int escapePods = 0;
    
    //lithium fusion
    boolean hasLF = false;
    
//  HPG
    boolean hasHPG = false;
    
    //grav decks (three different kinds)
    //regular
    int gravDeck = 0;
    int gravDeckLarge = 0;
    int gravDeckHuge = 0;
    
    //just give it some engine
    
    public int locations() {
        return 6;
    }
    
    public int getTotalGravDeck() {
        return (gravDeck + gravDeckLarge + gravDeckHuge);
    }
    
    public void setGravDeck(int n) {
        this.gravDeck = n;
    }
    
    public int getGravDeck() {
        return gravDeck;
    }
    
    public void setGravDeckLarge(int n) {
        this.gravDeckLarge = n;
    }
    
    public int getGravDeckLarge() {
        return gravDeckLarge;
    }
    
    public void setGravDeckHuge(int n) {
        this.gravDeckHuge = n;
    }
    
    public int getGravDeckHuge() {
        return gravDeckHuge;
    }
    
    public void setHPG(boolean b) {
        this.hasHPG = b;
    }
    
    public boolean hasHPG() {
        return hasHPG;
    }
    
    public void setLF(boolean b) {
        this.hasLF = b;
    }
    
    public boolean hasLF() {
        return hasLF;
    }
    
    public void setEscapePods(int n) {
        this.escapePods = n;
    }
    
    public int getEscapePods() {
        return escapePods;
    }
    
    public void setLifeBoats(int n) {
        this.lifeBoats = n;
    }
    
    public int getLifeBoats() {
        return lifeBoats;
    }
    
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
    
    public void setThresh(int val, int loc) {
        damThresh[loc] = val;
    }
    
    public int getThresh(int loc) {
        return damThresh[loc];
    }
    
    public void autoSetThresh()
    {
        for(int x = 0; x < locations(); x++)
        {
            initializeThresh(x);
        }    
    }
    
    public void initializeThresh(int loc)
    {
        int nThresh = (int)Math.ceil(getArmor(loc) / 10.0);
        setThresh(nThresh,loc);
    }
    
    public String[] getLocationAbbrs() {
        return LOCATION_ABBRS;
    }
    
    public String[] getLocationNames() { 
        return LOCATION_NAMES; 
    }
    
    public void setKFIntegrity(int kf) {
        this.kf_integrity = kf;
    }
    
    public int getKFIntegrity() {
        return kf_integrity;
    }
    
    public void setSailIntegrity(int sail) {
        this.sail_integrity = sail;
    }
    
    public int getSailIntegrity() {
        return sail_integrity;
    }
    
    public void initializeSailIntegrity() {
        int integrity = 1 + (int)Math.round((30.0 + this.weight / 7500.0)/ 20.0);
        this.setSailIntegrity(integrity);
    }
    
    public void initializeKFIntegrity() {
        int integrity = (int)Math.round(1.2 + (0.95) * this.weight/60000.0);
        this.setKFIntegrity(integrity);
    }
    
    public boolean canJump() {
        return kf_integrity > 0;
    }
    
    public void setEngine(Engine e) {
        engine = e;
    }
    
    public int getStandardDamage(int loc) {
        return standard_damage[loc];
    }
    
    public void resetStandardDamage() {
        for(int i = 0; i < locations(); i++) {
            standard_damage[i] = 0;
        }
    }
    
    public void addStandardDamage(int damage, HitData hit) {
        standard_damage[hit.getLocation()] = standard_damage[hit.getLocation()] + damage;
    }
    
    //different firing arcs
    public int getWeaponArc(int wn) {
        final Mounted mounted = getEquipment(wn);
        
        
        switch (mounted.getLocation()) {
        case LOC_NOSE:
            return Compute.ARC_NOSE;
        case LOC_FRS:
            return Compute.ARC_RIGHTSIDE_SPHERE;
        case LOC_FLS:
            return Compute.ARC_LEFTSIDE_SPHERE;
        case LOC_ARS:
            return Compute.ARC_RIGHTSIDEA_SPHERE;
        case LOC_ALS:
            return Compute.ARC_LEFTSIDEA_SPHERE;
        case LOC_AFT:
            return Compute.ARC_AFT;
        default:
            return Compute.ARC_360;
        }        
    }
    
    //different hit locations
    public HitData rollHitLocation(int table, int side) {

        /* 
         * Unlike other units, ASFs determine potential crits based on the to-hit roll
         * so I need to set this potential value as well as return the to hit data
         */
       
        int roll = Compute.d6(2);
        if(side == ToHitData.SIDE_FRONT) {
            // normal front hits
            switch( roll ) {
            case 2:
                setPotCrit(CRIT_LIFE_SUPPORT);
                return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
            case 3:
                setPotCrit(CRIT_CONTROL);
                return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
            case 4:
                setPotCrit(CRIT_WEAPON);
                return new HitData(LOC_FRS, false, HitData.EFFECT_NONE);
            case 5:
                setPotCrit(CRIT_RIGHT_THRUSTER);
                return new HitData(LOC_FRS, false, HitData.EFFECT_NONE);
            case 6:
                setPotCrit(CRIT_CIC);
                return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
            case 7:
                setPotCrit(CRIT_WEAPON);
                return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
            case 8:
                setPotCrit(CRIT_SENSOR);
                return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
            case 9:
                setPotCrit(CRIT_LEFT_THRUSTER);
                return new HitData(LOC_FLS, false, HitData.EFFECT_NONE);
            case 10:
                setPotCrit(CRIT_WEAPON);
                return new HitData(LOC_FLS, false, HitData.EFFECT_NONE);
            case 11:
                setPotCrit(CRIT_CREW);
                return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
            case 12:
                setPotCrit(CRIT_KF_DRIVE);
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
                setPotCrit(CRIT_AVIONICS);
                return new HitData(LOC_FLS, false, HitData.EFFECT_NONE);
            case 4:
                setPotCrit(CRIT_GRAV_DECK);
                return new HitData(LOC_FLS, false, HitData.EFFECT_NONE);
            case 5:
                setPotCrit(CRIT_WEAPON);
                return new HitData(LOC_FLS, false, HitData.EFFECT_NONE);
            case 6:
                setPotCrit(CRIT_DOCK_COLLAR);
                return new HitData(LOC_FLS, false, HitData.EFFECT_NONE);
            case 7:
                setPotCrit(CRIT_KF_DRIVE);
                return new HitData(LOC_FLS, false, HitData.EFFECT_NONE);
            case 8:
                setPotCrit(CRIT_CARGO);
                return new HitData(LOC_ALS, false, HitData.EFFECT_NONE);
            case 9:
                setPotCrit(CRIT_DOOR);
                return new HitData(LOC_ALS, false, HitData.EFFECT_NONE);
            case 10:
                setPotCrit(CRIT_WEAPON);
                return new HitData(LOC_ALS, false, HitData.EFFECT_NONE);
            case 11:
                setPotCrit(CRIT_WEAPON);
                return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
            case 12:
                setPotCrit(CRIT_ENGINE);
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
                setPotCrit(CRIT_AVIONICS);
                return new HitData(LOC_FRS, false, HitData.EFFECT_NONE);
            case 4:
                setPotCrit(CRIT_GRAV_DECK);
                return new HitData(LOC_FRS, false, HitData.EFFECT_NONE);
            case 5:
                setPotCrit(CRIT_WEAPON);
                return new HitData(LOC_FRS, false, HitData.EFFECT_NONE);
            case 6:
                setPotCrit(CRIT_DOCK_COLLAR);
                return new HitData(LOC_FRS, false, HitData.EFFECT_NONE);
            case 7:
                setPotCrit(CRIT_KF_DRIVE);
                return new HitData(LOC_FRS, false, HitData.EFFECT_NONE);
            case 8:
                setPotCrit(CRIT_CARGO);
                return new HitData(LOC_ARS, false, HitData.EFFECT_NONE);
            case 9:
                setPotCrit(CRIT_DOOR);
                return new HitData(LOC_ARS, false, HitData.EFFECT_NONE);
            case 10:
                setPotCrit(CRIT_WEAPON);
                return new HitData(LOC_ARS, false, HitData.EFFECT_NONE);
            case 11:
                setPotCrit(CRIT_WEAPON);
                return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
            case 12:
                setPotCrit(CRIT_ENGINE);
                return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
            }
        }
        else if(side == ToHitData.SIDE_REAR) {
            // normal aft hits
            switch( roll ) {
            case 2:
                setPotCrit(CRIT_FUEL_TANK);
                return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
            case 3:
                setPotCrit(CRIT_AVIONICS);
                return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
            case 4:
                setPotCrit(CRIT_WEAPON);
                return new HitData(LOC_ARS, false, HitData.EFFECT_NONE);
            case 5:
                setPotCrit(CRIT_RIGHT_THRUSTER);
                return new HitData(LOC_ARS, false, HitData.EFFECT_NONE);
            case 6:
                setPotCrit(CRIT_ENGINE);
                return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
            case 7:
                setPotCrit(CRIT_WEAPON);
                return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
            case 8:
                setPotCrit(CRIT_ENGINE);
                return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
            case 9:
                setPotCrit(CRIT_LEFT_THRUSTER);
                return new HitData(LOC_ALS, false, HitData.EFFECT_NONE);
            case 10:
                setPotCrit(CRIT_WEAPON);
                return new HitData(LOC_ALS, false, HitData.EFFECT_NONE);
            case 11:
                setPotCrit(CRIT_CONTROL);
                return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
            case 12:
                setPotCrit(CRIT_KF_DRIVE);
                return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
            }
        }    
            return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
    }
    
    public int getMaxEngineHits() {
        return 6;
    }
    
    /*This is my educated guess as to what BV2.0 will look like for 
     * jumpships, warships, and space stations. it is based on just 
     * doing the same calculations as in
     * the TechManual, but with the corrected multipliers
     */
    public int calculateBattleValue(boolean assumeLinkedC3, boolean ignoreC3) {
        double dbv = 0; // defensive battle value
        double obv = 0; // offensive bv

        // total armor points
        dbv += getTotalArmor() * 25;

        // total internal structure  
        dbv += getSI() * 20;

        // add defensive equipment
        double dEquipmentBV = 0;
        for (Mounted mounted : getEquipment()){
            EquipmentType etype = mounted.getType();

            // don't count destroyed equipment
            if (mounted.isDestroyed())
                continue;

            if ((etype instanceof WeaponType && etype.hasFlag(WeaponType.F_AMS) || etype.hasFlag(WeaponType.F_B_POD))
                    || (etype instanceof AmmoType && ((AmmoType)etype).getAmmoType() == AmmoType.T_AMS)
                    || (etype instanceof MiscType && (etype.hasFlag(MiscType.F_ECM)
                                            || etype.hasFlag(MiscType.F_AP_POD)
               // not yet coded:            || etype.hasFlag(MiscType.F_BRIDGE_LAYING)
                                            || etype.hasFlag(MiscType.F_BAP)))) {
                dEquipmentBV += etype.getBV(this);
            }
        }
        dbv += dEquipmentBV;

        //unit type multiplier
        dbv *= 0.8;
       
        
//      calculate heat efficiency
        int aeroHeatEfficiency = this.getHeatCapacity();

        // total up maximum heat generated
        // and add up BVs for ammo-using weapon types for excessive ammo rule
        Map<String, Double> weaponsForExcessiveAmmo = new HashMap<String, Double>();
        double maximumHeat = 0;
        for (Mounted mounted : getTotalWeaponList()) {
            WeaponType wtype = (WeaponType)mounted.getType();
            if(wtype instanceof BayWeapon) {
                continue;
            }
            double weaponHeat = wtype.getHeat();
            
            // only count non-damaged equipment
            if (mounted.isMissing() || mounted.isHit() ||
                    mounted.isDestroyed() || mounted.isBreached()) {
                continue;
            }
            
            // one shot weapons count 1/4
            if (wtype.getAmmoType() == AmmoType.T_ROCKET_LAUNCHER
                    || wtype.hasFlag(WeaponType.F_ONESHOT)) {
                weaponHeat *= 0.25;
            }

            // double heat for ultras
            if ((wtype.getAmmoType() == AmmoType.T_AC_ULTRA)
                    || (wtype.getAmmoType() == AmmoType.T_AC_ULTRA_THB)) {
                weaponHeat *= 2;
            }

            // Six times heat for RAC
            if (wtype.getAmmoType() == AmmoType.T_AC_ROTARY) {
                weaponHeat *= 6;
            }

            // half heat for streaks
            if ((wtype.getAmmoType() == AmmoType.T_SRM_STREAK)
                    || (wtype.getAmmoType() == AmmoType.T_MRM_STREAK)
                    || (wtype.getAmmoType() == AmmoType.T_LRM_STREAK)){
                weaponHeat *= 0.5;
            }
            maximumHeat += weaponHeat;
            // add up BV of ammo-using weapons for each type of weapon,
            // to compare with ammo BV later for excessive ammo BV rule
            if (!((wtype.hasFlag(WeaponType.F_ENERGY) && !(wtype.getAmmoType() == AmmoType.T_PLASMA))
                        || wtype.hasFlag(WeaponType.F_ONESHOT)
                        || wtype.hasFlag(WeaponType.F_INFANTRY)
                        || wtype.getAmmoType() == AmmoType.T_NA)) {
                String key = wtype.getAmmoType()+":"+wtype.getRackSize();
                if (!weaponsForExcessiveAmmo.containsKey(key)) {
                    weaponsForExcessiveAmmo.put(key, wtype.getBV(this));
                }
                else {
                    weaponsForExcessiveAmmo.put(key, wtype.getBV(this)+weaponsForExcessiveAmmo.get(key));
                }
            }
        }
                
        double weaponBV = 0;
        double weaponsBVFront = 0;
        double weaponsBVRear = 0;
        boolean hasTargComp = hasTargComp();
        
        if (maximumHeat <= aeroHeatEfficiency) {
            //count all weapons equal, adjusting for rear-firing and excessive ammo
            for (Mounted mounted : getTotalWeaponList()) {
                WeaponType wtype = (WeaponType)mounted.getType();
                if(wtype instanceof BayWeapon) {
                    continue;
                }
                double dBV = wtype.getBV(this);
                
                // don't count destroyed equipment
                if (mounted.isDestroyed())
                    continue;

                // don't count AMS, it's defensive
                if (wtype.hasFlag(WeaponType.F_AMS)) {
                    continue;
                }
                
                // and we'll add the tcomp here too
                if (wtype.hasFlag(WeaponType.F_DIRECT_FIRE)) {
                    if(hasTargComp)
                        dBV *= 1.25;
                }
                
                if (mounted.getLocation() == LOC_AFT) {
                    weaponsBVRear += dBV;
                } else {
                    weaponsBVFront += dBV;
                }
            }
        } else {
            // count weapons at full BV until heatefficiency is reached or passed with one weapon
            int heatAdded = 0;
            ArrayList<Mounted> weapons = this.getTotalWeaponList();
            Collections.sort(weapons, new WeaponComparator());
            for (Mounted weapon : weapons) {
                WeaponType wtype = (WeaponType)weapon.getType();
                if(wtype instanceof BayWeapon) {
                    continue;
                }
                double dBV = wtype.getBV(this);
                // don't count destroyed equipment
                if (weapon.isDestroyed())
                    continue;
                // don't count AMS, it's defensive
                if (wtype.hasFlag(WeaponType.F_AMS)) {
                    continue;
                }
                // calc MG Array here:
                if (wtype.hasFlag(WeaponType.F_MGA)) {
                    double mgaBV = 0;
                    for (Mounted possibleMG : this.getTotalWeaponList()) {
                        if (possibleMG.getType().hasFlag(WeaponType.F_MG) && possibleMG.getLocation() == weapon.getLocation()) {
                            mgaBV += possibleMG.getType().getBV(this);
                        }
                    }
                   dBV = mgaBV * 0.67;
                }
                // and we'll add the tcomp here too
                if (wtype.hasFlag(WeaponType.F_DIRECT_FIRE) && hasTargComp) {
                    dBV *= 1.25;
                }
                // artemis bumps up the value
                if (weapon.getLinkedBy() != null) {
                    Mounted mLinker = weapon.getLinkedBy();
                    if (mLinker.getType() instanceof MiscType && 
                            mLinker.getType().hasFlag(MiscType.F_ARTEMIS)) {
                        dBV *= 1.2;
                    }
                } 
                
                if (weapon.getLinkedBy() != null) {
                    Mounted mLinker = weapon.getLinkedBy();
                    if (mLinker.getType() instanceof MiscType && mLinker.getType().hasFlag(MiscType.F_APOLLO)) {
                        dBV *= 1.15;
                    }
                }

                if (heatAdded > aeroHeatEfficiency && wtype.getHeat() > 0)
                    dBV /= 2;
                if (weapon.getLocation() == LOC_AFT) {
                    weaponsBVRear += dBV;
                } else {
                    weaponsBVFront += dBV;
                }
                heatAdded += ((WeaponType)weapon.getType()).getHeat();
            }
        }
        if (weaponsBVFront > weaponsBVRear) {
            weaponBV += weaponsBVFront;
            weaponBV += (weaponsBVRear * 0.5);
        } else {
            weaponBV += weaponsBVRear;
            weaponBV += (weaponsBVFront * 0.5);
        }   
        // add offensive misc. equipment BV (everything except AMS, A-Pod, ECM - BMR p152)
        double oEquipmentBV = 0;
        for (Mounted mounted : getMisc()) {
            MiscType mtype = (MiscType)mounted.getType();
 
            // don't count destroyed equipment
            if (mounted.isDestroyed())
                continue;

            if (mtype.hasFlag(MiscType.F_ECM)
                    || mtype.hasFlag(MiscType.F_BAP)
                    || mtype.hasFlag(MiscType.F_AP_POD) 
//not yet coded:    || etype.hasFlag(MiscType.F_BRIDGE_LAYING)
                    || mtype.hasFlag(MiscType.F_TARGCOMP)) //targ counted with weapons 
                continue;
            oEquipmentBV += mtype.getBV(this);
            // need to do this here, a MiscType does not know the location
            // where it's mounted
            if (mtype.hasFlag(MiscType.F_HARJEL)) {
                if (this.getArmor(mounted.getLocation(), false) != IArmorState.ARMOR_DESTROYED) {
                    oEquipmentBV += this.getArmor(mounted.getLocation());
                }
                if (this.hasRearArmor(mounted.getLocation())
                        && this.getArmor(mounted.getLocation(), true) != IArmorState.ARMOR_DESTROYED) {
                    oEquipmentBV += this.getArmor(mounted.getLocation(), true);
                }
            }
        }
        weaponBV += oEquipmentBV;

        // add ammo bv
        double ammoBV = 0;
        // extra BV for when we have semiguided LRMs and someone else has TAG on our team        
        double tagBV = 0;
        Map<String, Double> ammo = new HashMap<String, Double>();
        ArrayList<String> keys = new ArrayList<String>(); 
        for (Mounted mounted : getAmmo()) {
            AmmoType atype = (AmmoType)mounted.getType();

            // don't count depleted ammo
            if (mounted.getShotsLeft() == 0)
                continue;

            // don't count AMS, it's defensive
            if (atype.getAmmoType() == AmmoType.T_AMS) {
                continue;
            }

            // don't count oneshot ammo, it's considered part of the launcher.
            if (mounted.getLocation() == Entity.LOC_NONE) {
                // assumption: ammo without a location is for a oneshot weapon
                continue;
            }
            
            double curBV = atype.getBV(this);
            if(mounted.byShot()) {
                double tons = (double)mounted.getShotsLeft() / atype.getShots();
                if(atype.getAmmoRatio() > 0) {
                    tons = mounted.getShotsLeft() * atype.getAmmoRatio();
                }
                curBV *= tons;
            }
            
            // semiguided ammo might count double
            if (atype.getMunitionType() == AmmoType.M_SEMIGUIDED) {
                Player tmpP = getOwner();
                
                if ( tmpP != null ){
                    // Okay, actually check for friendly TAG.
                    if (tmpP.hasTAG())
                        tagBV += curBV;
                    else if (tmpP.getTeam() != Player.TEAM_NONE && game != null) {
                       for (Enumeration<Team> e = game.getTeams(); e.hasMoreElements(); ) {
                            Team m = e.nextElement();
                            if (m.getId() == tmpP.getTeam()) {
                                if (m.hasTAG(game)) {
                                    tagBV += curBV;
                                }
                                // A player can't be on two teams.
                                // If we check his team and don't give the penalty, that's it.
                                break;
                            }
                        }
                    }
                }
            }
            String key = atype.getAmmoType()+":"+atype.getRackSize();
            if (!keys.contains(key))
                keys.add(key);
            if (!ammo.containsKey(key)) {
                ammo.put(key, curBV);
            }
            else {
                ammo.put(key, curBV+ammo.get(key));
            }
        }

        // Excessive ammo rule:
        // Only count BV for ammo for a weapontype until the BV of all weapons of that 
        // type on the mech is reached.
        for (String key : keys) {
            if (weaponsForExcessiveAmmo.get(key) != null) {
                if (ammo.get(key) > weaponsForExcessiveAmmo.get(key))
                    ammoBV += weaponsForExcessiveAmmo.get(key);
                else
                    ammoBV += ammo.get(key);
            } else {
                // Ammo with no matching weapons counts 0, unless it's a coolant pod
                // because coolant pods have no matching weapon
                if (key.equals(new Integer(AmmoType.T_COOLANT_POD).toString()+"1")) {
                    ammoBV += ammo.get(key);
                }
            }
        }
        weaponBV += ammoBV;
        
        // adjust further for speed factor
        // this is a bit weird, because the formula gives
        // a different result than the table, because MASC/TSM
        // is handled differently (page 315, TM, compare
        // http://forums.classicbattletech.com/index.php/topic,20468.0.html
        double speedFactor;
        double speedFactorTableLookup = getOriginalWalkMP();
        speedFactor = Math.pow(1+((speedFactorTableLookup-5)/10), 1.2);
        speedFactor = Math.round(speedFactor * 100) / 100.0;
        
        obv = weaponBV * speedFactor;

        // we get extra bv from some stuff
        double xbv = 0.0;
        //extra BV for semi-guided lrm when TAG in our team
        xbv += tagBV;
        // extra from c3 networks. a valid network requires at least 2 members
        // some hackery and magic numbers here.  could be better
        // also, each 'has' loops through all equipment.  inefficient to do it 3 times
        if (((hasC3MM() && calculateFreeC3MNodes() < 2) ||
            (hasC3M() && calculateFreeC3Nodes() < 3) ||
            (hasC3S() && C3Master > NONE) ||
            (hasC3i() && calculateFreeC3Nodes() < 5) ||
            assumeLinkedC3) && !ignoreC3 && (game != null)) {
            int totalForceBV = 0;
            totalForceBV += this.calculateBattleValue(false, true);
            for (Entity e : game.getC3NetworkMembers(this)) {
                if (!equals(e) && onSameC3NetworkAs(e)) {
                    totalForceBV+=e.calculateBattleValue(true);
                }
            }
            xbv += totalForceBV *= 0.05;
        }

        int finalBV = (int)Math.round(dbv + obv + xbv);

        // and then factor in pilot
        double pilotFactor = crew.getBVSkillMultiplier();
        
        int retVal = (int)Math.round((finalBV) * pilotFactor);
        
        // don't factor pilot in if we are just calculating BV for C3 extra BV
        if (ignoreC3)
            return finalBV;
        return retVal;
    }
    
    /**
     * Calculates the battle value of this ASF
     */
    public int calculateBattleValue(boolean assumeLinkedC3) {
        return calculateBattleValue(assumeLinkedC3, false);
    }
    
    public int getArcswGuns() {
        //return the number 
        int nArcs = 0;
        for(int i = 0; i < locations(); i++) {
                if(hasWeaponInArc(i))
                    nArcs++;
        }      
        return nArcs;
    }
    
    public boolean hasWeaponInArc(int loc) {
        boolean hasWeapons = false;
        for(Mounted weap: getWeaponList()) {
            if(weap.getLocation() == loc) {
                hasWeapons = true;
            }
        }
        return hasWeapons;
    }
    
    public double getFuelPerTon() {
        
        double points = 10.0;
        
        if(weight >= 250000) {
            points = 2.5;
            return points;
        } else if (weight >= 110000) {
            points = 5.0;
            return points;
        } 
        
        return points;
    }
    
    public double getArmorWeight(int loc) {
        //first I need to subtract SI bonus from total armor
        double armorPoints = getTotalOArmor();
        armorPoints -= Math.round((get0SI() * loc)/ 10.0);
        //this roundabout method is actually necessary to avoid rounding weirdness.  Yeah, it's dumb.
        //now I need to determine base armor points by type and weight
        
        double baseArmor = 0.8;
        if(isClan()) {
            baseArmor = 1.0;
        }
        
        if(weight >= 250000) {
            baseArmor = 0.4;
            if(isClan()) {
                baseArmor = 0.5;
            }
        } else if(weight >= 150000) {
            baseArmor = 0.6;
            if(isClan()) {
                baseArmor = 0.7;
            }
        } 
        
        
        
        if(armorType == EquipmentType.T_ARMOR_FERRO_IMP) {
            baseArmor += 0.2;
        } else if (armorType == EquipmentType.T_ARMOR_FERRO_CARBIDE) {
            baseArmor += 0.4;
        } else if (armorType == EquipmentType.T_ARMOR_LAMELLOR_FERRO_CARBIDE) {
            baseArmor += 0.6;
        }
        
        double armorPerTon = baseArmor;
        double weight=0.0;
        for(;(weight*armorPerTon)<armorPoints;weight+=.5) {}
        return weight;
    }
    
    public double getCost() {
        
        double cost = 0;

        //add in controls
        //bridge
        cost += 200000;
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
        
        //docking hard point
        cost += 100000 * getDocks();
        
        double engineWeight = weight * 0.012;
        cost += engineWeight * 1000;
        //drive unit
        cost += 500 * getOriginalWalkMP() * weight / 100.0;
        //control
        cost += 1000;
        
        //HPG
        if(hasHPG()) {
            cost += 1000000000;
        }
        
        //fuel tanks
        cost += 200 * getFuel() / getFuelPerTon();

        //armor
        cost += getArmorWeight(locations())*EquipmentType.getArmorCost(armorType);
        
        //heat sinks
        int sinkCost = 2000 + 4000 * getHeatType();// == HEAT_DOUBLE ? 6000: 2000;    
        cost += sinkCost*getHeatSinks();
        
        //KF Drive
        double driveCost = 0;
        //coil
        driveCost += 60000000 + (75000000 * getDocks());
        //initiator
        driveCost += 25000000 + (5000000 * getDocks());
        //controller
        driveCost += 50000000;
        //tankage
        driveCost += 50000 * getKFIntegrity();
        //sail
        driveCost += 50000 * ( 30 + (weight / 7500) );
        //charging system
        driveCost += 500000 + (200000 * getDocks());
        //is the core compact? (not for jumpships)
        //lithium fusion?
        if(hasLF()) {
            driveCost *= 3;
        }
        
        cost += driveCost;
        
        //grav deck
        cost += 5000000 * getGravDeck();
        cost += 10000000 * getGravDeckLarge();
        cost += 40000000 * getGravDeckHuge();        
        
        //weapons 
        cost += getWeaponsAndEquipmentCost();
        
        //get bays
        int baydoors = 0;
        int bayCost = 0;
        for(Bay next:getTransportBays()) {
            baydoors += next.getDoors();
            if(next instanceof MechBay || next instanceof ASFBay || next instanceof SmallCraftBay) {
                bayCost += 20000 * next.totalSpace;
            }
            if(next instanceof LightVehicleBay || next instanceof HeavyVehicleBay) {
                bayCost += 20000 * next.totalSpace;
            }
        }
        
        cost += bayCost + baydoors * 1000;
        
        //life boats and escape pods
        cost += 5000 * (getLifeBoats() + getEscapePods());
        
        double weightMultiplier = 1.25f;
        
        return Math.round(cost * weightMultiplier);
        
    }
    
    public boolean doomedOnGround() {
        return true;
    }
    
    public boolean doomedInAtmosphere() {
        return true;
    }
    
    public boolean doomedInSpace() {
        return false;
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
        
        //for large craft, ammo must be in the same ba        
        Mounted bay = whichBay(this.getEquipmentNum(mounted));
        if(bay != null && !bay.ammoInBay(this.getEquipmentNum(mountedAmmo))) 
            return success;
    
        
        if (mountedAmmo.isAmmoUsable() && !wtype.hasFlag(WeaponType.F_ONESHOT)
                && atype.getAmmoType() == wtype.getAmmoType()
                && atype.getRackSize() == wtype.getRackSize()) {
            mounted.setLinked(mountedAmmo);
            success = true;
        }
        return success;
    }
    
}

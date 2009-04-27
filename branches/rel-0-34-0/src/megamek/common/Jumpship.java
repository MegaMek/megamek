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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import megamek.common.weapons.BayWeapon;

/**
 * @author Jay Lawson
 */
public class Jumpship extends Aero implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 9154398176617208384L;
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
    private boolean hasHPG = false;

    //grav decks (three different kinds)
    //regular
    private int gravDeck = 0;
    private int gravDeckLarge = 0;
    private int gravDeckHuge = 0;

    //station-keeping thrust and accumulated thrust
    private double stationThrust = 0.2;
    private double accumulatedThrust = 0.0;

    //just give it some engine

    @Override
    public int locations() {
        return 6;
    }

    public int getTotalGravDeck() {
        return (gravDeck + gravDeckLarge + gravDeckHuge);
    }

    public void setGravDeck(int n) {
        gravDeck = n;
    }

    public int getGravDeck() {
        return gravDeck;
    }

    public void setGravDeckLarge(int n) {
        gravDeckLarge = n;
    }

    public int getGravDeckLarge() {
        return gravDeckLarge;
    }

    public void setGravDeckHuge(int n) {
        gravDeckHuge = n;
    }

    public int getGravDeckHuge() {
        return gravDeckHuge;
    }

    public void setHPG(boolean b) {
        hasHPG = b;
    }

    public boolean hasHPG() {
        return hasHPG;
    }

    public void setLF(boolean b) {
        hasLF = b;
    }

    public boolean hasLF() {
        return hasLF;
    }

    public void setEscapePods(int n) {
        escapePods = n;
    }

    public int getEscapePods() {
        return escapePods;
    }

    public void setLifeBoats(int n) {
        lifeBoats = n;
    }

    public int getLifeBoats() {
        return lifeBoats;
    }

    public void setNCrew(int crew) {
        nCrew = crew;
    }

    public void setNPassenger(int pass) {
        nPassenger = pass;
    }

    public int getNCrew() {
        return nCrew;
    }

    public int getNPassenger() {
        return nPassenger;
    }

    @Override
    public void setThresh(int val, int loc) {
        damThresh[loc] = val;
    }

    @Override
    public int getThresh(int loc) {
        return damThresh[loc];
    }

    @Override
    public void autoSetThresh()
    {
        for(int x = 0; x < locations(); x++)
        {
            initializeThresh(x);
        }
    }

    @Override
    public void initializeThresh(int loc)
    {
        int nThresh = (int)Math.ceil(getArmor(loc) / 10.0);
        setThresh(nThresh,loc);
    }

    @Override
    public String[] getLocationAbbrs() {
        return LOCATION_ABBRS;
    }

    @Override
    public String[] getLocationNames() {
        return LOCATION_NAMES;
    }

    public void setKFIntegrity(int kf) {
        kf_integrity = kf;
    }

    public int getKFIntegrity() {
        return kf_integrity;
    }

    public void setSailIntegrity(int sail) {
        sail_integrity = sail;
    }

    public int getSailIntegrity() {
        return sail_integrity;
    }

    public void initializeSailIntegrity() {
        int integrity = 1 + (int)Math.round((30.0 + weight / 7500.0)/ 20.0);
        setSailIntegrity(integrity);
    }

    public void initializeKFIntegrity() {
        int integrity = (int)Math.round(1.2 + (0.95) * weight/60000.0);
        setKFIntegrity(integrity);
    }

    public boolean canJump() {
        return kf_integrity > 0;
    }

    @Override
    public void setEngine(Engine e) {
        engine = e;
    }

    @Override
    public int getStandardDamage(int loc) {
        return standard_damage[loc];
    }

    @Override
    public void resetStandardDamage() {
        for(int i = 0; i < locations(); i++) {
            standard_damage[i] = 0;
        }
    }

    @Override
    public void addStandardDamage(int damage, HitData hit) {
        standard_damage[hit.getLocation()] = standard_damage[hit.getLocation()] + damage;
    }

    //different firing arcs
    //  different firing arcs
    @Override
    public int getWeaponArc(int wn) {
        final Mounted mounted = getEquipment(wn);

        int arc = Compute.ARC_NOSE;
        switch (mounted.getLocation()) {
        case LOC_NOSE:
            arc = Compute.ARC_NOSE;
            break;
        case LOC_FRS:
            arc = Compute.ARC_RIGHTSIDE_SPHERE;
            break;
        case LOC_FLS:
            arc = Compute.ARC_LEFTSIDE_SPHERE;
            break;
        case LOC_ARS:
            arc = Compute.ARC_RIGHTSIDEA_SPHERE;
            break;
        case LOC_ALS:
            arc = Compute.ARC_LEFTSIDEA_SPHERE;
            break;
        case LOC_AFT:
            arc = Compute.ARC_AFT;
            break;
        default:
            arc = Compute.ARC_360;
        }
        return rollArcs(arc);
    }

    //different hit locations
    @Override
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
                setPotCrit(CRIT_AVIONICS);
                return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
            case 3:
                setPotCrit(CRIT_SENSOR);
                return new HitData(LOC_FLS, false, HitData.EFFECT_NONE);
            case 4:
                setPotCrit(CRIT_WEAPON);
                return new HitData(LOC_FLS, false, HitData.EFFECT_NONE);
            case 5:
                setPotCrit(CRIT_DOCK_COLLAR);
                return new HitData(LOC_FLS, false, HitData.EFFECT_NONE);
            case 6:
                setPotCrit(CRIT_KF_DRIVE);
                return new HitData(LOC_FLS, false, HitData.EFFECT_NONE);
            case 7:
                setPotCrit(CRIT_WEAPON_BROAD);
                return new HitData(LOC_ALS, false, HitData.EFFECT_NONE);
            case 8:
                setPotCrit(CRIT_GRAV_DECK);
                return new HitData(LOC_ALS, false, HitData.EFFECT_NONE);
            case 9:
                setPotCrit(CRIT_DOOR);
                return new HitData(LOC_ALS, false, HitData.EFFECT_NONE);
            case 10:
                setPotCrit(CRIT_WEAPON);
                return new HitData(LOC_ALS, false, HitData.EFFECT_NONE);
            case 11:
                setPotCrit(CRIT_CARGO);
                return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
            case 12:
                setPotCrit(CRIT_ENGINE);
                return new HitData(LOC_AFT, false, HitData.EFFECT_NONE);
            }
        }
        else if(side == ToHitData.SIDE_RIGHT) {
            // normal left-side hits
            switch( roll ) {
            case 2:
                setPotCrit(CRIT_AVIONICS);
                return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
            case 3:
                setPotCrit(CRIT_SENSOR);
                return new HitData(LOC_FRS, false, HitData.EFFECT_NONE);
            case 4:
                setPotCrit(CRIT_WEAPON);
                return new HitData(LOC_FRS, false, HitData.EFFECT_NONE);
            case 5:
                setPotCrit(CRIT_DOCK_COLLAR);
                return new HitData(LOC_FRS, false, HitData.EFFECT_NONE);
            case 6:
                setPotCrit(CRIT_KF_DRIVE);
                return new HitData(LOC_FRS, false, HitData.EFFECT_NONE);
            case 7:
                setPotCrit(CRIT_WEAPON_BROAD);
                return new HitData(LOC_ARS, false, HitData.EFFECT_NONE);
            case 8:
                setPotCrit(CRIT_GRAV_DECK);
                return new HitData(LOC_ARS, false, HitData.EFFECT_NONE);
            case 9:
                setPotCrit(CRIT_DOOR);
                return new HitData(LOC_ARS, false, HitData.EFFECT_NONE);
            case 10:
                setPotCrit(CRIT_WEAPON);
                return new HitData(LOC_ARS, false, HitData.EFFECT_NONE);
            case 11:
                setPotCrit(CRIT_CARGO);
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

    @Override
    public int getMaxEngineHits() {
        return 6;
    }

    @Override
    public int calculateBattleValue(boolean ignoreC3, boolean ignorePilot) {
        double dbv = 0; // defensive battle value
        double obv = 0; // offensive bv

        int modularArmor = 0;
        for (Mounted mounted : getEquipment()) {
            if ((mounted.getType() instanceof MiscType) && mounted.getType().hasFlag(MiscType.F_MODULAR_ARMOR)) {
                modularArmor += mounted.getBaseDamageCapacity() - mounted.getDamageTaken();
            }
        }

        dbv += (getTotalArmor()+modularArmor) * 25.0;

        dbv += getSI() * 20.0;

        // add defensive equipment
        double dEquipmentBV = 0;
        for (Mounted mounted : getEquipment()) {
            EquipmentType etype = mounted.getType();

            // don't count destroyed equipment
            if (mounted.isDestroyed()) {
                continue;
            }

            if (((etype instanceof WeaponType) && (etype.hasFlag(WeaponType.F_AMS)))
                    || ((etype instanceof AmmoType) && (((AmmoType) etype).getAmmoType() == AmmoType.T_SCREEN_LAUNCHER))
                    || ((etype instanceof WeaponType) && (((WeaponType) etype).getAtClass() == WeaponType.CLASS_SCREEN))) {
                dEquipmentBV += etype.getBV(this);
            }

            if((etype instanceof AmmoType) && (((AmmoType) etype).getAmmoType() == AmmoType.T_AMS)) {
                double weight = mounted.getShotsLeft() / ((AmmoType)etype).getShots();
                dEquipmentBV += etype.getBV(this) * weight;
            }

        }
        dbv += dEquipmentBV;

        //unit type multiplier
        dbv *= getBVTypeModifier();

        // calculate heat efficiency
        int aeroHeatEfficiency = getHeatCapacity();

        //get arc BV and heat
        // and add up BVs for ammo-using weapon types for excessive ammo rule
        TreeMap<String, Double> weaponsForExcessiveAmmo = new TreeMap<String, Double>();
        TreeMap<Integer, Double> arcBVs = new TreeMap<Integer, Double>();
        TreeMap<Integer, Double> arcHeat = new TreeMap<Integer, Double>();
        for (Mounted mounted : getTotalWeaponList()) {
            WeaponType wtype = (WeaponType) mounted.getType();
            double weaponHeat = wtype.getHeat();
            int arc = getWeaponArc(getEquipmentNum(mounted));
            double dBV = wtype.getBV(this);
            //skip bays
            if(wtype instanceof BayWeapon) {
                continue;
            }
            //don't count defensive weapons
            if (wtype.hasFlag(WeaponType.F_AMS)) {
                continue;
            }
            //don't count screen launchers, they are defensive
            if(wtype.getAtClass() == WeaponType.CLASS_SCREEN) {
                continue;
            }
            // only count non-damaged equipment
            if (mounted.isMissing() || mounted.isHit() || mounted.isDestroyed() || mounted.isBreached()) {
                continue;
            }

            // double heat for ultras
            if ((wtype.getAmmoType() == AmmoType.T_AC_ULTRA) || (wtype.getAmmoType() == AmmoType.T_AC_ULTRA_THB)) {
                weaponHeat *= 2;
            }
            // Six times heat for RAC
            if (wtype.getAmmoType() == AmmoType.T_AC_ROTARY) {
                weaponHeat *= 6;
            }
            //add up BV of ammo-using weapons for each type of weapon,
            // to compare with ammo BV later for excessive ammo BV rule
            if (!((wtype.hasFlag(WeaponType.F_ENERGY) && !(wtype.getAmmoType() == AmmoType.T_PLASMA)) || wtype.hasFlag(WeaponType.F_ONESHOT) || wtype.hasFlag(WeaponType.F_INFANTRY) || (wtype.getAmmoType() == AmmoType.T_NA))) {
                String key = wtype.getAmmoType() + ":" + wtype.getRackSize() + ";" + arc;
                if (!weaponsForExcessiveAmmo.containsKey(key)) {
                    weaponsForExcessiveAmmo.put(key, wtype.getBV(this));
                } else {
                    weaponsForExcessiveAmmo.put(key, wtype.getBV(this) + weaponsForExcessiveAmmo.get(key));
                }
            }
            //calc MG Array here:
            if (wtype.hasFlag(WeaponType.F_MGA)) {
                double mgaBV = 0;
                for (Mounted possibleMG : getTotalWeaponList()) {
                    if (possibleMG.getType().hasFlag(WeaponType.F_MG)
                            && (possibleMG.getLocation() == mounted.getLocation())) {
                        mgaBV += possibleMG.getType().getBV(this);
                    }
                }
                dBV = mgaBV * 0.67;
            }
            // and we'll add the tcomp here too
            if (wtype.hasFlag(WeaponType.F_DIRECT_FIRE)) {
                if (hasTargComp()) {
                    dBV *= 1.25;
                }
            }
            // artemis bumps up the value
            if (mounted.getLinkedBy() != null) {
                Mounted mLinker = mounted.getLinkedBy();
                if ((mLinker.getType() instanceof MiscType) && mLinker.getType().hasFlag(MiscType.F_ARTEMIS)) {
                    dBV *= 1.2;
                }
                if ((mLinker.getType() instanceof MiscType) && mLinker.getType().hasFlag(MiscType.F_APOLLO)) {
                    dBV *= 1.15;
                }
            }

            double currentArcBV = 0.0;
            double currentArcHeat = 0.0;
            if(null != arcBVs.get(arc)) {
                currentArcBV = arcBVs.get(arc);
            }
            if(null != arcHeat.get(arc)) {
                currentArcHeat = arcHeat.get(arc);
            }
            arcBVs.put(arc, currentArcBV + dBV);
            arcHeat.put(arc, currentArcHeat + weaponHeat);
        }
        double weaponBV = 0.0;
        //lets traverse the hash and find the highest value BV arc
        int highArc = Integer.MIN_VALUE;
        int adjArc = Integer.MIN_VALUE;
        int oppArc = Integer.MIN_VALUE;
        double adjArcMult = 1.0;
        double oppArcMult = 0.5;
        double highBV = 0.0;
        double heatUsed = 0.0;
        Set<Integer> set= arcBVs.keySet();
        Iterator<Integer> iter = set.iterator();
        while(iter.hasNext()) {
            int key = iter.next();
            if((arcBVs.get(key) > highBV)
                    && ((key == Compute.ARC_NOSE) || (key == Compute.ARC_LEFT_BROADSIDE)
                            || (key == Compute.ARC_RIGHT_BROADSIDE) || (key == Compute.ARC_AFT))) {
                highArc = key;
                highBV = arcBVs.get(key);
            }
        }
        //now lets identify the adjacent and opposite arcs
        if(highArc > Integer.MIN_VALUE) {
            heatUsed += arcHeat.get(highArc);
            //now get the BV and heat for the two adjacent arcs
            int adjArcCW = getAdjacentArcCW(highArc);
            int adjArcCCW = getAdjacentArcCCW(highArc);
            double adjArcCWBV = 0.0;
            double adjArcCWHeat = 0.0;
            if((adjArcCW > Integer.MIN_VALUE) && (null != arcBVs.get(adjArcCW))) {
                adjArcCWBV = arcBVs.get(adjArcCW);
                adjArcCWHeat = arcHeat.get(adjArcCW);
            }
            double adjArcCCWBV = 0.0;
            double adjArcCCWHeat = 0.0;
            if((adjArcCCW > Integer.MIN_VALUE) && (null != arcBVs.get(adjArcCCW))) {
                adjArcCCWBV = arcBVs.get(adjArcCCW);
                adjArcCCWHeat = arcHeat.get(adjArcCCW);
            }
            if(adjArcCWBV > adjArcCCWBV) {
                adjArc = adjArcCW;
                if((heatUsed + adjArcCWHeat) > aeroHeatEfficiency) {
                    adjArcMult = 0.5;
                }
                heatUsed += adjArcCWHeat;
                oppArc = adjArcCCW;
                if((heatUsed + adjArcCCWHeat) > aeroHeatEfficiency) {
                    oppArcMult = 0.25;
                }
            } else {
                adjArc = adjArcCCW;
                if((heatUsed + adjArcCCWHeat) > aeroHeatEfficiency) {
                    adjArcMult = 0.5;
                }
                heatUsed += adjArcCCWHeat;
                oppArc = adjArcCW;
                if((heatUsed + adjArcCWHeat) > aeroHeatEfficiency) {
                    oppArcMult = 0.25;
                }
            }
        }
        //According to an email with Welshman, ammo should be now added into each arc BV
        //for the final calculation of BV, including the excessive ammo rule
        Map<String, Double> ammo = new HashMap<String, Double>();
        ArrayList<String> keys = new ArrayList<String>();
        for (Mounted mounted : getAmmo()) {
            int arc = getWeaponArc(getEquipmentNum(mounted));
            AmmoType atype = (AmmoType) mounted.getType();

            // don't count depleted ammo
            if (mounted.getShotsLeft() == 0) {
                continue;
            }

            // don't count AMS, it's defensive
            if (atype.getAmmoType() == AmmoType.T_AMS) {
                continue;
            }
            //don't count screen launchers, they are defensive
            if(atype.getAmmoType() == AmmoType.T_SCREEN_LAUNCHER) {
                continue;
            }
            // don't count oneshot ammo, it's considered part of the launcher.
            if (mounted.getLocation() == Entity.LOC_NONE) {
                // assumption: ammo without a location is for a oneshot weapon
                continue;
            }
            String key = atype.getAmmoType() + ":" + atype.getRackSize() + ";" + arc;
            //ammo mounts on large craft is not necessarily in single ton increments so I need to figure out tonnage first
            double weight = mounted.getShotsLeft() / atype.getShots();
            if(atype.isCapital()) {
                weight = mounted.getShotsLeft() * atype.getAmmoRatio();
            }
            if(atype.hasFlag(AmmoType.F_CAP_MISSILE)) {
                weight = mounted.getShotsLeft();
            }
            if (!keys.contains(key)) {
                keys.add(key);
            }
            if (!ammo.containsKey(key)) {
                ammo.put(key, weight * atype.getBV(this));
            } else {
                ammo.put(key, weight * atype.getBV(this) + ammo.get(key));
            }
        }

        // Excessive ammo rule:
        // Only count BV for ammo for a weapontype until the BV of all weapons
        // in that arc is reached
        for (String key : keys) {
            double ammoBV = 0.0;
            int arc= Integer.parseInt(key.split(";")[1]);
            //get the arc
            if (weaponsForExcessiveAmmo.get(key) != null) {
                if (ammo.get(key) > weaponsForExcessiveAmmo.get(key)) {
                    ammoBV += weaponsForExcessiveAmmo.get(key);
                } else {
                    ammoBV += ammo.get(key);
                }
            }
            double currentArcBV = 0.0;
            if(null != arcBVs.get(arc)) {
                currentArcBV = arcBVs.get(arc);
            }
            arcBVs.put(arc, currentArcBV + ammoBV);
        }


        //ok now lets go in and add the arcs
        if(highArc > Integer.MIN_VALUE) {
            //ok now add the BV from this arc and reset to zero
            weaponBV += arcBVs.get(highArc);
            arcBVs.put(highArc, 0.0);
            if((adjArc > Integer.MIN_VALUE) && (null != arcBVs.get(adjArc))) {
                weaponBV += adjArcMult * arcBVs.get(adjArc);
                arcBVs.put(adjArc, 0.0);
            }
            if((oppArc > Integer.MIN_VALUE) && (null != arcBVs.get(oppArc))) {
                weaponBV += oppArcMult * arcBVs.get(oppArc);
                arcBVs.put(oppArc, 0.0);
            }
            //ok now we can cycle through the rest and add 25%
            set= arcBVs.keySet();
            iter = set.iterator();
            while(iter.hasNext()) {
                int key = iter.next();
                weaponBV += (0.25 * arcBVs.get(key));
            }
        }

        // add offensive misc. equipment BV (everything except AMS, A-Pod, ECM -
        // BMR p152)
        double oEquipmentBV = 0;
        for (Mounted mounted : getMisc()) {
            MiscType mtype = (MiscType) mounted.getType();

            // don't count destroyed equipment
            if (mounted.isDestroyed()) {
                continue;
            }

            if (mtype.hasFlag(MiscType.F_TARGCOMP)) {
                continue;
            }
            double bv = mtype.getBV(this);
            oEquipmentBV += bv;
        }
        weaponBV += oEquipmentBV;

        // adjust further for speed factor
        int runMp = getRunMP();
        if(!(this instanceof Warship) && !(this instanceof SpaceStation)) {
            runMp = 1;
        }
        double speedFactor = Math.pow(1 + (((double) runMp - 5) / 10), 1.2);
        speedFactor = Math.round(speedFactor * 100) / 100.0;

        obv = weaponBV * speedFactor;

        // we get extra bv from some stuff
        double xbv = 0.0;
        // extra from c3 networks. a valid network requires at least 2 members
        // some hackery and magic numbers here. could be better
        // also, each 'has' loops through all equipment. inefficient to do it 3
        // times
        if (((hasC3MM() && (calculateFreeC3MNodes() < 2)) || (hasC3M() && (calculateFreeC3Nodes() < 3)) || (hasC3S() && (c3Master > NONE)) || (hasC3i() && (calculateFreeC3Nodes() < 5))) && !ignoreC3 && (game != null)) {
            int totalForceBV = 0;
            totalForceBV += this.calculateBattleValue(true, true);
            for (Entity e : game.getC3NetworkMembers(this)) {
                if (!equals(e) && onSameC3NetworkAs(e)) {
                    totalForceBV += e.calculateBattleValue(true, true);
                }
            }
            xbv += totalForceBV *= 0.05;
        }

        int finalBV = (int) Math.round(dbv + obv + xbv);

        // and then factor in pilot
        double pilotFactor = 1;
        if (!ignorePilot) {
            pilotFactor = crew.getBVSkillMultiplier();
        }

        int retVal = (int) Math.round((finalBV) * pilotFactor);

        // don't factor pilot in if we are just calculating BV for C3 extra BV
        if (ignoreC3) {
            return finalBV;
        }
        return retVal;
    }

    public int getArcswGuns() {
        //return the number
        int nArcs = 0;
        for(int i = 0; i < locations(); i++) {
            if(hasWeaponInArc(i)) {
                nArcs++;
            }
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

    @Override
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
            if((next instanceof MechBay) || (next instanceof ASFBay) || (next instanceof SmallCraftBay)) {
                bayCost += 20000 * next.totalSpace;
            }
            if((next instanceof LightVehicleBay) || (next instanceof HeavyVehicleBay)) {
                bayCost += 20000 * next.totalSpace;
            }
        }

        cost += bayCost + baydoors * 1000;

        //life boats and escape pods
        cost += 5000 * (getLifeBoats() + getEscapePods());

        double weightMultiplier = 1.25f;

        return Math.round(cost * weightMultiplier);

    }

    @Override
    public boolean doomedOnGround() {
        return true;
    }

    @Override
    public boolean doomedInAtmosphere() {
        return true;
    }

    @Override
    public boolean doomedInSpace() {
        return false;
    }

    /**
     * need to check bay location before loading ammo
     */
    @Override
    public boolean loadWeapon(Mounted mounted, Mounted mountedAmmo) {
        boolean success = false;
        WeaponType wtype = (WeaponType) mounted.getType();
        AmmoType atype = (AmmoType) mountedAmmo.getType();

        if(mounted.getLocation() != mountedAmmo.getLocation()) {
            return success;
        }

        //for large craft, ammo must be in the same ba
        Mounted bay = whichBay(getEquipmentNum(mounted));
        if((bay != null) && !bay.ammoInBay(getEquipmentNum(mountedAmmo))) {
            return success;
        }


        if (mountedAmmo.isAmmoUsable() && !wtype.hasFlag(WeaponType.F_ONESHOT)
                && (atype.getAmmoType() == wtype.getAmmoType())
                && (atype.getRackSize() == wtype.getRackSize())) {
            mounted.setLinked(mountedAmmo);
            success = true;
        }
        return success;
    }

    /*
     * (non-Javadoc)
     * @see megamek.common.Entity#getIniBonus()
     */
    @Override
    public int getHQIniBonus() {
        // large craft are considered to have > 7 tons comm equipment
        // hence they get +2 ini bonus as a mobile hq
        return 2;
    }

    /**
     * what location is opposite the given one
     */
    @Override
    public int getOppositeLocation(int loc) {
        switch(loc) {
        case Jumpship.LOC_NOSE:
            return Jumpship.LOC_AFT;
        case Jumpship.LOC_FLS:
            return Jumpship.LOC_ARS;
        case Jumpship.LOC_FRS:
            return Jumpship.LOC_ALS;
        case Jumpship.LOC_ALS:
            return Jumpship.LOC_FRS;
        case Jumpship.LOC_ARS:
            return Jumpship.LOC_FLS;
        case Jumpship.LOC_AFT:
            return Jumpship.LOC_NOSE;
        default:
            return Jumpship.LOC_NOSE;
        }
    }


    /**
     * All military jumpships automatically have ECM if in space
     */
    @Override
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
    @Override
    public int getECMRange() {
        if(!game.getOptions().booleanOption("stratops_ecm") || !game.getBoard().inSpace()) {
            return super.getECMRange();
        }
        if(!isMilitary()) {
            return Entity.NONE;
        }
        int range = 1;
        //the range might be affected by sensor/FCS damage
        range = range - getSensorHits() - getCICHits();
        return range;
    }

    /**
     * @return is  the crew of this vessel protected from gravitational effects, see StratOps, pg. 36
     */
    @Override
    public boolean isCrewProtected() {
        return isMilitary() && (getOriginalWalkMP() > 4);
    }

    public double getAccumulatedThrust() {
        return accumulatedThrust;
    }

    public void setAccumulatedThrust(double d) {
        accumulatedThrust = d;
    }

    public double getStationKeepingThrust() {
        return stationThrust;
    }

    @Override
    public void newRound(int roundNumber) {
        super.newRound(roundNumber);

        //accumulate some more thrust
        //We assume that thrust will be accumulated. If this is proven wrong by the movement
        //then we make the proper adjustments in server#processMovement
        //until I hear from Welshman, I am assuming that you cannot "hold back" thrust. So once you
        //get 1 thrust point, you have to spend it before you can accumulate more
        if(isDeployed() && (getAccumulatedThrust() < 1.0)) {
            setAccumulatedThrust(getAccumulatedThrust() + stationThrust);
        }
    }

    @Override
    public int getRunMP(boolean gravity, boolean ignoreheat) {
        if(this instanceof Warship) {
            return super.getRunMP(gravity, ignoreheat);
        }
        return (int)Math.floor(getAccumulatedThrust());
    }

    /**
     * find the adjacent firing arc on this vessel clockwise
     */
    public int getAdjacentArcCW(int arc) {
        switch(arc) {
        case Compute.ARC_NOSE:
            return Compute.ARC_RIGHTSIDE_SPHERE;
        case Compute.ARC_LEFTSIDE_SPHERE:
            return Compute.ARC_NOSE;
        case Compute.ARC_RIGHTSIDE_SPHERE:
            return Compute.ARC_RIGHTSIDEA_SPHERE;
        case Compute.ARC_LEFTSIDEA_SPHERE:
            return Compute.ARC_LEFTSIDE_SPHERE;
        case Compute.ARC_RIGHTSIDEA_SPHERE:
            return Compute.ARC_AFT;
        case Compute.ARC_AFT:
            return Compute.ARC_LEFTSIDEA_SPHERE;
        default:
            return Integer.MIN_VALUE;
        }
    }

    /**
     * find the adjacent firing arc on this vessel counter-clockwise
     */
    public int getAdjacentArcCCW(int arc) {
        switch(arc) {
        case Compute.ARC_NOSE:
            return Compute.ARC_LEFTSIDE_SPHERE;
        case Compute.ARC_RIGHTSIDE_SPHERE:
            return Compute.ARC_NOSE;
        case Compute.ARC_LEFTSIDE_SPHERE:
            return Compute.ARC_LEFTSIDEA_SPHERE;
        case Compute.ARC_LEFTSIDEA_SPHERE:
            return Compute.ARC_AFT;
        case Compute.ARC_RIGHTSIDEA_SPHERE:
            return Compute.ARC_RIGHTSIDE_SPHERE;
        case Compute.ARC_AFT:
            return Compute.ARC_RIGHTSIDEA_SPHERE;
        default:
            return Integer.MIN_VALUE;
        }
    }

    @Override
    public double getBVTypeModifier() {
        return 0.75;
    }
}

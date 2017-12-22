/*
 * MegaAero - Copyright (C) 2007 Jay Lawson This program is free software; you
 * can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
/*
 * Created on Jun 17, 2007
 */
package megamek.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import megamek.common.options.OptionsConstants;
import megamek.common.weapons.bayweapons.BayWeapon;

/**
 * @author Jay Lawson
 */
public class Jumpship extends Aero {

    /**
     *
     */
    private static final long serialVersionUID = 9154398176617208384L;
    // Additional Jumpship locations (FLS, FRS and ALS override Aero locations)
    public static final int LOC_FLS = 1;
    public static final int LOC_FRS = 2;
    public static final int LOC_ALS = 4;
    public static final int LOC_ARS = 5;

    public static final int GRAV_DECK_STANDARD_MAX = 100;
    public static final int GRAV_DECK_LARGE_MAX = 250;
    
    public static final int DRIVE_CORE_STANDARD    = 0;
    public static final int DRIVE_CORE_COMPACT     = 1;
    public static final int DRIVE_CORE_SUBCOMPACT  = 2;
    public static final int DRIVE_CORE_NONE        = 3;
    public static final int DRIVE_CORE_PRIMITIVE   = 4;
    
    // The percentage of the total unit weight taken up by the drive core. The value
    // given for primitive assumes a 30ly range, but the final value has to be computed.
    private static double[] DRIVE_CORE_WEIGHT_PCT = { 0.95, 0.4525, 0.5, 0.0, 0.95 };

    private static String[] LOCATION_ABBRS = { "NOS", "FLS", "FRS", "AFT", "ALS", "ARS" };
    private static String[] LOCATION_NAMES = { "Nose", "Left Front Side", "Right Front Side", "Aft", "Aft Left Side",
            "Aft Right Side" };

    private int kf_integrity = 0;
    private int sail_integrity = 0;
    private int driveCoreType = DRIVE_CORE_STANDARD;
    private int jumpRange = 30; // Primitive jumpships can have a reduced range

    // crew and passengers
    private int nCrew = 0;
    private int nPassenger = 0;
    private int nMarines = 0;
    private int nBattleArmor = 0;
    private int nOtherCrew = 0;
    // lifeboats and escape pods
    private int lifeBoats = 0;
    private int escapePods = 0;

    // lithium fusion
    boolean hasLF = false;

    // Battlestation
    private boolean isBattleStation = false;

    // HPG
    private boolean hasHPG = false;

    /**
     * Keep track of all of the grav decks and their sizes.
     *
     * This is a new approach for storing grav decks, which allows the size of each deck to be stored.  Previously,
     * we just stored the number of standard, large and huge grav decks, and could not specify the exact size of the
     * deck.
     */
    private List<Integer> gravDecks = new ArrayList<>();

    // station-keeping thrust and accumulated thrust
    private double stationThrust = 0.2;
    private double accumulatedThrust = 0.0;

    public Jumpship() {
        super();
        damThresh = new int[] { 0, 0, 0, 0, 0, 0 };
    }
    
    
    //ASEW Missile Effects, per location
    //Values correspond to Locations: NOS,FLS,FRS,AFT,ALS,ARS
    private int asewAffectedTurns[] = { 0, 0, 0, 0, 0, 0};
    
    /*
     * Sets the number of rounds a specified firing arc is affected by an ASEW missile
     * @param arc - integer representing the desired firing arc
     * @param turns - integer specifying the number of end phases that the effects last through
     * Technically, about 1.5 turns elapse per the rules for ASEW missiles in TO
     */
    public void setASEWAffected(int arc, int turns) {
        asewAffectedTurns[arc] = turns;
    }
    
    /*
     * Returns the number of rounds a specified firing arc is affected by an ASEW missile
     * @param arc - integer representing the desired firing arc
     */
    public int getASEWAffected(int arc) {
        return asewAffectedTurns[arc];
    }

    protected static final TechAdvancement TA_JUMPSHIP = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_NONE, 2300).setISApproximate(false, true)
            .setProductionFactions(F_TA).setTechRating(RATING_D)
            .setAvailability(RATING_D, RATING_E, RATING_D, RATING_F)
            .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    protected static final TechAdvancement TA_JUMPSHIP_PRIMITIVE = new TechAdvancement(TECH_BASE_IS)
            .setISAdvancement(2100, 2200, DATE_NONE, 2500)
            .setISApproximate(true, true, false, false)
            .setProductionFactions(F_TA).setTechRating(RATING_D)
            .setAvailability(RATING_D, RATING_X, RATING_X, RATING_X)
            .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    
    @Override
    public TechAdvancement getConstructionTechAdvancement() {
        return isPrimitive()? TA_JUMPSHIP_PRIMITIVE : TA_JUMPSHIP;
    }

    public CrewType defaultCrewType() {
        return CrewType.VESSEL;
    }
    
    @Override
    public int locations() {
        return 6;
    }

    /**
     * Get the number of grav decks on the ship.
     *
     * @return
     */
    public int getTotalGravDeck() {
        return gravDecks.size();
    }

    /**
     * Adds a grav deck whose size in meters is specified.
     *
     * @param size  The size in meters of the grav deck.
     */
    public void addGravDeck(int size) {
        gravDecks.add(size);
    }

    /**
     * Get a list of all grav decks mounted on this ship, where each value
     * represents the size in meters of the grav deck.
     *
     * @return
     */
    public List<Integer> getGravDecks() {
        return gravDecks;
    }

    /**
     * Old style for setting the number of grav decks.  This allows the user to specify N standard grav decks, which
     * will get added at a default value.
     *
     * @param n
     */
    public void setGravDeck(int n) {
        for (int i = 0; i < n; i++) {
            gravDecks.add(GRAV_DECK_STANDARD_MAX / 2);
        }
    }

    /**
     * Get the number of standard grav decks
     * @return
     */
    public int getGravDeck() {
        int count = 0;
        for (int size : gravDecks) {
            if (size < GRAV_DECK_STANDARD_MAX) {
                count++;
            }
        }
        return count;
    }

    /**
     * Old style method for adding N large grav decks.  A default value is chosen that is half-way between the standard
     * and huge sizes.
     *
     * @param n
     */
    public void setGravDeckLarge(int n) {
        for (int i = 0; i < n; i++) {
            gravDecks.add(GRAV_DECK_STANDARD_MAX + (GRAV_DECK_LARGE_MAX - GRAV_DECK_STANDARD_MAX) / 2);
        }
    }

    /**
     * Get the number of large grav decks.
     *
     * @return
     */
    public int getGravDeckLarge() {
        int count = 0;
        for (int size : gravDecks) {
            if (size >= GRAV_DECK_STANDARD_MAX && size < GRAV_DECK_LARGE_MAX) {
                count++;
            }
        }
        return count;
    }

    /**
     * Old style method for adding N huge grav decks.  A default value is chosen that is the current large maximum plus
     * half that value.
     *
     * @param n
     */
    public void setGravDeckHuge(int n) {
        for (int i = 0; i < n; i++) {
            gravDecks.add(GRAV_DECK_LARGE_MAX + (GRAV_DECK_LARGE_MAX) / 2);
        }
    }

    /**
     * Get the number of huge grav decks.
     *
     * @return
     */
    public int getGravDeckHuge() {
        int count = 0;
        for (int size : gravDecks) {
            if (size >= GRAV_DECK_LARGE_MAX) {
                count++;
            }
        }
        return count;
    }

    public void setHPG(boolean b) {
        hasHPG = b;
    }

    public boolean hasHPG() {
        return hasHPG;
    }

    public void setBattleStation(boolean b) {
        isBattleStation = b;

    }

    public boolean isBattleStation() {
        return isBattleStation;
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

    @Override
    public int getNCrew() {
        return nCrew;
    }

    public void setNPassenger(int pass) {
        nPassenger = pass;
    }

    @Override
    public int getNPassenger() {
        return nPassenger;
    }

    public void setNMarines(int m) {
        nMarines = m;
    }

    public int getNMarines() {
        return nMarines;
    }

    public void setNBattleArmor(int m) {
        nBattleArmor = m;
    }

    public int getNBattleArmor() {
        return nBattleArmor;
    }

    public void setNOtherCrew(int m) {
        nOtherCrew = m;
    }

    public int getNOtherCrew() {
        return nOtherCrew;
    }
    
    @Override
    public double getFuelPointsPerTon() {
        double ppt;
        if (getWeight() < 110000) {
            ppt = 10;
        } else if (getWeight() < 250000) {
            ppt = 5;
        } else {
            ppt = 2.5;
        }
        if (isPrimitive()) {
            return ppt / primitiveFuelFactor();
        }
        return ppt;
    }

    @Override
    public double getStrategicFuelUse() {
        double fuelUse;
    	if (weight >= 200000) {
    		fuelUse = 3.95;
    	} else if (weight >= 100000) {
    	    fuelUse = 1.98;
    	} else if (weight >= 50000) {
    	    fuelUse = 0.98;
    	} else {
    	    fuelUse = 0.28;
    	}
    	if (isPrimitive()) {
    	    return fuelUse * primitiveFuelFactor();
    	}
    	return fuelUse;
    }

    @Override
    public double primitiveFuelFactor() {
        int year = getOriginalBuildYear();
        if (year >= 2300) {
            return 1.0;
        } else if (year >= 2251) {
            return 1.1;
        } else if (year >= 2201) {
            return 1.4;
        } else if (year >= 2151) {
            return 1.7;
        } else {
            return 2.0;
        }
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
        int integrity = 1 + (int) Math.ceil((30.0 + (weight / 7500.0)) / 20.0);
        setSailIntegrity(integrity);
    }

    public void initializeKFIntegrity() {
        int integrity = (int) Math.ceil(1.2 + (((0.95) * weight) / 60000.0));
        setKFIntegrity(integrity);
    }

    public boolean canJump() {
        return kf_integrity > 0;
    }
    
    public int getDriveCoreType() {
        return driveCoreType;
    }
    
    public void setDriveCoreType(int driveCoreType) {
        this.driveCoreType = driveCoreType;
    }

    /**
     * Get maximum range of a jump
     */
    public int getJumpRange() {
        return jumpRange;
    }
    
    /**
     * Set maximum jump range (used for primitive jumpships)
     */
    public void setJumpRange(int range) {
        jumpRange = range;
    }
    
    /**
     * @return The weight of the jump drive core for this unit
     */
    public double getJumpDriveWeight() {
        double pct = DRIVE_CORE_WEIGHT_PCT[driveCoreType];
        if (driveCoreType == DRIVE_CORE_PRIMITIVE) {
            pct = 0.05 + 0.03 * jumpRange;
        }
        return Math.ceil(getWeight() * pct); 
    }

    // different firing arcs
    // different firing arcs
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

    // different hit locations
    @Override
    public HitData rollHitLocation(int table, int side) {

        /*
         * Unlike other units, ASFs determine potential crits based on the
         * to-hit roll so I need to set this potential value as well as return
         * the to hit data
         */

        int roll = Compute.d6(2);
        if (side == ToHitData.SIDE_FRONT) {
            // normal front hits
            switch (roll) {
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
        } else if (side == ToHitData.SIDE_LEFT) {
            // normal left-side hits
            switch (roll) {
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
        } else if (side == ToHitData.SIDE_RIGHT) {
            // normal left-side hits
            switch (roll) {
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
        } else if (side == ToHitData.SIDE_REAR) {
            // normal aft hits
            switch (roll) {
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
        if (useManualBV) {
            return manualBV;
        }
        double dbv = 0; // defensive battle value
        double obv = 0; // offensive bv

        int modularArmor = 0;
        for (Mounted mounted : getEquipment()) {
            if ((mounted.getType() instanceof MiscType) && mounted.getType().hasFlag(MiscType.F_MODULAR_ARMOR)) {
                modularArmor += mounted.getBaseDamageCapacity() - mounted.getDamageTaken();
            }
        }

        dbv += (getTotalArmor() + modularArmor) * 25.0;

        dbv += getSI() * 20.0;

        // add defensive equipment
        double amsBV = 0;
        double amsAmmoBV = 0;
        double screenBV = 0;
        double screenAmmoBV = 0;
        double defEqBV = 0;
        for (Mounted mounted : getEquipment()) {
            EquipmentType etype = mounted.getType();

            // don't count destroyed equipment
            if (mounted.isDestroyed()) {
                continue;
            }
            if (((etype instanceof WeaponType) && (etype.hasFlag(WeaponType.F_AMS)))) {
                amsBV += etype.getBV(this);
            } else if ((etype instanceof AmmoType) && (((AmmoType) etype).getAmmoType() == AmmoType.T_AMS)) {
                amsAmmoBV += etype.getBV(this);
            } else if ((etype instanceof AmmoType)
                    && (((AmmoType) etype).getAmmoType() == AmmoType.T_SCREEN_LAUNCHER)) {
                screenAmmoBV += etype.getBV(this);
            } else if ((etype instanceof WeaponType)
                    && (((WeaponType) etype).getAtClass() == WeaponType.CLASS_SCREEN)) {
                screenBV += etype.getBV(this);
            } else if ((etype instanceof MiscType)
                    && (etype.hasFlag(MiscType.F_ECM) || etype.hasFlag(MiscType.F_BAP))) {
                defEqBV += etype.getBV(this);
            }
        }
        dbv += amsBV;
        dbv += screenBV;
        dbv += Math.min(amsBV, amsAmmoBV);
        dbv += Math.min(screenBV, screenAmmoBV);
        dbv += defEqBV;

        // unit type multiplier
        dbv *= getBVTypeModifier();

        // calculate heat efficiency
        int aeroHeatEfficiency = getHeatCapacity();

        // get arc BV and heat
        // and add up BVs for ammo-using weapon types for excessive ammo rule
        TreeMap<String, Double> weaponsForExcessiveAmmo = new TreeMap<String, Double>();
        TreeMap<Integer, Double> arcBVs = new TreeMap<Integer, Double>();
        TreeMap<Integer, Double> arcHeat = new TreeMap<Integer, Double>();
        for (Mounted mounted : getTotalWeaponList()) {
            WeaponType wtype = (WeaponType) mounted.getType();
            double weaponHeat = wtype.getHeat();
            int arc = getWeaponArc(getEquipmentNum(mounted));
            double dBV = wtype.getBV(this);
            // skip bays
            if (wtype instanceof BayWeapon) {
                continue;
            }
            // don't count defensive weapons
            if (wtype.hasFlag(WeaponType.F_AMS)) {
                continue;
            }
            // don't count screen launchers, they are defensive
            if (wtype.getAtClass() == WeaponType.CLASS_SCREEN) {
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
            // add up BV of ammo-using weapons for each type of weapon,
            // to compare with ammo BV later for excessive ammo BV rule
            if (!((wtype.hasFlag(WeaponType.F_ENERGY) && !(wtype.getAmmoType() == AmmoType.T_PLASMA))
                    || wtype.hasFlag(WeaponType.F_ONESHOT) || wtype.hasFlag(WeaponType.F_INFANTRY)
                    || (wtype.getAmmoType() == AmmoType.T_NA))) {
                String key = wtype.getAmmoType() + ":" + wtype.getRackSize() + ";" + arc;
                if (!weaponsForExcessiveAmmo.containsKey(key)) {
                    weaponsForExcessiveAmmo.put(key, wtype.getBV(this));
                } else {
                    weaponsForExcessiveAmmo.put(key, wtype.getBV(this) + weaponsForExcessiveAmmo.get(key));
                }
            }
            // calc MG Array here:
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
                if ((mLinker.getType() instanceof MiscType) && mLinker.getType().hasFlag(MiscType.F_ARTEMIS_PROTO)) {
                    dBV *= 1.1;
                }
                if ((mLinker.getType() instanceof MiscType) && mLinker.getType().hasFlag(MiscType.F_ARTEMIS_V)) {
                    dBV *= 1.3;
                }
                if ((mLinker.getType() instanceof MiscType) && mLinker.getType().hasFlag(MiscType.F_APOLLO)) {
                    dBV *= 1.15;
                }
                if ((mLinker.getType() instanceof MiscType)
                        && mLinker.getType().hasFlag(MiscType.F_RISC_LASER_PULSE_MODULE)) {
                    dBV *= 1.25;
                }
            }

            double currentArcBV = 0.0;
            double currentArcHeat = 0.0;
            if (null != arcBVs.get(arc)) {
                currentArcBV = arcBVs.get(arc);
            }
            if (null != arcHeat.get(arc)) {
                currentArcHeat = arcHeat.get(arc);
            }
            arcBVs.put(arc, currentArcBV + dBV);
            arcHeat.put(arc, currentArcHeat + weaponHeat);
        }
        double weaponBV = 0.0;
        // lets traverse the hash and find the highest value BV arc
        int highArc = Integer.MIN_VALUE;
        int adjArc = Integer.MIN_VALUE;
        int oppArc = Integer.MIN_VALUE;
        double adjArcMult = 1.0;
        double oppArcMult = 0.5;
        double highBV = 0.0;
        double heatUsed = 0.0;
        Set<Integer> set = arcBVs.keySet();
        Iterator<Integer> iter = set.iterator();
        while (iter.hasNext()) {
            int key = iter.next();
            if ((arcBVs.get(key) > highBV) && ((key == Compute.ARC_NOSE) || (key == Compute.ARC_LEFT_BROADSIDE)
                    || (key == Compute.ARC_RIGHT_BROADSIDE) || (key == Compute.ARC_AFT))) {
                highArc = key;
                highBV = arcBVs.get(key);
            }
        }
        // now lets identify the adjacent and opposite arcs
        if (highArc > Integer.MIN_VALUE) {
            heatUsed += arcHeat.get(highArc);
            // now get the BV and heat for the two adjacent arcs
            int adjArcCW = getAdjacentArcCW(highArc);
            int adjArcCCW = getAdjacentArcCCW(highArc);
            double adjArcCWBV = 0.0;
            double adjArcCWHeat = 0.0;
            if ((adjArcCW > Integer.MIN_VALUE) && (null != arcBVs.get(adjArcCW))) {
                adjArcCWBV = arcBVs.get(adjArcCW);
                adjArcCWHeat = arcHeat.get(adjArcCW);
            }
            double adjArcCCWBV = 0.0;
            double adjArcCCWHeat = 0.0;
            if ((adjArcCCW > Integer.MIN_VALUE) && (null != arcBVs.get(adjArcCCW))) {
                adjArcCCWBV = arcBVs.get(adjArcCCW);
                adjArcCCWHeat = arcHeat.get(adjArcCCW);
            }
            if (adjArcCWBV > adjArcCCWBV) {
                adjArc = adjArcCW;
                if ((heatUsed + adjArcCWHeat) > aeroHeatEfficiency) {
                    adjArcMult = 0.5;
                }
                heatUsed += adjArcCWHeat;
                oppArc = adjArcCCW;
                if ((heatUsed + adjArcCCWHeat) > aeroHeatEfficiency) {
                    oppArcMult = 0.25;
                }
            } else {
                adjArc = adjArcCCW;
                if ((heatUsed + adjArcCCWHeat) > aeroHeatEfficiency) {
                    adjArcMult = 0.5;
                }
                heatUsed += adjArcCCWHeat;
                oppArc = adjArcCW;
                if ((heatUsed + adjArcCWHeat) > aeroHeatEfficiency) {
                    oppArcMult = 0.25;
                }
            }
        }
        // According to an email with Welshman, ammo should be now added into
        // each arc BV
        // for the final calculation of BV, including the excessive ammo rule
        Map<String, Double> ammo = new HashMap<String, Double>();
        ArrayList<String> keys = new ArrayList<String>();
        for (Mounted mounted : getAmmo()) {
            int arc = getWeaponArc(getEquipmentNum(mounted));
            AmmoType atype = (AmmoType) mounted.getType();

            // don't count depleted ammo
            if (mounted.getUsableShotsLeft() == 0) {
                continue;
            }

            // don't count AMS, it's defensive
            if (atype.getAmmoType() == AmmoType.T_AMS) {
                continue;
            }
            // don't count screen launchers, they are defensive
            if (atype.getAmmoType() == AmmoType.T_SCREEN_LAUNCHER) {
                continue;
            }
            // don't count oneshot ammo, it's considered part of the launcher.
            if (mounted.getLocation() == Entity.LOC_NONE) {
                // assumption: ammo without a location is for a oneshot weapon
                continue;
            }
            String key = atype.getAmmoType() + ":" + atype.getRackSize() + ";" + arc;
            double ammoWeight = mounted.getType().getTonnage(this);
            if (atype.isCapital()) {
                ammoWeight = mounted.getUsableShotsLeft() * atype.getAmmoRatio();
            }
            // new errata: round partial tons of ammo up to the full ton
            ammoWeight = Math.ceil(weight);
            if (atype.hasFlag(AmmoType.F_CAP_MISSILE)) {
                ammoWeight = mounted.getUsableShotsLeft();
            }
            if (!keys.contains(key)) {
                keys.add(key);
            }
            if (!ammo.containsKey(key)) {
                ammo.put(key, ammoWeight * atype.getBV(this));
            } else {
                ammo.put(key, (ammoWeight * atype.getBV(this)) + ammo.get(key));
            }
        }

        // Excessive ammo rule:
        // Only count BV for ammo for a weapontype until the BV of all weapons
        // in that arc is reached
        for (String key : keys) {
            double ammoBV = 0.0;
            int arc = Integer.parseInt(key.split(";")[1]);
            // get the arc
            if (weaponsForExcessiveAmmo.get(key) != null) {
                if (ammo.get(key) > weaponsForExcessiveAmmo.get(key)) {
                    ammoBV += weaponsForExcessiveAmmo.get(key);
                } else {
                    ammoBV += ammo.get(key);
                }
            }
            double currentArcBV = 0.0;
            if (null != arcBVs.get(arc)) {
                currentArcBV = arcBVs.get(arc);
            }
            arcBVs.put(arc, currentArcBV + ammoBV);
        }

        // ok now lets go in and add the arcs
        if (highArc > Integer.MIN_VALUE) {
            // ok now add the BV from this arc and reset to zero
            weaponBV += arcBVs.get(highArc);
            arcBVs.put(highArc, 0.0);
            if ((adjArc > Integer.MIN_VALUE) && (null != arcBVs.get(adjArc))) {
                weaponBV += adjArcMult * arcBVs.get(adjArc);
                arcBVs.put(adjArc, 0.0);
            }
            if ((oppArc > Integer.MIN_VALUE) && (null != arcBVs.get(oppArc))) {
                weaponBV += oppArcMult * arcBVs.get(oppArc);
                arcBVs.put(oppArc, 0.0);
            }
            // ok now we can cycle through the rest and add 25%
            set = arcBVs.keySet();
            iter = set.iterator();
            while (iter.hasNext()) {
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
        if (!(this instanceof Warship) && !(this instanceof SpaceStation)) {
            runMp = 1;
        }
        double speedFactor = Math.pow(1 + (((double) runMp - 5) / 10), 1.2);
        speedFactor = Math.round(speedFactor * 100) / 100.0;

        obv = weaponBV * speedFactor;

        double finalBV;
        if (useGeometricMeanBV()) {
            finalBV = 2 * Math.sqrt(obv * dbv);
            if (finalBV == 0) {
                finalBV = dbv + obv;
            }
        } else {
            finalBV = dbv + obv;
        }

        // we get extra bv from some stuff
        double xbv = 0.0;
        // extra from c3 networks. a valid network requires at least 2 members
        // some hackery and magic numbers here. could be better
        // also, each 'has' loops through all equipment. inefficient to do it 3
        // times
        if (!ignoreC3 && (game != null)) {
            xbv += getExtraC3BV((int) Math.round(finalBV));
        }

        finalBV = Math.round(finalBV + xbv);

        // and then factor in pilot
        double pilotFactor = 1;
        if (!ignorePilot) {
            pilotFactor = getCrew().getBVSkillMultiplier(game);
        }

        int retVal = (int) Math.round((finalBV) * pilotFactor);

        return retVal;
    }

    public int getArcswGuns() {
        // return the number
        int nArcs = 0;
        for (int i = 0; i < locations(); i++) {
            if (hasWeaponInArc(i)) {
                nArcs++;
            }
        }
        return nArcs;
    }

    public boolean hasWeaponInArc(int loc) {
        boolean hasWeapons = false;
        for (Mounted weap : getWeaponList()) {
            if (weap.getLocation() == loc) {
                hasWeapons = true;
            }
        }
        return hasWeapons;
    }

    public double getFuelPerTon() {

        double points = 10.0;

        if (weight >= 250000) {
            points = 2.5;
            return points;
        } else if (weight >= 110000) {
            points = 5.0;
            return points;
        }

        return points;
    }

    @Override
    public double getArmorWeight(int loc) {
        double armorPoints = getTotalOArmor();

        // now I need to determine base armor points by type and weight
        double baseArmor = 0.8;
        if (isClan()) {
            baseArmor = 1.0;
        }

        if (weight >= 250000) {
            baseArmor = 0.4;
            if (isClan()) {
                baseArmor = 0.5;
            }
        } else if (weight >= 150000) {
            baseArmor = 0.6;
            if (isClan()) {
                baseArmor = 0.7;
            }
        }

        if (armorType[0] == EquipmentType.T_ARMOR_LC_FERRO_IMP) {
            baseArmor += 0.2;
        } else if (armorType[0] == EquipmentType.T_ARMOR_LC_FERRO_CARBIDE) {
            baseArmor += 0.4;
        } else if (armorType[0] == EquipmentType.T_ARMOR_LC_LAMELLOR_FERRO_CARBIDE) {
            baseArmor += 0.6;
        }

        double armorPerTon = baseArmor;
        double armWeight = 0.0;
        for (; (armWeight * armorPerTon) < armorPoints; armWeight += .5) {
            // add armor in discrete batches
        }
        return armWeight;
    }

    @Override
    public double getCost(boolean ignoreAmmo) {
        double[] costs = new double[22];
        int costIdx = 0;
        double cost = 0;

        // Control Systems
        // Bridge
        costs[costIdx++] += 200000 + 10 * weight;
        // Computer
        costs[costIdx++] += 200000;
        // Life Support
        costs[costIdx++] += 5000 * (getNCrew() + getNPassenger());
        // Sensors
        costs[costIdx++] += 80000;
        // Fire Control Computer
        costs[costIdx++] += 100000;
        // Gunnery Control Systems
        costs[costIdx++] += 10000 * getArcswGuns();
        // Structural Integrity
        costs[costIdx++] += 100000 * getSI();

        // Station-Keeping Drive
        // Engine
        costs[costIdx++] += 1000 * weight * 0.012;
        // Engine Control Unit
        costs[costIdx++] += 1000;

        // KF Drive
        double[] driveCost = new double[6];
        int driveIdx = 0;
        double driveCosts = 0;
        // Drive Coil
        driveCost[driveIdx++] += 60000000 + (75000000 * getDocks());
        // Initiator
        driveCost[driveIdx++] += 25000000 + (5000000 * getDocks());
        // Controller
        driveCost[driveIdx++] += 50000000;
        // Tankage
        driveCost[driveIdx++] += 50000 * getKFIntegrity();
        // Sail
        driveCost[driveIdx++] += 50000 * (30 + (weight / 7500));
        // Charging System
        driveCost[driveIdx++] += 500000 + (200000 * getDocks()); 
        
        for (int i = 0; i < driveIdx; i++) {
            driveCosts += driveCost[i];
        }

        if (hasLF()) {
            driveCosts *= 3;
        }
        
        costs[costIdx++] += driveCosts;

        // K-F Drive Support Systems
        costs[costIdx++] += 10000000 * (weight / 10000);

        // Additional Ships Systems
        // Attitude Thrusters
        costs[costIdx++] += 25000;
        // Docking Collars
        costs[costIdx++] += 100000 * getDocks();
        // Fuel Tanks
        costs[costIdx++] += (200 * getFuel()) / getFuelPerTon() * 1.02;

        // Armor
        costs[costIdx++] += getArmorWeight(locations()) * EquipmentType.getArmorCost(armorType[0]);

        // Heat Sinks
        int sinkCost = 2000 + (4000 * getHeatType());
        costs[costIdx++] += sinkCost * getHeatSinks();

        // Escape Craft
        costs[costIdx++] += 5000 * (getLifeBoats() + getEscapePods());

        // Grav Decks
        double deckCost = 0;
        deckCost += 5000000 * getGravDeck();
        deckCost += 10000000 * getGravDeckLarge();
        deckCost += 40000000 * getGravDeckHuge();
        costs[costIdx++] += deckCost;

        // Transport Bays
        int baydoors = 0;
        int bayCost = 0;
        for (Bay next : getTransportBays()) {
            baydoors += next.getDoors();
            if ((next instanceof MechBay) || (next instanceof ASFBay) || (next instanceof SmallCraftBay)) {
                bayCost += 20000 * next.totalSpace;
            }
            if ((next instanceof LightVehicleBay) || (next instanceof HeavyVehicleBay)) {
                bayCost += 20000 * next.totalSpace;
            }
        }

        costs[costIdx++] += bayCost + (baydoors * 1000);

        // Weapons and Equipment
        // HPG
        if (hasHPG()) {
            costs[costIdx++] += 1000000000;
        } else {
            costs[costIdx++] += 0;
        }
        // Weapons and Equipment
        costs[costIdx++] += getWeaponsAndEquipmentCost(ignoreAmmo);

        double weightMultiplier = 1.25f;

        // Sum Costs
        for (int i = 0; i < costIdx; i++) {
            cost += costs[i];
        }

        costs[costIdx++] = -weightMultiplier; // Negative indicates multiplier
        cost = Math.round(cost * weightMultiplier);

        return cost;

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

        if (mounted.getLocation() != mountedAmmo.getLocation()) {
            return success;
        }

        // for large craft, ammo must be in the same ba
        Mounted bay = whichBay(getEquipmentNum(mounted));
        if ((bay != null) && !bay.ammoInBay(getEquipmentNum(mountedAmmo))) {
            return success;
        }

        if (mountedAmmo.isAmmoUsable() && !wtype.hasFlag(WeaponType.F_ONESHOT)
                && (atype.getAmmoType() == wtype.getAmmoType()) && (atype.getRackSize() == wtype.getRackSize())) {
            mounted.setLinked(mountedAmmo);
            success = true;
        }
        return success;
    }

    /*
     * (non-Javadoc)
     *
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
        switch (loc) {
        case LOC_NOSE:
            return LOC_AFT;
        case LOC_FLS:
            return LOC_ARS;
        case LOC_FRS:
            return LOC_ALS;
        case LOC_ALS:
            return LOC_FRS;
        case LOC_ARS:
            return LOC_FLS;
        case LOC_AFT:
            return LOC_NOSE;
        default:
            return LOC_NOSE;
        }
    }

    /**
     * All military jumpships automatically have ECM if in space
     */
    @Override
    public boolean hasActiveECM() {
        if (!game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ECM)
                || !game.getBoard().inSpace()) {
            return super.hasActiveECM();
        }
        return getECMRange() >= 0;
    }

    /**
     * What's the range of the ECM equipment?
     *
     * @return the <code>int</code> range of this unit's ECM. This value will be
     *         <code>Entity.NONE</code> if no ECM is active.
     */
    @Override
    public int getECMRange() {
        if (!game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ECM)
                || !game.getBoard().inSpace()) {
            return super.getECMRange();
        }
        if (!isMilitary()) {
            return Entity.NONE;
        }
        int range = 1;
        // the range might be affected by sensor/FCS damage
        range = range - getSensorHits() - getCICHits();
        return range;
    }

    /**
     * @return is the crew of this vessel protected from gravitational effects,
     *         see StratOps, pg. 36
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

        // accumulate some more 
        // We assume that  will be accumulated. If this is proven wrong by
        // the movement
        // then we make the proper adjustments in server#processMovement
        // until I hear from Welshman, I am assuming that you cannot "hold back"
        // thrust. So once you
        // get 1 thrust point, you have to spend it before you can accumulate
        // more
        if (isDeployed() && (isBattleStation() == true)) {
            setAccumulatedThrust(1);
        }

        if (isDeployed() && (getAccumulatedThrust() < 1.0)) {
            setAccumulatedThrust(getAccumulatedThrust() + stationThrust);
        }
    }

    @Override
    public int getRunMP(boolean gravity, boolean ignoreheat, boolean ignoremodulararmor) {
        if (this instanceof Warship) {
            return super.getRunMP(gravity, ignoreheat, ignoremodulararmor);
        }
        return (int) Math.floor(getAccumulatedThrust());
    }

    /**
     * find the adjacent firing arc on this vessel clockwise
     */
    public int getAdjacentArcCW(int arc) {
        switch (arc) {
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
        switch (arc) {
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

    @Override
    public boolean usesWeaponBays() {
        return true;
    }
    
    @Override
    public void setAlphaStrikeMovement(Map<String,Integer> moves) {
        moves.put("k", (int)(getStationKeepingThrust() * 10));
    }

    @Override
    public int getBattleForceSize() {
        // The tables are on page 356 of StartOps
        if (getWeight() < 100000) {
            return 1;
        }
        if (getWeight() < 300000) {
            return 2;
        }
        return 3;
    }

    @Override
    public int getBattleForceStructurePoints() {
        return 1;
    }
    
    @Override
    public int getNumBattleForceWeaponsLocations() {
        return 4;
    }
    
    @Override
    public String getBattleForceLocationName(int index) {
        // Remove leading F from FLS and FRS
        String retVal = getLocationAbbrs()[index];
        if (retVal.substring(0, 1).equals("F")) {
            return retVal.substring(1);
        }
        return retVal;
    }

    @Override
    public double getBattleForceLocationMultiplier(int index, int location, boolean rearMounted) {
        switch (index) {
        case LOC_NOSE:
            if (location == LOC_NOSE) {
                return 1.0;
            }
            if (isSpheroid() && (location == LOC_FLS || location == LOC_FRS)
                    && !rearMounted) {
                return 0.5;
            }
            break;
        case LOC_FRS:
            if (location == LOC_FRS || location == LOC_ARS) {
                return 0.5;
            }
            break;
        case LOC_FLS:
            if (location == LOC_FLS || location == LOC_ALS) {
                return 0.5;
            }
            break;
        case LOC_AFT:
            if (location == LOC_AFT) {
                return 1.0;
            }
            if (location == LOC_ALS || location == LOC_ARS) {
                return 0.5;
            }
            break;
        }
        return 0;
    }
    
    @Override
    public void addBattleForceSpecialAbilities(Map<BattleForceSPA,Integer> specialAbilities) {
        super.addBattleForceSpecialAbilities(specialAbilities);
        specialAbilities.put(BattleForceSPA.KF, null);
        if (hasLF()) {
            specialAbilities.put(BattleForceSPA.LF, null);
        }        
        if (getNCrew() >= 60) {
            specialAbilities.put(BattleForceSPA.CRW, (int)Math.round(getNCrew() / 120.0));
        }
    }
    
    @Override
    public boolean isFighter() {
        return false;
    }

    @Override
    public long getEntityType() {
        return Entity.ETYPE_AERO | Entity.ETYPE_JUMPSHIP;
    }

    /**
     * Do not recalculate walkMP when adding engine.
     */
    @Override
    protected int calculateWalk() {
    	return walkMP;
    }
}

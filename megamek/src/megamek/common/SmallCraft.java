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

import java.util.Map;

import megamek.common.options.OptionsConstants;

/**
 * @author Jay Lawson
 */
public class SmallCraft extends Aero {

    /**
     *
     */
    private static final long serialVersionUID = 6708788176436555036L;
    private static String[] LOCATION_ABBRS =
        { "NOS", "LS", "RS", "AFT" };
    private static String[] LOCATION_NAMES =
        { "Nose", "Left Side", "Right Side", "Aft" };

    // crew and passengers
    private int nCrew = 0;
    private int nPassenger = 0;
    private int nOfficers = 0;
    private int nGunners = 0;
    private int nBattleArmor = 0;
    private int nMarines = 0;
    private int nOtherPassenger = 0;

    // Is it Civilian or Military
    public static final int CIVILIAN = 0;
    public static final int MILITARY = 1;
    private int designType = 0;
    
    // escape pods and lifeboats
    private int escapePods = 0;
    private int lifeBoats = 0;
    
    private final static TechAdvancement TA_SM_CRAFT = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_NONE, 2350, 2400).setISApproximate(false, true, false)
            .setProductionFactions(F_TH).setTechRating(RATING_D)
            .setAvailability(RATING_D, RATING_E, RATING_D, RATING_D)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
    private final static TechAdvancement TA_SM_CRAFT_PRIMITIVE = new TechAdvancement(TECH_BASE_IS)
            .setISAdvancement(DATE_ES, 2200, DATE_NONE, 2400)
            .setISApproximate(false, true, false, false)
            .setProductionFactions(F_TA).setTechRating(RATING_D)
            .setAvailability(RATING_D, RATING_X, RATING_F, RATING_F)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);

    @Override
    public TechAdvancement getConstructionTechAdvancement() {
        if (isPrimitive()) {
            return TA_SM_CRAFT_PRIMITIVE;
        } else {
            return TA_SM_CRAFT;
        }
    }
    
    @Override
    public boolean isPrimitive() {
        return getArmorType(LOC_NOSE) == EquipmentType.T_ARMOR_PRIMITIVE_AERO;
    }

    public void setDesignType(int design) {
        designType = design;
    }

    public int getDesignType() {
        return designType;
    }
    
    public void setNCrew(int crew) {
        nCrew = crew;
    }
    
    public void setNOfficers(int officer) {
        nOfficers = officer;
    }
    
    public void setNGunners(int gunners) {
        nGunners = gunners;
    }
    
    public void setNPassenger(int pass) {
        nPassenger = pass;
    }

    public void setNBattleArmor(int ba) {
        nBattleArmor = ba;
    }

    public void setNMarines(int marines) {
        nMarines = marines;
    }

    public void setNOtherPassenger(int other) {
        nOtherPassenger = other;
    }

    @Override
    public int getNCrew() {
        return nCrew;
    }

    @Override
    public int getNPassenger() {
        return nPassenger;
    }
    
    @Override
    public int getNOfficers() {
        return nOfficers;
    }
    
    @Override
    public int getNGunners() {
        return nGunners;
    }
    
    @Override
    public int getNBattleArmor() {
        return nBattleArmor;
    }

    @Override
    public int getNMarines() {
        return nMarines;
    }

    public int getNOtherPassenger() {
        return nOtherPassenger;
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
    
    @Override
    public double getStrategicFuelUse() {
        if (isPrimitive()) {
            return 1.84 * primitiveFuelFactor();
        }
    	return 1.84;
    }

    @Override
    public double primitiveFuelFactor() {
        int year = getOriginalBuildYear();
        if (year >= 2500) {
            return 1.0;
        } else if (year >= 2400) {
            return 1.2;
        } else if (year >= 2300) {
            return 1.4;
        } else if (year >= 2251) {
            return 1.5;
        } else if (year >= 2201) {
            return 1.7;
        } else if (year >= 2151) {
            return 1.9;
        } else {
            return 2.2;
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

    @Override
    public int locations() {
        return 4;
    }

    // what is different - hit table is about it
    @Override
    public HitData rollHitLocation(int table, int side) {

        /*
         * Unlike other units, ASFs determine potential crits based on the to-hit roll
         * so I need to set this potential value as well as return the to hit data
         */

        int roll = Compute.d6(2);

        // special rules for spheroids in atmosphere
        // http://www.classicbattletech.com/forums/index.php/topic,54077.0.html
        if (isSpheroid() && table != ToHitData.HIT_SPHEROID_CRASH && 
                !game.getBoard().inSpace()) {
            int preroll = Compute.d6(1);
            if ((table == ToHitData.HIT_ABOVE) && (preroll < 4)) {
                side = ToHitData.SIDE_FRONT;
            } else if ((table == ToHitData.HIT_BELOW) && (preroll < 4)) {
                side = ToHitData.SIDE_REAR;
            } else if (preroll == 1) {
                side = ToHitData.SIDE_FRONT;
            } else if (preroll == 6) {
                side = ToHitData.SIDE_REAR;
            }
        }

        if ((table == ToHitData.HIT_ABOVE) || (table == ToHitData.HIT_BELOW)) {

            // have to decide which wing
            int wingloc = LOC_RWING;
            int wingroll = Compute.d6(1);
            if (wingroll > 3) {
                wingloc = LOC_LWING;
            }
            switch (roll) {
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
                    if (wingroll > 3) {
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
                    if (wingroll > 3) {
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

        if (side == ToHitData.SIDE_FRONT) {
            // normal front hits
            switch (roll) {
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
                    // Primitve dropships without kf-boom take avionics hit instead (IO, p. 119).
                    if ((this instanceof Dropship)
                            && (((Dropship)this).getCollarType() == Dropship.COLLAR_NO_BOOM)) {
                        setPotCrit(CRIT_AVIONICS);
                    }
                    return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
            }
        } else if (side == ToHitData.SIDE_LEFT) {
            // normal left-side hits
            switch (roll) {
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
        } else if (side == ToHitData.SIDE_RIGHT) {
            // normal right-side hits
            switch (roll) {
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
        } else if (side == ToHitData.SIDE_REAR) {
            // normal aft hits
            switch (roll) {
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

    // weapon arcs
    @Override
    public int getWeaponArc(int wn) {
        final Mounted mounted = getEquipment(wn);

        int arc = Compute.ARC_NOSE;
        if (!isSpheroid()) {
            switch (mounted.getLocation()) {
                case LOC_NOSE:
                    arc = Compute.ARC_NOSE;
                    break;
                case LOC_RWING:
                    if (mounted.isRearMounted()) {
                        arc = Compute.ARC_RWINGA;
                    } else {
                        arc = Compute.ARC_RWING;
                    }
                    break;
                case LOC_LWING:
                    if (mounted.isRearMounted()) {
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
            if ((game != null) && game.getBoard().inSpace()) {
                switch (mounted.getLocation()) {
                    case LOC_NOSE:
                        arc = Compute.ARC_NOSE;
                        break;
                    case LOC_RWING:
                        if (mounted.isRearMounted()) {
                            arc = Compute.ARC_RIGHTSIDEA_SPHERE;
                        } else {
                            arc = Compute.ARC_RIGHTSIDE_SPHERE;
                        }
                        break;
                    case LOC_LWING:
                        if (mounted.isRearMounted()) {
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
            } else {
                switch (mounted.getLocation()) {
                    case LOC_NOSE:
                        arc = Compute.ARC_360;
                        break;
                    case LOC_RWING:
                        arc = Compute.ARC_RIGHT_SPHERE_GROUND;
                        break;
                    case LOC_LWING:
                        arc = Compute.ARC_LEFT_SPHERE_GROUND;
                        break;
                    case LOC_AFT:
                        arc = Compute.ARC_360;
                        break;
                    default:
                        arc = Compute.ARC_360;
                }
            }

        }

        return rollArcs(arc);

    }

    public int getArcswGuns() {
        // return the number
        int nArcs = 0;
        for (int i = 0; i < locations(); i++) {
            if (hasWeaponInArc(i, false)) {
                nArcs++;
            }
            // check for rear locations
            if (hasWeaponInArc(i, true)) {
                nArcs++;
            }
        }
        return nArcs;
    }

    public boolean hasWeaponInArc(int loc, boolean rearMount) {
        boolean hasWeapons = false;
        for (Mounted weap : getWeaponList()) {
            if ((weap.getLocation() == loc) && (weap.isRearMounted() == rearMount)) {
                hasWeapons = true;
            }
        }
        return hasWeapons;
    }

    @Override
    public double getArmorWeight() {
        // first I need to subtract SI bonus from total armor
        int armorPoints = getTotalOArmor();
        armorPoints -= getSI() * locations();
        double armorPerTon = SmallCraft.armorPointsPerTon(getWeight(), isSpheroid(),
                getArmorType(0), TechConstants.isClan(getArmorTechLevel(0)));

        return Math.ceil(2.0 * armorPoints / armorPerTon) / 2.0;
    }
    
    public static double armorPointsPerTon(double craftWeight, boolean spheroid, int at, boolean isClan) {
        double base = 16.0;
        if (spheroid) {
            if (craftWeight >= 65000) {
                base = 6.0;
            } else if (craftWeight >= 50000) {
                base = 8.0;
            } else if (craftWeight >= 35000) {
                base = 10.0;
            } else if (craftWeight >= 20000) {
                base = 12.0;
            } else if (craftWeight >= 12500) {
                base = 14.0;
            }
        } else {
            if (craftWeight >= 25000) {
                base = 6.0;
            } else if (craftWeight >= 17500) {
                base = 8.0;
            } else if (craftWeight >= 12500) {
                base = 10.0;
            } else if (craftWeight >= 9500) {
                base = 12.0;
            } else if (craftWeight >= 6000) {
                base = 14.0;
            }
        }
        if (isClan) {
            if (base > 14) {
                base += 4;
            } else if (base > 12) {
                base += 3;
            } else if (base > 6) {
                base += 2;
            } else {
                base += 1;
            }
        }

        return base * EquipmentType.getArmorPointMultiplier(at, isClan);
    }

    /**
     * There is a mistake in some of the AT2r costs for some reason they added
     * ammo twice for a lot of the level 2 designs, leading to costs that are
     * too high
     */
    @Override
    public double getCost(boolean ignoreAmmo) {

        double cost = 0;

        // add in controls
        // bridge
        cost += 200000 + (10 * weight);
        // computer
        cost += 200000;
        // life support
        cost += 5000 * (getNCrew() + getNPassenger());
        // sensors
        cost += 80000;
        // fcs
        cost += 100000;
        // gunnery/control systems
        cost += 10000 * getArcswGuns();

        // structural integrity
        cost += 100000 * getSI();

        // additional flight systems (attitude thruster and landing gear)
        cost += 25000 + (10 * getWeight());

        // engine
        double engineMultiplier = 0.065;
        if (isClan()) {
            engineMultiplier = 0.061;
        }
        double engineWeight = getOriginalWalkMP() * weight * engineMultiplier;
        cost += engineWeight * 1000;
        // drive unit
        cost += (500 * getOriginalWalkMP() * weight) / 100.0;

        // fuel tanks
        cost += (200 * getFuel()) / 80.0 * 1.02;

        // armor
        cost += getArmorWeight() * EquipmentType.getArmorCost(armorType[0]);

        // heat sinks
        int sinkCost = 2000 + (4000 * getHeatType());// == HEAT_DOUBLE ? 6000:
        // 2000;
        cost += sinkCost * getHeatSinks();

        // weapons
        cost += getWeaponsAndEquipmentCost(ignoreAmmo);

        double weightMultiplier = 1 + (weight / 50f);

        return Math.round(cost * weightMultiplier);

    }

    @Override
    public int getMaxEngineHits() {
        return 6;
    }

    @Override
    public double getBVTypeModifier() {
        return 1.0;
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

        if (mountedAmmo.isAmmoUsable() && !wtype.hasFlag(WeaponType.F_ONESHOT) && (atype.getAmmoType() == wtype.getAmmoType()) && (atype.getRackSize() == wtype.getRackSize())) {
            mounted.setLinked(mountedAmmo);
            success = true;
        }
        return success;
    }

    /*
     * (non-Javadoc)
     * @see megamek.common.Entity#getTotalCommGearTons()
     */
    @Override
    public int getTotalCommGearTons() {
        return 3 + getExtraCommGearTons();
    }

    /**
     * All military small craft automatically have ECM if in space
     */
    @Override
    public boolean hasActiveECM() {
        if (!game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ECM) || !game.getBoard().inSpace()) {
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
        int range = -1;
        // if the unit has an ECM unit, then the range might be extended by one
        if (!isShutDown()) {
            for (Mounted m : getMisc()) {
                EquipmentType type = m.getType();
                if ((type instanceof MiscType) && type.hasFlag(MiscType.F_ECM) && !m.isInoperable()) {
                    if (type.hasFlag(MiscType.F_SINGLE_HEX_ECM)) {
                        range += 1;
                    } else {
                        range += 2;
                    }
                    break;
                }
            }
        }
        // the range might be affected by sensor/FCS damage
        range = range - getFCSHits() - getSensorHits();
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

    /**
     * Return the height of this small craft above the terrain.
     */
    @Override
    public int height() {
        if (isAirborne()) {
            return 0;
        }
        return 1;
    }

    @Override
    public int getBattleForceSize() {
        //The tables are on page 356 of StartOps
        if (getWeight() < 2500) {
            return 1;
        }
        if (getWeight() < 10000) {
            return 2;
        }
        return 3;
    }
    
    @Override
    public int getNumBattleForceWeaponsLocations() {
        return 4;
    }
    
    @Override
    public String getBattleForceLocationName(int index) {
        return getLocationAbbrs()[index];
    }
    
    @Override
    public double getBattleForceLocationMultiplier(int index, int location, boolean rearMounted) {
        switch (index) {
        case LOC_NOSE:
            if (location == LOC_NOSE) {
                return 1.0;
            }
            if (isSpheroid() && (location == LOC_LWING || location == LOC_RWING)
                    && !rearMounted) {
                return 0.5;
            }
            break;
        case LOC_LWING:
        case LOC_RWING:
            if (index == location) {
                if (isSpheroid()) {
                    return 0.5;
                }
                if (!rearMounted) {
                    return 1.0;
                }
            }
            break;
        case LOC_AFT:
            if (location == LOC_AFT) {
                return 1.0;
            }
            if (rearMounted && (location == LOC_LWING || location == LOC_RWING)) {
                return isSpheroid()? 0.5 : 1.0;
            }
            break;
        }
        return 0;
    }

    @Override
    public void addBattleForceSpecialAbilities(Map<BattleForceSPA,Integer> specialAbilities) {
        super.addBattleForceSpecialAbilities(specialAbilities);
        specialAbilities.put(BattleForceSPA.LG, null);
    }

    public long getEntityType(){
        return Entity.ETYPE_AERO | Entity.ETYPE_SMALL_CRAFT;
    }
    
    @Override
    public boolean isFighter() {
        return false;
    }

    /**
     * Do not recalculate walkMP when adding engine.
     */
    @Override
    protected int calculateWalk() {
    	return walkMP;
    }
}
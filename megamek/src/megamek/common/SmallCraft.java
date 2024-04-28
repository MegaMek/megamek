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
package megamek.common;

import megamek.client.ui.swing.calculationReport.CalculationReport;
import megamek.common.cost.SmallCraftCostCalculator;
import megamek.common.equipment.ArmorType;
import megamek.common.options.OptionsConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Jay Lawson
 * @since Jun 17, 2007
 */
public class SmallCraft extends Aero {

    private static final long serialVersionUID = 6708788176436555036L;

    public static final int LOC_HULL = 4;

    private static String[] LOCATION_ABBRS = {"NOS", "LS", "RS", "AFT", "HULL"};
    private static String[] LOCATION_NAMES = {"Nose", "Left Side", "Right Side", "Aft", "Hull"};

    // crew and passengers
    private int nOfficers = 0;
    private int nGunners = 0;
    private int nBattleArmor = 0;
    private int nOtherPassenger = 0;

    // Maps transported crew, passengers, marines to a host ship so we can match them up again post-game
    private Map<String, Integer> nOtherCrew = new HashMap<>();
    private Map<String, Integer> passengers = new HashMap<>();

    // escape pods and lifeboats
    private int escapePods = 0;
    private int lifeBoats = 0;
    private int escapePodsLaunched = 0;
    private int lifeBoatsLaunched = 0;

    private static final TechAdvancement TA_SM_CRAFT = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_NONE, 2350, 2400).setISApproximate(false, true, false)
            .setProductionFactions(F_TH).setTechRating(RATING_D)
            .setAvailability(RATING_D, RATING_E, RATING_D, RATING_D)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
    private static final TechAdvancement TA_SM_CRAFT_PRIMITIVE = new TechAdvancement(TECH_BASE_IS)
            //Per MUL team and per availability codes should exist to around 2781
            .setISAdvancement(DATE_ES, 2200, DATE_NONE, 2781, DATE_NONE)
            .setISApproximate(false, true, false, true, false)
            .setProductionFactions(F_TA).setTechRating(RATING_D)
            .setAvailability(RATING_D, RATING_X, RATING_F, RATING_F)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);

    @Override
    public int getUnitType() {
        return UnitType.SMALL_CRAFT;
    }

    @Override
    public TechAdvancement getConstructionTechAdvancement() {
        if (isPrimitive()) {
            return TA_SM_CRAFT_PRIMITIVE;
        } else {
            return TA_SM_CRAFT;
        }
    }

    /**
     * @return Returns the autoEject setting (always off for large craft)
     */
    @Override
    public boolean isAutoEject() {
        return false;
    }

    @Override
    public boolean isPrimitive() {
        return getArmorType(LOC_NOSE) == EquipmentType.T_ARMOR_PRIMITIVE_AERO;
    }

    @Override
    public boolean isSmallCraft() {
        return true;
    }

    @Override
    public void setNCrew(int crew) {
        nCrew = crew;
    }

    public void setNOfficers(int officer) {
        nOfficers = officer;
    }

    public void setNGunners(int gunners) {
        nGunners = gunners;
    }

    @Override
    public void setNPassenger(int pass) {
        nPassenger = pass;
    }

    public void setNBattleArmor(int ba) {
        nBattleArmor = ba;
    }

    @Override
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

    /**
     * Returns a mapping of how many crewmembers from other units this unit is carrying
     * and what ship they're from by external ID
     */
    public Map<String, Integer> getNOtherCrew() {
        return nOtherCrew;
    }

    /**
     * Convenience method to return all crew from other craft aboard from the above Map
     * @return
     */
    public int getTotalOtherCrew() {
        int toReturn = 0;
        for (String name : getNOtherCrew().keySet()) {
            toReturn += getNOtherCrew().get(name);
        }
        return toReturn;
    }

    /**
     * Adds a number of crewmembers from another ship keyed by that ship's external ID
     * @param id The external ID of the ship these crew came from
     * @param n The number to add
     */
    public void addNOtherCrew(String id, int n) {
        if (nOtherCrew.containsKey(id)) {
            nOtherCrew.replace(id, nOtherCrew.get(id) + n);
        } else {
            nOtherCrew.put(id, n);
        }
    }

    /**
     * Returns a mapping of how many passengers from other units this unit is carrying
     * and what ship they're from by external ID
     */
    public Map<String, Integer> getPassengers() {
        return passengers;
    }

    /**
     * Convenience method to return all passengers aboard from the above Map
     * @return
     */
    public int getTotalPassengers() {
        int toReturn = 0;
        for (String name : getPassengers().keySet()) {
            toReturn += getPassengers().get(name);
        }
        return toReturn;
    }

    /**
     * Adds a number of passengers from another ship keyed by that ship's external ID
     * @param id The external ID of the ship these passengers came from
     * @param n The number to add
     */
    public void addPassengers(String id, int n) {
        if (passengers.containsKey(id)) {
            passengers.replace(id, passengers.get(id) + n);
        } else {
            passengers.put(id, n);
        }
    }

    public void setEscapePods(int n) {
        escapePods = n;
    }

    @Override
    public int getEscapePods() {
        return escapePods;
    }

    /**
     * Returns the total number of escape pods launched so far
     */
    @Override
    public int getLaunchedEscapePods() {
        return escapePodsLaunched;
    }

    /**
     * Updates the total number of escape pods launched so far
     * @param n The number to change
     */
    @Override
    public void setLaunchedEscapePods(int n) {
        escapePodsLaunched = n;
    }

    public void setLifeBoats(int n) {
        lifeBoats = n;
    }

    @Override
    public int getLifeBoats() {
        return lifeBoats;
    }

    /**
     * Returns the total number of lifeboats launched so far
     */
    @Override
    public int getLaunchedLifeBoats() {
        return lifeBoatsLaunched;
    }

    /**
     * Updates the total number of lifeboats launched so far
     * @param n The number to change
     */
    @Override
    public void setLaunchedLifeBoats(int n) {
        lifeBoatsLaunched = n;
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
        return 5;
    }

    @Override
    public int getBodyLocation() {
        return LOC_HULL;
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
                            && (((Dropship) this).getCollarType() == Dropship.COLLAR_NO_BOOM)) {
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
                    if (mounted.isInWaypointLaunchMode()) {
                        arc = Compute.ARC_NOSE_WPL;
                        break;
                    }
                    arc = Compute.ARC_NOSE;
                    break;
                case LOC_RWING:
                    if (mounted.isRearMounted()) {
                        if (mounted.isInWaypointLaunchMode()) {
                            arc = Compute.ARC_RWINGA_WPL;
                            break;
                        }
                        arc = Compute.ARC_RWINGA;
                    } else {
                        if (mounted.isInWaypointLaunchMode()) {
                            arc = Compute.ARC_RWING_WPL;
                            break;
                        }
                        arc = Compute.ARC_RWING;
                    }
                    break;
                case LOC_LWING:
                    if (mounted.isRearMounted()) {
                        if (mounted.isInWaypointLaunchMode()) {
                            arc = Compute.ARC_LWINGA_WPL;
                            break;
                        }
                        arc = Compute.ARC_LWINGA;
                    } else {
                        if (mounted.isInWaypointLaunchMode()) {
                            arc = Compute.ARC_LWING_WPL;
                            break;
                        }
                        arc = Compute.ARC_LWING;
                    }
                    break;
                case LOC_AFT:
                    if (mounted.isInWaypointLaunchMode()) {
                        arc = Compute.ARC_AFT_WPL;
                        break;
                    }
                    arc = Compute.ARC_AFT;
                    break;
                default:
                    arc = Compute.ARC_360;
            }
        } else {
            if ((game != null) && game.getBoard().inSpace()) {
                switch (mounted.getLocation()) {
                    case LOC_NOSE:
                        if (mounted.isInWaypointLaunchMode()) {
                            arc = Compute.ARC_NOSE_WPL;
                            break;
                        }
                        arc = Compute.ARC_NOSE;
                        break;
                    case LOC_RWING:
                        if (mounted.isRearMounted()) {
                            if (mounted.isInWaypointLaunchMode()) {
                                arc = Compute.ARC_RIGHTSIDEA_SPHERE_WPL;
                                break;
                            }
                            arc = Compute.ARC_RIGHTSIDEA_SPHERE;
                        } else {
                            if (mounted.isInWaypointLaunchMode()) {
                                arc = Compute.ARC_RIGHTSIDE_SPHERE_WPL;
                                break;
                            }
                            arc = Compute.ARC_RIGHTSIDE_SPHERE;
                        }
                        break;
                    case LOC_LWING:
                        if (mounted.isRearMounted()) {
                            if (mounted.isInWaypointLaunchMode()) {
                                arc = Compute.ARC_LEFTSIDEA_SPHERE_WPL;
                                break;
                            }
                            arc = Compute.ARC_LEFTSIDEA_SPHERE;
                        } else {
                            if (mounted.isInWaypointLaunchMode()) {
                                arc = Compute.ARC_LEFTSIDE_SPHERE_WPL;
                                break;
                            }
                            arc = Compute.ARC_LEFTSIDE_SPHERE;
                        }
                        break;
                    case LOC_AFT:
                        if (mounted.isInWaypointLaunchMode()) {
                            arc = Compute.ARC_AFT_WPL;
                            break;
                        }
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
        // first I need to subtract SI bonus from total armor. We need to retain the fractional part
        //  for primitive craft because the primitive multiplier is applied to both before rounding.
        double armorPoints = getTotalOArmor();
        int freeSI = getSI() * (locations() - 1); // no armor in hull location
        if (isPrimitive()) {
            armorPoints -= freeSI * 0.66;
        } else {
            armorPoints -= freeSI;
        }
        ArmorType armor = ArmorType.forEntity(this);
        double armorPerTon = armor.getPointsPerTon(this);

        return RoundWeight.nextHalfTon(armorPoints / armorPerTon);
    }

    @Override
    public double getCost(CalculationReport calcReport, boolean ignoreAmmo) {
        return SmallCraftCostCalculator.calculateCost(this, calcReport, ignoreAmmo);
    }

    @Override
    public double getPriceMultiplier() {
        return 1 + (weight / 50f);
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
    public long getEntityType() {
        return Entity.ETYPE_AERO | Entity.ETYPE_SMALL_CRAFT;
    }

    @Override
    public boolean isFighter() {
        return false;
    }

    /**
     * Fighters may carry external ordnance;
     * Other Aerospace units with cargo bays and the Internal Bomb Bay quirk may carry bombs internally.
     * @return boolean
     */
    @Override
    public boolean isBomber() {
        return (hasQuirk(OptionsConstants.QUIRK_POS_INTERNAL_BOMB));
    }


    @Override
    public boolean isAerospaceFighter() {
        return false;
    }

    /**
     * Do not recalculate walkMP when adding engine.
     */
    @Override
    protected int calculateWalk() {
        return walkMP;
    }

    @Override
    public boolean isLargeAerospace() {
        return true;
    }

    @Override
    public int getLandingLength() {
        return 8;
    }

    @Override
    public void autoSetMaxBombPoints() {
        // Only internal cargo bays can be considered for this type of unit.
        maxIntBombPoints = getTransportBays().stream().mapToInt(
                    tb -> (tb instanceof CargoBay) ? (int) Math.floor(tb.getUnused()) : 0
                ).sum();
    }

    @Override
    public int getGenericBattleValue() {
        return (int) Math.round(Math.exp(-0.068 + 1.421*Math.log(getWeight())));
    }
}
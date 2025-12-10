/*
 * Copyright (C) 2007 Jay Lawson
 * Copyright (C) 2008-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.units;

import java.io.Serial;
import java.util.HashMap;
import java.util.Map;

import megamek.client.ui.clientGUI.calculationReport.CalculationReport;
import megamek.common.HitData;
import megamek.common.SimpleTechLevel;
import megamek.common.TechAdvancement;
import megamek.common.ToHitData;
import megamek.common.bays.CargoBay;
import megamek.common.compute.Compute;
import megamek.common.cost.SmallCraftCostCalculator;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.ArmorType;
import megamek.common.equipment.Engine;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.options.OptionsConstants;
import megamek.common.util.RoundWeight;

/**
 * @author Jay Lawson
 * @since Jun 17, 2007
 */
public class SmallCraft extends Aero {

    @Serial
    private static final long serialVersionUID = 6708788176436555036L;

    public static final int LOC_HULL = 4;

    private static final String[] LOCATION_ABBREVIATIONS = { "NOS", "LS", "RS", "AFT", "HULL" };
    private static final String[] LOCATION_NAMES = { "Nose", "Left Side", "Right Side", "Aft", "Hull" };

    // crew and passengers
    private int nOfficers = 0;
    private int nGunners = 0;
    private int nBattleArmor = 0;
    private int nOtherPassenger = 0;

    // Maps transported crew, passengers, marines to a host ship so we can match
    // them up again post-game
    private final Map<String, Integer> nOtherCrew = new HashMap<>();
    private final Map<String, Integer> passengers = new HashMap<>();

    // escape pods and lifeboats
    private int escapePods = 0;
    private int lifeBoats = 0;
    private int escapePodsLaunched = 0;
    private int lifeBoatsLaunched = 0;

    private static final TechAdvancement TA_SM_CRAFT = new TechAdvancement(TechBase.ALL).setAdvancement(DATE_NONE,
                2350,
                2400)
          .setISApproximate(false, true, false)
          .setProductionFactions(Faction.TH)
          .setTechRating(TechRating.D)
          .setAvailability(AvailabilityValue.D, AvailabilityValue.E, AvailabilityValue.D, AvailabilityValue.D)
          .setStaticTechLevel(SimpleTechLevel.STANDARD);
    private static final TechAdvancement TA_SM_CRAFT_PRIMITIVE = new TechAdvancement(TechBase.IS)
          // Per MUL team and per availability codes should exist to around 2781
          .setISAdvancement(DATE_ES,
                2200,
                DATE_NONE,
                2781,
                DATE_NONE)
          .setISApproximate(false,
                true,
                false,
                true,
                false)
          .setProductionFactions(Faction.TA)
          .setTechRating(TechRating.D)
          .setAvailability(AvailabilityValue.D,
                AvailabilityValue.X,
                AvailabilityValue.F,
                AvailabilityValue.F)
          .setStaticTechLevel(SimpleTechLevel.STANDARD);

    public SmallCraft() {
        // A placeholder engine for SC and DS
        setEngine(new Engine(400, 0, 0));
    }

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
     * Returns a mapping of how many crewmembers from other units this unit is carrying and what ship they're from by
     * external ID
     */
    public Map<String, Integer> getNOtherCrew() {
        return nOtherCrew;
    }

    /**
     * Convenience method to return all crew from other craft aboard from the above Map
     *
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
     *
     * @param id The external ID of the ship these crew came from
     * @param n  The number to add
     */
    public void addNOtherCrew(String id, int n) {
        if (nOtherCrew.containsKey(id)) {
            nOtherCrew.replace(id, nOtherCrew.get(id) + n);
        } else {
            nOtherCrew.put(id, n);
        }
    }

    /**
     * Returns a mapping of how many passengers from other units this unit is carrying and what ship they're from by
     * external ID
     */
    public Map<String, Integer> getPassengers() {
        return passengers;
    }

    /**
     * Convenience method to return all passengers aboard from the above Map
     *
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
     *
     * @param id The external ID of the ship these passengers came from
     * @param n  The number to add
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
     *
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
     *
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
    public String[] getLocationAbbreviations() {
        return LOCATION_ABBREVIATIONS;
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
         * Unlike other units, ASFs determine potential crits based on the to-hit roll,
         * so I need to set this potential value as well as return the to hit data
         */

        int roll = Compute.d6(2);

        // special rules for spheroids in atmosphere
        // http://www.classicbattletech.com/forums/index.php/topic,54077.0.html
        if (isSpheroid() && table != ToHitData.HIT_SPHEROID_CRASH && !isSpaceborne()) {
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
            int wingLocation = LOC_RIGHT_WING;
            int wingRoll = Compute.d6(1);
            if (wingRoll > 3) {
                wingLocation = LOC_LEFT_WING;
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
                case 5, 9:
                    setPotCrit(CRIT_RIGHT_THRUSTER);
                    if (wingRoll > 3) {
                        setPotCrit(CRIT_LEFT_THRUSTER);
                    }
                    return new HitData(wingLocation, false, HitData.EFFECT_NONE);
                case 6:
                    setPotCrit(CRIT_CARGO);
                    return new HitData(wingLocation, false, HitData.EFFECT_NONE);
                case 7:
                    setPotCrit(CRIT_WEAPON);
                    return new HitData(wingLocation, false, HitData.EFFECT_NONE);
                case 8:
                    setPotCrit(CRIT_DOOR);
                    return new HitData(wingLocation, false, HitData.EFFECT_NONE);
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
                    return new HitData(LOC_RIGHT_WING, false, HitData.EFFECT_NONE);
                case 5:
                    setPotCrit(CRIT_RIGHT_THRUSTER);
                    return new HitData(LOC_RIGHT_WING, false, HitData.EFFECT_NONE);
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
                    return new HitData(LOC_LEFT_WING, false, HitData.EFFECT_NONE);
                case 10:
                    setPotCrit(CRIT_WEAPON);
                    return new HitData(LOC_LEFT_WING, false, HitData.EFFECT_NONE);
                case 11:
                    setPotCrit(CRIT_SENSOR);
                    return new HitData(LOC_NOSE, false, HitData.EFFECT_NONE);
                case 12:
                    setPotCrit(CRIT_KF_BOOM);
                    // Primitive dropships without kf-boom take avionics hit instead (IO, p. 119).
                    if ((this instanceof Dropship) && (((Dropship) this).getCollarType() == Dropship.COLLAR_NO_BOOM)) {
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
                case 5, 9:
                    setPotCrit(CRIT_LEFT_THRUSTER);
                    return new HitData(LOC_LEFT_WING, false, HitData.EFFECT_NONE);
                case 6:
                    setPotCrit(CRIT_CARGO);
                    return new HitData(LOC_LEFT_WING, false, HitData.EFFECT_NONE);
                case 7:
                    setPotCrit(CRIT_WEAPON);
                    return new HitData(LOC_LEFT_WING, false, HitData.EFFECT_NONE);
                case 8:
                    setPotCrit(CRIT_DOOR);
                    return new HitData(LOC_LEFT_WING, false, HitData.EFFECT_NONE);
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
                case 5, 9:
                    setPotCrit(CRIT_RIGHT_THRUSTER);
                    return new HitData(LOC_RIGHT_WING, false, HitData.EFFECT_NONE);
                case 6:
                    setPotCrit(CRIT_CARGO);
                    return new HitData(LOC_RIGHT_WING, false, HitData.EFFECT_NONE);
                case 7:
                    setPotCrit(CRIT_WEAPON);
                    return new HitData(LOC_RIGHT_WING, false, HitData.EFFECT_NONE);
                case 8:
                    setPotCrit(CRIT_DOOR);
                    return new HitData(LOC_RIGHT_WING, false, HitData.EFFECT_NONE);
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
                    return new HitData(LOC_RIGHT_WING, false, HitData.EFFECT_NONE);
                case 5:
                    setPotCrit(CRIT_DOOR);
                    return new HitData(LOC_RIGHT_WING, false, HitData.EFFECT_NONE);
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
                    return new HitData(LOC_LEFT_WING, false, HitData.EFFECT_NONE);
                case 10:
                    setPotCrit(CRIT_WEAPON);
                    return new HitData(LOC_LEFT_WING, false, HitData.EFFECT_NONE);
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
    public int getWeaponArc(int weaponNumber) {
        final Mounted<?> mounted = getEquipment(weaponNumber);

        int arc;
        if (!isSpheroid()) {
            switch (mounted.getLocation()) {
                case LOC_NOSE:
                    if (mounted.isInWaypointLaunchMode()) {
                        arc = Compute.ARC_NOSE_WPL;
                        break;
                    }
                    arc = Compute.ARC_NOSE;
                    break;
                case LOC_RIGHT_WING:
                    if (mounted.isRearMounted()) {
                        if (mounted.isInWaypointLaunchMode()) {
                            arc = Compute.ARC_RIGHT_WING_AFT_WPL;
                            break;
                        }
                        arc = Compute.ARC_RIGHT_WING_AFT;
                    } else {
                        if (mounted.isInWaypointLaunchMode()) {
                            arc = Compute.ARC_RIGHT_WING_WPL;
                            break;
                        }
                        arc = Compute.ARC_RIGHT_WING;
                    }
                    break;
                case LOC_LEFT_WING:
                    if (mounted.isRearMounted()) {
                        if (mounted.isInWaypointLaunchMode()) {
                            arc = Compute.ARC_LEFT_WING_AFT_WPL;
                            break;
                        }
                        arc = Compute.ARC_LEFT_WING_AFT;
                    } else {
                        if (mounted.isInWaypointLaunchMode()) {
                            arc = Compute.ARC_LEFT_WING_WPL;
                            break;
                        }
                        arc = Compute.ARC_LEFT_WING;
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
            if (isSpaceborne()) {
                switch (mounted.getLocation()) {
                    case LOC_NOSE:
                        if (mounted.isInWaypointLaunchMode()) {
                            arc = Compute.ARC_NOSE_WPL;
                            break;
                        }
                        arc = Compute.ARC_NOSE;
                        break;
                    case LOC_RIGHT_WING:
                        if (mounted.isRearMounted()) {
                            if (mounted.isInWaypointLaunchMode()) {
                                arc = Compute.ARC_RIGHT_SIDE_AFT_SPHERE_WPL;
                                break;
                            }
                            arc = Compute.ARC_RIGHT_SIDE_AFT_SPHERE;
                        } else {
                            if (mounted.isInWaypointLaunchMode()) {
                                arc = Compute.ARC_RIGHT_SIDE_SPHERE_WPL;
                                break;
                            }
                            arc = Compute.ARC_RIGHT_SIDE_SPHERE;
                        }
                        break;
                    case LOC_LEFT_WING:
                        if (mounted.isRearMounted()) {
                            if (mounted.isInWaypointLaunchMode()) {
                                arc = Compute.ARC_LEFT_SIDE_AFT_SPHERE_WPL;
                                break;
                            }
                            arc = Compute.ARC_LEFT_SIDE_AFT_SPHERE;
                        } else {
                            if (mounted.isInWaypointLaunchMode()) {
                                arc = Compute.ARC_LEFT_SIDE_SPHERE_WPL;
                                break;
                            }
                            arc = Compute.ARC_LEFT_SIDE_SPHERE;
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
                arc = switch (mounted.getLocation()) {
                    case LOC_RIGHT_WING -> Compute.ARC_RIGHT_SPHERE_GROUND;
                    case LOC_LEFT_WING -> Compute.ARC_LEFT_SPHERE_GROUND;
                    default -> Compute.ARC_360;
                };
            }

        }

        return rollArcs(arc);

    }

    public int getArcsWithGuns() {
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
        for (Mounted<?> weapon : getWeaponList()) {
            if ((weapon.getLocation() == loc) && (weapon.isRearMounted() == rearMount)) {
                hasWeapons = true;
                break;
            }
        }
        return hasWeapons;
    }

    @Override
    public double getArmorWeight() {
        // first I need to subtract SI bonus from total armor. We need to retain the
        // fractional part
        // for primitive craft because the primitive multiplier is applied to both
        // before rounding.
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
    public boolean loadWeapon(WeaponMounted mounted, AmmoMounted mountedAmmo) {
        boolean success = false;
        WeaponType weaponType = mounted.getType();
        AmmoType ammoType = mountedAmmo.getType();

        if (mounted.getLocation() != mountedAmmo.getLocation()) {
            return false;
        }

        if (mountedAmmo.isAmmoUsable() &&
              !weaponType.hasFlag(WeaponType.F_ONE_SHOT) &&
              (ammoType.getAmmoType() == weaponType.getAmmoType()) &&
              (ammoType.getRackSize() == weaponType.getRackSize())) {
            mounted.setLinked(mountedAmmo);
            success = true;
        }
        return success;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.units.Entity#getTotalCommGearTons()
     */
    @Override
    public int getTotalCommGearTons() {
        return 3 + getExtraCommGearTons();
    }

    @Override
    public boolean hasActiveECM() {
        // Military small craft automatically have ECM if in space
        if (isActiveOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_ECM) && isSpaceborne()) {
            return getECMRange() >= 0;
        } else {
            return super.hasActiveECM();
        }
    }

    /**
     * What's the range of the ECM equipment?
     *
     * @return the <code>int</code> range of this unit's ECM. This value will be
     *       <code>Entity.NONE</code> if no ECM is active.
     */
    @Override
    public int getECMRange() {
        if (!isActiveOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_ECM) || !isSpaceborne()) {
            return super.getECMRange();
        }
        if (!isMilitary()) {
            return Entity.NONE;
        }
        int range = -1;
        // if the unit has an ECM unit, then the range might be extended by one
        if (!isShutDown()) {
            for (Mounted<?> m : getMisc()) {
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

    @Override
    public boolean isCrewProtected() {
        return isMilitary() && (getOriginalWalkMP() > 4);
    }

    @Override
    public int height() {
        return isAirborne() ? 0 : 1;
    }

    @Override
    public long getEntityType() {
        return Entity.ETYPE_AERO | Entity.ETYPE_SMALL_CRAFT;
    }

    /**
     * Fighters may carry external ordnance; Other Aerospace units with cargo bays and the Internal Bomb Bay quirk may
     * carry bombs internally.
     *
     * @return boolean
     */
    @Override
    public boolean isBomber() {
        return (hasQuirk(OptionsConstants.QUIRK_POS_INTERNAL_BOMB));
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
        maxIntBombPoints = getTransportBays().stream()
              .mapToInt(tb -> (tb instanceof CargoBay) ? (int) Math.floor(tb.getUnused()) : 0)
              .sum();
    }

    @Override
    public int getGenericBattleValue() {
        return (int) Math.round(Math.exp(-0.068 + 1.421 * Math.log(getWeight())));
    }

    @Override
    public boolean hasPatchworkArmor() {
        return false;
    }

    @Override
    public int getRecoveryTime() {
        return 120;
    }
}

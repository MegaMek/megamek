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
public class Warship extends Jumpship implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 4650692419224312511L;
    //     locations
    public static final int        LOC_NOSE               = 0;
    public static final int        LOC_FLS                = 1;
    public static final int        LOC_FRS                = 2;
    public static final int        LOC_AFT                = 3;
    public static final int        LOC_ALS                = 4;
    public static final int        LOC_ARS                = 5;
    public static final int           LOC_LBS                  = 6;
    public static final int           LOC_RBS                  = 7;

    protected static String[] LOCATION_ABBRS = { "NOS", "FLS", "FRS", "AFT", "ALS", "ARS", "LBS", "RBS" };
    protected static String[] LOCATION_NAMES = { "Nose", "Left Front Side", "Right Front Side", "Aft", "Aft Left Side", "Aft Right Side", "Left Broadsides", "Right Broadsides" };

    private int damThresh[] = {0,0,0,0,0,0,0,0};
    private int standard_damage[] = {0,0,0,0,0,0,0,0};

    private int kf_integrity = 0;
    private int sail_integrity = 0;

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
        return 8;
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
    public void setKFIntegrity(int kf) {
        kf_integrity = kf;
    }

    @Override
    public int getKFIntegrity() {
        return kf_integrity;
    }

    @Override
    public void setSailIntegrity(int sail) {
        sail_integrity = sail;
    }

    @Override
    public int getSailIntegrity() {
        return sail_integrity;
    }

    @Override
    public void initializeSailIntegrity() {
        int integrity = 1 + (int)Math.round((30.0 + weight / 20000.0)/ 20.0);
        setSailIntegrity(integrity);
    }

    @Override
    public void initializeKFIntegrity() {
        int integrity = (int)Math.round(2 + 0.4525 * weight/25000.0);
        setKFIntegrity(integrity);
    }

    @Override
    public boolean canJump() {
        return kf_integrity > 0;
    }

    //do damage threshhold using standard armor values
    //or I will have a big rounding issue
    @Override
    public void initializeThresh(int loc)
    {
        int nThresh = (int)Math.ceil(getArmor(loc)/ 10.0 );
        setThresh(nThresh,loc);
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

    //broadside weapon arcs
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
        case LOC_LBS:
            arc = Compute.ARC_LEFT_BROADSIDE;
            break;
        case LOC_RBS:
            arc = Compute.ARC_RIGHT_BROADSIDE;
            break;
        default:
            arc = Compute.ARC_360;
        }
        return rollArcs(arc);
    }

    @Override
    public double getCost() {

        double cost = 0.0f;


        //Double.MAX
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
        cost += 25000;

        //docking hard point
        cost += 100000 * getDocks();

        double engineWeight = getOriginalWalkMP() * weight * 0.06;
        cost += engineWeight * 1000;
        //drive unit
        cost += 500 * getOriginalWalkMP() * weight / 100.0;
        //control equipment
        cost += 1000;

        //HPG
        if(hasHPG()) {
            cost += 1000000000;
        }

        //fuel tanks
        cost += 200 * getFuel() / getFuelPerTon();

        //armor
        cost += getArmorWeight(locations()-2)*EquipmentType.getArmorCost(armorType);

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
        driveCost += 50000 * Math.round( 30 + (weight / 20000) );
        //charging system
        driveCost += 500000 + (200000 * getDocks());
        //is the core compact - yes
        driveCost *= 5;
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
        //Bay doors are not counted in the AT2r example
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

        double weightMultiplier = 2.00f;

        //TODO: The number is too big. No idea how to fix this
        //I had to hack it: change all costs to 1000s of C-bills and
        //adjust mech selector to account for it
        return Math.round(cost * weightMultiplier);
    }

    /**
     * All warships automatically have ECM if in space
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
        int range = 2;
        //the range might be affected by sensor/FCS damage
        range = range - getSensorHits() - getCICHits();
        return range;
    }

    /**
     * find the adjacent firing arc on this vessel clockwise
     */
    @Override
    public int getAdjacentArcCW(int arc) {
        switch(arc) {
        case Compute.ARC_NOSE:
            return Compute.ARC_RIGHTSIDE_SPHERE;
        case Compute.ARC_LEFTSIDE_SPHERE:
            return Compute.ARC_NOSE;
        case Compute.ARC_RIGHTSIDE_SPHERE:
            return Compute.ARC_RIGHT_BROADSIDE;
        case Compute.ARC_LEFTSIDEA_SPHERE:
            return Compute.ARC_LEFT_BROADSIDE;
        case Compute.ARC_RIGHTSIDEA_SPHERE:
            return Compute.ARC_AFT;
        case Compute.ARC_LEFT_BROADSIDE:
            return Compute.ARC_LEFTSIDE_SPHERE;
        case Compute.ARC_RIGHT_BROADSIDE:
            return Compute.ARC_RIGHTSIDEA_SPHERE;
        case Compute.ARC_AFT:
            return Compute.ARC_LEFTSIDEA_SPHERE;
        default:
            return Integer.MIN_VALUE;
        }
    }

    /**
     * find the adjacent firing arc on this vessel counter-clockwise
     */
    @Override
    public int getAdjacentArcCCW(int arc) {
        switch(arc) {
        case Compute.ARC_NOSE:
            return Compute.ARC_LEFTSIDE_SPHERE;
        case Compute.ARC_RIGHTSIDE_SPHERE:
            return Compute.ARC_NOSE;
        case Compute.ARC_LEFTSIDE_SPHERE:
            return Compute.ARC_LEFT_BROADSIDE;
        case Compute.ARC_LEFTSIDEA_SPHERE:
            return Compute.ARC_AFT;
        case Compute.ARC_RIGHTSIDEA_SPHERE:
            return Compute.ARC_RIGHT_BROADSIDE;
        case Compute.ARC_LEFT_BROADSIDE:
            return Compute.ARC_LEFTSIDEA_SPHERE;
        case Compute.ARC_RIGHT_BROADSIDE:
            return Compute.ARC_RIGHTSIDE_SPHERE;
        case Compute.ARC_AFT:
            return Compute.ARC_RIGHTSIDEA_SPHERE;
        default:
            return Integer.MIN_VALUE;
        }
    }

    @Override
    public double getBVTypeModifier() {
        return 0.8;
    }
}

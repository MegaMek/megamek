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

import java.util.Map;

import megamek.common.options.OptionsConstants;

/**
 * @author Jay Lawson
 */
public class Warship extends Jumpship {

    /**
     *
     */
    private static final long serialVersionUID = 4650692419224312511L;

    // additional Warship locations
    public static final int LOC_LBS = 6;
    public static final int LOC_RBS = 7;

    private static String[] LOCATION_ABBRS = { "NOS", "FLS", "FRS", "AFT", "ALS", "ARS", "LBS", "RBS" };
    private static String[] LOCATION_NAMES = { "Nose", "Left Front Side", "Right Front Side", "Aft", "Aft Left Side",
            "Aft Right Side", "Left Broadsides", "Right Broadsides" };

    private int kf_integrity = 0;
    private int sail_integrity = 0;

    public Warship() {
        super();
        damThresh = new int[] { 0, 0, 0, 0, 0, 0, 0, 0 };
        setDriveCoreType(DRIVE_CORE_COMPACT);
    }
    
    //ASEW Missile Effects, per location
    //Values correspond to Locations, as seen above: NOS,FLS,FRS,AFT,ALS,ARS,LBS,RBS
    private int asewAffectedTurns[] = { 0, 0, 0, 0, 0, 0, 0, 0};
    
    /*
     * Sets the number of rounds a specified firing arc is affected by an ASEW missile
     * @param arc - integer representing the desired firing arc
     * @param turns - integer specifying the number of end phases that the effects last through
     * Technically, about 1.5 turns elapse per the rules for ASEW missiles in TO
     * Because Warships have 8 arcs instead of 6, this overrides the method in Jumpship
     */
    @Override
    public void setASEWAffected(int arc, int turns) {
        asewAffectedTurns[arc] = turns;
    }
    
    /*
     * Returns the number of rounds a specified firing arc is affected by an ASEW missile
     * @param arc - integer representing the desired firing arc
     */
    @Override
    public int getASEWAffected(int arc) {
        return asewAffectedTurns[arc];
    }
 
    
    private static final TechAdvancement TA_WARSHIP = new TechAdvancement(TECH_BASE_ALL)
            .setISAdvancement(2295, 2305, DATE_NONE, 2950, 3050)
            .setClanAdvancement(2295, 2305).setApproximate(true, false, false, false, false)
            .setPrototypeFactions(F_TA).setProductionFactions(F_TH)
            .setReintroductionFactions(F_FS, F_LC, F_DC).setTechRating(RATING_E)
            .setAvailability(RATING_D, RATING_E, RATING_E, RATING_F)
            .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    
    @Override
    public TechAdvancement getConstructionTechAdvancement() {
        return TA_WARSHIP;
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
        return 8;
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
        int integrity = 1 + (int) Math.ceil((30.0 + weight / 20000.0) / 20.0);
        setSailIntegrity(integrity);
    }

    @Override
    public void initializeKFIntegrity() {
        int integrity = (int) Math.ceil(2 + 0.4525 * weight / 25000.0);
        setKFIntegrity(integrity);
    }

    @Override
    public boolean canJump() {
        return kf_integrity > 0;
    }
    
    @Override
    public double getJumpDriveWeight() {
        double pct = 0.45; //TODO: compact
        return Math.ceil(getWeight() * pct); 
    }

    // broadside weapon arcs
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
    public double getArmorWeight(int loc) {
        // first I need to subtract SI bonus from total armor
        double armorPoints = getTotalOArmor();

        armorPoints -= Math.round((get0SI() * loc) / 10.0);
        // this roundabout method is actually necessary to avoid rounding
        // weirdness. Yeah, it's dumb.
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
        double[] costs = new double[23];
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

        // Maneuvering Drive
        // Drive Unit
        costs[costIdx++] += 500 * getOriginalWalkMP() * (weight / 100.0);
        // Engine
        costs[costIdx++] += 1000 * getOriginalWalkMP() * weight * 0.06;
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
        driveCost[driveIdx++] += 50000 * (30 + (weight / 20000));
        // Charging System
        driveCost[driveIdx++] += 500000 + (200000 * getDocks()); 
        
        for (int i = 0; i < driveIdx; i++) {
            driveCosts += driveCost[i];
        }

        driveCosts *= 5;
        if (hasLF()) {
            driveCosts *= 3;
        }

        costs[costIdx++] += driveCosts;

        // K-F Drive Support Systems
        costs[costIdx++] += 20000000 * (50 + weight / 10000);

        // Additional Ships Systems
        // Attitude Thrusters
        costs[costIdx++] += 25000;
        // Docking Collars
        costs[costIdx++] += 100000 * getDocks();
        // Fuel Tanks
        costs[costIdx++] += (200 * getFuel()) / getFuelPerTon() * 1.02;

        // Armor
        costs[costIdx++] += getArmorWeight(locations() - 2) * EquipmentType.getArmorCost(armorType[0]);

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

        double weightMultiplier = 2.00f;

        // Sum Costs
        for (int i = 0; i < costIdx; i++) {
            cost += costs[i];
        }

        costs[costIdx++] = -weightMultiplier; // Negative indicates multiplier
        cost = Math.round(cost * weightMultiplier);

        return cost;
    }

    /**
     * All warships automatically have ECM if in space
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
        int range = 2;
        // the range might be affected by sensor/FCS damage
        range = range - getSensorHits() - getCICHits();
        return range;
    }

    /**
     * find the adjacent firing arc on this vessel clockwise
     */
    @Override
    public int getAdjacentArcCW(int arc) {
        switch (arc) {
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
        switch (arc) {
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

    @Override
    public void setAlphaStrikeMovement(Map<String,Integer> moves) {
        moves.put("", getWalkMP());
    }

    @Override
    public int getBattleForceSize() {
        // The tables are on page 356 of StartOps
        if (getWeight() < 500000) {
            return 1;
        }
        if (getWeight() < 800000) {
            return 2;
        }
        if (getWeight() < 1200000) {
            return 3;
        }
        return 4;
    }

    @Override
    public int getBattleForceStructurePoints() {
        return (int) Math.ceil(this.getSI() * 0.66);
    }

    @Override
    public int getNumBattleForceWeaponsLocations() {
        return 8;
    }

    public int getNumAlphaStrikeWeaponsLocations() {
        return 4;
    }

    @Override
    public double getBattleForceLocationMultiplier(int index, int location, boolean rearMounted) {
        if (index == location) {
            return 1.0;
        }
        return 0;
    }
    
    public double getAlphaStrikeLocationMultiplier(int index, int location, boolean rearMounted) {
        switch (location) {
        case LOC_NOSE:
        case LOC_FLS:
        case LOC_FRS:
            if (index == 0) {
                return 1.0;
            }
            break;
        case LOC_LBS:
            if (index == 1) {
                return 1.0;
            }
            break;            
        case LOC_RBS:
            if (index == 2) {
                return 1.0;
            }
            break;            
        case LOC_AFT:
        case LOC_ALS:
        case LOC_ARS:
            if (index == 3) {
                return 1.0;
            }
            break;
        }
        return 0;
    }

    @Override
    public String getBattleForceLocationName(int index) {
        return getLocationAbbrs()[index];
    }
    
    @Override
    public String getAlphaStrikeLocationName(int index) {
        switch (index) {
        case 0:
            return getLocationAbbrs()[LOC_NOSE];
        case 1:
            return getLocationAbbrs()[LOC_LBS];
        case 2:
            return getLocationAbbrs()[LOC_RBS];
        case 3:
            return getLocationAbbrs()[LOC_AFT];
        }
        return "";
    }
    
    public long getEntityType(){
        return Entity.ETYPE_AERO | Entity.ETYPE_JUMPSHIP | Entity.ETYPE_WARSHIP;
    }
}

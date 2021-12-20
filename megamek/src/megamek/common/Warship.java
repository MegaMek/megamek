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

import java.text.NumberFormat;
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
    public static final int LOC_LBS = 7;
    public static final int LOC_RBS = 8;

    private static String[] LOCATION_ABBRS = { "NOS", "FLS", "FRS", "AFT", "ALS", "ARS", "HULL", "LBS", "RBS" };
    private static String[] LOCATION_NAMES = { "Nose", "Left Front Side", "Right Front Side",
            "Aft", "Aft Left Side", "Aft Right Side", "Hull", "Left Broadsides", "Right Broadsides" };

    public Warship() {
        super();
        damThresh = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        setDriveCoreType(DRIVE_CORE_COMPACT);
    }

    @Override
    public int getUnitType() {
        return UnitType.WARSHIP;
    }

    //ASEW Missile Effects, per location
    //Values correspond to Locations, as seen above: NOS,FLS,FRS,AFT,ALS,ARS,LBS,RBS
    private int[] asewAffectedTurns = { 0, 0, 0, 0, 0, 0, 0, 0};
    
    /*
     * Accessor for the asewAffectedTurns array, which may be different for inheriting classes.
     */
    @Override
    protected int[] getAsewAffectedTurns() {
        return asewAffectedTurns;
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
        // Primitives don't distinguish between jumpships and warships.
        if (isPrimitive()) {
            return super.getConstructionTechAdvancement();
        }
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
        return 9;
    }

    @Override
    public void initializeSailIntegrity() {
        int integrity = 1 + (int) Math.ceil((30.0 + weight / 20000.0) / 20.0);
        setOSailIntegrity(integrity);
        setSailIntegrity(integrity);
    }

    @Override
    public void initializeKFIntegrity() {
        int integrity = (int) Math.ceil(2 + getJumpDriveWeight() / 25000.0);
        setOKFIntegrity(integrity);
        setKFIntegrity(integrity);
        //Helium Tanks make up about 2/3 of the drive core.
        setKFHeliumTankIntegrity((int) (integrity * 0.67));
    }
    
    // broadside weapon arcs
    @Override
    public int getWeaponArc(int wn) {
        final Mounted mounted = getEquipment(wn);
        
        int arc = Compute.ARC_NOSE;
        switch (mounted.getLocation()) {
            case LOC_NOSE:
                if (mounted.isInWaypointLaunchMode()) {
                    arc = Compute.ARC_NOSE_WPL;
                    break;
                }
                arc = Compute.ARC_NOSE;
                break;
            case LOC_FRS:
                if (mounted.isInWaypointLaunchMode()) {
                    arc = Compute.ARC_RIGHTSIDE_SPHERE_WPL;
                    break;
                }
                arc = Compute.ARC_RIGHTSIDE_SPHERE;
                break;
            case LOC_FLS:
                if (mounted.isInWaypointLaunchMode()) {
                    arc = Compute.ARC_LEFTSIDE_SPHERE_WPL;
                    break;
                }
                arc = Compute.ARC_LEFTSIDE_SPHERE;
                break;
            case LOC_ARS:
                if (mounted.isInWaypointLaunchMode()) {
                    arc = Compute.ARC_RIGHTSIDEA_SPHERE_WPL;
                    break;
                }
                arc = Compute.ARC_RIGHTSIDEA_SPHERE;
                break;
            case LOC_ALS:
                if (mounted.isInWaypointLaunchMode()) {
                    arc = Compute.ARC_LEFTSIDEA_SPHERE_WPL;
                    break;
                }
                arc = Compute.ARC_LEFTSIDEA_SPHERE;
                break;
            case LOC_AFT:
                if (mounted.isInWaypointLaunchMode()) {
                    arc = Compute.ARC_AFT_WPL;
                    break;
                }
                arc = Compute.ARC_AFT;
                break;
            case LOC_LBS:
                if (mounted.isInWaypointLaunchMode()) {
                    arc = Compute.ARC_LEFT_BROADSIDE_WPL;
                    break;
                }
                arc = Compute.ARC_LEFT_BROADSIDE;
                break;
            case LOC_RBS:
                if (mounted.isInWaypointLaunchMode()) {
                    arc = Compute.ARC_RIGHT_BROADSIDE_WPL;
                    break;
                }
                arc = Compute.ARC_RIGHT_BROADSIDE;
                break;
            default:
                arc = Compute.ARC_360;
                break;
        }
        return rollArcs(arc);
    }

    @Override
    public double getArmorWeight() {
        return getArmorWeight(locations() - 3); // No armor for RBS/LBS/HULL
    }
    
    @Override
    public double getCost(boolean ignoreAmmo) {
        double[] costs = new double[24];
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
        driveCost[driveIdx++] += 60000000.0 + (75000000.0 * getDocks(true));
        // Initiator
        driveCost[driveIdx++] += 25000000.0 + (5000000.0 * getDocks(true));
        // Controller
        driveCost[driveIdx++] += 50000000.0;
        // Tankage
        driveCost[driveIdx++] += 50000.0 * getKFIntegrity();
        // Sail
        driveCost[driveIdx++] += 50000.0 * (30 + (weight / 20000.0));
        // Charging System
        driveCost[driveIdx++] += 500000.0 + (200000.0 * getDocks(true));
        
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
        costs[costIdx++] += getArmorWeight() * EquipmentType.getArmorCost(armorType[0]);

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
        long bayCost = 0;
        long quartersCost = 0;
        // Passenger and crew quarters and infantry bays are considered part of the structure
        // and don't add to the cost
        for (Bay next : getTransportBays()) {
            baydoors += next.getDoors();
            if (!next.isQuarters() && !(next instanceof InfantryBay) && !(next instanceof BattleArmorBay)) {
                bayCost += next.getCost();
            }
        }

        costs[costIdx++] += bayCost + (baydoors * 1000L);
        costs[costIdx++] = quartersCost;

        // Weapons and Equipment
        // HPG
        if (hasHPG()) {
            costs[costIdx++] += 1000000000;
        } else {
            costs[costIdx++] += 0;
        }
        // Weapons and Equipment
        costs[costIdx++] += getWeaponsAndEquipmentCost(ignoreAmmo);

        // Sum Costs
        for (int i = 0; i < costIdx; i++) {
            cost += costs[i];
        }

        costs[costIdx++] = -getPriceMultiplier(); // Negative indicates multiplier
        cost = Math.round(cost * getPriceMultiplier());
        addCostDetails(cost, costs);
        return cost;
    }

    @Override
    public double getPriceMultiplier() {
        return 2.0; // Weight multiplier
    }
    
    private void addCostDetails(double cost, double[] costs) {
        bvText = new StringBuffer();
        String[] left = { "Bridge", "Computer", "Life Support", "Sensors", "FCS", "Gunnery Control Systems",
                "Structural Integrity", "Drive Unit", "Engine", "Engine Control Unit",
                "KF Drive", "KF Drive Support System", "Attitude Thrusters", "Docking Collars",
                "Fuel Tanks", "Armor", "Heat Sinks", "Life Boats/Escape Pods", "Grav Decks",
                "Bays", "Quarters", "HPG", "Weapons/Equipment", "Weight Multiplier" };

        NumberFormat commafy = NumberFormat.getInstance();

        bvText.append("<HTML><BODY><CENTER><b>Cost Calculations For ");
        bvText.append(getChassis());
        bvText.append(" ");
        bvText.append(getModel());
        bvText.append("</b></CENTER>");
        bvText.append(nl);

        bvText.append(startTable);
        // find the maximum length of the columns.
        for (int l = 0; l < left.length; l++) {

            if (l == 21) {
                getWeaponsAndEquipmentCost(true);
            } else {
                bvText.append(startRow);
                bvText.append(startColumn);
                bvText.append(left[l]);
                bvText.append(endColumn);
                bvText.append(startColumn);

                if (costs[l] == 0) {
                    bvText.append("N/A");
                } else if (costs[l] < 0) {
                    bvText.append("x ");
                    bvText.append(commafy.format(-costs[l]));
                } else {
                    bvText.append(commafy.format(costs[l]));

                }
                bvText.append(endColumn);
                bvText.append(endRow);
            }
        }
        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append("-------------");
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(startRow);
        bvText.append(startColumn);
        bvText.append("Total Cost:");
        bvText.append(endColumn);
        bvText.append(startColumn);
        bvText.append(commafy.format(cost));
        bvText.append(endColumn);
        bvText.append(endRow);

        bvText.append(endTable);
        bvText.append("</BODY></HTML>");
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

    @Override
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
    
    @Override
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
            default:
                return "";
        }
    }
    
    @Override
    public long getEntityType() {
        return Entity.ETYPE_AERO | Entity.ETYPE_JUMPSHIP | Entity.ETYPE_WARSHIP;
    }
    
    @Override
    public boolean canChangeSecondaryFacing() {
        // flying warships can execute the "ECHO" maneuver (stratops 113), aka a torso twist, 
        // if they have the MP for it
        return isAirborne() && !isEvading() && (mpUsed <= getRunMP() - 2);
    }
    
    /**
     * Can this warship "torso twist" in the given direction?
     */
    @Override
    public boolean isValidSecondaryFacing(int dir) {
        int rotate = dir - getFacing();
        if (canChangeSecondaryFacing()) {
            return (rotate == 0) || (rotate == 1) || (rotate == -1)
                    || (rotate == -5) || (rotate == 5);
        }
        return rotate == 0;
    }
    
    /**
     * Return the nearest valid direction to "torso twist" in
     */
    @Override
    public int clipSecondaryFacing(int dir) {
        if (isValidSecondaryFacing(dir)) {
            return dir;
        }
        
        // can't twist without enough MP
        if (!canChangeSecondaryFacing()) {
            return getFacing();
        }
        
        // otherwise, twist once in the appropriate direction
        final int rotate = (dir + (6 - getFacing())) % 6;
        
        return rotate >= 3 ? (getFacing() + 5) % 6 : (getFacing() + 1) % 6;
    }
    
    /**
     * Handler for when the entity enters a new round
     */
    @Override
    public void newRound(int roundNumber) {
        super.newRound(roundNumber);
        
        if (getGame().useVectorMove()) {
            setFacing(getSecondaryFacing());
        }
        
        setSecondaryFacing(getFacing());
    }
    
    /**
     * Utility function that handles situations where a facing change
     * has some kind of permanent effect on the entity.
     */
    @Override
    public void postProcessFacingChange() {
        mpUsed += 2;
    }
}

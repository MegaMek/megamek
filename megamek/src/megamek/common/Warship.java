/*
 * MegaAero - Copyright (C) 2007 Jay Lawson
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common;

import java.text.NumberFormat;
import java.util.Map;

import megamek.client.ui.swing.calculationReport.CalculationReport;
import megamek.common.cost.MekCostCalculator;
import megamek.common.cost.WarShipCostCalculator;
import megamek.common.options.OptionsConstants;

/**
 * @author Jay Lawson
 * @since Jun 17, 2007
 */
public class Warship extends Jumpship {
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

    // ASEW Missile Effects, per location
    // Values correspond to Locations, as seen above: NOS, FLS, FRS, AFT, ALS, ARS, LBS, RBS
    private int[] asewAffectedTurns = { 0, 0, 0, 0, 0, 0, 0, 0 };

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
        // Primitives don't distinguish between JumpShips and WarShips.
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
        // Helium Tanks make up about 2/3 of the drive core.
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
    public double getCost(CalculationReport calcReport, boolean ignoreAmmo) {
        return WarShipCostCalculator.calculateCost(this, calcReport, ignoreAmmo);
    }

    @Override
    public double getPriceMultiplier() {
        return 2.0;
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
        return getAlphaStrikeLocationMultiplier(index, location, rearMounted);
    }
    
    @Override
    public double getAlphaStrikeLocationMultiplier(int index, int location, boolean rearMounted) {
        // ASC p. 103
        switch (location) {
            case LOC_NOSE:
            case LOC_FLS:
            case LOC_FRS:
                if (index == 0) {
                    return 1.0;
                }
                break;
            case LOC_LBS:
            case LOC_ALS:
                if (index == 1) {
                    return 1.0;
                }
                break;
            case LOC_RBS:
            case LOC_ARS:
                if (index == 2) {
                    return 1.0;
                }
                break;
            case LOC_AFT:
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
        
        return (getFacing() + (rotate >= 3 ? 5 : 1)) % 6;
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

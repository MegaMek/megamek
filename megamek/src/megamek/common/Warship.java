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

import megamek.client.ui.swing.calculationReport.CalculationReport;
import megamek.common.cost.WarShipCostCalculator;
import megamek.common.options.OptionsConstants;

import java.io.Serial;

import static megamek.common.Compute.*;

/**
 * @author Jay Lawson
 * @since Jun 17, 2007
 */
public class Warship extends Jumpship {
    @Serial
    private static final long serialVersionUID = 4650692419224312511L;

    // additional Warship locations
    public static final int LOC_LBS = 7;
    public static final int LOC_RBS = 8;

    private static final String[] LOCATION_ABBRS = { "NOS", "FLS", "FRS", "AFT", "ALS", "ARS", "HULL", "LBS", "RBS" };
    private static final String[] LOCATION_NAMES = { "Nose", "Left Front Side", "Right Front Side",
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

    /** ASEW Missile Effects, per location; values correspond to NOS, FLS, FRS, AFT, ALS, ARS, LBS, RBS */
    private final int[] asewAffectedTurns = { 0, 0, 0, 0, 0, 0, 0, 0 };

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
        return isPrimitive() ? super.getConstructionTechAdvancement() : TA_WARSHIP;
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

    @Override
    public int getWeaponArc(int wn) {
        final Mounted<?> mounted = getEquipment(wn);
        int arc = switch (mounted.getLocation()) {
            case LOC_NOSE -> mounted.isInWaypointLaunchMode() ? ARC_NOSE_WPL : ARC_NOSE;
            case LOC_FRS -> mounted.isInWaypointLaunchMode() ? ARC_RIGHTSIDE_SPHERE_WPL : ARC_RIGHTSIDE_SPHERE;
            case LOC_FLS -> mounted.isInWaypointLaunchMode() ? ARC_LEFTSIDE_SPHERE_WPL : ARC_LEFTSIDE_SPHERE;
            case LOC_ARS -> mounted.isInWaypointLaunchMode() ? ARC_RIGHTSIDEA_SPHERE_WPL : ARC_RIGHTSIDEA_SPHERE;
            case LOC_ALS -> mounted.isInWaypointLaunchMode() ? ARC_LEFTSIDEA_SPHERE_WPL : ARC_LEFTSIDEA_SPHERE;
            case LOC_AFT -> mounted.isInWaypointLaunchMode() ? ARC_AFT_WPL : ARC_AFT;
            case LOC_LBS -> mounted.isInWaypointLaunchMode() ? ARC_LEFT_BROADSIDE_WPL : ARC_LEFT_BROADSIDE;
            case LOC_RBS -> mounted.isInWaypointLaunchMode() ? ARC_RIGHT_BROADSIDE_WPL : ARC_RIGHT_BROADSIDE;
            default -> Compute.ARC_360;
        };
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

    @Override
    public boolean hasActiveECM() {
        // All warships automatically have ECM if in space
        if (!game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ECM)
                || !game.getBoard().inSpace()) {
            return super.hasActiveECM();
        }
        return getECMRange() >= 0;
    }

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

    @Override
    public boolean isValidSecondaryFacing(int dir) {
        int rotate = dir - getFacing();
        if (canChangeSecondaryFacing()) {
            return (rotate == 0) || (rotate == 1) || (rotate == -1)
                    || (rotate == -5) || (rotate == 5);
        }
        return rotate == 0;
    }

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

    @Override
    public void newRound(int roundNumber) {
        super.newRound(roundNumber);

        if (getGame().useVectorMove()) {
            setFacing(getSecondaryFacing());
        }

        setSecondaryFacing(getFacing());
    }

    @Override
    public void postProcessFacingChange() {
        mpUsed += 2;
    }

    @Override
    public boolean isJumpShip() {
        return false;
    }

    @Override
    public boolean isWarShip() {
        return true;
    }

    @Override
    public int getGenericBattleValue() {
        return (int) Math.round(Math.exp(-1.3484 + 0.9382 * Math.log(getWeight())));
    }
}

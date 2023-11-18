/*
 * MegaAero - Copyright (C) 2010 Jason Tighe This program is free software; you
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
import megamek.common.cost.FixedWingSupportCostCalculator;

/**
 * @author Jason Tighe
 * @since 10/31/2010
 */
public class FixedWingSupport extends ConvFighter {
    private static final long serialVersionUID = 347113432982248518L;


    public static final int LOC_BODY = 5;

    private static String[] LOCATION_ABBRS = { "NOS", "LWG", "RWG", "AFT", "WNG", "BOD" };
    private static String[] LOCATION_NAMES = { "Nose", "Left Wing", "Right Wing", "Aft", "Wings", "Body" };
    private int[] barRating;

    public FixedWingSupport() {
        super();
        damThresh = new int[] { 0, 0, 0, 0, 0, 0 };
        barRating = new int[locations()];
    }

    @Override
    public boolean isFixedWingSupport() {
        return true;
    }

    @Override
    public boolean isConventionalFighter() {
        return false;
    }

    public void setBARRating(int rating, int loc) {
        barRating[loc] = rating;
    }

    @Override
    public void setBARRating(int rating) {
        for (int i = 0; i < locations(); i++) {
            barRating[i] = rating;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#getBARRating()
     */
    @Override
    public int getBARRating(int loc) {
        return barRating[loc];
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#hasBARArmor()
     */
    @Override
    public boolean hasBARArmor(int loc) {
        return true;
    }

    @Override
    public boolean hasArmoredChassis() {
        return hasWorkingMisc(MiscType.F_ARMORED_CHASSIS);
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
        return 6;
    }

    @Override
    public boolean isSupportVehicle() {
        return true;
    }

    @Override
    public void autoSetSI() {
        initializeSI(getOriginalWalkMP());
    }

    @Override
    public boolean isVSTOL() {
        return hasWorkingMisc(MiscType.F_VSTOL_CHASSIS);
    }

    @Override
    public boolean isSTOL() {
        return hasWorkingMisc(MiscType.F_STOL_CHASSIS);
    }

    public boolean hasPropChassisMod() {
        return hasWorkingMisc(MiscType.F_PROP);
    }

    /**
     * The mass of each point of fuel in kg, based on weight class and engine tech rating.
     */
    private static final int[][] KG_PER_FUEL_POINT = {
            { 50, 30, 23, 15, 13, 10 }, // small
            { 63, 38, 25, 20, 18, 15 }, // medium
            { 83, 50, 35, 28, 23, 20 } // large
    };

    /**
     * While most aerospace units measure fuel weight in points per ton, support vehicles measure
     * in kg per point. Vehicles that do not require fuel return 0.
     *
     * @return The mass of each point of fuel in kg.
     */
    public int kgPerFuelPoint() {
        if (!requiresFuel()) {
            return 0;
        }
        int kg = KG_PER_FUEL_POINT[getWeightClass() - EntityWeightClass.WEIGHT_SMALL_SUPPORT][getEngineTechRating()];
        if (hasPropChassisMod() || getMovementMode().equals(EntityMovementMode.AIRSHIP)) {
            kg = (int) Math.ceil(kg * 0.75);
        }
        return kg;
    }

    @Override
    public double getFuelTonnage() {
        double weight = getOriginalFuel() * kgPerFuelPoint() / 1000.0;
        return RoundWeight.standard(weight, this);
    }

    @Override
    public double getFuelPointsPerTon() {
        return 1000.0 / kgPerFuelPoint();
    }

    @Override
    public boolean requiresFuel() {
        return !(((hasPropChassisMod() || getMovementMode().isAirship()))
                && hasEngine() && (getEngine().isFusion() || (getEngine().getEngineType() == Engine.FISSION)
                || (getEngine().getEngineType() == Engine.SOLAR)));
    }

    private static final TechAdvancement TA_FIXED_WING_SUPPORT = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_PS, DATE_PS, DATE_PS)
            .setTechRating(RATING_B).setAvailability(RATING_C, RATING_D, RATING_C, RATING_C)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
    private static final TechAdvancement TA_FIXED_WING_SUPPORT_LARGE = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_PS, DATE_PS, DATE_PS)
            .setTechRating(RATING_B).setAvailability(RATING_D, RATING_E, RATING_D, RATING_D)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
    private static final TechAdvancement TA_AIRSHIP_SUPPORT_SMALL = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_PS, DATE_PS, DATE_PS)
            .setTechRating(RATING_A).setAvailability(RATING_C, RATING_D, RATING_C, RATING_C)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
    private static final TechAdvancement TA_AIRSHIP_SUPPORT_MEDIUM = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_PS, DATE_PS, DATE_PS)
            .setTechRating(RATING_B).setAvailability(RATING_D, RATING_E, RATING_D, RATING_D)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
    // Availability missing from TO. Using medium
    private static final TechAdvancement TA_AIRSHIP_SUPPORT_LARGE = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_PS, DATE_PS, DATE_PS)
            .setTechRating(RATING_C).setAvailability(RATING_D, RATING_E, RATING_D, RATING_D)
            .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    // Also using early spaceflight for intro dates based on common sense.
    private static final TechAdvancement TA_SATELLITE_SMALL = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_ES, DATE_ES, DATE_ES)
            .setTechRating(RATING_C).setAvailability(RATING_C, RATING_D, RATING_C, RATING_C)
            .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    private static final TechAdvancement TA_SATELLITE_MEDIUM = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_ES, DATE_ES, DATE_ES)
            .setTechRating(RATING_C).setAvailability(RATING_C, RATING_D, RATING_D, RATING_D)
            .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    private static final TechAdvancement TA_SATELLITE_LARGE = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_ES, DATE_ES, DATE_ES)
            .setTechRating(RATING_C).setAvailability(RATING_D, RATING_E, RATING_D, RATING_D)
            .setStaticTechLevel(SimpleTechLevel.ADVANCED);

    @Override
    public TechAdvancement getConstructionTechAdvancement() {
        return getConstructionTechAdvancement(getMovementMode(), getWeightClass());
    }

    public static TechAdvancement getConstructionTechAdvancement(EntityMovementMode movementMode, int weightClass) {
        if (movementMode.equals(EntityMovementMode.AIRSHIP)) {
            if (weightClass == EntityWeightClass.WEIGHT_LARGE_SUPPORT) {
                return TA_AIRSHIP_SUPPORT_LARGE;
            } else if (weightClass == EntityWeightClass.WEIGHT_MEDIUM_SUPPORT) {
                return TA_AIRSHIP_SUPPORT_MEDIUM;
            } else {
                return TA_AIRSHIP_SUPPORT_SMALL;
            }
        } else if (movementMode.equals(EntityMovementMode.STATION_KEEPING)) {
            if (weightClass == EntityWeightClass.WEIGHT_LARGE_SUPPORT) {
                return TA_SATELLITE_LARGE;
            } else if (weightClass == EntityWeightClass.WEIGHT_MEDIUM_SUPPORT) {
                return TA_SATELLITE_MEDIUM;
            } else {
                return TA_SATELLITE_SMALL;
            }
        } else if (weightClass == EntityWeightClass.WEIGHT_LARGE_SUPPORT) {
            return TA_FIXED_WING_SUPPORT_LARGE;
        } else {
            return TA_FIXED_WING_SUPPORT;
        }
    }

    @Override
    protected int calculateWalk() {
        return getOriginalWalkMP();
    }

    @Override
    public void autoSetMaxBombPoints() {
        // fixed wing support craft need external stores hardpoints to be able to carry bombs
        int bombpoints = 0;
        for (Mounted misc : getMisc()) {
            if (misc.getType().hasFlag(MiscType.F_EXTERNAL_STORES_HARDPOINT)) {
                bombpoints++;
            }
        }
        maxBombPoints = bombpoints;
    }

    @Override
    public void initializeThresh(int loc) {
        int bar = getBARRating(loc);
        if (bar == 10) {
            setThresh((int) Math.ceil(getArmor(loc) / 10.0), loc);
        } else if (bar >= 2) {
            setThresh(1, loc);
        } else {
            setThresh(0, loc);
        }
    }

    @Override
    public double getBaseEngineValue() {
        if (getWeight() < 5) {
            return 0.005;
        } else if (getWeight() <= 100) {
            return 0.01;
        } else {
            return 0.015;
        }
    }

    @Override
    public double getBaseChassisValue() {
        if (getWeight() < 5) {
            return 0.08;
        } else if (getWeight() <= 100) {
            return 0.1;
        } else {
            return 0.15;
        }
    }

    public int getTotalSlots() {
        return 5 + (int) Math.floor(getWeight() / 10);
    }

    @Override
    public double getCost(CalculationReport calcReport, boolean ignoreAmmo) {
        return FixedWingSupportCostCalculator.calculateCost(this, calcReport, ignoreAmmo);
    }

    @Override
    public double getPriceMultiplier() {
        double priceMultiplier = 1.0;
        switch (movementMode) {
            case AERODYNE:
                priceMultiplier = 1 + weight / 50.0;
                break;
            case AIRSHIP:
                priceMultiplier = 1 + weight / 10000;
                break;
            case STATION_KEEPING:
                priceMultiplier = 1 + weight / 75.0;
                break;
            default:
                break;
        }
        return priceMultiplier;
    }

    @Override
    public double getBVTypeModifier() {
        return 1.0;
    }

    @Override
    public long getEntityType() {
        return Entity.ETYPE_AERO | Entity.ETYPE_CONV_FIGHTER | Entity.ETYPE_FIXED_WING_SUPPORT;
    }

    @Override
    public boolean isAerospaceSV() {
        return true;
    }
}

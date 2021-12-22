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
/*
 * Created on 10/31/2010
 */
package megamek.common;

import megamek.common.verifier.SupportVeeStructure;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Jason Tighe
 */
public class FixedWingSupport extends ConvFighter {


    /**
     *
     */
    private static final long serialVersionUID = 347113432982248518L;


    public static final int LOC_BODY = 5;

    private static String[] LOCATION_ABBRS =
        { "NOS", "LWG", "RWG", "AFT", "WNG", "BOD" };
    private static String[] LOCATION_NAMES =
        { "Nose", "Left Wing", "Right Wing", "Aft", "Wings", "Body" };
    private int[] barRating;

    public FixedWingSupport() {
        super();
        damThresh = new int[] { 0, 0, 0, 0, 0, 0 };
        barRating = new int[locations()];
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
            {50, 30, 23, 15, 13, 10}, // small
            {63, 38, 25, 20, 18, 15}, // medium
            {83, 50, 35, 28, 23, 20} // large
    };

    /**
     * While most aerospace units measure fuel weight in points per ton, support vehicles measure
     * in kg per point. Vehicles that do not require fuel return 0.
     *
     * @return The mass of each point of fuel in kg.
     */
    public int kgPerFuelPoint() {
        int kg = KG_PER_FUEL_POINT[getWeightClass() - EntityWeightClass.WEIGHT_SMALL_SUPPORT][getEngineTechRating()];
        if (hasPropChassisMod() || getMovementMode().equals(EntityMovementMode.AIRSHIP)) {
            if (getEngine().isFusion() || (getEngine().getEngineType() == Engine.FISSION)
                    || (getEngine().getEngineType() == Engine.SOLAR)) {
                return 0;
            }
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
    public int getBattleForceSize() {
        //The tables are on page 356 of StartOps
        if (getWeight() < 5) {
            return 1;
        }
        if (getWeight() < 100) {
            return 2;
        }

        return 3;
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
    public void addBattleForceSpecialAbilities(Map<BattleForceSPA,Integer> specialAbilities) {
        super.addBattleForceSpecialAbilities(specialAbilities);
        specialAbilities.put(BattleForceSPA.ATMO, null);
        if (getMaxBombPoints() > 0) {
            specialAbilities.put(BattleForceSPA.BOMB, getMaxBombPoints() / 5);
        }
    }

    @Override
    public double getCost(boolean ignoreAmmo) {
        double[] costs = new double[13 + locations()];
        int i = 0;
        // Chassis cost for Support Vehicles

        double chassisCost = 2500 * SupportVeeStructure.getWeightStructure(this);
        if (hasMisc(MiscType.F_AMPHIBIOUS)) {
            chassisCost *= 1.25;
        }
        if (hasMisc(MiscType.F_ARMORED_CHASSIS)) {
            chassisCost *= 2.0;
        }
        if (hasMisc(MiscType.F_ENVIRONMENTAL_SEALING)) {
            chassisCost *= 1.75;
        }
        if (hasMisc(MiscType.F_PROP)) {
            chassisCost *= 0.75;
        }
        if (hasMisc(MiscType.F_STOL_CHASSIS)) {
            chassisCost *= 1.5;
        }
        if (hasMisc(MiscType.F_ULTRA_LIGHT)) {
            chassisCost *= 1.5;
        }
        if (hasMisc(MiscType.F_VSTOL_CHASSIS)) {
            chassisCost *= 2;
        }
        costs[i++] = chassisCost;

        // Engine Costs
        double engineCost = 0.0;
        if (hasEngine()) {
            engineCost = 5000 * getEngine().getWeightEngine(this)
                    * Engine.getSVCostMultiplier(getEngine().getEngineType());
        }
        costs[i++] = engineCost;

        // armor
        if (getArmorType(firstArmorIndex()) == EquipmentType.T_ARMOR_STANDARD) {
            int totalArmorPoints = 0;
            for (int loc = 0; loc < locations(); loc++) {
                totalArmorPoints += getOArmor(loc);
            }
            costs[i++] = totalArmorPoints *
                    EquipmentType.getSupportVehicleArmorCostPerPoint(getBARRating(firstArmorIndex()));
        } else {
            if (hasPatchworkArmor()) {
                for (int loc = 0; loc < locations(); loc++) {
                    costs[i++] = getArmorWeight(loc)
                            * EquipmentType.getArmorCost(armorType[loc]);
                }

            } else {
                costs[i++] = getArmorWeight()
                        * EquipmentType.getArmorCost(armorType[0]);
            }
        }

        // Compute final structural cost
        int structCostIdx = i++;
        for (int c = 0; c < structCostIdx; c++) {
            costs[structCostIdx] += costs[c];
        }
        double techRatingMultiplier = 0.5 + (getStructuralTechRating() * 0.25);
        costs[structCostIdx] *= techRatingMultiplier;

        double freeHeatSinks = (hasEngine() ? getEngine().getWeightFreeEngineHeatSinks() : 0);
        int sinks = 0;
        double paWeight = 0;
        for (Mounted m : getWeaponList()) {
            WeaponType wt = (WeaponType) m.getType();
            if (wt.hasFlag(WeaponType.F_LASER) || wt.hasFlag(WeaponType.F_PPC)) {
                sinks += wt.getHeat();
                paWeight += m.getTonnage() / 10.0;
            }
        }
        paWeight = Math.ceil(paWeight * 2) / 2;
        if ((hasEngine() && (getEngine().isFusion() || getEngine().getEngineType() == Engine.FISSION))
                || getWeightClass() == EntityWeightClass.WEIGHT_SMALL_SUPPORT) {
            paWeight = 0;
        }
        costs[i++] = 20000 * paWeight;
        costs[i++] = 2000 * Math.max(0, sinks - freeHeatSinks);

        costs[i++] = getWeaponsAndEquipmentCost(ignoreAmmo);

        double cost = 0; // calculate the total
        for (int x = structCostIdx; x < i; x++) {
            cost += costs[x];
        }
        if (isOmni()) { // Omni conversion cost goes here.
            cost *= 1.25;
            costs[i++] = -1.25;
        } else {
            costs[i++] = 0;
        }

        cost *= getPriceMultiplier();
        costs[i++] = -getPriceMultiplier();

        addCostDetails(cost, costs);
        return Math.round(cost);
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

    private void addCostDetails(double cost, double[] costs) {
        bvText = new StringBuffer();
        List<String> left = new ArrayList<>();

        left.add("Chassis");
        left.add("Engine");
        left.add("Armor");
        left.add("Final Structural Cost");
        left.add("Power Amplifiers");
        left.add("Heat Sinks");
        left.add("Equipment");
        left.add("Omni Multiplier");
        left.add("Tonnage Multiplier");

        NumberFormat commafy = NumberFormat.getInstance();

        bvText.append("<HTML><BODY><CENTER><b>Cost Calculations For ");
        bvText.append(getChassis());
        bvText.append(" ");
        bvText.append(getModel());
        bvText.append("</b></CENTER>");
        bvText.append(nl);

        bvText.append(startTable);
        // find the maximum length of the columns.
        for (int l = 0; l < left.size(); l++) {

            if (l == 6) {
                getWeaponsAndEquipmentCost(true);
            } else {
                if (left.get(l).equals("Final Structural Cost")) {
                    bvText.append(startRow);
                    bvText.append(startColumn);
                    bvText.append(endColumn);
                    bvText.append(startColumn);
                    bvText.append("-------------");
                    bvText.append(endColumn);
                    bvText.append(endRow);
                }
                bvText.append(startRow);
                bvText.append(startColumn);
                bvText.append(left.get(l));
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

    @Override
    public double getBVTypeModifier() {
        return 1.0;
    }

    @Override
    public long getEntityType() {
        return Entity.ETYPE_AERO | Entity.ETYPE_CONV_FIGHTER | Entity.ETYPE_FIXED_WING_SUPPORT;
    }
}
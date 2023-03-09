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
import megamek.common.cost.ConvFighterCostCalculator;
import megamek.common.options.OptionsConstants;

/**
 * @author Jay Lawson
 * @since Jun 12, 2008
 */
public class ConvFighter extends Aero {
    private static final long serialVersionUID = 6297668284292929409L;

    @Override
    public int getUnitType() {
        return UnitType.CONV_FIGHTER;
    }

    @Override
    public boolean isConventionalFighter() {
        return true;
    }

    @Override
    public boolean isAerospaceFighter() {
        return false;
    }

    @Override
    public boolean doomedInVacuum() {
        return true;
    }

    @Override
    public boolean doomedInSpace() {
        return true;
    }

    @Override
    public int getHeatCapacity() {
        return DOES_NOT_TRACK_HEAT;
    }
    
    @Override
    public boolean tracksHeat() {
        return false;
    }

    @Override
    public double getFuelPointsPerTon() {
        return 160;
    }

    @Override
    public int getFuelUsed(int thrust) {
        if (!hasEngine()) {
            return 0;
        }
        int overThrust = Math.max(thrust - getWalkMP(), 0);
        int safeThrust = thrust - overThrust;
        int used = safeThrust + (2 * overThrust);
        if (!getEngine().isFusion()) {
            used = (int) Math.floor(safeThrust * 0.5) + overThrust;
        } else if (game.getOptions().booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_CONV_FUSION_BONUS)) {
            used = (int) Math.floor(safeThrust * 0.5) + (2 * overThrust);
        }
        return used;
    }

    protected static final TechAdvancement TA_CONV_FIGHTER = new TechAdvancement(TECH_BASE_ALL)
                .setAdvancement(DATE_NONE, 2470, 2490).setProductionFactions(F_TH)
                .setTechRating(RATING_D).setAvailability(RATING_C, RATING_D, RATING_C, RATING_B)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
    
    @Override
    public TechAdvancement getConstructionTechAdvancement() {
        return TA_CONV_FIGHTER;
    }
    
    @Override
    public double getBVTypeModifier() {
        return 1.1;
    }

    @Override
    public double getCost(CalculationReport calcReport, boolean ignoreAmmo) {
        return ConvFighterCostCalculator.calculateCost(this, calcReport, ignoreAmmo);
    }

    @Override
    public double getPriceMultiplier() {
        double priceMultiplier = 1.0;
        // omni multiplier (leaving this in for now even though conventional fighters don't make for legal omnis)
        if (isOmni()) {
            priceMultiplier *= 1.25f;
        }
        priceMultiplier *= 1 + (weight / 200.0); // weight multiplier
        return priceMultiplier;
    }

    @Override
    protected int calculateWalk() {
        if (!hasEngine()) {
            return 0;
        }
        if (isPrimitive()) {
            double rating = getEngine().getRating();
            rating /= 1.2;
            if ((rating % 5) != 0) {
                return (int) (((rating - (rating % 5)) + 5) / (int) weight);
            }
            return (int) (rating / (int) weight);
        }
        return (getEngine().getRating() / (int) weight);
    }

    @Override
    public long getEntityType() {
        return Entity.ETYPE_AERO | Entity.ETYPE_CONV_FIGHTER;
    }
}

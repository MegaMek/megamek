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
 * Created on Jun 12, 2008
 *
 */
package megamek.common;

import megamek.common.options.OptionsConstants;

/**
 * @author Jay Lawson
 */
public class ConvFighter extends Aero {

    /**
     *
     */
    private static final long serialVersionUID = 6297668284292929409L;

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
        return 999;
    }

    @Override
    public int getFuelUsed(int thrust) {
        if(!hasEngine()) {
            return 0;
        }
        int overThrust =  Math.max(thrust - getWalkMP(), 0);
        int safeThrust = thrust - overThrust;
        int used = safeThrust + (2 * overThrust);
        if (!getEngine().isFusion()) {
            used = (int) Math.floor(safeThrust * 0.5) + overThrust;
        } else if (game.getOptions().booleanOption(
                OptionsConstants.ADVAERORULES_STRATOPS_CONV_FUSION_BONUS)) {
            used = (int) Math.floor(safeThrust * 0.5) + (2 * overThrust);
        }
        return used;
    }

    @Override
    public double getBVTypeModifier() {
        if (hasStealth()) {
            return 1.4;
        }
        return 1.1;
    }

    @Override
    public double getCost(boolean ignoreAmmo) {

        double cost = 0;

        // add in cockpit
        double avionicsWeight = Math.ceil(weight / 5) / 2;
        cost += 4000 * avionicsWeight;

        // add VSTOL gear if applicable
        if (isVSTOL()) {
            double vstolWeight = Math.ceil(weight / 10) / 2;
            cost += 5000 * vstolWeight;
        }

        // Structural integrity
        cost += 4000 * getSI();

        // additional flight systems (attitude thruster and landing gear)
        cost += 25000 + (10 * getWeight());

        // engine
        if(hasEngine()) {
            cost += (getEngine().getBaseCost() * getEngine().getRating() * weight) / 75.0;
        }
        
        // fuel tanks
        cost += (200 * getFuel()) / 160.0;

        // armor
        if (hasPatchworkArmor()) {
            for (int loc = 0; loc < locations(); loc++) {
                cost += getArmorWeight(loc) * EquipmentType.getArmorCost(armorType[loc]);
            }

        } else {
            cost += getArmorWeight() * EquipmentType.getArmorCost(armorType[0]);
        }
        // heat sinks
        int sinkCost = 2000 + (4000 * getHeatType());// == HEAT_DOUBLE ? 6000:
        // 2000;
        cost += sinkCost * getHeatSinks();

        // weapons
        cost += getWeaponsAndEquipmentCost(ignoreAmmo);

        // power amplifiers, if any
        cost += 20000 * getPowerAmplifierWeight();

        // omni multiplier (leaving this in for now even though conventional
        // fighters
        // don't make for legal omnis)
        double omniMultiplier = 1;
        if (isOmni()) {
            omniMultiplier = 1.25f;
        }

        double weightMultiplier = 1 + (weight / 200.0);

        return Math.round(cost * omniMultiplier * weightMultiplier);

    }

    @Override
    protected int calculateWalk() {
        if(!hasEngine()) {
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

    public long getEntityType() {
        return Entity.ETYPE_AERO | Entity.ETYPE_CONV_FIGHTER;
    }

}
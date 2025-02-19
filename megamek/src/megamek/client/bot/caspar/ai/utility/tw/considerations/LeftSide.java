/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 */

package megamek.client.bot.caspar.ai.utility.tw.considerations;

import com.fasterxml.jackson.annotation.JsonTypeName;
import megamek.common.*;

import java.util.Map;

/**
 * Amount of armor on a specific side
 */
@JsonTypeName("LeftSide")
public class LeftSide extends OverallArmor {

    public LeftSide() {
    }

    @Override
    protected double getDamage(Aero unit) {
        return unit.getArmor(Aero.LOC_LWING) / (double) unit.getOArmor(Aero.LOC_LWING);
    }

    @Override
    protected double getDamage(Tank unit) {
        return unit.getArmor(Tank.LOC_LEFT) / (double) unit.getOArmor(Tank.LOC_LEFT);
    }

    @Override
    protected double getDamage(SuperHeavyTank unit) {
        return (unit.getArmor(SuperHeavyTank.LOC_FRONTLEFT) + unit.getArmor(SuperHeavyTank.LOC_REARLEFT))/ (double) (unit.getOArmor(SuperHeavyTank.LOC_FRONTLEFT) + unit.getOArmor(SuperHeavyTank.LOC_REARLEFT));
    }

    @Override
    protected double getDamage(Mek unit) {
        return (unit.getArmor(Mek.LOC_LT) + unit.getArmor(Mek.LOC_LARM) + unit.getArmor(Mek.LOC_LLEG))
            / (double) (unit.getOArmor(Mek.LOC_LT) + unit.getOArmor(Mek.LOC_LARM) + unit.getOArmor(Mek.LOC_LLEG));
    }

    @Override
    protected double getDamage(Jumpship unit) {
        return (unit.getArmor(Jumpship.LOC_FLS) + unit.getArmor(Jumpship.LOC_ALS)) / (double) (unit.getOArmor(Jumpship.LOC_FLS) + unit.getOArmor(Jumpship.LOC_ALS));
    }

    @Override
    protected double getDamage(SpaceStation unit) {
        return (unit.getArmor(SpaceStation.LOC_FLS) + unit.getArmor(SpaceStation.LOC_ALS)) / (double) (unit.getOArmor(SpaceStation.LOC_FLS) + unit.getOArmor(SpaceStation.LOC_ALS));
    }

    @Override
    protected double getDamage(Warship unit) {
        return (unit.getArmor(Warship.LOC_FLS) + unit.getArmor(Warship.LOC_ALS) + unit.getArmor(Warship.LOC_LBS)) / (double) (unit.getOArmor(Warship.LOC_FLS) + unit.getOArmor(Warship.LOC_ALS) + unit.getOArmor(Warship.LOC_LBS));
    }

    @Override
    public LeftSide copy() {
        var copy = new LeftSide();
        copy.setCurve(getCurve().copy());
        copy.setParameters(Map.copyOf(getParameters()));
        copy.setName(getName());
        return copy;
    }
}

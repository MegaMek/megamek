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
@JsonTypeName("RightSide")
public class RightSide extends OverallArmor {

    public RightSide() {
    }

    @Override
    protected double getDamage(Aero unit) {
        return unit.getArmor(Aero.LOC_RWING) / (double) unit.getOArmor(Aero.LOC_RWING);
    }

    @Override
    protected double getDamage(Tank unit) {
        return unit.getArmor(Tank.LOC_RIGHT) / (double) unit.getOArmor(Tank.LOC_RIGHT);
    }

    @Override
    protected double getDamage(SuperHeavyTank unit) {
        return (unit.getArmor(SuperHeavyTank.LOC_FRONTRIGHT) + unit.getArmor(SuperHeavyTank.LOC_REARRIGHT)) / (double) (unit.getOArmor(SuperHeavyTank.LOC_FRONTRIGHT) + unit.getOArmor(SuperHeavyTank.LOC_REARRIGHT));
    }

    @Override
    protected double getDamage(Mek unit) {
        return (unit.getArmor(Mek.LOC_RT) + unit.getArmor(Mek.LOC_RARM) + unit.getArmor(Mek.LOC_RLEG))
            / (double) (unit.getOArmor(Mek.LOC_RT) + unit.getOArmor(Mek.LOC_RARM) + unit.getOArmor(Mek.LOC_RLEG));
    }

    @Override
    protected double getDamage(Jumpship unit) {
        return (unit.getArmor(Jumpship.LOC_FRS) + unit.getArmor(Jumpship.LOC_ARS)) / (double) (unit.getOArmor(Jumpship.LOC_FRS) + unit.getOArmor(Jumpship.LOC_ARS));
    }

    @Override
    protected double getDamage(SpaceStation unit) {
        return (unit.getArmor(SpaceStation.LOC_FRS) + unit.getArmor(SpaceStation.LOC_ARS)) / (double) (unit.getOArmor(SpaceStation.LOC_FRS) + unit.getOArmor(SpaceStation.LOC_ARS));
    }

    @Override
    protected double getDamage(Warship unit) {
        return (unit.getArmor(Warship.LOC_FRS) + unit.getArmor(Warship.LOC_ARS) + unit.getArmor(Warship.LOC_RBS)) / (double) (unit.getOArmor(Warship.LOC_FRS) + unit.getOArmor(Warship.LOC_ARS) + unit.getOArmor(Warship.LOC_RBS));
    }

    @Override
    public RightSide copy() {
        var copy = new RightSide();
        copy.setCurve(getCurve().copy());
        copy.setParameters(Map.copyOf(getParameters()));
        copy.setName(getName());
        return copy;
    }
}

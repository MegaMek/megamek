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
import megamek.ai.utility.Curve;
import megamek.common.*;

import java.util.Map;

/**
 * Amount of armor on a specific side
 */
@JsonTypeName("FrontSide")
public class FrontSide extends OverallArmor {

    public FrontSide() {
    }

    public FrontSide(String name, Curve curve) {
        super(name, curve);
    }

    public FrontSide(String name) {
        super(name);
    }

    @Override
    protected double getDamage(Aero unit) {
        return unit.getArmor(Aero.LOC_NOSE) / (double) unit.getOArmor(Aero.LOC_NOSE);
    }

    @Override
    protected double getDamage(Tank unit) {
        return unit.getArmor(Tank.LOC_FRONT) / (double) unit.getOArmor(Tank.LOC_FRONT);
    }

    @Override
    protected double getDamage(SuperHeavyTank unit) {
        return unit.getArmor(SuperHeavyTank.LOC_FRONT) / (double) unit.getOArmor(SuperHeavyTank.LOC_FRONT);
    }

    @Override
    protected double getDamage(Mek unit) {
        return (unit.getArmor(Mek.LOC_CT) + unit.getArmor(Mek.LOC_LT) + unit.getArmor(Mek.LOC_RT))
            / (double) (unit.getOArmor(Mek.LOC_CT) + unit.getOArmor(Mek.LOC_LT) + unit.getOArmor(Mek.LOC_RT));
    }

    @Override
    protected double getDamage(ProtoMek unit) {
        return unit.getArmor(ProtoMek.LOC_BODY) / (double) unit.getOArmor(ProtoMek.LOC_BODY);
    }

    @Override
    protected double getDamage(Jumpship unit) {
        return unit.getArmor(Jumpship.LOC_NOSE) / (double) unit.getOArmor(Jumpship.LOC_NOSE);
    }

    @Override
    protected double getDamage(SpaceStation unit) {
        return unit.getArmor(SpaceStation.LOC_NOSE) / (double) unit.getOArmor(SpaceStation.LOC_NOSE);
    }

    @Override
    protected double getDamage(Warship unit) {
        return unit.getArmor(Warship.LOC_NONE) / (double) unit.getOArmor(Warship.LOC_NOSE);
    }

    @Override
    public FrontSide copy() {
        var copy = new FrontSide();
        copy.setCurve(getCurve().copy());
        copy.setParameters(Map.copyOf(getParameters()));
        copy.setName(getName());
        return copy;
    }
}

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
import megamek.ai.utility.DecisionContext;
import megamek.common.*;

import java.util.Map;

/**
 * Amount of armor on a specific side
 */
@JsonTypeName("OverallArmor")
public class OverallArmor extends TWConsideration {

    public OverallArmor() {
    }

    public OverallArmor(String name, Curve curve) {
        super(name, curve);
    }

    public OverallArmor(String name) {
        super(name);
    }

    @Override
    public double score(DecisionContext context) {
        var unit = context.getCurrentUnit();
        if (unit instanceof Warship warship) {
            return getDamage(warship);
        } else if (unit instanceof SpaceStation spaceStation) {
            return getDamage(spaceStation);
        } else if (unit instanceof Jumpship jumpship) {
            return getDamage(jumpship);
        } else if (unit instanceof Aero aero) {
            return getDamage(aero);
        } else if (unit instanceof SuperHeavyTank superHeavyTank) {
            return getDamage(superHeavyTank);
        } else if (unit instanceof GunEmplacement gunEmplacement) {
            return getDamage(gunEmplacement);
        } else if (unit instanceof Tank tank) {
            return getDamage(tank);
        } else if (unit instanceof Mek mek) {
            return getDamage(mek);
        } else if (unit instanceof ProtoMek protoMek) {
            return getDamage(protoMek);
        } else {
            return unit.getArmorRemainingPercent();
        }
    }

    protected double getDamage(Aero unit) {
        return unit.getArmorRemainingPercent();
    }

    protected double getDamage(Tank unit) {
        return unit.getArmorRemainingPercent();
    }

    protected double getDamage(GunEmplacement unit) {
        return unit.getArmorRemainingPercent();
    }

    protected double getDamage(SuperHeavyTank unit) {
        return unit.getArmorRemainingPercent();
    }

    protected double getDamage(Mek unit) {
        return unit.getArmorRemainingPercent();
    }

    protected double getDamage(ProtoMek unit) {
        return unit.getArmorRemainingPercent();
    }

    protected double getDamage(Jumpship unit) {
        return unit.getArmorRemainingPercent();
    }

    protected double getDamage(SpaceStation unit) {
        return unit.getArmorRemainingPercent();
    }

    protected double getDamage(Warship unit) {
        return unit.getArmorRemainingPercent();
    }

    @Override
    public OverallArmor copy() {
        var copy = new OverallArmor();
        copy.setCurve(getCurve().copy());
        copy.setParameters(Map.copyOf(getParameters()));
        copy.setName(getName());
        return copy;
    }
}

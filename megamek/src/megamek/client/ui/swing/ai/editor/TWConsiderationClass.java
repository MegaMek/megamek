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

package megamek.client.ui.swing.ai.editor;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import megamek.ai.utility.Consideration;
import megamek.client.bot.queen.ai.utility.tw.considerations.*;

public enum TWConsiderationClass {

    DamageOutput(DamageOutput.class),
    FacingTheEnemy(FacingTheEnemy.class),
    FavoriteTargetInRange(FavoriteTargetInRange.class),
    IsVIPCloser(IsVIPCloser.class),
    MyUnitBotSettings(MyUnitBotSettings.class),
    MyUnitHeatManagement(MyUnitHeatManagement.class),
    MyUnitIsMovingTowardsWaypoint(MyUnitIsMovingTowardsWaypoint.class),
    MyUnitArmor(MyUnitArmor.class),
    MyUnitIsCrippled(MyUnitIsCrippled.class),
    MyUnitUnderThreat(MyUnitUnderThreat.class),
    TargetUnitsArmor(TargetUnitsArmor.class),
    MyUnitRoleIs(MyUnitRoleIs.class),
    TargetWithinRange(TargetWithinRange.class),
    TargetWithinOptimalRange(TargetWithinOptimalRange.class);

    private final Class<? extends Consideration<?,?>> considerationClass;

    TWConsiderationClass(Class<? extends Consideration<?,?>> considerationClass) {
        this.considerationClass = considerationClass;
    }

    public Class<? extends Consideration<?,?>> getConsiderationClass() {
        return considerationClass;
    }

    public static TWConsiderationClass fromClass(Class<?> considerationClass) {
        for (TWConsiderationClass twConsiderationClass : values()) {
            if (twConsiderationClass.considerationClass.equals(considerationClass)) {
                return twConsiderationClass;
            }
        }
        return null;
    }
}

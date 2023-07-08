/*
 * Copyright (c) 2010 - Ben Mazur (bmazur@sev.org).
 * Copyright (c) 2022-2023 - The MegaMek Team. All Rights Reserved.
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
 */
package megamek.common;

public class BattleArmorHandlesTank extends BattleArmorHandles {
    private static final long serialVersionUID = 1031947858009941399L;

    @Override
    public final boolean isWeaponBlockedAt(int loc, boolean isRear) {
        Entity carriedBA = game.getEntity(carriedUnit);
        if (carriedBA == null) {
            return false;
        } else {
            int tloc = BattleArmor.LOC_SQUAD;
            int tloc2 = BattleArmor.LOC_SQUAD;
            switch (loc) {
                case Tank.LOC_REAR:
                    tloc = BattleArmor.LOC_TROOPER_5;
                    tloc2 = BattleArmor.LOC_TROOPER_6;
                    break;
                case Tank.LOC_LEFT:
                    tloc = BattleArmor.LOC_TROOPER_3;
                    tloc2 = BattleArmor.LOC_TROOPER_4;
                    break;
                case Tank.LOC_RIGHT:
                    tloc = BattleArmor.LOC_TROOPER_1;
                    tloc2 = BattleArmor.LOC_TROOPER_2;
                    break;
            }
            return ((carriedBA.locations() > tloc) && (carriedBA.getInternal(tloc) > 0))
                    || ((carriedBA.locations() > tloc2) && (carriedBA.getInternal(tloc2) > 0));
        }
    }
}
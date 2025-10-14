/*
 * Copyright (c) 2010 - Ben Mazur (bmazur@sev.org).
 * Copyright (C) 2006-2022-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.battleArmor;

import java.io.Serial;

import megamek.common.units.Entity;
import megamek.common.units.Tank;

public class BattleArmorHandlesTank extends BattleArmorHandles {
    @Serial
    private static final long serialVersionUID = 1031947858009941399L;

    @Override
    public final boolean isWeaponBlockedAt(int loc, boolean isRear) {
        Entity carriedBA = game.getEntity(carriedUnit);
        if (carriedBA == null) {
            return false;
        } else {
            int troopLocation1 = BattleArmor.LOC_SQUAD;
            int troopLocation2 = BattleArmor.LOC_SQUAD;
            troopLocation2 = switch (loc) {
                case Tank.LOC_REAR -> {
                    troopLocation1 = BattleArmor.LOC_TROOPER_5;
                    yield BattleArmor.LOC_TROOPER_6;
                }
                case Tank.LOC_LEFT -> {
                    troopLocation1 = BattleArmor.LOC_TROOPER_3;
                    yield BattleArmor.LOC_TROOPER_4;
                }
                case Tank.LOC_RIGHT -> {
                    troopLocation1 = BattleArmor.LOC_TROOPER_1;
                    yield BattleArmor.LOC_TROOPER_2;
                }
                default -> troopLocation2;
            };
            return ((carriedBA.locations() > troopLocation1) && (carriedBA.getInternal(troopLocation1) > 0))
                  || ((carriedBA.locations() > troopLocation2) && (carriedBA.getInternal(troopLocation2) > 0));
        }
    }
}

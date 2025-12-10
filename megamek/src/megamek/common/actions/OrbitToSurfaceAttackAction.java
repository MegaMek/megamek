/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.actions;

import megamek.common.compute.Compute;
import megamek.common.game.Game;
import megamek.common.equipment.WeaponType;

public class OrbitToSurfaceAttackAction extends ArtilleryAttackAction {

    public OrbitToSurfaceAttackAction(int entityId, int targetType, int targetId, int weaponId, Game game) {
        super(entityId, targetType, targetId, weaponId, game);
        WeaponType wType = (WeaponType) getEntity(game).getEquipment(weaponId).getType();
        if (wType.hasFlag(WeaponType.F_DIRECT_FIRE) && wType.hasFlag(WeaponType.F_ENERGY)) {
            turnsTilHit = 0;
        } else if (wType.hasFlag(WeaponType.F_DIRECT_FIRE) && wType.hasFlag(WeaponType.F_BALLISTIC)) {
            turnsTilHit = 1;
        } else if (wType.hasFlag(WeaponType.F_MISSILE)) {
            // @@MultiBoardTODO: Leave open and decide by the server?
            turnsTilHit = Compute.d6(1);
        }
    }
}

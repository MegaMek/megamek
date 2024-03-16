/*
 * Copyright (c) 2013 - Ben Mazur (bmazur@sev.org).
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.weapons;

import megamek.common.Game;
import megamek.common.HitData;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.server.GameManager;

public class ReengineeredLaserWeaponHandler extends EnergyWeaponHandler {
    private static final long serialVersionUID = -7390162086880372388L;

    public ReengineeredLaserWeaponHandler(ToHitData toHit, WeaponAttackAction waa, Game g,
                                          GameManager m) {
        super(toHit, waa, g, m);
        // so that reflective armor doesn't halve the damage
        generalDamageType = HitData.DAMAGE_IGNORES_DMG_REDUCTION;
    }
}

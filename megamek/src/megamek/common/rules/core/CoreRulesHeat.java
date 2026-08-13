package megamek.common.rules.core;
/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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


import megamek.common.CriticalSlot;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Mounted;
import megamek.common.rules.RulesHeat;

import java.util.ArrayList;

public class CoreRulesHeat extends RulesHeat {

    /**
     * {@inheritDoc}
     * Attempts to stand do not generate heat Core p.100
     */
    @Override
    public int standingHeat() {
        return 0;
    }

    /**
     * {@inheritDoc}
     * Life support crit hits affect heat and pilot damage. Core p.98
     */
    @Nullable
    @Override
    public LifeSupportHeat checkLifeSupportHeat(int damageHeat,
          boolean torsoMountedCockpit,
          boolean mtHeat, boolean bPainShunt) {
        if (bPainShunt) {
            return null;
        }
        if ((damageHeat >= 47) && mtHeat) {
            // mekwarrior takes 5 damage
            return new LifeSupportHeat(47, 5);
        } else if ((damageHeat >= 39) && mtHeat) {
            // mekwarrior takes 4 damage
            return new LifeSupportHeat(39, 4);
        } else if ((damageHeat >= 32) && mtHeat) {
            // mekwarrior takes 3 damage
            return new LifeSupportHeat(32, 3);
        } else if (damageHeat >= 20) {
            // mekwarrior takes 2 damage
            return new LifeSupportHeat(20, 2);
        } else if (damageHeat >= 15 && torsoMountedCockpit) {
            return new LifeSupportHeat(15, 2);
        } else if (damageHeat >=1 && torsoMountedCockpit) {
            return new LifeSupportHeat(1, 1);
        } else if (damageHeat >= 10) {
            // mekwarrior takes 1 damage
            return new LifeSupportHeat(10, 1);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * Ammo explosions from heat Core p.107
     */
    @Nullable
    @Override
    public CriticalSlot explodeAmmo(ArrayList<CriticalSlot> ammoCriticals) {
        CriticalSlot returnSlot = null;
        int damage = 0;
        int rack = 0;

        if (ammoCriticals.isEmpty()) {
            return null;
        }

        for (CriticalSlot cs : ammoCriticals) {
            Mounted<?> mounted = cs.getMount();
            AmmoType ammoType = (AmmoType) mounted.getType();

            // TW page 160, compare one rack's
            // damage. Ties go to most rounds.
            int newRack = ammoType.getDamagePerShot() * ammoType.getRackSize();
            int newDamage = mounted.getExplosionDamage();

            Mounted<?> mount2 = cs.getMount2();
            if ((mount2 != null) && (mount2.getType() instanceof AmmoType) && (mount2.getHittableShotsLeft() > 0)) {
                // must be for same weaponType, so rackSize stays
                ammoType = (AmmoType) mount2.getType();
                newRack += ammoType.getDamagePerShot() * ammoType.getRackSize();
                newDamage += mount2.getExplosionDamage();
            }

            if (cs.isHit() || mounted.isHit()) {
                // If it is already hit, do nothing
                continue;
            }

            if (newRack > rack) {
                rack = newRack;
                damage = newDamage;
                returnSlot = cs;
                continue;
            }
            if (newRack < rack) {
                continue;
            }
            // Assume the same rack size now.
            if (newDamage > damage) {
                damage = newDamage;
                returnSlot = cs;
            }
        }

        return returnSlot;
    }
}

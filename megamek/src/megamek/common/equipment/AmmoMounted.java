/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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


package megamek.common.equipment;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import megamek.common.AmmoType;
import megamek.common.BombType;
import megamek.common.BombType.BombTypeEnum;
import megamek.common.Entity;
import megamek.common.HandheldWeapon;
import megamek.common.Mounted;
import megamek.common.WeaponType;

public class AmmoMounted extends Mounted<AmmoType> {

    public AmmoMounted(Entity entity, AmmoType type) {
        super(entity, type);

        setShotsLeft(type.getShots());
        setSize(type.getTonnage(entity));
    }

    /**
     * Change the type of ammo in this bin
     *
     * @param at The new ammo type
     */
    public void changeAmmoType(AmmoType at) {
        setType(at);
        if (getLocation() == Entity.LOC_NONE) {
            // Oneshot launcher
            setShotsLeft(1);
        } else {
            // Regular launcher
            setShotsLeft(at.getShots());
        }
    }

    @Override
    public int getExplosionDamage() {
        int rackSize = getType().getRackSize();
        int damagePerShot = getType().getDamagePerShot();
        // Anti-ship EW bomb does no damage but deals a 5-point explosion if LAM bomb bay is hit
        if ((getType() instanceof BombType)
              && (((BombType) getType()).getBombType() == BombTypeEnum.ASEW)) {
            damagePerShot = 5;
        }

        //Capital missiles need a racksize for this
        if (getType().hasFlag(AmmoType.F_CAP_MISSILE)) {
            rackSize = 1;
        }

        //Screen launchers need a racksize. Damage is 15 per TW p251
        if (getType().getAmmoType() == AmmoType.AmmoTypeEnum.SCREEN_LAUNCHER) {
            rackSize = 1;
            damagePerShot = 15;
        }

        EnumSet<AmmoType.Munitions> mType = getType().getMunitionType();
        // both Dead-Fire and Tandem-charge SRM's do 3 points of damage per
        // shot when critted
        // Dead-Fire LRM's do 2 points of damage per shot when critted.
        if ((mType.contains(AmmoType.Munitions.M_DEAD_FIRE))
              || (mType.contains(AmmoType.Munitions.M_TANDEM_CHARGE))) {
            damagePerShot++;
        } else if (getType().getAmmoType() == AmmoType.AmmoTypeEnum.TASER) {
            damagePerShot = 6;
        }

        if (getType().getAmmoType() == AmmoType.AmmoTypeEnum.MEK_MORTAR) {
            if ((mType.contains(AmmoType.Munitions.M_AIRBURST))
                  || (mType.contains(AmmoType.Munitions.M_FLARE))
                  || (mType.contains(AmmoType.Munitions.M_SMOKE_WARHEAD))) {
                damagePerShot = 1;
            } else {
                damagePerShot = 2;
            }
        }

        return damagePerShot * rackSize * getBaseShotsLeft();
    }

    /**
     * Sets the capacity of the ammo bin. Used for units that allocate by shot rather than by ton.
     *
     * @param capacity The capacity of the ammo bin in tons.
     */
    public void setAmmoCapacity(double capacity) {
        // alias for setSize
        setSize(capacity);
    }

    @Override
    public boolean isOneShot() {
        if (getLinkedBy() != null) {
            // There should not be any circular references, but we should track where we've been just in case.
            // Do a couple checks first to avoid instantiating a set unnecessarily.
            Set<Mounted<?>> checked = new HashSet<>();
            for (Mounted<?> current = getLinkedBy(); current != null; current = current.getLinkedBy()) {
                if (checked.contains(current)) {
                    return false;
                }
                if ((current.getType() instanceof WeaponType) && current.isOneShot()) {
                    return true;
                }
                checked.add(current);
            }
        }
        return false;
    }

    @Override
    public boolean isOneShotAmmo() {
        return isOneShot();
    }

    @Override
    public boolean isGroundBomb() {
        return getType().hasFlag(AmmoType.F_GROUND_BOMB);
    }

    @Override
    public double getTonnage() {
        if (getEntity() instanceof HandheldWeapon) {
            return getType().getKgPerShot() * getSize() / 1000.0;
        } else {
            return super.getTonnage();
        }
    }
}

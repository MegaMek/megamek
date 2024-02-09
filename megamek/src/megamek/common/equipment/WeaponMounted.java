/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */

package megamek.common.equipment;

import megamek.common.*;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.gaussrifles.GaussWeapon;

public class WeaponMounted extends Mounted<WeaponType> {

    public WeaponMounted(Entity entity, WeaponType type) {
        super(entity, type);
    }

    @Override
    public int getExplosionDamage() {
        // TacOps Gauss Weapon rule p. 102
        if ((getType() instanceof GaussWeapon) && getType().hasModes()
                && curMode().equals("Powered Down")) {
            return 0;
        }
        if ((isHotLoaded() || hasQuirk(OptionsConstants.QUIRK_WEAP_NEG_AMMO_FEED_PROBLEMS))
                && (getLinked() != null) && (getLinked().getUsableShotsLeft() > 0)) {
            Mounted<?> link = getLinked();
            AmmoType atype = ((AmmoType) link.getType());
            int damagePerShot = atype.getDamagePerShot();
            // Launchers with Dead-Fire missiles in them do an extra point of
            // damage per shot when critted
            if (atype.getMunitionType().contains(AmmoType.Munitions.M_DEAD_FIRE)) {
                damagePerShot++;
            }

            return getType().getRackSize() * damagePerShot;
        }

        if (getType().hasFlag(WeaponType.F_PPC) && (hasChargedCapacitor() != 0)) {
            if (isFired()) {
                if (hasChargedCapacitor() == 2) {
                    return 15;
                }
                return 0;
            }
            if (hasChargedCapacitor() == 2) {
                return 30;
            }
            return 15;
        }

        if ((getType().getAmmoType() == AmmoType.T_MPOD) && isFired()) {
            return 0;
        }

        return getType().getExplosionDamage();
    }

    public int getCurrentHeat() {
        int heat = getType().getHeat();

        // AR10's have heat based upon the loaded missile
        if (getType().getName().equals("AR10")) {
            AmmoType ammoType = (AmmoType) getLinked().getType();
            if (ammoType.hasFlag(AmmoType.F_AR10_BARRACUDA)) {
                return 10;
            } else if (ammoType.hasFlag(AmmoType.F_AR10_WHITE_SHARK)) {
                return 15;
            } else { // AmmoType.F_AR10_KILLER_WHALE
                return 20;
            }
        }

        if (getType().hasFlag(WeaponType.F_ENERGY) && getType().hasModes()) {
            heat = Compute.dialDownHeat(this, getType());
        }
        // multiply by number of shots and number of weapons
        heat = heat * getCurrentShots() * getNWeapons();
        if (hasQuirk(OptionsConstants.QUIRK_WEAP_POS_IMP_COOLING)) {
            heat = Math.max(1, heat - 1);
        }
        if (hasQuirk(OptionsConstants.QUIRK_WEAP_NEG_POOR_COOLING)) {
            heat += 1;
        }
        if (hasQuirk(OptionsConstants.QUIRK_WEAP_NEG_NO_COOLING)) {
            heat += 2;
        }
        if (hasChargedCapacitor() == 2) {
            heat += 10;
        }
        if (hasChargedCapacitor() == 1) {
            heat += 5;
        }
        if ((getLinkedBy() != null)
                && !getLinkedBy().isInoperable()
                && (getLinkedBy().getType() instanceof MiscType)
                && getLinkedBy().getType().hasFlag(
                MiscType.F_LASER_INSULATOR)) {
            heat -= 1;
            if (heat == 0) {
                heat++;
            }
        }

        return heat;
    }

    @Override
    public boolean isOneShot() {
        return getType().hasFlag(WeaponType.F_ONESHOT);
    }
}

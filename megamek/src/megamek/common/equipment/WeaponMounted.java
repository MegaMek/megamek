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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WeaponMounted extends Mounted<WeaponType> {

    // A list of ids (equipment numbers) for the weapons and ammo linked to
    // this bay (if the mounted is of the BayWeapon type)
    // I can also use this for weapons of the same type on a capital fighter
    // and now Machine Gun Arrays too!
    private List<Integer> bayWeapons = new ArrayList<>();
    private List<Integer> bayAmmo = new ArrayList<>();

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

    /**
     * @return The ammo currently linked by this weapon, or null if there is no linked ammo.
     */
    public AmmoMounted getLinkedAmmo() {
        return (getLinked() instanceof AmmoMounted) ? (AmmoMounted) getLinked() : null;
    }

    /**
     * Returns how many shots the weapon is using
     */
    public int getCurrentShots() {
        WeaponType wtype = (WeaponType) getType();
        int nShots = getNumShots(wtype, curMode(), false);
        // sets number of shots for MG arrays
        if (wtype.hasFlag(WeaponType.F_MGA)) {
            nShots = 0;
            for (int eqn : getBayWeapons()) {
                Mounted<?> m = getEntity().getEquipment(eqn);
                if (null == m) {
                    continue;
                }
                if ((m.getLocation() == getLocation())
                        && !m.isDestroyed()
                        && !m.isBreached()
                        && m.getType().hasFlag(WeaponType.F_MG)
                        && (((WeaponType) m.getType()).getRackSize() == ((WeaponType) getType())
                        .getRackSize())) {
                    nShots++;
                }
            }
        }
        return nShots;
    }

    @Override
    public boolean isOneShot() {
        return getType().hasFlag(WeaponType.F_ONESHOT);
    }

    public void addWeaponToBay(int w) {
        bayWeapons.add(w);
    }

    @Override
    public List<Integer> getBayWeapons() {
        return bayWeapons;
    }

    public void addAmmoToBay(int a) {
        bayAmmo.add(a);
    }

    @Override
    public List<Integer> getBayAmmo() {
        return bayAmmo;
    }

    /**
     *
     * @param mAmmoId equipment number of ammo
     * @return        whether the ammo is in this weapon's bay
     */
    public boolean ammoInBay(int mAmmoId) {
        for (int nextAmmoId : bayAmmo) {
            if (nextAmmoId == mAmmoId) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected List<String> bayComponentsToString() {
        List<String> list = new ArrayList<>();
        if (!bayWeapons.isEmpty()) {
            List<String> bayWeaponIds = bayWeapons.stream().map(id -> "[" + id + "]").collect(Collectors.toList());
            list.add("Bay Weapons: " + String.join(", ", bayWeaponIds));
        }
        if (!bayAmmo.isEmpty()) {
            List<String> bayAmmoIds = bayAmmo.stream().map(id -> "[" + id + "]").collect(Collectors.toList());
            list.add("Bay Ammo: " + String.join(", ", bayAmmoIds));
        }
        return list;
    }
}

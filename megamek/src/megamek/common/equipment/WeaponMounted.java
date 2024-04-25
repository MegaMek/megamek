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
import megamek.common.actions.WeaponAttackAction;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.WeaponHandler;
import megamek.common.weapons.gaussrifles.GaussWeapon;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Vector;
import java.util.stream.Collectors;

public class WeaponMounted extends Mounted<WeaponType> {

    // A list of ids (equipment numbers) for the weapons and ammo linked to
    // this bay (if the mounted is of the BayWeapon type)
    // I can also use this for weapons of the same type on a capital fighter
    // and now Machine Gun Arrays too!
    private final List<Integer> bayWeapons = new ArrayList<>();
    private final List<Integer> bayAmmo = new ArrayList<>();

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

    @Override
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
        int nShots = getNumShots(getType(), curMode(), false);
        // sets number of shots for MG arrays
        if (getType().hasFlag(WeaponType.F_MGA)) {
            nShots = 0;
            for (WeaponMounted m : getBayWeapons()) {
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

    /**
     * Adds the weapon with the given equipment number to the bay.
     * @param equipmentNum The equipment number of the weapon to add
     * @see Entity#getEquipmentNum(Mounted)
     */
    public void addWeaponToBay(int equipmentNum) {
        bayWeapons.add(equipmentNum);
    }

    public void addWeaponToBay(WeaponMounted weapon) {
        addWeaponToBay(weapon.getEquipmentNum());
    }

    /**
     * Removes the weapon with the given equipment number from the bay.
     * @param equipmentNum The equipment number of the weapon to remove.
     * @see Entity#getEquipmentNum(Mounted)
     */
    public void removeWeaponFromBay(int equipmentNum) {
        bayWeapons.remove(Integer.valueOf(equipmentNum));
    }

    /**
     * Removes a weapon from the bay.
     * @param weapon The weapon to remove.
     */
    public void removeWeaponFromBay(WeaponMounted weapon) {
        removeWeaponFromBay(weapon.getEquipmentNum());
    }

    /**
     * Removes all weapons from the bay.
     */
    public void clearBayWeapons() {
        bayWeapons.clear();
    }

    /**
     * @return All the weapon mounts in the bay.
     */
    public List<WeaponMounted> getBayWeapons() {
        return bayWeapons.stream()
                .map(i -> getEntity().getWeapon(i))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Fetches the bay weapon at a given index in the bay weapons list.
     * @param index The index
     * @return      The weapon mount at that
     */
    public WeaponMounted getBayWeapon(int index) {
        if ((index >= 0) && (index < bayWeapons.size())) {
            return getEntity().getWeapon(index);
        } else {
            return null;
        }
    }

    /**
     * Adds the ammo with the given equipment number to the bay.
     * @param equipmentNum The equipment number of the ammo
     * @see Entity#getEquipmentNum(Mounted)
     */
    public void addAmmoToBay(int equipmentNum) {
        bayAmmo.add(equipmentNum);
    }

    public void addAmmoToBay(AmmoMounted ammo) {
        bayAmmo.add(ammo.getEquipmentNum());
    }

    /**
     * Removes the ammo with the given equipment number from the bay.
     * @param equipmentNum The equipment number of the ammo.
     * @see Entity#getEquipmentNum(Mounted)
     */
    public void removeAmmoFromBay(int equipmentNum) {
        bayAmmo.remove(Integer.valueOf(equipmentNum));
    }

    /**
     * Clears all ammo from the bay.
     */
    public void clearBayAmmo() {
        bayAmmo.clear();
    }

    public List<AmmoMounted> getBayAmmo() {
        return bayWeapons.stream()
                .map(i -> getEntity().getAmmo(i))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     *
     * @param mAmmoId equipment number of ammo
     * @return        whether the ammo is in this weapon's bay
     */
    public boolean ammoInBay(int mAmmoId) {
        return bayAmmo.contains(mAmmoId);
    }

    /**
     * Removes the weapon or ammo with the given equipment number from the bay.
     * @param equipmentNum The weapon or ammo equipment number
     * @see Entity#getEquipmentNum(Mounted)
     */
    public void removeFromBay(int equipmentNum) {
        bayWeapons.remove(Integer.valueOf(equipmentNum));
        bayAmmo.remove(Integer.valueOf(equipmentNum));
    }

    /**
     * Removes the weapon or ammo from the bay.
     * @param mounted The weapon or ammo to remove.
     */
    public void removeFromBay(Mounted<?> mounted) {
        removeFromBay(mounted.getEquipmentNum());
    }

    /**
     * Checks whether the bay contains the given weapon or ammo.
     * @param equipmentNum The equipment number of the weapon or ammo.
     * @return Whether the bay contains the equipment.
     * @see Entity#getEquipmentNum(Mounted)
     */
    public boolean bayContains(int equipmentNum) {
        return bayWeapons.contains(equipmentNum) || bayAmmo.contains(equipmentNum);
    }

    /**
     * Checks whether the bay contains the given weapon or ammo.
     * @param mounted The weapon or ammo.
     * @return Wehther the bay contains the equipment.
     */
    public boolean bayContains(Mounted<?> mounted) {
        return bayContains(mounted.getEquipmentNum());
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


    /**
     * Assign APDS systems to the most dangerous incoming missile attacks. This
     * should only be called once per turn, or AMS will get extra attacks
     */
    public WeaponAttackAction assignAPDS(List<WeaponHandler> vAttacks) {
        // Shouldn't have null entity, but if we do...
        if (getEntity() == null) {
            return null;
        }

        // Ensure we only target attacks in our arc & range
        List<WeaponAttackAction> vAttacksInArc = new Vector<>(vAttacks.size());
        for (WeaponHandler wr : vAttacks) {
            boolean isInArc = Compute.isInArc(getEntity().getGame(),
                    getEntity().getId(), getEntity().getEquipmentNum(this),
                    getEntity().getGame().getEntity(wr.waa.getEntityId()));
            boolean isInRange = getEntity().getPosition().distance(
                    wr.getWaa().getTarget(getEntity().getGame()).getPosition()) <= 3;
            if (isInArc && isInRange) {
                vAttacksInArc.add(wr.waa);
            }
        }
        // find the most dangerous salvo by expected damage
        WeaponAttackAction waa = Compute.getHighestExpectedDamage(getEntity()
                .getGame(), vAttacksInArc, true);
        if (waa != null) {
            waa.addCounterEquipment(this);
            return waa;
        }
        return null;
    }

    /**
     * @return Whether this mount is an advanced point defense system.
     */
    public boolean isAPDS() {
        if ((getEntity() instanceof BattleArmor)
                && getType().getInternalName().equals("ISBAAPDS")) {
            return true;
        } else {
            return getType().getAmmoType() == AmmoType.T_APDS;
        }
    }
}

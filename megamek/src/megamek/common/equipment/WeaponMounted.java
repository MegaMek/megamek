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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Vector;
import java.util.stream.Collectors;

import megamek.common.actions.WeaponAttackAction;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.compute.Compute;
import megamek.common.compute.ComputeArc;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.weapons.gaussRifles.GaussWeapon;
import megamek.common.weapons.handlers.WeaponHandler;

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
        if ((isHotLoaded() || hasQuirk(OptionsConstants.QUIRK_WEAPON_NEG_AMMO_FEED_PROBLEMS))
              && (getLinked() != null) && (getLinked().getUsableShotsLeft() > 0)) {
            Mounted<?> link = getLinked();
            AmmoType ammoType = ((AmmoType) link.getType());
            int damagePerShot = ammoType.getDamagePerShot();
            // Launchers with Dead-Fire missiles in them do an extra point of
            // damage per shot when critted
            if (ammoType.getMunitionType().contains(AmmoType.Munitions.M_DEAD_FIRE)) {
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

        if ((getType().getAmmoType() == AmmoType.AmmoTypeEnum.MPOD) && isFired()) {
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

        // Apply Gothic Dazzle Mode heat reduction
        if (curMode().getName().contains("Dazzle")) {
            // Half heat (rounded down, min 0)
            heat = Math.max(0, heat / 2);
        }

        // multiply by number of shots and number of weapons
        heat = heat * getCurrentShots() * getNWeapons();
        if (hasQuirk(OptionsConstants.QUIRK_WEAPON_POS_IMP_COOLING)) {
            heat = Math.max(1, heat - 1);
        }
        if (hasQuirk(OptionsConstants.QUIRK_WEAPON_NEG_POOR_COOLING)) {
            heat += 1;
        }
        if (hasQuirk(OptionsConstants.QUIRK_WEAPON_NEG_NO_COOLING)) {
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
        if (curMode().getName().startsWith("Pulse")) {
            heat += 2;
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
                      && (m.getType().getRackSize() == getType()
                      .getRackSize())) {
                    nShots++;
                }
            }
        }
        return nShots;
    }

    @Override
    public boolean isOneShot() {
        return getType().hasFlag(WeaponType.F_ONE_SHOT);
    }

    /**
     * Adds the weapon with the given equipment number to the bay.
     *
     * @param equipmentNum The equipment number of the weapon to add
     *
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
     *
     * @param equipmentNum The equipment number of the weapon to remove.
     *
     * @see Entity#getEquipmentNum(Mounted)
     */
    public void removeWeaponFromBay(int equipmentNum) {
        bayWeapons.remove(Integer.valueOf(equipmentNum));
    }

    /**
     * Removes a weapon from the bay.
     *
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
     *
     * @param index The index
     *
     * @return The weapon mount at that
     */
    public WeaponMounted getBayWeapon(int index) {
        if ((index >= 0) && (index < bayWeapons.size())) {
            return getEntity().getWeapon(bayWeapons.get(index));
        } else {
            return null;
        }
    }

    /**
     * Adds the ammo with the given equipment number to the bay.
     *
     * @param equipmentNum The equipment number of the ammo
     *
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
     *
     * @param equipmentNum The equipment number of the ammo.
     *
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
        return bayAmmo.stream()
              .map(i -> getEntity().getAmmo(i))
              .filter(Objects::nonNull)
              .collect(Collectors.toList());
    }

    /**
     * @param mAmmoId equipment number of ammo
     *
     * @return whether the ammo is in this weapon's bay
     */
    public boolean ammoInBay(int mAmmoId) {
        return bayAmmo.contains(mAmmoId);
    }

    /**
     * Removes the weapon or ammo with the given equipment number from the bay.
     *
     * @param equipmentNum The weapon or ammo equipment number
     *
     * @see Entity#getEquipmentNum(Mounted)
     */
    public void removeFromBay(int equipmentNum) {
        bayWeapons.remove(Integer.valueOf(equipmentNum));
        bayAmmo.remove(Integer.valueOf(equipmentNum));
    }

    /**
     * Removes the weapon or ammo from the bay.
     *
     * @param mounted The weapon or ammo to remove.
     */
    public void removeFromBay(Mounted<?> mounted) {
        removeFromBay(mounted.getEquipmentNum());
    }

    /**
     * Checks whether the bay contains the given weapon or ammo.
     *
     * @param equipmentNum The equipment number of the weapon or ammo.
     *
     * @return Whether the bay contains the equipment.
     *
     * @see Entity#getEquipmentNum(Mounted)
     */
    public boolean bayContains(int equipmentNum) {
        return bayWeapons.contains(equipmentNum) || bayAmmo.contains(equipmentNum);
    }

    /**
     * Checks whether the bay contains the given weapon or ammo.
     *
     * @param mounted The weapon or ammo.
     *
     * @return Whether the bay contains the equipment.
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
     * Assign APDS systems to the most dangerous incoming missile attacks. This should only be called once per turn, or
     * AMS will get extra attacks
     */
    public WeaponAttackAction assignAPDS(List<WeaponHandler> vAttacks) {
        // Shouldn't have null entity, but if we do...
        if (getEntity() == null) {
            return null;
        }

        // Ensure we only target attacks in our arc & range
        List<WeaponAttackAction> vAttacksInArc = new Vector<>(vAttacks.size());
        for (WeaponHandler wr : vAttacks) {
            boolean isInArc = ComputeArc.isInArc(getEntity().getGame(),
                  getEntity().getId(), getEntity().getEquipmentNum(this),
                  getEntity().getGame().getEntity(wr.weaponAttackAction.getEntityId()));
            boolean isInRange = getEntity().getPosition().distance(
                  wr.getWeaponAttackAction().getTarget(getEntity().getGame()).getPosition()) <= 3;
            if (isInArc && isInRange) {
                vAttacksInArc.add(wr.weaponAttackAction);
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
            return getType().getAmmoType() == AmmoType.AmmoTypeEnum.APDS;
        }
    }

    /**
     * @return The heat generated by the bay, or the heat of the individual weapon if it's not a bay
     */
    public int getHeatByBay() {
        int heat = 0;
        if (!getBayWeapons().isEmpty()) {
            heat = getBayWeapons().stream().mapToInt(WeaponMounted::getCurrentHeat).sum();
        } else {
            heat += getCurrentHeat();
        }
        return heat;
    }

    @Override
    public boolean isGroundBomb() {
        return getType().hasFlag(WeaponType.F_DIVE_BOMB) || getType().hasFlag(WeaponType.F_ALT_BOMB);
    }
}

/*
 * Copyright (C) 2024-2026 The MegaMek Team. All Rights Reserved.
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
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.units.AbstractBuildingEntity;
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

    /**
     * {@code true} when this mount is a unit's one-shot Disposable Weapon (TO:AuE p.116, Corrected Sixth Printing)
     * rather than the same weapon type fired as a normal platoon/secondary weapon. Used to route the attack to
     * {@code InfantryDisposableWeaponHandler} and to present the once-per-scenario disposable attack option.
     */
    private boolean disposableWeapon = false;

    public WeaponMounted(Entity entity, WeaponType type) {
        super(entity, type);
    }

    /**
     * @return {@code true} if this mount is a unit's one-shot Disposable Weapon (TO:AuE p.116, Corrected Sixth
     *       Printing)
     */
    public boolean isDisposableWeapon() {
        return disposableWeapon;
    }

    /**
     * Marks this mount as a one-shot Disposable Weapon (TO:AuE p.116, Corrected Sixth Printing).
     *
     * @param disposableWeapon whether this mount is a Disposable Weapon
     */
    public void setDisposableWeapon(boolean disposableWeapon) {
        this.disposableWeapon = disposableWeapon;
    }

    /**
     * Whether this weapon can jam at all, which is what decides whether a gamemaster is offered a Jammed switch
     * for it. The list is drawn from the attack handlers that set a jam: the autocannon family (standard, light,
     * improved, ProtoMek, ultra, rotary and hyper-velocity; a standard autocannon jams in rapid-fire mode or on
     * caseless ammo), the prototype gauss rifle and prototype LB-X autocannon, any ammo-fed weapon with the Ammo
     * Feed Problems quirk, and every weapon of an Advanced Building (TO:AR p.119). Missiles, lasers and
     * production gauss rifles have no jam path, so a jam on them would be a state nothing in play can produce.
     *
     * @return {@code true} if some rule can jam this weapon
     */
    public boolean canJam() {
        if (getEntity() instanceof AbstractBuildingEntity) {
            return true;
        }
        WeaponType weaponType = getType();
        boolean isAmmoFed = weaponType.getAmmoType() != AmmoType.AmmoTypeEnum.NA;
        if (isAmmoFed && hasQuirk(OptionsConstants.QUIRK_WEAPON_NEG_AMMO_FEED_PROBLEMS)) {
            return true;
        }
        boolean isPrototype = weaponType.hasFlag(WeaponType.F_PROTOTYPE);
        return switch (weaponType.getAmmoType()) {
            case AC, LAC, AC_IMP, PAC, AC_ULTRA, AC_ULTRA_THB, AC_ROTARY, HYPER_VELOCITY -> true;
            case GAUSS, AC_LBX -> isPrototype;
            default -> false;
        };
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

        return Game.rulesManager.getRulesExplosions().equipmentDamage(this, getType());
    }

    /**
     * @return This weapon's Attack Value at the given range bracket, with the 5 of a PPC capacitor that fires its
     *       charge (a capacitor in a weapon bay counts as charged, TO:AUE p.149), or 0 out of range
     */
    public double getAttackValue(int range) {
        double attackValue = switch (range) {
            case WeaponType.RANGE_SHORT -> getType().getShortAV();
            case WeaponType.RANGE_MED -> getType().getMedAV();
            case WeaponType.RANGE_LONG -> getType().getLongAV();
            case WeaponType.RANGE_EXT -> getType().getExtAV();
            default -> 0;
        };
        return ((attackValue > 0) && firesCapacitorCharge()) ? (attackValue + CAPACITOR_DAMAGE_BONUS) : attackValue;
    }

    @Override
    public int getCurrentHeat() {
        int heat = getType().getHeat();

        // The AR10's heat depends on the loaded missile (SO:AA p. 104, TW p. 304).
        if ((getType().getAmmoType() == AmmoType.AmmoTypeEnum.AR10) && (getLinked() instanceof AmmoMounted ammo)) {
            if (ammo.getType().hasFlag(AmmoType.F_AR10_BARRACUDA)) {
                heat = 10;
            } else if (ammo.getType().hasFlag(AmmoType.F_AR10_WHITE_SHARK)) {
                heat = 15;
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
        if (firesCapacitorCharge()) {
            heat += 5;
        }
        return (int) laserEnhancedHeat(heat, curMode().getName().startsWith("Pulse"));
    }

    /**
     * @return The heat of this weapon with its laser enhancements: 2 more in the pulse mode of a RISC laser pulse
     *       module (IO:AE p. 87), then, with a working laser insulator, 1 less to a minimum of 1 (TO:AUE p. 134)
     *
     * @param heat      The heat before the enhancements
     * @param pulseMode True for the heat of the pulse mode
     */
    public double laserEnhancedHeat(double heat, boolean pulseMode) {
        MiscMounted pulseModule = pulseMode ? getEnhancement(MiscType.F_RISC_LASER_PULSE_MODULE) : null;
        double enhancedHeat = (pulseModule != null) ? (heat + pulseModule.getType().getHeat()) : heat;
        Mounted<?> insulator = getEnhancement(MiscType.F_LASER_INSULATOR);
        return ((insulator != null) && !insulator.isInoperable()) ? Math.max(1, enhancedHeat - 1) : enhancedHeat;
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
    @Deprecated(since = "0.51.0", forRemoval = true)
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
    @Deprecated(since = "0.51.0", forRemoval = true)
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
    @Deprecated(since = "0.51.0", forRemoval = true)
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

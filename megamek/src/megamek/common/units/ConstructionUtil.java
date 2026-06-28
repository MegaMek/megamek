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
package megamek.common.units;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import megamek.common.CriticalSlot;
import megamek.common.annotations.Nullable;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.loaders.MekFileParser;

/**
 * Helper methods for constructing/changing units, including adding, moving and removing equipment. This happens mostly
 * in MML but some units have modular equipment which MM must be able to switch out and doing this is involved and
 * error-prone and should be unified. Unfortunately, UnitUtil cannot simply be copied over to MM as it accesses too many
 * other MML classes.
 * <p>
 * This class contains methods that apply to all or multiple unit types. Type specific methods should be placed in their
 * own construction util classes, such as BaConstructionUtil.
 */
public class ConstructionUtil {

    /**
     * Removes a piece of equipment from the given unit. "Remove" means that it is deleted entirely, not just
     * unallocated. Attached equipment, e.g. weapons in a BA AP mount, will become unattached. Note that, in MM, some
     * previously linked equipment should be dealt with (removed) as well to avoid errors.
     *
     * @param unit  The entity
     * @param mount The equipment
     */
    public static void removeMounted(Entity unit, Mounted<?> mount) {
        Objects.requireNonNull(unit);
        Objects.requireNonNull(mount);

        removeCriticalSlots(unit, mount);

        if (unit instanceof BattleArmor battleArmor) {
            // DWP and APM require special treatment
            BaConstructionUtil.unallocateMounted(battleArmor, mount);
        }

        // We will need to reset the equipment numbers of the bay ammo and weapons
        Map<WeaponMounted, List<WeaponMounted>> bayWeapons = new HashMap<>();
        Map<WeaponMounted, List<AmmoMounted>> bayAmmo = new HashMap<>();
        for (WeaponMounted bay : unit.getWeaponBayList()) {
            bayWeapons.put(bay, bay.getBayWeapons());
            bayAmmo.put(bay, bay.getBayAmmo());
        }

        // Some special checks for Aeros
        if (unit instanceof Aero) {
            if (mount instanceof WeaponMounted) {
                // Aeros have additional weapon lists that need to be cleared
                unit.getTotalWeaponList().remove(mount);
                unit.getWeaponBayList().remove(mount);
                unit.getWeaponGroupList().remove(mount);
            }
        }

        unit.getEquipment().remove(mount);

        switch (mount) {
            case MiscMounted ignored -> unit.getMisc().remove(mount);
            case AmmoMounted ignored -> unit.getAmmo().remove(mount);
            case WeaponMounted ignored -> {
                unit.getWeaponList().remove(mount);
                unit.getTotalWeaponList().remove(mount);
            }
            default -> {
            }
        }

        if (mount instanceof WeaponMounted && bayWeapons.containsKey(mount)) {
            bayWeapons.get(mount).forEach(w -> {
                removeCriticalSlots(unit, w);
                changeMountStatus(unit, w, Entity.LOC_NONE, Entity.LOC_NONE, false);
            });
            bayAmmo.get(mount).forEach(a -> {
                removeCriticalSlots(unit, a);
                Mounted<?> moveTo = findUnallocatedAmmo(unit, a.getType());

                if (null != moveTo) {
                    moveTo.setShotsLeft(moveTo.getBaseShotsLeft() + a.getBaseShotsLeft());
                    removeMounted(unit, a);
                }

                changeMountStatus(unit, a, Entity.LOC_NONE, Entity.LOC_NONE, false);
            });
            bayWeapons.remove(mount);
            bayAmmo.remove(mount);
        }

        for (WeaponMounted bay : bayWeapons.keySet()) {
            bay.clearBayWeapons();
            for (WeaponMounted w : bayWeapons.get(bay)) {
                if (mount != w) {
                    bay.addWeaponToBay(w);
                }
            }
        }

        for (WeaponMounted bay : bayAmmo.keySet()) {
            bay.clearBayAmmo();
            for (AmmoMounted a : bayAmmo.get(bay)) {
                if (mount != a) {
                    bay.addAmmoToBay(a);
                }
            }
        }

        // Remove ammo added for a one-shot launcher
        if ((mount.getType() instanceof WeaponType) && mount.isOneShot()) {
            List<AmmoMounted> osAmmo = new ArrayList<>();
            for (AmmoMounted ammo = (AmmoMounted) mount.getLinked();
                  ammo != null;
                  ammo = (AmmoMounted) ammo.getLinked()) {
                osAmmo.add(ammo);
            }
            osAmmo.forEach(m -> {
                unit.getEquipment().remove(m);
                unit.getAmmo().remove(m);
            });
        }

        // It's possible that the equipment we are removing was linked to something else, and so the linkedBy state may
        // be set. We should remove it. Using getLinked could be unreliable, so we'll brute force it. An example of this
        // would be removing a linked Artemis IV FCS
        for (Mounted<?> m : unit.getEquipment()) {
            if (mount.equals(m.getLinkedBy())) {
                m.setLinkedBy(null);
            }
        }

        if ((mount.getType() instanceof MiscType) && (mount.getType().hasFlag(MiscType.F_HEAD_TURRET) || mount.getType()
              .hasFlag(MiscType.F_SHOULDER_TURRET) || mount.getType().hasFlag(MiscType.F_QUAD_TURRET))) {
            for (Mounted<?> m : unit.getEquipment()) {
                if (m.getLocation() == mount.getLocation()) {
                    m.setMekTurretMounted(false);
                }
            }
        }

        if ((mount.getType() instanceof MiscType) && mount.getType().hasFlag(MiscType.F_SPONSON_TURRET)) {
            for (Mounted<?> m : unit.getEquipment()) {
                m.setSponsonTurretMounted(false);
            }
        }

        if ((mount.getType() instanceof MiscType) && mount.getType().hasFlag(MiscType.F_PINTLE_TURRET)) {
            for (Mounted<?> m : unit.getEquipment()) {
                if (m.getLocation() == mount.getLocation()) {
                    m.setPintleTurretMounted(false);
                }
            }
        }
        unit.recalculateTechAdvancement();
    }

    /**
     * Sets the corresponding critical slots to null for the Mounted object. All crit slots of the unit are checked for
     * any presence of that Mounted (object equality!) and emptied where found. When mounted is null, this method does
     * nothing. Note that the crit slots are checked even if the mounted equipment is unallocated (in Entity.LOC_NONE)
     * or not part of the unit at all.
     *
     * @param unit    The entity
     * @param mounted The equipment to test
     */
    public static void removeCriticalSlots(Entity unit, @Nullable Mounted<?> mounted) {
        for (int loc = 0; loc < unit.locations(); loc++) {
            for (int slot = 0; slot < unit.getNumberOfCriticalSlots(loc); slot++) {
                CriticalSlot criticalSlot = unit.getCritical(loc, slot);
                if ((criticalSlot != null) && (criticalSlot.getType() == CriticalSlot.TYPE_EQUIPMENT)) {
                    if ((criticalSlot.getMount() != null) && (criticalSlot.getMount().equals(mounted))) {
                        // If there are two pieces of equipment in this slot, remove first one, and replace it with
                        // the second
                        if (criticalSlot.getMount2() != null) {
                            criticalSlot.setMount(criticalSlot.getMount2());
                            criticalSlot.setMount2(null);
                        } else {
                            // If it's the only Mounted, clear the slot
                            unit.setCritical(loc, slot, null);
                        }
                    } else if ((criticalSlot.getMount2() != null) && (criticalSlot.getMount2().equals(mounted))) {
                        criticalSlot.setMount2(null);
                    }
                }
            }
        }
    }

    /**
     * Updates the location for a Mounted equipment. If the equipment was previously in another location, links to other
     * equipment are removed. If it is placed in a location on the unit (i.e., not Entity.LOC_NONE), new links are
     * possibly created using MekFileParser.postLoadInit(). On Meks, Clan CASE placement is updated. This method does
     * *NOT* change nor create Critical Slots.
     * <p>
     * Note: for BattleArmor, this affects which suit the equipment is placed on (as that is what Mounted. Location
     * means for BA), but not where on the suit it's located (ie, BAMountLocation isn't affected). BattleArmor should
     * change this outside of this method.
     *
     * @param unit              The entity The unit being modified
     * @param eq                The equipment to test The equipment mount to move
     * @param location          The location to move the mount to
     * @param secondaryLocation The secondary location for split equipment, otherwise
     *                          {@link Entity#LOC_NONE Entity.LOC_NONE}
     * @param rear              Whether to mount with a rear facing
     */
    public static void changeMountStatus(Entity unit, Mounted<?> eq, int location, int secondaryLocation,
          boolean rear) {
        if ((location != eq.getLocation() && !eq.isOneShot())) {
            if (eq.getLinked() != null) {
                eq.getLinked().setLinkedBy(null);
                eq.setLinked(null);
            }
            if (eq.getLinkedBy() != null) {
                eq.getLinkedBy().setLinked(null);
                eq.setLinkedBy(null);
            }
        }
        eq.setLocation(location, rear);
        eq.setSecondLocation(secondaryLocation, rear);
        eq.setSplit(secondaryLocation > -1);
        // If we're adding it to a location on the unit, check equipment linkages
        if (location > Entity.LOC_NONE) {
            try {
                MekFileParser.postLoadInit(unit);
            } catch (Exception ignored) {
                // Exception thrown for not having equipment to link to yet, which is acceptable here
            }
        }
        if (unit instanceof Mek mek) {
            MekConstructionUtil.updateClanCasePlacement(mek);
        }
    }

    /**
     * Find unallocated ammo of the same type. Used by large aerospace units when removing ammo from a location to find
     * the group to add it to.
     *
     * @param unit The entity
     * @param at   The type of armor to match
     *
     * @return An unallocated non-one-shot ammo mount of the same type, or null if there is not one.
     */
    public static Mounted<?> findUnallocatedAmmo(Entity unit, EquipmentType at) {
        for (Mounted<?> m : unit.getAmmo()) {
            if ((m.getLocation() == Entity.LOC_NONE) && at.equals(m.getType()) && ((m.getLinkedBy() == null)
                  || !m.getLinkedBy().getType().hasFlag(WeaponType.F_ONE_SHOT))) {
                return m;
            }
        }
        return null;
    }


}

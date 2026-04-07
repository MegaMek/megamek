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

import megamek.common.battleArmor.BattleArmor;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.equipment.enums.MiscTypeFlag;
import megamek.logging.MMLogger;

/**
 * Helper methods for constructing/changing Battle Armor units. This happens mostly in MML but some units have modular
 * equipment which MM must be able to switch out and the code for this is involved and should be unified as much as
 * possible.
 * <p>
 * This is the MM extension of BattleArmorUtil.
 */
public class BaConstructionUtil {

    private static final MMLogger LOGGER = MMLogger.create(BaConstructionUtil.class);

    /**
     * Unallocates (removes from arm/body etc to the unallocated equipment list) the given mounted. For special mounts
     * for other equipment (DWP etc), that other equipment is removed from this mount first, emptying the given mounted.
     * This method will unallocate regardless of the type of equipment, i.e., it does not check if this equipment should
     * ever go unallocated (e.g. fixed location equipment). It is therefore up to the caller to select equipment to
     * unallocate.
     *
     * @param mounted The equipment to unallocate
     */
    public static void unallocateMounted(BattleArmor battleArmor, Mounted<?> mounted) {
        if (isFilledDwp(mounted) || isFilledApm(mounted) || isFilledArmoredGlove(mounted)) {
            emptyDwpApm(mounted);
        }
        if ((mounted.isAPMMounted() || mounted.isDWPMounted()) && mounted.getLinkedBy() != null) {
            emptyDwpApm(mounted.getLinkedBy());
        }
        mounted.setDWPMounted(false);
        mounted.setAPMMounted(false);
        mounted.setBaMountLoc(BattleArmor.MOUNT_LOC_NONE);
        ConstructionUtil.changeMountStatus(battleArmor, mounted, BattleArmor.LOC_SQUAD, BattleArmor.LOC_SQUAD, false);
    }

    /**
     * @return True when the given mounted is a Detachable Weapon Pack and it has a weapon allocated to it.
     */
    public static boolean isFilledDwp(Mounted<?> mounted) {
        return mounted.is(EquipmentTypeLookup.BA_DWP) && mounted.getLinked() != null;
    }

    /**
     * @return True when the given mounted is an Anti-Personnel weapon mount (only the misc item, not an armored glove!)
     *       and it has a weapon allocated to it.
     */
    public static boolean isFilledApm(Mounted<?> mounted) {
        return mounted.is(EquipmentTypeLookup.BA_APM) && mounted.getLinked() != null;
    }

    /**
     * @return True when the given mounted is an Armored Glove manipulator and it has an AP weapon allocated to it.
     */
    public static boolean isFilledArmoredGlove(Mounted<?> mounted) {
        return mounted.getType().hasFlag(MiscTypeFlag.F_ARMORED_GLOVE) && mounted.getLinked() != null;
    }

    /**
     * Empties the given APM (including armored glove) or DWP, removing any weapon or other equipment attached to it.
     * Can be safely called (does nothing) when there is no equipment on the given mount or the given mount is neither
     * an APM nor DWP (in this case, logs a warning).
     *
     * @param mount The APM/DWP to empty
     */
    public static void emptyDwpApm(Mounted<?> mount) {
        if (!mount.is(EquipmentTypeLookup.BA_DWP) && !mount.getType().hasFlag(MiscType.F_AP_MOUNT)) {
            LOGGER.warn("Trying to unattach equipment from something that is neither APM nor DWP!");
            return;
        }
        if (mount.getLinked() != null) {
            Mounted<?> attachedEquipment = mount.getLinked();
            attachedEquipment.setLinkedBy(null);
            attachedEquipment.setDWPMounted(false);
            attachedEquipment.setAPMMounted(false);
            mount.setLinked(null);
        }
    }

    /**
     * Mounts the given weapon on the given Anti-Personnel Weapon Mount, which may be either the misc item or an armored
     * glove. Any previously mounted weapon is removed from it, becoming unallocated. Does nothing and logs a warning
     * when the given apm Mounted is not a suitable AP mount or the given weapon may not be mounted on an APM.
     *
     * @param weapon The weapon to mount on the APM
     * @param apm    The APM to receive the weapon
     */
    public static void mountOnApm(Mounted<?> weapon, Mounted<?> apm) {
        if (!(apm instanceof MiscMounted miscMounted) || !miscMounted.getType().hasFlag(MiscType.F_AP_MOUNT)) {
            LOGGER.warn("Trying to APM-mount on an item that is not an AP mount or armored glove!");
            return;
        }
        if (!weapon.getType().hasFlag(WeaponType.F_INFANTRY)) {
            LOGGER.warn("Trying to APM-mount invalid equipment!");
            return;
        }
        emptyDwpApm(apm);
        apm.setLinked(weapon);
        weapon.setLinkedBy(apm);
        weapon.setAPMMounted(true);
//        weapon.setBaMountLoc(BattleArmor.MOUNT_LOC_NONE); // only necessary for DWP
    }
}

/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.ai.dataset;

import java.util.ArrayList;
import java.util.List;

import megamek.common.compute.Compute;
import megamek.common.units.Entity;
import megamek.common.equipment.WeaponMounted;
import megamek.logging.MMLogger;

/**
 * WeaponDataEncoder encodes weapons as a list of Integers
 *
 * @author Luana Coppio
 */
public class WeaponDataEncoder {
    private static final MMLogger logger = MMLogger.create(WeaponDataEncoder.class);

    /**
     * Encodes the weapons of an entity into a List of Integers, each weapon is encoded as a sequence of 5 integers
     * which represents the max damage it causes, its arc, short range, medium range and long range.
     *
     * @param entity The entity from which to encode the weapon data
     *
     * @return the encoded list [max damage, arc code, short range, medium range, long range]
     */
    public static List<Integer> getEncodedWeaponData(Entity entity) {
        List<Integer> weaponData = new ArrayList<>();
        entity.getWeaponListWithHHW().forEach(weapon -> serializeWeaponData(weapon, entity, weaponData));
        return weaponData;
    }

    private static void serializeWeaponData(WeaponMounted weapon, Entity entity, List<Integer> weaponData) {
        try {
            Entity weaponEntity = weapon.getEntity();
            int equipmentId = weaponEntity.getEquipmentNum(weapon);
            var mounted = weaponEntity.getEquipment(equipmentId);
            if (mounted == null) {
                logger.warn("No such equipment {} [{}] for {}", weapon, equipmentId, entity);
                return;
            }

            int arc = weaponEntity.getWeaponArc(equipmentId);
            int shortRange = weapon.getType().getShortRange();
            int mediumRange = weapon.getType().getMediumRange();
            int longRange = weapon.getType().getLongRange();

            int damage = Compute.computeTotalDamage(weapon);
            weaponData.add(damage);
            weaponData.add(arc);
            weaponData.add(shortRange);
            weaponData.add(mediumRange);
            weaponData.add(longRange);
        } catch (Exception e) {
            logger.error(e, "Error while trying to serialize Weapon {} data for {}", weapon, entity);
            // Error, log this instead
            weaponData.add(-1);
            weaponData.add(-1);
            weaponData.add(-1);
            weaponData.add(-1);
            weaponData.add(-1);
        }
    }
}

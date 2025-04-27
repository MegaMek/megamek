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

import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.equipment.WeaponMounted;
import megamek.logging.MMLogger;

/**
 * WeaponDataEncoder encodes weapons as a list of Integers
 * @author Luana Coppio
 */
public class WeaponDataEncoder {
    private static final MMLogger logger = MMLogger.create(WeaponDataEncoder.class);
    private static final int WEAPON_DATA_SIZE = 5;
    // Whenever there is an error in the weapon data, we add this sequence of -1s to the list
    // to indicate that the data is invalid and must be ignored.
    private static final int[] DEFAULT_INVALID_ENTRIES = { -1, -1, -1, -1, -1};

    /**
     * Encodes the weapons of an entity into a List of Integers, each weapon is encoded as a sequence of 5 integers
     * which represents the max damage it causes, its arc, short range, medium range and long range.
     * @param entity The entity from which to encode the weapon data
     * @return the encoded list
     */
    public static List<Integer> getEncodedWeaponData(Entity entity) {
        int weaponCount = entity.getWeaponList().size();
        List<Integer> result = new ArrayList<>(weaponCount * WEAPON_DATA_SIZE);
        entity.getWeaponList().forEach(weapon -> serializeWeaponData(weapon, entity, result));
        return result;
    }

    private static void serializeWeaponData(WeaponMounted weapon, Entity entity, List<Integer> collector) {
        try {
            int equipmentId = entity.getEquipmentNum(weapon);
            var mounted = entity.getEquipment(equipmentId);

            // Use default values if mounted is null
            if (mounted == null) {
                logger.warn("No such equipment {} [{}] for {}", weapon, equipmentId, entity);
                addInvalidEntries(collector);
                return;
            }

            // Add all values in sequence without creating intermediate collections
            collector.add(Compute.computeTotalDamage(weapon));
            collector.add(entity.getWeaponArc(equipmentId));
            collector.add(weapon.getType().getShortRange());
            collector.add(weapon.getType().getMediumRange());
            collector.add(weapon.getType().getLongRange());

        } catch (Exception e) {
            logger.error(e, "Error while trying to serialize Weapon {} data for {}", weapon, entity);
            addInvalidEntries(collector);
        }
    }

    private static void addInvalidEntries(List<Integer> collector) {
        for (int value : DEFAULT_INVALID_ENTRIES) {
            collector.add(value);
        }
    }
}

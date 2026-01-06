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
package megamek.common.util;

import java.util.LinkedHashMap;
import java.util.Map;

import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.enums.AmmoTypeFlag;

public class YamlSerializerAmmoType extends YamlSerializerEquipmentType {
    static public final String TYPENAME = "ammo";

    /**
     * Constructor for YamlSerializerAmmoType.
     */
    public YamlSerializerAmmoType() {
    }

    /**
     * Constructs a map containing the YAML-serializable data for the given ammo type.
     *
     * @param equipment The ammo type to serialize
     *
     * @return A map containing the YAML-serializable data for the equipment type
     */
    @Override
    public Map<String, Object> serialize(EquipmentType equipment) {
        if (!(equipment instanceof AmmoType ammo)) {
            throw new IllegalArgumentException("Expected AmmoType but got " + equipment.getClass().getSimpleName());
        }
        Map<String, Object> data = super.serialize(ammo);
        data.put("type", TYPENAME);
        String[] flagStrings = ammo.getFlags().getSetFlagNamesAsArray(AmmoTypeFlag.class);
        if (flagStrings.length > 0) {
            data.put("flags", flagStrings);
        }
        addAmmoDetails(data, ammo);
        return data;
    }

    private static void addAmmoDetails(Map<String, Object> data, AmmoType ammo) {
        AmmoType defaultAmmo = new AmmoType();
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("type", ammo.getAmmoType().name());
        YamlEncDec.addPropIfNotDefault(details,
              "kgPerShot",
              getDoubleFieldValue(ammo, "kgPerShot"),
              getDoubleFieldValue(defaultAmmo, "kgPerShot"));

        //TODO: work in progress!

        data.put("ammo", details);
    }
}

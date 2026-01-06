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

import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.enums.MiscTypeFlag;

public class YamlSerializerMiscType extends YamlSerializerEquipmentType {
    static public final String TYPENAME = "misc";

    /**
     * Constructor for YamlSerializerAmmoType.
     */
    public YamlSerializerMiscType() {
    }

    /**
     * Constructs a map containing the YAML-serializable data for the given misc type.
     *
     * @param equipment The misc type to serialize
     *
     * @return A map containing the YAML-serializable data for the equipment type
     */
    @Override
    public Map<String, Object> serialize(EquipmentType equipment) {
        if (!(equipment instanceof MiscType misc)) {
            throw new IllegalArgumentException("Expected MiscType but got " + equipment.getClass().getSimpleName());
        }
        Map<String, Object> data = super.serialize(misc);
        data.put("type", TYPENAME);
        String[] flagStrings = misc.getFlags().getSetFlagNamesAsArray(MiscTypeFlag.class);
        if (flagStrings.length > 0) {
            data.put("flags", flagStrings);
        }
        addMiscDetails(data, misc);
        return data;
    }

    private static void addMiscDetails(Map<String, Object> data, MiscType misc) {
        MiscType defaultMisc = new MiscType();
        Map<String, Object> details = new LinkedHashMap<>();
        YamlEncDec.addPropIfNotDefault(details,
              "damageDivisor",
              getDoubleFieldValue(misc, "damageDivisor"),
              getDoubleFieldValue(defaultMisc, "damageDivisor"));
        YamlEncDec.addPropIfNotDefault(details,
              "baseDamageAbsorptionRate",
              getIntegerFieldValue(misc, "baseDamageAbsorptionRate"),
              getIntegerFieldValue(defaultMisc, "baseDamageAbsorptionRate"));
        YamlEncDec.addPropIfNotDefault(details,
              "baseDamageCapacity",
              getIntegerFieldValue(misc, "baseDamageCapacity"),
              getIntegerFieldValue(defaultMisc, "baseDamageCapacity"));
        YamlEncDec.addPropIfNotDefault(details,
              "industrial",
              getBooleanFieldValue(misc, "industrial"),
              getBooleanFieldValue(defaultMisc, "industrial"));
        data.put("misc", details);
    }
}

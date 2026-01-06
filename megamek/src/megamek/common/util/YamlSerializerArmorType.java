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

import static megamek.common.equipment.EquipmentType.*;

import java.util.LinkedHashMap;
import java.util.Map;

import megamek.common.equipment.ArmorType;
import megamek.common.equipment.EquipmentType;

public class YamlSerializerArmorType extends YamlSerializerEquipmentType {
    static public final String TYPENAME = "armor";

    /**
     * Constructor for YamlSerializerArmorType.
     */
    public YamlSerializerArmorType() {
    }

    /**
     * Constructs a map containing the YAML-serializable data for the given armor type.
     *
     * @param equipment The armor type to serialize
     *
     * @return A map containing the YAML-serializable data for the equipment type
     */
    @Override
    public Map<String, Object> serialize(EquipmentType equipment) {
        if (!(equipment instanceof ArmorType armor)) {
            throw new IllegalArgumentException("Expected ArmorType but got " + equipment.getClass().getSimpleName());
        }
        Map<String, Object> data = super.serialize(armor);
        data.put("type", TYPENAME);
        addArmorDetails(data, armor);
        return data;
    }

    private static String getArmorName(int armorId) {
        return switch (armorId) {
            case T_ARMOR_STANDARD -> "STANDARD";
            case T_ARMOR_FERRO_FIBROUS -> "FERRO_FIBROUS";
            case T_ARMOR_REACTIVE -> "REACTIVE";
            case T_ARMOR_REFLECTIVE -> "REFLECTIVE";
            case T_ARMOR_HARDENED -> "HARDENED";
            case T_ARMOR_LIGHT_FERRO -> "LIGHT_FERRO";
            case T_ARMOR_HEAVY_FERRO -> "HEAVY_FERRO";
            case T_ARMOR_PATCHWORK -> "PATCHWORK";
            case T_ARMOR_STEALTH -> "STEALTH";
            case T_ARMOR_FERRO_FIBROUS_PROTO -> "FERRO_FIBROUS_PROTO";
            case T_ARMOR_COMMERCIAL -> "COMMERCIAL";
            case T_ARMOR_LC_FERRO_CARBIDE -> "LC_FERRO_CARBIDE";
            case T_ARMOR_LC_LAMELLOR_FERRO_CARBIDE -> "LC_LAMELLOR_FERRO_CARBIDE";
            case T_ARMOR_LC_FERRO_IMP -> "LC_FERRO_IMP";
            case T_ARMOR_INDUSTRIAL -> "INDUSTRIAL";
            case T_ARMOR_HEAVY_INDUSTRIAL -> "HEAVY_INDUSTRIAL";
            case T_ARMOR_FERRO_LAMELLOR -> "FERRO_LAMELLOR";
            case T_ARMOR_PRIMITIVE -> "PRIMITIVE";
            case T_ARMOR_EDP -> "EDP";
            case T_ARMOR_ALUM -> "ALUM";
            case T_ARMOR_HEAVY_ALUM -> "HEAVY_ALUM";
            case T_ARMOR_LIGHT_ALUM -> "LIGHT_ALUM";
            case T_ARMOR_STEALTH_VEHICLE -> "STEALTH_VEHICLE";
            case T_ARMOR_ANTI_PENETRATIVE_ABLATION -> "ANTI_PENETRATIVE_ABLATION";
            case T_ARMOR_HEAT_DISSIPATING -> "HEAT_DISSIPATING";
            case T_ARMOR_IMPACT_RESISTANT -> "IMPACT_RESISTANT";
            case T_ARMOR_BALLISTIC_REINFORCED -> "BALLISTIC_REINFORCED";
            case T_ARMOR_FERRO_ALUM_PROTO -> "FERRO_ALUM_PROTO";
            case T_ARMOR_BA_STANDARD -> "BA_STANDARD";
            case T_ARMOR_BA_STANDARD_PROTOTYPE -> "BA_STANDARD_PROTOTYPE";
            case T_ARMOR_BA_STANDARD_ADVANCED -> "BA_STANDARD_ADVANCED";
            case T_ARMOR_BA_STEALTH_BASIC -> "BA_STEALTH_BASIC";
            case T_ARMOR_BA_STEALTH -> "BA_STEALTH";
            case T_ARMOR_BA_STEALTH_IMP -> "BA_STEALTH_IMP";
            case T_ARMOR_BA_STEALTH_PROTOTYPE -> "BA_STEALTH_PROTOTYPE";
            case T_ARMOR_BA_FIRE_RESIST -> "BA_FIRE_RESIST";
            case T_ARMOR_BA_MIMETIC -> "BA_MIMETIC";
            case T_ARMOR_BA_REFLECTIVE -> "BA_REFLECTIVE";
            case T_ARMOR_BA_REACTIVE -> "BA_REACTIVE";
            case T_ARMOR_PRIMITIVE_FIGHTER -> "PRIMITIVE_FIGHTER";
            case T_ARMOR_PRIMITIVE_AERO -> "PRIMITIVE_AERO";
            case T_ARMOR_AEROSPACE -> "AEROSPACE";
            case T_ARMOR_STANDARD_PROTOMEK -> "STANDARD_PROTOMEK";
            case T_ARMOR_SV_BAR_2 -> "SV_BAR_2";
            case T_ARMOR_SV_BAR_3 -> "SV_BAR_3";
            case T_ARMOR_SV_BAR_4 -> "SV_BAR_4";
            case T_ARMOR_SV_BAR_5 -> "SV_BAR_5";
            case T_ARMOR_SV_BAR_6 -> "SV_BAR_6";
            case T_ARMOR_SV_BAR_7 -> "SV_BAR_7";
            case T_ARMOR_SV_BAR_8 -> "SV_BAR_8";
            case T_ARMOR_SV_BAR_9 -> "SV_BAR_9";
            case T_ARMOR_SV_BAR_10 -> "SV_BAR_10";
            default -> "UNKNOWN";
        };
    }

    private static void addArmorDetails(Map<String, Object> data, ArmorType armor) {
        ArmorType defaultArmor = new ArmorType();
        Map<String, Object> details = new LinkedHashMap<>();
        int armorType = armor.getArmorType();
        details.put("type", getArmorName(armorType));
        details.put("typeId", armorType);
        YamlEncDec.addPropIfNotDefault(details,
              "fighterSlots",
              getIntegerFieldValue(armor, "fighterSlots"),
              getIntegerFieldValue(defaultArmor, "fighterSlots"));
        YamlEncDec.addPropIfNotDefault(details,
              "patchworkSlotsMekSV",
              getIntegerFieldValue(armor, "patchworkSlotsMekSV"),
              getIntegerFieldValue(defaultArmor, "patchworkSlotsMekSV"));
        YamlEncDec.addPropIfNotDefault(details,
              "patchworkSlotsCVFtr",
              getIntegerFieldValue(armor, "patchworkSlotsCVFtr"),
              getIntegerFieldValue(defaultArmor, "patchworkSlotsCVFtr"));
        YamlEncDec.addPropIfNotDefault(details,
              "bar",
              getIntegerFieldValue(armor, "bar"),
              getIntegerFieldValue(defaultArmor, "bar"));
        YamlEncDec.addPropIfNotDefault(details,
              "pptMultiplier",
              getDoubleFieldValue(armor, "pptMultiplier"),
              getDoubleFieldValue(defaultArmor, "pptMultiplier"));
        YamlEncDec.addPropIfNotDefault(details,
              "weightPerPoint",
              getDoubleFieldValue(armor, "weightPerPoint"),
              getDoubleFieldValue(defaultArmor, "weightPerPoint"));

        double[] pptDropship = getTypedFieldValue(armor, "pptDropship", double[].class);
        double[] pptDropshipDefault = getTypedFieldValue(defaultArmor, "pptDropship", double[].class);
        if (pptDropship != null && pptDropship.length > 0) {
            // Only add if different from default (default is usually empty)
            if (pptDropshipDefault == null || pptDropshipDefault.length != pptDropship.length) {
                details.put("pptDropship", pptDropship);
            }
        }

        double[] pptCapital = getTypedFieldValue(armor, "pptCapital", double[].class);
        double[] pptCapitalDefault = getTypedFieldValue(defaultArmor, "pptCapital", double[].class);
        if (pptCapital != null && pptCapital.length > 0) {
            if (pptCapitalDefault == null || pptCapitalDefault.length != pptCapital.length) {
                details.put("pptCapital", pptCapital);
            }
        }

        Map<?, ?> weightPerPointSV = getTypedFieldValue(armor, "weightPerPointSV", Map.class);
        Map<?, ?> weightPerPointSVDefault = getTypedFieldValue(defaultArmor, "weightPerPointSV", Map.class);
        if (weightPerPointSV != null && !weightPerPointSV.isEmpty()) {
            if (!weightPerPointSV.equals(weightPerPointSVDefault)) {
                details.put("weightPerPointSV", weightPerPointSV);
            }
        }

        data.put("armor", details);
    }
}

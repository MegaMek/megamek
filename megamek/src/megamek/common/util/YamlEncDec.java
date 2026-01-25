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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.WeaponType;
import megamek.logging.MMLogger;

public class YamlEncDec {
    private static final MMLogger logger = MMLogger.create(YamlEncDec.class);

    /**
     * Serializes an EquipmentType to a YAML-compatible map.
     * The equipment type's getYamlData() method handles all serialization,
     * including type-specific data, flags, and version information.
     *
     * @param equipmentType The EquipmentType to serialize.
     *
     * @return A Map containing the serialized data.
     */
    public static Map<String, Object> serialize(EquipmentType equipmentType) {
        return equipmentType.getYamlData();
    }

    private static String sanitizeFileName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        String sanitized = name;
        sanitized = sanitized.replaceAll("[\\<>:\"/\\\\|?*\\p{Cntrl}]", "");
        sanitized = sanitized.replaceAll("[. ]+$", "");
        sanitized = sanitized.replaceAll("^_|_$", "");
        if (sanitized.isEmpty()) {
            return null;
        }
        return sanitized;
    }

    public static void addPropIfNotEmpty(Map<String, Object> data, String key, Object value) {
        if (value != null && !value.toString().isEmpty()) {
            data.put(key, value);
        }
    }

    /**
     * Formats a double value to avoid scientific notation in YAML output.
     * Returns the appropriate Number type for clean serialization.
     *
     * @param value The double value to format
     * @return Long for whole numbers, BigDecimal for decimals
     */
    public static Number formatDouble(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            // Whole number - return as long (serializes as plain integer)
            return (long) value;
        }
        // Has decimal places - BigDecimal handles small decimals correctly
        return BigDecimal.valueOf(value);
    }

    public static void addPropIfNotDefault(Map<String, Object> data, String key, Object value, Object defaultValue) {
        if (data == null || key == null || key.trim().isEmpty()) {
            return;
        }

        // Handle null cases
        if (value == null && defaultValue == null) {
            return; // Both null, don't add
        }

        if (value == null || defaultValue == null) {
            data.put(key, value); // One is null, add the value
            return;
        }

        // For numeric types, handle floating point precision
        if (value instanceof Number && defaultValue instanceof Number) {
            if (isFloatingPoint(value) || isFloatingPoint(defaultValue)) {
                double diff = Math.abs(((Number) value).doubleValue() - ((Number) defaultValue).doubleValue());
                if (diff > 1e-9) {
                    data.put(key, value);
                }
            } else if (!value.equals(defaultValue)) {
                data.put(key, value);
            }
            return;
        }

        // Default comparison using equals()
        if (!value.equals(defaultValue)) {
            data.put(key, value);
        }
    }

    private static boolean isFloatingPoint(Object value) {
        return value instanceof Double || value instanceof Float;
    }

    public static void writeEquipmentDatabase(String targetFolder) {
        try {
            logger.info("Exporting YAML files to {}", targetFolder);
            HashMap<String, Boolean> seen = new HashMap<>();

            YAMLMapper mapper = new YAMLMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);

            // Write everything except ammo types that come from mutations
            for (Enumeration<EquipmentType> equipmentTypes = EquipmentType.getAllTypes();
                  equipmentTypes.hasMoreElements(); ) {
                EquipmentType equipmentType = equipmentTypes.nextElement();
                if (equipmentType instanceof AmmoType ammo) {
                    if (ammo.getBaseAmmo() != null) {
                        continue;
                    }
                }
                writeEquipmentYamlEntry(equipmentType, targetFolder, mapper, seen);
            }
            // Now write the ammo types that comes from mutations
            for (Enumeration<EquipmentType> equipmentTypes = EquipmentType.getAllTypes();
                  equipmentTypes.hasMoreElements(); ) {
                EquipmentType equipmentType = equipmentTypes.nextElement();
                if (equipmentType instanceof AmmoType ammo) {
                    if (ammo.getBaseAmmo() == null) {
                        continue;
                    }
                } else {
                    continue;
                }
                writeEquipmentYamlEntry(equipmentType, targetFolder, mapper, seen);
            }
        } catch (Exception e) {
            logger.error(e, "Error writing YAML database: {}", e.getMessage());
        }
    }

    private static void writeEquipmentYamlEntry(EquipmentType equipmentType, String targetFolder, YAMLMapper yamlMapper,
          HashMap<String, Boolean> seen)
          throws Exception {
        String typeFolder;
        String seenKey = null;
        if (equipmentType instanceof AmmoType) {
            typeFolder = "ammo";
        } else if (equipmentType instanceof WeaponType) {
            typeFolder = "weapon";
        } else if (equipmentType instanceof MiscType) {
            typeFolder = "misc";
        } else {
            throw new Exception("Failed YAML export for unknown equipment type: " + equipmentType.getName());
        }

        File parentDir = new File(targetFolder + File.separator + typeFolder);
        if (!parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new Exception("Could not create directory: " + parentDir.getAbsolutePath());
            }
        }
        String fileName = null;
        Map<String, Object> content = null;
        boolean appendMode = false;
        System.out.println("- " + equipmentType.getName());
        if (equipmentType instanceof AmmoType ammo) {
            if (ammo.getBaseAmmo() != null) {
                fileName = ammo.getBaseAmmo().getShortName();
            } else {
                fileName = ammo.getShortName();
            }
            content = ammo.getYamlData();
        } else if (equipmentType instanceof WeaponType weapon) {
            fileName = weapon.getShortName();
            content = weapon.getYamlData();
        } else if (equipmentType instanceof MiscType misc) {
            fileName = misc.getShortName();
            content = misc.getYamlData();
        }
        //TODO: BombType, SmallWeaponAmmoType, ArmorType
        fileName = sanitizeFileName(fileName);
        if (fileName == null) {
            throw new Exception("Filename could not be determined for equipment type: "
                  + equipmentType.getName()
                  + ". Skipping YAML export for this item.");
        }
        if (content == null) {
            throw new Exception("Content for YAML export is null for: " + fileName + ". Skipping.");
        }
        seenKey = typeFolder + "_" + fileName;
        if (equipmentType instanceof AmmoType ammo) {
            if (ammo.getBaseAmmo() != null) {
                if (!seen.containsKey(seenKey)) {
                    throw new Exception("Not found seen key " + seenKey + " for ammo mutation " + ammo.getName());
                }

            }
        }
        final String fullPath = parentDir.getAbsolutePath() + File.separator + fileName + ".yaml";
        appendMode = seen.containsKey(seenKey);
        final File f = new File(fullPath);
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(f, appendMode))) {
            yamlMapper.writeValue(bufferedWriter, content);
        }
        seen.put(seenKey, true);
    }
}

package megamek.common;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import megamek.logging.MMLogger;

public class YamlEncDec {
    private static final MMLogger logger = MMLogger.create(YamlEncDec.class);

    public static String VARIABLE = "variable";
    public static String VERSION = "1.0";

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
    
    public static void writeEquipmentDatabase(String targetFolder) {
        try {
            logger.info("Exporting YAML files to " + targetFolder);
            HashMap <String, Boolean> seen = new HashMap<>();
            
            YAMLMapper mapper = new YAMLMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);

            // Write everything except ammo types that come from mutations
            for (Enumeration<EquipmentType> equipmentTypes = EquipmentType.getAllTypes(); equipmentTypes.hasMoreElements(); ) {
                EquipmentType equipmentType = equipmentTypes.nextElement();
                if (equipmentType instanceof AmmoType ammo) {
                    if (ammo.base != null) {
                        continue;
                    }
                }
                writeEquipmentYamlEntry(equipmentType, targetFolder, mapper, seen);
            }
            // Now write the ammo types that comes from mutations
            for (Enumeration<EquipmentType> equipmentTypes = EquipmentType.getAllTypes(); equipmentTypes.hasMoreElements(); ) {
                EquipmentType equipmentType = equipmentTypes.nextElement();
                if (equipmentType instanceof AmmoType ammo) {
                    if (ammo.base == null) {
                        continue;
                    }
                } else {
                    continue;
                }
                writeEquipmentYamlEntry(equipmentType, targetFolder, mapper, seen);
            }
        } catch (Exception e) {
            System.out.println("Error writing YAML database: " + e.getMessage());
            logger.error("", e);
        }
    }

    private static void writeEquipmentYamlEntry(EquipmentType equipmentType, String targetFolder, YAMLMapper yamlMapper, HashMap <String, Boolean> seen)
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
        System.out.println("- "+equipmentType.getName());
        if (equipmentType instanceof AmmoType ammo) {
            if (ammo.base != null) {
                fileName = ammo.base.getShortName();
            } else {
                fileName = ammo.getShortName();
            }
            content = ammo.getYamlData();
        } else
        if (equipmentType instanceof WeaponType weapon) {
            fileName =  weapon.getShortName();
            content = weapon.getYamlData();
        } else 
        if (equipmentType instanceof MiscType misc) {
            fileName =  misc.getShortName();
            content = misc.getYamlData();
        }
        //TODO: BombType, SmallWeaponAmmoType, ArmorType
        fileName = sanitizeFileName(fileName);
        if (fileName == null) {
            throw new Exception("Filename could not be determined for equipment type: " + equipmentType.getName() + ". Skipping YAML export for this item.");
        }
        if (content == null) {
            throw new Exception("Content for YAML export is null for: " + fileName + ". Skipping.");
        }
        seenKey = typeFolder+"_"+fileName;
        if (equipmentType instanceof AmmoType ammo) {
            if (ammo.base != null) {
                if (!seen.containsKey(seenKey)) {
                    throw new Exception("Not found seen key "+seenKey+ " for ammo mutation "+ammo.getName());
                };
            }
        }
        final String fullPath = parentDir.getAbsolutePath() + File.separator + fileName + ".yaml";
        appendMode = seen.containsKey(seenKey);
        final File f = new File(fullPath);
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(f, appendMode))) {
            if (!appendMode) {
                bufferedWriter.append("version: \"" + YamlEncDec.VERSION + "\"\n");
            }
            yamlMapper.writeValue(bufferedWriter, content);
        }
        seen.put(seenKey, true);
    }
}

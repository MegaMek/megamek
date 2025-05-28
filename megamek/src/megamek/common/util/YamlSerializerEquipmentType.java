package megamek.common.util;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

import megamek.common.*;
import megamek.common.ITechnology.AvailabilityValue;
import megamek.common.ITechnology.Faction;
import megamek.common.TechAdvancement.AdvancementPhase;
import megamek.common.ITechnology.Era;
import megamek.logging.MMLogger;

/**
 * Utility class for serializing EquipmentType objects to YAML-compatible data structures.
 * This isolates YAML serialization logic from the core EquipmentType class.
 */
public final class YamlSerializerEquipmentType {
    private static final MMLogger logger = MMLogger.create(YamlSerializerEquipmentType.class);
    
    private YamlSerializerEquipmentType() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Constructs a map containing the YAML-serializable data for the given equipment type.
     * 
     * @param equipment The equipment type to serialize
     * @return A map containing the YAML-serializable data for the equipment type
     */
    public static Map<String, Object> serialize(EquipmentType equipment) {
        Map<String, Object> data = new LinkedHashMap<>();
        
        // Basic identification
        addBasicIdentification(data, equipment);
        
        // Equipment statistics
        addStatistics(data, equipment);
        
        // Equipment modes
        addModes(data, equipment);
        
        // Technology advancement
        addTechAdvancement(data, equipment);
        
        return data;
    }
    
    /**
     * Adds basic identification information to the YAML data map.
     */
    private static void addBasicIdentification(Map<String, Object> data, EquipmentType equipment) {
        data.put("id", equipment.getInternalName());
        data.put("name", equipment.getName());
        
        YamlEncDec.addPropIfNotEmpty(data, "shortName", equipment.getShortName());
        YamlEncDec.addPropIfNotEmpty(data, "sortingName", equipment.getSortingName());
        YamlEncDec.addPropIfNotEmpty(data, "rulesRefs", equipment.getRulesRefs());
        
        addAliases(data, equipment);
    }
    
    /**
     * Adds alias names to the YAML data map, excluding duplicates.
     */
    private static void addAliases(Map<String, Object> data, EquipmentType equipment) {
        Enumeration<String> names = equipment.getNames();
        if (names == null || !names.hasMoreElements()) {
            return;
        }
        
        Set<String> uniqueAliases = new LinkedHashSet<>();
        
        while (names.hasMoreElements()) {
            String aliasName = names.nextElement();
            if (aliasName != null && !aliasName.trim().isEmpty()) {
                if (aliasName.equals(equipment.getInternalName())
                        || aliasName.equals(equipment.getName())
                        || aliasName.equals(equipment.getShortName())) {
                    continue;
                }
                uniqueAliases.add(aliasName);
            }
        }
        
        if (!uniqueAliases.isEmpty()) {
            data.put("aliases", new ArrayList<>(uniqueAliases));
        }
    }
    
    /**
     * Gets the value of a field directly using reflection, avoiding method calls.
     * 
     * @param object The object to get the field value from
     * @param fieldName The name of the field
     * @return The field value, or null if the field cannot be accessed
     */
    private static Object getFieldValue(Object object, String fieldName) {
        if (object == null) {
            return null;
        }
        
        try {
            Class<?> clazz = object.getClass();
            Field field = null;
            
            // Search for the field in the class hierarchy
            while (clazz != null && field == null) {
                try {
                    field = clazz.getDeclaredField(fieldName);
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }
            
            if (field == null) {
                logger.warn("Field '{}' not found in class hierarchy of {}", fieldName, object.getClass().getSimpleName());
                return null;
            }
            
            field.setAccessible(true);
            return field.get(object);
            
        } catch (IllegalAccessException | SecurityException e) {
            logger.warn("Failed to access field '{}' in {}: {}", fieldName, object.getClass().getSimpleName(), e.getMessage());
            return null;
        }
    }
    
    /**
     * Gets a typed field value.
     * 
     * @param <T> The expected type of the field
     * @param object The object to get the field value from
     * @param fieldName The name of the field
     * @param expectedType The expected type of the field
     * @return The field value cast to the expected type, or null if not accessible or wrong type
     */
    @SuppressWarnings("unchecked")
    private static <T> T getTypedFieldValue(Object object, String fieldName, Class<T> expectedType) {
        Object value = getFieldValue(object, fieldName);
        
        if (value == null) {
            return null;
        }
        
        if (expectedType.isInstance(value)) {
            return (T) value;
        }
        
        logger.warn("Field '{}' has type {} but expected {}", fieldName, value.getClass().getSimpleName(), expectedType.getSimpleName());
        return null;
    }

    private static Double getDoubleFieldValue(Object object, String fieldName) {
        return getTypedFieldValue(object, fieldName, Double.class);
    }

    private static Integer getIntegerFieldValue(Object object, String fieldName) {
        return getTypedFieldValue(object, fieldName, Integer.class);

    }

    private static Boolean getBooleanFieldValue(Object object, String fieldName) {
        return getTypedFieldValue(object, fieldName, Boolean.class);
    }

    private static String getStringFieldValue(Object object, String fieldName) {
        return getTypedFieldValue(object, fieldName, String.class);
    }
    
    /**
     * Adds equipment statistics to the YAML data map.
     */
    private static void addStatistics(Map<String, Object> data, EquipmentType equipment) {
        EquipmentType defaultEquipment = createDefaultInstance(equipment);
        Map<String, Object> stats = new LinkedHashMap<>();
        
        // Core statistics
        addVariableOrFixedValue(stats, "tonnage", getDoubleFieldValue(equipment, "tonnage"), equipment::isVariableTonnage);
        addVariableOrFixedValue(stats, "cost", getDoubleFieldValue(equipment, "cost"), equipment::isVariableCost);
        addVariableOrFixedValue(stats, "bv", getDoubleFieldValue(equipment, "bv"), equipment::isVariableBV);
        addVariableOrFixedValue(stats, "criticals", getIntegerFieldValue(equipment, "criticals"), equipment::isVariableCriticals);

        // Optional statistics
        YamlEncDec.addPropIfNotDefault(stats, "hittable", getBooleanFieldValue(equipment, "hittable"), getBooleanFieldValue(defaultEquipment, "hittable"));
        YamlEncDec.addPropIfNotDefault(stats, "spreadable", getBooleanFieldValue(equipment, "spreadable"), getBooleanFieldValue(defaultEquipment, "spreadable"));
        YamlEncDec.addPropIfNotDefault(stats, "explosive", getBooleanFieldValue(equipment, "explosive"), getBooleanFieldValue(defaultEquipment, "explosive"));
        YamlEncDec.addPropIfNotDefault(stats, "toHitModifier", getIntegerFieldValue(equipment, "toHitModifier"), getIntegerFieldValue(defaultEquipment, "toHitModifier"));
        YamlEncDec.addPropIfNotDefault(stats, "tankslots", getIntegerFieldValue(equipment, "tankslots"), getIntegerFieldValue(defaultEquipment, "tankslots"));
        YamlEncDec.addPropIfNotDefault(stats, "svslots", getIntegerFieldValue(equipment, "svslots"), getIntegerFieldValue(defaultEquipment, "svslots"));
        YamlEncDec.addPropIfNotDefault(stats, "omniFixedOnly", getBooleanFieldValue(equipment, "omniFixedOnly"), getBooleanFieldValue(defaultEquipment, "omniFixedOnly"));
        YamlEncDec.addPropIfNotDefault(stats, "instantModeSwitch", getBooleanFieldValue(equipment, "instantModeSwitch"), getBooleanFieldValue(defaultEquipment, "instantModeSwitch"));

        data.put("stats", stats);
    }
    
    /**
     * Adds a value to the map, using VARIABLE constant if the value is variable.
     */
    private static void addVariableOrFixedValue(Map<String, Object> map, String key, Object value, BooleanSupplier isVariable) {
        if (isVariable.getAsBoolean()) {
            map.put(key, YamlEncDec.VARIABLE);
        } else {
            map.put(key, value);
        }
    }
    
    /**
     * Creates a default instance of the appropriate equipment type for comparison.
     */
    private static EquipmentType createDefaultInstance(EquipmentType equipment) {
        try {
            if (equipment instanceof WeaponType) {
                return WeaponType.class.getDeclaredConstructor().newInstance();
            } else if (equipment instanceof AmmoType) {
                return AmmoType.class.getDeclaredConstructor().newInstance();
            } else if (equipment instanceof MiscType) {
                return MiscType.class.getDeclaredConstructor().newInstance();
            } else {
                // Fallback to generic EquipmentType
                return new EquipmentType();
            }
        } catch (Exception e) {
            logger.warn("Failed to create default instance for comparison: " + e.getMessage());
            return new EquipmentType();
        }
    }
    
    /**
     * Adds equipment modes to the YAML data map.
     */
    private static void addModes(Map<String, Object> data, EquipmentType equipment) {
        if (!equipment.hasModes()) {
            return;
        }
        
        List<String> modeNames = new ArrayList<>();
        Enumeration<EquipmentMode> modes = equipment.getModes();
        
        while (modes.hasMoreElements()) {
            EquipmentMode mode = modes.nextElement();
            if (mode != null && mode.getName() != null) {
                modeNames.add(mode.getName());
            }
        }
        
        if (!modeNames.isEmpty()) {
            data.put("modes", modeNames);
        }
    }
    
    /**
     * Adds technology advancement information to the YAML data map.
     */
    private static void addTechAdvancement(Map<String, Object> data, EquipmentType equipment) {
        TechAdvancement techAdvancement = equipment.getTechAdvancement();
        if (techAdvancement == null) {
            return;
        }
        
        EquipmentType defaultEquipment = createDefaultInstance(equipment);
        TechAdvancement defaultTech = defaultEquipment.getTechAdvancement();
        
        Map<String, Object> techData = new LinkedHashMap<>();
        
        // Basic tech information
        techData.put("base", techAdvancement.getTechBase().name().toLowerCase());
        techData.put("rating", techAdvancement.getTechRating().getName());
        techData.put("level", techAdvancement.getStaticTechLevel().toString());
        
        // Availability by era
        addAvailabilityData(techData, techAdvancement);
        
        // Advancement dates
        addAdvancementData(techData, techAdvancement, defaultTech);
        
        // Faction information
        addFactionData(techData, techAdvancement);
        
        data.put("tech", techData);
    }
    
    /**
     * Adds availability information by era to the technology data.
     */
    private static void addAvailabilityData(Map<String, Object> techData, TechAdvancement techAdvancement) {
        Map<String, Object> availability = new LinkedHashMap<>();
        for (Era era : Era.values()) {
            AvailabilityValue availabilityValue = techAdvancement.getBaseAvailability(era);
            availability.put(era.name().toLowerCase(), availabilityValue.getName());
        }
        techData.put("availability", availability);
    }
    
    /**
     * Adds advancement phase dates to the technology data.
     */
    private static void addAdvancementData(Map<String, Object> techData, TechAdvancement techAdvancement, TechAdvancement defaultTech) {
        Map<String, Object> advancement = new LinkedHashMap<>();
        
        Map<String, Object> advancementIS = createAdvancementPhaseMap(false, techAdvancement, defaultTech);
        Map<String, Object> advancementClan = createAdvancementPhaseMap(true, techAdvancement, defaultTech);
        
        if (!advancementIS.isEmpty()) {
            advancement.put("is", advancementIS);
        }
        if (!advancementClan.isEmpty()) {
            advancement.put("clan", advancementClan);
        }
        
        if (!advancement.isEmpty()) {
            techData.put("advancement", advancement);
        }
    }
    
    /**
     * Creates advancement phase map for IS or Clan technology.
     */
    private static Map<String, Object> createAdvancementPhaseMap(boolean isClan, TechAdvancement techAdvancement, TechAdvancement defaultTech) {
        Map<String, Object> advancementMap = new LinkedHashMap<>();
        
        for (AdvancementPhase phase : AdvancementPhase.values()) {
            Integer advancement = isClan ? techAdvancement.getClanAdvancement(phase) 
                                       : techAdvancement.getISAdvancement(phase);
            if (advancement == null) {
                continue;
            }
            
            boolean isApproximate = isClan ? techAdvancement.getClanApproximate(phase) 
                                          : techAdvancement.getISApproximate(phase);
            String advancementStr = (isApproximate ? "~" : "") + advancement;
            
            String defaultAdvancementStr = getDefaultAdvancementString(phase, isClan, defaultTech);
            
            YamlEncDec.addPropIfNotDefault(advancementMap, phase.name().toLowerCase(),
                  advancementStr, defaultAdvancementStr);
        }
        
        return advancementMap;
    }
    
    /**
     * Gets the default advancement string for comparison.
     */
    private static String getDefaultAdvancementString(AdvancementPhase phase, boolean isClan, TechAdvancement defaultTech) {
        Integer defaultAdvancement = isClan ? defaultTech.getClanAdvancement(phase) 
                                           : defaultTech.getISAdvancement(phase);
        if (defaultAdvancement == null) {
            return null;
        }
        
        boolean defaultApproximate = isClan ? defaultTech.getClanApproximate(phase) 
                                           : defaultTech.getISApproximate(phase);
        return (defaultApproximate ? "~" : "") + defaultAdvancement;
    }
    
    /**
     * Adds faction information to the technology data.
     */
    private static void addFactionData(Map<String, Object> techData, TechAdvancement techAdvancement) {
        Map<String, Object> factions = new LinkedHashMap<>();
        
        addFactionList(factions, "prototype", techAdvancement.getPrototypeFactions());
        addFactionList(factions, "production", techAdvancement.getProductionFactions());
        addFactionList(factions, "extinction", techAdvancement.getExtinctionFactions());
        addFactionList(factions, "reintroduction", techAdvancement.getReintroductionFactions());
        
        if (!factions.isEmpty()) {
            techData.put("factions", factions);
        }
    }
    
    /**
     * Adds a faction list to the factions map if not empty.
     */
    private static void addFactionList(Map<String, Object> factions, String key, Set<Faction> factionSet) {
        if (factionSet == null || factionSet.isEmpty()) {
            return;
        }
        
        List<String> factionCodes = factionSet.stream()
            .filter(Objects::nonNull)
            .map(Faction::getCodeMM)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        
        if (!factionCodes.isEmpty()) {
            factions.put(key, factionCodes);
        }
    }
}
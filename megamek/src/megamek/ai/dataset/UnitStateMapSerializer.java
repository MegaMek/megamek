/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
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
 */
package megamek.ai.dataset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import megamek.ai.dataset.UnitStateMap.Field;
import megamek.common.UnitRole;
import megamek.common.enums.GamePhase;

/**
 * <p>Serializer for UnitStateMap to TSV format.</p>
 * <p>Uses a flexible map-based approach with enum fields.</p>
 * @author Luana Coppio
 */
public class UnitStateMapSerializer extends TabSeparatedValueSerializer<UnitStateMap> {

    // Define the default field order for serialization
    private final List<Field> fieldOrder;

    private static final Map<Class<?>, Function<Object, String>> FORMAT_HANDLERS = new HashMap<>();
    static {
        FORMAT_HANDLERS.put(Boolean.class, value -> (Boolean) value ? "1" : "0");
        FORMAT_HANDLERS.put(boolean.class, value -> (Boolean) value ? "1" : "0");
        FORMAT_HANDLERS.put(Double.class, LOG_DECIMAL::format);
        FORMAT_HANDLERS.put(double.class, LOG_DECIMAL::format);
        FORMAT_HANDLERS.put(Float.class, LOG_DECIMAL::format);
        FORMAT_HANDLERS.put(float.class, LOG_DECIMAL::format);
        FORMAT_HANDLERS.put(GamePhase.class, value -> ((GamePhase) value).name());
        FORMAT_HANDLERS.put(UnitRole.class, value -> value == null ? UnitRole.NONE.name() : ((UnitRole) value).name());
        FORMAT_HANDLERS.put(List.class, value -> ((List<?>) value).stream()
                                                       .map(String::valueOf)
                                                       .collect(Collectors.joining(" ")));
    }

    /**
     * Creates a serializer with default field order.
     */
    public UnitStateMapSerializer() {
        // Use all fields in their enum order
        this.fieldOrder = Arrays.asList(Field.values());
    }

    /**
     * Creates a serializer with custom field order.
     * @param fieldOrder The desired order of fields for serialization
     */
    public UnitStateMapSerializer(List<Field> fieldOrder) {
        this.fieldOrder = new ArrayList<>(fieldOrder);
    }

    @Override
    public String serialize(UnitStateMap stateMap) {
        List<String> values = new ArrayList<>(fieldOrder.size());

        // Get all fields from the map
        Map<Field, Object> allFields = stateMap.getAllFields();

        // Process each field in the defined order
        for (Field field : fieldOrder) {
            Object value = allFields.get(field);

            // Handle null values
            if (value == null) {
                // Special case for UnitRole
                if (field == Field.ROLE) {
                    values.add(UnitRole.NONE.name());
                } else {
                    values.add("");
                }
                continue;
            }

            // Get the class of the value
            Class<?> type = value.getClass();

            // Use format handler if available for this type
            if (FORMAT_HANDLERS.containsKey(type)) {
                values.add(FORMAT_HANDLERS.get(type).apply(value));
            } else if (type.isEnum()) {
                values.add(((Enum<?>) value).name());
            } else if (List.class.isAssignableFrom(type)) {
                values.add(FORMAT_HANDLERS.get(List.class).apply(value));
            } else {
                values.add(String.valueOf(value));
            }
        }

        return String.join("\t", values);
    }

    @Override
    public String getHeaderLine() {
        return fieldOrder.stream()
                     .map(Field::name)
                     .collect(Collectors.joining("\t"));
    }
}

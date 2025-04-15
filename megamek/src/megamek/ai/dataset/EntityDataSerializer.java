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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import megamek.common.UnitRole;
import megamek.common.enums.AimingMode;
import megamek.common.enums.GamePhase;

/**
 * Abstract base class for serializers that convert entity data maps to TSV format.
 * @param <F> The enum type representing field names
 * @param <T> The type of EntityDataMap to be serialized
 * @author Luana Coppio
 */
public abstract class EntityDataSerializer<F extends Enum<F>, T extends EntityDataMap<F>> {

    protected static final DecimalFormat LOG_DECIMAL = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.US));

        // Shared format handlers for special types that need custom serialization
        protected static final Map<Class<?>, Function<Object, String>> FORMAT_HANDLERS = new HashMap<>();

        // Initialize default formatters for common types
        static {
            // Boolean values
            FORMAT_HANDLERS.put(Boolean.class, value -> (Boolean) value ? "1" : "0");
            FORMAT_HANDLERS.put(boolean.class, value -> (Boolean) value ? "1" : "0");

            // Numeric values with decimal formatting
            FORMAT_HANDLERS.put(Double.class, LOG_DECIMAL::format);
            FORMAT_HANDLERS.put(double.class, LOG_DECIMAL::format);
            FORMAT_HANDLERS.put(Float.class, LOG_DECIMAL::format);
            FORMAT_HANDLERS.put(float.class, LOG_DECIMAL::format);

            // Common enum types
            FORMAT_HANDLERS.put(UnitRole.class, value -> ((UnitRole) value).name());
            FORMAT_HANDLERS.put(GamePhase.class, value -> ((GamePhase) value).name());
            FORMAT_HANDLERS.put(AimingMode.class, value -> ((AimingMode) value).name());

        // Lists with space-separated values
        FORMAT_HANDLERS.put(List.class, value -> {
            // Special handling for lists of enums
            if (!((List<?>) value).isEmpty() && ((List<?>) value).get(0) instanceof Enum) {
                return ((List<?>) value).stream()
                             .map(step -> ((Enum<?>) step).name())
                             .collect(Collectors.joining(" "));
            }
            // Default handling for other lists
            return ((List<?>) value).stream()
                         .map(String::valueOf)
                         .collect(Collectors.joining(" "));
        });
    }

    // Define the field order for serialization
    private final List<F> fieldOrder;

    /**
     * Creates a serializer with all fields of the enum in their natural order.
     * @param fieldEnumClass The class of the enum type F
     */
    protected EntityDataSerializer(Class<F> fieldEnumClass) {
        this.fieldOrder = Arrays.asList(fieldEnumClass.getEnumConstants());
    }

    /**
     * Creates a serializer with custom field order.
     * @param fieldOrder The desired order of fields for serialization
     */
    protected EntityDataSerializer(List<F> fieldOrder) {
        this.fieldOrder = new ArrayList<>(fieldOrder);
    }

    public String serialize(T entityData) {
        List<String> values = new ArrayList<>(fieldOrder.size());

        // Get all fields from the map
        Map<F, Object> allFields = entityData.getAllFields();

        // Process each field in the defined order
        for (F field : fieldOrder) {
            Object value = allFields.get(field);

            // Handle null values
            if (value == null) {
                // Delegate to subclass for field-specific null value handling
                values.add(getNullValue(field));
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

    public String getHeaderLine() {
        return fieldOrder.stream()
                     .map(Enum::name)
                     .collect(Collectors.joining("\t"));
    }

    /**
     * Adds a custom field format handler.
     * @param type The class type to handle
     * @param formatter The formatting function
     */
    public void addFormatHandler(Class<?> type, Function<Object, String> formatter) {
        FORMAT_HANDLERS.put(type, formatter);
    }

    /**
     * Gets the string representation for a null value for the given field.
     * Subclasses can override this to provide field-specific default values.
     * @param field The field enum
     * @return The string representation for a null value
     */
    protected String getNullValue(F field) {
        return "";
    }
}

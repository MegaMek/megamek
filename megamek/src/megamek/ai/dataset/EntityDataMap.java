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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract base class for data maps that use enum fields.
 * @param <F> The enum type representing field names
 * @author Luana Coppio
 */
public abstract class EntityDataMap<F extends Enum<F>> {

    // The class of the enum type F, used for creating EnumMap
    private final Class<F> fieldEnumClass;

    // Use EnumMap for type safety with our enum fields
    private final Map<F, Object> data;

    // Keep track of insertion order separately
    private final List<F> fieldOrder = new ArrayList<>();

    /**
     * Creates an empty EntityDataMap.
     * @param fieldEnumClass The class of the enum type F
     */
    protected EntityDataMap(Class<F> fieldEnumClass) {
        this.fieldEnumClass = fieldEnumClass;
        this.data = new EnumMap<>(fieldEnumClass);
    }

    /**
     * Adds a field to the data map.
     * @param field The field enum
     * @param value The field value
     * @return This EntityDataMap for method chaining
     */
    public EntityDataMap<F> put(F field, Object value) {
        if (!data.containsKey(field)) {
            fieldOrder.add(field);
        }
        data.put(field, value);
        return this;
    }

    /**
     * Gets a field value from the data map.
     * @param field The field enum
     * @return The field value, or null if not present
     */
    public Object get(F field) {
        return data.get(field);
    }

    /**
     * Gets a field value with type casting.
     * @param <T> The expected type
     * @param field The field enum
     * @param type The class of the expected type
     * @return The field value cast to the expected type, or null if not present
     */
    @SuppressWarnings("unchecked")
    public <T> T get(F field, Class<T> type) {
        Object value = data.get(field);
        if (type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * Gets a list field value with proper generic typing.
     * @param <T> The element type of the list
     * @param field The field enum
     * @param elementType The class of the list elements
     * @return The field value as a properly typed list, or null if not present
     */
    @SuppressWarnings("unchecked")
    protected <T> List<T> getList(F field, Class<T> elementType) {
        Object value = data.get(field);
        if (!(value instanceof List<?> list)) {
            return null;
        }

        if (list.isEmpty()) {
            return (List<T>) list;
        }

        // Check first element's type as a representative
        Object firstElement = list.get(0);
        if (elementType.isInstance(firstElement)) {
            return (List<T>) list;
        }

        return null;
    }

    /**
     * Gets all fields and values in the map.
     * @return The underlying map of data
     */
    public Map<F, Object> getAllFields() {
        return new EnumMap<>(data);
    }

    /**
     * Gets the ordered list of field enums.
     * @return List of fields in insertion order
     */
    public List<F> getFieldOrder() {
        return new ArrayList<>(fieldOrder);
    }

    /**
     * Gets the class of the field enum.
     * @return The enum class
     */
    public Class<F> getFieldEnumClass() {
        return fieldEnumClass;
    }
}

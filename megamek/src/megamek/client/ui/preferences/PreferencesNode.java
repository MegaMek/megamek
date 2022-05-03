/*
 * Copyright (c) 2019-2021 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.preferences;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a group of {@link PreferenceElement}s that are part of the same Class.
 *
 * This class is not thread-safe.
 */
public class PreferencesNode {
    //region Variable Declarations
    private final Class<?> node;
    private final Map<String, PreferenceElement> elements;
    private Map<String, String> initialValues;
    private boolean initialized;
    private boolean finalized;
    //endregion Variable Declarations

    //region Constructors
    public PreferencesNode(final Class<?> node) {
        this.node = Objects.requireNonNull(node);
        this.elements = new HashMap<>();
        setInitialValues(new HashMap<>());
        setInitialized(false);
        setFinalized(false);
    }
    //endregion Constructors

    //region Getters/Setters
    public Class<?> getNode() {
        return node;
    }

    public Map<String, PreferenceElement> getElements() {
        return elements;
    }

    public Map<String, String> getInitialValues() {
        return initialValues;
    }

    public void setInitialValues(final Map<String, String> initialValues) {
        this.initialValues = initialValues;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(final boolean initialized) {
        this.initialized = initialized;
    }

    public boolean isFinalized() {
        return finalized;
    }

    public void setFinalized(final boolean finalized) {
        this.finalized = finalized;
    }
    //endregion Getters/Setters

    /**
     * Adds new elements to be managed by this node.
     * If there are initial values set for this node, we will try to set an initial value
     * for each element.
     * @param elements the elements to manage.
     */
    public void manage(final PreferenceElement... elements) {
        for (final PreferenceElement element : elements) {
            final PreferenceElement actual = getElements().get(element.getName());
            if (actual != null) {
                getInitialValues().put(actual.getName(), actual.getValue());
                actual.dispose();
            }

            getElements().put(element.getName(), element);

            if (getInitialValues().containsKey(element.getName())) {
                element.initialize(getInitialValues().get(element.getName()));
            }
        }
    }

    /**
     * Sets the initial values for elements managed for this node.
     * This method should only be called once.
     * @param initialValues the initial values for the elements.
     */
    public void initialize(final Map<String, String> initialValues) throws Exception {
        if (!isInitialized()) {
            throw new Exception();
        }
        setInitialized(true);
        setInitialValues(Objects.requireNonNull(initialValues));
    }

    /**
     * This method should only be called once.
     * @return the final values of all the elements managed by this node.
     */
    public Map<String, String> getFinalValues() throws Exception {
        if (isFinalized()) {
            throw new Exception();
        }

        setFinalized(true);
        final Map<String, String> finalValues = new HashMap<>(getElements().size());

        // Use the values we had stored from initialization
        finalValues.putAll(getInitialValues());

        // Overwrite the initial values with values generated during this session
        for (final PreferenceElement wrapper : getElements().values()) {
            finalValues.put(wrapper.getName(), wrapper.getValue());
        }

        return finalValues;
    }
}

/*
 * Copyright (C) 2019-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.preferences;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import megamek.logging.MMLogger;

/**
 * Represents a group of {@link PreferenceElement}s that are part of the same Class.
 * <p>
 * This class is not thread-safe.
 */
public class PreferencesNode {
    private final static MMLogger logger = MMLogger.create(PreferencesNode.class);

    // region Variable Declarations
    private final Class<?> node;
    private final Map<String, PreferenceElement> elements;
    private Map<String, String> initialValues;
    private boolean initialized;
    private boolean finalized;
    // endregion Variable Declarations

    // region Constructors
    public PreferencesNode(final Class<?> node) {
        this.node = Objects.requireNonNull(node);
        this.elements = new HashMap<>();
        setInitialValues(new HashMap<>());
        setInitialized(false);
        setFinalized(false);
    }
    // endregion Constructors

    // region Getters/Setters
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
    // endregion Getters/Setters

    /**
     * Adds new elements to be managed by this node. If there are initial values set for this node, we will try to set
     * an initial value for each element.
     *
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
                try {
                    element.initialize(getInitialValues().get(element.getName()));
                } catch (Exception ex) {
                    logger.error(ex, "Failed initializing element " + element.getName());
                }
            }
        }
    }

    /**
     * Sets the initial values for elements managed for this node. This method should only be called once.
     *
     * @param initialValues the initial values for the elements.
     *
     * @throws Exception if initialization has already occurred
     */
    public void initialize(final Map<String, String> initialValues) throws Exception {
        if (isInitialized()) {
            throw new Exception();
        }
        setInitialized(true);
        setInitialValues(Objects.requireNonNull(initialValues));
    }

    /**
     * This method should only be called once.
     *
     * @return the final values of all the elements managed by this node.
     *
     * @throws Exception if this method is called a second time
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

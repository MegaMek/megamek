/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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


/**
 * The <code>IPreferenceStore</code> interface represents a mapping of named preferences to values. In case of absence
 * of the value for a given name the default value is returned. If there is no default value for that preference, then
 * predefined default value for the preference type is returned. Predefined default values for the primitive types
 * follows:
 * <ul>
 *  <li><code>boolean</code> = <code>false</code></li>
 *  <li><code>int</code> = <code>0</code></li>
 *  <li><code>long</code> = <code>0</code></li>
 *  <li><code>String</code> = <code>""</code> (the empty string)</li>
 *  <li><code>float</code> = <code>0.0f</code></li>
 *  <li><code>double</code> = <code>0.0</code></li>
 * </ul>
 * A preference change event is reported whenever a preferences current
 * value changes.
 */

package megamek.common.preference;

public interface IPreferenceStore {

    /**
     * The predefined default value for <code>boolean</code> preference
     */
    boolean BOOLEAN_DEFAULT = false;

    /**
     * The predefined default value for <code>int</code> preference
     */
    int INT_DEFAULT = 0;

    /**
     * The predefined default value for <code>long</code> preference
     */
    long LONG_DEFAULT = 0L;

    /**
     * The predefined default value for <code>String</code> preference
     */
    String STRING_DEFAULT = "";

    /**
     * The predefined default value for <code>float</code> preference
     */
    float FLOAT_DEFAULT = 0.0f;

    /**
     * The predefined default value for <code>double</code> preference
     */
    double DOUBLE_DEFAULT = 0.0;

    /**
     * The string representation used for <code>true</code>
     */
    String TRUE = "true";

    /**
     * The string representation used for <code>false</code>
     */
    String FALSE = "false";

    /**
     * Returns the default value for the <code>boolean</code> preference with
     * the given name.
     */
    boolean getDefaultBoolean(String name);

    /**
     * Returns the default value for the <code>int</code> preference with the
     * given name.
     */
    int getDefaultInt(String name);

    /**
     * Returns the default value for the <code>long</code> preference with the
     * given name.
     */
    long getDefaultLong(String name);

    /**
     * Returns the default value for the <code>String</code> preference with
     * the given name.
     */
    String getDefaultString(String name);

    /**
     * Returns the default value for the <code>double</code> preference with
     * the given name.
     */
    double getDefaultDouble(String name);

    /**
     * Returns the default value for the <code>float</code> preference with
     * the given name.
     */
    float getDefaultFloat(String name);

    /**
     * Sets the default value for the <code>boolean</code> preference with the
     * given name.
     */
    void setDefault(String name, boolean value);

    /**
     * Sets the default value for the <code>int</code> preference with the
     * given name.
     */
    void setDefault(String name, int value);

    /**
     * Sets the default value for the <code>long</code> preference with the
     * given name.
     */
    void setDefault(String name, long value);

    /**
     * Sets the default value for the <code>string</code> preference with the
     * given name.
     */
    void setDefault(String name, String value);

    /**
     * Sets the default value for the <code>float</code> preference with the
     * given name.
     */
    void setDefault(String name, float value);

    /**
     * Sets the default value for the <code>double</code> preference with the
     * given name.
     */
    void setDefault(String name, double value);

    /**
     * Returns the value of the <code>boolean</code> preference with the given
     * name. Returns the predefined default value in case if there is no
     * preference with the given name, or if the current value cannot be treated
     * as a boolean.
     */
    boolean getBoolean(String name);

    /**
     * Returns the value of the <code>int</code> preference with the given
     * name. Returns the predefined default value in case if there is no
     * preference with the given name, or if the current value cannot be treated
     * as a int.
     */
    int getInt(String name);

    /**
     * Returns the value of the <code>long</code> preference with the given
     * name. Returns the predefined default value in case if there is no
     * preference with the given name, or if the current value cannot be treated
     * as a long.
     */
    long getLong(String name);

    /**
     * Returns the value of the <code>String</code> preference with the given
     * name. Returns the predefined default value in case if there is no
     * preference with the given name, or if the current value cannot be treated
     * as a String.
     */
    String getString(String name);

    /**
     * Returns the value of the <code>float</code> preference with the given
     * name. Returns the predefined default value in case if there is no
     * preference with the given name, or if the current value cannot be treated
     * as a float.
     */
    float getFloat(String name);

    /**
     * Returns the value of the <code>double</code> preference with the given
     * name. Returns the predefined default value in case if there is no
     * preference with the given name, or if the current value cannot be treated
     * as a double.
     */
    double getDouble(String name);

    /**
     * Sets the current value of the <code>boolean</code> preference with the
     * given name. A preference change event is reported if the current value of
     * the preference changes from its previous value.
     */
    void setValue(String name, boolean value);

    /**
     * Sets the current value of the <code>int</code> preference with the
     * given name. A preference change event is reported if the current value of
     * the preference changes from its previous value.
     */
    void setValue(String name, int value);

    /**
     * Sets the current value of the <code>long</code> preference with the
     * given name. A preference change event is reported if the current value of
     * the preference changes from its previous value.
     */
    void setValue(String name, long value);

    /**
     * Sets the current value of the <code>String</code> preference with the
     * given name. A preference change event is reported if the current value of
     * the preference changes from its previous value.
     */
    void setValue(String name, String value);

    /**
     * Sets the current value of the <code>float</code> preference with the
     * given name. A preference change event is reported if the current value of
     * the preference changes from its previous value.
     */
    void setValue(String name, float value);

    /**
     * Sets the current value of the <code>double</code> preference with the
     * given name. A preference change event is reported if the current value of
     * the preference changes from its previous value.
     */
    void setValue(String name, double value);

    /**
     * Adds a preference change listener to this store.
     *
     * @param listener a preference change listener
     */
    void addPreferenceChangeListener(
          IPreferenceChangeListener listener);

    /**
     * Removes the given listener from this store.
     */
    void removePreferenceChangeListener(
          IPreferenceChangeListener listener);

    /**
     * Just sets the current value of the preference with the given name.
     * <strong>does not fire preference change event</strong>
     */
    void putValue(String name, String value);

    String[] getAdvancedProperties();

    boolean hasProperty(String name);
}

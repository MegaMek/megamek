/*
 * MegaMek - Copyright (C) 2024 - The MegaMek Team
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common.options;

import megamek.common.annotations.Nullable;

import java.util.Collection;
import java.util.Enumeration;

public interface IGameOptions {

    /**
     * Returns a count of all options in this object.
     *
     * @return Option count.
     */
    int count();

    /**
     * Returns a count of all options in this object with the given group key.
     *
     * @param groupKey the group key to filter on. Null signifies to return all options indiscriminately.
     * @return Option count.
     */
    int count(String groupKey);

    /**
     * Returns a string of all the quirk "codes" for this entity, using sep as
     * the separator
     *
     * @param separator The separator to insert between codes, in addition to a space
     */
    String getOptionList(String separator);

    /**
     * Returns a string of all the quirk "codes" for this entity, using sep as
     * the separator, filtering on a specific group key.
     *
     * @param separator The separator to insert between codes, in addition to a space
     * @param groupKey  The group key to use to filter options. Null signifies to return all options indiscriminately.
     */
    String getOptionListString(String separator, String groupKey);

    /**
     * Returns the <code>Enumeration</code> of the option groups in thioptions container.
     *
     * @return <code>Enumeration</code> of the <code>IOptionGroup</code>
     */
    Enumeration<IOptionGroup> getGroups();

    /**
     * Returns the <code>Enumeration</code> of the options in this options container. The order of
     * options is not specified.
     *
     * @return <code>Enumeration</code> of the <code>IOption</code>
     */
    Enumeration<IOption> getOptions();

    /**
     * Returns a collection of all of the options in this options container, regardless of whether they're
     * active/selected or not. Note that this Collection is unmodifiable, but the contained IOptions are not
     * copied, so changing their state will affect this options object.
     *
     * @return A collection containing all IOptions of this options object
     */
    Collection<IOption> getOptionsList();

    /**
     * Returns the UI specific data to allow the user to set the option
     *
     * @param name option name
     * @return UI specific data
     * @see IOptionInfo
     */
    IOptionInfo getOptionInfo(String name);

    /**
     * Returns the option by name or <code>null</code> if there is no such option
     *
     * @param name option name
     * @return the option or <code>null</code> if there is no such option
     */
    @Nullable
    IOption getOption(String name);

    /**
     * Returns the value of the desired option as the <code>boolean</code>
     *
     * @param name option name
     * @return the value of the desired option as the <code>boolean</code>
     */
    boolean booleanOption(String name);

    /**
     * Returns the value of the desired option as the <code>int</code>
     *
     * @param name option name
     * @return the value of the desired option as the <code>int</code>
     */
    int intOption(String name);

    /**
     * Returns the value of the desired option as the <code>float</code>
     *
     * @param name option name
     * @return the value of the desired option as the <code>float</code>
     */
    float floatOption(String name);

    /**
     * Returns the value of the desired option as the <code>String</code>
     *
     * @param name option name
     * @return the value of the desired option as the <code>String</code>
     */
    String stringOption(String name);

    /**
     * Returns the options info object for this options object.
     *
     * @return the options info object for this options object.
     */
    IOptionsInfo getOptionsInfo();
}

/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * 
 *  This program is free software; you can redistribute it and/or modify it 
 *  under the terms of the GNU General Public License as published by the Free 
 *  Software Foundation; either version 2 of the License, or (at your option) 
 *  any later version.
 * 
 *  This program is distributed in the hope that it will be useful, but 
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 *  for more details.
 */

package megamek.common.options;

import java.util.Arrays;

/**
 * Basic option. It's just <code>String</code> name - <code>Object</code>
 * value pair
 */
public interface IBasicOption {

    /**
     * Returns the option name
     * 
     * @return name of the option
     */
    public abstract String getName();

    /**
     * Returns the option value
     * 
     * @return option value
     */
    public abstract Object getValue();

    /** @return True when this Option's name is equal to the given Option name. */
    default boolean is(String otherName) {
        return isAnyOf(otherName);
    }

    /** @return True when this Option's name is equal to at least one of the given Option names. */
    default boolean isAnyOf(String otherName, String... otherNames) {
        return getName().equals(otherName) || Arrays.stream(otherNames).anyMatch(name -> getName().equals(name));
    }

    /** @return True when this Option's name is not equal to any of the given names. */
    default boolean isNoneOf(String otherName, String... otherNames) {
        return !isAnyOf(otherName, otherNames);
    }
}

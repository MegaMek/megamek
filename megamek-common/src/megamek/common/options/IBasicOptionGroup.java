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

import java.util.Enumeration;

/**
 * Basic interface for options group. It only represents the group name, the key
 * which can be an empty <code>String</code> and the <code>Enumeration</code>
 * of the option names in this group
 */
public interface IBasicOptionGroup {

    /**
     * Returns the 'internal'(NON-NLS) name of the group, which is the only ID
     * that is unique in the parent container
     * 
     * @see megamek.common.options.IOptionGroup#getDisplayableName()
     * @return group name
     */
    public abstract String getName();

    /**
     * Returns the the <code>String</code> key that can be used by clients to
     * distinguish option groups by their own criterion
     * 
     * @return group key which can be an empty <code>String</code>
     */
    public abstract String getKey();

    /**
     * @return the <code>Enumeration</code> of the names of the options that
     *         in this group
     */
    public abstract Enumeration<String> getOptionNames();

}

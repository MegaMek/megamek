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

}

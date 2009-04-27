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
 * Interface that represents the "static" (common to all instances) information
 * about the Options container
 */
public interface IOptionsInfo {

    /**
     * Returns the <code>IOptionInfo</code> for the specified option
     * 
     * @param name option name
     * @return the <code>IOptionInfo</code> for the specified option
     * @see IOptionInfo
     */
    public abstract IOptionInfo getOptionInfo(String name);

    /**
     * Returns the <code>Enumeration</code> of the
     * <code>IBasicOptionGroup</code>
     * 
     * @return the <code>Enumeration</code> of the
     *         <code>IBasicOptionGroup</code>
     */
    public abstract Enumeration<IBasicOptionGroup> getGroups();

}

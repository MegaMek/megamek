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
 * Public interface for the group of the options. It extends the
 * <code>IBasicOptionGroup</code> and adds the ability to query the options
 * that belongs to this group. Instances of this interface are ONLY returned as
 * the members of the <code>Enumeration</code> returned by the
 * megamek.common.options.IOptions#getGroups()
 * 
 * @see IOptions#getGroups()
 */
public interface IOptionGroup extends IBasicOptionGroup {

    /**
     * Returns the user friendly NLS dependent name suitable for displaying in
     * the options editor dialogs etc.
     * 
     * @return displayable name
     */
    public abstract String getDisplayableName();

    /**
     * @return the <code>Enumeration</code> of the <code>IOption</code>
     * @see IOptions#getGroups()
     */
    public abstract Enumeration<IOption> getOptions();
}

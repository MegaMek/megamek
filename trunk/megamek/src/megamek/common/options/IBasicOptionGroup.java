/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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
 * Basic interface for options group. It only represents 
 * the group name, the optional key and the <code>Enumeration</code>
 * of the option names in this group    
 */

interface IBasicOptionGroup {
    
    /**
     * 
     * @return group name
     */
    public abstract String getName();

    /**
     *  
     * @return optional group key which can be empty string
     */
    public abstract String getKey();
        
    /**
     * 
     * @return the <code>Enumeration</code> of the names of the optiongs 
     * that belongs to this group
     */
    public abstract Enumeration getOptionNames();

}

/*
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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

package megamek.common;

/**
 * Implementing classes perform a test to determine if an <code>Entity</code>
 * meets the selection criteria.
 * 
 * @author James Damour (suvarov454@users.sourceforge.net)
 */
public interface EntitySelector {
    /**
     * Determine if the given <code>Entity</code> meets the selection
     * criteria.
     * 
     * @param entity the <code>Entity</code> being tested.
     * @return <code>true</code> if the entity meets the criteria.
     */
    public boolean accept(Entity entity);
}

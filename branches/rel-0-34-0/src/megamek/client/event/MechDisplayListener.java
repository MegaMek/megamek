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

package megamek.client.event;

/**
 * Classes which implement this interface provide methods that deal with the
 * events that are generated when the MechDisplay is changed.
 * <p>
 * After creating an instance of a class that implements this interface it can
 * be added to a Board using the <code>addMechDisplayListener</code> method
 * and removed using the <code>removeMechDisplayListener</code> method. When
 * MechDisplay is changed the appropriate method will be invoked.
 * </p>
 * 
 * @see MechDisplayListenerAdapter
 * @see MechDisplayEvent
 */
public interface MechDisplayListener extends java.util.EventListener {

    /**
     * Sent when user selects a weapon in the weapon panel.
     * 
     * @param b an event
     */
    public void WeaponSelected(MechDisplayEvent b);
}

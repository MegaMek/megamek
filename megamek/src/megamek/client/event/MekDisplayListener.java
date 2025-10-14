/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.client.event;

/**
 * Classes which implement this interface provide methods that deal with the events that are generated when the
 * MekDisplay is changed.
 * <p>
 * After creating an instance of a class that implements this interface it can be added to a Board using the
 * <code>addMekDisplayListener</code> method and removed using the <code>removeMekDisplayListener</code> method. When
 * MekDisplay is changed the appropriate method will be invoked.
 * </p>
 *
 * @see MekDisplayListenerAdapter
 * @see MekDisplayEvent
 */
public interface MekDisplayListener extends java.util.EventListener {

    /**
     * Sent when user selects a weapon in the weapon panel.
     *
     * @param b an event
     */
    void weaponSelected(MekDisplayEvent b);
}

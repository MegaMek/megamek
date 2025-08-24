/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2005-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.options;

import java.util.Enumeration;

/**
 * Public interface for the group of the options. It extends the
 * <code>IBasicOptionGroup</code> and adds the ability to query the options
 * that belongs to this group. Instances of this interface are ONLY returned as the members of the
 * <code>Enumeration</code> returned by the AbstractOptions#getGroups()
 *
 * @see AbstractOptions#getGroups()
 */
public interface IOptionGroup extends IBasicOptionGroup {

    /**
     * Returns the user-friendly NLS dependent name suitable for displaying in the options editor dialogs etc.
     *
     * @return displayable name
     */
    String getDisplayableName();

    /**
     * @return the <code>Enumeration</code> of the <code>IOption</code>
     *
     * @see AbstractOptions#getGroups()
     */
    Enumeration<IOption> getOptions();

    /**
     * @return the <code>Enumeration</code> of the <code>IOption</code> sorted in alpha-numerically ascending order.
     *
     * @see AbstractOptions#getGroups()
     */
    Enumeration<IOption> getSortedOptions();
}

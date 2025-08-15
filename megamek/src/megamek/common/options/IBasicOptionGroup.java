/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
 * Basic interface for options group. It only represents the group name, the key which can be an empty
 * <code>String</code> and the <code>Enumeration</code> of the option names in this group
 */
public interface IBasicOptionGroup {

    /**
     * Returns the 'internal'(NON-NLS) name of the group, which is the only ID that is unique in the parent container
     *
     * @return group name
     *
     * @see megamek.common.options.IOptionGroup#getDisplayableName()
     */
    String getName();

    /**
     * Returns the <code>String</code> key that can be used by clients to distinguish option groups by their own
     * criterion
     *
     * @return group key which can be an empty <code>String</code>
     */
    String getKey();

    /**
     * @return the <code>Enumeration</code> of the names of the options that in this group
     */
    Enumeration<String> getOptionNames();
}

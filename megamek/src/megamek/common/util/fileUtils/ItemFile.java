/*
 * Copyright (C) 2004 - Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.util.fileUtils;

import java.awt.Image;

/**
 * This interface represents a categorizable file.
 *
 * @author James Damour
 * @since January 18, 2004
 */
public abstract class ItemFile {
    protected Object item = null; // allows us to cache the item

    /**
     * Get the item for this file.
     *
     * @throws Exception if there's any error getting the item.
     */
    public abstract Object getItem() throws Exception;

    protected boolean isNullOrEmpty() {
        return (item == null) || ((item instanceof Image)
              && (((Image) item).getWidth(null) < 0) || (((Image) item).getHeight(null) < 0));
    }
}

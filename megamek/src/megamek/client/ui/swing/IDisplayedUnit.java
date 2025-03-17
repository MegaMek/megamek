/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 */

package megamek.client.ui.swing;

import megamek.common.Entity;
import megamek.common.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

public interface IDisplayedUnit {

    /**
     * @return The unit currently shown in the Unit Display. Note: This can be a
     *         another unit than the one that
     *         is selected to move or fire.
     */
    @Nullable Entity getDisplayedUnit();

    void setDisplayedUnit(@Nullable Entity unit);

    default Collection<Entity> getSelectedUnits() {
        return Collections.singleton(getDisplayedUnit());
    }
}

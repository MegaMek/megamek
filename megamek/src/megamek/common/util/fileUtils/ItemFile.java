/*
 * Copyright (C) 2004 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.util.fileUtils;

import java.awt.*;

/**
 * This interface represents a categorizable file.
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

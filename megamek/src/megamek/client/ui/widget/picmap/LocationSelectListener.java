/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.widget.picmap;

/**
 * Receives the location a user picked out of an armor diagram, such as by double-clicking a unit location in the
 * unit display or the damage editor.
 */
public interface LocationSelectListener {

    /**
     * Called when the user selects a unit location in an armor diagram.
     *
     * @param location the index of the selected location, as used by {@link megamek.common.units.Entity}
     */
    void locationSelected(int location);

    /**
     * Whether a single click selects a location, rather than a double click. The unit display uses a double click,
     * because a single click there would switch tabs whenever the diagram was clicked. A diagram whose whole
     * purpose is picking a location, such as the one in the damage editor, selects on a single click.
     *
     * @return {@code true} to be told about single clicks as well
     */
    default boolean selectsOnSingleClick() {
        return false;
    }
}

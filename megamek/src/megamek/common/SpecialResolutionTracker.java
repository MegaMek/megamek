/*
 * MegaMek - Copyright (C) 2019 The MegaMek Team
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
 * Class that tracks whether this shot was fired using a weapon with special to-hit handling.
 * Allows this waa to bypass all the standard to-hit modifier checks
 */
public class SpecialResolutionTracker {
    private boolean specialResolution;
    
    public boolean isSpecialResolution() {
        return specialResolution;
    }

    public void setSpecialResolution(boolean state) {
        specialResolution = state;
    }
}

/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
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

import java.io.Serializable;

/**
 * This is a support vehicle VTOL
 * @author beerockxs
 */
public class SupportVTOL extends VTOL implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 2771230410747098997L;
    private int barRating;

    public void setBARRating(int rating) {
        barRating = rating;
    }

    /*
     * (non-Javadoc)
     * @see megamek.common.Entity#getBARRating()
     */
    @Override
    public int getBARRating() {
        return barRating;
    }

    /*
     * (non-Javadoc)
     * @see megamek.common.Entity#hasBARArmor()
     */
    @Override
    public boolean hasBARArmor() {
        return true;
    }

    /*
     * (non-Javadoc)
     * @see megamek.common.Entity#hasArmoredChassis()
     */
    @Override
    public boolean hasArmoredChassis() {
        for (Mounted misc : miscList) {
            if (misc.getType().hasFlag(MiscType.F_ARMORED_CHASSIS)) {
                return true;
            }
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * @see megamek.common.Entity#getTotalCommGearTons()
     */
    @Override
    public int getTotalCommGearTons() {
        return getExtraCommGearTons();
    }
}

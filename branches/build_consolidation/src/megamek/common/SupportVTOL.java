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
    private int[] barRating;

    public SupportVTOL() {
        super();
        barRating = new int[locations()];
    }

    public void setBARRating(int rating, int loc) {
        barRating[loc] = rating;
    }

    public void setBARRating(int rating) {
        for (int i = 0; i < locations(); i++) {
            barRating[i] = rating;
        }
    }


    /*
     * (non-Javadoc)
     * @see megamek.common.Entity#getBARRating()
     */
    @Override
    public int getBARRating(int loc) {
        return barRating[loc];
    }

    /*
     * (non-Javadoc)
     * @see megamek.common.Entity#hasBARArmor()
     */
    @Override
    public boolean hasBARArmor(int loc) {
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

    @Override
    public int getBattleForceSize() {
        //The tables are on page 356 of StartOps
        if (getWeight() < 5) {
            return 1;
        }
        if (getWeight() < 30) {
            return 2;
        }
        return 3;
    }

    public float getBaseEngineValue() {
        if (getWeight() < 5) {
            return 0.002f;
        } else if (!isSuperHeavy()) {
            return 0.0025f;
        } else {
            return 0.004f;
        }
    }

    public float getBaseChassisValue() {
        if (getWeight() < 5) {
            return 0.2f;
        } else if (!isSuperHeavy()) {
            return 0.25f;
        } else {
            return 0.3f;
        }
    }

    @Override
    public int getTotalSlots() {
        return 5 + (int) Math.floor(getWeight() / 10);
    }
 }

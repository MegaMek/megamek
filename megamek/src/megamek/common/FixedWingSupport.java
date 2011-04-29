/*
 * MegaAero - Copyright (C) 2010 Jason Tighe This program is free software; you
 * can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
/*
 * Created on 10/31/2010
 */
package megamek.common;

/**
 * @author Jason Tighe
 */
public class FixedWingSupport extends ConvFighter {


    /**
     *
     */
    private static final long serialVersionUID = 347113432982248518L;


    public static final int LOC_BODY = 5;

    private static String[] LOCATION_ABBRS =
        { "NOS", "LWG", "RWG", "AFT", "WNG", "BOD" };
    private static String[] LOCATION_NAMES =
        { "Nose", "Left Wing", "Right Wing", "Aft", "Wings", "Body" };
    private int[] barRating;

    public FixedWingSupport() {
        super();
        damThresh = new int[] { 0, 0, 0, 0, 0, 0 };
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
     *
     * @see megamek.common.Entity#getBARRating()
     */
    @Override
    public int getBARRating(int loc) {
        return barRating[loc];
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#hasBARArmor()
     */
    @Override
    public boolean hasBARArmor(int loc) {
        return true;
    }

    @Override
    public String[] getLocationAbbrs() {
        return LOCATION_ABBRS;
    }

    @Override
    public String[] getLocationNames() {
        return LOCATION_NAMES;
    }

    @Override
    public int locations() {
        return 6;
    }

    @Override
    public void autoSetSI() {
        initializeSI(getOriginalWalkMP());
    }


    @Override
    public boolean isVSTOL() {
        if (hasWorkingMisc(MiscType.F_VSTOL_CHASSIS, -1)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isSTOL() {
        if (hasWorkingMisc(MiscType.F_STOL_CHASSIS, -1)) {
            return true;
        }
        return false;
    }
    
    @Override
    public int getBattleForceSize() {
        //The tables are on page 356 of StartOps
        if (getWeight() < 5) {
            return 1;
        }
        if (getWeight() < 100) {
            return 2;
        }

        return 3;
    }
}
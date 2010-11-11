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
    private int barRating;

    public void setBARRating(int rating) {
        barRating = rating;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#getBARRating()
     */
    @Override
    public int getBARRating() {
        return barRating;
    }

    /*
     * (non-Javadoc)
     *
     * @see megamek.common.Entity#hasBARArmor()
     */
    @Override
    public boolean hasBARArmor() {
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


}
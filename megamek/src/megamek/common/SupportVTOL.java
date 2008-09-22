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
    private boolean armoredChassisAndControl;
    
    public void setBARRating(int rating) {
        barRating = rating;
    }
    
    public int getBARRating() {
        return barRating;
    }

    public boolean hasArmoredChassisAndControl() {
        return armoredChassisAndControl;
    }

    public void setArmoredChassisAndControl(boolean armoredChassisAndControl) {
        this.armoredChassisAndControl = armoredChassisAndControl;
    }
    
    /*
     * (non-Javadoc)
     * @see megamek.common.Entity#getTotalCommGearTons()
     */
    public int getTotalCommGearTons() {
        return getExtraCommGearTons();
    }
}

/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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

/*
 * LosEffects.java
 *
 * Created on October 14, 2002, 11:19 PM
 */

package megamek.common;

/**
 * Keeps track of the cumulative effects of intervening terrain on LOS
 *
 * @author  Ben
 */
public class LosEffects {
    boolean blocked = false;
    int lightWoods = 0;
    int heavyWoods = 0;
    int smoke = 0;
    boolean targetCover = false;  // that means partial cover
    boolean attackerCover = false;  // ditto
    
    /** Creates a new instance of LosEffects */
    public LosEffects() {
        ;
    }
    
    public void add(LosEffects other) {
        this.blocked |= other.blocked;
        this.lightWoods += other.lightWoods;
        this.heavyWoods += other.heavyWoods;
        this.smoke += other.smoke;
        this.targetCover |= other.targetCover;
        this.attackerCover |= other.attackerCover;
    }
}

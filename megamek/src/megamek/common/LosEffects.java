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
    
    /** Getter for property targetCover.
     * @return Value of property targetCover.
     */
    public boolean isTargetCover() {
        return targetCover;
    }
    
    /** Setter for property targetCover.
     * @param targetCover New value of property targetCover.
     */
    public void setTargetCover(boolean targetCover) {
        this.targetCover = targetCover;
    }
    
    /** Getter for property attackerCover.
     * @return Value of property attackerCover.
     */
    public boolean isAttackerCover() {
        return attackerCover;
    }
    
    /** Setter for property attackerCover.
     * @param attackerCover New value of property attackerCover.
     */
    public void setAttackerCover(boolean attackerCover) {
        this.attackerCover = attackerCover;
    }
    
}

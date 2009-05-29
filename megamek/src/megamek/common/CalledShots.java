/* MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
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
 * Some static methods for called shots
 */
public class CalledShots {

    //locations for called shots
    public static final int CALLED_HIGH = 0;
    public static final int CALLED_LOW = 1;
    public static final int CALLED_LEFT = 2;
    public static final int CALLED_RIGHT = 3;
    public static final int CALLED_NUM = 4;
    
    private static final String[] calledLocNames = {"high", "low", "left", "right"};
    
    public static String getCalledName(int i) {
        if(i >= CALLED_NUM) {
            return "Unknown";
        }
        return calledLocNames[i];
    }
    
    public static String[] getCalledLocations() {
        return calledLocNames;
    }
    
    public static boolean[] getEnabledLocations(Targetable target, int partialCover) {
        boolean[] enabled = new boolean[CALLED_NUM];
        
        for(int call = 0; call < CALLED_NUM; call++) {
            if(!(target instanceof Entity)) {
                enabled[call] = false;
                continue;
            }
            Entity te = (Entity)target;
            if(te instanceof Infantry || te instanceof Protomech) {
                enabled[call] = false;
                continue;
            }
            //only meks can be high high or low
            if(!(te instanceof Mech) && (call == CALLED_HIGH || call == CALLED_LOW)) {
                enabled[call] = false;
                continue;
            }
            //deal with partial cover
            //TODO: this doesn't seem to be working
            if (call == CALLED_LOW & (partialCover & LosEffects.COVER_HORIZONTAL) != 0) {
                enabled[call] = false;
                continue;
            }
            enabled[call] = true;
            
        }
        
        return enabled;
    }
}
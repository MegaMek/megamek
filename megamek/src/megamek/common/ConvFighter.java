/*
* MegaAero - Copyright (C) 2007 Jay Lawson
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
 * Created on Jun 12, 2008
 *
 */
package megamek.common;

import java.io.Serializable;

/**
 * @author Jay Lawson
 */
public class ConvFighter extends Aero implements Serializable {
    
    /**
     * 
     */
    private static final long serialVersionUID = 6297668284292929409L;

    public boolean doomedInVacuum() {
        return true;
    }
    
    public boolean doomedInSpace() {
        return true;
    }  
    
    public int getHeatCapacity() {
        return 999;
    }
    
    public int getFuelUsed(int thrust) {
        int used = (thrust + Math.max(thrust - getWalkMP(), 0));
        if(game.getOptions().booleanOption("air_breathers") && !getEngine().isFusion()) {
            used = (int)Math.floor(used / 2.0);
        }
        return used;
    }
    
    public double getBVTypeModifier() {
        return 1.1;
    }
    
}
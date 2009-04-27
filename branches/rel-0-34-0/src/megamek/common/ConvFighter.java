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
        int overThrust =  Math.max(thrust - getWalkMP(), 0);
        int safeThrust = thrust - overThrust;
        int used = safeThrust + 2 * overThrust;
        if(!getEngine().isFusion()) {
            used = (int)Math.floor(safeThrust * 0.5) + overThrust;
        } else if(game.getOptions().booleanOption("stratops_conv_fusion_bonus")) {
            used = (int)Math.floor(safeThrust * 0.5) + 2 * overThrust;
        }
        return used;
    }
    
    public double getBVTypeModifier() {
        return 1.1;
    }
    
}
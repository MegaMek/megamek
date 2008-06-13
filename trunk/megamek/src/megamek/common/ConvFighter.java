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

/**
 * @author Jay Lawson
 */
public class ConvFighter extends Aero {
    
    private static final long serialVersionUID = -9013512155929642136L;
    
    public boolean doomedInVacuum() {
        return true;
    }
    
    public boolean doomedInSpace() {
        return true;
    }  
    
    public int getHeatCapacity() {
        return 999;
    }
}
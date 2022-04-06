/*
 * MegaMek - Copyright (C) 2021 - The MegaMek Team
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

package megamek.common.actions;

/**
 * Contains information necessary to carry out a "blood stalker" action 
 */
public class ActivateBloodStalkerAction extends AbstractEntityAction {    
    private int targetID;
    
    public int getTargetID() {
        return targetID;
    }
    
    public void setTargetID(int value) {
        targetID = value;
    }
    
    public ActivateBloodStalkerAction(int entityID, int targetID) {
        super(entityID);
        this.targetID = targetID;
    }
}

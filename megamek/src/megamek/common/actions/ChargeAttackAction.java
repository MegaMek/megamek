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
 * ChargeAttackAction.java
 *
 * Created on March 12, 2002, 3:23 PM
 */

package megamek.common.actions;

import megamek.common.*;

/**
 * Represents one unit charging another.  Stores information about where the 
 * target is supposed to be for the charge to be successful, as well as normal
 * attack info.
 *
 * @author  Ben Mazur
 */
public class ChargeAttackAction extends DisplacementAttackAction {
    
    private Coords targetPos;

    /** Creates new ChargeAttackAction */
    public ChargeAttackAction(int entityId, int targetId, Coords targetPos) {
        super(entityId, targetId, targetPos);
    }

    public ChargeAttackAction(int entityId, int targetType, int targetId, Coords targetPos) {
        super(entityId, targetType, targetId, targetPos);
    }

}

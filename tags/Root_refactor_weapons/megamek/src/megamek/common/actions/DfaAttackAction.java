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

/*
 * DfaAttackAction.java
 *
 * Created on March 16, 2002, 11:43 AM
 */

package megamek.common.actions;

import megamek.common.*;

/**
 *
 * @author  Ben
 * @version 
 */
public class DfaAttackAction extends DisplacementAttackAction {

    /** Creates new DfaAttackAction */
    public DfaAttackAction(int entityId, int targetId, Coords targetPos) {
        super(entityId, targetId, targetPos);
    }

    public DfaAttackAction(int entityId, int targetType, int targetId, Coords targetPos) {
        super(entityId, targetType, targetId, targetPos);
    }

}

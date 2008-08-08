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
 * DisplacementAttackAction.java
 *
 * Created on May 23, 2002, 12:05 PM
 */

package megamek.common.actions;

import megamek.common.Coords;

/**
 * @author Ben
 * @version
 */
public class DisplacementAttackAction extends AbstractAttackAction {

    /**
     * 
     */
    private static final long serialVersionUID = -1713221946987876208L;
    private Coords targetPos;

    /** Creates new DisplacementAttackAction */
    public DisplacementAttackAction(int entityId, int targetId, Coords targetPos) {
        super(entityId, targetId);
        this.targetPos = targetPos;
    }

    public DisplacementAttackAction(int entityId, int targetType, int targetId,
            Coords targetPos) {
        super(entityId, targetType, targetId);
        this.targetPos = targetPos;
    }

    public Coords getTargetPos() {
        return targetPos;
    }

    public void setTargetPos(Coords targetPos) {
        this.targetPos = targetPos;
    }

}

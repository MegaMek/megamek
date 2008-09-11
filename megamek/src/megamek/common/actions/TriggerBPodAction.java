/**
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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

import megamek.common.Entity;

public class TriggerBPodAction extends AbstractEntityAction {

    /**
     * 
     */
    private static final long serialVersionUID = -9087658958702066030L;
    /** Save the equipment ID of the Anti-BA Pod being triggered. */
    int podId;
    Entity target;

    /**
     * Trigger the indicated AP Pod on the entity with the given entity ID.
     * 
     * @param entityId the <code>int</code> ID of the triggering entity.
     * @param equipId the <code>int</code> ID of the triggered AP Pod.
     */
    public TriggerBPodAction(int entityId, int equipId, Entity target) {
        super(entityId);
        this.podId = equipId;
        this.target = target;
    }

    /**
     * Get the equipment ID of the AP Pod being triggered.
     * 
     * @return the <code>int</code> equipment ID of the AP Pod.
     */
    public int getPodId() {
        return this.podId;
    }
    
    public Entity getTarget() {
        return target;
    }
}

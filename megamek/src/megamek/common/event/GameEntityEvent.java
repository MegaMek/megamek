/*
* MegaMek -
* Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
* Copyright (C) 2018 The MegaMek Team
*
* This program is free software; you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2 of the License, or (at your option) any later
* version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
*/

package megamek.common.event;

import megamek.common.Entity;

/**
 * Instances of descendant classes are sent as a result of Game changes related
 * to entities such as addind/removing/changing
 *
 * @see GameEntityChangeEvent
 * @see GameEntityNewEvent
 * @see GameListener
 */
public abstract class GameEntityEvent extends GameEvent {

    /**
     *
     */
    private static final long serialVersionUID = -2152420685366625391L;
    protected Entity entity;

    public GameEntityEvent(Object source) {
        super(source);
        this.entity = null;
    }

    /**
     * Constructs new GameEntityEvent
     *
     * @param source
     * @param entity
     */
    public GameEntityEvent(Object source, Entity entity) {
        super(source);
        this.entity = entity;
    }

    /**
     * @return the entity.
     */
    public Entity getEntity() {
        return entity;
    }

}

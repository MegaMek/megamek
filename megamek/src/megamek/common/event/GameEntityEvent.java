/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.event;

import megamek.common.Entity;

/**
 * Instances of descendant classes are sent as a result of Game changes related to entities such as
 * addind/removing/changing
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

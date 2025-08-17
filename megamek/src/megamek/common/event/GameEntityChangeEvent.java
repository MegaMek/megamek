/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

import java.util.Vector;

import megamek.common.units.Entity;
import megamek.common.units.UnitLocation;

/**
 * Instances of this class are sent game entity is changed
 *
 * @see GameListener
 */
public class GameEntityChangeEvent extends GameEntityEvent {
    private static final long serialVersionUID = -7241101183271789555L;
    protected Vector<UnitLocation> movePath;
    protected Entity oldEntity;

    /**
     * Constructs new GameEntityChangeEvent
     *
     * @param source
     * @param entity
     */
    public GameEntityChangeEvent(final Object source, final Entity entity) {
        this(source, entity, null);
    }

    /**
     * Constructs new GameEntityChangeEvent
     *
     * @param source
     * @param entity
     * @param movePath
     */
    public GameEntityChangeEvent(final Object source, final Entity entity,
          final Vector<UnitLocation> movePath) {
        super(source, entity);
        oldEntity = null;
        this.movePath = movePath;
    }

    /**
     * Constructs new GameEntityChangeEvent, storing the entity prior to changes. This old entity may be needed in
     * certain cases, like when a Dropship is taking off, since some of the old state is important.
     *
     * @param source
     * @param entity
     * @param movePath
     */
    public GameEntityChangeEvent(final Object source, final Entity entity,
          final Vector<UnitLocation> movePath, Entity oldEntity) {
        super(source, entity);
        this.oldEntity = oldEntity;
        this.movePath = movePath;
    }

    /**
     * @return the movePath.
     */
    public Vector<UnitLocation> getMovePath() {
        return movePath;
    }

    public Entity getOldEntity() {
        return oldEntity;
    }

    @Override
    public String toString() {
        if (movePath == null) {
            return "There is nothing to move!";
        }
        var entity = getEntity();
        var movePathIsEmpty = movePath.isEmpty();
        if (entity != null && !movePathIsEmpty && movePath.lastElement().coords() != null) {
            return entity + " moved to " + movePath.lastElement().coords().toFriendlyString();
        } else if (entity != null) {
            return entity + " probably deployed.";
        } else {
            return "There is nothing to move!";
        }
    }

    @Override
    public void fireEvent(GameListener gl) {
        gl.gameEntityChange(this);
    }

    @Override
    public String getEventName() {
        return "Entity Change";
    }
}

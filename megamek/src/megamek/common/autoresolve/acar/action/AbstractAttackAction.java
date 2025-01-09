/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.autoresolve.acar.action;

public abstract class AbstractAttackAction implements AttackAction {

    private final int entityId;
    private final int targetId;

    public AbstractAttackAction(int entityId, int targetId) {
        this.entityId = entityId;
        this.targetId = targetId;
    }

    @Override
    public int getEntityId() {
        return entityId;
    }

    @Override
    public int getTargetId() {
        return targetId;
    }

    @Override
    public String toString() {
        return "[" + getClass().getSimpleName() + "]: Unit ID " + entityId + "; Target ID " + targetId;
    }
}

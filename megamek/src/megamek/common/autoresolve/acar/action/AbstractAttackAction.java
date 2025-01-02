/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 *  This file is part of MekHQ.
 *
 *  MekHQ is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MekHQ is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */

package megamek.common.autoresolve.acar.action;

import mekhq.campaign.autoresolve.acar.action.AttackAction;

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

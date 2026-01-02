/*
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
package megamek.common.actions;

import java.io.Serial;

import megamek.common.game.Game;
import megamek.common.units.Entity;

/**
 * Action to INITIATE new infantry vs. infantry combat in a building.
 * Only valid when NO combat currently exists in the target building.
 */
public class InitiateInfantryCombatAction extends InfantryCombatAction {
    @Serial
    private static final long serialVersionUID = -1234567890123456790L;

    /**
     * Creates a new initiate infantry combat action.
     *
     * @param entityId the attacking infantry entity ID
     * @param targetId the target entity ID (AbstractBuildingEntity)
     */
    public InitiateInfantryCombatAction(int entityId, int targetId) {
        super(entityId, targetId, false);
    }

    @Override
    public String toSummaryString(Game game) {
        Entity target = game.getEntity(getTargetId());
        String targetName = (target != null) ? target.getDisplayName() : "Unknown";
        return "Initiate combat at " + targetName;
    }
}

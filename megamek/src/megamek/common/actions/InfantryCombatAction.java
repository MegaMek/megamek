/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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

import megamek.common.game.Game;
import megamek.common.units.Entity;

/**
 * Represents an infantry vs. infantry boarding combat action.
 *
 * <p>Applicable to:
 * <ul>
 *   <li>Building clearing operations (AbstractBuildingEntity)</li>
 *   <li>Naval boarding actions (Large Naval Vessels) - future</li>
 * </ul>
 *
 * <p>Uses TOAR Infantry vs. Infantry rules with Marine Points Score calculations.</p>
 */
public class InfantryCombatAction extends AbstractEntityAction {

    /**
     * The target entity ID (AbstractBuildingEntity or vessel).
     */
    private final int targetId;

    /**
     * True if this action represents a withdrawal from combat (attackers only).
     */
    private boolean isWithdrawing;

    /**
     * Creates a new infantry boarding combat action.
     *
     * @param entityId the attacking infantry entity ID
     * @param targetEntityId the target entity ID (AbstractBuildingEntity or vessel)
     */
    public InfantryCombatAction(int entityId, int targetEntityId) {
        this(entityId, targetEntityId, false);
    }

    /**
     * Creates a new infantry boarding combat action with withdrawal option.
     *
     * @param entityId the attacking infantry entity ID
     * @param targetEntityId the target entity ID (AbstractBuildingEntity or vessel)
     * @param isWithdrawing true if withdrawing from combat
     */
    public InfantryCombatAction(int entityId, int targetEntityId, boolean isWithdrawing) {
        super(entityId);
        this.targetId = targetEntityId;
        this.isWithdrawing = isWithdrawing;
    }

    public int getTargetId() {
        return targetId;
    }

    public boolean isWithdrawing() {
        return isWithdrawing;
    }

    public void setWithdrawing(boolean withdrawing) {
        isWithdrawing = withdrawing;
    }

    @Override
    public String toSummaryString(Game game) {
        Entity target = game.getEntity(getTargetId());
        String targetName = (target != null) ? target.getDisplayName() : "Unknown";

        if (isWithdrawing) {
            return "Withdraw from " + targetName;
        } else {
            return "Board " + targetName;
        }
    }
}

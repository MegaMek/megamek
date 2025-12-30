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
 * BattleMek, `Mek and AeroTek are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * BattleMekWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.actions;

import java.io.Serial;

import megamek.common.ToHitData;
import megamek.common.game.Game;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;

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
public class InfantryCombatAction extends AbstractAttackAction {
    @Serial
    private static final long serialVersionUID = -1234567890123456789L;

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
        super(entityId, targetEntityId);
        this.isWithdrawing = isWithdrawing;
    }

    public boolean isWithdrawing() {
        return isWithdrawing;
    }

    public void setWithdrawing(boolean withdrawing) {
        isWithdrawing = withdrawing;
    }

    /**
     * Validates whether this action is possible.
     *
     * @param game the current game
     * @return ToHitData indicating if action is possible
     */
    public ToHitData toHit(Game game) {
        return toHit(game, getEntityId(), getTargetId(), isWithdrawing);
    }

    /**
     * Static validation method for infantry boarding combat actions.
     *
     * @param game the current game
     * @param attackerId the attacking infantry ID
     * @param targetId the target entity ID
     * @param isWithdrawing true if this is a withdrawal action
     * @return ToHitData indicating if action is possible
     */
    public static ToHitData toHit(Game game, int attackerId, int targetId, boolean isWithdrawing) {
        final Entity attacker = game.getEntity(attackerId);
        final Entity target = game.getEntity(targetId);

        // Validate attacker exists and is infantry
        if (attacker == null) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Attacker does not exist");
        }
        if (!(attacker instanceof Infantry)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Attacker must be infantry");
        }

        // Validate target exists and is an AbstractBuildingEntity (for now)
        // TODO: Add support for large naval vessels when naval boarding is implemented
        if (target == null) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Target does not exist");
        }
        if (!(target instanceof AbstractBuildingEntity)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                  "Target must be an advanced building");
        }

        final Infantry inf = (Infantry) attacker;

        // If withdrawing, must already be in combat as attacker
        if (isWithdrawing) {
            if (inf.getInfantryCombatTargetId() != targetId) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                      "Not engaged in combat with this target");
            }
            if (!inf.isInfantryCombatAttacker()) {
                return new ToHitData(TargetRoll.IMPOSSIBLE,
                      "Only attackers can withdraw from boarding combat");
            }
            return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS,
                  "Withdrawing from boarding combat");
        }

        // If already in combat, can't initiate new combat
        if (inf.getInfantryCombatTargetId() != Entity.NONE) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                  "Already engaged in boarding combat");
        }

        // Must be in same hex as target
        if (!attacker.getPosition().equals(target.getPosition())) {
            return new ToHitData(TargetRoll.IMPOSSIBLE,
                  "Must be in same hex as target");
        }

        // TODO: Check if there are enemy infantry in the target to fight

        return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS,
              "Initiating boarding combat");
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

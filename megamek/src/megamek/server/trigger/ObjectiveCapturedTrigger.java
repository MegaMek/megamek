/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

package megamek.server.trigger;

import java.util.Objects;

import megamek.common.annotations.Nullable;
import megamek.common.equipment.ICarryable;
import megamek.common.equipment.ObjectiveMarker;
import megamek.common.game.IGame;
import megamek.common.game.InGameObject;
import megamek.common.interfaces.IEntityRemovalConditions;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * This Trigger reacts once the named Mobile Objective has been captured: carried off the battlefield by a unit that
 * fled (Standard Missions, Objectives - Mobile Objectives: a carrier departing a legal battlefield edge takes the
 * objective with it). When the player name is blank, a capture by any side triggers; otherwise only by the given
 * player's units. Note that this trigger reacts every time it is checked once the objective is captured; use
 * {@link OnceTrigger} to limit it.
 *
 * @param objectiveName The exact name of the objective marker
 * @param playerName    The player whose unit must have captured the objective, or blank/{@code null} for any
 */
public record ObjectiveCapturedTrigger(String objectiveName, String playerName) implements Trigger {

    public ObjectiveCapturedTrigger(String objectiveName, @Nullable String playerName) {
        this.objectiveName = objectiveName;
        this.playerName = Objects.requireNonNullElse(playerName, "");
    }

    private static final MMLogger LOGGER = MMLogger.create(ObjectiveCapturedTrigger.class);

    @Override
    public boolean isTriggered(IGame game, TriggerSituation event) {
        for (InGameObject inGameObject : game.getGraveyard()) {
            if (!(inGameObject instanceof Entity fledEntity)
                  || (fledEntity.getRemovalCondition() != IEntityRemovalConditions.REMOVE_IN_RETREAT)) {
                continue;
            }
            if (!playerName.isBlank() && ((fledEntity.getOwner() == null)
                  || !playerName.equals(fledEntity.getOwner().getName()))) {
                continue;
            }
            for (ICarryable carriedObject : fledEntity.getDistinctCarriedObjects()) {
                if ((carriedObject instanceof ObjectiveMarker marker)
                      && objectiveName.equals(marker.generalName())
                      && !marker.isDestroyed()) {
                    LOGGER.debug("[VictoryTrigger] {}: TRIGGERED (carried off the battlefield by {})",
                          this, fledEntity.getShortName());
                    return true;
                }
            }
        }
        LOGGER.trace("[VictoryTrigger] {}: not triggered - no fled unit carried the objective off", this);
        return false;
    }

    @Override
    public String toString() {
        return "ObjectiveCaptured: " + objectiveName + (playerName.isBlank() ? "" : " by " + playerName);
    }
}

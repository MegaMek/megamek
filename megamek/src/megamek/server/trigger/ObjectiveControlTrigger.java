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
import megamek.common.equipment.ObjectiveMarker;
import megamek.common.game.IGame;
import megamek.logging.MMLogger;

/**
 * This Trigger reacts while the named objective marker is controlled - by the given player's side, or by any side
 * when the player name is blank. Objective control is resolved in each End Phase (Standard Missions, Objectives), so
 * this trigger reflects the latest End Phase resolution. It reacts every time it is checked while control holds; use
 * {@link OnceTrigger} to limit it.
 *
 * @param objectiveName The exact name of the objective marker
 * @param playerName    The player whose side must control the objective, or blank/{@code null} for any side
 */
public record ObjectiveControlTrigger(String objectiveName, String playerName) implements Trigger {

    public ObjectiveControlTrigger(String objectiveName, @Nullable String playerName) {
        this.objectiveName = objectiveName;
        this.playerName = Objects.requireNonNullElse(playerName, "");
    }

    private static final MMLogger LOGGER = MMLogger.create(ObjectiveControlTrigger.class);

    @Override
    public boolean isTriggered(IGame game, TriggerSituation event) {
        ObjectiveMarker marker = ObjectiveTriggerHelper.findMarker(game, objectiveName);
        if (marker == null) {
            LOGGER.trace("[VictoryTrigger] {}: not triggered - no objective of that name exists", this);
            return false;
        }
        if (marker.isDestroyed()) {
            LOGGER.trace("[VictoryTrigger] {}: not triggered - the objective is destroyed", this);
            return false;
        }
        boolean triggered = ObjectiveTriggerHelper.controllerMatches(game, playerName, marker);
        if (triggered) {
            LOGGER.debug("[VictoryTrigger] {}: TRIGGERED (controlling team {}, player ID {})",
                  this, marker.getControllingTeam(), marker.getControllingPlayerId());
        } else {
            LOGGER.trace("[VictoryTrigger] {}: not triggered (controlling team {}, player ID {})",
                  this, marker.getControllingTeam(), marker.getControllingPlayerId());
        }
        return triggered;
    }

    @Override
    public String toString() {
        return "ObjectiveControl: " + objectiveName + (playerName.isBlank() ? "" : " by " + playerName);
    }
}

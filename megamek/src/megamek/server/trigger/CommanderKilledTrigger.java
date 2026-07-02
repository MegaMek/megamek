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
import megamek.common.game.Game;
import megamek.common.game.IGame;
import megamek.logging.MMLogger;

/**
 * This Trigger reacts while all commanders of a side's enemies have been destroyed (like the classic "Enemy
 * Commander Destroyed" victory option; configure units as commanders in the lobby). When the player name is blank,
 * any side qualifying triggers. Unlike the classic option, this trigger needs no game option - putting it in a
 * victory condition formula is the opt-in. Note that this trigger reacts every time it is checked while the
 * condition holds; use {@link OnceTrigger} to limit it.
 *
 * @param playerName The player whose enemies' commanders must be dead, or blank/{@code null} for any side
 */
public record CommanderKilledTrigger(String playerName) implements Trigger {

    private static final MMLogger LOGGER = MMLogger.create(CommanderKilledTrigger.class);

    public CommanderKilledTrigger(@Nullable String playerName) {
        this.playerName = Objects.requireNonNullElse(playerName, "");
    }

    @Override
    public boolean isTriggered(IGame game, TriggerSituation event) {
        if (!(game instanceof Game twGame)) {
            return false;
        }
        boolean triggered = ObjectiveTriggerHelper.anySideMatches(game, playerName,
              player -> twGame.getPlayersList().stream()
                    .filter(otherPlayer -> otherPlayer.isEnemyOf(player))
                    .mapToInt(twGame::getLiveCommandersOwnedBy)
                    .sum() == 0);
        if (triggered) {
            LOGGER.debug("[VictoryTrigger] {}: TRIGGERED", this);
        } else {
            LOGGER.trace("[VictoryTrigger] {}: not triggered", this);
        }
        return triggered;
    }

    @Override
    public String toString() {
        return "CommanderKilled: all enemy commanders" + (playerName.isBlank() ? "" : " of " + playerName);
    }
}

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
import megamek.common.game.IGame;
import megamek.logging.MMLogger;
import megamek.server.victory.BVVictoryCondition;

/**
 * This Trigger reacts while a side's current BV is at least the given percentage ratio of its enemies' current BV
 * (like the classic "Outnumber enemy" victory option; e.g. 300 means three times the surviving enemy BV; a side with
 * no surviving enemy BV always qualifies). When the player name is blank, any side qualifying triggers. Note that
 * this trigger reacts every time it is checked while the condition holds; use {@link OnceTrigger} to limit it.
 *
 * @param playerName   The player whose side must have the BV advantage, or blank/{@code null} for any side
 * @param ratioPercent The friendly/enemy BV ratio in percent that must be reached
 */
public record BVRatioTrigger(String playerName, int ratioPercent) implements Trigger {

    private static final MMLogger LOGGER = MMLogger.create(BVRatioTrigger.class);

    public BVRatioTrigger(@Nullable String playerName, int ratioPercent) {
        this.playerName = Objects.requireNonNullElse(playerName, "");
        this.ratioPercent = ratioPercent;
    }

    @Override
    public boolean isTriggered(IGame game, TriggerSituation event) {
        boolean triggered = ObjectiveTriggerHelper.anySideMatches(game, playerName, player -> {
            int friendlyBV = BVVictoryCondition.friendlyBV(game, player);
            int enemyBV = BVVictoryCondition.enemyBV(game, player);
            return (enemyBV == 0) || ((100 * friendlyBV) / enemyBV >= ratioPercent);
        });
        if (triggered) {
            LOGGER.debug("[VictoryTrigger] {}: TRIGGERED", this);
        } else {
            LOGGER.trace("[VictoryTrigger] {}: not triggered", this);
        }
        return triggered;
    }

    @Override
    public String toString() {
        return "BVRatio: " + ratioPercent + "%" + (playerName.isBlank() ? "" : " for " + playerName);
    }
}

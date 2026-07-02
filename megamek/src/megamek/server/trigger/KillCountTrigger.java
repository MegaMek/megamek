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

import java.util.Enumeration;
import java.util.Objects;

import megamek.common.Player;
import megamek.common.annotations.Nullable;
import megamek.common.game.Game;
import megamek.common.game.IGame;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * This Trigger reacts while a side has destroyed at least the given number of enemy units (like the classic "Kill
 * count victory" option; friendly fire does not count). When the player name is blank, any side qualifying triggers.
 * Note that this trigger reacts every time it is checked while the condition holds; use {@link OnceTrigger} to limit
 * it.
 *
 * @param playerName The player whose side must have the kills, or blank/{@code null} for any side
 * @param killCount  The number of kills that must be reached
 */
public record KillCountTrigger(String playerName, int killCount) implements Trigger {

    private static final MMLogger LOGGER = MMLogger.create(KillCountTrigger.class);

    public KillCountTrigger(@Nullable String playerName, int killCount) {
        this.playerName = Objects.requireNonNullElse(playerName, "");
        this.killCount = killCount;
    }

    @Override
    public boolean isTriggered(IGame game, TriggerSituation event) {
        if (!(game instanceof Game twGame)) {
            return false;
        }
        boolean triggered = ObjectiveTriggerHelper.anySideMatches(game, playerName,
              player -> countKillsBySideOf(twGame, player) >= killCount);
        if (triggered) {
            LOGGER.debug("[VictoryTrigger] {}: TRIGGERED", this);
        } else {
            LOGGER.trace("[VictoryTrigger] {}: not triggered", this);
        }
        return triggered;
    }

    /**
     * @return The number of enemy units destroyed by the player's side (team kills summed; the player's own kills
     *       when unteamed), counted like the classic kill count victory - friendly fire excluded
     */
    private int countKillsBySideOf(Game game, Player player) {
        return countKills(game, player, game.getWreckedEntities()) + countKills(game, player,
              game.getCarcassEntities());
    }

    private int countKills(Game game, Player player, Enumeration<Entity> victims) {
        int kills = 0;
        while (victims.hasMoreElements()) {
            Entity wreck = victims.nextElement();
            if (!(game.getEntityFromAllSources(wreck.getKillerId()) instanceof Entity killer)
                  || (killer.getOwner() == null) || (wreck.getOwner() == null)) {
                continue;
            }
            // friendly fire does not count; the killer must be on the tested player's side
            if (!wreck.getOwner().isEnemyOf(killer.getOwner()) || killer.getOwner().isEnemyOf(player)) {
                continue;
            }
            kills++;
        }
        return kills;
    }

    @Override
    public String toString() {
        return "KillCount: " + killCount + "+" + (playerName.isBlank() ? "" : " by " + playerName);
    }
}

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
package megamek.client.bot.princess;

import java.util.HashSet;
import java.util.Set;

import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * Decides which of a Princess bot's units withdraw under Forced Withdrawal, and keeps the list of them the bot judges
 * each turn's attacks against. CASPAR inherits it through Princess.
 *
 * <p>A unit withdraws when it is crippled and its bot follows Forced Withdrawal, or when a gamemaster ordered it to,
 * and never when a gamemaster ordered it to fight to the death (see
 * {@link megamek.common.enums.ForcedWithdrawalOrder}). Every withdrawal decision the bot makes asks
 * {@link #isWithdrawing(Entity)}, so its movement, firing and honor rules always agree.</p>
 *
 * <p>The list itself lives in {@link BotMemory}, the bot's one store of what it carries between turns. It is taken
 * at the end of each turn rather than read live, because the bot judges a turn's attacks against the units that
 * were withdrawing when that turn began.</p>
 */
public class ForcedWithdrawalTracker {

    private static final MMLogger LOGGER = MMLogger.create(ForcedWithdrawalTracker.class);

    private final Princess owner;

    /**
     * @param owner the bot whose units this tracks
     */
    ForcedWithdrawalTracker(Princess owner) {
        this.owner = owner;
    }

    /**
     * Returns whether one of the bot's units withdraws under Forced Withdrawal right now.
     *
     * @param entity the unit
     *
     * @return {@code true} if the unit withdraws: crippled under the bot's rules, counting crew damage, or ordered to
     *       by a gamemaster
     */
    public boolean isWithdrawing(Entity entity) {
        return entity.getForcedWithdrawalOrder().isWithdrawing(owner.getForcedWithdrawal(), entity.isCrippled(true));
    }

    /**
     * Retakes the list of the bot's withdrawing units, at the start of the bot's first game and at the end of every
     * turn. Taken even when the bot ignores Forced Withdrawal, since a gamemaster can still order one of its units
     * off the field.
     */
    void refreshWithdrawingUnits() {
        Set<Integer> withdrawingUnitIds = new HashSet<>();
        for (Entity entity : owner.getEntitiesOwned()) {
            if (isWithdrawing(entity)) {
                withdrawingUnitIds.add(entity.getId());
            }
        }
        owner.getMemory().setCrippledUnits(withdrawingUnitIds);
        LOGGER.debug("[ForcedWithdrawal] {} withdrawing units now {}", owner.getName(), withdrawingUnitIds);
    }

    /**
     * Forgets which units were withdrawing, when a game ends and the players return to the lobby. Unit IDs start
     * again in the next game, so a kept list would have the bot treat whichever new unit reuses a withdrawing unit's
     * ID as fleeing for the whole first round.
     */
    void forgetWithdrawingUnits() {
        LOGGER.debug("[ForcedWithdrawal] {} back in the lobby; forgetting withdrawing units {}", owner.getName(),
              owner.getMemory().crippledUnitIds());
        owner.getMemory().setCrippledUnits(Set.of());
    }

    /**
     * @return a snapshot of the bot's units withdrawing as of the last refresh, which the bot reports to clients
     */
    Set<Integer> withdrawingUnitIds() {
        return owner.getMemory().crippledUnitIds();
    }
}

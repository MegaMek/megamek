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
package megamek.common.game;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import megamek.common.units.Entity;

/**
 * The Forced Withdrawal state each Princess bot last reported, as a client sees it. Both the WITHDRAWING status tag on
 * the board and the honor warning read from here, so they always agree with each other and with the bot.
 *
 * <p>A bot that has not reported yet is treated as having no withdrawing units. Bots report at their first movement
 * phase and at the end of every turn, before any unit of theirs can be crippled, so nothing is missed.</p>
 *
 * <p>Concurrent because reports are written on the packet-handling thread and read on the EDT while drawing and
 * firing. Each report is immutable and replaced whole.</p>
 */
public class ForcedWithdrawalReports {

    private final Map<Integer, BotHonorReport> reportsByBot = new ConcurrentHashMap<>();

    /**
     * Stores a bot's latest report, replacing its previous one.
     *
     * @param botPlayerId the reporting bot's player ID
     * @param report      what the bot reported
     */
    public void record(int botPlayerId, BotHonorReport report) {
        reportsByBot.put(botPlayerId, report);
    }

    /**
     * Forgets every report. Call when a game ends and the players return to the lobby: unit IDs start again from the
     * beginning in the next game, so an old report would tag whichever new unit happens to reuse a withdrawing unit's
     * ID.
     */
    public void clear() {
        reportsByBot.clear();
    }

    /**
     * Returns {@code true} when the unit's owner is a bot that follows Forced Withdrawal and has reported this unit as
     * withdrawing. Crippled units of human players and of bots that ignore Forced Withdrawal are never withdrawing.
     *
     * @param entity the unit to check
     *
     * @return {@code true} if the unit is withdrawing under the Forced Withdrawal rules
     */
    public boolean isWithdrawing(Entity entity) {
        BotHonorReport report = reportsByBot.get(entity.getOwnerId());
        boolean isOwnerFollowingForcedWithdrawal = (report != null) && report.followsForcedWithdrawal();
        return isOwnerFollowingForcedWithdrawal && report.withdrawingUnitIds().contains(entity.getId());
    }

    /**
     * Returns {@code true} only when the bot has reported that it does not follow Forced Withdrawal. Such a bot fights
     * to the death and never judges anyone's honor. A bot that has not reported yet is assumed to follow the rules,
     * which is the Princess default.
     *
     * @param botPlayerId the bot's player ID
     *
     * @return {@code true} if the bot is known to ignore Forced Withdrawal
     */
    public boolean ignoresForcedWithdrawal(int botPlayerId) {
        BotHonorReport report = reportsByBot.get(botPlayerId);
        return (report != null) && !report.followsForcedWithdrawal();
    }
}

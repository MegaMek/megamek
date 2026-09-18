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

import java.util.ArrayList;
import java.util.List;

import megamek.common.annotations.Nullable;

/**
 * What a bot remembers about one infantry vs. infantry action (TO:AR p. 169) from round to round: the page for that
 * building in the {@link BotMemory}.
 *
 * <p>An action can run for many rounds. The board shows only this round's odds, which cannot tell a fight that went
 * in at two to one and is sliding from one that started at the same odds it has now and is holding. This page keeps
 * the odds of every round the bot has looked at the fight, so the trend can be asked for.</p>
 */
public class FightMemory {

    private final int buildingId;
    private final List<OddsRecord> oddsByRound = new ArrayList<>();

    /**
     * The strengths in one round of an action, as the bot saw them on its declaration turn.
     *
     * @param round          the game round
     * @param attackerPoints the Marine Points Score of the attacking side
     * @param defenderPoints the Marine Points Score of the defending side, building modifier included
     */
    public record OddsRecord(int round, double attackerPoints, double defenderPoints) {

        /**
         * @return attackers over defenders; a defence of nothing makes the odds as good as they can be
         */
        public double odds() {
            if (defenderPoints <= 0) {
                return Double.MAX_VALUE;
            }
            return attackerPoints / defenderPoints;
        }
    }

    FightMemory(int buildingId) {
        this.buildingId = buildingId;
    }

    public int getBuildingId() {
        return buildingId;
    }

    /**
     * Notes this round's strengths. Looking at the same round twice keeps the newer figures, so a round is always
     * one record.
     *
     * @param record the strengths to note
     */
    void rememberOdds(OddsRecord record) {
        if (!oddsByRound.isEmpty() && (oddsByRound.getLast().round() == record.round())) {
            oddsByRound.removeLast();
        }
        oddsByRound.add(record);
    }

    /**
     * @return the first strengths the bot noted for this action, or {@code null} if it has noted none
     */
    public @Nullable OddsRecord firstRecord() {
        return oddsByRound.isEmpty() ? null : oddsByRound.getFirst();
    }

    /**
     * @return the newest strengths the bot noted for this action, or {@code null} if it has noted none
     */
    public @Nullable OddsRecord latestRecord() {
        return oddsByRound.isEmpty() ? null : oddsByRound.getLast();
    }

    /**
     * @return how many rounds of this action the bot has noted
     */
    public int roundsNoted() {
        return oddsByRound.size();
    }

    /**
     * Whether the attackers' odds have fallen in each of the last so many rounds, judged from the newest record
     * backwards. Two falls need three records; with fewer the answer is {@code false}, since a trend that short
     * is a die roll and not a slide.
     *
     * @param consecutiveFalls how many round-on-round falls in a row are asked for
     *
     * @return {@code true} if the odds fell that many rounds running
     */
    public boolean oddsHaveFallenFor(int consecutiveFalls) {
        if ((consecutiveFalls < 1) || (oddsByRound.size() <= consecutiveFalls)) {
            return false;
        }
        for (int step = 0; step < consecutiveFalls; step++) {
            OddsRecord newer = oddsByRound.get(oddsByRound.size() - 1 - step);
            OddsRecord older = oddsByRound.get(oddsByRound.size() - 2 - step);
            if (newer.odds() >= older.odds()) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return the noted strengths, oldest first
     */
    public List<OddsRecord> oddsByRound() {
        return List.copyOf(oddsByRound);
    }
}

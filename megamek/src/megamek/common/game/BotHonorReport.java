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

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * What a Princess bot tells every client about its honor rules, sent through
 * {@link megamek.common.net.enums.PacketCommand#PRINCESS_DISHONORED}.
 *
 * <p>Forced Withdrawal is a per-bot behavior setting, not a game option, so a human client cannot know on its own
 * whether a bot follows it or which of its units it counts as withdrawing. The bot says so here. Its withdrawing units
 * are exactly the ones it will treat as "fleeing" when it judges this turn's attacks, so the WITHDRAWING tag and the
 * honor warning can match the bot's verdict instead of guessing from damage.</p>
 *
 * <p>Sent over the network only; it is never part of a savegame.</p>
 *
 * @param dishonoredPlayerIds     the players this bot considers dishonored, already resolved for pirates
 * @param followsForcedWithdrawal {@code true} if this bot follows the Forced Withdrawal rules
 * @param withdrawingUnitIds      the bot's own units withdrawing under Forced Withdrawal; empty when it does not follow
 *                                the rules
 */
public record BotHonorReport(List<Integer> dishonoredPlayerIds, boolean followsForcedWithdrawal,
      Set<Integer> withdrawingUnitIds) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Copies the collections into a plain {@link ArrayList} and {@link HashSet}. The server's network filter
     * ({@link megamek.common.net.marshalling.SanityInputFilter}) rejects the JDK's immutable sets, such as those made by
     * {@code Set.copyOf}, and drops the sending bot's connection, so the stored copies must be classes it allows.
     */
    public BotHonorReport {
        dishonoredPlayerIds = new ArrayList<>(dishonoredPlayerIds);
        withdrawingUnitIds = new HashSet<>(withdrawingUnitIds);
    }

    /**
     * @return the players this bot considers dishonored, as a read-only view
     */
    @Override
    public List<Integer> dishonoredPlayerIds() {
        return Collections.unmodifiableList(dishonoredPlayerIds);
    }

    /**
     * @return the bot's units withdrawing under Forced Withdrawal, as a read-only view
     */
    @Override
    public Set<Integer> withdrawingUnitIds() {
        return Collections.unmodifiableSet(withdrawingUnitIds);
    }
}

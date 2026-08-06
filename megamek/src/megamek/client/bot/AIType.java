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
package megamek.client.bot;

import megamek.common.annotations.Nullable;

/**
 * Identifies which bot AI implementation to construct. Used by {@link BotFactory} as the single selection point for
 * building bots, so callers (lobby, scenario loaders, headless runners) no longer hardcode a concrete bot class.
 * Additional AI types (for example a future experimental bot) are added here as their implementations land.
 */
public enum AIType {
    /** The default MegaMek bot, {@link megamek.client.bot.princess.Princess}. */
    PRINCESS,

    /** The experimental successor to Princess, {@link megamek.client.bot.caspar.Caspar}. */
    CASPAR;

    /**
     * Parses an AI type from a string (case-insensitive match against the enum name), as used by the scenario
     * {@code ai:} key and the {@code /replacePlayer -b:} chat argument.
     *
     * @param value the string to parse, may be {@code null}
     *
     * @return the matching {@link AIType}, or {@code null} if the value is {@code null} or matches no type
     */
    public static @Nullable AIType fromString(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        for (AIType aiType : values()) {
            if (aiType.name().equalsIgnoreCase(trimmed)) {
                return aiType;
            }
        }
        return null;
    }
}
